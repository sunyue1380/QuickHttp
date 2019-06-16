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
    private AbstractElement root = new AbstractElement();
    private List<AbstractElement> allElements = new ArrayList<>();

    public static Element parse(List<HTMLToken> htmlTokenList){
        Element root = new HTMLTokenParser(htmlTokenList).root;
        return root;
    }

    private HTMLTokenParser(List<HTMLToken> htmlTokenList){
        this.htmlTokenList = htmlTokenList;
        root.tagName = "root";
        parse();
    }

    /**语义分析*/
    private void parse(){
        AbstractElement current = root;
        allElements.add(root);
        for(HTMLToken htmlToken:htmlTokenList){
            switch(htmlToken.tokenType){
                case openTag:{
                    AbstractElement newElement = new AbstractElement();
                    allElements.add(newElement);
//                    if(current==null){
//                        root = newElement;
//                    }else{
//                    }
                    newElement.parent = current;
                    newElement.parent.childList.add(newElement);
                    newElement.parent.childTextList.add(newElement);
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
                    if(!"!DOCTYPE".equals(current.tagName.toUpperCase())){
                        current.attributes.putAll(AttributeParser.parse(htmlToken.value));
                    }
                }break;
                case openTagClose:{
                }break;
                case textContent:{
                    if(current!=null){
                        //<!DOCTYPE HTML> 这里是空白 <head>
                        AbstractElement textElement = new AbstractElement();
                        textElement.isTextNode = true;
                        textElement.ownOriginText = htmlToken.value;
                        textElement.ownText = escapeOwnOriginText(htmlToken.value);
                        textElement.parent = current;
                        textElement.outerHtml = textElement.ownText;
                        textElement.textContent = textElement.ownText;
                        current.childTextList.add(textElement);
                        current.textList.add(textElement);
                    }
                }break;
                case closeTag:{
                    if(htmlToken.value.equals(">")||htmlToken.value.equals("/>")){
                        current.isSingleNode = true;
                        current = current.parent;
                    }else if(("</"+current.tagName+">").equals(htmlToken.value)){
                        //检查结束标签标签名
                        current = current.parent;
                    }
                }break;
            }
        }
        for(AbstractElement element:allElements){
            if(element.isComment){
                continue;
            }
            StringBuilder ownTextBuilder = new StringBuilder("");
            StringBuilder originTextBuilder = new StringBuilder("");
            for(Element e:element.textList){
                AbstractElement ee = (AbstractElement) e;
                ownTextBuilder.append(ee.ownText);
                originTextBuilder.append(ee.ownOriginText);
            }
            element.ownText = ownTextBuilder.toString();
            element.ownOriginText = originTextBuilder.toString();
            element.assureAllElements();
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
        /**是否是文本节点*/
        private boolean isTextNode;
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
        /**子元素*/
        private List<Element> childList = new ArrayList<>();
        /**子元素节点*/
        private Elements textList = new Elements();
        /**子元素(包含文本元素)*/
        private List<Element> childTextList = new ArrayList<>();
        /**深度遍历后的子元素(包含文本元素)*/
        private Elements childTextElements;
        /**深度遍历后的子元素*/
        private Elements childElements;
        /**深度遍历后的文本元素*/
        private Elements textElements;
        private String textContent;
        private String outerHtml;
        private String html;
        /**节点在父节点的子节点中的索引*/
        private int elementSiblingpos = -1;

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
        public Element selectFirst(String cssQuery) {
            return select(cssQuery).first();
        }

        @Override
        public Element selectLast(String cssQuery) {
            return select(cssQuery).last();
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
            StringBuilder builder = new StringBuilder();
            for(Element element:textElements){
                builder.append(element.ownText());
            }
            textContent = builder.toString();
            return textContent;
        }

        @Override
        public Elements textElement() {
            return textList;
        }

        @Override
        public String html() {
            if(this.html==null){
                this.html = iterateChildTextElements(this.childTextElements.subList(1,this.childTextElements.size()-1));
            }
            return this.html;
        }

        @Override
        public String ownText() {
            return this.ownText;
        }

        @Override
        public String outerHtml() {
            if(this.outerHtml==null){
                this.outerHtml = iterateChildTextElements(this.childTextElements);
            }
            return this.outerHtml;
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
            return childElements;
        }

        @Override
        public String toString(){
            if(isComment){
                return "<"+ownText+">";
            }else if(isSingleNode){
                return "<"+tagName+attribute+"/>";
            }else{
                return "<"+tagName+attribute+">"+ownOriginText.replaceAll("\r\n","换行符")+"</"+tagName+">";
            }
        }

        /**遍历栈,生成html字符串*/
        private String iterateChildTextElements(List<Element> childTextElements){
            StringBuilder builder = new StringBuilder();
            Stack<AbstractElement> stack = new Stack();
            for(int i=0;i<childTextElements.size();i++){
                AbstractElement abstractElement = (AbstractElement) childTextElements.get(i);
                while(!stack.isEmpty()&&abstractElement.parent!=null&&stack.peek()!=abstractElement.parent){
                    builder.append("</"+stack.pop().tagName+">");
                }
                if(abstractElement.isComment){
                    builder.append("<"+abstractElement.ownOriginText+">");
                }else if(abstractElement.isTextNode){
                    builder.append(abstractElement.ownOriginText);
                }else{
                    //放入标签
                    if(abstractElement.isSingleNode){
                        builder.append("<"+abstractElement.tagName+abstractElement.attribute);
                        if(!abstractElement.tagName.startsWith("!")&&!abstractElement.tagName.startsWith("?")){
                            //排除特殊标签
                            builder.append("/");
                        }
                        builder.append(">");
                    }else{
                        builder.append("<"+abstractElement.tagName+abstractElement.attribute+">");
                        if(abstractElement.childTextList.size()==0){
                            builder.append("</"+abstractElement.tagName+">");
                        }else{
                            stack.push(abstractElement);
                        }
                    }
                }
            }
            while(!stack.isEmpty()){
                AbstractElement element = stack.pop();
                if(element.isSingleNode){
                    builder.append("/>");
                }else{
                    builder.append("</"+element.tagName+">");
                }
            }
            return builder.toString();
        }

        /**深度遍历子元素形成列表*/
        private void assureAllElements(){
            childTextElements = new Elements();
            textElements = new Elements();
            childElements = new Elements();
            Stack<AbstractElement> stack = new Stack<>();
            stack.push(this);
            while(!stack.isEmpty()){
                AbstractElement element = stack.pop();
                childTextElements.add(element);
                if(element.isTextNode){
                    textElements.add(element);
                }else{
                    childElements.add(element);
                }
                List<Element> childElements = element.childTextList;
                for(int i=childElements.size()-1;i>=0;i--){
                    stack.push((AbstractElement) childElements.get(i));
                }
            }
        }
    }
}
