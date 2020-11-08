package org.giiwa.dao;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.giiwa.dao.Helper.W;
import org.giiwa.engine.JS;
import org.giiwa.json.JSON;
import org.junit.Test;

public class HelperTest {

	@Test
	public void test() {
		W q = W.create();
		q.and("a=b").and(W.create("b", "c"));

		System.out.println(q.toString());
//		System.out.println(q.sortkeys());

//		q.scan(e -> {
//			if (X.isSame(e.name, "a")) {
//				System.out.println(e.name);
//				e.container().and(W.create("a", "1").or("a", "2"));
//				e.remove();
//			}
//
//		});

		JSON m = JSON.create();
		m.append("q", q);
		String js = "q.scan(function(e) {if(e.name=='b') {e.name='a,b,c';}})";
		try {
			JS.run(js, m);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());

		}

		System.out.println(q.toString());

		q = W.create();
		q.and("a", "1").and("b", null);

		q = W.create();
		q.and("a>=1").and("a>'1'");
		System.out.println("q=" + q.toString());

		q.and(W.create("b", 1).or("b", 2));

		q.sort("a").sort("b", -1);
		String sql = q.toSQL();
		System.out.println("sql=" + sql);
		q = W.create();
		q.and(sql);
		sql = q.toSQL();
		System.out.println("sql=" + sql);

		q = W.create();
		q.and("a='1' and a!='2'");
		System.out.println("q=" + q);

		q = W.create();
		q.and("a like '1' and b !like '2'");
		System.out.println("q=" + q);
//		System.out.println("q.where=" + q.where());
//		System.out.println("q.query=" + q.query());

		q.scan(e -> {
			if (X.isSame(e.name, "b")) {
				e.name = "c,d";
				e.replace(Arrays.asList("1", "2"));
//				e.value = Arrays.asList("1", "2");
			}
		});
		System.out.println("q=" + q);

		q = W.create();
		q.and(W.create("a", 1));
		q.or(W.create("b", 2));
		System.out.println("q=" + q.toString());

		q = W.create();
		q.and(" cip_publisher='学苑出版社' AND ( cip_firstbookname like 孙子兵法 ) and ( cip_isbn like )");
		System.out.println("q=" + q.toString());
		q.scan(e -> {
			if (X.isSame(e.name, "cip_firstbookname")) {
				e.name = "cip_firstbookname,cip_secondbookname,cip_seriesbookname";
			} else if (X.isSame(e.name, "cip_publisher")) {
				e.value = Arrays.asList("学苑出版社");
			}
		});
		System.out.println("q=" + q.toString());
//		q = W.create().and("parentid", 95).or("type", new Integer[] { 0, 1 }, W.OP.neq).sort("seq");
//
//		System.out.println("q=" + q);
	}

}
