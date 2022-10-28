package org.giiwa.cache;

import org.giiwa.conf.Global;
import org.junit.Test;

public class CacheTest {

	@Test
	public void test() {
		Global.setConfig("group", "demo");
		Cache.init("memcached://127.0.0.1:11211", null, null);
//		Cache.init("redis://127.0.0.1:6379");

		String s = Cache.get("a");
		System.out.println(s);

		Cache.set("a", "Sssss", 10000);
		s = Cache.get("a");
//		Cache.remove("a");

//		Cache.remove("aaa");

		System.out.println(s);

		if (Cache.trylock("aaa")) {
//			Object o = Cache.get("aaa");
			System.out.println("true");
		} else {
			System.out.println("false");
		}
	}

}
