package cn.schoolwow.quickhttp.document.parser;

import cn.schoolwow.quickhttp.document.element.AbstractElement;
import cn.schoolwow.quickhttp.document.element.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
                        AttributeParser.parse(htmlToken.value,current.attributes);
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
}
