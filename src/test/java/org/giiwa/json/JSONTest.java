package org.giiwa.json;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.giiwa.bean.Temp;
import org.giiwa.dao.Bean;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.X;
import org.giiwa.engine.JS;
import org.giiwa.net.client.Http;
import org.giiwa.task.Console;
import org.giiwa.web.Controller;
import org.junit.Test;

public class JSONTest {

	@Test
	public void test() throws SQLException {

		String ss = "{a:'a',b:1}";
		JSON j = JSON.fromObject(ss);
		System.out.println(j);

		ss = "{a:'a',b:1, c:{a:1, b:'a'}}";
		j = JSON.fromObject(ss);
		j.remove("a");

		System.out.println(j);

		System.out.println(j.get("b").getClass());
		ss = "[{a:'a',b:1}]";

		List<JSON> l1 = JSON.fromObjects(ss);
		System.out.println(l1);
		System.out.println(l1.get(0).get("b").getClass());

		ss = "{\"list\":['333',1.0,2.0,3.0,5.0,7.0,11.0,13.0,17.0,19.0,23.0,29.0,31.0,37.0,41.0,43.0,47.0,53.0,59.0,61.0,67.0,71.0,73.0,79.0,83.0,89.0,97.0]}";
		j = JSON.fromObject(ss);
		System.out.println(j.getObjects("list").iterator().next().getClass());
		System.out.println(j.getObjects("list").iterator().next().getClass());

		ss = "{a:1}";
		System.out.println(JSON.fromObjects(ss));

		String code = "d.test([{a:1, b:[{a:2}, {b:2}]}, {c:[1,2,3]}])";
		JSON d = JSON.create();
		try {
			JS.run(code, JSON.create().append("d", d));
		} catch (Exception e) {
			e.printStackTrace();
		}

		String s = "n=1&filter=b='2009087010230000'";
		JSON j1 = JSON.fromObject(s);

		System.out.println(j1);

		W q = W.create();
		String s1 = j1.getString("filter");
		System.out.println(s1);

		q.and(s1);

		q = W.create();
		q.and(" a='or' and b != 'and' AND c='java'");

		System.out.println(q.toString());

		j1 = JSON.create();
		j1.append("ret.aaa", 1);
		System.out.println(j1.toPrettyString());

		System.out.println("ret.aaa=" + j1.get("ret.aaa"));

		System.out.println("ret1.aaa=" + j1.get("ret1.aaa"));

		j1.append("a", 11);
		String js = "print('j1.a=' + j1.ret.aaa);j1.b = 'aaaa';";
		try {

			JS.run(js, JSON.create().append("j1", j1));
			Object o = j1.get("b");
			JSON j2 = JSON.fromObject(o);
			System.out.println(j2);
			System.out.println(o);

			j1.scan((p, e) -> {
				System.out.println(e + "=" + p.get(e));
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		j1 = JSON.create();
		j1.append("a", 1);
		j1.append("x.b", 2);
		s = "$a a$$x.b aa";
		System.out.println("s=" + s);
		s = j1.parse(s);
		System.out.println("s=" + s);

		j1 = JSON.create();
		j1.append("ret.a", new ArrayList<String>());
		System.out.println(j1.toPrettyString());
		JSON j2 = j1.copy();
		j1.append("ret.a", "1");
		j1.append("ret.a", "2");
		System.out.println(j2.toPrettyString());
		System.out.println(j1.toPrettyString());
	}

	@Test
	public void testHtml() {
		String s1 = "<link rel='icon' href='ab   c' />";
		JSON j1 = JSON.fromObject(s1);
		System.out.println(j1);

		s1 = "<script>window</script>";
		j1 = JSON.fromObject(s1);
		System.out.println(j1);

	}

	@Test
	public void testXml() {

		System.out.println("starting ...");

		System.out.println();

		Console._DEBUG = true;

		try {

			Http h = Http.create();

			String url = "https://www.rand.org/content/rand/about/people/_jcr_content/par/stafflist.xml";
			Http.Response r1 = h.get(url);
//			System.out.println(r1.body);
			JSON j1 = r1.xml();

			System.out.println(j1.toPrettyString());

		} catch (Exception e) {
			e.printStackTrace(System.out);

		}

	}

	@Test
	public void testXml2() {

		System.out.println("starting ...");

		System.out.println();

		try {

			String filename = "/Users/joe/Downloads/X9813809.xml";
			String s = X.IO.read(new File(filename), "UTF8");
			JSON j1 = JSON.fromXml(s);

			System.out.println(j1.toPrettyString());

		} catch (Exception e) {
			e.printStackTrace(System.out);

		}

	}

	@Test
	public void testXml3() {

		System.out.println("starting ...");

		System.out.println();

		try {

			String s = "<msg> <appmsg appid=\"\" sdkver=\"0\"> <title><![CDATA[国务院最新印发！取消和调整33个罚款事项→]]></title> <des><![CDATA[]]></des> <action></action> <type>5</type> <showtype>1</showtype> <content><![CDATA[]]></content> <contentattr>0</contentattr> <url><![CDATA[http://mp.weixin.qq.com/s?__biz=MjM5NzQ5MTkyMA==&mid=2657807483&idx=1&sn=e26bf6a37bcdce1d7409b4c3cab811b7&chksm=bd4769c48a30e0d26ce9bd0d320bd5cec5f52967ecf5206a1c88e1131678d7e07ef8cff5a6da&scene=0&xtrack=1#rd]]></url> <lowurl><![CDATA[]]></lowurl> <appattach> <totallen>0</totallen> <attachid></attachid> <fileext></fileext> </appattach> <extinfo></extinfo> <mmreader> <category type=\"20\" count=\"2\"> <name><![CDATA[央视财经]]></name> <topnew> <cover><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5Qd9Hfd8hIPzwAYF63Q6DVSwZHAHItmHdVfj7GHQm2Eiaps94XN0iaYEAFA/640?wxtype=jpeg&wxfrom=0]]></cover> <width>0</width> <height>0</height> <digest><![CDATA[]]></digest> </topnew> <item> <itemshowtype>0</itemshowtype> <title><![CDATA[国务院最新印发！取消和调整33个罚款事项→]]></title> <url><![CDATA[http://mp.weixin.qq.com/s?__biz=MjM5NzQ5MTkyMA==&mid=2657807483&idx=1&sn=e26bf6a37bcdce1d7409b4c3cab811b7&chksm=bd4769c48a30e0d26ce9bd0d320bd5cec5f52967ecf5206a1c88e1131678d7e07ef8cff5a6da&scene=0&xtrack=1#rd]]></url> <shorturl><![CDATA[]]></shorturl> <longurl><![CDATA[]]></longurl> <pub_time>1698907518</pub_time> <summary><![CDATA[为进一步优化营商环境，国务院日前印发《关于取消和调整一批罚款事项的决定》（以下简称《决定》），取消和调整工业和信息化、住房和城乡建设等领域33个罚款事项，其中，取消16个罚款事项，调整17个罚款事项。]]></summary> <cover><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5Qd9Hfd8hIPzwAYF63Q6DVSwZHAHItmHdVfj7GHQm2Eiaps94XN0iaYEAFA/640?wxtype=jpeg&wxfrom=0]]></cover> <tweetid></tweetid> <digest><![CDATA[]]></digest> <fileid>510323706</fileid> <sources> <source> <name><![CDATA[央视财经]]></name> </source> </sources> <styles></styles> <native_url></native_url> <del_flag>0</del_flag> <contentattr>0</contentattr> <play_length>0</play_length> <play_url><![CDATA[]]></play_url> <voice_id><![CDATA[]]></voice_id> <tid><![CDATA[]]></tid> <nonce_id><![CDATA[]]></nonce_id> <voice_type>0</voice_type><player><![CDATA[]]></player> <music_source>0</music_source> <pic_num>0</pic_num> <vid></vid> <author><![CDATA[]]></author> <recommendation><![CDATA[]]></recommendation> <pic_urls></pic_urls> <multi_picture_cover></multi_picture_cover> <comment_topic_id>3176182808277565442</comment_topic_id> <cover_235_1><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5Qd9Hfd8hIPzwAYF63Q6DVSwZHAHItmHdVfj7GHQm2Eiaps94XN0iaYEAFA/640?wxtype=jpeg&wxfrom=0]]></cover_235_1> <cover_1_1><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5QdAkycp2knXNMrz3SEwsZz9ibwQVXEjaB5uEib5l7WxmmS58lvua3VKafg/300?wxtype=jpeg&wxfrom=0]]></cover_1_1> <cover_16_9><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5Qd9Hfd8hIPzwAYF63Q6DVSwZHAHItmHdVfj7GHQm2Eiaps94XN0iaYEAFA/640?wxtype=jpeg&wxfrom=0]]></cover_16_9> <appmsg_like_type>2</appmsg_like_type> <video_width>0</video_width> <video_height>0</video_height> <is_pay_subscribe>0</is_pay_subscribe> <finder_feed> <object_id><![CDATA[]]></object_id> <object_nonce_id><![CDATA[]]></object_nonce_id> <feed_type>0</feed_type> <nickname><![CDATA[]]></nickname> <avatar><![CDATA[]]></avatar> <desc><![CDATA[]]></desc> <media_count>0</media_count> <media_list> </media_list> <mega_video> <object_id><![CDATA[]]></object_id> <object_nonce_id><![CDATA[]]></object_nonce_id> </mega_video> </finder_feed> <finder_live> <finder_username><![CDATA[]]></finder_username> <category><![CDATA[]]></category> <finder_nonce_id><![CDATA[]]></finder_nonce_id> <export_id><![CDATA[]]></export_id> <nickname><![CDATA[]]></nickname> <head_url><![CDATA[]]></head_url> <desc><![CDATA[]]></desc> <live_status></live_status> <live_source_type_str><![CDATA[]]></live_source_type_str> <ext_flag></ext_flag> <auth_icon_url><![CDATA[]]></auth_icon_url> <auth_icon_type_str><![CDATA[]]></auth_icon_type_str> <media> <cover_url><![CDATA[]]></cover_url> <height></height> <width></width> </media> </finder_live> </item> <item> <itemshowtype>0</itemshowtype> <title><![CDATA[又一国内最大！今天投产→]]></title> <url><![CDATA[http://mp.weixin.qq.com/s?__biz=MjM5NzQ5MTkyMA==&mid=2657807483&idx=2&sn=8dd79f11a04f2c5af187924634549d49&chksm=bd4769c48a30e0d2916ffba7a1345b0788a543cf3c85b053cda435a31b81bebe9e38de5625da&scene=0&xtrack=1#rd]]></url> <shorturl><![CDATA[]]></shorturl> <longurl><![CDATA[]]></longurl> <pub_time>1698907518</pub_time> <summary><![CDATA[供暖季来临，国内天然气保供能力增强。今天（11月2日），我国自主研发国内首座27万立方米液化天然气储罐在青岛投产，这也是国内容积最大的液化天然气储罐，它的投产大幅提升了华北地区天然气供应保障能力。国内]]></summary> <cover><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5QdY4UxRLxPVaZlxLBIV2hwiaNZanng9r0ia1Sz2b4ial6zDWcicZsnUibn3HA/300?wxtype=jpeg&wxfrom=0]]></cover> <tweetid></tweetid> <digest><![CDATA[]]></digest> <fileid>510323832</fileid> <sources> <source> <name><![CDATA[央视财经]]></name> </source> </sources> <styles></styles> <native_url></native_url> <del_flag>0</del_flag> <contentattr>0</contentattr> <play_length>0</play_length> <play_url><![CDATA[]]></play_url> <voice_id><![CDATA[]]></voice_id> <tid><![CDATA[]]></tid> <nonce_id><![CDATA[]]></nonce_id> <voice_type>0</voice_type><player><![CDATA[]]></player> <music_source>0</music_source> <pic_num>0</pic_num> <vid></vid> <author><![CDATA[]]></author> <recommendation><![CDATA[]]></recommendation> <pic_urls></pic_urls> <multi_picture_cover></multi_picture_cover> <comment_topic_id>3176182810274054145</comment_topic_id> <cover_235_1><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5Qdek2icRMaZbBM9HXia69SoKYvOXhq0fSAaV8ZAnuzVQISwkVRINiaPBNxA/300?wxtype=jpeg&wxfrom=0]]></cover_235_1> <cover_1_1><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5QdY4UxRLxPVaZlxLBIV2hwiaNZanng9r0ia1Sz2b4ial6zDWcicZsnUibn3HA/300?wxtype=jpeg&wxfrom=0]]></cover_1_1> <cover_16_9><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5QdY4UxRLxPVaZlxLBIV2hwiaNZanng9r0ia1Sz2b4ial6zDWcicZsnUibn3HA/300?wxtype=jpeg&wxfrom=0]]></cover_16_9> <appmsg_like_type>2</appmsg_like_type> <video_width>0</video_width> <video_height>0</video_height> <is_pay_subscribe>0</is_pay_subscribe> <finder_feed> <object_id><![CDATA[]]></object_id> <object_nonce_id><![CDATA[]]></object_nonce_id> <feed_type>0</feed_type> <nickname><![CDATA[]]></nickname> <avatar><![CDATA[]]></avatar> <desc><![CDATA[]]></desc> <media_count>0</media_count> <media_list> </media_list> <mega_video> <object_id><![CDATA[]]></object_id> <object_nonce_id><![CDATA[]]></object_nonce_id> </mega_video> </finder_feed> <finder_live> <finder_username><![CDATA[]]></finder_username> <category><![CDATA[]]></category> <finder_nonce_id><![CDATA[]]></finder_nonce_id> <export_id><![CDATA[]]></export_id> <nickname><![CDATA[]]></nickname> <head_url><![CDATA[]]></head_url> <desc><![CDATA[]]></desc> <live_status></live_status> <live_source_type_str><![CDATA[]]></live_source_type_str> <ext_flag></ext_flag> <auth_icon_url><![CDATA[]]></auth_icon_url> <auth_icon_type_str><![CDATA[]]></auth_icon_type_str> <media> <cover_url><![CDATA[]]></cover_url> <height></height> <width></width> </media> </finder_live> </item> </category> <publisher> <username><![CDATA[gh_9dc0e48d383a]]></username> <nickname><![CDATA[央视财经]]></nickname> </publisher> <template_header></template_header> <template_detail></template_detail> <forbid_forward>0</forbid_forward> </mmreader> <thumburl><![CDATA[https://mmbiz.qpic.cn/sz_mmbiz_jpg/bPJzOqwPBpbQO9ia8o62ic7MFYDS7tg5Qd9Hfd8hIPzwAYF63Q6DVSwZHAHItmHdVfj7GHQm2Eiaps94XN0iaYEAFA/640?wxtype=jpeg&wxfrom=0]]></thumburl> </appmsg> <fromusername><![CDATA[gh_9dc0e48d383a]]></fromusername> <appinfo> <version></version> <appname><![CDATA[央视财经]]></appname> <isforceupdate>1</isforceupdate> </appinfo> </msg>";
			s = "<msg><appinfo> <version></version><appname><![CDATA[彼岸的鹰]]></appname><isforceupdate>1</isforceupdate> </appinfo> </msg>";

			JSON j1 = JSON.fromXml(s);

			System.out.println(j1.toPrettyString());

		} catch (Exception e) {
			e.printStackTrace(System.out);

		}

	}

	@Test
	public void testParse() {
		JSON j1 = JSON.create();
		j1.append("x", "a,b,c");

		String s1 = j1.parse("var a = $x.split(,).replace(s->'\\'' + s + '\\'').join( + ) + 1");

		System.out.println(s1);

	}

	@Test
	public void testParse2() {
		JSON j1 = JSON.create();
		j1.append("s1", JSON.create().append("temp", "123"));

		String s1 = j1.parse("var s=$s1.temp");

		System.out.println(s1);

	}

	@Test
	public void testObject() {

		Map<String, Object> j1 = new HashMap<String, Object>();
		j1.put("b", this);
		JSON j2 = JSON.fromObject(j1);
		System.out.println(j2.toPrettyString());

		((JSONTest) j2.get("b")).testParse2();

	}

	@Test
	public void testList() {

		JSON j1 = JSON.create();
		List<JSON> l1 = JSON.createList();
		l1.add(JSON.create().append("a", 1));
		j1.put("l1", l1);

		System.out.println(j1.toPrettyString());

		System.out.println(JSON.fromObject(j1.toPrettyString()).toPrettyString());

	}

	@Test
	public void testTemp() {

		try {
			Object s = JS.run("return {a:1, b:2, image:temp}", JSON.create().append("temp", Temp.create("a")));
			List<JSON> l1 = JSON.fromObjects(s);
			System.out.println(l1);

			for (JSON j : l1) {
				V v = V.fromJSON(j);

				Object o = v.value("image");
				v.force("image", "a");
				System.out.println(o.getClass());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testBean() {

		try {
			Object s = JS.run("return {a:1, b:2, image:temp}", JSON.create().append("temp", new Bean()));
			List<JSON> l1 = JSON.fromObjects(s);
			System.out.println(l1);

			for (JSON j : l1) {
				V v = V.fromJSON(j);

				Object o = v.value("image");
				v.force("image", "a");
				System.out.println(o.getClass());
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testArray() {

		byte[] bb = new byte[10];

		JSON j1 = JSON.create();
		j1.append("a", bb);

		System.out.println(j1.get("a").getClass());

		JSON j2 = JSON.fromObject(j1);

		System.out.println(j2.get("a").getClass());

	}

	@Test
	public void testV() {

		JSON j1 = JSON.create();
		j1.append("a", V.ignore);
		JSON j2 = JSON.fromObject(j1);

		System.out.println(j1);
		System.out.println(j2);

		V v = V.fromJSON(j1);
		System.out.println(v);

		System.out.println(v.value("a") == V.ignore);
		System.out.println(v.value("a") == "ignore");

		System.out.println(X.isSame(v.value("a"), null));

	}

	@Test
	public void testNull() {

		JSON j1 = JSON.create();
		j1.append("a", null);
		j1.append("b.a", null);
		System.out.println("j=" + j1.toString());
		System.out.println("j=" + j1.toPrettyString());

		System.out.println("j=" + j1 + ", j.key=" + j1.keySet());

		JSON j2 = JSON.fromObject(j1);
		System.out.println("j=" + j2 + ", j.key=" + j2.keySet());
		System.out.println("j=" + j1 + ", j.key=" + j1.keySet());

		V v = V.fromJSON(j1);
		System.out.println(v);

		j1 = JSON.fromObject("{a:null, b:1}");
		System.out.println(j1 + ", key=" + j1.keySet());

	}

	@Test
	public void testArray2() {

		Bean b = new Bean();
		b.put("a", 1);

		try {
			JS.run("b.aa=[1,2,3]", JSON.create().append("b", b));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		JSON j1 = JSON.fromObject(b);
		System.out.println(j1);

	}

	@Test
	public void testMerge() throws SQLException {

		JSON j1 = JSON.create();

		j1.append("ret.req", new Controller());

//		j1.merge("", JSON.create().append("a", 1));
		j1.append("ret.req", JSON.create().append("a", 1));

		System.out.println(j1);

	}

	@Test
	public void testSerializable() {

		JSON j1 = JSON.create();
		j1.put("a", 1);
		j1.put("a1", "1");
		j1.put("a2", 1.1);
		j1.put("a4", true);
		j1.put("a5", '1');
		j1.put("a6", "1111".getBytes());

		j1.put("file", new File("/a"));
		j1.put("temp", Temp.create("a"));

		System.out.println(j1.toPrettyString());
		JSON j2 = j1.serializable();
		System.out.println(j2.toPrettyString());

	}

	@Test
	public void testAsList2() {

		try {
			String js = "var e={}; e.a=['1,2', '2,3'];e;";
			Object o = JS.run(js);
			System.out.println(o.getClass());
			JSON j1 = JSON.fromObject(o);
			System.out.println(j1.toPrettyString());
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Test
	public void testCases() {

		JSON j1 = JSON.create();
		j1.put("A", "1");
		System.out.println(j1.toPrettyString());
		System.out.println(j1.get("a"));
	}

}
