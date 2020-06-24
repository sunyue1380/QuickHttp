package cn.schoolwow.quickhttp.util;

import cn.schoolwow.quickhttp.QuickHttp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.Proxy;
import java.net.URL;
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
    /**Cookie存放地址*/
    public static File cookiesFile;

    /**线程池配置*/
    public static int corePoolSize = Runtime.getRuntime().availableProcessors();
    public static int maximumPoolSize = corePoolSize*5;
    public static BlockingQueue blockingQueue = new LinkedBlockingQueue();

    static{
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        //获取真实路径
        URL url = null;
        try {
            url = Class.forName(stackTraceElements[3].getClassName()).getResource("");
            if("jar".equals(url.getProtocol())){
                url = QuickHttp.class.getProtectionDomain().getCodeSource().getLocation();
            }else if("file".equals(url.getProtocol())) {
                url = Thread.currentThread().getContextClassLoader().getResource("");
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if(null!=url){
            String path = url.getPath();
            if(path.startsWith("file:")){
                path = path.substring("file:".length());
            }
            if (System.getProperty("os.name").contains("dows")) {
                path = path.substring(1);
            }
            if (path.contains("jar")) {
                path = path.substring(0, path.lastIndexOf("."));
                path = path.substring(0, path.lastIndexOf("/"));
            }
            QuickHttpConfig.cookiesFile = new File(path+"/cookies.json");
            logger.debug("[cookie文件路径]{}",QuickHttpConfig.cookiesFile.getAbsolutePath());
        }else{
            logger.warn("[根目录获取为空]");
        }
    }
}
