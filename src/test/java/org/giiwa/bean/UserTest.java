package org.giiwa.bean;

import org.junit.Test;

public class UserTest {

	@Test
	public void test() {
		User u = new User();
		u.id = 10;
		System.out.println(u.token());
	}

}
