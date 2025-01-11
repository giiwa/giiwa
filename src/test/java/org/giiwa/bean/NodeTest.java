package org.giiwa.bean;

import org.giiwa.dao.MongoHelper;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper.W;
import org.junit.Test;

public class NodeTest {

	@Test
	public void test() {

		try {

			MongoHelper h = MongoHelper.create("mongodb://g09:27018/demo", null, null);
			Beans<Node> l1 = h.load("gi_node", W.create(), 0, 10, Node.class);
			l1.asList(e -> {
				System.out.println(e);
				return null;
			});

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

}
