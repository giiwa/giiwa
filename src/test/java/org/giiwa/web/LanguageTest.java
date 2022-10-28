package org.giiwa.web;

import org.junit.Test;

public class LanguageTest {

	@Test
	public void testParse() {

		String s = "1998";

		Language lang = Language.getLanguage("zb_cn");

		System.out.println(lang.format(lang.parse(s, new String[] { "yyyy.MM", "yyyy" }), "yyyy-MM-01"));

	}

	@Test
	public void testParseTime() {

		Language lang = Language.getLanguage("zh_cn");

		String s = "2020-10-10 09:10:11";
		long t = lang.parsetime(s);
		String date = lang.format(t, "yyyy-MM-dd HH:mm:ss");
		System.out.println("t=" + t + ", date=" + date);

		s = "2020-10-10H09:10:11";
		t = lang.parsetime(s);
		date = lang.format(t, "yyyy-MM-dd HH:mm:ss");
		System.out.println("t=" + t + ", date=" + date);

		s = "2020-10-10T09:10:11";
		t = lang.parsetime(s);
		date = lang.format(t, "yyyy-MM-dd HH:mm:ss");
		System.out.println("t=" + t + ", date=" + date);

		s = "2020-10-10T09:10:11Z";
		t = lang.parsetime(s);
		date = lang.format(t, "yyyy-MM-dd HH:mm:ss");
		System.out.println("t=" + t + ", date=" + date);

		t = 1638392586713L;
		System.out.println(lang.format(t, "yyyy-MM-dd HH:mm:ss"));

	}

}
