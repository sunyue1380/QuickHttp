package cn.schoolwow.quickhttp.domain;

import cn.schoolwow.quickhttp.connection.Connection;

import java.io.File;
import java.io.Serializable;
import java.net.HttpCookie;
import java.net.Proxy;
import java.net.URL;
import java.util.*;

/**请求元数据*/
public class RequestMeta implements Cloneable,Serializable{
    /**访问地址*/
    public URL url;
    /**请求方法*/
    public transient Connection.Method method = Connection.Method.GET;
    /**Http代理*/
    public transient Proxy proxy;
    /**Header信息*/
    public Map<String,String> headers = new HashMap<>();
    /**Data信息*/
    public Map<String,String> dataMap = new HashMap<>();
    /**DataFile信息*/
    public Map<String, File> dataFileMap = new IdentityHashMap<>();
    /**超时设置*/
    public int timeout = 10000;
    /**自动重定向*/
    public boolean followRedirects = true;
    /**是否忽略http状态异常*/
    public boolean ignoreHttpErrors = false;
    /**自定义请求体*/
    public String requestBody;
    /**请求编码*/
    public String charset = "utf-8";
    /**请求类型*/
    public String contentType;
    /**重试次数*/
    public int retryTimes = -1;
    /**重定向次数*/
    public int redirectTimes = 0;
    /**保存HttpCookie*/
    public transient List<HttpCookie> httpCookieList = new ArrayList<>();

    public RequestMeta(){
        headers.put("User-Agent", Connection.UserAgent.CHROME.userAgent);
        headers.put("Accept-Encoding", "gzip, deflate");
    }
}
