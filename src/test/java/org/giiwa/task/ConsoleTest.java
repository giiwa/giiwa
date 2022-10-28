package org.giiwa.task;

import org.giiwa.dao.TimeStamp;
import org.junit.Test;

public class ConsoleTest {

	@Test
	public void test() {

		Task.init(10);

		Console.open(new String[] { Thread.currentThread().getName() }, msg -> {
			System.out.println(msg);
			return false;
		});

		TimeStamp t = TimeStamp.create();
		Task.schedule(t1 -> {
			for (int i = 0; i < 100; i++) {
				Console.inst.log("i=" + i);
			}
		});

		System.out.println("cost=" + t.past());

		try {
			Thread.sleep(10000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
