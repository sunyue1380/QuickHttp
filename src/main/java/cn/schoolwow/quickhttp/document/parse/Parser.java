package cn.schoolwow.quickhttp.document.parse;

import cn.schoolwow.quickhttp.document.element.Element;
import com.sun.deploy.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class Parser {
    private Logger logger = LoggerFactory.getLogger(Parser.class);
    private char[] chars; //输入参数
    private int index = 0; //当前位置
    private int sectionStart=0; //token起始位置
    private State state = State.openingTag;//起始状态
    private List<Token> tokenList = new ArrayList<>(); //Token列表
    private AbstractElement root = null;
    private AbstractElement current = root;

    public static Element parse(String html){
        return new Parser(html).root;
    }

    private Parser(String html){
        this.chars = html.toCharArray();
        parseHTML();
        parseToken();
    }

    /**词法分析*/
    private void parseHTML(){
        while(index<chars.length){
            char c = chars[index];
            switch(state){
                case openingTag:{
                    if(index>0&&chars[index-1]=='<'){
                        addToken(TokenType.openTag);
                        state = State.inTagName;
                    }
                    //判断是否是注释标签
                    //<!--[if !IE]><!--> chars[index]=='!'&&chars[index+1]=='-'&&chars[index+2]=='-'
                    if(chars[index]=='!'&&chars[index+1]=='-'&&chars[index+2]=='-'){
                        state = State.inComment;
                    }
                }break;
                case inTagName:{
                    if(c==' '){
                        addToken(TokenType.tagName);
                        //<html data  >
                        state = State.inAttribute;
                    }else if(c=='>'){
                        addToken(TokenType.tagName);
                        //<html>
                        state = State.openTagClosing;
                    }else if(c=='/'&&chars[index+1]=='>'){
                        //<br/>
                        addToken(TokenType.tagName);
                        state = State.openTagClosing;
                    }
                }break;
                case inComment:{
                    if(chars[index]=='>'&&chars[index-1]=='-'&&chars[index-2]=='-'){
                        addToken(TokenType.commentTag);
                        state = State.openTagClosing;
                    }
                }break;
                case inAttribute:{
                    if(c=='>'){
                        addToken(TokenType.attribute);
                        //<html   lang=en>
                        state = State.openTagClosing;
                    }else if(c=='/'&&chars[index+1]=='>'){
                        //<br data-http=111/>
                        addToken(TokenType.attribute);
                        state = State.openTagClosing;
                    }
                }break;
                case openTagClosing:{
                    //<body>sdd</body>
                    if(chars[index-1]=='>'&&c!='<'){
                        addToken(TokenType.openTagClose);
                        state = State.inTextContent;
                    }else if(c=='<'&&chars[index+1]=='/'){
                        addToken(TokenType.openTagClose);
                        state = State.closingTag;
                    }else if(c=='<'){
                        addToken(TokenType.openTagClose);
                        state = State.openingTag;
                    }
                }break;
                case inTextContent:{
                    //<body>sdd</body>
                    if(c=='<'&&chars[index+1]=='/'){
                        addToken(TokenType.textContent);
                        state = State.closingTag;
                    }else if(c=='<'){
                        addToken(TokenType.textContent);
                        state = State.openingTag;
                    }
                }break;
                case closingTag:{
                    if(chars[index-1]=='>'&&c=='<'&&chars[index+1]=='/'){
                        //</body></html>
                        addToken(TokenType.closeTag);
                    }else if(chars[index-1]=='>'&&c!='<'){
                        //</body>hahaha<a>
                        addToken(TokenType.closeTag);
                        state = State.inLiteral;
                    }else if(chars[index-1]=='>'&&c=='<'){
                        //</body>hahaha<a>
                        addToken(TokenType.closeTag);
                        state = State.openingTag;
                    }else if(index==chars.length-1){
                        //</html>$
                        Token token = new Token();
                        token.startIndex = sectionStart;
                        token.endIndex = index;
                        token.tokenType = TokenType.closeTag;
                        char[] valueChars = new char[index-sectionStart+1];
                        for(int i=sectionStart;i<=index;i++){
                            valueChars[i-sectionStart]=chars[i];
                        }
                        token.value = new String(valueChars);
                        sectionStart = index;
                        tokenList.add(token);
                        break;
                    }
                }break;
                case inLiteral:{
                    if(c=='<'&&chars[index+1]=='/'){
                        addToken(TokenType.literal);
                        state = State.closingTag;
                    }else if(c=='<'){
                        addToken(TokenType.literal);
                        state = State.openingTag;
                    }
                }break;
            }
            index++;
        }
        //TODO 打印出token
        StringBuffer sb = new StringBuffer();
        for(Token token:tokenList){
            sb.append(token.value+",");
        }
        logger.trace(sb.toString());
    }

    /**语义分析*/
    private void parseToken(){
        for(int i=0;i<tokenList.size();i++){
            Token token = tokenList.get(i);
            switch(token.tokenType){
                case openTag:{
                    if(!tokenList.get(i+1).tokenType.equals(TokenType.commentTag)){
                        AbstractElement newElement = new AbstractElement();
                        if(current==null){
                            root = newElement;
                            current = root;
                        }else{
                            newElement.parent = current;
                            newElement.parent.childList.add(newElement);
                        }
                        current = newElement;
                    }
                }break;
                case tagName:{
                    current.tagName = token.value;
                }break;
                case commentTag:{

                }break;
                case attribute:{
                    current.attributes.putAll(AttributeParser.parse(token.value));
                }break;
                case openTagClose:{
                    if(token.value.contains("/>")){
                        current.isSingleNode = true;
                    }
                }break;
                case textContent:{
                    //处理转义字符
                    token.value = token.value.replace("&quot;","\"");
                    token.value = token.value.replace("&amp;","&");
                    token.value = token.value.replace("&lt;","<");
                    token.value = token.value.replace("&gt;",">");
                    token.value = token.value.replace("&nbsp;"," ");
                    current.textContent = token.value;
                }break;
                case closeTag:{
                    current = current.parent;
                }break;
                case literal:{

                }break;
            }
        }
        current = root;
    }

    /**添加Token信息*/
    private void addToken(TokenType tokenType){
        Token token = new Token();
        token.startIndex = sectionStart;
        token.endIndex = index;
        token.tokenType = tokenType;
        if(index==sectionStart){
            token.value = chars[index]+"";
        }else{
            char[] valueChars = new char[index-sectionStart];
            for(int i=sectionStart;i<index;i++){
                valueChars[i-sectionStart]=chars[i];
            }
            token.value = new String(valueChars);
        }
        sectionStart = index;
        tokenList.add(token);
    }

    class Token {
        /**开始下标*/
        public int startIndex;
        /**结束下标*/
        public int endIndex;
        /**Token值*/
        public String value;
        /**Token类型*/
        public TokenType tokenType;
    }

    enum TokenType {
        openTag("开始标签"),
        tagName("标签名称"),
        attribute("标签属性"),
        openTagClose("开始标签结束"),
        textContent("标签文本内容"),
        literal("结束标签与开始标签之间文本"),
        closeTag("结束标签"),
        commentTag("注释标签");

        private String name;

        TokenType(String name) {
            this.name = name;
        }
    }

    enum State {
        /**在开始标签中*/
        openingTag,
        /**在标签名中*/
        inTagName,
        /**在标签属性中*/
        inAttribute,
        /**在开始标签结束标签中*/
        openTagClosing,
        /**在节点文本节点内容中*/
        inTextContent,
        /**HTML中非文本节点的文本*/
        inLiteral,
        /**在关闭标签中*/
        closingTag,
        /**在注释中*/
        inComment;
    }

    class AbstractElement implements Element {
        /**节点名称*/
        private String tagName;
        /**是否是单节点*/
        private boolean isSingleNode;
        /**父节点*/
        private AbstractElement parent;
        /**属性*/
        private Map<String,String> attributes = new HashMap<>();
        /**文本内容*/
        private String textContent = "";
        /**子节点*/
        private List<Element> childList = new ArrayList<>();

        @Override
        public Map<String, String> attribute() {
            return attributes;
        }

        public String tagName() {
            return tagName;
        }

        public String text() {
            return textContent;
        }

        @Override
        public int elementSiblingIndex() {
            if(parent==null){
                return 0;
            }
            for(int i=0;i<parent.childList.size();i++){
                if(parent.childList.get(i)==this){
                    return i;
                }
            }
            return 0;
        }

        @Override
        public String toString(){
            return "<"+tagName+">";
        }
    }
}
