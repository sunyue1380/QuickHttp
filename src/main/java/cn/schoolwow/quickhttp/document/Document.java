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

    public Elements select(String cssQuery) {
        Elements elements = new Elements();
        Evaluator evaluator = QueryParser.parse(cssQuery);
        //广度遍历
        LinkedList<Element> linkedList = new LinkedList();
        linkedList.offer(root);
        while(!linkedList.isEmpty()){
            Element element = linkedList.poll();
            //注释标签
            if(element.tagName()==null){
                continue;
            }
            //排除掉注释标签
            if(evaluator.matches(element)){
                elements.add(element);
            }
            linkedList.addAll(element.childElements());
        }

        return elements;
    }
}