package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.response.Response;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

public class QuickHttpTest {
    Logger logger = LoggerFactory.getLogger(QuickHttpTest.class);

    @Test
    public void testQuickHttp() throws IOException {
        Response response = QuickHttp.connect("http://127.0.0.1")
                .method(Connection.Method.POST)
                .data("key","value")
                .data("file", Paths.get("pom.xml"))
                .referrer("http://www.baidu.com")
                .userAgent(Connection.UserAgent.ANDROID)
                .ajax()
                .retryTimes(3)
                .timeout(10000)
                .ignoreHttpErrors(false)
                .charset("utf-8")
                .execute();
        System.out.println(response.statusCode());
    }

    @Test
    public void testQuickHttp2() throws IOException {
        QuickHttp.proxy("127.0.0.1",8888);
        Response response = QuickHttp.connect("http://127.0.0.1")
                .method(Connection.Method.POST)
                .referrer("http://www.baidu.com")
                .userAgent(Connection.UserAgent.ANDROID)
                .ajax()
                .retryTimes(3)
                .timeout(10000)
                .ignoreHttpErrors(false)
                .charset("utf-8")
                .requestBody(Paths.get("pom.xml"))
                .execute();
        System.out.println(response.statusCode());
    }

}
