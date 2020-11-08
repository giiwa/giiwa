package org.giiwa.web;

import static org.junit.Assert.*;

import org.junit.Test;

public class QueryStringTest {

	@Test
	public void test() {
		String url = "https://www.aa.com/?a=1a=2";
		QueryString s1 = new QueryString(url);
		System.out.println(s1.toString());
	}

}
