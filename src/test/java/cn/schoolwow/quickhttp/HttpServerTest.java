package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.handler.FileUploadHander;
import cn.schoolwow.quickhttp.handler.LoginHandler;
import cn.schoolwow.quickhttp.handler.RegisterHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import org.junit.Test;

import javax.net.ssl.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.ThreadPoolExecutor;

public class HttpServerTest {
    @Test
    public void testHttpServer() throws IOException, InterruptedException, NoSuchProviderException, NoSuchAlgorithmException, KeyStoreException, CertificateException, UnrecoverableKeyException, KeyManagementException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8899), 0);
        server.createContext("/system/register", new RegisterHandler());
        server.createContext("/system/login", new LoginHandler());
        server.createContext("/system/fileUpload", new FileUploadHander());
        server.start();
        System.out.println("http服务启动:http://127.0.0.1:8899/system/register");
        Thread.sleep(100000000l);
    }
}
