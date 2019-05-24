package cn.schoolwow.quickhttp.document.query;

import cn.schoolwow.quickhttp.util.ValidateUtil;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Pattern;

public class QueryParser {
    private static Logger logger = LoggerFactory.getLogger(QueryParser.class);

    private static final char[] combinators = {',', '>', '+', '~', ' '};
    private static final Map<String,Evaluator> pseudoMap = new HashMap<>();
    static{
        pseudoMap.put(":first-child",new Evaluator.IsFirstChild());
        pseudoMap.put(":last-child",new Evaluator.IsLastChild());
        pseudoMap.put(":first-of-type",new Evaluator.IsFirstOfType());
        pseudoMap.put(":last-of-type",new Evaluator.IsLastOfType());
        pseudoMap.put(":only-child",new Evaluator.IsOnlyChild());
        pseudoMap.put(":only-of-type",new Evaluator.IsOnlyOfType());
        pseudoMap.put(":empty",new Evaluator.IsEmpty());
    }
    private char[] chars;
    private int pos;
    private Evaluator root;
    private static final List<Evaluator> evaluatorList = new ArrayList<>();

    public static Evaluator parse(String cssQuery){
        return new QueryParser(cssQuery).root;
    }

    private QueryParser(String cssQuery){
        evaluatorList.clear();

        chars = cssQuery.toCharArray();
        Selector[] selectors = Selector.values();
        while(pos<chars.length){
            boolean find = false;
            for(Selector selector:selectors){
                int count = selector.condition.apply(chars,pos);
                if(count>0){
                    pos+=count;
                    find = true;
                    break;
                }
            }
            if(!find){
                pos++;
            }
        }
        logger.debug("[原始选择器列表]{}",evaluatorList);
        //从右往左处理Evaluator
        final List<Evaluator> filterEvaluator = new ArrayList<>();
        //处理StructuralEvaluator
        for(int i=evaluatorList.size()-1;i>=0;i--){
            Evaluator evaluator = evaluatorList.get(i);
            if(evaluator instanceof StructuralEvaluator){
                filterEvaluator.add(evaluator);
                i--;
            }else {
                filterEvaluator.add(evaluatorList.get(i));
            }
        }
        evaluatorList.clear();
        evaluatorList.addAll(filterEvaluator);
        filterEvaluator.clear();
        //处理CombiningEvaluator
        for(int i=0;i<evaluatorList.size();i++){
            Evaluator evaluator = evaluatorList.get(i);
            Evaluator nextEvaluator = i<evaluatorList.size()-1?evaluatorList.get(i+1):null;
            if(evaluator instanceof CombiningEvaluator.Or){
                //将它前后的选择器加入进来
                List<Evaluator> subEvaluatorList = new ArrayList<>();
                subEvaluatorList.add(evaluatorList.get(i+1));
                subEvaluatorList.add(evaluatorList.get(i-1));
                filterEvaluator.add(new CombiningEvaluator.Or(subEvaluatorList));
                i++;
            }else if(nextEvaluator==null||!(nextEvaluator instanceof CombiningEvaluator)){
                filterEvaluator.add(evaluatorList.get(i));
            }
        }

        if(filterEvaluator.size()==1){
            root = filterEvaluator.get(0);
        }else{
            root = new CombiningEvaluator.And(filterEvaluator);
        }
    }

    private enum Selector{
        ByIdOrClass((chars,pos)->{
            if(chars[pos]!='#'&&chars[pos]!='.'){
                return 0;
            }
            int last = pos;
            while(last<chars.length-1&&!isCombinators(chars[last])){
                last++;
            }
            String content = new String(chars,pos,last-pos+1);
            if(chars[pos]=='#'){
                Evaluator.Id idEvaluator = new Evaluator.Id(content.substring(1));
                evaluatorList.add(idEvaluator);
                logger.debug("[添加id选择器]{}",idEvaluator);
            }else if(chars[pos]=='.'){
                Evaluator.Class aClassEvaluator = new Evaluator.Class(content.substring(1));
                evaluatorList.add(aClassEvaluator);
                logger.debug("[添加class选择器]{}",aClassEvaluator);
            }
            return content.length();
        }),
        ByTag((chars,pos)->{
            if(!Character.isLetterOrDigit(chars[pos])){
                return 0;
            }
            int last = pos;
            while(last<chars.length-1&&Character.isLetterOrDigit(chars[last])){
                last++;
            }
            String content = new String(chars,pos,last-pos);
            Evaluator.Tag tag = new Evaluator.Tag(content);
            evaluatorList.add(tag);
            logger.debug("[添加tag选择器]{}",tag);
            return content.length();
        }),
        ByAttribute((chars,pos)->{
            if(chars[pos]!='['){
                return 0;
            }
            int last = pos;
            while(last<chars.length-1&&chars[last]!=']'){
                last++;
            }
            ValidateUtil.checkArgument(chars[last]==']',"不合法的属性选择器! pos:"+pos);
            String content = new String(chars,pos,last-pos+1);
            Evaluator evaluator = null;
            if(content.charAt(1)=='^'){
                evaluator = new Evaluator.AttributeStarting(content.substring(2,content.length()-1));
            }else if(content.contains("^=")){
                String[] tokens = content.split("\\^=");
                ValidateUtil.checkArgument(tokens.length==2,"分割属性字符串失败!tokens:"+JSON.toJSONString(tokens));
                evaluator = new Evaluator.AttributeWithValueStarting(tokens[0],tokens[1]);
            }else if(content.contains("$=")){
                String[] tokens = content.split("\\$=");
                ValidateUtil.checkArgument(tokens.length==2,"分割属性字符串失败!tokens:"+JSON.toJSONString(tokens));
                evaluator = new Evaluator.AttributeWithValueEnding(tokens[0],tokens[1]);
            }else if(content.contains("*=")){
                String[] tokens = content.split("\\*=");
                ValidateUtil.checkArgument(tokens.length==2,"分割属性字符串失败!tokens:"+JSON.toJSONString(tokens));
                evaluator = new Evaluator.AttributeWithValueContaining(tokens[0],tokens[1]);
            }else if(content.contains("~=")){
                String[] tokens = content.split("\\~=");
                ValidateUtil.checkArgument(tokens.length==2,"分割属性字符串失败!tokens:"+JSON.toJSONString(tokens));
                evaluator = new Evaluator.AttributeWithValueMatching(tokens[0], Pattern.compile(tokens[1]));
            }else if(content.contains("=")){
                String[] tokens = content.split("=");
                ValidateUtil.checkArgument(tokens.length==2,"分割属性字符串失败!tokens:"+JSON.toJSONString(tokens));
                evaluator = new Evaluator.AttributeWithValue(tokens[0], tokens[1]);
            }else{
                evaluator = new Evaluator.Attribute(content.substring(1,content.length()-1));
            }
            evaluatorList.add(evaluator);
            logger.debug("[添加Attribute选择器]{}",evaluator);
            return content.length();
        }),
        ByCombination((chars,pos)->{
            if(!isCombinators(chars[pos])){
                return 0;
            }
            int last = pos;
            while(last<chars.length-1&&isCombinators(chars[last])){
                last++;
            }
            String content = new String(chars,pos,last-pos);
            Evaluator lastEvaluator = evaluatorList.isEmpty()?null:evaluatorList.get(evaluatorList.size()-1);
            Evaluator evaluator = null;
            if(content.contains(">")){
                evaluator = new StructuralEvaluator.ImmediateParent(lastEvaluator);
            }else if(content.contains("+")){
                evaluator = new StructuralEvaluator.ImmediatePreviousSibling(lastEvaluator);
            }else if(content.contains("~")){
                evaluator = new StructuralEvaluator.PreviousSibling(lastEvaluator);
            }else if(content.contains(",")){
                evaluator = new CombiningEvaluator.Or(null);
            }else if(content.contains(" ")){
                evaluator = new StructuralEvaluator.Parent(lastEvaluator);
            }
            ValidateUtil.checkNotNull(lastEvaluator,"不合法的解析器!value:"+content);
            evaluatorList.add(evaluator);
            logger.debug("[添加Combination选择器]{}",evaluator);
            return content.length();
        }),
        ByPseudoCommon((chars,pos)->{
            String prefix = new String(chars,pos,":first-of-type".length());
            Set<String> keySet = pseudoMap.keySet();
            for(String key:keySet){
                if(prefix.startsWith(key)){
                    evaluatorList.add(pseudoMap.get(key));
                    break;
                }
            }
            //TODO
            return null;
        }),
        ByNth((chars,pos)->{
            String prefix = new String(chars,pos,5);
            if(!prefix.equals(":nth-")){
                return 0;
            }
            int last = pos;
            while(last<chars.length-1&&chars[last]!=')'){
                last++;
            }
            String content = new String(chars,pos,last-pos+1);
            String data = content.substring(content.indexOf("(")+1,content.lastIndexOf(")"));
            String[] tokens = data.split("n");
            int a=-1,b=-1;
            if(tokens.length==1){
                a=0;
                b=Integer.parseInt(tokens[0]);
            }else if(tokens.length==2){
                if(tokens[0].equals("-")){
                    a = -1;
                }else{
                    a=Integer.parseInt(tokens[0]);
                }
                b=Integer.parseInt(tokens[1]);
            }
            if(a<0&&b<0){
                return 0;
            }
            Evaluator structuralEvaluator = null;
            if(content.startsWith(":nth-child(")){
                structuralEvaluator = new StructuralEvaluator.IsNthChild(a,b);
            }else if(content.startsWith(":nth-last-child(")){
                structuralEvaluator = new StructuralEvaluator.IsNthLastChild(a,b);
            }else if(content.startsWith(":nth-of-type(")){
                structuralEvaluator = new StructuralEvaluator.IsNthOfType(a,b);
            }else if(content.startsWith(":nth-last-of-type(")){
                structuralEvaluator = new StructuralEvaluator.IsNthLastOfType(a,b);
            }
            ValidateUtil.checkNotNull(structuralEvaluator,"无法识别的选择器!"+content);
            evaluatorList.add(structuralEvaluator);
            logger.debug("[添加Nth选择器]{}",structuralEvaluator);
            return content.length();
        }),
        ByPseudo((chars,pos)->{
            if(chars[pos]!=':'){
                return 0;
            }
            int last = pos;
            while(last<chars.length-1&&chars[last]!=')'){
                last++;
            }
            String content = new String(chars,pos,last-pos+1);
            String data = content.substring(content.indexOf("(")+1,content.lastIndexOf(")"));
            Evaluator evaluator = null;
            Evaluator lastEvaluator = evaluatorList.isEmpty()?null:evaluatorList.get(evaluatorList.size()-1);
            if(content.contains(":lt")){
                evaluator = new Evaluator.IndexLessThan(Integer.parseInt(data));
            }else if(content.contains(":gt")){
                evaluator = new Evaluator.IndexGreaterThan(Integer.parseInt(data));
            }else if(content.contains(":eq")){
                evaluator = new Evaluator.IndexEquals(Integer.parseInt(data));
            }else if(content.contains(":has")){
                evaluator = new StructuralEvaluator.Has(lastEvaluator);
            }else if(content.contains(":not")){
                evaluator = new StructuralEvaluator.Not(lastEvaluator);
            }else if(content.contains(":containsOwn")){
                evaluator = new StructuralEvaluator.ContainsOwnText(data);
            }else if(content.contains(":matchesOwn")){
                evaluator = new StructuralEvaluator.MatchesOwn(Pattern.compile(data));
            }else if(content.contains(":contains")){
                evaluator = new StructuralEvaluator.ContainsText(data);
            }else if(content.contains(":matches")){
                evaluator = new StructuralEvaluator.Matches(Pattern.compile(data));
            }
            ValidateUtil.checkNotNull(evaluator,"不合法的选择器!value:"+content);
            evaluatorList.add(evaluator);
            logger.debug("[添加伪类选择器]{}",evaluator);
            return content.length();
        });
        private BiFunction<char[],Integer,Integer> condition;

        Selector(BiFunction<char[], Integer, Integer> condition) {
            this.condition = condition;
        }
    }

    private static boolean isCombinators(char c){
        for(char combinator:combinators){
            if(c==combinator){
                return true;
            }
        }
        return false;
    }
}
