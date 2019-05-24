package cn.schoolwow.quickhttp.document.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class HTMLParser {
    private Logger logger = LoggerFactory.getLogger(HTMLParser.class);
    private static final String[] singleNodeList = {"br","hr","img","input","param","meta","link","!doctype"};
    private char[] chars; //输入参数
    private int pos = 0; //当前位置
    private int sectionStart=0; //token起始位置
    private boolean singleNode; //是否是单节点标签
    private boolean isInScript; //是否在脚本中
    private State state = State.openingTag;//起始状态
    private List<HTMLToken> tokenList = new ArrayList<>(); //Token列表
    
    public static List<HTMLToken> parse(String html){
        return new HTMLParser(html).tokenList;
    }
    
    private HTMLParser(String html){
        chars = html.toCharArray();
        parseHTML();
    }
    
    /**词法分析*/
    private void parseHTML(){
        while(pos<chars.length){
            switch(state){
                case openingTag:{
                    if(chars[pos]=='!'&&chars[pos+1]=='-'&&chars[pos+2]=='-'){
                        //<!--comment-->
                        addToken(HTMLToken.TokenType.openTag);
                        state = State.inComment;
                    }else if(pos>0&&chars[pos-1]=='<'){
                        //<body
                        addToken(HTMLToken.TokenType.openTag);
                        state = State.inTagName;
                    }
                }break;
                case inTagName:{
                    if(chars[pos]==' '){
                        //<body id="identify">
                        addToken(HTMLToken.TokenType.tagName);
                        String tagName = tokenList.get(tokenList.size()-1).value.toLowerCase();
                        if(isSingleNode(tagName)){
                            singleNode = true;
                        }else {
                            singleNode = false;
                        }
                        state = State.inAttribute;
                    }else if(chars[pos]=='>'){
                        //<body> <input> <br/>
                        addToken(HTMLToken.TokenType.tagName);
                        String tagName = tokenList.get(tokenList.size()-1).value.toLowerCase();
                        if(isSingleNode(tagName)){
                            singleNode = true;
                            state = State.closingTag;
                        }else {
                            singleNode = false;
                            state = State.openTagClosing;
                        }
                    }else if(chars[pos]=='/'&&chars[pos+1]=='>'){
                        addToken(HTMLToken.TokenType.tagName);
                        state = State.closingTag;
                    }
                }break;
                case inComment:{
                    //<!--comment-->
                    if(chars[pos]=='>'&&chars[pos-1]=='-'&&chars[pos-2]=='-'){
                        addToken(HTMLToken.TokenType.commentTag);
                        singleNode = true;
                        state = State.closingTag;
                    }
                }break;
                case inAttribute:{
                    if(chars[pos]=='>'){
                        addToken(HTMLToken.TokenType.attribute);
                        state = singleNode? State.closingTag: State.openTagClosing;
                    }else if(chars[pos]=='/'&&chars[pos+1]=='>'){
                        addToken(HTMLToken.TokenType.attribute);
                        state = State.closingTag;
                    }
                }break;
                case openTagClosing:{
                    //<input>
                    if(chars[pos-1]=='>'&&chars[pos]!='<'){
                        //<body>text</body>
                        addToken(HTMLToken.TokenType.openTagClose);
                        state = State.inTextContent;
                    }else if(chars[pos]=='<'&&chars[pos+1]=='/'){
                        //<body></body>
                        addToken(HTMLToken.TokenType.openTagClose);
                        state = State.closingTag;
                    }else if(chars[pos]=='<'){
                        //<body><p></p>
                        addToken(HTMLToken.TokenType.openTagClose);
                        state = State.openingTag;
                    }
                }break;
                case inTextContent:{
                    if(isInScript){
                        if(new String(chars,pos,"</script>".length()).equals("</script>")){
                            //<script>var s = /<div></div>/.replaceAll('')</script>
                            addToken(HTMLToken.TokenType.textContent);
                            state = State.closingTag;
                        }
                    }else if(chars[pos]=='<'&&chars[pos+1]=='/'){
                        //<body>textContent</body>
                        addToken(HTMLToken.TokenType.textContent);
                        state = State.closingTag;
                    }else if(chars[pos]=='<'){
                        //<body>textContent<p></p>
                        addToken(HTMLToken.TokenType.textContent);
                        state = State.openingTag;
                    }
                }break;
                case closingTag:{
                    if(chars[pos-1]=='>'&&chars[pos]=='<'&&chars[pos+1]=='/'){
                        //</body></html>
                        addToken(HTMLToken.TokenType.closeTag);
                    }else if(chars[pos-1]=='>'&&chars[pos]!='<'){
                        //</body>  </html>
                        addToken(HTMLToken.TokenType.closeTag);
                        state = State.inLiteral;
                    }else if(chars[pos-1]=='>'&&chars[pos]=='<'){
                        //</body><script>
                        addToken(HTMLToken.TokenType.closeTag);
                        state = State.openingTag;
                    }else if(pos==chars.length-1){
                        //</html>$
                        addToken(HTMLToken.TokenType.closeTag);
                        break;
                    }
                }break;
                case inLiteral:{
                    if(chars[pos]=='<'&&chars[pos+1]=='/'){
                        //</body> </html>
                        addToken(HTMLToken.TokenType.literal);
                        state = State.closingTag;
                    }else if(chars[pos]=='<'){
                        //</body>   <p>
                        addToken(HTMLToken.TokenType.literal);
                        state = State.openingTag;
                    }
                }break;
            }
            pos++;
        }
        logger.debug("[Token列表]{}",tokenList.toString());
    }

    /**添加Token信息*/
    private void addToken(HTMLToken.TokenType tokenType){
        HTMLToken token = new HTMLToken();
        token.start = sectionStart;
        token.end = pos;
        token.tokenType = tokenType;
        if(pos==sectionStart){
            token.value = chars[pos]+"";
        }else{
            int count = token.end-token.start;
            if(pos==chars.length-1){
                count++;
            }
            token.value = new String(chars,token.start,count);
        }
        if(tokenType.equals(HTMLToken.TokenType.tagName)&&token.value.equals("script")){
            isInScript = true;
        }
        if(tokenType.equals(HTMLToken.TokenType.closeTag)&&token.value.equals("</script>")){
            isInScript = false;
        }
        sectionStart = pos;
        tokenList.add(token);
    }

    /**当前节点是否是单节点*/
    private static boolean isSingleNode(String tagName){
        for(String singleNode:singleNodeList){
            if(tagName.equals(singleNode)){
                return true;
            }
        }
        return false;
    }

    private enum State {
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
