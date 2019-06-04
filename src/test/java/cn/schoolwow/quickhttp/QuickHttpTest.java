package cn.schoolwow.quickhttp;

import cn.schoolwow.quickhttp.connection.Connection;
import cn.schoolwow.quickhttp.document.Document;
import cn.schoolwow.quickhttp.document.element.Element;
import cn.schoolwow.quickhttp.document.element.Elements;
import cn.schoolwow.quickhttp.response.Response;
import cn.schoolwow.quickhttp.util.Interceptor;
import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLEncoder;

public class QuickHttpTest {
    private Logger logger = LoggerFactory.getLogger(QuickHttp.class);

    @Test
    public void testInterceptor() throws InterruptedException {
        QuickHttp.intercept(new Interceptor() {
            @Override
            public void beforeConnect(Connection connection) {
                logger.info("[beforeConnect]调用了");
            }

            @Override
            public void afterConnection(Connection connection,Response response) {
                logger.info("[afterConnection]调用了");
            }
        });
        QuickHttp.connect("https://www.baidu.com").enqueue(new Response.CallBack() {
            @Override
            public void onResponse(Response response) {
                logger.info("[onResponse方法被调用]");
            }

            @Override
            public void onError(Connection connection,IOException e) {
                logger.info("[onError方法被调用]");
            }
        });
        Thread.sleep(10000);
    }

    @Test
    @Ignore
    public void testBaiDuTieBa() throws IOException, InterruptedException {
        String cookie = "BAIDUID=64FFCF8E9D30F389CF0A983CE116552A:FG=1; TIEBA_USERTYPE=8def7439a5a2c7591006f317;  FP_UID=1f38decd5bcf79518e9e7437789d799e;  STOKEN=11433157625ea05f28eeecfd79a3d434318879f5280d0163a981c0c2c9f3e040; TIEBAUID=7fe8d83cee15d7da3078c7ac;BDUSS=1MWEJ5aml4d0RwcExxQ1p3V0JxV1RacWJPZjJOMk5oV2twblh6czZRUWtFM0ZiQVFBQUFBJCQAAAAAAAAAAAEAAABRNB8mu9DI9MP31MIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACSGSVskhklbaU;";
        QuickHttp.addCookie(cookie,"https://wenku.baidu.com/");

        //判断登录态是否失效
        {
            String api = "http://tieba.baidu.com/dc/common/tbs";
            Connection connection = QuickHttp.connect(api);
            Response response = connection.execute();
            JSONObject result = response.bodyAsJSONObject();
            if(result.getInteger("is_login")==0){
                logger.warn("[百度cookie失效]请更新Cookie!");
                return;
            }
        }
        Elements ass = new Elements();
        //获取贴吧列表
        {
            String api = "http://tieba.baidu.com/f/like/mylike";
            for (int i = 1; i < 2; i++) {
                Connection connection = QuickHttp.connect(api + "?v=" + System.currentTimeMillis() + "&pn=" + i);
                Elements as = connection.execute().parse().select(".forum_table tr:gt(0) td:eq(0) a");
                if (as.size() > 0) {
                    ass.addAll(as);
                }
            }
        }
        if (ass.size() == 0) {
            logger.error("[没有关注贴吧]当前用户未关注任何贴吧!");
            return;
        }

        //开始贴吧签到
        {
            for (Element as : ass) {
                String name = as.text();
                long time = 1000 + (long) (Math.random() * 3000);
                logger.debug("[等待签到]等待[{}]毫秒后签到贴吧[{}]", time, name);
                Thread.sleep(time);

                //获取连续签到天数
                String api = "http://tieba.baidu.com/sign/loadmonth?kw="+ URLEncoder.encode(name,"utf-8")+"&ie=utf-8&t="+Math.random();
                Connection connection = QuickHttp.connect(api);
                JSONObject result = connection.execute().bodyAsJSONObject();
                JSONObject o = result.getJSONObject("data").getJSONObject("sign_user_info");
                logger.debug("[{}]连续签到{}天,总共签到{}天,rank:{}",name,o.getInteger("sign_keep"),o.getInteger("sign_total"),o.getInteger("rank"));

                //判断是否签到过
                api = "http://tieba.baidu.com/mo/m" + as.attr("href").substring(2);
                Document doc = connection.url(api).execute().parse();
                Elements _as = doc.select("body > div > div:nth-child(2) > table > tr > td:nth-child(2) > a");
                if (_as.size() == 0) {
                    logger.info("[{}]该贴吧已经签到过!",name);
                    continue;
                }

                //签到
                api = "http://tieba.baidu.com" + _as.get(0).attr("href");
                doc = connection.url(api).execute().parse();
                if (doc.select("body > div > span").size() > 0) {
                    logger.info("[{}]签到完成!=>{}", name, doc.select("body > div > span").text());
                } else {
                    logger.warn("[{}]签到失败!", name);
                }
            }
        }
    }

    @Test
    @Ignore
    public void testBaiDuWenKu() throws Exception {
        String cookie = "BAIDUID=64FFCF8E9D30F389CF0A983CE116552A:FG=1; TIEBA_USERTYPE=8def7439a5a2c7591006f317;  FP_UID=1f38decd5bcf79518e9e7437789d799e;  STOKEN=11433157625ea05f28eeecfd79a3d434318879f5280d0163a981c0c2c9f3e040; TIEBAUID=7fe8d83cee15d7da3078c7ac;BDUSS=1MWEJ5aml4d0RwcExxQ1p3V0JxV1RacWJPZjJOMk5oV2twblh6czZRUWtFM0ZiQVFBQUFBJCQAAAAAAAAAAAEAAABRNB8mu9DI9MP31MIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAACSGSVskhklbaU;";
        QuickHttp.addCookie(cookie,"https://wenku.baidu.com/");
        //百度文库签到
        {
            String api = "https://wenku.baidu.com/task/submit/signin";
            Connection connection = QuickHttp.connect(api)
                    .referrer("https://wenku.baidu.com/task/browse/daily");
            Response response = connection.execute();
            logger.info("[百度文库签到结果]{}",response.body());
        }
        //获取详细签到
        {
            String api = "https://wenku.baidu.com/task/browse/daily";
            Connection connection = QuickHttp.connect(api)
                    .referrer("https://wenku.baidu.com/task/browse/daily");
            Response response = connection.execute();
            logger.info("[编码格式]{}",response.charset());
            Document doc = response.parse();
            String s = doc.select("#signin > div.bd > div.task-detail > ul").text();
            logger.info("[签到详细信息]:{}",s);
        }
    }

    @Test
    @Ignore
    public void testPCEggs() throws Exception {
        //获取Cookie
        {
            String api = "http://www.pceggs.com/signIn/signIn.aspx";
            String cookie = "CLIENTKEY=2172-7028-4923; CLIENTKEY_ShowLogin=7156-4761-5344; forever.pceggs.com=UserID=sDsxwA4LJ//at15Fyhkvhd3H/8CvzLWU&Time=8+GXHauynj7kQZt4+QlpO6tmYe9Kk9Dw&Date=8+GXHauynj7o5/nCWxV/6A==&Status=KAyeeDyZo6Y=; re.pceggs.com=computerid=0ZiHLhvJ3lTxmyT5iVekZA==&sign=80DF782E5937F00F1FDB3865CB078AE7; ckurl.pceggs.com=ckurl=http://www.pceggs.com/pceggsindex.aspx";
            Connection connection = QuickHttp.connect(api)
                    .header("Cookie", cookie);
            Response response = connection.execute();
            response.close();
            logger.info("[Cookie].ADWASPX7A5C561934E_PCEGGS:{}",response.cookie(".ADWASPX7A5C561934E_PCEGGS").getName());
            Assert.assertTrue(response.hasCookie(".ADWASPX7A5C561934E_PCEGGS"));
        }

        //PC蛋蛋签到
        {
            String api = "http://www.pceggs.com/signIn/signin_ajax.aspx";
            Connection connection = QuickHttp.connect(api)
                    .method(Connection.Method.POST)
                    .ajax()
                    .referrer(api)
                    .followRedirects(false)
                    .data("action", "usersignadd")
                    .data("t", System.currentTimeMillis() + "");
            Response response = connection.execute();
            JSONObject result = response.bodyAsJSONObject();
            logger.info("[PC蛋蛋签到结果]{}",result.toJSONString());
        }
    }
}
