package org.giiwa.core.task;

import java.util.Arrays;
import java.util.List;

import org.giiwa.mq.MQ;
import org.junit.Test;

public class TaskTest {

	@Test
	public void test() {
		Task t = new Task() {

			@Override
			public void onExecute() {
				// TODO Auto-generated method stub

			}
		};

		// try {
		// Task.init(20);
		// MQ.init();
		//
		// List<Integer> l1 = Task.reduce(Arrays.asList(23, 43, 56, 97, 32), e -> {
		// System.out.println("e->" + e);
		// return e;
		// });
		// System.out.println("l1=" + l1);
		//
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

}
