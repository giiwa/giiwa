package org.giiwa.task;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.giiwa.cache.Cache;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
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

	@Test
	public void testStop() {

		Thread t1 = new Thread() {

			@Override
			public void run() {
				System.out.println("t1 starting ...");
				try {
					synchronized (this) {
						Thread.sleep(X.AHOUR);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("t1 end ...");
			}

		};

		Thread t2 = new Thread() {
			@Override
			public void run() {
				System.out.println("t2 starting ...");
				try {
					synchronized (t1) {
						Thread.sleep(1);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				System.out.println("t2 end ...");
			}

		};

		t1.start();
		t2.start();
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

//		t1.interrupt();
		t1.stop();

	}

	@Test
	public void testParallel() {

		Config.init();

//		Cache.init(null, null, null);
//
		Task.init(100);

		try {

			List<Integer> l1 = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

			Task.forEach(l1, 5, e -> {
				try {
					Thread.sleep((int) (Math.random() * 10000));
					System.out.println(e + "\t-\t" + Thread.currentThread().getName());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			});

			System.out.println("done.");

			Thread.sleep(X.AMINUTE);

		} catch (Exception e) {

			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSchedule() {

		Config.init();
		Task.init(100);

		Task.schedule(t -> {
			System.out.println(Thread.currentThread().getName());
		});

		Task.schedule(t -> {
			System.out.println(Thread.currentThread().getName());
		});

	}

}
