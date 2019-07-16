package cn.schoolwow.quickhttp.util;

import java.io.File;
import java.net.Proxy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class QuickHttpConfig {
    /**缓冲区大小*/
    public static final int BUFFER_SIZE = 1024*32;
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
    /**开启Refer字段*/
    public static boolean refer = false;
    /**Cookie存放地址*/
    public static File cookiesFile = new File("cookies.txt");

    /**线程池配置*/
    public static int corePoolSize = Runtime.getRuntime().availableProcessors();
    public static int maximumPoolSize = corePoolSize*5;
    public static BlockingQueue blockingQueue = new LinkedBlockingQueue();
}
