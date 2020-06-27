package cn.schoolwow.quickhttp.domain;

import cn.schoolwow.quickhttp.connection.Connection;

import java.io.File;
import java.net.HttpCookie;
import java.net.Proxy;
import java.net.URL;
import java.util.*;

/**请求元数据*/
public class RequestMeta implements Cloneable{
    /**访问地址*/
    public URL url;
    /**请求方法*/
    public Connection.Method method = Connection.Method.GET;
    /**Http代理*/
    public Proxy proxy;
    /**Header信息*/
    public Map<String,String> headers = new HashMap<>();
    /**Data信息*/
    public Map<String,String> dataMap = new HashMap<>();
    /**DataFile信息*/
    public Map<String, File> dataFileMap = new IdentityHashMap<>();
    /**超时设置*/
    public int timeout = 3000;
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
    public List<HttpCookie> httpCookieList = new ArrayList<>();

    public RequestMeta(){
        headers.put("User-Agent", Connection.UserAgent.CHROME.userAgent);
        headers.put("Accept-Encoding", "gzip, deflate");
    }

    @Override
    public RequestMeta clone(){
        RequestMeta requestMeta = new RequestMeta();
        requestMeta.url = url;
        requestMeta.method = method;
        requestMeta.proxy = proxy;
        requestMeta.headers = headers;
        requestMeta.dataMap = dataMap;
        requestMeta.dataFileMap = dataFileMap;
        requestMeta.timeout = timeout;
        requestMeta.followRedirects = followRedirects;
        requestMeta.ignoreHttpErrors = ignoreHttpErrors;
        requestMeta.requestBody = requestBody;
        requestMeta.charset = charset;
        requestMeta.contentType = contentType;
        requestMeta.retryTimes = retryTimes;
        requestMeta.redirectTimes = redirectTimes;
        requestMeta.httpCookieList = httpCookieList;
        return requestMeta;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RequestMeta that = (RequestMeta) o;

        if (timeout != that.timeout) return false;
        if (followRedirects != that.followRedirects) return false;
        if (ignoreHttpErrors != that.ignoreHttpErrors) return false;
        if (retryTimes != that.retryTimes) return false;
        if (redirectTimes != that.redirectTimes) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (method != that.method) return false;
        if (proxy != null ? !proxy.equals(that.proxy) : that.proxy != null) return false;
        if (headers != null ? !headers.equals(that.headers) : that.headers != null) return false;
        if (dataMap != null ? !dataMap.equals(that.dataMap) : that.dataMap != null) return false;
        if (dataFileMap != null ? !dataFileMap.equals(that.dataFileMap) : that.dataFileMap != null) return false;
        if (requestBody != null ? !requestBody.equals(that.requestBody) : that.requestBody != null) return false;
        if (charset != null ? !charset.equals(that.charset) : that.charset != null) return false;
        if (contentType != null ? !contentType.equals(that.contentType) : that.contentType != null) return false;
        return httpCookieList != null ? httpCookieList.equals(that.httpCookieList) : that.httpCookieList == null;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (method != null ? method.hashCode() : 0);
        result = 31 * result + (proxy != null ? proxy.hashCode() : 0);
        result = 31 * result + (headers != null ? headers.hashCode() : 0);
        result = 31 * result + (dataMap != null ? dataMap.hashCode() : 0);
        result = 31 * result + (dataFileMap != null ? dataFileMap.hashCode() : 0);
        result = 31 * result + timeout;
        result = 31 * result + (followRedirects ? 1 : 0);
        result = 31 * result + (ignoreHttpErrors ? 1 : 0);
        result = 31 * result + (requestBody != null ? requestBody.hashCode() : 0);
        result = 31 * result + (charset != null ? charset.hashCode() : 0);
        result = 31 * result + (contentType != null ? contentType.hashCode() : 0);
        result = 31 * result + retryTimes;
        result = 31 * result + redirectTimes;
        result = 31 * result + (httpCookieList != null ? httpCookieList.hashCode() : 0);
        return result;
    }
}
