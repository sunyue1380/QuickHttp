package cn.schoolwow.quickhttp.document.parse;

import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AttributeParser {
    private Logger logger = LoggerFactory.getLogger(AttributeParser.class);
    private char[] chars; //输入参数
    private int pos = 0; //当前位置
    private int sectionStart=0; //token起始位置
    private AttributeParser.State state;//起始状态
    private Map<String,String> attributes = new HashMap<>();
    private String currentKey;

    public static Map<String,String> parse(String attribute){
        return new AttributeParser(attribute).attributes;
    }

    private AttributeParser(String attribute){
        this.chars = attribute.toCharArray();
        parseAttribute();
    }

    /**词法分析*/
    private void parseAttribute(){
        //判断初始状态
        if(chars[pos]==' '){
            state = State.inSpace;
        }else{
            state = State.inKey;
        }
        pos++;
        while(pos<chars.length){
            switch(state){
                case inSpace:{
                    if(isKeyValueStart()){
                        if(isLastEqual()){
                            state = State.inValue;
                            sectionStart = pos;
                        }else{
                            if(currentKey!=null){
                                addAttribute(AttributeType.key);
                            }
                            state = State.inKey;
                            sectionStart = pos;
                        }
                    }else if(chars[pos]=='='){
                        state = State.inEqual;
                    }else if(isQuoteStartEnd()){
                        state = State.inQuoteStart;
                        sectionStart = pos;
                    }else if(pos==chars.length-1){
                        addAttribute(AttributeType.key);
                    }
                }break;
                case inKey:{
                    if(pos==chars.length-1){
                        currentKey = new String(chars,sectionStart,pos-sectionStart);
                        addAttribute(AttributeType.key);
                    }else if(chars[pos]==' '){
                        currentKey = new String(chars,sectionStart,pos-sectionStart);
                        state = State.inSpace;
                    }else if(chars[pos]=='='){
                        currentKey = new String(chars,sectionStart,pos-sectionStart);
                        state = State.inEqual;
                    }
                }break;
                case inEqual:{
                    if(chars[pos]==' '){
                        state = State.inSpace;
                    }else if(isKeyValueStart()){
                        state = State.inValue;
                        sectionStart = pos;
                    }else if(isQuoteStartEnd()){
                        state = State.inQuoteStart;
                        sectionStart = pos;
                    }
                }break;
                case inValue:{
                    if(chars[pos]==' '||pos==chars.length-1){
                        state = State.inSpace;
                        addAttribute(AttributeType.keyValue);
                    }
                }break;
                case inQuoteStart:{
                    if(pos==chars.length-1){
                        addAttribute(AttributeType.quoteKeyValue);
                    }else if(isQuoteStartEnd()){
                        state = State.inQuoteEnd;
                    }
                }break;
                case inQuoteEnd:{
                    if(pos==chars.length-1){
                        addAttribute(AttributeType.quoteKeyValue);
                    }else if(chars[pos]==' '){
                        state = State.inSpace;
                        addAttribute(AttributeType.quoteKeyValue);
                    }
                }break;
            }
            pos++;
        }
        logger.debug("[属性列表]{}", JSON.toJSONString(attributes));
    }

    private void addAttribute(AttributeType attributeType){
        int count = pos-sectionStart;
        if(pos==chars.length-1){
            count++;
        }
        String value = new String(chars,sectionStart,count);
        if(value.charAt(value.length()-1)=='='){
            value = value.substring(0,value.length()-1);
        }
        switch(attributeType){
            case key:{
                attributes.put(value.trim(),"");
            }break;
            case keyValue:{
                attributes.put(currentKey.trim(),value);
            }break;
            case quoteKeyValue:{
                attributes.put(currentKey.trim(),value.substring(1,value.length()-1));
            }break;
        }
        currentKey = null;
        sectionStart = pos;
    }

    private boolean isLastEqual(){
        if(pos==0){
            return false;
        }
        int last = pos-1;
        while(last>0&&chars[last]==' '){
            last--;
        }
        return chars[last]=='=';
    }

    private boolean isQuoteStartEnd(){
        return chars[pos]=='"'||chars[pos]=='\'';
    }

    private boolean isKeyValueStart(){
        return chars[pos]=='_'||Character.isLetterOrDigit(chars[pos]);
    }

    private enum AttributeType {
        key,keyValue,quoteKeyValue;
    }

    private enum State {
        /**在属性名中*/
        inKey,
        /**在属性值中*/
        inValue,
        /**引号开始*/
        inQuoteStart,
        /**引号结束*/
        inQuoteEnd,
        /**在空格中*/
        inSpace,
        /**等于符号*/
        inEqual;
    }
}
