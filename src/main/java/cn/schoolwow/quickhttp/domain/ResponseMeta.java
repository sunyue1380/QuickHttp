package cn.schoolwow.quickhttp.domain;

import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.DocumentParser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

/**返回元数据*/
public class ResponseMeta {
    /**关联httpurlConnection*/
    public HttpURLConnection httpURLConnection;
    /**顶级域*/
    public String topHost;
    /**状态码*/
    public int statusCode;
    /**消息*/
    public String statusMessage;
    /**编码格式*/
    public String charset;
    /**内容类型*/
    public String contentType;
    /**文件信息*/
    public String contentDisposition;
    /**头部信息*/
    public Map<String,String> headerMap = new HashMap<>();
    /**输入流*/
    public InputStream inputStream;
    /**输入流字符串*/
    public String body;
    /**Document对象*/
    public Document document;
    /**Document对象*/
    public DocumentParser documentParser;
}
