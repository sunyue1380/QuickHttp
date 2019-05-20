package cn.schoolwow.quickhttp.document.element;

import cn.schoolwow.quickhttp.document.parse.Node;

public class Element {
    private Node node;

    public Element(Node node) {
        this.node = node;
    }

    public String tagName() {
        return node.tagName;
    }

    public String text() {
        return node.textContent;
    }

    @Override
    public String toString(){
        return "<"+node.tagName+">";
    }
}
