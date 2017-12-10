package org.giiwa.core.base;

import static org.junit.Assert.*;

import org.giiwa.framework.web.Language;
import org.junit.Test;

public class UrlTest {

	@Test
	public void test() {
		String s = "http://11:10/aaa?a=1";
		Url u = Url.create(s);

		assertNotNull("parse failed", u);
		assertEquals("protocol failed", "http", u.getProtocol());
		assertEquals("11", u.getIp());
		assertEquals(10, u.getPort(10));
		assertNotEquals(11, u.getPort(10));
		assertEquals("/aaa", u.getUri());
		assertEquals("1", u.get("a"));

		long t = 1512835200000L;
		System.out.println(Language.getLanguage().format(t, "yyyy-MM-dd HH:mm:ss"));
	}

}
