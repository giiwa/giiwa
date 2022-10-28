package org.giiwa.task;

import static org.junit.Assert.*;

import org.giiwa.cache.Cache;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.net.mq.MQ;
import org.junit.Test;

public class TaskTest {

	@Test
	public void test() {

//		Config.init();
		Global.setConfig("site.group", "demo");

		Cache.init(null, null, null);

		Task.init(100);

		MQ.init();

		try {

			TimeStamp t = TimeStamp.create();

			t.reset();

//			int n = Task.mapreduce(e -> {
//				if (e == null)
//					return 1;
//				int n1 = X.toInt(e);
//				if (n1 > 100) {
//					return null;
//				}
//				return n1 + 1;
//
//			}, i -> {
//				return i * 10;
//			}, l1 -> {
//				int r = 0;
//				for (int i : l1) {
//					r += i;
//				}
//				return r;
//			});

//			System.out.println("cost=" + t.past() + ", n=" + n);

			// Thread.sleep(X.AHOUR);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
