package org.giiwa.engine;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.dao.TimeStamp;
import org.junit.Test;

public class VelocityTest {

	@Test
	public void test() {
		String s = "${age}>10";

		TimeStamp t = TimeStamp.create();
		for (int i = 0; i < 1; i++) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("age", i);
			try {
				System.out.println(Velocity.test(s, m));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// System.out.println(i);
		}
		System.out.println(t.pastms() + "ms");
	}

}
