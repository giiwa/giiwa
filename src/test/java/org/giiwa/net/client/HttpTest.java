package org.giiwa.net.client;

import static org.junit.Assert.*;

import org.junit.Test;

public class HttpTest {

	@Test
	public void test() {
		String s = "https://search.jd.com/Search?keyword=iphone&enc=utf-8&wq=iphone&pvid=953db99c2e714e0fa516ddd1a6937f24";
		Http h = Http.create();
		Http.Response r = h.get(s);
		System.out.println(r.cookie());
	}

	@Test
	public void testDns() {
		String s = "https://www.doctrine.af.mil/";
		Http h = Http.create();
		h.dns("www.doctrine.af.mil", "104.69.79.225");
		Http.Response r = h.get(s);
		System.out.println(r.body);

	}

}
