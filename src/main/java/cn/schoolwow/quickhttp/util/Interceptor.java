package cn.schoolwow.quickhttp.util;

import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.response.Response;

public interface Interceptor {
    /**
     * 在http请求发送之前
     * @param connection http 连接对象
     * */
    void beforeConnect(Connection connection);

    /**
     * 在http请求发送完毕之后
     * @param connection 连接对象
     * @param response 返回对象
     * */
    void afterConnection(Connection connection,Response response);
}
