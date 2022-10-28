package org.giiwa.dao;

import static org.junit.Assert.*;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.giiwa.dao.Helper.W;
import org.giiwa.misc.StringFinder;
import org.giiwa.web.Language;
import org.junit.Test;

public class SQLTest {

	@Test
	public void test() {
		try {
			String s = "a>10/2*5 and b>11 and (c>1 or r>2)";
			W q = SQL.where2W(StringFinder.create(s));
			System.out.println(q);

			// SQL.query(null, "select *");
//		JSON q1 = SQL.sql(StringFinder.create("select a,b,c from gi_oplog order by a limit 10"));
//		System.out.println(q1);

			// SQL.query(h, sql);
			s = "11 and (1 or 2) and !12";

			q = SQL.where2W("a", StringFinder.create(s));
			System.out.println(q);

			q = W.create();
			q.and("cate='J1|J3/7'");
			System.out.println(q);
			q.scan(e -> {
				if (X.isSame("cate", e.name)) {
					if (e.value.toString().indexOf("/") > -1) {
//					e.remove();
						String[] ss = X.range(e.value.toString(), "/");
						e.replace(ss);
					}
				}
			});
			System.out.println(q);

			s = "a='a(b)'";
			q = SQL.where2W(StringFinder.create(s));
			System.out.println(q);

			s = "a=NULL and b='a'";
			q = SQL.where2W(StringFinder.create(s));
			System.out.println(q.query());

			s = "a='1' and b='2' order by c desc";
			q = SQL.where2W(s);
			System.out.println(q);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

	@Test
	public void testOrder() {

		String word = "pinyin !like 'hz' order by aaa desc";

		try {
			W q = SQL.where2W(word);
			System.out.println(q.toSQL());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testW1() {
		String s = "ab+c";
		System.out.println(s.matches(".*[\\+\\-*/].*"));
	}

	@Test
	public void testSQL() {

		W q = W.create();
//		q.and("date2>0");
//
//		System.out.println(q.toString());
//
//		q = W.create();
//		q.and("syts_2='0'");
//		System.out.println(q.toString());
//
//		q = W.create();
//		q.and("syts_2=\"0\"");
//		System.out.println(q.toString());
//
//		q = W.create();
//		q.and("sort a desc");
//		System.out.println(q.toString());

		q = W.create().and("type='PDF'");
		System.out.println(q);

	}

	@Test
	public void testDate() {

		W q = W.create();
		q = W.create().and("updated>$lang.parse('2022-01','yyyy-MM')");
		q.scan(e -> {
			if (X.isSame(e.name, "updated")) {
				e.value = new Timestamp(X.toLong(e.value));
			}
		});

		System.out.println(q);

	}

	@Test
	public void testSQL2() {
		W q = W.create();
		q.and("datastatus !=2");
		q.and("publisher", Arrays.asList("上海世界图书出版公司", "世界图书出版公司", "世界图书出版公司北京公司", "世界图书出版广东有限公司", "世界图书出版有限公司北京分公司",
				"世界图书出版西安公司", "世界图书出版西安有限公司", "广东世界图书出版公司"));
		q.and("printdegree='1' or selecttype ='newbook' or selecttype = 'reversion'");
		System.out.println(q.query());
	}

	@Test
	public void testSQL3() {
		W q = W.create();
		String name = "updated>$time.add('-7d').time";
		try {
			q = SQL.where2W(StringFinder.create(name));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(q);
	}

	@Test
	public void testSQL4() {
		try {
			String name = "pinyin !like 'hz' and pinyin !like 'a'";
			W q = SQL.where2W(StringFinder.create(name));
			System.out.println(q);
			System.out.println(q.query());

			name = "pinyin !like 'hz' and pinyin like 'a'";
			q = SQL.where2W(StringFinder.create(name));
			System.out.println(q);
			System.out.println(q.query());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testSQL5() {
		try {
			String name = "updated>=-9223372036854775808 and month>=20220000";
			W q = SQL.where2W(StringFinder.create(name));
			System.out.println(q);
			System.out.println(q.query());

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testtodate() {
		String s = "createdate>=\"2022-01-01\" and createdate<\"2022-10-01\"";
		try {
			Language lang = Language.getLanguage("zh_cn");
			W q = SQL.where2W(s);
			System.out.println(q);
			q.scan(e -> {
				e.value = lang.parse(e.value.toString(), "yyyy-MM-dd");
			});
			System.out.println(q);

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void testSortKey() {
		try {
			String sql = "updatetime=1";
			W q = SQL.where2W(sql);
			System.out.println(q);

			List<LinkedHashMap<String, Object>> l1 = q.sortkeys();

			System.out.println(l1);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testW2() {

		W q = W.create();
		q.and("tag='jd' or tag='tianmao'").and("tab!=0 and tab!=1 and tab!=2 and tab!=4").and("type='A'")
				.sort("cip_createdate", -1);

		System.out.println(q.toSQL());
	}

	@Test
	public void testNull() {
		String s = "a!=null or b!=null";
		W q = W.create();
		q.and(s);
		System.out.println(q.toSQL());

		s = "a!=null and b!=null";
		q = W.create();
		q.and(s);
		System.out.println(q.toSQL());

		s = "a!=null or b!='1'";
		q = W.create();
		q.and(s);
		System.out.println(q.toSQL());

		s = "a!=null and b!='1'";
		q = W.create();
		q.and(s);
		System.out.println(q.toSQL());

	}
}
