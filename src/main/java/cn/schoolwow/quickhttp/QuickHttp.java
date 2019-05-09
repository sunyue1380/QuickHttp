package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.AbstractConnection;
import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.util.QuickHttpConfig;
import cn.schoolwow.quickhttp.util.ValidateUtil;

import java.net.*;

public class QuickHttp {
    static{
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        //打开限制头部
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    /**设置全局代理*/
    public void proxy(Proxy proxy) {
        ValidateUtil.checkNotNull(proxy,"代理对象不能为空!");
        QuickHttpConfig.proxy = proxy;
    }

    /**设置全局代理*/
    public void proxy(String host, int port) {
        ValidateUtil.checkNotEmpty(host,"代理地址不能为空!");
        ValidateUtil.checkArgument(port>0,"代理端口必须大于0!port:"+port);
        QuickHttpConfig.proxy = new Proxy(Proxy.Type.HTTP,new InetSocketAddress(host,port));
    }

    /**设置全局重试次数*/
    public void retryTimes(int retryTimes) {
        ValidateUtil.checkArgument(retryTimes>0,"重试次数必须大于0!retryTimes:"+retryTimes);
        QuickHttpConfig.retryTimes = retryTimes;
    }

    /**连接*/
    public static Connection connect(String url){
        return AbstractConnection.getConnection(url);
    }
}
