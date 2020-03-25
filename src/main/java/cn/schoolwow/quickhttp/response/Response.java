package cn.schoolwow.quickhttp.response;

import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.DocumentParser;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpCookie;
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

    String body() throws IOException;

    JSONObject bodyAsJSONObject() throws IOException;

    JSONArray bodyAsJSONArray() throws IOException;

    JSONObject jsonpAsJSONObject() throws IOException;

    JSONArray jsonpAsJSONArray() throws IOException;

    byte[] bodyAsBytes() throws IOException;

    BufferedInputStream bodyStream();

    Document parse() throws IOException;

    DocumentParser parser() throws IOException;

    void close();

    interface CallBack{
        void onResponse(Response response);

        void onError(Connection connection, IOException e);
    }
}
