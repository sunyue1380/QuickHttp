package cn.schoolwow.quickhttp.document;

import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import cn.schoolwow.quickhttp.document.parse.Node;
import cn.schoolwow.quickhttp.document.parse.Parser;
import com.alibaba.fastjson.JSON;
import com.sun.deploy.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Document {
    Logger logger = LoggerFactory.getLogger(Element.class);
    enum Selector {
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
            String[] tokens = StringUtils.splitString(value,".");
            for (String token : tokens) {
                if (!className.contains(token)) {
                    return false;
                }
            }
            return true;
        }),
        ByAll((s)->{
            if("*".equals(s)){
                return s;
            }else{
                return null;
            }
        },(value,node)->{
            return true;
        }),
        ByTag((s)->{
            if(s.matches("[a-zA-Z]+")){
                return s;
            }else{
                return null;
            }
        },(value,node)->{
            return node.tagName.equals(value);
        });

        private Function<String, String> condition;
        private BiFunction<String, Node, Boolean> nodePredicate;

        Selector(Function<String, String> condition, BiFunction<String, Node, Boolean> nodePredicate) {
            this.condition = condition;
            this.nodePredicate = nodePredicate;
        }
    }

    private List<Node> nodeList = new ArrayList<>();

    public static Document parse(String html) {
        return new Document(html);
    }

    private Document(String html) {
        //广度遍历放入所有节点
        Stack<Node> stack = new Stack<>();
        stack.push(Parser.parse(html));
        while (!stack.isEmpty()) {
            Node node = stack.pop();
            nodeList.add(node);
            for (Node child : node.childList) {
                stack.push(child);
            }
        }
    }

    public Elements select(String cssQuery) {
        LinkedList<Node> linkedList = new LinkedList();
        linkedList.addAll(nodeList);
        Selector[] selectors = Selector.values();

        String[] tokens = StringUtils.splitString(cssQuery," ");
        for(String token:tokens){
            int length = nodeList.size();
            for(int i=0;i<length;i++){
                Node node = linkedList.poll();
                for(Selector selector:selectors){
                    String value = selector.condition.apply(token);
                    if(value!=null&&selector.nodePredicate.apply(value,node)){
                        logger.debug("[符合条件]选择器:{},类型:{},节点标签:{}",token,selector.name(),node.tagName);
                        linkedList.offer(node);
                        break;
                    }
                }
            }
        }
        List<Element> elementList = new ArrayList<>(linkedList.size());
        for(Node node:linkedList){
            elementList.add(new Element(node));
        }
        return new Elements(elementList);
    }
}