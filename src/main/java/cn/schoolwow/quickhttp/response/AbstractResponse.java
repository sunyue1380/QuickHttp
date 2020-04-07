package cn.schoolwow.quickhttp.response;

import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.DocumentParser;
import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import cn.schoolwow.quickhttp.domain.ResponseMeta;
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
import java.net.SocketTimeoutException;
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
    /**返回元数据*/
    private ResponseMeta responseMeta = new ResponseMeta();
    /**输入流*/
    private BufferedInputStream bufferedInputStream;
    /**输入流字符串*/
    private String body;
    /**Document对象*/
    private Document document;
    /**Document对象*/
    private DocumentParser documentParser;

    public AbstractResponse(HttpURLConnection httpURLConnection) throws IOException{
        this.httpURLConnection = httpURLConnection;
        this.statusCode = httpURLConnection.getResponseCode();
        this.statusMessage = httpURLConnection.getResponseMessage();
        if(null==this.statusMessage){
            this.statusMessage = "";
        }
        //提取头部信息
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        Set<String> keySet = headerFields.keySet();
        for(String key:keySet){
            if(key==null){
                continue;
            }
            String value = httpURLConnection.getHeaderField(key);
            switch(key.toLowerCase()){
                case "content-type":{
                    responseMeta.contentType = value;
                }break;
                case "content-encoding":{
                    responseMeta.contentEncoding = value;
                }break;
                case "content-length":{
                    responseMeta.contentLength = Long.parseLong(value);
                }break;
                case "content-disposition":{
                    responseMeta.contentDisposition = value;
                }break;
            }
            headerMap.put(key,value);
        }
        headerMap = Collections.unmodifiableMap(headerMap);
        logger.debug("[获取头部信息]headFields:{}", JSON.toJSONString(headerMap));
        //提取body信息
        {
            InputStream inputStream = httpURLConnection.getErrorStream()!=null?httpURLConnection.getErrorStream():httpURLConnection.getInputStream();
            if(responseMeta.contentEncoding!=null&&!responseMeta.contentEncoding.isEmpty()){
                if(responseMeta.contentEncoding.equals("gzip")){
                    logger.debug("[返回gzip格式流]Content-Encoding:{}",responseMeta.contentEncoding);
                    inputStream = new GZIPInputStream(inputStream);
                }else if(responseMeta.contentEncoding.equals("deflate")){
                    logger.debug("[返回deflate格式流]Content-Encoding:{}",responseMeta.contentEncoding);
                    inputStream = new InflaterInputStream(inputStream,new Inflater(true));
                }
            }
            bufferedInputStream = new BufferedInputStream(inputStream);
        }
        getCharset();
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
        return responseMeta.contentType;
    }

    @Override
    public long contentLength() {
        return responseMeta.contentLength;
    }

    @Override
    public String filename() {
        if(responseMeta.contentDisposition==null){
            return null;
        }
        String contentDisposition = responseMeta.contentDisposition;
        String prefix = "filename=";
        String filename = contentDisposition.substring(contentDisposition.indexOf(prefix)+prefix.length());
        filename = filename.replace("\"","").trim();
        return filename;
    }

    @Override
    public boolean acceptRanges() {
        return hasHeaderWithValue("Accept-Ranges","bytes");
    }

    @Override
    public boolean hasHeader(String name) {
        return headerMap.containsKey(name);
    }

    @Override
    public boolean hasHeaderWithValue(String name, String value) {
        return hasHeader(name)&&headerMap.get(name).equals(value);
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
        return null!=QuickHttp.getCookie(httpURLConnection.getURL().getHost(),name);
    }

    @Override
    public boolean hasCookieWithValue(String name, String value) {
        HttpCookie httpCookie = QuickHttp.getCookie(httpURLConnection.getURL().getHost(),name);
        return null!=httpCookie&&httpCookie.getValue().equals(value);
    }

    @Override
    public HttpCookie cookie(String name) {
        return QuickHttp.getCookie(httpURLConnection.getURL().getHost(),name);
    }

    @Override
    public List<HttpCookie> cookieList() {
        return QuickHttp.getCookies(httpURLConnection.getURL().getHost());
    }

    @Override
    public String body() throws IOException {
        if(body!=null){
            return body;
        }
        byte[] bytes = bodyAsBytes();
        body = Charset.forName(charset).decode(ByteBuffer.wrap(bytes)).toString();
        return body;
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

    public JSONObject jsonpAsJSONObject() throws IOException {
        body();
        int startIndex = body.indexOf("(")+1,endIndex = body.lastIndexOf(")");
        return JSON.parseObject(body.substring(startIndex,endIndex));
    }

    public JSONArray jsonpAsJSONArray() throws IOException {
        body();
        int startIndex = body.indexOf("(")+1,endIndex = body.lastIndexOf(")");
        return JSON.parseArray(body.substring(startIndex,endIndex));
    }

    @Override
    public byte[] bodyAsBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[QuickHttpConfig.BUFFER_SIZE];
        int length = 0 ;
        int retryTimes = QuickHttpConfig.retryTimes;
        while(retryTimes>0){
            try {
                while((length=bufferedInputStream.read(bytes,0,bytes.length))!=-1){
                    baos.write(bytes,0,length);
                }
                break;
            }catch (SocketTimeoutException e){
                logger.warn("[超时异常]{}",e.getMessage());
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
            retryTimes--;
        }
        baos.flush();
        bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }

    @Override
    public BufferedInputStream bodyStream() {
        return bufferedInputStream;
    }

    @Override
    public Document parse() throws IOException {
        if(document==null){
            if(body==null){
                body();
            }
            document = Document.parse(body);
        }
        return document;
    }

    @Override
    public DocumentParser parser() throws IOException {
        if(documentParser==null){
            if(body==null){
                body();
            }
            documentParser = DocumentParser.parse(body);
        }
        return documentParser;
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
        getCharsetFromContentType(responseMeta.contentType);
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
        if(charset==null){
            charset = "utf-8";
            logger.debug("[获取charset为空]使用默认编码:utf-8");
        }else{
            logger.debug("[获取charset]charset:{}",charset);
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
