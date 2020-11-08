package org.giiwa.json;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.giiwa.dao.Helper.W;
import org.giiwa.engine.JS;
import org.junit.Test;

public class JSONTest {

	@Test
	public void test() {
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
				System.out.println(e.getKey() + "=" + e.getValue());
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

}
