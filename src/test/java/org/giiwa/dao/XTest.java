package org.giiwa.dao;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.giiwa.engine.JS;
import org.giiwa.json.JSON;
import org.giiwa.task.Task;
import org.junit.Test;

public class XTest {

	@Test
	public void test() {
		String s = "a-c";
		char[] ss = X.range2(s, "-");
		System.out.println(Arrays.toString(ss));

		System.out.println(X.toLong("9700262001", -1));
	}

	@Test
	public void testTo() {
		double d = 6.462212122;
		System.out.println(X.toFloat(d, 0));
	}

	@Test
	public void testClone() {
		JSON j1 = JSON.create();
		j1.append("a", JSON.create().append("b", 1));

		JSON j2 = X.clone(j1);

		Task t1 = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				// TODO Auto-generated method stub

			}

		};

		JSON j4 = j1.append("_task", t1);
		JSON j3 = X.clone(j4);

		System.out.println("j1=" + j1);
		System.out.println("j2=" + j2);
		System.out.println("j3=" + j3.get("_task"));
		System.out.println("j4=" + j4);

		System.out.println("t1=" + t1);
		Task t2 = X.clone(t1);

		System.out.println("t2=" + t2);

	}

	@Test
	public void testToLong() {
		String d = "1.63E+12";
		System.out.println(X.toLong(X.toDouble(d)));
	}

	@Test
	public void isSame() {

		byte[] s1 = new byte[] { 78, 94, 115, -92, -59, 125, 89, -116, 127, 25, -66, 108, 88, -11, 71, -9, -34, -5,
				-110, 54 };
		byte[] s2 = new byte[] { 78, 94, 115, -92, -59, 125, 89, -116, 127, 25, -66, 108, 88, -11, 71, -9, -34, -5,
				-110, 54 };

		System.out.println(X.isSame(s1, s2));

	}

	@Test
	public void testToLong1() {
		double d = 11.5;
		System.out.println(X.toLong(d));
	}

	@Test
	public void testInt() {
		String s = "1212131122122222122212";
		System.out.println(X.toInt(s, 0));
	}

	@Test
	public void testAsList() {

		try {
			String js = "var l1=[1,2,3,[1,2]];l1;";
			Object o = JS.run(js);
			System.out.println(o.getClass());
			List l2 = X.asList(o, e1 -> e1);
			System.out.println(l2.size());
		} catch (Exception e) {
			e.printStackTrace();
		}

//		X.asList(o, e1->e1);

	}

	@Test
	public void testIsSame() {

		List<String> l1 = new ArrayList<String>();
		List<String> l2 = new ArrayList<String>();

		l1.add("a");
		l1.add("b");

		l2.add("b");
		l2.add("a");

		System.out.println(X.isSame(l1, l2));

	}

	@Test
	public void testSize() {

		String s = "4g";
		System.out.println(X.inst.size(s));

	}

}
