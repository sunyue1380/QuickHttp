package cn.schoolwow.quickhttp.connection;

import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.response.AbstractResponse;
import cn.schoolwow.quickhttp.response.Response;
import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import cn.schoolwow.quickhttp.util.ValidateUtil;
import com.alibaba.fastjson.JSON;
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
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class AbstractConnection implements Connection{
    private static Logger logger = LoggerFactory.getLogger(AbstractConnection.class);
    private static final char[] mimeBoundaryChars =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int boundaryLength = 32;
    /**保存历史记录*/
    private static Map<String,String> historyMap = new HashMap<>();

    /**访问地址*/
    private URL url;
    /**请求方法*/
    private Method method = Method.GET;
    /**Http代理*/
    protected Proxy proxy;
    /**Header信息*/
    private Map<String,String> headers = new HashMap<>();
    /**Data信息*/
    private Map<String,String> dataMap = new HashMap<>();
    /**DataFile信息*/
    private Map<String,File> dataFileMap = new IdentityHashMap<>();
    /**超时设置*/
    private int timeout = 3000;
    /**自动重定向*/
    private boolean followRedirects = true;
    /**是否忽略http状态异常*/
    private boolean ignoreHttpErrors = false;
    /**自定义请求体*/
    private String requestBody;
    /**请求编码*/
    private String charset = "utf-8";
    /**请求类型*/
    private String contentType;
    /**用户代理*/
    private String userAgent = UserAgent.CHROME.userAgent;
    /**重试次数*/
    private int retryTimes = -1;
    /**重定向次数*/
    private int redirectTimes = 0;
    /**Cookie*/
    private String hostCookie;
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
    public Connection contentType(String contentType) {
        this.contentType = contentType;
        return this;
    }

    @Override
    public Connection ajax() {
        return header("X-Requested-With", "XMLHttpRequest")
                .header("Origin",url.getProtocol()+"://"+url.getHost());
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
        //IdentityHashMap的判断依据是==,故new String(key)时必要的,不要删除此代码
        dataFileMap.put(new String(key),file);
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
        return this;
    }

    @Override
    public Connection requestBody(JSONArray array) {
        this.requestBody = array.toJSONString();
        return this;
    }

    @Override
    public Connection header(String name, String value) {
        ValidateUtil.checkNotEmpty(name,"name不能为空!");
        if(name.toLowerCase().equals("cookie")){
            QuickHttp.addCookie(value,this.url);
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
        QuickHttp.addCookie(name,value,this.url);
        return this;
    }

    @Override
    public Connection cookie(HttpCookie httpCookie) {
        QuickHttp.addCookie(httpCookie);
        return this;
    }

    @Override
    public Connection cookie(List<HttpCookie> httpCookieList) {
        for(HttpCookie httpCookie:httpCookieList){
            cookie(httpCookie);
        }
        return null;
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
    public Connection basicAuth(String username, String password) {
        String encoded = Base64.getEncoder().encodeToString((username+":"+password).getBytes());
        headers.put("Authorization","Basic "+encoded);
        return this;
    }

    @Override
    public Connection charset(String charset) {
        this.charset = charset;
        return this;
    }

    @Override
    public Connection noCookie() {
        hostCookie = QuickHttp.getCookieString(url);
        QuickHttp.removeCookie(url);
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
                String value = entry.getValue();
                if(null!=value){
                    value = URLEncoder.encode(value,charset);
                }
                parameterBuilder.append(URLEncoder.encode(entry.getKey(),charset)+"="+value+"&");
            }
            parameterBuilder.deleteCharAt(parameterBuilder.length()-1);
        }
        //创建Connection实例
        if(proxy==null){
            proxy = QuickHttpConfig.proxy;
        }
        //重试机制
        if(retryTimes<0){
            retryTimes = QuickHttpConfig.retryTimes;
        }
        Response response = null;
        for(int i=0;i<=retryTimes;i++){
            try {
                HttpURLConnection connection = createHttpUrlConnection(parameterBuilder);
                response = new AbstractResponse(connection);
                break;
            }catch (SocketTimeoutException | ConnectException e){
                if(i==retryTimes){
                    throw e;
                }
                timeout = timeout*2;
                if(timeout>=QuickHttpConfig.maxTimeout){
                    timeout = QuickHttpConfig.maxTimeout;
                }
                logger.warn("[链接超时]原因:{},第{}次尝试重连,总共{}次,设置超时时间:{},地址:{}",e.getMessage(),i,retryTimes,timeout,url);
            }
        }
        //HttpUrlConnection无法处理从http到https的重定向或者https到http的重定向
        while(followRedirects&&response.statusCode()>=300&&response.statusCode()<400&&response.hasHeader("Location")){
            if(redirectTimes>=QuickHttpConfig.maxRedirectTimes){
                throw new IOException("重定向次数过多!当前次数:"+redirectTimes+",限制最大次数:"+QuickHttpConfig.maxRedirectTimes);
            }
            //处理相对路径形式的重定向
            String redirectUrl = response.header("Location");
            if(redirectUrl.startsWith("http")){
                this.url(redirectUrl);
            }else if(redirectUrl.startsWith("/")){
                this.url(this.url.getProtocol()+"://"+this.url.getHost()+":"+this.url.getDefaultPort()+"/"+redirectUrl);
            }else{
                String u = url.toString();
                this.url(u.substring(0,u.lastIndexOf("/"))+"/"+redirectUrl);
            }
            redirectTimes++;
            response = execute();
        }
        if(!ignoreHttpErrors){
            if(response.statusCode()<200||response.statusCode()>=400){
                throw new IOException("http状态异常!statusCode:"+response.statusCode()+",访问地址:"+url.toExternalForm());
            }
        }
        historyMap.put(url.getHost(),url.toString());
        if(QuickHttpConfig.interceptor!=null){
            QuickHttpConfig.interceptor.afterConnection(this,response);
        }
        if(hostCookie!=null&&!hostCookie.isEmpty()){
            QuickHttp.addCookie(hostCookie,url);
            hostCookie = null;
        }
        //写入文本文件
        List<HttpCookie> httpCookieList = QuickHttp.getCookies();
        if(httpCookieList.size()>0&&null!=QuickHttpConfig.cookiesFile){
            logger.debug("[写入Cookie文件]写入Cookie个数:{}",httpCookieList.size());
            PrintWriter printWriter = new PrintWriter(QuickHttpConfig.cookiesFile);
            printWriter.print(JSON.toJSONString(httpCookieList));
            printWriter.flush();
            printWriter.close();
        }
        return response;
    }

    @Override
    public void enqueue(Response.CallBack callBack) {
        ThreadPoolExecutorHolder.threadPoolExecutor.submit(()->{
            try {
                Response response = execute();
                callBack.onResponse(response);
            } catch (IOException e) {
                e.printStackTrace();
                callBack.onError(this,e);
            }
        });
    }

    /**创建HttppUrlConnection对象*/
    private HttpURLConnection createHttpUrlConnection(StringBuilder parameterBuilder) throws IOException {
        URL actualUrl = url;
        //设置url请求参数
        if(!method.hasBody()){
            String parameter = (url.getQuery()==null?"":url.getQuery())+parameterBuilder.toString();
            if(parameter!=null&&!parameter.equals("")){
                parameter = "?"+parameter;
            }
            actualUrl = new URL(url.getProtocol()+"://"+url.getAuthority()+url.getPath()+parameter);
        }
        final HttpURLConnection httpURLConnection = (HttpURLConnection) (
                proxy==null?actualUrl.openConnection():actualUrl.openConnection(proxy)
        );
        logger.info("[打开链接]地址:{} {},代理:{}",method.name(),actualUrl,proxy==null?"无":proxy.address());
        //判断是否https
        if (httpURLConnection instanceof HttpsURLConnection) {
            ((HttpsURLConnection)httpURLConnection).setSSLSocketFactory(AbstractConnection.sslSocketFactory);
            ((HttpsURLConnection)httpURLConnection).setHostnameVerifier(AbstractConnection.hostnameVerifier);
        }
        //当前Cookie
        {
            try {
                CookieManager cookieManager = (CookieManager) CookieHandler.getDefault();
                List<HttpCookie> httpCookieList = cookieManager.getCookieStore().get(url.toURI());
                for(HttpCookie httpCookie:httpCookieList){
                    logger.debug("[设置Cookie]{}:{},{}",httpCookie.getName(),httpCookie.getValue(),JSON.toJSONString(httpCookie));
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
        logger.debug("[设置链接超时时间]设置链接超时时间:{}",httpURLConnection.getConnectTimeout());
        httpURLConnection.setReadTimeout(timeout);
        logger.debug("[设置读取超时时间]设置读取超时时间:{}",httpURLConnection.getReadTimeout());
        httpURLConnection.setInstanceFollowRedirects(false);
        //设置用户代理
        httpURLConnection.setRequestProperty("User-Agent",userAgent);
        logger.debug("[设置用户代理]UserAgent:{}",userAgent);
        //设置Content-Encoding
        httpURLConnection.setRequestProperty("Accept-Encoding","gzip, deflate");
        //设置Referer
        if(QuickHttpConfig.refer&&!historyMap.isEmpty()&&!headers.containsKey("Referer")){
            String referer = historyMap.get(url.getHost());
            if(referer!=null&&!referer.equals("")){
                httpURLConnection.setRequestProperty("Referer",referer);
                logger.debug("[设置Referer]Referer:{}",referer);
            }
        }
        //设置头部
        {
            Set<Map.Entry<String, String>> entrySet = headers.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
                logger.debug("[设置头部]name:{},value:{}", entry.getKey(), entry.getValue());
            }
        }
        if(QuickHttpConfig.interceptor!=null){
            QuickHttpConfig.interceptor.beforeConnect(this);
        }

        //执行请求
        httpURLConnection.setDoInput(true);
        if(method.hasBody()){
            //优先级 dataFile > requestBody > dataMap
            String boundary = null;
            if(!dataFileMap.isEmpty()){
                boundary = mimeBoundary();
                httpURLConnection.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary+"; charset="+charset);
                httpURLConnection.setChunkedStreamingMode(0);
            }else if(requestBody!=null&&!requestBody.equals("")){
                httpURLConnection.setRequestProperty("Content-Type","application/json; charset="+charset+";");
                httpURLConnection.setFixedLengthStreamingMode(requestBody.getBytes().length);
            }else if(!dataMap.isEmpty()){
                httpURLConnection.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset="+charset);
                httpURLConnection.setFixedLengthStreamingMode(parameterBuilder.toString().getBytes().length);
            }
            if(contentType!=null&&!contentType.isEmpty()){
                httpURLConnection.setRequestProperty("Content-Type",contentType);
            }

            //开始正式写入数据
            httpURLConnection.setDoOutput(true);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, charset));
            if(!dataFileMap.isEmpty()){
                if(!dataMap.isEmpty()) {
                    Set<Map.Entry<String, String>> entrySet = dataMap.entrySet();
                    for (Map.Entry<String, String> entry : entrySet) {
                        w.write("--"+boundary+"\r\n");
                        w.write("Content-Disposition: form-data; name=\""+entry.getKey().replace("\"", "%22")+"\"\r\n");
                        w.write("\r\n");
                        w.write(entry.getValue());
                        w.write("\r\n");
                    }
                }
                Set<Map.Entry<String, File>> entrySet = dataFileMap.entrySet();
                for (Map.Entry<String, File> entry : entrySet) {
                    File file = entry.getValue();
                    String name = entry.getKey().replace("\"", "%22");
                    String fileName = file.getName().replace("\"", "%22");

                    w.write("--"+boundary+"\r\n");
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
                w.write("--"+boundary+"--\r\n");
            }else if(requestBody!=null&&!requestBody.equals("")){
                w.write(requestBody);
            }else if(!dataMap.isEmpty()){
                w.write(parameterBuilder.toString());
            }
            w.flush();
            w.close();
        }
        return httpURLConnection;
    }

    private static class ThreadPoolExecutorHolder{
        private static ThreadPoolExecutor threadPoolExecutor;
        static{
            threadPoolExecutor = new ThreadPoolExecutor(QuickHttpConfig.corePoolSize,QuickHttpConfig.maximumPoolSize,1, TimeUnit.MINUTES,QuickHttpConfig.blockingQueue);
        }
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
