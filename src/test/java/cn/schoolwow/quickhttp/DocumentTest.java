package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import com.alibaba.fastjson.JSON;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

public class DocumentTest {
    private Logger logger = LoggerFactory.getLogger(DocumentTest.class);

    @Test
    public void testHTML() throws IOException {
        String html = FileUtils.readFileToString(new File("index.html"),"utf-8");
//        System.out.println(html.substring(4166));
//        System.out.println(html.length());

//        String html = "<html id=\"2121212\"><h1>title1</h1><p>text1</p><p>222</p></html>";
        Document document = Document.parse(html);

        String outerHTML = document.root().outerHtml();
        FileUtils.writeStringToFile(new File("target.html"),outerHTML,"utf-8");
        System.out.println(outerHTML);

    }

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
    public void testCombination() throws IOException {
        String html = FileUtils.readFileToString(new File("index.html"),"utf-8");
//        String html = "<html><body id=\"content\"><h2></h2><h2></h2><h2></h2><h2></h2></body></html>";
        Document document = Document.parse(html);
        String cssQuery = "#content > h2:nth-child(22)";
        {
            System.out.println(cssQuery);

            System.out.println("jsoup:"+Jsoup.parse(html).select(cssQuery).size());

            Elements elements = document.select(cssQuery);
            System.out.println(elements.size());
            Assert.assertEquals(1, elements.size());
        }
    }

    @Test
    public void testByTagName() {
        String html = "<html><h1>title1</h1><p>text1</p><p>222</p></html>";
        Document document = Document.parse(html);
        {
            Elements elements = document.select("#11");
            logger.info(JSON.toJSONString(elements));
            Assert.assertEquals(1,elements.size());
        }{
            Elements elements = document.select("*");
            Assert.assertEquals(4,elements.size());
        }
    }

    @Test
    public void testJsoup() {
        String html = "<html><h1><p>21211</p></h1><p>text1</p><p>aaaa</p><p>222</p></html>";
        org.jsoup.select.Elements elements = Jsoup.parse(html).select("html p");
        System.out.println(elements);
        elements = Jsoup.parse(html).select("html > p");
        System.out.println(elements);
    }
}