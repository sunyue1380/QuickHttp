package cn.schoolwow.quickhttp.document.parse;

import cn.schoolwow.quickhttp.document.element.Element;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class HTMLTokenParserTest {
    private Logger logger = LoggerFactory.getLogger(HTMLTokenParser.class);
    @Test
    public void testIndex() throws IOException {
//        String html = FileUtils.readFileToString(new File("index.html"),"utf-8");
//        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
//        Element element = HTMLTokenParser.parse(htmlTokenList);
//        FileUtils.writeStringToFile(new File("target.html"),element.outerHtml(),"utf-8");
    }

    @Test
    public void testBasic() {
        String html = "<html></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Element root = HTMLTokenParser.parse(htmlTokenList);
        Assert.assertEquals("html",root.tagName());
        Assert.assertEquals(html,root.outerHtml());
    }

    @Test
    public void testAttribute() {
        String html = "<html id=\"quote\" class='singleQuote'></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Element root = HTMLTokenParser.parse(htmlTokenList);
        Assert.assertEquals("html",root.tagName());
        Assert.assertEquals("quote",root.attr("id"));
        Assert.assertEquals("singleQuote",root.attr("class"));
        Assert.assertEquals(html,root.outerHtml());
    }

    @Test
    public void testComment() {
        String html = "<html><!--this is a comment--></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Element root = HTMLTokenParser.parse(htmlTokenList);
        Assert.assertEquals("html",root.tagName());
        Element commentElement = root.firstChild();
        Assert.assertEquals("!--this is a comment--",commentElement.ownText());
        Assert.assertEquals(html,root.outerHtml());
    }

    @Test
    public void testSingleNode() {
        String html = "<html><input id='block'><br/></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Element root = HTMLTokenParser.parse(htmlTokenList);
        Assert.assertEquals("block",root.childElement(1).attr("id"));
        Assert.assertEquals("br",root.childElement(2).tagName());
        Assert.assertEquals("<html><input id='block'/><br/></html>",root.outerHtml());
    }

    @Test
    public void testTextNode() {
        String html = "<html>hello<h1>title</h1></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Element root = HTMLTokenParser.parse(htmlTokenList);
        Assert.assertEquals("hello",root.ownText());
        Assert.assertEquals("title",root.firstChild().ownText());
        Assert.assertEquals(html,root.outerHtml());
    }

    @Test
    public void testScript() {
        String html = "<html><body><script>replace('</div>');</script></body></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Element root = HTMLTokenParser.parse(htmlTokenList);
        Assert.assertEquals("replace('</div>');",root.firstChild().firstChild().ownText());
        Assert.assertEquals(html,root.outerHtml());
    }

    @Test
    public void testMissing() {
        String html = "<html><body><p>12313<span>21212</body></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Element root = HTMLTokenParser.parse(htmlTokenList);
        Assert.assertEquals("<html><body><p>12313<span>21212</span></p></body></html>",root.outerHtml());
    }

    @Test
    public void testInput() {
        String html = "<input value=\"<iframe src='http://player.youku.com/embed/XNTQwMTgxMTE2' allowfullscreen></iframe>\"/>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Element root = HTMLTokenParser.parse(htmlTokenList);
        Assert.assertEquals(html,root.outerHtml());
    }
}