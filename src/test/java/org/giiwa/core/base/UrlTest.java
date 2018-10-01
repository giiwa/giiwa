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

		s = "http://{ip=111}:{port=bbb}/a/{aaaa}?a=2";
		u.parse(s);
		System.out.println(u);

		u = Url.create(s);
		System.out.println(u);
		s = "jdbc:mysql://123.125.114.30:3306/1112?user=lkkkkk&password=2222&useUnicode=true&characterEncoding=8859_1";
		u = Url.create(s);
		u.parse("jdbc:mysql://{ip}:{port=3306}/{dbname}?user={username}&password={password}&useUnicode={useUnicode=true}&characterEncoding={characterEncoding=8859_1}");
		System.out.println(u);
		

		u = Url.create("172.100.3.*");
		System.out.println(u);
		
	}

}
