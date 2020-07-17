package cn.schoolwow.quickhttp.connection;

import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.domain.RequestMeta;
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
import java.nio.file.StandardCopyOption;
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

    private RequestMeta requestMeta = new RequestMeta();
    /**自定义SSL工厂*/
    private static SSLSocketFactory sslSocketFactory;
    /**HostnameVerifier*/
    private static HostnameVerifier hostnameVerifier;
    static{
        try {
            SSLContext sslcontext = SSLContext.getInstance("TLSv1.2");
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

    public static Connection getConnection(URL url){
        return new AbstractConnection(url);
    }

    private AbstractConnection(String url){
        this.url(url);
    }

    private AbstractConnection(URL url){
        this.url(url);
    }

    @Override
    public Connection url(URL url) {
        ValidateUtil.checkNotNull(url,"URL不能为空!");
        requestMeta.url = url;
        return this;
    }

    @Override
    public Connection url(String url) {
        ValidateUtil.checkNotEmpty(url,"URL不能为空!");
        try {
            requestMeta.url = new URL(url);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("URL不合法!url:"+url, e);
        }
        return this;
    }

    @Override
    public Connection proxy(Proxy proxy) {
        ValidateUtil.checkNotNull(proxy,"代理对象不能为空!");
        requestMeta.proxy = proxy;
        return this;
    }

    @Override
    public Connection proxy(String host, int port) {
        ValidateUtil.checkNotEmpty(host,"代理地址不能为空!");
        ValidateUtil.checkArgument(port>0,"代理端口必须大于0!port:"+port);
        requestMeta.proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(host,port));
        return this;
    }

    @Override
    public Connection userAgent(String userAgent) {
        ValidateUtil.checkNotEmpty(userAgent,"用户代理不能为空!");
        requestMeta.headers.put("User-Agent",userAgent);
        return this;
    }

    @Override
    public Connection userAgent(UserAgent userAgent) {
        ValidateUtil.checkNotNull(userAgent,"用户代理不能为空!");
        requestMeta.headers.put("User-Agent",userAgent.userAgent);
        return this;
    }

    @Override
    public Connection referrer(String referrer) {
        ValidateUtil.checkNotEmpty(referrer,"Referer值不能为空!");
        requestMeta.headers.put("Referer",referrer);
        return this;
    }

    @Override
    public Connection contentType(String contentType) {
        requestMeta.contentType = contentType;
        return this;
    }

    @Override
    public Connection ajax() {
        return header("X-Requested-With", "XMLHttpRequest")
                .header("Origin",requestMeta.url.getProtocol()+"://"+requestMeta.url.getHost());
    }

    @Override
    public Connection ranges(long start, long end) {
        return header("Range","bytes="+start+"-"+(end>0?end:""));
    }

    @Override
    public Connection timeout(int millis) {
        ValidateUtil.checkArgument(millis>=0,"超时时间必须大于0!millis:"+millis);
        requestMeta.timeout = millis;
        return this;
    }

    @Override
    public Connection followRedirects(boolean followRedirects) {
        requestMeta.followRedirects = followRedirects;
        return this;
    }

    @Override
    public Connection method(String method) {
        for(Method methodEnum:Method.values()){
            if(methodEnum.name().equalsIgnoreCase(method)){
                requestMeta.method = methodEnum;
                break;
            }
        }
        ValidateUtil.checkNotNull(requestMeta.method,"不支持的请求方法!"+method);
        return this;
    }

    @Override
    public Connection method(Method method) {
        ValidateUtil.checkNotNull(method,"请求方法不能为空!");
        requestMeta.method = method;
        return this;
    }

    @Override
    public Connection ignoreHttpErrors(boolean ignoreHttpErrors) {
        requestMeta.ignoreHttpErrors = ignoreHttpErrors;
        return this;
    }

    @Override
    public Connection sslSocketFactory(SSLSocketFactory sslSocketFactory) {
        ValidateUtil.checkNotNull(sslSocketFactory,"sslSocketFactory不能为空!");
        AbstractConnection.sslSocketFactory = sslSocketFactory;
        return this;
    }

    @Override
    public Connection data(String key, String value) {
        requestMeta.dataMap.put(key,value);
        return this;
    }

    @Override
    public Connection data(String key, File file) {
        //IdentityHashMap的判断依据是==,故new String(key)时必要的,不要删除此代码
        requestMeta.dataFileMap.put(new String(key),file);
        return this;
    }

    @Override
    public Connection data(String key, String name, InputStream inputStream){
        data(key,name,"QuickHttpTempFile_"+System.currentTimeMillis()+".tmp",inputStream);
        return this;
    }

    @Override
    public Connection data(String key, String name, String fileName, InputStream inputStream){
        try {
            File file = new File(System.getProperty("java.io.tmpdir")+File.separator+"quickhttp"+File.separator+fileName);
            Files.copy(inputStream,file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            requestMeta.dataFileMap.put(new String(key),file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    @Override
    public Connection data(Map<String, String> data) {
        requestMeta.dataMap.putAll(data);
        return this;
    }

    @Override
    public Connection requestBody(String body) {
        requestMeta.requestBody = body;
        return this;
    }

    @Override
    public Connection requestBody(JSONObject body) {
        requestMeta.requestBody = body.toJSONString();
        return this;
    }

    @Override
    public Connection requestBody(JSONArray array) {
        requestMeta.requestBody = array.toJSONString();
        return this;
    }

    @Override
    public Connection header(String name, String value) {
        ValidateUtil.checkNotEmpty(name,"name不能为空!");
        requestMeta.headers.put(name,value);
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
        HttpCookie httpCookie = new HttpCookie(name,value);
        httpCookie.setMaxAge(3600000);
        httpCookie.setDomain("."+requestMeta.url.getHost());
        httpCookie.setPath("/");
        httpCookie.setVersion(0);
        httpCookie.setDiscard(false);
        requestMeta.httpCookieList.add(httpCookie);
        return this;
    }

    @Override
    public Connection cookie(HttpCookie httpCookie) {
        requestMeta.httpCookieList.add(httpCookie);
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
        requestMeta.headers.put("Authorization","Basic "+encoded);
        return this;
    }

    @Override
    public Connection charset(String charset) {
        requestMeta.charset = charset;
        return this;
    }

    @Override
    public Connection retryTimes(int retryTimes){
        requestMeta.retryTimes = retryTimes;
        return this;
    }

    @Override
    public Response execute() throws IOException {
        String protocol = requestMeta.url.getProtocol();
        ValidateUtil.checkArgument(protocol.matches("http(s)?"),"只支持http和https协议.当前协议:"+protocol);
        //生成参数序列化字符串
        StringBuilder parameterBuilder = new StringBuilder();
        if(!requestMeta.dataMap.isEmpty()){
            Set<Map.Entry<String,String>> entrySet = requestMeta.dataMap.entrySet();
            for(Map.Entry<String,String> entry:entrySet){
                String value = entry.getValue();
                if(null!=value){
                    value = URLEncoder.encode(value,requestMeta.charset);
                }
                parameterBuilder.append(URLEncoder.encode(entry.getKey(),requestMeta.charset)+"="+value+"&");
            }
            parameterBuilder.deleteCharAt(parameterBuilder.length()-1);
        }
        //创建Connection实例
        if(requestMeta.proxy==null){
            requestMeta.proxy = QuickHttpConfig.proxy;
        }
        //重试机制
        if(requestMeta.retryTimes<0){
            requestMeta.retryTimes = QuickHttpConfig.retryTimes;
        }
        Response response = null;
        //解决由于CookieHandler的全局作用域带来的错误修改Cookie值的情况
        synchronized (QuickHttp.cookieManager){
            QuickHttp.addCookie(requestMeta.httpCookieList);
            for(int i=0;i<=requestMeta.retryTimes;i++){
                try {
                    HttpURLConnection connection = createHttpUrlConnection(parameterBuilder);
                    response = new AbstractResponse(connection);
                    break;
                }catch (SocketTimeoutException | ConnectException e){
                    if(i==requestMeta.retryTimes){
                        throw e;
                    }
                    logger.warn("[链接超时]重试{}/{}次,原因:{},超时时间:{}ms,,地址:{}",i+1,requestMeta.retryTimes,e.getMessage(),requestMeta.timeout,requestMeta.url);
                    requestMeta.timeout = requestMeta.timeout*2;
                    if(requestMeta.timeout>=QuickHttpConfig.maxTimeout){
                        requestMeta.timeout = QuickHttpConfig.maxTimeout;
                    }
                }
            }
        }
        //HttpUrlConnection无法处理从http到https的重定向或者https到http的重定向
        while(requestMeta.followRedirects&&response.statusCode()>=300&&response.statusCode()<400&&response.hasHeader("Location")){
            if(requestMeta.redirectTimes>=QuickHttpConfig.maxRedirectTimes){
                throw new IOException("重定向次数过多!当前次数:"+requestMeta.redirectTimes+",限制最大次数:"+QuickHttpConfig.maxRedirectTimes);
            }
            //处理相对路径形式的重定向
            String redirectUrl = response.header("Location");
            if(redirectUrl.startsWith("http")){
                this.url(redirectUrl);
            }else if(redirectUrl.startsWith("/")){
                this.url(requestMeta.url.getProtocol()+"://"+requestMeta.url.getHost()+":"+(requestMeta.url.getPort()==-1?requestMeta.url.getDefaultPort():requestMeta.url.getPort())+redirectUrl);
            }else{
                String u = requestMeta.url.toString();
                this.url(u.substring(0,u.lastIndexOf("/"))+"/"+redirectUrl);
            }
            requestMeta.redirectTimes++;
            response = execute();
        }
        if(!requestMeta.ignoreHttpErrors){
            if(response.statusCode()<200||response.statusCode()>=400){
                throw new IOException("http状态异常!statusCode:"+response.statusCode()+",访问地址:"+requestMeta.url.toExternalForm());
            }
        }
        if(QuickHttpConfig.interceptor!=null){
            QuickHttpConfig.interceptor.afterConnection(this,response);
        }
        //写入文本文件
        List<HttpCookie> httpCookieList = QuickHttp.getCookies();
        if(httpCookieList.size()>0&&null!=QuickHttp.cookiesFileUrl){
            logger.debug("[写入Cookie文件]写入Cookie个数:{}",httpCookieList.size());
            File file = new File(QuickHttp.cookiesFileUrl.getFile());
            PrintWriter printWriter = new PrintWriter(file);
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

    @Override
    public RequestMeta requestMeta() {
        return this.requestMeta;
    }

    @Override
    public Connection requestMeta(RequestMeta requestMeta) {
        this.requestMeta = requestMeta;
        return this;
    }

    @Override
    public Connection clone(){
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(this.requestMeta);
            oos.close();

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            ObjectInputStream ois = new ObjectInputStream(bais);
            RequestMeta requestMeta = (RequestMeta) ois.readObject();
            requestMeta.method = this.requestMeta.method;
            requestMeta.proxy = this.requestMeta.proxy;
            requestMeta.httpCookieList = this.requestMeta.httpCookieList;
            AbstractConnection connection = (AbstractConnection) QuickHttp.connect(this.requestMeta.url)
                    .requestMeta(requestMeta);
            return connection;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**创建HttppUrlConnection对象*/
    private HttpURLConnection createHttpUrlConnection(StringBuilder parameterBuilder) throws IOException {
        URL actualUrl = requestMeta.url;
        //设置url请求参数
        if(!requestMeta.method.hasBody()){
            String parameter = (requestMeta.url.getQuery()==null?"":requestMeta.url.getQuery())+parameterBuilder.toString();
            if(parameter!=null&&!parameter.equals("")){
                parameter = "?"+parameter;
            }
            actualUrl = new URL(requestMeta.url.getProtocol()+"://"+requestMeta.url.getAuthority()+requestMeta.url.getPath()+parameter);
        }
        final HttpURLConnection httpURLConnection = (HttpURLConnection) (
                requestMeta.proxy==null?actualUrl.openConnection():actualUrl.openConnection(requestMeta.proxy)
        );
        logger.info("[请求行]{} {},代理:{}",requestMeta.method.name(),actualUrl,requestMeta.proxy==null?"无":requestMeta.proxy.address());
        //判断是否https
        if (httpURLConnection instanceof HttpsURLConnection) {
            ((HttpsURLConnection)httpURLConnection).setSSLSocketFactory(AbstractConnection.sslSocketFactory);
            ((HttpsURLConnection)httpURLConnection).setHostnameVerifier(AbstractConnection.hostnameVerifier);
        }
        httpURLConnection.setRequestMethod(requestMeta.method.name());
        httpURLConnection.setConnectTimeout(requestMeta.timeout/4);
        httpURLConnection.setReadTimeout(requestMeta.timeout/4*3);
        httpURLConnection.setInstanceFollowRedirects(false);
        //设置头部
        {
            Set<Map.Entry<String, String>> entrySet = requestMeta.headers.entrySet();
            for (Map.Entry<String, String> entry : entrySet) {
                httpURLConnection.setRequestProperty(entry.getKey(), entry.getValue());
            }
        }
        logger.debug("[请求头部]{}",requestMeta.headers);
        if(QuickHttpConfig.interceptor!=null){
            QuickHttpConfig.interceptor.beforeConnect(this);
        }

        //执行请求
        httpURLConnection.setDoInput(true);
        if(requestMeta.method.hasBody()){
            //优先级 dataFile > requestBody > dataMap
            String boundary = null;
            if(!requestMeta.dataFileMap.isEmpty()){
                boundary = mimeBoundary();
                httpURLConnection.setRequestProperty("Content-Type","multipart/form-data; boundary="+boundary+"; charset="+requestMeta.charset);
                httpURLConnection.setChunkedStreamingMode(0);
                logger.debug("[请求体]multipart/form-data,{}",requestMeta.dataFileMap);
            }else if(requestMeta.requestBody!=null&&!requestMeta.requestBody.equals("")){
                httpURLConnection.setRequestProperty("Content-Type","application/json; charset="+requestMeta.charset+";");
                httpURLConnection.setFixedLengthStreamingMode(requestMeta.requestBody.getBytes().length);
                logger.debug("[请求体]application/json,{}",requestMeta.requestBody);
            }else if(!requestMeta.dataMap.isEmpty()){
                httpURLConnection.setRequestProperty("Content-Type","application/x-www-form-urlencoded; charset="+requestMeta.charset);
                httpURLConnection.setFixedLengthStreamingMode(parameterBuilder.toString().getBytes().length);
                logger.debug("[请求体]application/x-www-form-urlencoded,{}",requestMeta.dataMap);
            }
            if(requestMeta.contentType!=null&&!requestMeta.contentType.isEmpty()){
                httpURLConnection.setRequestProperty("Content-Type",requestMeta.contentType);
            }

            //开始正式写入数据
            httpURLConnection.setDoOutput(true);
            OutputStream outputStream = httpURLConnection.getOutputStream();
            final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, requestMeta.charset));
            if(!requestMeta.dataFileMap.isEmpty()){
                if(!requestMeta.dataMap.isEmpty()) {
                    Set<Map.Entry<String, String>> entrySet = requestMeta.dataMap.entrySet();
                    for (Map.Entry<String, String> entry : entrySet) {
                        w.write("--"+boundary+"\r\n");
                        w.write("Content-Disposition: form-data; name=\""+entry.getKey().replace("\"", "%22")+"\"\r\n");
                        w.write("\r\n");
                        w.write(entry.getValue());
                        w.write("\r\n");
                    }
                }
                Set<Map.Entry<String, File>> entrySet = requestMeta.dataFileMap.entrySet();
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
            }else if(requestMeta.requestBody!=null&&!requestMeta.requestBody.equals("")){
                w.write(requestMeta.requestBody);
            }else if(!requestMeta.dataMap.isEmpty()){
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
