package cn.schoolwow.quickhttp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class QuickHttpConfig {
    private static Logger logger = LoggerFactory.getLogger(QuickHttpConfig.class);
    /**全局代理*/
    public static Proxy proxy;
    /**默认全局重试次数*/
    public static int retryTimes = 3;
    /**最大超时时间(毫秒)*/
    public static int maxTimeout = 300000;
    /**默认最大重定向次数*/
    public static int maxRedirectTimes = 10;
    /**过滤器*/
    public static List<Interceptor> interceptorList = new ArrayList<>();
    /**线程池配置*/
    public static ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()*2);
}
