package cn.schoolwow.quickhttp.response;

import cn.schoolwow.quickhttp.document.Document;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.HttpCookie;
import java.util.List;
import java.util.Map;

public interface Response {
    /**获取状态码*/
    int statusCode();

    /**获取消息*/
    String statusMessage();

    /**获取编码格式*/
    String charset();

    /**获取返回格式类型*/
    String contentType();

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

    String body();

    JSONObject bodyAsJSONObject();

    JSONArray bodyAsJSONArray();

    JSONObject jsonpAsJSONObject();

    JSONArray jsonpAsJSONArray();

    byte[] bodyAsBytes();

    BufferedInputStream bodyStream();

    Document parse() throws IOException;

    void close();

    interface CallBack{
        void onResponse(Response response);

        void onError(IOException e);
    }
}
