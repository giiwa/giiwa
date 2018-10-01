package org.giiwa.mq;

import java.util.concurrent.atomic.AtomicLong;

import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.junit.Test;

public class MQTest {

	@Test
	public void test() {
		Config.init();

		String url = "failover:(tcp://192.168.70.85:61616)?timeout=3000&jms.prefetchPolicy.all=2&jms.useAsyncSend=true";
		// url =
		// "failover:(tcp://joe.mac:61616)?timeout=3000&jms.prefetchPolicy.all=2&jms.useAsyncSend=true";
		// url =
		// "failover:(tcp://192.168.1.6:61616)?timeout=3000&jms.prefetchPolicy.all=2&jms.useAsyncSend=true";

		Task.init(200);

		Global.setConfig("mq.type", "activemq");
		// Global.setConfig("mq.type", "kafkamq");
		Global.setConfig("activemq.url", url);
		Global.setConfig("mq.group", "demo");
		Config.getConf().setProperty("node.name", "test");

		System.out.println("init mq ...");
		if (false && MQ.init()) {
			// MQ.logger(false);
			System.out.println("starting ...");

			TimeStamp t2 = TimeStamp.create();
			int n = 100000;
			int c = 1;

			Tester[] t = new Tester[c];
			for (int i = 0; i < t.length; i++) {
				t[i] = new Tester("t" + i, n);
				try {
					t[i].bind();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				// "t" + i
				t[i].send("echo1", JSON.create());
			}
			if (t2.pastms() > 0)
				System.out.println(
						"sent: " + c * n + ", cost: " + t2.pastms() + "ms, send TPS: " + (c * n * 1000L / t2.pastms()));

			t2.reset();
			n = 1;
			MQ.Request r = new MQ.Request();
			r.setBody(JSON.create().toString().getBytes());
			try {
				for (int i = 0; i < n; i++) {
					r.seq = i;
					// r.ttl = 10;
					MQ.send("echo2", r);
					Thread.sleep(100);
					if (i % 10000 == 0) {
						System.out.println("sending: " + i + ",  cost: " + t2.pastms() + "ms");
						t2.reset();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			synchronized (t) {
				int i = 0;
				try {
					for (Tester t1 : t) {
						while (!t1.isFinished() && i < 100) {
							t.wait(1000);
							i++;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			for (Tester t1 : t) {
				t1.println();
			}

			if (t2.pastms() > 0)
				System.out.println("transation TPS: " + (n * c) * 1000 / t2.pastms());
		} else {
			System.out.println("init failed");
		}

		System.out.println("done");

		// byte[] bb = new byte[] { 17, 123, 10, 32, 32, 32, 34, 97, 34, 32, 58, 32,
		// 34, 97, 34, 10, 125, 10 };
		// String s = new String(bb).trim();
		// // JSON j1 = JSON.fromObject(bb);
		// JSON j2 = JSON.fromObject(s);
		//
		// System.out.println(s);
		// System.out.println(Arrays.toString(s.trim().getBytes()));

		// System.exit(0);
	}

	public static class Tester extends IStub {

		int n;
		AtomicLong seq = new AtomicLong();
		AtomicLong back = new AtomicLong();
		AtomicLong total = new AtomicLong();

		JSON status = JSON.create();
		JSON msg;
		String to;
		long created = System.currentTimeMillis();

		public Tester(String name, int n) {
			super(name);
			this.n = n;
		}

		public void println() {
			System.out.println(status.toString());
		}

		public void send(String to, JSON msg) {
			this.msg = msg;
			this.to = to;

			// long s = seq.incrementAndGet();
			msg.put("sendtime", System.currentTimeMillis());

			try {
				MQ.Request r = new MQ.Request();
				r.type = 0;
				r.data = msg.toString().getBytes();
				this.send(to, r);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public boolean isFinished() {
			return back.get() == n;
		}

		@Override
		public void onRequest(long seq, MQ.Request req) {
			// System.out.println("from:" + from);

			long min = status.getLong("min", Long.MAX_VALUE);
			long max = status.getLong("max", Long.MIN_VALUE);

			long t = System.currentTimeMillis() - msg.getLong("sendtime");
			total.addAndGet(t);
			if (t < min) {
				status.put("min", t);
			}
			if (t > max) {
				status.put("max", t);
			}
			status.put("total", total.get());

			back.incrementAndGet();
			status.put("aver", total.get() / back.get());

			if (this.seq.get() < n) {
				send(this.to, msg);
			}

			if (back.get() == n) {
				status.put("duration", System.currentTimeMillis() - created);
			}
		}

	}
}
