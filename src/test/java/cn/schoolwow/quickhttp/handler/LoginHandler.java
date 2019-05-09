package cn.schoolwow.quickhttp.handler;

import cn.schoolwow.quickhttp.util.ValidateUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

public class LoginHandler implements HttpHandler {
    private Logger logger = LoggerFactory.getLogger(LoginHandler.class);
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            httpExchange.sendResponseHeaders(200,0);
            StringWriter sw = new StringWriter();
            IOUtils.copy(httpExchange.getRequestBody(),sw,"utf-8");
            JSONObject o = JSON.parseObject(sw.toString());
            ValidateUtil.checkArgument("quickhttp".equals(o.getString("username")));
            ValidateUtil.checkArgument("123456".equals(o.getString("password")));
            httpExchange.getResponseBody().write("true".getBytes("utf-8"));
        }catch (IllegalArgumentException e){
            httpExchange.getResponseBody().write("false".getBytes("utf-8"));
        }
        logger.info("[请求执行完毕]");
        httpExchange.getResponseBody().flush();
        httpExchange.getResponseBody().close();
    }
}
