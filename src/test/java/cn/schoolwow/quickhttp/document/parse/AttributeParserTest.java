package cn.schoolwow.quickhttp.document.parse;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class AttributeParserTest {
    private Logger logger = LoggerFactory.getLogger(AttributeParser.class);

    @Test
    public void testBasic() {
        String attribute = "id=\"quote\" class='singleQuote'";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        logger.info("[属性]{}",attibuteMap);
        Assert.assertEquals(2,attibuteMap.size());
        Assert.assertEquals(true,attibuteMap.containsKey("id"));
        Assert.assertEquals(true,attibuteMap.containsKey("class"));
        Assert.assertEquals("quote",attibuteMap.get("id"));
        Assert.assertEquals("singleQuote",attibuteMap.get("class"));
    }

    @Test
    public void testBasic2() {
        String attribute = " style= \"width:100px; \"";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        logger.info("[属性]{}",attibuteMap);
        Assert.assertEquals(1,attibuteMap.size());
        Assert.assertEquals(true,attibuteMap.containsKey("style"));
        Assert.assertEquals("width:100px; ",attibuteMap.get("style"));
    }

    @Test
    public void testBasic3(){
        String attribute = "type=password";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        logger.info("[属性]{}",attibuteMap);
        Assert.assertEquals(1,attibuteMap.size());
        Assert.assertEquals(true,attibuteMap.containsKey("type"));
        Assert.assertEquals("password",attibuteMap.get("type"));
    }

    @Test
    public void testOne(){
        String attribute = "disabled name = 'username'";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        logger.info("[属性]{}",attibuteMap);
        Assert.assertEquals(2,attibuteMap.size());
        Assert.assertEquals(true,attibuteMap.containsKey("disabled"));
        Assert.assertEquals(true,attibuteMap.containsKey("name"));
        Assert.assertEquals("",attibuteMap.get("disabled"));
        Assert.assertEquals("username",attibuteMap.get("name"));
    }

    @Test
    public void testSpace() {
        String attribute = "name = \"user name\"";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        logger.info("[属性]{}",attibuteMap);
        Assert.assertEquals(1,attibuteMap.size());
        Assert.assertEquals(true,attibuteMap.containsKey("name"));
        Assert.assertEquals("user name",attibuteMap.get("name"));
    }

    @Test
    public void testSpace2() {
        String attribute = " ng-click = hello();";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        logger.info("[属性]{}",attibuteMap);
        Assert.assertEquals(1,attibuteMap.size());
        Assert.assertEquals(true,attibuteMap.containsKey("ng-click"));
        Assert.assertEquals("hello();",attibuteMap.get("ng-click"));
    }

    @Test
    public void testAll() {
        String attribute = "disabled id=\"quote\" class = \"ha ha\" type=password ng-click = hello();";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        logger.info("[属性]{}",attibuteMap);
        Assert.assertEquals(5,attibuteMap.size());
    }

    @Test
    public void testComposit() {
        String attribute = " data-href=\"http://news.baidu.com\" href=\"http://news.baidu.com/ns?word=word&tn=news&cl=2&rn=20&ct=1&fr=wenku\" class=\"logSend\" data-logsend='{\"send\":[\"general\", \"toptablink\",{\"to\":\"news\"}]}' wdfield=\"word\" onmousedown=\"setHeadUrl(this)\"";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        logger.info("[属性]{}",attibuteMap);
        Assert.assertEquals(6,attibuteMap.size());
    }
}