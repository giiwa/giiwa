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
import java.util.Set;

import javax.jms.BytesMessage;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.advisory.AdvisorySupport;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.task.Task;

class ActiveMQ extends MQ {

	private static Log log = LogFactory.getLog(ActiveMQ.class);

	private String group = X.EMPTY;
	private Session session;
	private ActiveMQConnection conn;

	/**
	 * Creates the.
	 *
	 * @return the mq
	 */
	public static MQ create() {

		ActiveMQ m = new ActiveMQ();

		String url = Global.getString("activemq.url", ActiveMQConnection.DEFAULT_BROKER_URL);
		String user = Global.getString("activemq.user", ActiveMQConnection.DEFAULT_USER);
		String password = Global.getString("activemq.passwd", ActiveMQConnection.DEFAULT_PASSWORD);

		m.group = Global.getString("site.group", "demo");
		if (!m.group.endsWith(".")) {
			m.group += ".";
		}

		try {
			ActiveMQConnectionFactory factory = new ActiveMQConnectionFactory(user, password, url);

			m.conn = (ActiveMQConnection) factory.createConnection();
			m.conn.start();

			m.session = m.conn.createSession(false, Session.AUTO_ACKNOWLEDGE);

			GLog.applog.info("sys", "startup", "connected ActiveMQ with [" + url + "]", null, null);

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("admin.mq", "startup", "failed ActiveMQ with [" + url + "]", e, null, null);
		}

		m.new Cleanup().schedule(X.AMINUTE);

		return m;
	}

	private transient List<WeakReference<R>> cached = new ArrayList<WeakReference<R>>();

	/**
	 * QueueTask
	 * 
	 * @author joe
	 * 
	 */
	public class R implements MessageListener {

		public String name;
		IStub cb;
		MessageConsumer consumer1;
		MessageConsumer consumer2;
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
			if (consumer1 != null) {
				try {
					consumer1.close();
					consumer1 = null;
				} catch (JMSException e) {
					log.error(e.getMessage(), e);
				}
			}
			if (consumer2 != null) {
				try {
					consumer2.close();
					consumer2 = null;
				} catch (JMSException e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		private R(String name, IStub cb, Mode mode) throws JMSException {
			this.name = name;
			this.cb = cb;

			if (session != null) {

				if (mode == Mode.QUEUE || mode == Mode.BOTH) {
					Destination dest = new ActiveMQQueue(group + name);
					consumer1 = session.createConsumer(dest);
					consumer1.setMessageListener(this);

					if (log.isWarnEnabled()) {
						log.warn("bind queue [" + cb.getName() + "]");
					}
				}

				if (mode == Mode.TOPIC || mode == Mode.BOTH) {
					Destination dest = new ActiveMQTopic(group + name);
					consumer2 = session.createConsumer(dest);
					consumer2.setMessageListener(this);

					if (log.isWarnEnabled()) {
						log.warn("bind topic [" + cb.getName() + "]");
					}
				}

				cached.add(new WeakReference<R>(this));

			} else {
				if (log.isDebugEnabled()) {
					log.debug("MQ not init yet!");
				}
				throw new JMSException("MQ not init yet!");
			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see javax.jms.MessageListener.onMessage(javax.jms.Message)
		 */
		@Override
		public void onMessage(Message m) {
			try {

				if (m instanceof BytesMessage) {

					BytesMessage m1 = (BytesMessage) m;

					long length = m1.getBodyLength();
					int pos = 0;

					List<Request> l1 = new LinkedList<Request>();
					while (pos < length) {

						count++;

						Request r = new Request();
						r.seq = m1.readLong();
						pos += Long.SIZE / Byte.SIZE;
						r.ver = m1.readByte();
						pos++;
						r.tt = m1.readLong();
						pos += Long.SIZE / Byte.SIZE;
						int len = m1.readInt();
						if (len > 0) {
							byte[] bb = new byte[len];
							m1.readBytes(bb);
							r.from = new String(bb);
						}
						pos += Integer.SIZE / Byte.SIZE;
						pos += len;

						r.type = m1.readInt();
						pos += Integer.SIZE / Byte.SIZE;

						len = m1.readInt();
						if (len > 0) {
							byte[] bb = new byte[len];
							m1.readBytes(bb);
							r.cmd = new String(bb);
						}
						pos += Integer.SIZE / Byte.SIZE;
						pos += len;

						len = m1.readInt();
						if (len > 0) {
							r.data = new byte[len];
							m1.readBytes(r.data);
						}
						pos += Integer.SIZE / Byte.SIZE;
						pos += len;

						l1.add(r);

						if (count % 10000 == 0) {
							log.debug("process the 10000 messages, cost " + t.reset() + "ms");
						}
					}

					process(name, l1, cb);

					if (log.isDebugEnabled()) {
						log.debug("got: " + l1.size() + " in one packet, name=" + name + ", cb=" + cb);
					}

				} else {
					log.error("mq.onmessagem unknown message=" + m);
				}

			} catch (Exception e) {
				log.error("mq.onmessage error", e);
			} finally {
				try {
					m.acknowledge();
				} catch (Exception e) {
					log.error("mq.onmessage error", e);
				}
			}
		}
	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {
		if (session == null) {
			throw new JMSException("MQ not init yet");
		}

//		GLog.applog.info(org.giiwa.app.web.admin.mq.class, "bind",
//				"[" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);

		new R(name, stub, mode);
	}

	@Override
	protected long _topic(String to, MQ.Request r) throws Exception {

		// if (X.isEmpty(r.data))
		// throw new Exception("message can not be empty");

		if (session == null) {
			throw new Exception("MQ not init yet");
		}

		/**
		 * get the message producer by destination name
		 */
		Sender p = getSender(to, MQ.Mode.TOPIC);
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

		if (session == null) {
			throw new Exception("MQ not init yet");
		}

		/**
		 * get the message producer by destination name
		 */
		Sender p = getSender(to, MQ.Mode.QUEUE);
		if (p == null) {
			throw new Exception("MQ not ready yet");
		}
		p.send(r);

		return r.seq;

	}

	private Sender getSender(String name, MQ.Mode type) throws JMSException {

		String name1 = name + ":" + type;
		if (session == null) {
			return null;
		}

		if (senders.containsKey(name1)) {
			return senders.get(name1);
		}

		Destination dest = null;
		if (MQ.Mode.QUEUE.equals(type)) {
			dest = new ActiveMQQueue(group + name);
		} else {
			dest = new ActiveMQTopic(group + name);
		}

		MessageProducer p = session.createProducer(dest);

		p.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
		// p.setTimeToLive(0);

		Sender s = new Sender(name1, name, p);
		senders.put(name1, s);
//					s.schedule(0);

		if (log.isInfoEnabled()) {
			log.info("create a new producer, =>" + name + ", mode=" + type + ", senders=" + senders.keySet());
		}

		return s;
//		}

//		return null;
	}

	/**
	 * queue producer cache
	 */
	private Map<String, Sender> senders = new HashMap<String, Sender>();

	class Sender {

		long last = System.currentTimeMillis();
		String name;
		String to;
		MessageProducer p;

		public void send(Request r) throws JMSException {

			last = System.currentTimeMillis();

			if (log.isDebugEnabled()) {
				log.debug("sending, r=" + r);
			}

			BytesMessage m = session.createBytesMessage();
			m.writeLong(r.seq);
			m.writeByte(r.ver);
			m.writeLong(r.tt);

			byte[] ff = r.from == null ? null : r.from.getBytes();
			if (ff == null) {
				m.writeInt(0);
			} else {
				m.writeInt(ff.length);
				m.writeBytes(ff);
			}

			m.writeInt(r.type);

			ff = r.cmd == null ? null : r.cmd.getBytes();
			if (ff == null) {
				m.writeInt(0);
			} else {
				m.writeInt(ff.length);
				m.writeBytes(ff);
			}

			if (r.data == null) {
				m.writeInt(0);
			} else {
				m.writeInt(r.data.length);
				m.writeBytes(r.data);
			}

			if (r.data != null && r.data.length > 1024 * 1000 * 1) {
				// > 1M
				Exception e = new Exception();
				GLog.applog.warn("mq", "send", "message body[" + r.data.length + "] exceed 1M", e, null, null);
				log.warn("message body[" + r.data.length + "] exceed 1M", e);
			}

			p.send(m, r.persistent, r.priority, r.ttl);

			if (log.isDebugEnabled()) {
				log.debug("Sending: " + group + name + ", size=" + (r.data == null ? 0 : r.data.length));
			}

		}

		public Sender(String name, String to, MessageProducer p) {
			this.name = name;
			this.to = to;
			this.p = p;
		}

		public String getName() {
			return "sender." + name;
		}

		public void close() {
			try {
				log.warn("close [" + name + "], dest=" + p.getDestination() + ", senders=" + senders.keySet());

//				conn.destroyDestination(p.getDestination());
				p.close();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

//		@Override
//		public void onExecute() {
//			try {
//				BytesMessage m = null;
//				synchronized (p) {
//					while (this.m == null) {
//						if (last < System.currentTimeMillis() - X.AMINUTE) {
//							break;
//						}
//
//						p.wait(10000);
//					}
//					m = this.m;
//					this.m = null;
//					this.len = 0;
//
//					p.notify();
//				}
//
//				if (m != null) {
//					p.send(m, persistent, priority, ttl);
//					// p.send(m);
//
//					if (log.isDebugEnabled()) {
//						log.debug("Sending:" + group + name);
//					}
//				} else if (last < System.currentTimeMillis() - X.AMINUTE) {
//					senders.remove(name);
//					p.close();
//				}
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		}

//		@Override
//		public void onFinish() {
//			if (last < System.currentTimeMillis() - X.AMINUTE) {
//				if (log.isDebugEnabled())
//					log.debug("sender." + name + " is stopped.");
//			} else {
//				this.schedule(0);
//			}
//		}

	}

	class Cleanup extends Task {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public int getPriority() {
			return Thread.MIN_PRIORITY;
		}

		@Override
		public String getName() {
			return "mq.cleanup";
		}

		@Override
		public void onFinish() {
			this.schedule(X.AMINUTE);
		}

		@Override
		public void onExecute() {
			Object[] ss = senders.keySet().toArray();
			for (Object s : ss) {
				Sender s1 = senders.get(s);
				if (s1 != null && System.currentTimeMillis() - s1.last > X.AMINUTE) {
					senders.remove(s);
					s1.close();
				}
			}

//			try {
//				Set<ActiveMQQueue> l1 = conn.getDestinationSource().getQueues();
//				for (ActiveMQQueue e : l1) {
//
//				}
//
//				Set<ActiveMQTopic> l2 = conn.getDestinationSource().getTopics();
//				for (ActiveMQTopic e : l2) {
//
//				}
//
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
		}

	}

	@Override
	protected void _unbind(IStub stub) throws Exception {
		// find R
		for (int i = cached.size() - 1; i >= 0; i--) {
			WeakReference<R> w = cached.get(i);

			if (w == null) {
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
		if (session != null) {
			try {
				session.close();
			} catch (JMSException e) {
				log.error(e.getMessage(), e);
			}
			session = null;
		}
	}

	@Override
	public void destroy(String name, Mode mode) {
		if (conn != null) {

			try {

				if (log.isInfoEnabled()) {
					log.info("destory dest, name=" + name + ", mode=" + mode);
				}

				if (mode.equals(Mode.QUEUE)) {
					ActiveMQQueue dest = new ActiveMQQueue(group + name);
					conn.destroyDestination(dest);

					ActiveMQTopic topic = AdvisorySupport.getProducerAdvisoryTopic(dest);
					if (topic != null) {
						conn.destroyDestination(topic);
					}
				} else {
					ActiveMQTopic dest = new ActiveMQTopic(group + name);
					conn.destroyDestination(dest);

					ActiveMQTopic topic = AdvisorySupport.getProducerAdvisoryTopic(dest);
					if (topic != null) {
						conn.destroyDestination(topic);
					}

				}
			} catch (Exception e) {
				log.error("destory dest, name=" + name + ", mode=" + mode, e);
			}
		}
	}

	public static void main(String[] args) {

		try {
			Global.setConfig("activemq.url",
					"failover:(tcp://g03:61616)?timeout=3000&jms.prefetchPolicy.all=2&jms.useAsyncSend=true");
			Global.setConfig("mq.type", "activemq");

			ActiveMQ mq = (ActiveMQ) ActiveMQ.create();

			Set<ActiveMQQueue> l1 = mq.conn.getDestinationSource().getQueues();
			for (ActiveMQQueue e : l1) {
				System.out.println(e);
			}

			Set<ActiveMQTopic> l2 = mq.conn.getDestinationSource().getTopics();
			for (ActiveMQTopic e : l2) {
				System.out.println(e);
			}

			System.out.println("done.");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
