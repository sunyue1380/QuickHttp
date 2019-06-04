package cn.schoolwow.quickhttp.document.parse;

import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import cn.schoolwow.quickhttp.document.query.Evaluator;
import cn.schoolwow.quickhttp.document.query.QueryParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class HTMLTokenParser {
    private Logger logger = LoggerFactory.getLogger(HTMLTokenParser.class);
    private List<HTMLToken> htmlTokenList;
    private AbstractElement root = null;
    private List<AbstractElement> allElements = new ArrayList<>();

    public static Element parse(List<HTMLToken> htmlTokenList){
        Element root = new HTMLTokenParser(htmlTokenList).root;
        return root;
    }

    private HTMLTokenParser(List<HTMLToken> htmlTokenList){
        this.htmlTokenList = htmlTokenList;
        parse();
    }

    /**语义分析*/
    private void parse(){
        AbstractElement current = root;
        for(HTMLToken htmlToken:htmlTokenList){
            switch(htmlToken.tokenType){
                case openTag:{
                    AbstractElement newElement = new AbstractElement();
                    allElements.add(newElement);
                    if(current==null){
                        root = newElement;
                    }else{
                        newElement.parent = current;
                        newElement.parent.childList.add(newElement);
                    }
                    current = newElement;
                }break;
                case tagName:{
                    current.tagName = htmlToken.value.toLowerCase();
                }break;
                case commentTag:{
                    current.isComment = true;
                    current.ownOriginText = htmlToken.value;
                    current.ownText = escapeOwnOriginText(current.ownOriginText);
                }break;
                case attribute:{
                    current.attribute = htmlToken.value;
                    current.attributes.putAll(AttributeParser.parse(htmlToken.value));
                }break;
                case openTagClose:{
                }break;
                case textContent:{
                    current.originTextNodes.add(htmlToken.value);
                    current.textNodes.add(escapeOwnOriginText(htmlToken.value));
                }break;
                case closeTag:{
                    if(htmlToken.value.equals(">")||htmlToken.value.equals("/>")){
                        current.isSingleNode = true;
                    }
                    if(current.ownOriginText==null){
                        StringBuilder sb = new StringBuilder("");
                        for(String _ownOriginText:current.originTextNodes){
                            sb.append(_ownOriginText);
                        }
                        current.ownOriginText = sb.toString();
                    }
                    if(current.ownText==null){
                        StringBuilder sb = new StringBuilder("");
                        for(String _ownText:current.textNodes){
                            sb.append(_ownText);
                        }
                        current.ownText = sb.toString();
                    }
                    current = current.parent;
                }break;
            }
        }
    }

    private String escapeOwnOriginText(String ownOriginText){
        return ownOriginText.replace("&quot;","\"")
                .replace("&amp;","&")
                .replace("&lt;","<")
                .replace("&gt;",">")
                .replace("&nbsp;"," ");
    }

    class AbstractElement implements Element {
        /**节点名称*/
        private String tagName;
        /**是否是单节点*/
        private boolean isSingleNode;
        /**是否是注释节点*/
        private boolean isComment;
        /**父节点*/
        private AbstractElement parent;
        /**属性*/
        private Map<String,String> attributes = new HashMap<>();
        /**属性文本*/
        private String attribute = "";
        /**原始文本内容*/
        private String ownOriginText = "";
        /**转义后文本内容*/
        private String ownText = "";
        /**子节点*/
        private List<Element> childList = new ArrayList<>();

        /**深度遍历后的元素*/
        private Elements allElements;
        /**所有节点文本*/
        private String textContent;
        /**原始节点文本列表*/
        private List<String> originTextNodes = new ArrayList<>();
        /**转义节点文本列表*/
        private List<String> textNodes = new ArrayList<>();
        /**节点在父节点的子节点中的索引*/
        private int elementSiblingpos = -1;
        /**用于深度遍历*/
        private boolean isVisited;

        @Override
        public Elements select(String cssQuery) {
            Elements elements = new Elements();
            Evaluator evaluator = QueryParser.parse(cssQuery);
            //广度遍历
            LinkedList<Element> linkedList = new LinkedList();
            linkedList.offer(this);
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

        @Override
        public Map<String, String> attribute() {
            return attributes;
        }

        @Override
        public String id() {
            return attributes.get("id");
        }

        @Override
        public boolean hasClass(String className) {
            String elementClassName = attributes.get("class");
            if(elementClassName==null||elementClassName.isEmpty()){
                return false;
            }
            String[] classNames = new String[]{className};
            if(className.contains(".")){
                classNames = className.split("\\.");
            }
            for(String _className:classNames){
                if(!elementClassName.contains(_className)){
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean hasAttr(String attributeKey) {
            return attributes.containsKey(attributeKey);
        }

        @Override
        public String attr(String attributeKey) {
            return attributes.get(attributeKey);
        }

        public String tagName() {
            return tagName;
        }

        public String text() {
            if(textContent!=null){
                return textContent;
            }
            assureAllElements();
            StringBuilder builder = new StringBuilder();
            for(Element element:allElements){
                builder.append(element.ownText());
            }
            textContent = builder.toString();
            return textContent;
        }

        @Override
        public List<String> textNodes() {
            return textNodes;
        }

        @Override
        public String html() {
            resetVisited();
            Stack<Element> stack = new Stack<>();
            for(Element child:childList){
                stack.push(child);
            }
            return iterateStack(stack);
        }

        @Override
        public String ownText() {
            return this.ownText;
        }

        @Override
        public String outerHtml() {
            resetVisited();
            Stack<Element> stack = new Stack<>();
            stack.push(this);
            return iterateStack(stack);
        }

        @Override
        public String val() {
            if("textarea".equals(tagName)){
                return text();
            }else if(hasAttr("value")){
                return attr("value");
            }else{
                return null;
            }
        }

        @Override
        public Element parent() {
            return parent;
        }

        @Override
        public Element firstChild() {
            if(childList.isEmpty()){
                return null;
            }
            return childList.get(0);
        }

        @Override
        public Element lastChild() {
            if(childList.isEmpty()){
                return null;
            }
            return childList.get(childList.size()-1);
        }

        @Override
        public Element childElement(int index) {
            if(index<1||index>childList.size()){
                return null;
            }
            return childList.get(index-1);
        }

        @Override
        public Elements childElements() {
            return new Elements(childList);
        }

        @Override
        public Elements siblingElements() {
            Elements elements = new Elements();
            for(Element element:parent.childList){
                if(element!=this){
                    elements.add(element);
                }
            }
            return elements;
        }

        @Override
        public Element previousElementSibling() {
            int pos = elementSiblingIndex();
            if(pos-1>=0){
                return parent.childList.get(pos-1);
            }else{
                return null;
            }
        }

        @Override
        public Element nextElementSibling() {
            int pos = elementSiblingIndex();
            if(pos+1<parent.childList.size()){
                return parent.childList.get(pos+1);
            }else{
                return null;
            }
        }

        @Override
        public int elementSiblingIndex() {
            if(elementSiblingpos>=0){
                return elementSiblingpos;
            }
            if(parent==null){
                elementSiblingpos = 0;
                return elementSiblingpos;
            }
            for(int i=0;i<parent.childList.size();i++){
                if(parent.childList.get(i)==this){
                    elementSiblingpos = i;
                    return elementSiblingpos;
                }
            }
            elementSiblingpos = 0;
            return elementSiblingpos;
        }

        @Override
        public Elements getAllElements() {
            assureAllElements();
            return allElements;
        }

        @Override
        public String toString(){
            if(isComment){
                return "<"+ownText+">";
            }else if(isSingleNode){
                return "<"+tagName+attribute+"/>";
            }else{
                return "<"+tagName+attribute+">"+ownOriginText==null?"":(ownOriginText.replaceAll("\r\n","换行符"))+"</"+tagName+">";
            }
        }

        /**遍历栈,生成html字符串*/
        private String iterateStack(Stack<Element> stack){
            StringBuilder sb = new StringBuilder();
            while(!stack.isEmpty()){
                AbstractElement element = (AbstractElement) stack.peek();
                if(element.childList.isEmpty()){
                    if(element.isComment){
                        sb.append("<"+element.ownText+">");
                    }else if(element.isSingleNode){
                        sb.append("<"+element.tagName+element.attribute+"/>");
                    }else{
                        sb.append("<"+element.tagName+element.attribute+">"+(element.ownOriginText==null?"":element.ownOriginText)+"</"+element.tagName+">");
                    }
                    //子节点
                    element.isVisited = true;
                    stack.pop();
                }else if(isAllVisited(element.childList)){
                    //非叶节点但所有子节点已经访问完毕
                    sb.append("</"+element.tagName+">");
                    element.isVisited = true;
                    stack.pop();
                }else{
                    sb.append("<"+element.tagName+element.attribute+">"+(element.ownOriginText==null?"":element.ownOriginText));
                    //从右往左压入子节点
                    Elements childElements = element.childElements();
                    for(int i=childElements.size()-1;i>=0;i--){
                        stack.push(childElements.get(i));
                    }
                }
            }
            return sb.toString();
        }

        private boolean isAllVisited(List<Element> childList){
            for(Element element:childList){
                if(!((AbstractElement)element).isVisited){
                    return false;
                }
            }
            return true;
        }

        private void resetVisited(){
            Stack<Element> stack = new Stack<>();
            stack.push(this);
            while(!stack.isEmpty()){
                AbstractElement element = (AbstractElement) stack.pop();
                element.isVisited = false;
                for(Element child:element.childList){
                    stack.push(child);
                }
            }
        }

        private void assureAllElements(){
            if(allElements==null){
                allElements = new Elements();
                Stack<Element> stack = new Stack<>();
                stack.push(this);
                while(!stack.isEmpty()){
                    Element element = stack.pop();
                    allElements.add(element);
                    Elements childElements = element.childElements();
                    for(int i=childElements.size()-1;i>=0;i--){
                        stack.push(childElements.get(i));
                    }
                }
                logger.trace("[DOM树]{}",allElements);
            }
            resetVisited();
        }
    }
}
