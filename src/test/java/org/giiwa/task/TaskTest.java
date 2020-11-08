package org.giiwa.task;

import static org.junit.Assert.*;

import org.giiwa.cache.Cache;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.net.mq.MQ;
import org.junit.Test;

public class TaskTest {

	@Test
	public void test() {
		Config.init();
		Global.setConfig("site.group", "demo");

		Cache.init(null);

		Task.init(100);

		MQ.init();

		try {

			TimeStamp t = TimeStamp.create();

			t.reset();

			// Thread.sleep(X.AHOUR);
		} catch (Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

}
