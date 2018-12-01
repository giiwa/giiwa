package org.giiwa.core.dfile.command;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.giiwa.core.bean.X;
import org.giiwa.core.dfile.ICommand;
import org.giiwa.core.nio.IResponseHandler;
import org.giiwa.core.nio.Request;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;

public class NOTIFY implements ICommand {

	@Override
	public void process(Request in, IResponseHandler handler) {

		String name = in.readString();
		ObjectInputStream oi = null;
		try {
			List<Object[]> l1 = waiter.get(name);
			if (l1 != null && !l1.isEmpty()) {
				byte[] data = in.readBytes();
				oi = new ObjectInputStream(new ByteArrayInputStream(data));
				Object d = oi.readObject();
				log.debug("notify, name=" + name);
				
				for (Object[] a : l1) {
					if (a[0] != null && a[0] instanceof IStub) {
						((IStub) a[0]).onRequest(in.seq, MQ.Request.create().seq(in.seq).put((Serializable) d));
					} else {
						synchronized (a) {
							a[0] = d;
							a.notifyAll();
						}
					}
				}
				
			} else {
				log.debug("not waiter for name=" + name);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(oi);
		}

	}

	private static Map<String, List<Object[]>> waiter = new HashMap<String, List<Object[]>>();

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
				l1.add(a);
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
				l1.remove(a);
				if (l1.isEmpty()) {
					waiter.remove(name);
				}
			}
		}

		return ((T) a[0]);
	}

	public static void bind(String name, IStub stub) {

		Object[] a = new Object[1];
		a[0] = stub;
		List<Object[]> l1 = waiter.get(name);
		if (l1 == null) {
			l1 = new ArrayList<Object[]>();
			waiter.put(name, l1);
		}
		l1.add(a);

	}

}
