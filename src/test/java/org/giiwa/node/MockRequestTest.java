package org.giiwa.node;

import org.junit.Test;

import jakarta.servlet.http.Cookie;

public class MockRequestTest extends MockRequest {

	@Test
	public void test() {

		Cookie c = new Cookie("sid", "0ba5454e-d59d-442f-bb32-8264fe453bcb");

		System.out.println(c);
	}

}
