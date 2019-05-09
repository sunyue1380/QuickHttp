package cn.schoolwow.quickhttp.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class FileUploadHander implements HttpHandler {
    private Logger logger = LoggerFactory.getLogger(FileUploadHander.class);
    @Override
    public void handle(HttpExchange httpExchange) throws IOException {
        try {
            httpExchange.sendResponseHeaders(200,0);
            File file = new File("response.txt");
            FileUtils.copyInputStreamToFile(httpExchange.getRequestBody(),file);
            httpExchange.getResponseBody().write("true".getBytes("utf-8"));
        }catch (IllegalArgumentException e){
            httpExchange.getResponseBody().write("false".getBytes("utf-8"));
        }
        logger.info("[请求执行完毕]");
        httpExchange.getResponseBody().flush();
        httpExchange.getResponseBody().close();
    }
}
