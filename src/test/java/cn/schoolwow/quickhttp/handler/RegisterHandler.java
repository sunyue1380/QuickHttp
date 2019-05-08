package cn.schoolwow.quickhttp.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

public class RegisterHandler implements HttpHandler {
    private Logger logger = LoggerFactory.getLogger(RegisterHandler.class);
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            StringWriter sw = new StringWriter();
            IOUtils.copy(httpExchange.getRequestBody(),sw,"utf-8");
            Assert.assertTrue("输入参数异常!"+sw.toString(),"password=123456&username=quickhttp".equals(sw.toString()));
            String token = httpExchange.getRequestHeaders().getFirst("token");
            Assert.assertTrue("token异常!","quickhttp".equals(token));
            httpExchange.getResponseHeaders().add("result","true");
        }catch (Error e){
            httpExchange.getResponseHeaders().add("result",e.getMessage());
        }
        httpExchange.sendResponseHeaders(200,0);
        httpExchange.getResponseBody().flush();
        httpExchange.getResponseBody().close();
    }
}
