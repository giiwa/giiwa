package org.giiwa.dao;

import org.giiwa.conf.Config;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.junit.Test;

public class DMTest {

	@Test
	public void test() {

		Config.init();
		RDSHelper.init();

		Helper.DBHelper helper = RDSHelper.create("jdbc:dm://192.168.0.109:5236/test", "SYSDBA", "1231231234", 10,
				null);

		System.out.println("connected = " + helper.listTables(null, 10));
	}

	@Test
	public void testInsert() {

		Config.init();
		RDSHelper.init();

		Helper.DBHelper helper = RDSHelper.create("jdbc:dm://192.168.0.109:5236/test", "SYSDBA", "1231231234", 10,
				null);

		try {
			helper.insertTable("demo", V.create().append("a", 3).append("b", 6));
			Beans<Bean> bs = helper.load("demo", W.create(), 0, 10, Bean.class);
			System.out.println("bs = " + JSON.toPrettyString(bs.jsons()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testUpdate() {

		Config.init();
		RDSHelper.init();

		Helper.DBHelper helper = RDSHelper.create("jdbc:dm://192.168.0.42:5236/config", "demo", "1231231234", 10,
				null);

		try {
			helper.updateTable("demo", W.create().and("a", 2), V.create().append("c", 3));
			Beans<Bean> bs = helper.load("demo", W.create(), 0, 10, Bean.class);
			System.out.println("bs = " + JSON.toPrettyString(bs.jsons()));
			
			helper.updateTable("demo", W.create().and("a", null), V.create().append("a", 0));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testList() {

		Config.init();
		RDSHelper.init();

		Helper.DBHelper helper = RDSHelper.create("jdbc:dm://192.168.2.235:5236/CONFIG2", "SYSDBA", "1231231234", 10,
				null);

		System.out.println("connected = " + helper.listTables(null, 10));
	}

}
