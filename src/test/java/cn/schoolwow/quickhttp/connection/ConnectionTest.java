package cn.schoolwow.quickhttp.connection;

import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.response.Response;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ConnectionTest {
    private Logger logger = LoggerFactory.getLogger(Connection.class);
    int port = 8899;
    @Test
    public void testRegisterSuccess() throws IOException {
        //TODO 注册成功
        Response response = QuickHttp.connect("http://127.0.0.1:"+port+"/system/register")
                .method(Connection.Method.POST)
                .data("username","quickhttp")
                .data("password","123456")
                .header("token","quickhttp")
//                .proxy("127.0.0.1",8888)
                .execute();
        logger.info("[返回result]result:{}",response.header("result"));
        Assert.assertTrue(response.statusCode()==200);
        Assert.assertTrue(response.header("result").equals("true"));
    }
}