package org.giiwa.engine;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.task.Task;
import org.junit.Test;

public class JSTest {

	@Test
	public void test() {
		Task.init(10);

		String s = "a = 0;for(i=0;i<10000;i++) {a+=b;}";
		try {
			Map<String, Object> p1 = new HashMap<String, Object>();
			p1.put("b", 10);

			Map<String, Object> p2 = new HashMap<String, Object>();
			p2.put("b", 5);

			Task.schedule(() -> {
				try {
					System.out.println(JS.run(s, p1));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			Task.schedule(() -> {
				try {
					System.out.println(JS.run(s, p2));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			Object r = JS.calculate("2+1.0");
			System.out.println(r + ", " + r.getClass());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

	@Test
	public void testBindings() {


		Task[] tt = new Task[2];
		tt[0] = new Task() {

			int n = 1000;
			
			@Override
			public void onExecute() {
				
				String code = "function aaa() {return b + c;};_d1.ret=aaa();";
				JSON d1 = JSON.create();
				JSON j1 = JSON.create();
				j1.append("b", 1);
				j1.append("c", 2);
				j1.append("a", 3);
				j1.append("_d1", d1);

				try {
					JS.run(code, j1);
					Object r = d1.get("ret");
					System.out.println(r);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
			
			
		};
		

	}

}
