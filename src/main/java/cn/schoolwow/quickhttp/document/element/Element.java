package cn.schoolwow.quickhttp.document.element;

import java.util.Map;

public interface Element {
    Map<String,String> attribute();
    String tagName();
    String text();
    int elementSiblingIndex();
}
