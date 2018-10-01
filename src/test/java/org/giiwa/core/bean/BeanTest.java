package org.giiwa.core.bean;

import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;
import org.junit.Test;

public class BeanTest {

	@Test
	public void test() {
		// fail("Not yet implemented");

		W q = W.create();
		q.and("a", 1);
		q.and("b", 1);
		q.and("c", 1);
		q.or("d", 2);
		q.and(W.create().or("a", 1).or("b", 1));

		System.out.println(q.query());

		Bean b = new A();
		b.set("a", 2);

		JSON j = JSON.create();
		j.append("b", b);
		System.out.println(j.toString());

	}

	static class A extends Bean {

	}

	static class B extends A {

	}
}
