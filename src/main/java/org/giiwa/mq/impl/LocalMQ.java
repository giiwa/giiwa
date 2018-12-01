/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.mq.impl;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.GLog;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;

public final class LocalMQ extends MQ {

	private static Log log = LogFactory.getLog(LocalMQ.class);

	private Map<String, List<R>> consumers = new HashMap<String, List<R>>();

	/**
	 * Creates the.
	 *
	 * @return the mq
	 */
	public static MQ create() {
		LocalMQ m = new LocalMQ();

		return m;
	}

	private transient List<WeakReference<R>> cached = new ArrayList<WeakReference<R>>();

	/**
	 * QueueTask
	 * 
	 * @author joe
	 * 
	 */
	public class R {

		public String name;
		IStub cb;
		TimeStamp t = TimeStamp.create();
		int count = 0;

		/**
		 * Close.
		 */
		public void close() {
			List<R> l1 = consumers.get(name);
			if (l1 != null) {
				l1.remove(this);
			}
		}

		private R(String name, IStub cb, Mode m) throws JMSException {
			this.name = name;
			this.cb = cb;

			String name1 = name + ":" + m.name();

			List<R> l1 = consumers.get(name1);

			if (l1 == null) {
				l1 = new ArrayList<R>();
				consumers.put(name1, l1);
			}
			l1.add(this);

			cached.add(new WeakReference<R>(this));

		}

		public void onMessage(Request m) {
			try {
				// System.out.println("got a message.., " + t.reset() +
				// "ms");
				List<Request> l1 = new LinkedList<Request>();
				l1.add(m);
				process(name, l1, cb);

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {
		GLog.applog.info(org.giiwa.app.web.admin.mq.class, "bind",
				"[" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);

		new R(name, stub, mode);
	}

	@Override
	protected long _topic(String to, MQ.Request r) throws Exception {

		// if (X.isEmpty(r.data))
		// throw new Exception("message can not be empty");

		/**
		 * get the message producer by destination name
		 */
		Sender p = getSender(to, Mode.TOPIC);
		if (p == null) {
			throw new Exception("MQ not ready yet");
		}

		p.send(r);

		return r.seq;
	}

	@Override
	protected long _send(String to, MQ.Request r) throws Exception {

		// if (X.isEmpty(r.data))
		// throw new Exception("message can not be empty");

		/**
		 * get the message producer by destination name
		 */
		Sender p = getSender(to, Mode.QUEUE);
		if (p == null) {
			throw new Exception("MQ not ready yet");
		}
		p.send(r);

		return r.seq;

	}

	private Sender getSender(String name, Mode m) {
		String name1 = name + ":" + m.name();
		synchronized (senders) {
			if (senders.containsKey(name1)) {
				return senders.get(name1);
			}

			try {

				Sender s = new Sender(name1);
				s.schedule(0);
				senders.put(name1, s);

				return s;
			} catch (Exception e) {
				log.error(name, e);
			}
		}

		return null;
	}

	/**
	 * queue producer cache
	 */
	private Map<String, Sender> senders = new HashMap<String, Sender>();

	class Sender extends Task {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		long last = System.currentTimeMillis();
		String name;
		ArrayBlockingQueue<Request> queue = new ArrayBlockingQueue<Request>(1000);

		public void send(Request r) throws JMSException {
			if (log.isDebugEnabled())
				log.debug("sending, r=" + r);

			try {
				if (consumers.containsKey(name)) {
					queue.add(r);
					last = System.currentTimeMillis();
				}
			} catch (Exception e) {
				log.error("sender=" + name, e);
				queue.clear();
			}
		}

		public Sender(String name) {
			this.name = name;
		}

		public String getName() {
			return "sender." + name;
		}

		@Override
		public void onExecute() {
			try {
				Request r = queue.poll(X.AMINUTE, TimeUnit.MILLISECONDS);

				if (r != null) {

					List<R> l1 = consumers.get(name);
					if (l1 != null && !l1.isEmpty()) {
						if (log.isDebugEnabled())
							log.debug("Sending: [" + name + "], consumer=" + l1);

						for (R r1 : l1) {
							r1.onMessage(r);
						}

					} else {
						log.warn("no consumer for [" + name + "], queue.size=" + queue.size() + ",consumers="
								+ consumers);
					}

				} else if (last < System.currentTimeMillis() - X.AMINUTE) {
					synchronized (senders) {
						senders.remove(name);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		@Override
		public void onFinish() {
			if (last < System.currentTimeMillis() - X.AMINUTE) {
				log.debug("sender." + name + " is stopped.");
			} else {
				this.schedule(0);
			}
		}

	}

	@Override
	protected void _unbind(IStub stub) throws Exception {
		// find R
		for (int i = cached.size() - 1; i >= 0; i--) {
			WeakReference<R> w = cached.get(i);

			if (w == null || w.get() == null) {
				cached.remove(i);
			} else {
				R r = w.get();
				if (r == null || r.cb == null) {
					cached.remove(i);
				} else if (r.cb == stub) {
					r.close();
					cached.remove(i);
				}
			}
		}
	}

}
