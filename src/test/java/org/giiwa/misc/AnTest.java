package org.giiwa.misc;

import org.giiwa.task.Consumer;
import org.giiwa.task.Task;
import org.junit.Test;

public class AnTest {

	@Test
	public void test() {

		test1();
		test1();

	}

	void test1() {
		test(t -> {
			System.out.println(this);
		});
		test(t -> {
			System.out.println(this);
		});
	}

	void test(Consumer<Task> cc) {
		cc.accept(null);
		System.out.println(cc.getClass());
	}

}
