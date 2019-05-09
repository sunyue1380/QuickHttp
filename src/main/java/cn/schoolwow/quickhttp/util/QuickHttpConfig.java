package cn.schoolwow.quickhttp.util;

import java.net.Proxy;

public class QuickHttpConfig {
    /**缓冲区大小*/
    public static final int BUFFER_SIZE = 1024*32;
    /**全局代理*/
    public static Proxy proxy;
    /**默认重试次数*/
    public static int retryTimes = 3;
}
