package cn.schoolwow.quickhttp.domain;

/**返回元数据*/
public class ResponseMeta {
    /**内容类型*/
    public String contentType;
    /**返回大小*/
    public long contentLength = -1;
    /**内容编码*/
    public String contentEncoding;
    /**文件信息*/
    public String contentDisposition;
}
