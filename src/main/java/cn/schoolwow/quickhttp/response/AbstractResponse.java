package cn.schoolwow.quickhttp.response;

import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.net.HttpURLConnection;
import java.nio.Buffer;
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
    private BufferedInputStream bufferedInputStream;
    /**输入流字符串*/
    private String body;
    /**Document对象*/
    private Document document;

    public AbstractResponse(HttpURLConnection httpURLConnection) throws IOException{
        this.httpURLConnection = httpURLConnection;
        this.statusCode = httpURLConnection.getResponseCode();
        this.statusMessage = httpURLConnection.getResponseMessage();
        this.httpCookieList = QuickHttp.getCookies(httpURLConnection.getURL());
        //提取头部信息
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        Set<String> keySet = headerFields.keySet();
        for(String key:keySet){
            if(key==null){
                continue;
            }
            headerMap.put(key.toLowerCase(),httpURLConnection.getHeaderField(key));
        }
        headerMap = Collections.unmodifiableMap(headerMap);
        logger.debug("[获取头部信息]headFields:{}", JSON.toJSONString(headerMap));
        //提取body信息
        {
            InputStream inputStream = httpURLConnection.getErrorStream()!=null?httpURLConnection.getErrorStream():httpURLConnection.getInputStream();
            String contentEncoding = headerMap.get("content-encoding");
            if(contentEncoding!=null&&!contentEncoding.isEmpty()){
                if(contentEncoding.equals("gzip")){
                    logger.debug("[返回gzip格式流]Content-Encoding:{}",contentEncoding);
                    inputStream = new GZIPInputStream(inputStream);
                }else if(contentEncoding.equals("deflate")){
                    logger.debug("[返回deflate格式流]Content-Encoding:{}",contentEncoding);
                    inputStream = new InflaterInputStream(inputStream,new Inflater(true));
                }
            }
            bufferedInputStream = new BufferedInputStream(inputStream);
        }
        getCharset();
        if(this.charset==null){
            this.charset = "utf-8";
            logger.debug("[获取charset为空]使用默认编码:utf-8");
        }else{
            logger.debug("[获取charset]charset:{}",charset);
        }
    }

    @Override
    public String url() {
        return httpURLConnection.getURL().toString();
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
    public long contentLength() {
        if(!headerMap.containsKey("content-length")){
            return -1;
        }
        return Long.parseLong(headerMap.get("content-length"));
    }

    @Override
    public String filename() {
        if(!headerMap.containsKey("content-disposition")){
            return null;
        }
        String contentDisposition = headerMap.get("content-disposition");
        String prefix = "filename=";
        String filename = contentDisposition.substring(contentDisposition.indexOf(prefix)+prefix.length());
        filename = filename.replace("\"","").trim();
        return filename;
    }

    @Override
    public boolean hasHeader(String name) {
        return headerMap.containsKey(name.toLowerCase());
    }

    @Override
    public boolean hasHeaderWithValue(String name, String value) {
        return hasHeader(name)&&headerMap.get(name.toLowerCase()).equals(value);
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
        return httpCookieList.stream().anyMatch(httpCookie -> httpCookie.getName().equals(name));
    }

    @Override
    public boolean hasCookieWithValue(String name, String value) {
        return httpCookieList.stream().anyMatch(httpCookie -> httpCookie.getName().equals(name)&&httpCookie.getValue().equals(value));
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
            int length = 0;
            byte[] bytes = new byte[QuickHttpConfig.BUFFER_SIZE];
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            while((length = bufferedInputStream.read(bytes,0,bytes.length))!=-1){
                baos.write(bytes,0,length);
            }
            body = Charset.forName(charset).decode(ByteBuffer.wrap(baos.toByteArray())).toString();
            return body;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }finally {
            close();
        }
    }

    @Override
    public JSONObject bodyAsJSONObject(){
        body();
        JSONObject object = JSON.parseObject(body);
        return object;
    }

    @Override
    public JSONArray bodyAsJSONArray(){
        body();
        JSONArray array = JSON.parseArray(body);
        return array;
    }

    public JSONObject jsonpAsJSONObject(){
        body();
        int startIndex = body.indexOf("(")+1,endIndex = body.lastIndexOf(")");
        return JSON.parseObject(body.substring(startIndex,endIndex));
    }

    public JSONArray jsonpAsJSONArray(){
        body();
        int startIndex = body.indexOf("(")+1,endIndex = body.lastIndexOf(")");
        return JSON.parseArray(body.substring(startIndex,endIndex));
    }

    @Override
    public byte[] bodyAsBytes() {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] bytes = new byte[8192];
            int length = 0 ;
            while((length=bufferedInputStream.read(bytes,0,bytes.length))!=-1){
                baos.write(bytes,0,length);
            }
            bytes = baos.toByteArray();
            baos.close();
            return bytes;
        }catch (IOException e){
            e.printStackTrace();
            return null;
        }finally {
            close();
        }
    }

    @Override
    public BufferedInputStream bodyStream() {
        return bufferedInputStream;
    }

    @Override
    public Document parse(){
        if(document==null){
            if(body==null){
                body();
            }
            document = Document.parse(body);
        }
        return document;
    }

    @Override
    public void close() {
        try {
            bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.httpURLConnection.disconnect();
    }

    private void getCharset() throws IOException {
        {
            String contentType = headerMap.get("content-type");
            getCharsetFromContentType(contentType);
        }
        if(charset==null){
            byte[] bytes = new byte[1024*5];
            bufferedInputStream.mark(bytes.length);
            bufferedInputStream.read(bytes,0,bytes.length);
            boolean readFully = (bufferedInputStream.read()==-1);
            bufferedInputStream.reset();
            ByteBuffer firstBytes = ByteBuffer.wrap(bytes);
            getCharsetFromBOM(firstBytes);
            if(charset==null){
                getCharsetFromMeta(firstBytes,readFully);
            }
        }
    }

    private void getCharsetFromMeta(ByteBuffer byteBuffer,boolean readFully){
        String docData = Charset.forName("utf-8").decode(byteBuffer).toString();
        //判断是否是HTML或者XML文档
        if(!docData.startsWith("<?xml")&&!docData.startsWith("<!DOCTYPE")){
            return;
        }
        Document doc = Document.parse(docData);
        if(doc.root()==null){
            //不是HTML文档
            return;
        }
        Elements metaElements = doc.select("meta[http-equiv=content-type], meta[charset]");
        for (Element meta : metaElements) {
            if (meta.hasAttr("http-equiv")) {
                getCharsetFromContentType(meta.attr("content"));
            }
            if (charset == null && meta.hasAttr("charset")) {
                charset = meta.attr("charset");
            }
            break;
        }

        if(charset==null){
            Element root = doc.root();
            if(doc.root().tagName().equals("?xml")&&root.hasAttr("encoding")){
                charset = root.attr("encoding");
            }
        }
        if(readFully){
            this.document = doc;
        }
    }

    private void getCharsetFromBOM(ByteBuffer byteBuffer) throws IOException {
        final Buffer buffer = byteBuffer;
        buffer.mark();
        byte[] bom = new byte[4];
        if (byteBuffer.remaining() >= bom.length) {
            byteBuffer.get(bom);
            buffer.rewind();
        }
        if (bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF ||
                bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == 0x00 && bom[3] == 0x00) {
            charset = "utf-32";
        } else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF ||
                bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            charset = "utf-16";
        } else if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            charset = "utf-8";
        }
        if(charset!=null){
            bufferedInputStream.skip(1);
        }
    }

    private void getCharsetFromContentType(String contentType){
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
            }
        }
    }
}
