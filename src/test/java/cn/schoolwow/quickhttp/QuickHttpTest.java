package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.response.Response;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class QuickHttpTest {
    Logger logger = LoggerFactory.getLogger(QuickHttpTest.class);
    @Test
    public void testBaiDu() throws IOException {
        Response response = QuickHttp.connect("https://www.baidu.com")
                .execute();
        logger.info("url:{} , status:{} , message:{}",response.url(),response.statusCode(),response.statusMessage());
    }

    @Test
    public void testUpload() throws IOException {
        Response response = QuickHttp.connect("http://1.w2wz.com/upload.php")
                .method(Connection.Method.POST)
                .data("MAX_FILE_SIZE","1048576")
                .data("uploadimg",new File("upload.jpg"))
                .execute();
        logger.info("status:{} , message:{}",response.statusCode(),response.statusMessage());
        logger.info("body:{}",response.body());
    }

    @Test
    public void testDownload() throws IOException {
        Response response = QuickHttp.connect("http://pic37.nipic.com/20140113/8800276_184927469000_2.png")
                .execute();
        File file = new File("quickhttp.png");
        FileOutputStream fos = FileUtils.openOutputStream(file);
        IOUtils.copy(response.bodyStream(),fos);
        fos.close();
        response.close();
        logger.info("[下载文件]是否存在:{}",file.exists());
    }
}