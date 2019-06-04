package cn.schoolwow.quickhttp.document;

import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import cn.schoolwow.quickhttp.document.parse.HTMLParser;
import cn.schoolwow.quickhttp.document.parse.HTMLTokenParser;
import cn.schoolwow.quickhttp.document.query.Evaluator;
import cn.schoolwow.quickhttp.document.query.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class Document {
    Logger logger = LoggerFactory.getLogger(Element.class);
    private Element root;

    public static Document parse(String html) {
        return new Document(html);
    }
    private Document(String html) {
        root = HTMLTokenParser.parse(HTMLParser.parse(html));
    }

    public Element root() {
        return root;
    }

    public String title(){
        Elements titles = select("html > head > title");
        if(titles==null||titles.size()==0){
            return "";
        }
        return titles.first().text();
    }

    public Element selectFirst(String cssQuery) {
        return select(cssQuery).first();
    }

    public Element selectLast(String cssQuery) {
        return select(cssQuery).last();
    }

    public Elements select(String cssQuery) {
        return root.select(cssQuery);
    }
}