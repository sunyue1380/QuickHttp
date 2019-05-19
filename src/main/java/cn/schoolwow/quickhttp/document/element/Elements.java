package cn.schoolwow.quickhttp.document.element;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class Elements extends ArrayList<Element>{
    public Elements() {
    }

    public Elements(int initialCapacity) {
        super(initialCapacity);
    }

    public Elements(Collection<Element> elements) {
        super(elements);
    }

    public Elements(List<Element> elements) {
        super(elements);
    }

    public Elements(Element... elements) {
        super(Arrays.asList(elements));
    }

    /**
     * 返回集合第一个标签的标签名
     * */
    public String tagName(){
        return this.get(0).tagName();
    }
}
