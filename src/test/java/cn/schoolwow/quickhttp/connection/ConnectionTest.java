package cn.schoolwow.quickhttp.connection;

import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.response.Response;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class ConnectionTest {
    private Logger logger = LoggerFactory.getLogger(Connection.class);
    int port = 8899;

    @Test
    public void testFileUpload() throws IOException {
        Response response = QuickHttp.connect("http://127.0.0.1:"+port+"/system/fileUpload")
                .method(Connection.Method.POST)
                .proxy("127.0.0.1",8888)
                .userAgent(Connection.UserAgent.CHROME)
                .data("pom.xml",new File("pom.xml"))
                .data("username","quickhttp")
                .data("password","123456")
                .ignoreHttpErrors(false)
                .execute();
        Assert.assertTrue(response.statusCode()==200);
        Assert.assertTrue("true".equals(response.body()));
    }

    @Test
    public void testRequestBody() throws IOException {
        JSONObject o = new JSONObject();
        o.put("username","quickhttp");
        o.put("password","123456");

        Response response = QuickHttp.connect("http://127.0.0.1:"+port+"/system/login")
                .method(Connection.Method.POST)
                .proxy("127.0.0.1",8888)
                .userAgent(Connection.UserAgent.CHROME)
                .requestBody(o)
                .ignoreHttpErrors(false)
                .execute();
        Assert.assertTrue(response.statusCode()==200);
        Assert.assertTrue("true".equals(response.body()));
    }

    @Test
    public void testRequest() throws IOException {
        Response response = QuickHttp.connect("http://127.0.0.1:"+port+"/system/register")
                .method(Connection.Method.POST)
                .proxy("127.0.0.1",8888)
                .userAgent(Connection.UserAgent.CHROME)
                .referrer("http://127.0.0.1")
                .timeout(3000)
                .ignoreHttpErrors(true)
                .data("username","quickhttp")
                .data("password","123456")
                .header("token","quickhttp")
                .charset("utf-8")
                .retryTimes(10)
                .execute();
        logger.info("[状态码和消息]statusCode:{},statusMessage:{}",response.statusCode(),response.statusMessage());
        logger.info("[编码格式]charset:{}",response.charset());
        logger.info("[返回格式类型]content-type:{}",response.contentType());
        logger.info("[hasHeader]hasHeader(result):{}",response.header("result"));
        logger.info("[hasHeaderWithValue]hasHeaderWithValue(result,true):{}",response.hasHeaderWithValue("result","true"));
        logger.info("[header]header(result):{}",response.header("result"));
        logger.info("[headers]headers:{}",response.headers());
        logger.info("[hasCookie]hasCookie(quickhttp):{}",response.hasCookie("quickhttp"));
        logger.info("[hasCookieWithValue]hasCookieWithValue(quickhttp,123456):{}",response.hasCookieWithValue("quickhttp","123456"));
        logger.info("[cookie]cookie(quickhttp):{}",response.cookie("quickhttp"));
        logger.info("[cookieList]cookieList():{}", JSON.toJSONString(response.cookieList()));
        logger.info("[body]body:{}", response.body());
        Assert.assertTrue(200==response.statusCode());
        Assert.assertTrue("true".equals(response.body()));
    }

    @Test
    public void testJSONObject() throws IOException {
        Response response = QuickHttp.connect("http://127.0.0.1:"+port+"/system/register")
                .method(Connection.Method.GET)
                .proxy("127.0.0.1",8888)
                .userAgent(Connection.UserAgent.MAC)
                .timeout(3000)
                .retryTimes(10)
                .execute();
        logger.info("[body]body:{}", response.body());
        logger.info("[body]bodyAsJSON:{}", response.bodyAsJSONObject().toJSONString());
    }

    @Test
    public void testJsonp() throws IOException {
        Response response = QuickHttp.connect("http://127.0.0.1:"+port+"/system/register")
                .method(Connection.Method.PUT)
                .proxy("127.0.0.1",8888)
                .userAgent(Connection.UserAgent.MAC)
                .timeout(3000)
                .retryTimes(10)
                .execute();
        logger.info("[body]body:{}", response.body());
        logger.info("[body]bodyAsJSON:{}", response.jsonpAsJSONObject().toJSONString());
    }
}