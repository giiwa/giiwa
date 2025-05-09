package org.giiwa.conf;

import org.giiwa.cache.Cache;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.joda.time.Instant;
import org.junit.Test;

public class GlobalTest {

	@Test
	public void test() {
		String s = "http://g14.giisoo.com";
		Global.setConfig("server", s);
		Local.setConfig("a", s);
		System.out.println("server=" + Global.getString("server", X.EMPTY));
		System.out.println("server=" + Local.getString("a", X.EMPTY));
	}

	@Test
	public void testNow() {

		Global.setConfig("group", "demo");
		Cache.init("redis://g09:6379", "", "");

		int n = 10000;
		TimeStamp t = TimeStamp.create();
		for (int i = 0; i < n; i++) {
			long now = Global.now();
//			System.out.println(now + "=>" + System.currentTimeMillis());
		}

		long ns = t.pastns();
		System.out.println("cost=" + t.past() + ", a=" + (ns / n) + "ns");
		System.out.println(ns / (n * 5439));
	}

}
