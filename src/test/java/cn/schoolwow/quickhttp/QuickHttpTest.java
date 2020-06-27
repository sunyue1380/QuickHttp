package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.domain.RequestMeta;
import cn.schoolwow.quickhttp.response.Response;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class QuickHttpTest {
    Logger logger = LoggerFactory.getLogger(QuickHttpTest.class);

    @Test
    public void testClone() throws IOException {
        Connection connection = QuickHttp.connect("https://www.baidu.com")
                .userAgent(Connection.UserAgent.ANDROID)
                .followRedirects(true)
                .timeout(3000)
                .retryTimes(3);
        RequestMeta requestMeta = connection.requestMeta();
        RequestMeta cloneRequestMeta = connection.clone().requestMeta();
        Assert.assertEquals(false,requestMeta==cloneRequestMeta);
        Assert.assertEquals(true,requestMeta.equals(cloneRequestMeta));
    }

    @Test
    public void testBaiDu() throws IOException {
        Response response = QuickHttp.connect("https://www.baidu.com")
                .execute();
        logger.info("url:{} , status:{} , message:{}",response.url(),response.statusCode(),response.statusMessage());
        Assert.assertEquals(200,response.statusCode());
    }

    @Test
    public void testUpload() throws IOException {
        Response response = QuickHttp.connect("http://1.w2wz.com/upload.php")
                .method(Connection.Method.POST)
                .data("MAX_FILE_SIZE","1048576")
                .data("uploadimg",new File("upload.jpg"))
                .execute();
        logger.info("status:{} , message:{}",response.statusCode(),response.statusMessage());
        logger.info("body:{}",response.body());
    }
}
