package org.giiwa.bean;

import static org.junit.Assert.fail;

import org.giiwa.json.JSON;
import org.junit.Test;

public class AppTest {

	@Test
	public void test() {

		App a = new App();
		a.appid = "1";
		a.secret = "123123";

		JSON j1 = JSON.create();
		j1.put("name", "1");
		j1.put("key", "122");

		try {
			String data = App.encode(j1.toPrettyString(), a.secret);
			System.out.println("data=" + data);
			String jo = App.decode(data, a.secret);
			System.out.println("jo=" + jo);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}

	}

}
