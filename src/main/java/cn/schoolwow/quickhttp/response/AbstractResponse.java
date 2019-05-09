package cn.schoolwow.quickhttp.response;

import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class AbstractResponse implements Response{
    private Logger logger = LoggerFactory.getLogger(AbstractResponse.class);
    /**HttpUrlConnection对象*/
    private HttpURLConnection httpURLConnection;
    /**状态码*/
    private int statusCode;
    /**消息*/
    private String statusMessage;
    /**编码格式*/
    private String charset = "utf-8";
    /**头部信息*/
    private Map<String,String> headerMap = new HashMap<>();
    /**本URI对应的Cookie*/
    private List<HttpCookie> httpCookieList;
    /**输入流*/
    private InputStream inputStream;
    /**输入流字符串*/
    private String body;

    public AbstractResponse(HttpURLConnection httpURLConnection,int retryTimes) throws IOException {
        this.httpURLConnection = httpURLConnection;
        try {
            this.httpCookieList = ((CookieManager) CookieHandler.getDefault()).getCookieStore().get(httpURLConnection.getURL().toURI());
        }catch (Exception e){
            e.printStackTrace();
            logger.warn("[Cookie获取失败]网站Cookie信息获取失败!url:"+httpURLConnection.getURL());
        }
        //重试机制
        if(retryTimes<=0){
            retryTimes = QuickHttpConfig.retryTimes;
        }
        //获取状态信息
        int timeout = httpURLConnection.getConnectTimeout();
        for(int i=1;i<=retryTimes;i++){
            try {
                httpURLConnection.setConnectTimeout(timeout);
                httpURLConnection.setReadTimeout(timeout/2);
                this.statusCode = httpURLConnection.getResponseCode();
                break;
            }catch (SocketTimeoutException e){
                timeout = timeout*2;
                if(timeout>=60000){
                    timeout = 60000;
                }
                logger.warn("[链接超时]第{}次尝试重连,总共{}次,设置超时时间:{},地址:{}",i,retryTimes,timeout,httpURLConnection.getURL());
            }
        }
        this.statusMessage = httpURLConnection.getResponseMessage();
        //提取头部信息
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        logger.debug("[获取头部信息]headFields:{}", JSON.toJSONString(headerFields));
        Set<String> keySet = headerFields.keySet();
        for(String key:keySet){
            if(key==null){
                continue;
            }
            headerMap.put(key.toLowerCase(),httpURLConnection.getHeaderField(key));
        }
        headerMap = Collections.unmodifiableMap(headerMap);
        //提取编码格式
        {
            String contentType = headerMap.get("content-type");
            String prefix = "charset=";
            if(contentType!=null&&contentType.contains(prefix)){
                int startIndex = contentType.indexOf(prefix);
                if(startIndex>=0){
                    int endIndex = contentType.lastIndexOf(";");
                    if(endIndex>startIndex){
                        charset = contentType.substring(startIndex+prefix.length(),endIndex).trim();
                    }else if(endIndex<startIndex){
                        charset = contentType.substring(startIndex+prefix.length()).trim();
                    }
                    logger.debug("[提取charset][Charset]:{},[Content-Type]:{}",charset,contentType);
                }
            }
        }
        //提取body信息
        {
            inputStream = httpURLConnection.getErrorStream()!=null?httpURLConnection.getErrorStream():httpURLConnection.getInputStream();
            String contentEncoding = headerMap.get("content-encoding");
            if(contentEncoding!=null&&!contentEncoding.isEmpty()){
                if(contentEncoding.equals("gzip")){
                    inputStream = new GZIPInputStream(inputStream);
                }else if(contentEncoding.equals("deflate")){
                    inputStream = new InflaterInputStream(inputStream,new Inflater(true));
                }
            }
        }
    }

    @Override
    public int statusCode() {
        return this.statusCode;
    }

    @Override
    public String statusMessage() {
        return this.statusMessage;
    }

    @Override
    public String charset() {
        return this.charset;
    }

    @Override
    public String contentType() {
        return headerMap.get("content-type");
    }

    @Override
    public boolean hasHeader(String name) {
        return headerMap.containsKey(name);
    }

    @Override
    public boolean hasHeaderWithValue(String name, String value) {
        return headerMap.containsKey(name)&&headerMap.get(name).equals(value);
    }

    @Override
    public String header(String name) {
        return headerMap.get(name);
    }

    @Override
    public Map<String, String> headers() {
        return headerMap;
    }

    @Override
    public boolean hasCookie(String name) {
        return httpCookieList.stream().allMatch(httpCookie -> httpCookie.getName().equals(name));
    }

    @Override
    public boolean hasCookieWithValue(String name, String value) {
        return httpCookieList.stream().allMatch(httpCookie -> httpCookie.getName().equals(name)&&httpCookie.getValue().equals(value));
    }

    @Override
    public HttpCookie cookie(String name) {
        return httpCookieList.stream().filter(httpCookie -> httpCookie.getName().equals(name)).findFirst().orElse(null);
    }

    @Override
    public List<HttpCookie> cookieList() {
        return this.httpCookieList;
    }

    @Override
    public String body() {
        if(body!=null){
            return body;
        }
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(inputStream));
            StringBuffer buffer = new StringBuffer();
            String line = null;
            while ((line = in.readLine()) != null){
                buffer.append(line);
            }
            body = Charset.forName(charset).decode(ByteBuffer.wrap(buffer.toString().getBytes())).toString();
            return buffer.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            close();
        }
    }

    @Override
    public JSONObject bodyAsJSONObject() throws IOException {
        body();
        JSONObject object = JSON.parseObject(body);
        return object;
    }

    @Override
    public JSONArray bodyAsJSONArray() throws IOException {
        body();
        JSONArray array = JSON.parseArray(body);
        return array;
    }

    public JSONObject jsonpAsJSONObject() throws IOException{
        body();
        int startIndex = body.indexOf("(")+1,endIndex = body.lastIndexOf(")");
        return JSON.parseObject(body.substring(startIndex,endIndex));
    }

    public JSONArray jsonpAsJSONArray() throws IOException{
        body();
        int startIndex = body.indexOf("(")+1,endIndex = body.lastIndexOf(")");
        return JSON.parseArray(body.substring(startIndex,endIndex));
    }

    @Override
    public byte[] bodyAsBytes() {
        try {
            return new byte[inputStream.available()];
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            close();
        }
    }

    @Override
    public BufferedInputStream bodyStream() {
        try {
            return new BufferedInputStream(inputStream);
        } finally {
            close();
        }
    }

    @Override
    public void close() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.httpURLConnection.disconnect();
    }
}
