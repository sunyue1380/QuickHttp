package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class DocumentTest {
    private Logger logger = LoggerFactory.getLogger(DocumentTest.class);

//    @Test
//    public void testComposit() throws IOException {
//        String html = FileUtils.readFileToString(new File("index.html"),"gb2312");
//        Document document = Document.parse(html);
//        Element element = document.select("#signin > div.bd > div.task-detail > ul").getFirst();
//        System.out.println(element.text());
//    }

    @Test
    public void testPart() throws IOException {
        String html = FileUtils.readFileToString(new File("index.html"),"utf-8");
        html = html.substring(0,4096);
        Document document = Document.parse(html);
        Element element = document.select("meta[http-equiv=content-type], meta[charset]").getFirst();
        Assert.assertEquals("meta",element.tagName());
    }

    @Test
    public void testEvaluator() {
        Map<String,String> elementMap = new LinkedHashMap<>();
        elementMap.put("<html id=\"id\"></html>","#id");
        elementMap.put("<html class=\".m1.m2\"></html>",".m1.m2");
        elementMap.put("<html attr></html>","[attr]");
        elementMap.put("<html attrPrefixOne></html>","[^attrPrefix]");
        elementMap.put("<html attr=val></html>","[attr=val]");
        elementMap.put("<html attr=\"val\"></html>","[attr=\"val\"]");
        elementMap.put("<html attr=valPrefixOne></html>","[attr^=valPrefix]");
        elementMap.put("<html attr=OnevalSuffix></html>","[attr$=valSuffix]");
        elementMap.put("<html attr=OnevalContainingTwo></html>","[attr*=valContaining]");
        elementMap.put("<html [attr=123]></html>","[attr~=\\d+]");

        Set<String> keySet = elementMap.keySet();
        for(String key:keySet){
            Elements elements = Document.parse(key).select(elementMap.get(key));
            Assert.assertEquals(1,elements.size());
            Assert.assertEquals("html",elements.tagName());
        }
    }

    @Test
    public void testCombination() {
        Map<String,String> elementMap = new LinkedHashMap<>();
        elementMap.put("<html><head></head></html>","html head");
        elementMap.put("<html><head></head></html>","html > head");
        elementMap.put("<html><p></p><head></head></html>","p + head");
        elementMap.put("<html><p></p><h1></h1><head></head></html>","p ~ head");
        Set<String> keySet = elementMap.keySet();
        for(String key:keySet){
            Elements elements = Document.parse(key).select(elementMap.get(key));
            Assert.assertEquals(1,elements.size());
            Assert.assertEquals("head",elements.tagName());
        }
    }

    @Test
    public void testOr() {
        Elements elements = Document.parse("<html><head></head></html>").select("html,head");
        Assert.assertEquals(2,elements.size());
    }

    @Test
    public void testCommonPseudo() {
        Map<String,String> elementMap = new LinkedHashMap<>();
        elementMap.put("<div><p></p></div>","div p:first-child");
        elementMap.put("<div><p></p></div>","div p:last-child");
        elementMap.put("<div><p></p></div>","div p:first-of-type");
        elementMap.put("<div><p></p></div>","div p:last-of-type");
        elementMap.put("<div><p></p></div>","div p:only-child");
        elementMap.put("<div><p></p></div>","div p:only-of-type");
        elementMap.put("<div><p></p></div>","div p:empty");
        Set<String> keySet = elementMap.keySet();
        for(String key:keySet){
            Elements elements = Document.parse(key).select(elementMap.get(key));
            Assert.assertEquals(1,elements.size());
            Assert.assertEquals("p",elements.tagName());
        }
    }

    @Test
    public void testPseudo() {
        Map<String,String> elementMap = new LinkedHashMap<>();
        elementMap.put("<div><p></p><p></p></div>","div p:lt(1)");
        elementMap.put("<div><p></p><p></p></div>","div p:gt(0)");
        elementMap.put("<div><p></p><p></p></div>","div p:eq(0)");
        //TODO 待添加剩余测试用例
        Set<String> keySet = elementMap.keySet();
        for(String key:keySet){
            Elements elements = Document.parse(key).select(elementMap.get(key));
            Assert.assertEquals(1,elements.size());
            Assert.assertEquals("p",elements.tagName());
        }
    }

    @Test
    public void testNth() {
        Map<String,String> elementMap = new LinkedHashMap<>();
        elementMap.put("<div><p></p><p></p></div>","div p:nth-child(1)");
        elementMap.put("<div><p></p><p></p></div>","div p:nth-last-child(1)");
        elementMap.put("<div><p></p><p></p></div>","div p:nth-of-type(1)");
        elementMap.put("<div><p></p><p></p></div>","div p:nth-last-of-type(1)");
        Set<String> keySet = elementMap.keySet();
        for(String key:keySet){
            Elements elements = Document.parse(key).select(elementMap.get(key));
            Assert.assertEquals(1,elements.size());
            Assert.assertEquals("p",elements.tagName());
        }
    }
}