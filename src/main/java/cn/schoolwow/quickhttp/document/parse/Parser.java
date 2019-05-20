package cn.schoolwow.quickhttp.document.parse;

import com.sun.deploy.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Parser {
    private Logger logger = LoggerFactory.getLogger(Parser.class);
    private char[] chars; //输入参数
    private int index = 0; //当前位置
    private int sectionStart=0; //token起始位置
    private State state = State.openingTag;//起始状态
    private List<Token> tokenList = new ArrayList<>(); //Token列表
    private Node root = null;
    private Node current = root;

    public static Node parse(String html){
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
                        Node newNode = new Node();
                        if(current==null){
                            root = newNode;
                            current = root;
                        }else{
                            newNode.parent = current;
                            newNode.parent.childList.add(newNode);
                        }
                        current = newNode;
                    }
                }break;
                case tagName:{
                    current.tagName = token.value;
                }break;
                case commentTag:{

                }break;
                case attribute:{
                    String[] attributeToken = StringUtils.splitString(token.value.replaceAll("\\s+"," ")," ");
                    for(int j=0;j<attributeToken.length;j++){
                        if(attributeToken[j].equals("")){
                            continue;
                        }
                        if(!attributeToken[j].contains("=")){
                            current.attributes.put(attributeToken[j],"");
                        }else{
                            String[] keyValueToken = attributeToken[j].split("=");
                            String key = keyValueToken[0].replaceAll("['|\"]","").trim();
                            String value = keyValueToken[1].replaceAll("['|\"]","").trim();
                            current.attributes.put(key,value);
                        }
                    }
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
}
