package cn.schoolwow.quickhttp.handler;

import cn.schoolwow.quickhttp.util.ValidateUtil;
import com.alibaba.fastjson.JSONObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

public class RegisterHandler implements HttpHandler {
    private Logger logger = LoggerFactory.getLogger(RegisterHandler.class);
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            String method = httpExchange.getRequestMethod().toUpperCase();
            logger.info("[请求方法]method:{}",method);
            switch (method){
                case "GET":{
                    httpExchange.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");
                    JSONObject o = new JSONObject();
                    o.put("quickhttp","123456");
                    httpExchange.sendResponseHeaders(200,o.toJSONString().length());
                    httpExchange.getResponseBody().write(o.toJSONString().getBytes());
                }break;
                case "PUT":{
                    JSONObject o = new JSONObject();
                    o.put("quickhttp","123456");
                    String content = ("jsoupcallback20190509("+o.toJSONString()+");");
                    httpExchange.sendResponseHeaders(200,content.length());
                    httpExchange.getResponseBody().write(content.getBytes());
                }break;
                case "POST":{
                    httpExchange.sendResponseHeaders(200,0);
                    StringWriter sw = new StringWriter();
                    IOUtils.copy(httpExchange.getRequestBody(),sw,"utf-8");
                    logger.debug("[输入参数]{}",sw.toString());
                    ValidateUtil.checkArgument("password=123456&username=quickhttp".equals(sw.toString()),"输入参数异常");

                    String token = httpExchange.getRequestHeaders().getFirst("token");
                    logger.debug("[头部]token:{}",token);
                    ValidateUtil.checkArgument("quickhttp".equals(token),"token异常!");
                    httpExchange.getResponseHeaders().set("Set-Cookie","quickhttp=123456;");
                    logger.debug("[添加返回头部]Set-Cookie:quickhttp=123456;");
                    httpExchange.getResponseBody().write("true".getBytes("utf-8"));
                }break;
            }
        }catch (IllegalArgumentException e){
            httpExchange.getResponseBody().write("false".getBytes("utf-8"));
        }
        logger.info("[请求执行完毕]");
        httpExchange.getResponseBody().flush();
        httpExchange.getResponseBody().close();
    }
}
