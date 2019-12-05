package cn.schoolwow.quickhttp.connection;

import cn.schoolwow.quickhttp.QuickHttp;
import cn.schoolwow.quickhttp.response.Response;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class AbstractConnectionTest {

    @Test
    public void getConnection() throws IOException {
        QuickHttp.proxy("127.0.0.1",8888);
        Response response = QuickHttp.connect("http://127.0.0.1:9000/api/attachment/uploadAttachment")
                .method(Connection.Method.POST)
                .basicAuth("sunyue","aa1122335")
                .data("file",new File("C:\\Users\\64882\\Downloads\\git.schoolwow.cn\\Nginx\\1_git.schoolwow.cn_bundle.crt"))
                .data("file",new File("C:\\Users\\64882\\Downloads\\git.schoolwow.cn\\Apache\\2_git.schoolwow.cn.crt"))
                .execute();
        System.out.println(response.statusCode());
    }
}
