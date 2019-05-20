package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.parse.Node;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;
import java.util.function.Function;

public class DocumentTest {
    private Logger logger = LoggerFactory.getLogger(DocumentTest.class);

    enum Selector{
        //#id
        ById((s) -> {
            if (s.charAt(0) == '#') {
                return s.substring(1);
            } else {
                return null;
            }
        }, (value, node) -> {
            if (node.attributes.containsKey("id") && value.equals(node.attributes.get("id"))) {
                return true;
            } else {
                return false;
            }
        }),
        //.m1.m2
        ByClass((s) -> {
            if (s.charAt(0) == '.') {
                return s;
            } else {
                return null;
            }
        }, (value, node) -> {
            if (!node.attributes.containsKey("class")) {
                return false;
            }
            String className = node.attributes.get("class");
            String[] tokens = value.split(".");
            for (String token : tokens) {
                if (!className.contains(token)) {
                    return false;
                }
            }
            return true;
        });

        private Function<String, String> condition;
        private BiFunction<String, Node, Boolean> nodePredicate;

        Selector(Function<String, String> condition, BiFunction<String, Node, Boolean> nodePredicate) {
            this.condition = condition;
            this.nodePredicate = nodePredicate;
        }
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
    public void testSelector() {
        System.out.println(Selector.ByClass.condition.apply(".c1.c2")!=null);
    }
}