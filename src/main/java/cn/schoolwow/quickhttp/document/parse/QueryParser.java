package cn.schoolwow.quickhttp.document.parse;

import cn.schoolwow.quickhttp.document.element.Element;
import com.sun.deploy.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public class QueryParser {
    private Logger logger = LoggerFactory.getLogger(QueryParser.class);
    private char[] chars; //输入参数
    private int index = 0; //当前位置
    private int sectionStart=0; //token起始位置
    private QueryParser.State state;//起始状态
    private List<Selector> selectorList = new ArrayList<>();

    public static List<Selector> parse(String attribute){
        return new QueryParser(attribute).selectorList;
    }

    private QueryParser(String attribute){
        this.chars = attribute.toCharArray();
    }

    enum State {
        inId,
        inTag,
        inNthChild,
        inImmediateParent,
        inParent,
    }

    enum Selector {
        //#id
        ById((query, element) -> {
            String value = query.substring(1);
            if (element.attribute().containsKey("id") && value.equals(element.attribute().get("id"))) {
                return true;
            } else {
                return false;
            }
        }),
        //.m1.m2
        ByClass((query, element) -> {
            if (!element.attribute().containsKey("class")) {
                return false;
            }
            String className = element.attribute().get("class");
            String[] tokens = StringUtils.splitString(query,".");
            for (String token : tokens) {
                if (!className.contains(token)) {
                    return false;
                }
            }
            return true;
        }),
        ByTag((query,element)->{
            return element.tagName().equals(query);
        }),
        ByNthChildTag((query,element)->{
            int index = Integer.parseInt(query.substring(query.indexOf("(")+1,query.lastIndexOf(")")));
            return element.elementSiblingIndex()+1==index;
        }),
        ByImmediateParent((query,element)->{
            return true;
        });

        private BiFunction<String, Element, Boolean> nodePredicate;

        Selector(BiFunction<String, Element, Boolean> nodePredicate) {
            this.nodePredicate = nodePredicate;
        }
    }
}
