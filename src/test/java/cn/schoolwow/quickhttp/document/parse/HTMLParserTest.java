package cn.schoolwow.quickhttp.document.parse;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class HTMLParserTest {
    @Test
    public void testBasic() {
        String html = "<html></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Assert.assertEquals(4,htmlTokenList.size());
    }

    @Test
    public void testAttribute() {
        String html = "<html id=\"quote\" class='singleQuote'></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Assert.assertEquals(5,htmlTokenList.size());
    }

    @Test
    public void testComment() {
        String html = "<html><!--this is a comment--></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Assert.assertEquals(7,htmlTokenList.size());
    }

    @Test
    public void testSingleNode() {
        String html = "<html><input id='block'><br/></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Assert.assertEquals(11,htmlTokenList.size());
    }

    @Test
    public void testTextNode() {
        String html = "<html>hello<h1>title</h1></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Assert.assertEquals(10,htmlTokenList.size());
    }

    @Test
    public void testScript() {
        String html = "<html><script>replace('</div>');</script></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Assert.assertEquals(9,htmlTokenList.size());
    }

    @Test
    public void testBodyScript() {
        String html = "<html><body><script>replace('</div>');</script></body></html>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Assert.assertEquals(13,htmlTokenList.size());
    }

    @Test
    public void testXML() {
        String html = "<?xml version=\"1.0\"?><softCompany></softCompany>";
        List<HTMLToken> htmlTokenList = HTMLParser.parse(html);
        Assert.assertEquals(8,htmlTokenList.size());
    }
}