package org.giiwa.mq;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.mq.MQ.Request;

public class Notify extends IStub {

	private static Log log = LogFactory.getLog(Notify.class);

	public static String name = "notify";

	private static Map<String, Object[]> waiter = new HashMap<String, Object[]>();

	public Notify() {
		super(name);
	}

	@Override
	public void onRequest(long seq, Request req) {

		String name = req.from;

		Object[] a = waiter.get(name);
		if (a != null) {
			a[0] = req.get();
			synchronized (a) {
				a.notifyAll();
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> T wait(String name, long timeout, Runnable prepare) {
		Object[] a = new Object[1];
		try {
			synchronized (a) {
				waiter.put(name, a);
				if (prepare != null) {
					prepare.run();
				}
				a.wait(timeout);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return ((T) a[0]);
	}

}
