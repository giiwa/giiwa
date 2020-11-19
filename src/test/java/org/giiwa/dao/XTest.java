package org.giiwa.dao;

import static org.junit.Assert.*;

import java.util.Arrays;

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
		System.out.println(X.toFloat(d, 0, 10));
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
		String d = "aaa";
		d = null;
		System.out.println(X.toLong(d));
	}

}
