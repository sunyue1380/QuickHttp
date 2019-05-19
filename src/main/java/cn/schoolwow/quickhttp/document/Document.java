package cn.schoolwow.quickhttp.document;

import cn.schoolwow.quickhttp.document.parse.Node;
import cn.schoolwow.quickhttp.document.parse.Parser;

public class Document {
    private Node root = null;

    public static Document parse(String html){
        return new Document(html);
    }

    public Document(String html) {
        root = Parser.parse(html);
    }
}
