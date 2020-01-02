package cn.schoolwow.quickhttp.document.element;

import cn.schoolwow.quickhttp.document.query.Evaluator;
import cn.schoolwow.quickhttp.document.query.QueryParser;

import java.util.*;

public class AbstractElement implements Element{
    /**节点名称*/
    public String tagName;
    /**是否是单节点*/
    public boolean isSingleNode;
    /**是否是注释节点*/
    public boolean isComment;
    /**是否是文本节点*/
    public boolean isTextNode;
    /**父节点*/
    public AbstractElement parent;
    /**属性*/
    public Map<String,String> attributes = new HashMap<>();
    /**属性文本*/
    public String attribute = "";
    /**原始文本内容*/
    public String ownOriginText = "";
    /**转义后文本内容*/
    public String ownText = "";
    /**子元素*/
    public List<Element> childList = new ArrayList<>();
    /**子元素节点*/
    public Elements textList = new Elements();
    /**子元素(包含文本元素)*/
    public List<Element> childTextList = new ArrayList<>();
    /**深度遍历后的子元素(包含文本元素)*/
    public Elements childTextElements;
    /**深度遍历后的子元素*/
    public Elements childElements;
    /**深度遍历后的文本元素*/
    public Elements textElements;
    public String textContent;
    public String outerHtml;
    public String html;
    /**节点在父节点的子节点中的索引*/
    public int elementSiblingpos = -1;

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
    public void assureAllElements(){
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
