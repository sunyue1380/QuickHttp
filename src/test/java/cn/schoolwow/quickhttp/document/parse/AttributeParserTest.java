package cn.schoolwow.quickhttp.document.parse;

import com.alibaba.fastjson.JSON;
import org.junit.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class AttributeParserTest {

    @Test
    public void parse() {
        String attribute = " single key=value quoteKey=\"QuoteValue\" endKey=endValue";
        Map<String,String> attibuteMap = AttributeParser.parse(attribute);
        Set<String> keySet = attibuteMap.keySet();
        for(String key:keySet){
            String value = attibuteMap.get(key);
            if(value!=null&&!value.isEmpty()){
                System.out.println(key+":"+value);
            }else{
                System.out.println(key);
            }
        }
    }
}