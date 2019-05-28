package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.AbstractConnection;
import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.util.Interceptor;
import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import cn.schoolwow.quickhttp.util.ValidateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuickHttp {
    private static Logger logger = LoggerFactory.getLogger(QuickHttp.class);
    private static CookieManager cookieManager = new CookieManager();
    static{
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        //打开限制头部
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }


    public static void intercept(Interceptor interceptor){
        QuickHttpConfig.interceptor = interceptor;
    }

    public static void addCookie(String cookie,String url){
        try {
            addCookie(cookie,new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void addCookie(String cookie,URL u){
        ValidateUtil.checkNotEmpty(cookie,"cookie不能为空!");
        String[] tokens = cookie.split(";");
        for(String token:tokens){
            int startIndex = token.indexOf("=");
            String name = token.substring(0,startIndex).trim();
            String value = token.substring(startIndex+1).trim();
            addCookie(name,value,u);
        }
    }

    public static void addCookie(String name, String value,String url){
        try {
            addCookie(name,value,new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void addCookie(String name, String value,URL u){
        ValidateUtil.checkNotNull(name,"name不能为空!");
        ValidateUtil.checkNotNull(u,"URL不能为空!");
        HttpCookie httpCookie = new HttpCookie(name,value);
        httpCookie.setMaxAge(3600000);
        httpCookie.setDomain(getTopHost(u.getHost()));
        httpCookie.setPath("/");
        httpCookie.setVersion(0);
        httpCookie.setDiscard(false);
        addCookie(httpCookie);
    }

    public static void addCookie(HttpCookie httpCookie){
        try {
            URI uri = new URI(httpCookie.getDomain());
            cookieManager.getCookieStore().add(uri,httpCookie);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    public static void addCookie(List<HttpCookie> httpCookieList){
        for(HttpCookie httpCookie:httpCookieList){
            addCookie(httpCookie);
        }
    }

    public static void addCookies(Map<String, String> cookies,String url){
        try {
            addCookies(cookies,new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public static void addCookies(Map<String, String> cookies,URL u){
        Set<String> keySet = cookies.keySet();
        for(String key:keySet){
            addCookie(key, cookies.get(key),u);
        }
    }

    public static HttpCookie getCookie(String url, String name){
        ValidateUtil.checkNotEmpty(name,"name不能为空!");
        List<HttpCookie> httpCookieList = getCookies(url);
        for(HttpCookie httpCookie:httpCookieList){
            if(httpCookie.getName().equals(name)){
                return httpCookie;
            }
        }
        return null;
    }

    public static List<HttpCookie> getCookies(String url){
        try {
            return getCookies(new URL(url));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<HttpCookie> getCookies(URL u){
        try {
            CookieManager cookieManager = ((CookieManager) CookieHandler.getDefault());
            return cookieManager.getCookieStore().get(u.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**设置全局代理*/
    public static void proxy(Proxy proxy) {
        ValidateUtil.checkNotNull(proxy,"代理对象不能为空!");
        QuickHttpConfig.proxy = proxy;
        logger.info("[设置全局代理]地址:{}",proxy.address());
    }

    /**设置全局代理*/
    public static void proxy(String host, int port) {
        ValidateUtil.checkNotEmpty(host,"代理地址不能为空!");
        ValidateUtil.checkArgument(port>0,"代理端口必须大于0!port:"+port);
        QuickHttpConfig.proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(host,port));
        logger.info("[设置全局代理]地址:{},端口:{}",host,port);
    }

    /**设置全局重试次数*/
    public static void retryTimes(int retryTimes) {
        ValidateUtil.checkArgument(retryTimes>0,"重试次数必须大于0!retryTimes:"+retryTimes);
        QuickHttpConfig.retryTimes = retryTimes;
        logger.info("[设置最大重试次数]最大重试次数:{}",retryTimes);
    }

    /**连接*/
    public static Connection connect(String url){
        return AbstractConnection.getConnection(url);
    }

    private static String getTopHost(String host){
        //设置成顶级域名
        int endIndex = host.lastIndexOf(".");
        int startIndex = endIndex-1;
        while(startIndex>0&&host.charAt(startIndex)!='.'){
            startIndex--;
        }
        host = host.substring(startIndex);
        return host;
    }
}
