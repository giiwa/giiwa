package org.giiwa.mq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.mq.MQ.Request;

class Notify extends IStub {

	private static Log log = LogFactory.getLog(Notify.class);

	public static String name = "notify";

	private static Map<String, List<Object[]>> waiter = new HashMap<String, List<Object[]>>();

	public Notify() {
		super(name);
	}

	@Override
	public void onRequest(long seq, Request req) {

		String name = req.from;

		Object d = req.get();
		List<Object[]> l1 = waiter.get(name);
		if (l1 != null) {
			synchronized (l1) {
				for (Object[] a : l1) {
					synchronized (a) {
						a[0] = d;
						a.notifyAll();
					}
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> T wait(String name, long timeout, Runnable prepare) {
		Object[] a = new Object[1];
		try {
			synchronized (a) {
				List<Object[]> l1 = waiter.get(name);
				if (l1 == null) {
					l1 = new ArrayList<Object[]>();
					waiter.put(name, l1);
				}
				synchronized (l1) {
					l1.add(a);
				}

				if (prepare != null) {
					prepare.run();
				}
				a.wait(timeout);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			List<Object[]> l1 = waiter.get(name);
			if (l1 != null) {
				synchronized (l1) {
					l1.remove(a);
					if (l1.isEmpty()) {
						waiter.remove(name);
					}
				}
			}
		}

		return ((T) a[0]);
	}

}
