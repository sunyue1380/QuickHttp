package cn.schoolwow.quickhttp.util;

import cn.schoolwow.quickhttp.QuickHttp;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.Proxy;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QuickHttpConfig {
    private static Logger logger = LoggerFactory.getLogger(QuickHttpConfig.class);
    /**缓冲区大小*/
    public static final int BUFFER_SIZE = 1024*8;
    /**全局代理*/
    public static Proxy proxy;
    /**默认重试次数*/
    public static int retryTimes = 3;
    /**最大超时时间(毫秒)*/
    public static int maxTimeout = 300000;
    /**默认最大重定向次数*/
    public static int maxRedirectTimes = 10;
    /**过滤器*/
    public static Interceptor interceptor;
    /**线程池配置*/
    public static int corePoolSize = Runtime.getRuntime().availableProcessors();
    public static int maximumPoolSize = corePoolSize*5;
    public static BlockingQueue blockingQueue = new LinkedBlockingQueue();

}
