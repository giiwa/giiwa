package org.giiwa.core.base;

import static org.junit.Assert.*;

import org.junit.Test;

public class HttpTest {

	@Test
	public void test() {
		String s = "aaa.club.autohome.com";

		System.out.println(Http.domain(s, 10));
	}

}
