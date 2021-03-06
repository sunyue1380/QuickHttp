package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.AbstractConnection;
import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.util.Interceptor;
import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import cn.schoolwow.quickhttp.util.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuickHttp {
    private static Logger logger = LoggerFactory.getLogger(QuickHttp.class);
    public static CookieManager cookieManager = new CookieManager();

    static{
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        //打开限制头部
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        //禁止httpUrlConnection自动重试
        System.setProperty("sun.net.http.retryPost", "false");
    }

    /**
     * 不要自动设置Cookie
     * */
    public static void noCookie(){
        CookieHandler.setDefault(null);
    }

    /**
     * 恢复Cookie策略
     * */
    public static void restoreCookie(){
        CookieHandler.setDefault(cookieManager);
    }

    /**
     * 设置Cookie策略
     * @param cookiePolicy cookie策略
     * */
    public static void setCookiePolicy(CookiePolicy cookiePolicy){
        cookieManager.setCookiePolicy(cookiePolicy);
    }

    /**
     * 设置代理选择器
     * @param proxySelector 代理选择器
     * */
    public static void setProxySelector(ProxySelector proxySelector){
        ProxySelector.setDefault(proxySelector);
    }

    /**
     * 拦截器
     * @param interceptor 拦截器实现类
     * */
    public static void intercept(Interceptor interceptor){
        QuickHttpConfig.interceptorList.add(interceptor);
    }

    /**
     * 返回拦截器列表
     * */
    public static List<Interceptor> intercept(){
        return QuickHttpConfig.interceptorList;
    }

    /**
     * 获取Cookie
     * @param domain 域名
     * @param name Cookie名称
     * */
    public static HttpCookie getCookie(String domain, String name){
        ValidateUtil.checkNotEmpty(name,"name不能为空!");
        List<HttpCookie> httpCookieList = getCookies(domain);
        for(HttpCookie httpCookie:httpCookieList){
            if(httpCookie.getName().equals(name)){
                return httpCookie;
            }
        }
        return null;
    }

    /**
     * 获取域名下的所有Cookie
     * @param domain 域名
     * */
    public static List<HttpCookie> getCookies(String domain){
        ValidateUtil.checkNotEmpty(domain,"域名不能为空!");
        List<HttpCookie> httpCookieList = new ArrayList<>();
        List<HttpCookie> httpCookieListStore = cookieManager.getCookieStore().getCookies();
        for(HttpCookie httpCookie:httpCookieListStore){
            if(httpCookie.getDomain().contains(domain)){
                httpCookieList.add(httpCookie);
            }
        }
        return httpCookieList;
    }

    /**
     * 获取域名下Cookie头部
     * @param domain 域名
     * */
    public static String getCookieString(String domain){
        List<HttpCookie> httpCookieList = getCookies(domain);
        StringBuilder builder = new StringBuilder();
        for(HttpCookie httpCookie:httpCookieList){
            builder.append(httpCookie.getName()+"="+httpCookie.getValue()+";");
        }
        return builder.toString();
    }

    /**
     * 获取域名下的所有Cookie
     * */
    public static List<HttpCookie> getCookies(){
        return cookieManager.getCookieStore().getCookies();
    }

    /**
     * 添加Cookie
     * @param cookie Cookie字段
     * @param domain 域名
     * */
    public static void addCookie(String cookie,String domain){
        if(null==cookie||cookie.isEmpty()){
            return;
        }
        String[] tokens = cookie.split(";");
        for(String token:tokens){
            int startIndex = token.indexOf("=");
            String name = token.substring(0,startIndex).trim();
            String value = token.substring(startIndex+1).trim();
            addCookie(name,value,domain);
        }
    }

    /**
     * 添加Cookie
     * @param name cookie键
     * @param value cookie值
     * @param domain 域名
     * */
    public static void addCookie(String name, String value,String domain){
        ValidateUtil.checkNotNull(name,"name不能为空!");
        ValidateUtil.checkNotNull(domain,"域名不能为空!");
        HttpCookie httpCookie = new HttpCookie(name,value);
        httpCookie.setMaxAge(3600000);
        httpCookie.setDomain(domain);
        httpCookie.setPath("/");
        httpCookie.setVersion(0);
        httpCookie.setDiscard(false);
        addCookie(httpCookie);
    }

    /**
     * 添加Cookie
     * @param httpCookie Cookie对象
     * */
    public static void addCookie(HttpCookie httpCookie){
        ValidateUtil.checkNotEmpty(httpCookie.getDomain(),"域名不能为空!");
        if(!httpCookie.getDomain().startsWith(".")){
            httpCookie.setDomain("."+httpCookie.getDomain());
        }
        if(httpCookie.getMaxAge()<=0){
            httpCookie.setMaxAge(3600);
        }
        try {
            cookieManager.getCookieStore().add(new URI(httpCookie.getDomain()),httpCookie);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    /**
     * 添加Cookie
     * @param httpCookieList Cookie列表
     * */
    public static void addCookie(List<HttpCookie> httpCookieList){
        for(HttpCookie httpCookie:httpCookieList){
            addCookie(httpCookie);
        }
    }

    /**
     * 添加Cookie
     * @param cookies Cookie键值对
     * @param domain 域名
     * */
    public static void addCookies(Map<String, String> cookies,String domain){
        Set<String> keySet = cookies.keySet();
        for(String key:keySet){
            addCookie(key,cookies.get(key),domain);
        }
    }

    /**
     * 删除域名下所有Cookie
     * @param domain 域名
     * */
    public static void removeCookie(String domain){
        List<HttpCookie> httpCookieList = getCookies(domain);
        if(domain.startsWith(".")){
            domain = "."+domain;
        }
        URI uri = null;
        try {
            uri = new URI(domain);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        for(HttpCookie httpCookie:httpCookieList){
            cookieManager.getCookieStore().remove(uri,httpCookie);
        }
    }

    /**
     * 删除指定域名下的Cookie
     * @param domain 域名
     * @param name Cookie名称
     * */
    public static void removeCookie(String domain, String name){
        List<HttpCookie> httpCookieList = getCookies(domain);
        if(domain.startsWith(".")){
            domain = "."+domain;
        }
        URI uri = null;
        try {
            uri = new URI(domain);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        for(HttpCookie httpCookie:httpCookieList){
            if(httpCookie.getName().equals(name)){
                cookieManager.getCookieStore().remove(uri,httpCookie);
            }
        }
    }

    /**
     * 删除指定Cookie
     * @param httpCookie 要删除的httpCookie对象
     * */
    public static void removeCookie(HttpCookie httpCookie){
        URI uri = null;
        try {
            uri = new URI(httpCookie.getDomain());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        cookieManager.getCookieStore().remove(uri,httpCookie);
    }

    /**
     * 删除所有Cookie
     * */
    public static boolean removeAllCookie(){
        return cookieManager.getCookieStore().removeAll();
    }

    /**
     * 设置全局代理
     * @param origin http协议origin
     * */
    public static void origin(String origin) {
        ValidateUtil.checkNotEmpty(origin,"全局origin不能为空!");
        if(!origin.startsWith("http")){
            throw new IllegalArgumentException("origin必须以http开头!");
        }
        QuickHttpConfig.origin = origin;
    }

    /**
     * 设置全局代理
     * @param proxy 代理对象
     * */
    public static void proxy(Proxy proxy) {
        ValidateUtil.checkNotNull(proxy,"代理对象不能为空!");
        QuickHttpConfig.proxy = proxy;
    }

    /**
     * 设置全局代理
     * @param host 代理主机
     * @param port 代理主机端口
     * */
    public static void proxy(String host, int port) {
        ValidateUtil.checkNotEmpty(host,"代理地址不能为空!");
        ValidateUtil.checkArgument(port>0,"代理端口必须大于0!port:"+port);
        QuickHttpConfig.proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(host,port));
    }

    /**
     * 设置全局重试次数
     * @param retryTimes 重试次数
     * */
    public static void retryTimes(int retryTimes) {
        ValidateUtil.checkArgument(retryTimes>0,"重试次数必须大于0!retryTimes:"+retryTimes);
        QuickHttpConfig.retryTimes = retryTimes;
    }

    /**
     * 设置全局最大超时时间
     * @param maxTimeout 最大超时时间(毫秒)
     * */
    public static void maxTimeout(int maxTimeout) {
        ValidateUtil.checkArgument(maxTimeout>0,"最大超时时间必须大于0!maxTimeout:"+maxTimeout);
        QuickHttpConfig.maxTimeout = maxTimeout;
    }

    /**
     * 访问url
     * @param url 地址
     * */
    public static Connection connect(String url){
        return AbstractConnection.getConnection(url);
    }

    /**
     * 访问url
     * @param url 地址
     * */
    public static Connection connect(URL url){
        return AbstractConnection.getConnection(url);
    }
}
