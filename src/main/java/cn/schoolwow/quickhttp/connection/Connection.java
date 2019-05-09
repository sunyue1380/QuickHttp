package cn.schoolwow.quickhttp.connection;

import cn.schoolwow.quickhttp.response.Response;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.net.ssl.SSLSocketFactory;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.Map;

public interface Connection {
    enum UserAgent {
        CHROME("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36")
        ,ANDROID("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Mobile Safari/537.36")
        ,MAC("User-Agent, Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");

        public final String userAgent;
        UserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }

    enum Method {
        GET(false), POST(true), PUT(true), DELETE(false), PATCH(true), HEAD(false), OPTIONS(false), TRACE(false);

        private final boolean hasBody;

        Method(boolean hasBody) {
            this.hasBody = hasBody;
        }

        public final boolean hasBody() {
            return hasBody;
        }
    }

    Connection url(URL url);

    Connection url(String url);

    Connection proxy(Proxy proxy);

    Connection proxy(String host, int port);

    Connection userAgent(String userAgent);

    Connection userAgent(UserAgent userAgent);

    Connection referrer(String referrer);

    Connection timeout(int millis);

    Connection followRedirects(boolean followRedirects);

    Connection method(Method method);

    Connection ignoreHttpErrors(boolean ignoreHttpErrors);

    Connection sslSocketFactory(SSLSocketFactory sslSocketFactory);

    Connection data(String key, String value);

    Connection data(String key, File file);

    Connection data(Map<String, String> data);

    Connection requestBody(String body);

    Connection requestBody(JSONObject body);

    Connection requestBody(JSONArray array);

    Connection header(String name, String value);

    Connection headers(Map<String,String> headers);

    Connection cookie(String name, String value);

    Connection cookies(Map<String, String> cookies);

    Connection charset(String charset);

    Connection retryTimes(int retryTimes);

    Response execute() throws IOException;
}
