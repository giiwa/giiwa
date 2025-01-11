package org.giiwa.dao;

import org.giiwa.dao.Helper.V;
import org.giiwa.json.JSON;
import org.junit.Test;

public class VTest {

	@Test
	public void test() {

		JSON j1 = JSON.create();
		j1.append("a", 1);
		j1.append("b", null);
		j1.append("c", "");

		System.out.println(j1.toPrettyString());

		V v = V.fromJSON(j1);
		System.out.println(v);

	}

}
