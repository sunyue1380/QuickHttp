package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.response.Response;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Paths;

public class QuickHttpTest {
    Logger logger = LoggerFactory.getLogger(QuickHttpTest.class);
    @BeforeClass
    public static void beforeClass(){
        QuickHttp.proxy("127.0.0.1",8888);
    }

    @Test
    public void testMultipart() throws IOException {
        Response response = QuickHttp.connect("http://127.0.0.1/?param1=1")
                .method(Connection.Method.POST)
                .charset("utf-8")
                .parameter("param2","2")
                .data("key","value")
                .data("file", Paths.get("pom.xml"))
                .referrer("http://127.0.0.1")
                .basicAuth("quickhttp","123456")
                .userAgent(Connection.UserAgent.ANDROID)
                .ajax()
                .retryTimes(3)
                .timeout(10000)
                .ignoreHttpErrors(true)
                .execute();
        Assert.assertEquals(response.statusCode(),403);
    }

    @Test
    public void testRequestBody() throws IOException {
        Response response = QuickHttp.connect("http://127.0.0.1/")
                .method(Connection.Method.POST)
                .charset("utf-8")
                .parameter("param","1")
                .data("key","value")
                .requestBody("this is some text")
                .contentType("text/xml")
                .referrer("http://127.0.0.1")
                .basicAuth("quickhttp","123456")
                .userAgent(Connection.UserAgent.ANDROID)
                .ajax()
                .retryTimes(3)
                .timeout(10000)
                .ignoreHttpErrors(true)
                .execute();
        Assert.assertEquals(response.statusCode(),403);
    }

    @Test
    public void testForm() throws IOException {
        Response response = QuickHttp.connect("http://127.0.0.1/")
                .method(Connection.Method.POST)
                .charset("utf-8")
                .parameter("param","1")
                .data("key","value")
                .referrer("http://127.0.0.1")
                .basicAuth("quickhttp","123456")
                .userAgent(Connection.UserAgent.ANDROID)
                .ajax()
                .retryTimes(3)
                .timeout(10000)
                .ignoreHttpErrors(true)
                .execute();
        Assert.assertEquals(response.statusCode(),403);
    }
}
