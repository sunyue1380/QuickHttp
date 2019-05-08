package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.AbstractConnection;
import cn.schoolwow.quickhttp.connection.Connection;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;

public class QuickHttp {
    static{
        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        //打开限制头部
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    public static Connection connect(String url){
        return AbstractConnection.getConnection(url);
    }
}
