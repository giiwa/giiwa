package org.giiwa.dao;

import static org.junit.Assert.*;

import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.StringFinder;
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
	public void testW1() {
		String s = "ab+c";
		System.out.println(s.matches(".*[\\+\\-*/].*"));
	}

	@Test
	public void testSQL() {

		W q = W.create();
		q.and("date2>0");

		System.out.println(q.toString());

	}

}
