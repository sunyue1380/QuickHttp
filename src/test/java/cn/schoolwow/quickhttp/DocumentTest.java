package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import com.alibaba.fastjson.JSON;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DocumentTest {
    private Logger logger = LoggerFactory.getLogger(DocumentTest.class);

    @Test
    public void testById() {
        String html = "<html id=\"identify\">identify</html>";
        Document document = Document.parse(html);
        Element element = document.select("#identify").getFirst();
        Assert.assertEquals("html",element.tagName());
    }

    @Test
    public void testByClassName() {
        String html = "<html class=\"c1\"><head class=\"c1 c2\"></head></html>";
        Document document = Document.parse(html);
        {
            Element element = document.select(".c1").getFirst();
            Assert.assertEquals("html",element.tagName());
        }{
            Element element = document.select(".c1.c2").getFirst();
            Assert.assertEquals("head",element.tagName());
        }
    }

    @Test
    public void testByTagName() {
        String html = "<html><h1>title1</h1><p>text1</p><p>222</p></html>";
        Document document = Document.parse(html);
        {
            Elements elements = document.select("html p");
            logger.info(JSON.toJSONString(elements));
            Assert.assertEquals(2,elements.size());
        }{
            Elements elements = document.select("*");
            Assert.assertEquals(4,elements.size());
        }
    }
}