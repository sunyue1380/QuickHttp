package cn.schoolwow.quickhttp.document.parse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AttributeParser {
    private Logger logger = LoggerFactory.getLogger(AttributeParser.class);
    private char[] chars; //输入参数
    private int index = 0; //当前位置
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
        if(chars[index]==' '){
            state = State.inSpace;
        }else{
            state = State.inKey;
        }
        index++;
        while(index<chars.length){
            switch(state){
                case inSpace:{
                    //<head  id=1>
                    if((chars[index-1]==' '&&chars[index]!=' ')||index==chars.length-1){
                        state = State.inKey;
                        sectionStart = index;
                    }
                }break;
                case inKey:{
                    if((chars[index-1]!=' '&&chars[index]==' ')||index==chars.length-1){
                        //<head disable di>
                        addAttribute(AttributeType.key);
                    }else if(chars[index]=='='){
                        currentKey = new String(chars,sectionStart,index-sectionStart);
                        state = State.equal;
                    }
                }break;
                case equal:{
                    if(chars[index]=='"'||chars[index]=='\''){
                        state = State.inQuoteValue;
                        sectionStart = index;
                    }else if(chars[index]==' '){
                        addAttribute(AttributeType.key);
                    }else{
                        state = State.inValue;
                        sectionStart = index;
                    }
                }break;
                case inValue:{
                    if(chars[index]==' '||index==chars.length-1){
                        state = State.inSpace;
                        addAttribute(AttributeType.keyValue);
                    }
                }break;
                case inQuoteValue:{
                    if(((chars[index-1]=='"'||chars[index-1]=='\'')&&chars[index]==' ')||index==chars.length-1){
                        state = State.inSpace;
                        addAttribute(AttributeType.quoteKeyValue);
                    }
                }break;
            }
            index++;
        }
    }

    private void addAttribute(AttributeType attributeType){
        int count = index-sectionStart;
        if(index==chars.length-1){
            count++;
        }
        String value = new String(chars,sectionStart,count);
        if(value.charAt(value.length()-1)=='='){
            value = value.substring(0,value.length()-1);
        }
        switch(attributeType){
            case key:{
                attributes.put(value,"");
            }break;
            case keyValue:{
                attributes.put(currentKey,value);
            }break;
            case quoteKeyValue:{
                attributes.put(currentKey,value.substring(1,value.length()-1));
            }break;
        }
        sectionStart = index;
    }

    enum AttributeType {
        key,keyValue,quoteKeyValue;
    }

    enum State {
        /**在属性名中*/
        inKey,
        /**在属性值中*/
        inValue,
        /**在引号属性*/
        inQuoteValue,
        /**在空格中*/
        inSpace,
        /**等于符号*/
        equal;
    }
}
