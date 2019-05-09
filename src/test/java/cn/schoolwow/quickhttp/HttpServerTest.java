package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.handler.RegisterHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;

public class HttpServerTest {
    @Test
    public void testHttpServer() throws IOException, InterruptedException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8899), 0);
        server.createContext("/system/register", new RegisterHandler());
        server.start();
        System.out.println("服务启动:http://127.0.0.1:8899/system/register");
        Thread.sleep(100000000l);
    }
}
