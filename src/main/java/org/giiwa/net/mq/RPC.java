package org.giiwa.net.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.net.mq.MQ.Request;

class RPC extends IStub {

//	private static Log log = LogFactory.getLog(RPC.class);

	public static String name = "rpc." + Local.id();

	private static Map<Long, Object[]> waiter = new HashMap<Long, Object[]>();

	public static RPC inst = new RPC();

	private static AtomicLong seq = new AtomicLong(0);

	public RPC() {
		super(name);
	}

	@Override
	public void onRequest(long seq, Request req) {

		Object[] l1 = waiter.get(seq);
		if (l1 != null) {
			synchronized (l1) {
				synchronized (l1) {
					try {
						l1[0] = req.get();
						l1[1] = req.type;
					} catch (Exception e) {
						l1[0] = e;
						l1[1] = 500;
					}
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

		Object[] oo = new Object[2];
		waiter.put(req.seq, oo);
		MQ.send(name, req);

		synchronized (oo) {
			if (oo[0] == null) {
				oo.wait(timeout - t.pastms());
			}
		}

		waiter.remove(req.seq);

		int state = X.toInt(oo[1]);
		if (state == 200) {
			return (T) oo[0];
		}

		if (state > 200) {
			throw new Exception((String) oo[0]);
		} else {
			throw new Exception("Timeout calling [" + name + "]");
		}

	}

}
