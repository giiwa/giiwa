package org.giiwa.web;

import org.junit.Test;

public class LanguageTest {

	@Test
	public void testParse() {
		
		String s = "1998";

		Language lang = Language.getLanguage("zb_cn");

		System.out.println(lang.format(lang.parse(s, new String[] { "yyyy.MM", "yyyy" }), "yyyy-MM-01"));

	}

}
