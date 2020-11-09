package org.giiwa.dao;

import static org.junit.Assert.*;

import org.giiwa.json.JSON;
import org.junit.Test;

public class BeanTest {

	@SuppressWarnings("serial")
	@Test
	public void test() {
		try {
			Bean b = new Bean() {
				transient int aaa = 1;

				public String toString() {
					return json().append("aaa", aaa).toString();
				}
			};
			b.set("a.a", 1);
			System.out.println(b.toString());

			JSON j1 = b.json();
//			System.out.println(j1.toPrettyString());

			Bean a = (Bean) b.clone();
			System.out.println(a);

		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());

		}
	}

	@Test
	public void testUrl() {
		Bean b = new Bean();
		b.set("url", "https://123.com");
		System.out.println(b.json());
		System.out.println(b.json().toUrl());

	}

}
