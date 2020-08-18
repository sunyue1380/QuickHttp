package cn.schoolwow.quickhttp.response;

import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.DocumentParser;
import cn.schoolwow.quickhttp.domain.ResponseMeta;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpCookie;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface Response {
    /**获取返回网址*/
    String url();

    /**获取状态码*/
    int statusCode();

    /**获取消息*/
    String statusMessage();

    /**获取编码格式*/
    String charset();

    /**获取返回格式类型*/
    String contentType();

    /**获取大小*/
    long contentLength();

    /**获取文件名*/
    String filename();

    /**是否支持分段下载*/
    boolean acceptRanges();

    /**是否有该Header*/
    boolean hasHeader(String name);

    /**是否存在该Header*/
    boolean hasHeaderWithValue(String name,String value);

    /**获取头部信息*/
    String header(String name);

    /**获取所有Header信息*/
    Map<String,String> headers();

    /**是否存在该Cookie*/
    boolean hasCookie(String name);

    /**是否存在该Cookie*/
    boolean hasCookieWithValue(String name,String value);

    /**获取Cookie信息*/
    HttpCookie cookie(String name);

    List<HttpCookie> cookieList();

    /**设置最大下载速率(kb/s)*/
    Response maxDownloadSpeed(int maxDownloadSpeed);

    String body() throws IOException;

    /**返回JSON对象*/
    JSONObject bodyAsJSONObject() throws IOException;

    /**返回JSON数组*/
    JSONArray bodyAsJSONArray() throws IOException;

    /**解析jsonp返回JSON对象*/
    JSONObject jsonpAsJSONObject() throws IOException;

    /**解析jsonp返回JSON数组*/
    JSONArray jsonpAsJSONArray() throws IOException;

    /**返回字节数组*/
    byte[] bodyAsBytes() throws IOException;

    /**写入到文件里*/
    void bodyAsFile(Path file) throws IOException;

    /**获取输入流*/
    InputStream bodyStream();

    Document parse() throws IOException;

    DocumentParser parser() throws IOException;

    void disconnect();

    /**获取返回元数据*/
    ResponseMeta responseMeta();

    interface CallBack{
        void onResponse(Response response);

        void onError(Connection connection, IOException e);
    }
}
