package cn.schoolwow.quickhttp.document;

import cn.schoolwow.quickhttp.QuickHttp;
import org.junit.Test;

public class DocumentParserTest {

    @Test
    public void testParse() throws Exception {
        DocumentParser documentParser = QuickHttp.connect("https://www.baidu.com/").execute().parser();
        documentParser.parse((element)->{
            System.out.println("元素:"+element.tagName()+", 属性:"+element.attribute().toString());
            return false;
        });
    }
}
