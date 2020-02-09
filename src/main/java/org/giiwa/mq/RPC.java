package org.giiwa.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.conf.Local;
import org.giiwa.mq.MQ.Request;

class RPC extends IStub {

	private static Log log = LogFactory.getLog(RPC.class);

	public static String name = "rpc." + Local.id();

	private static Map<Long, Object[]> waiter = new HashMap<Long, Object[]>();

	public static RPC inst = new RPC();

	private static AtomicLong seq = new AtomicLong(0);

	public RPC() {
		super(name);
	}

	@Override
	public void onRequest(long seq, Request req) {

		Object d = null;
		try {
			d = req.get();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		Object[] l1 = waiter.get(seq);
		if (l1 != null) {
			synchronized (l1) {
				synchronized (l1) {
					l1[0] = d;
					l1.notifyAll();
				}
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static <T> T call(String name, Request req, long timeout) throws Exception {

		TimeStamp t = TimeStamp.create();

		req.from = RPC.name;
		req.seq = seq.incrementAndGet();

		Object[] oo = new Object[1];
		waiter.put(req.seq, oo);
		MQ.send(name, req);

		synchronized (oo) {
			if (oo[0] == null) {
				oo.wait(timeout - t.pastms());
			}
		}

		waiter.remove(req.seq);

		if (timeout < t.pastms()) {
			throw new Exception("timeout");
		}

		return (T) oo[0];

	}

}
