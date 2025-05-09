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
package org.giiwa.net.mq;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;

class LocalMQ extends MQ {

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

		@Override
		public String toString() {
			return "R [name=" + name + "]";
		}

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

			if (m == Mode.QUEUE || m == Mode.BOTH) {
				String name1 = name + ":" + Mode.QUEUE.name();

				synchronized (consumers) {
					List<R> l1 = consumers.get(name1);

					if (l1 == null) {
						l1 = new ArrayList<R>();
						consumers.put(name1, l1);
					}
					l1.add(this);
				}
			}

			if (m == Mode.TOPIC || m == Mode.BOTH) {
				String name1 = name + ":" + Mode.TOPIC.name();

				synchronized (consumers) {
					List<R> l1 = consumers.get(name1);

					if (l1 == null) {
						l1 = new ArrayList<R>();
						consumers.put(name1, l1);
					}
					l1.add(this);
				}
			}

			cached.add(new WeakReference<R>(this));

		}

		public void onMessage(Request m) {
			List<Request> l1 = new LinkedList<Request>();
			l1.add(m);
			process(name, l1, cb);

			if (log.isDebugEnabled())
				log.debug("got: " + l1.size() + " in one packet, name=" + name + ", cb=" + cb);

		}
	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {

		new R(name, stub, mode);
	}

	@Override
	protected long _topic(String to, MQ.Request r) throws Exception {

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
		if (senders.containsKey(name1)) {
			return senders.get(name1);
		}

		Sender s = new Sender(name1);
		senders.put(name1, s);

		return s;
	}

	/**
	 * queue producer cache
	 */
	private Map<String, Sender> senders = new HashMap<String, Sender>();

	class Sender {

		long last = Global.now();
		String name;

		public void send(Request r) throws JMSException {
			if (log.isDebugEnabled())
				log.debug("sending, r=" + r);

			List<R> l1 = consumers.get(name);
			if (l1 != null && !l1.isEmpty()) {
				if (log.isDebugEnabled())
					log.debug("Sending: [" + name + "], consumer=" + l1);

				for (R r1 : l1) {
					r1.onMessage(r);
				}

			}

		}

		public Sender(String name) {
			this.name = name;
		}

		public String getName() {
			return "sender." + name;
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

	@Override
	protected void _stop() {
		consumers.clear();
		cached.clear();
		senders.clear();
	}

	@Override
	public void destroy(String name, Mode mode) throws Exception {
		consumers.remove(name);
		senders.remove(name);
	}

}
