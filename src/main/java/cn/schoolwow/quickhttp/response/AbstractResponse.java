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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

public class AbstractResponse implements Response{
    private Logger logger = LoggerFactory.getLogger(AbstractResponse.class);
    /**返回元数据*/
    private ResponseMeta responseMeta = new ResponseMeta();

    public AbstractResponse(HttpURLConnection httpURLConnection) throws IOException{
        responseMeta.httpURLConnection = httpURLConnection;
        responseMeta.statusCode = httpURLConnection.getResponseCode();
        responseMeta.statusMessage = httpURLConnection.getResponseMessage();
        if(null==responseMeta.statusMessage){
            responseMeta.statusMessage = "";
        }
        //提取头部信息
        Map<String, List<String>> headerFields = httpURLConnection.getHeaderFields();
        Set<String> keySet = headerFields.keySet();
        for(String key:keySet){
            if(null==key){
                continue;
            }
            responseMeta.headerMap.put(key,httpURLConnection.getHeaderField(key));
        }
        responseMeta.contentType = httpURLConnection.getContentType();
        responseMeta.contentDisposition = httpURLConnection.getHeaderField("content-disposition");
        //提取body信息
        {
            String contentEncoding = httpURLConnection.getContentEncoding();
            InputStream inputStream = httpURLConnection.getErrorStream()!=null?httpURLConnection.getErrorStream():httpURLConnection.getInputStream();
            if(contentEncoding!=null&&!contentEncoding.isEmpty()){
                if(contentEncoding.equals("gzip")){
                    inputStream = new GZIPInputStream(inputStream);
                }else if(contentEncoding.equals("deflate")){
                    inputStream = new InflaterInputStream(inputStream,new Inflater(true));
                }
            }
            responseMeta.bufferedInputStream = new BufferedInputStream(inputStream);
        }
        getCharset();
        logger.debug("[返回头]{} {}",responseMeta.statusCode,responseMeta.statusMessage);
        logger.debug("[返回头部]{}",responseMeta.headerMap);
    }

    @Override
    public String url() {
        return responseMeta.httpURLConnection.getURL().toString();
    }

    @Override
    public int statusCode() {
        return responseMeta.statusCode;
    }

    @Override
    public String statusMessage() {
        return responseMeta.statusMessage;
    }

    @Override
    public String charset() {
        return responseMeta.charset;
    }

    @Override
    public String contentType() {
        return responseMeta.httpURLConnection.getContentType();
    }

    @Override
    public long contentLength() {
        return responseMeta.httpURLConnection.getContentLengthLong();
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
        return responseMeta.headerMap.containsKey(name);
    }

    @Override
    public boolean hasHeaderWithValue(String name, String value) {
        return hasHeader(name)&&responseMeta.headerMap.get(name).equals(value);
    }

    @Override
    public String header(String name) {
        return responseMeta.headerMap.get(name);
    }

    @Override
    public Map<String, String> headers() {
        return responseMeta.headerMap;
    }

    @Override
    public boolean hasCookie(String name) {
        return null!=QuickHttp.getCookie(responseMeta.httpURLConnection.getURL().getHost(),name);
    }

    @Override
    public boolean hasCookieWithValue(String name, String value) {
        HttpCookie httpCookie = QuickHttp.getCookie(responseMeta.httpURLConnection.getURL().getHost(),name);
        return null!=httpCookie&&httpCookie.getValue().equals(value);
    }

    @Override
    public HttpCookie cookie(String name) {
        return QuickHttp.getCookie(responseMeta.httpURLConnection.getURL().getHost(),name);
    }

    @Override
    public List<HttpCookie> cookieList() {
        return QuickHttp.getCookies(responseMeta.httpURLConnection.getURL().getHost());
    }

    @Override
    public String body() throws IOException {
        if(responseMeta.body!=null){
            return responseMeta.body;
        }
        byte[] bytes = bodyAsBytes();
        responseMeta.body = Charset.forName(responseMeta.charset).decode(ByteBuffer.wrap(bytes)).toString();
        return responseMeta.body;
    }

    @Override
    public JSONObject bodyAsJSONObject() throws IOException {
        body();
        JSONObject object = JSON.parseObject(responseMeta.body);
        return object;
    }

    @Override
    public JSONArray bodyAsJSONArray() throws IOException {
        body();
        JSONArray array = JSON.parseArray(responseMeta.body);
        return array;
    }

    public JSONObject jsonpAsJSONObject() throws IOException {
        body();
        int startIndex = responseMeta.body.indexOf("(")+1,endIndex = responseMeta.body.lastIndexOf(")");
        return JSON.parseObject(responseMeta.body.substring(startIndex,endIndex));
    }

    public JSONArray jsonpAsJSONArray() throws IOException {
        body();
        int startIndex = responseMeta.body.indexOf("(")+1,endIndex = responseMeta.body.lastIndexOf(")");
        return JSON.parseArray(responseMeta.body.substring(startIndex,endIndex));
    }

    @Override
    public byte[] bodyAsBytes() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = new byte[QuickHttpConfig.BUFFER_SIZE];
        int length = 0;
        int retryTimes = QuickHttpConfig.retryTimes;
        for(int i=0;i<retryTimes;i++){
            try {
                while((length=responseMeta.bufferedInputStream.read(bytes,0,bytes.length))!=-1){
                    baos.write(bytes,0,length);
                }
                break;
            }catch (SocketTimeoutException e){
                logger.warn("[读取超时]重试{}/{},原因:{},链接:{}",i,retryTimes,e.getMessage(),url());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }
        }
        baos.flush();
        bytes = baos.toByteArray();
        baos.close();
        return bytes;
    }

    @Override
    public BufferedInputStream bodyStream() {
        return responseMeta.bufferedInputStream;
    }

    @Override
    public Document parse() throws IOException {
        if(responseMeta.document==null){
            if(responseMeta.body==null){
                body();
            }
            responseMeta.document = Document.parse(responseMeta.body);
        }
        return responseMeta.document;
    }

    @Override
    public DocumentParser parser() throws IOException {
        if(responseMeta.documentParser==null){
            if(responseMeta.body==null){
                body();
            }
            responseMeta.documentParser = DocumentParser.parse(responseMeta.body);
        }
        return responseMeta.documentParser;
    }

    @Override
    public void close() {
        try {
            responseMeta.bufferedInputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        responseMeta.httpURLConnection.disconnect();
    }

    @Override
    public ResponseMeta responseMeta() {
        return responseMeta;
    }

    private void getCharset() throws IOException {
        getCharsetFromContentType(responseMeta.contentType);
        if(responseMeta.charset==null){
            byte[] bytes = new byte[1024*5];
            responseMeta.bufferedInputStream.mark(bytes.length);
            responseMeta.bufferedInputStream.read(bytes,0,bytes.length);
            boolean readFully = (responseMeta.bufferedInputStream.read()==-1);
            responseMeta.bufferedInputStream.reset();
            ByteBuffer firstBytes = ByteBuffer.wrap(bytes);
            getCharsetFromBOM(firstBytes);
            if(responseMeta.charset==null){
                getCharsetFromMeta(firstBytes,readFully);
            }
        }
        if(responseMeta.charset==null){
            responseMeta.charset = "utf-8";
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
            if (responseMeta.charset == null && meta.hasAttr("charset")) {
                responseMeta.charset = meta.attr("charset");
            }
            break;
        }

        if(responseMeta.charset==null){
            Element root = doc.root();
            if(doc.root().tagName().equals("?xml")&&root.hasAttr("encoding")){
                responseMeta.charset = root.attr("encoding");
            }
        }
        if(readFully){
            responseMeta.document = doc;
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
            responseMeta.charset = "utf-32";
        } else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF ||
                bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            responseMeta.charset = "utf-16";
        } else if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            responseMeta.charset = "utf-8";
        }
        if(responseMeta.charset!=null){
            responseMeta.bufferedInputStream.skip(1);
        }
    }

    private void getCharsetFromContentType(String contentType){
        String prefix = "charset=";
        if(contentType!=null&&contentType.contains(prefix)){
            int startIndex = contentType.indexOf(prefix);
            if(startIndex>=0){
                int endIndex = contentType.lastIndexOf(";");
                if(endIndex>startIndex){
                    responseMeta.charset = contentType.substring(startIndex+prefix.length(),endIndex).trim();
                }else if(endIndex<startIndex){
                    responseMeta.charset = contentType.substring(startIndex+prefix.length()).trim();
                }
            }
        }
    }
}
