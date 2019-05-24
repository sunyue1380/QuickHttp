package cn.schoolwow.quickhttp.document.query;

import org.jsoup.Jsoup;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class QueryParserTest {

    @Test
    public void testEvaluator() {
        Map<String,Class> evaluatorMap = new LinkedHashMap<>();
        evaluatorMap.put("#id",Evaluator.Id.class);
        evaluatorMap.put(".class",Evaluator.Class.class);
        evaluatorMap.put("[attr]",Evaluator.Attribute.class);
        evaluatorMap.put("[^attrPrefix]",Evaluator.AttributeStarting.class);
        evaluatorMap.put("[attr=val]",Evaluator.AttributeWithValue.class);
        evaluatorMap.put("[attr=\"val\"]",Evaluator.AttributeWithValue.class);
        evaluatorMap.put("[attr^=valPrefix]",Evaluator.AttributeWithValueStarting.class);
        evaluatorMap.put("[attr$=valSuffix]",Evaluator.AttributeWithValueEnding.class);
        evaluatorMap.put("[attr*=valContaining]",Evaluator.AttributeWithValueContaining.class);
        evaluatorMap.put("[attr~=regex]",Evaluator.AttributeWithValueMatching.class);
        Set<String> keySet = evaluatorMap.keySet();
        for(String key:keySet){
            Evaluator evaluator = QueryParser.parse(key);
            Assert.assertEquals(evaluatorMap.get(key),evaluator.getClass());
        }
    }

    @Test
    public void testCombination() {
        Map<String,Class> evaluatorMap = new LinkedHashMap<>();
        evaluatorMap.put("div p",StructuralEvaluator.Parent.class);
        evaluatorMap.put("div > p",StructuralEvaluator.ImmediateParent.class);
        evaluatorMap.put("div + p",StructuralEvaluator.ImmediatePreviousSibling.class);
        evaluatorMap.put("div ~ p",StructuralEvaluator.PreviousSibling.class);
        Set<String> keySet = evaluatorMap.keySet();
        for(String key:keySet){
            Evaluator evaluator = QueryParser.parse(key);
            Assert.assertEquals(evaluatorMap.get(key),evaluator.getClass());
        }
    }

    @Test
    public void testOr() {
        Evaluator evaluator = QueryParser.parse("div , p");
        Assert.assertEquals(CombiningEvaluator.Or.class,evaluator.getClass());
    }

    @Test
    public void testPseudo() {
        Map<String,Class> evaluatorMap = new LinkedHashMap<>();
        evaluatorMap.put("div:lt(0)", Evaluator.IndexLessThan.class);
        evaluatorMap.put("div:gt(0)", Evaluator.IndexGreaterThan.class);
        evaluatorMap.put("div:eq(0)", Evaluator.IndexEquals.class);
        evaluatorMap.put("div:has(p)", StructuralEvaluator.Has.class);
        evaluatorMap.put("div:not(p)", StructuralEvaluator.Not.class);
        evaluatorMap.put("div:contains(quickhttp)", Evaluator.ContainsText.class);
        evaluatorMap.put("div:matches(\\\\d+)", Evaluator.Matches.class);
        evaluatorMap.put("div:containsOwn(quickhttp)", Evaluator.ContainsOwnText.class);
        evaluatorMap.put("div:matchesOwn(\\\\d+)", Evaluator.MatchesOwn.class);
        Set<String> keySet = evaluatorMap.keySet();
        for(String key:keySet){
            Evaluator evaluator = QueryParser.parse(key);
            if(evaluator instanceof StructuralEvaluator){
                Assert.assertEquals(evaluatorMap.get(key),evaluator.getClass());
            }else{
                Assert.assertEquals(CombiningEvaluator.And.class,evaluator.getClass());
                CombiningEvaluator.And andEvaluator = (CombiningEvaluator.And) evaluator;
                List<Evaluator> evaluatorList = andEvaluator.evaluatorList;
                Assert.assertEquals(evaluatorMap.get(key),evaluatorList.get(0).getClass());
            }
        }
    }

    @Test
    public void testNth() {
        Map<String,Class> evaluatorMap = new LinkedHashMap<>();
        evaluatorMap.put("div:nth-child(10n-1)", Evaluator.IsNthChild.class);
        evaluatorMap.put("div:nth-last-child(-n+2)", Evaluator.IsNthLastChild.class);
        evaluatorMap.put("div:nth-of-type(2n+1)", Evaluator.IsNthOfType.class);
        evaluatorMap.put("div:nth-last-of-type(2n+1)", Evaluator.IsNthLastOfType.class);
        Set<String> keySet = evaluatorMap.keySet();
        for(String key:keySet){
            Evaluator evaluator = QueryParser.parse(key);
            Assert.assertEquals(CombiningEvaluator.And.class,evaluator.getClass());
            CombiningEvaluator.And andEvaluator = (CombiningEvaluator.And) evaluator;
            List<Evaluator> evaluatorList = andEvaluator.evaluatorList;
            Assert.assertEquals(evaluatorMap.get(key),evaluatorList.get(0).getClass());
        }
    }

    @Test
    public void testJsoup() {
        Jsoup.parse("<div><p></p></div>").select("div,p").size();
    }
}