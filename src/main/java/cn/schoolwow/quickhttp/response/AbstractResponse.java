package cn.schoolwow.quickhttp.response;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
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
    private String charset;
    /**头部信息*/
    private Map<String,String> headerMap = new HashMap<>();
    /**本URI对应的Cookie*/
    private List<HttpCookie> httpCookieList;
    /**输入流*/
    private InputStream inputStream;

    public AbstractResponse(HttpURLConnection httpURLConnection) throws IOException {
        this.httpURLConnection = httpURLConnection;
        try {
            this.httpCookieList = ((CookieManager) CookieHandler.getDefault()).getCookieStore().get(httpURLConnection.getURL().toURI());
        }catch (Exception e){
            e.printStackTrace();
            logger.warn("[Cookie获取失败]网站Cookie信息获取失败!url:"+httpURLConnection.getURL());
        }
        //获取状态信息
        this.statusCode = httpURLConnection.getResponseCode();
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
                    logger.debug("[提取charset]charset:{},content-type:{}",charset,contentType);
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
        try {
            byte[] bytes = new byte[inputStream.available()];
            return Charset.forName(charset).decode(ByteBuffer.wrap(bytes)).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.httpURLConnection.disconnect();
        }
        return null;
    }

    @Override
    public byte[] bodyAsBytes() {
        try {
            return new byte[inputStream.available()];
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.httpURLConnection.disconnect();
        }
        return null;
    }

    @Override
    public BufferedInputStream bodyStream() {
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
            return bufferedInputStream;
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            this.httpURLConnection.disconnect();
        }
    }

    @Override
    public boolean close() {
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.httpURLConnection.setUseCaches(true);
        this.httpURLConnection.disconnect();
        return false;
    }
}
