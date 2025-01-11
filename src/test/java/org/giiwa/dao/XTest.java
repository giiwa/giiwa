package org.giiwa.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.giiwa.dao.Helper.W;
import org.giiwa.engine.JS;
import org.giiwa.json.JSON;
import org.giiwa.task.Task;
import org.junit.Test;

public class XTest {

	@Test
	public void test() {
		String s = "a-c";
		char[] ss = X.range2(s, "-");
		System.out.println(Arrays.toString(ss));

		System.out.println(X.toLong("9700262001", -1));
	}

	@Test
	public void testTo() {
		double d = 6.462212122;
		System.out.println(X.toFloat(d, 0));
	}

	@Test
	public void testTime() {

		System.out.println(X.time("10m"));
		System.out.println(X.time("~10m"));

		System.out.println(X.time("10m-15m"));

	}

	@Test
	public void testClone() {
		JSON j1 = JSON.create();
		j1.append("a", JSON.create().append("b", 1));

		JSON j2 = X.clone(j1);

		Task t1 = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				// TODO Auto-generated method stub

			}

		};

		JSON j4 = j1.append("_task", t1);
		JSON j3 = X.clone(j4);

		System.out.println("j1=" + j1);
		System.out.println("j2=" + j2);
		System.out.println("j3=" + j3.get("_task"));
		System.out.println("j4=" + j4);

		System.out.println("t1=" + t1);
		Task t2 = X.clone(t1);

		System.out.println("t2=" + t2);

	}

	@Test
	public void testToLong() {
		String d = "1.63E+12";
		System.out.println(X.toLong(X.toDouble(d)));

		d = "二十";
		System.out.println(d + "=>" + X.toLong(d));

	}

	@Test
	public void isSame() {

		byte[] s1 = new byte[] { 78, 94, 115, -92, -59, 125, 89, -116, 127, 25, -66, 108, 88, -11, 71, -9, -34, -5,
				-110, 54 };
		byte[] s2 = new byte[] { 78, 94, 115, -92, -59, 125, 89, -116, 127, 25, -66, 108, 88, -11, 71, -9, -34, -5,
				-110, 54 };

		System.out.println(X.isSame(s1, s2));

	}

	@Test
	public void testToLong1() {
		double d = 11.5;
		System.out.println(X.toLong(d));
	}

	@Test
	public void testInt() {
		String s = "1212131122122222122212";
		System.out.println(X.toInt(s, 0));

		byte[] a = new byte[] { 0, 0, 0, 1 };
		System.out.println(X.toInt(a));

	}

	@SuppressWarnings("rawtypes")
	@Test
	public void testAsList() {

		try {
			String js = "var l1=[1,2,3,[1,2]];l1;";
			Object o = JS.run(js);
			System.out.println(o.getClass());
			List l2 = X.asList(o, e1 -> e1);
			System.out.println(l2.size());
		} catch (Exception e) {
			e.printStackTrace();
		}

//		X.asList(o, e1->e1);

	}

	@Test
	public void testIsSame() {

		List<String> l1 = new ArrayList<String>();
		List<String> l2 = new ArrayList<String>();

		l1.add("a");
		l1.add("b");

		l2.add("b");
		l2.add("a");

		System.out.println(X.isSame(l1, l2));

	}

	@Test
	public void testSize() {

		String s = "4g";
		System.out.println(X.inst.size(s));

	}

	@Test
	public void testCompare() {

		List<String> group = Arrays.asList("jd_cate.keyword");
		List<String> func = Arrays.asList("sum(price_month)");

		int len = group.size() - 1;

		String s = "{a:[{\"jd_cate.keyword\":\"中小学教辅\",\"sum(price_month)\":4871972896}, {\"jd_cate.keyword\":\"书法\",\"sum(price_month)\":80896431}, {\"jd_cate.keyword\":\"传记\",\"sum(price_month)\":99886665}, {\"jd_cate.keyword\":\"体育/运动\",\"sum(price_month)\":30560059}, {\"jd_cate.keyword\":\"健身与保健\",\"sum(price_month)\":120248413}, {\"jd_cate.keyword\":\"其它\",\"sum(price_month)\":12302969}, {\"jd_cate.keyword\":\"农业/林业\",\"sum(price_month)\":16467342}, {\"jd_cate.keyword\":\"动漫\",\"sum(price_month)\":149584634}, {\"jd_cate.keyword\":\"励志与成功\",\"sum(price_month)\":272031622}, {\"jd_cate.keyword\":\"医学\",\"sum(price_month)\":223166539}, {\"jd_cate.keyword\":\"历史\",\"sum(price_month)\":300128464}, {\"jd_cate.keyword\":\"哲学/宗教\",\"sum(price_month)\":184007070}, {\"jd_cate.keyword\":\"国学/古籍\",\"sum(price_month)\":226641299}, {\"jd_cate.keyword\":\"外语学习\",\"sum(price_month)\":460549652}, {\"jd_cate.keyword\":\"大中专教材教辅\",\"sum(price_month)\":367321614}, {\"jd_cate.keyword\":\"套装书\",\"sum(price_month)\":3374621}, {\"jd_cate.keyword\":\"娱乐/休闲\",\"sum(price_month)\":22015192}, {\"jd_cate.keyword\":\"婚恋与两性\",\"sum(price_month)\":7601249}, {\"jd_cate.keyword\":\"孕产/胎教\",\"sum(price_month)\":18337221}, {\"jd_cate.keyword\":\"字典词典/工具书\",\"sum(price_month)\":113398254}, {\"jd_cate.keyword\":\"家居\",\"sum(price_month)\":15565917}, {\"jd_cate.keyword\":\"小说\",\"sum(price_month)\":737267032}, {\"jd_cate.keyword\":\"工业技术\",\"sum(price_month)\":60158563}, {\"jd_cate.keyword\":\"建筑\",\"sum(price_month)\":32790935}, {\"jd_cate.keyword\":\"心理学\",\"sum(price_month)\":148526919}, {\"jd_cate.keyword\":\"摄影\",\"sum(price_month)\":20238277}, {\"jd_cate.keyword\":\"政治/军事\",\"sum(price_month)\":399522698}, {\"jd_cate.keyword\":\"文化\",\"sum(price_month)\":63685945}, {\"jd_cate.keyword\":\"文化用品\",\"sum(price_month)\":74952}, {\"jd_cate.keyword\":\"文学\",\"sum(price_month)\":480141680}, {\"jd_cate.keyword\":\"旅游/地图\",\"sum(price_month)\":35697655}, {\"jd_cate.keyword\":\"日文图书\",\"sum(price_month)\":15482}, {\"jd_cate.keyword\":\"时尚/美妆\",\"sum(price_month)\":8747251}, {\"jd_cate.keyword\":\"杂志/期刊\",\"sum(price_month)\":149093}, {\"jd_cate.keyword\":\"法律\",\"sum(price_month)\":154299931}, {\"jd_cate.keyword\":\"港台图书\",\"sum(price_month)\":1270830}, {\"jd_cate.keyword\":\"烹饪/美食\",\"sum(price_month)\":36077199}, {\"jd_cate.keyword\":\"电子与通信\",\"sum(price_month)\":8548301}, {\"jd_cate.keyword\":\"社会科学\",\"sum(price_month)\":117933244}, {\"jd_cate.keyword\":\"科学与自然\",\"sum(price_month)\":43187889}, {\"jd_cate.keyword\":\"科普读物\",\"sum(price_month)\":125484781}, {\"jd_cate.keyword\":\"童书\",\"sum(price_month)\":4024063312}, {\"jd_cate.keyword\":\"管理\",\"sum(price_month)\":178999316}, {\"jd_cate.keyword\":\"经济\",\"sum(price_month)\":100627317}, {\"jd_cate.keyword\":\"绘画\",\"sum(price_month)\":125002364}, {\"jd_cate.keyword\":\"考试\",\"sum(price_month)\":1125041860}, {\"jd_cate.keyword\":\"育儿/家教\",\"sum(price_month)\":173209985}, {\"jd_cate.keyword\":\"艺术\",\"sum(price_month)\":160513137}, {\"jd_cate.keyword\":\"计算机与互联网\",\"sum(price_month)\":121126097}, {\"jd_cate.keyword\":\"赠品\",\"sum(price_month)\":792}, {\"jd_cate.keyword\":\"进口原版\",\"sum(price_month)\":1477835}, {\"jd_cate.keyword\":\"金融与投资\",\"sum(price_month)\":64954773}, {\"jd_cate.keyword\":\"青春文学\",\"sum(price_month)\":311581998}, {\"jd_cate.keyword\":\"音乐\",\"sum(price_month)\":100800518}]}";
		JSON j1 = JSON.fromObject(s);
		List<JSON> l1 = j1.getList("a");
		System.out.println(JSON.toPrettyString(l1));
		System.out.println("size=" + l1.size());

		System.out.println("len=" + len);

		try {
			Collections.sort(l1, new Comparator<JSON>() {

				@Override
				public int compare(JSON o1, JSON o2) {
					for (int i = 0; i < len; i++) {
						String name = group.get(i);

						int c = X.compareTo(o1.get(name), o2.get(name));
						if (c != 0) {
							return c;
						}
					}

					for (String name : func) {
						Object c1 = o1.get(name);
						Object c2 = o2.get(name);

						int c = X.compareTo(c1, c2);
						System.out.println(c1 + "," + c2 + "," + c);
						if (c != 0) {
							return -c;
						}
					}

					return 0;
				}

			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		System.out.println("size=" + l1.size());
	}

	@Test
	public void testWeight() {

		W q = W.create();
		q.and(Arrays.asList("cip_firstbookname", "ca_keyword", "cip_contentsummary"), "aa",
				Arrays.asList(100, 100, 50));
		System.out.println(q.toSQL());

	}

	@Test
	public void testToString() {

		Object o = new Exception("ok");
		String s = X.toString(o);
		System.out.println(s);

	}

	@Test
	public void testSplit() {

		String s = "/i/aa2/html";
		String[] ss = X.split(s, "/");
		System.out.println(ss.length);

	}

	@Test
	public void testIsNumber() {

		String s = "";
		System.out.println(s + "=" + X.isNumber(s));

		s = null;
		System.out.println(s + "=" + X.isNumber(s));

		s = "1";
		System.out.println(s + "=" + X.isNumber(s));
	}

	@Test
	public void testDouble() {

		double d1 = 0.1d;
		float d2 = 0.2f;

		System.out.println(X.isSame(d1, d2));
		d2 = 0.1f;
		System.out.println(X.isSame(d1, d2));

	}

	@Test
	public void testisIn() {

		Object[] value = new Object[] { true, null };
		System.out.println(X.isIn(true, value) + ", " + X.isIn(null, value));

		System.out.println(X.isIn("text", "text\\[\\]"));

	}

	@Test
	public void testFilename() {

		String name = "/aa/?A12*:2<\"2>";
		String s = X.filename(name);
		System.out.println(s);

	}

}
