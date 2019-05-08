package cn.schoolwow.quickhttp;

import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.List;

public class QuickHttpTest {
    private Logger logger = LoggerFactory.getLogger(QuickHttp.class);
    @Test
    public void testCookieHandler() throws Exception {
        //TODO 开始添加测试用例并测试

        CookieManager cookieManager = new CookieManager();
        cookieManager.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
        CookieHandler.setDefault(cookieManager);
        CookieHandler.getDefault();
        URI uri = new URI("http://localhost/cookie");
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("POST");
        connection.connect();
        connection.setRequestProperty("","");
        connection.addRequestProperty("","");
        connection.setInstanceFollowRedirects(true);
        System.out.println(connection.getResponseCode());
        System.out.println(connection.getResponseMessage());
        List<HttpCookie> httpCookies = cookieManager.getCookieStore().getCookies();
        for(HttpCookie httpCookie:httpCookies){
            System.out.println(JSON.toJSONString(httpCookie));
        }
        logger.info("[CookieHandler]获取Cookie:{}", JSON.toJSONString(httpCookies));
        connection.disconnect();

        //再次访问应该带上Cookie
        connection = (HttpURLConnection) uri.toURL().openConnection();
        connection.connect();
        System.out.println(connection.getResponseCode());
        System.out.println(connection.getResponseMessage());
        String result = connection.getHeaderField("result");
        System.out.println("result:"+result);
        Assert.assertNotNull("会话维持失败!",result);
        Assert.assertTrue("会话维持失败!",result.equals("true"));
    }
}
