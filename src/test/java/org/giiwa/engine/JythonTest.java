package org.giiwa.engine;

import org.giiwa.dao.TimeStamp;
import org.giiwa.json.JSON;
import org.junit.Test;

public class JythonTest extends Groovy {

	@Test
	public void test() {
		try {
			TimeStamp t = TimeStamp.create();
			String code = "return x";
			JSON p = JSON.create();
			for (int i = 0; i < 1000; i++) {
				p.put("x", i);
				Object o = Jython.run(code, p);
				System.out.println(o + ", cost=" + t.reset() + ", i=" + i);
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
