package cn.schoolwow.quickhttp.connection;

import cn.schoolwow.quickhttp.response.AbstractResponse;
import cn.schoolwow.quickhttp.response.Response;
import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import cn.schoolwow.quickhttp.util.ValidateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

public class AbstractConnection implements Connection{
    private static Logger logger = LoggerFactory.getLogger(AbstractConnection.class);
    private static final char[] mimeBoundaryChars =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int boundaryLength = 32;

    /**访问地址*/
    private URL url;
    /**请求方法*/
    private Method method;
    /**Http代理*/
    protected Proxy proxy;
    /**Header信息*/
    private Map<String,String> headers = new HashMap<>();
    /**Data信息*/
    private Map<String,String> dataMap = new HashMap<>();
    /**DataFile信息*/
    private Map<String,File> dataFileMap = new HashMap<>();
    /**超时设置*/
    private int timeout = 3000;
    /**自动重定向*/
    private boolean followRedirects = true;
    /**是否忽略http状态异常*/
    private boolean ignoreHttpErrors = true;
    /**自定义请求体*/
    private String requestBody;
    /**请求编码*/
    private String charset = "utf-8";
    /**请求类型*/
    private String contentType = "application/x-www-form-urlencoded; charset="+charset;
    /**用户代理*/
    private String userAgent = UserAgent.CHROME.userAgent;
    /**重试次数*/
    private int retryTimes = -1;
    /**Cookie管理器*/
    private CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
    /**自定义SSL工厂*/
    private static SSLSocketFactory sslSocketFactory;
    /**HostnameVerifier*/
    private static HostnameVerifier hostnameVerifier;
    static{
        try {
            SSLContext sslcontext = SSLContext.getInstance("SSL","SunJSSE");
            sslcontext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate certificates[],String authType) throws CertificateException {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] ax509certificate, String s) throws CertificateException {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }
            }}, new java.security.SecureRandom());
            sslSocketFactory = sslcontext.getSocketFactory();
            hostnameVerifier = (s,sslSession)->true;
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("[初始化SSL信息失败]信息:{}",e.getMessage());
        }
    }

    public static Connection getConnection(String url){
        return new AbstractConnection(url);
    }

    private AbstractConnection(String url){
        this.url(url);
    }

    @Override
    public Connection url(URL url) {
        ValidateUtil.checkNotNull(url,"URL不能为空!");
        this.url = url;
        return this;
    }

    @Override
    public Connection url(String url) {
        ValidateUtil.checkNotEmpty(url,"URL不能为空!");
        try {
            this.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("URL不合法!url:"+url, e);
        }
        return this;
    }

    @Override
    public Connection proxy(Proxy proxy) {
        ValidateUtil.checkNotNull(proxy,"代理对象不能为空!");
        this.proxy = proxy;
        return this;
    }

    @Override
    public Connection proxy(String host, int port) {
        ValidateUtil.checkNotEmpty(host,"代理地址不能为空!");
        ValidateUtil.checkArgument(port>0,"代理端口必须大于0!port:"+port);
        this.proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(host,port));
        return this;
    }

    @Override
    public Connection userAgent(String userAgent) {
        ValidateUtil.checkNotEmpty(userAgent,"用户代理不能为空!");
        this.userAgent = userAgent;
        return this;
    }

    @Override
    public Connection userAgent(UserAgent userAgent) {
        ValidateUtil.checkNotNull(userAgent,"用户代理不能为空!");
        this.userAgent = userAgent.userAgent;
        return this;
    }

    @Override
    public Connection referrer(String referrer) {
        ValidateUtil.checkNotEmpty(referrer,"Referer值不能为空!");
        headers.put("Referer",referrer);
        return this;
    }

    @Override
    public Connection timeout(int millis) {
        ValidateUtil.checkArgument(millis>=0,"超时时间必须大于0!millis:"+millis);
        this.timeout = millis;
        return this;
    }

    @Override
    public Connection followRedirects(boolean followRedirects) {
        this.followRedirects = followRedirects;
        return this;
    }

    @Override
    public Connection method(Method method) {
        ValidateUtil.checkNotNull(method,"请求方法不能为空!");
        this.method = method;
        return this;
    }

    @Override
    public Connection ignoreHttpErrors(boolean ignoreHttpErrors) {
        this.ignoreHttpErrors = ignoreHttpErrors;
        return this;
    }

    @Override
    public Connection sslSocketFactory(SSLSocketFactory sslSocketFactory) {
        ValidateUtil.checkNotNull(method,"sslSocketFactory不能为空!");
        AbstractConnection.sslSocketFactory = sslSocketFactory;
        return this;
    }

    @Override
    public Connection data(String key, String value) {
        dataMap.put(key,value);
        return this;
    }

    @Override
    public Connection data(String key, File file) {
        dataFileMap.put(key,file);
        return this;
    }

    @Override
    public Connection data(Map<String, String> data) {
        dataMap.putAll(data);
        return this;
    }

    @Override
    public Connection requestBody(String body) {
        this.requestBody = body;
        return this;
    }

    @Override
    public Connection requestBody(JSONObject body) {
        this.requestBody = body.toJSONString();
        this.contentType = "application/json; charset="+charset;
        return this;
    }

    @Override
    public Connection requestBody(JSONArray array) {
        this.requestBody = array.toJSONString();
        this.contentType = "application/json; charset="+charset;
        return this;
    }

    @Override
    public Connection header(String name, String value) {
        ValidateUtil.checkNotEmpty(name,"name不能为空!");
        if(name.toLowerCase().equals("cookie")){
            String[] tokens = value.split(";");
            for(String token:tokens){
                int startIndex = token.indexOf("=");
                String _name = token.substring(0,startIndex).trim();
                String _value = token.substring(startIndex+1).trim();
                cookie(_name,_value);
            }
        }else{
            this.headers.put(name,value);
        }
        return this;
    }

    @Override
    public Connection headers(Map<String, String> headers) {
        ValidateUtil.checkNotEmpty(headers,"headers不能为空!");
        Set<String> keySet = headers.keySet();
        for(String key:keySet){
            header(key, headers.get(key));
        }
        return this;
    }

    @Override
    public Connection cookie(String name, String value) {
        ValidateUtil.checkNotNull(this.url,"URL不能为空!");
        try {
            List<HttpCookie> httpCookieList = cookieManager.getCookieStore().get(url.toURI());
            boolean find = false;
            for(HttpCookie httpCookie:httpCookieList){
                if(httpCookie.getName().equals(name)){
                    httpCookie.setValue(value);
                    find = true;
                    break;
                }
            }
            if(!find){
                HttpCookie httpCookie = new HttpCookie(name,value);
                httpCookieList.add(httpCookie);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public Connection cookies(Map<String, String> cookies) {
        Set<String> keySet = cookies.keySet();
        for(String key:keySet){
            cookie(key, cookies.get(key));
        }
        return this;
    }

    @Override
    public Connection charset(String charset) {
        this.charset = charset;
        return this;
    }

    @Override
    public Connection retryTimes(int retryTimes){
        this.retryTimes = retryTimes;
        return this;
    }

    @Override
    public Response execute() throws IOException {
        String protocol = url.getProtocol();
        ValidateUtil.checkArgument(protocol.matches("http(s)?"),"只支持http和https协议.当前协议:"+protocol);
        //生成参数序列化字符串
        StringBuilder parameterBuilder = new StringBuilder();
        if(!dataMap.isEmpty()){
            Set<Map.Entry<String,String>> entrySet = dataMap.entrySet();
            for(Map.Entry<String,String> entry:entrySet){
                parameterBuilder.append(URLEncoder.encode(entry.getKey(),charset)+"="+URLEncoder.encode(entry.getValue(),charset)+"&");
            }
            parameterBuilder.deleteCharAt(parameterBuilder.length()-1);
        }
        //设置url请求参数
        if(!method.hasBody()){
            String parameter = (url.getQuery()==null?"":url.getQuery())+parameterBuilder.toString();
            if(parameter!=null&&!parameter.equals("")){
                parameter = "?"+parameter;
            }
            url = new URL(url.getProtocol()+"://"+url.getAuthority()+url.getPath()+parameter);
        }
        //创建Connection实例
        if(proxy==null){
            proxy = QuickHttpConfig.proxy;
        }
        final HttpURLConnection httpURLConnection = (HttpURLConnection) (
                proxy==null?url.openConnection():url.openConnection(proxy)
        );
        logger.debug("[打开链接]地址:{} {},代理:{}",method.name(),url,proxy==null?"无":proxy.address());
        //判断是否https
        if (httpURLConnection instanceof HttpsURLConnection) {
            ((HttpsURLConnection)httpURLConnection).setSSLSocketFactory(AbstractConnection.sslSocketFactory);
            ((HttpsURLConnection)httpURLConnection).setHostnameVerifier(AbstractConnection.hostnameVerifier);
        }
        //设置头部
        {
            Set<Map.Entry<String, String>> entrySet = headers.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                logger.debug("[设置头部]name:{},value:{}", entry.getKey(), entry.getValue());
            }
        }
        //当前Cookie
        {
            try {
                List<HttpCookie> httpCookieList = cookieManager.getCookieStore().get(url.toURI());
                for(HttpCookie httpCookie:httpCookieList){
                    logger.debug("[设置Cookie]name:{},value:{},path:{}",httpCookie.getName(),httpCookie.getValue(),httpCookie.getPath());
                }
            }catch (URISyntaxException e){
                e.printStackTrace();
            }
        }
        //设置请求方法
        httpURLConnection.setRequestMethod(method.name());
        logger.debug("[设置请求方法]设置Method:{}",method.name());
        //设置超时时间
        httpURLConnection.setConnectTimeout(timeout);
        logger.debug("[设置超时时间]设置超时时间:{}",timeout);
        httpURLConnection.setReadTimeout(timeout/2);
        //设置是否自动重定向
        httpURLConnection.setInstanceFollowRedirects(followRedirects);
        //设置用户代理
        httpURLConnection.setRequestProperty("User-Agent",userAgent);
        logger.debug("[设置用户代理]UserAgent:{}",userAgent);
        //设置请求类型
        httpURLConnection.setRequestProperty("Content-Type",contentType);
        logger.debug("[设置类型]Content-Type:{}",contentType);
        //设置Content-Encoding
        httpURLConnection.setRequestProperty("Accept-Encoding","gzip, deflate");
        //执行请求
        httpURLConnection.setDoInput(true);
        if(method.hasBody()){
            //优先级 dataFile > requestBody > dataMap

            //设置Content-Length
            String boundary = null;
            if(!dataFileMap.isEmpty()){
                boundary = mimeBoundary();
                httpURLConnection.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary);
                httpURLConnection.setChunkedStreamingMode(0);
            }else if(requestBody!=null&&!requestBody.equals("")){
                httpURLConnection.setFixedLengthStreamingMode(requestBody.length());
            }else if(!dataMap.isEmpty()){
                httpURLConnection.setFixedLengthStreamingMode(parameterBuilder.toString().length());
            }

            //开始正式写入数据
            httpURLConnection.setDoOutput(true);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, charset));
            if(!dataFileMap.isEmpty()){
                if(!dataMap.isEmpty()) {
                    Set<Map.Entry<String, String>> entrySet = dataMap.entrySet();
                    for (Map.Entry<String, String> entry : entrySet) {
                        w.write("--");
                        w.write(boundary);
                        w.write("\r\n");
                        w.write("Content-Disposition: form-data; name=\""+entry.getKey().replaceAll("\"", "%22")+"\"\r\n");
                        w.write("\r\n");
                        w.write(entry.getValue());
                        w.write("\r\n");
                    }
                }
                Set<Map.Entry<String, File>> entrySet = dataFileMap.entrySet();
                for (Map.Entry<String, File> entry : entrySet) {
                    File file = entry.getValue();
                    String name = entry.getKey().replaceAll("\"", "%22");
                    String fileName = file.getName().replaceAll("\"", "%22");

                    w.write("--");
                    w.write(boundary);
                    w.write("\r\n");
                    w.write("Content-Disposition: form-data; name=\""+name+"\"; filename=\""+fileName+"\"\r\n");
                    w.write("Content-Type: "+Files.probeContentType(Paths.get(file.getAbsolutePath()))+"\r\n");
                    w.write("\r\n");
                    w.flush();
                    //写入文件二进制流
                    FileInputStream fileInputStream = new FileInputStream(file);
                    final byte[] buffer = new byte[QuickHttpConfig.BUFFER_SIZE];
                    int len;
                    while ((len = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, len);
                    }
                    outputStream.flush();
                    w.write("\r\n");
                }
                w.write("--");
                w.write(boundary);
                w.write("--");
            }else if(requestBody!=null&&!requestBody.equals("")){
                w.write(requestBody);
            }else if(!dataMap.isEmpty()){
                w.write(parameterBuilder.toString());
            }
            w.flush();
            w.close();
        }

        int statusCode = httpURLConnection.getResponseCode();
        if(!ignoreHttpErrors){
            if(statusCode<200||statusCode>=400){
                throw new IOException("http状态异常!statusCode:"+statusCode+",访问地址:"+url.toExternalForm());
            }
        }
        Response response = new AbstractResponse(httpURLConnection,retryTimes);
        return response;
    }

    /**创建随机Boundary字符串作为分隔符*/
    private static String mimeBoundary() {
        final StringBuilder mime = new StringBuilder(boundaryLength);
        final Random rand = new Random();
        for (int i = 0; i < boundaryLength; i++) {
            mime.append(mimeBoundaryChars[rand.nextInt(mimeBoundaryChars.length)]);
        }
        return mime.toString();
    }
}
