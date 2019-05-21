package cn.schoolwow.quickhttp.document;

import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import cn.schoolwow.quickhttp.document.parse.Parser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Document {
    Logger logger = LoggerFactory.getLogger(Element.class);
    private Element root;

    public static Document parse(String html) {
        return new Document(html);
    }
    private Document(String html) {
        root = Parser.parse(html);
    }

    public Elements select(String cssQuery) {
        return null;
    }
}