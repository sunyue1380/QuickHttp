package cn.schoolwow.quickhttp.connection;

import cn.schoolwow.quickhttp.domain.RequestMeta;
import cn.schoolwow.quickhttp.response.Response;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.net.HttpCookie;
import java.net.Proxy;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public interface Connection extends Cloneable{
    enum UserAgent {
        CHROME("Mozilla/5.0 (Windows NT 10.0; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36")
        ,ANDROID("Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Mobile Safari/537.36")
        ,MAC("User-Agent, Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_0) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/74.0.3729.131 Safari/537.36");

        public final String userAgent;
        UserAgent(String userAgent) {
            this.userAgent = userAgent;
        }
    }

    enum ContentType {
        MULTIPART_FORMDATA,
        APPLICATION_JSON,
        APPLICATION_X_WWW_FORM_URLENCODED;
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

    Connection contentType(String contentType);

    Connection contentType(ContentType contentType);

    Connection boundary(String boundary);

    Connection ajax();

    /**
     * 设置分段下载
     * @param start 开始字节
     * @param end 结束字节(0表示获取剩下所有字节)
     * */
    Connection ranges(long start, long end);

    Connection timeout(int millis);

    Connection followRedirects(boolean followRedirects);

    Connection method(String method);

    Connection method(Method method);

    Connection ignoreHttpErrors(boolean ignoreHttpErrors);

    Connection sslSocketFactory(SSLSocketFactory sslSocketFactory);

    Connection parameter(String key, String value);

    Connection data(String key, String value);

    Connection data(String key, Path file);

    Connection data(Map<String, String> data);

    Connection requestBody(String body);

    Connection requestBody(JSONObject body);

    Connection requestBody(JSONArray array);

    Connection requestBody(Path file) throws IOException;

    Connection header(String name, String value);

    Connection headers(Map<String,String> headers);

    Connection cookie(String name, String value);

    Connection cookie(HttpCookie httpCookie);

    Connection cookie(List<HttpCookie> httpCookieList);

    Connection cookies(Map<String, String> cookies);

    Connection basicAuth(String username, String password);

    Connection charset(String charset);

    Connection retryTimes(int retryTimes);

    Response execute() throws IOException;

    void enqueue(Response.CallBack callBack);

    RequestMeta requestMeta();

    Connection requestMeta(RequestMeta requestMeta);

    Connection clone();
}
