package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.AbstractConnection;
import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.util.Interceptor;
import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import cn.schoolwow.quickhttp.util.ValidateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.*;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class QuickHttp {
    private static Logger logger = LoggerFactory.getLogger(QuickHttp.class);
    public static CookieManager cookieManager = new CookieManager();
    static{
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        //打开限制头部
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");

        //判断Cookie文件是否存在,若存在则加载
        File file = QuickHttpConfig.cookiesFile;
        if(null!=file&&file.exists()){
            try {
                StringBuilder sb = new StringBuilder();
                Scanner scanner = new Scanner(file);
                while(scanner.hasNext()){
                    sb.append(scanner.nextLine());
                }
                JSONArray array = JSON.parseArray(sb.toString());
                for(int i=0;i<array.size();i++){
                    JSONObject o = array.getJSONObject(i);
                    HttpCookie httpCookie = new HttpCookie(o.getString("name"),o.getString("value"));
                    httpCookie.setDomain(o.getString("domain"));
                    httpCookie.setMaxAge(o.getLong("maxAge"));
                    //判断是否过期
                    if(file.lastModified()+(httpCookie.getMaxAge()*1000)<=System.currentTimeMillis()){
                        logger.trace("[过期cookie]name:{},value:{},domain:{}",httpCookie.getName(),httpCookie.getValue(),httpCookie.getDomain());
                        continue;
                    }
                    httpCookie.setPath(o.getString("path"));
                    httpCookie.setSecure(o.getBoolean("secure"));
                    httpCookie.setHttpOnly(o.getBoolean("httpOnly"));
                    httpCookie.setDiscard(o.getBoolean("discard"));
                    httpCookie.setComment(o.getString("comment"));
                    httpCookie.setCommentURL(o.getString("commentURL"));
                    httpCookie.setVersion(0);
                    QuickHttp.addCookie(httpCookie);
                }
                logger.info("[载入cookie文件]载入cookie个数:{}",array.size());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
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
     * 拦截器
     * @param interceptor 拦截器实现类
     * */
    public static void intercept(Interceptor interceptor){
        QuickHttpConfig.interceptor = interceptor;
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
        if(!domain.startsWith(".")){
            domain = "."+domain;
        }
        try {
            return cookieManager.getCookieStore().get(new URI(domain));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
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
     * 删除所有Cookie
     * */
    public static boolean removeAllCookie(){
        return cookieManager.getCookieStore().removeAll();
    }

    /**
     * 设置全局代理
     * @param proxy 代理对象
     * */
    public static void proxy(Proxy proxy) {
        ValidateUtil.checkNotNull(proxy,"代理对象不能为空!");
        QuickHttpConfig.proxy = proxy;
        logger.info("[设置全局代理]地址:{}",proxy.address());
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
        logger.info("[设置全局代理]地址:{},端口:{}",host,port);
    }

    /**
     * 设置全局重试次数
     * @param retryTimes 重试次数
     * */
    public static void retryTimes(int retryTimes) {
        ValidateUtil.checkArgument(retryTimes>0,"重试次数必须大于0!retryTimes:"+retryTimes);
        QuickHttpConfig.retryTimes = retryTimes;
        logger.info("[设置最大重试次数]最大重试次数:{}",retryTimes);
    }

    /**
     * 设置全局最大超时时间
     * @param maxTimeout 最大超时时间(毫秒)
     * */
    public static void maxTimeout(int maxTimeout) {
        ValidateUtil.checkArgument(maxTimeout>0,"最大超时时间必须大于0!retryTimes:"+maxTimeout);
        QuickHttpConfig.maxTimeout = maxTimeout;
        logger.info("[设置最大超时时间]最大超时时间:{}",maxTimeout);
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
