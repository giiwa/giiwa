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

import javax.jms.BytesMessage;
import javax.jms.Connection;
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
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.task.SysTask;

class ActiveMQ extends MQ {

	private static Log log = LogFactory.getLog(ActiveMQ.class);

	private String group = X.EMPTY;
	private Session session;

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

			Connection connection = factory.createConnection();
			connection.start();

			m.session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			GLog.applog.info(org.giiwa.app.web.admin.mq.class, "startup", "connected ActiveMQ with [" + url + "]", null,
					null);

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			// e.printStackTrace();
			GLog.applog.warn(org.giiwa.app.web.admin.mq.class, "startup", "failed ActiveMQ with [" + url + "]", null,
					null);
		}

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
		MessageConsumer consumer;
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
			if (consumer != null) {
				try {
					consumer.close();
				} catch (JMSException e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		private R(String name, IStub cb, Mode mode) throws JMSException {
			this.name = name;
			this.cb = cb;

			if (session != null) {
				Destination dest = null;
				if (mode == Mode.QUEUE) {
					dest = new ActiveMQQueue(group + name);
				} else {
					dest = new ActiveMQTopic(group + name);
				}

				consumer = session.createConsumer(dest);
				consumer.setMessageListener(this);

				cached.add(new WeakReference<R>(this));

			} else {
				log.warn("MQ not init yet!");
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
				// System.out.println("got a message.., " + t.reset() +
				// "ms");

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
						pos ++;
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

					if (log.isDebugEnabled())
						log.debug("got: " + l1.size() + " in one packet, name=" + name + ", cb=" + cb);

				} else {
					log.warn("unknown message=" + m);
				}

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {
		if (session == null)
			throw new JMSException("MQ not init yet");

		GLog.applog.info(org.giiwa.app.web.admin.mq.class, "bind",
				"[" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);

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

	private Sender getSender(String name, MQ.Mode type) {
		String name1 = name + ":" + type;
		synchronized (senders) {
			if (session != null) {
				if (senders.containsKey(name1)) {
					return senders.get(name1);
				}

				try {
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
					s.schedule(0);

					return s;
				} catch (Exception e) {
					log.error(name, e);
				}
			}
		}

		return null;
	}

	/**
	 * queue producer cache
	 */
	private Map<String, Sender> senders = new HashMap<String, Sender>();

	class Sender extends SysTask {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		long last = System.currentTimeMillis();
		String name;
		String to;
		MessageProducer p;
		BytesMessage m = null;
		int len = 0;
		int priority = 1;
		long ttl = (int) X.AMINUTE;
		int persistent = DeliveryMode.NON_PERSISTENT;

		public void send(Request r) throws JMSException {

			if (log.isDebugEnabled())
				log.debug("sending, r=" + r);

			last = System.currentTimeMillis();
			synchronized (p) {
				try {
					if (len > 2 * 1024 * 1024) {
						// slow down
						p.wait(1000);
					}
				} catch (Exception e) {
					// forget this exception
				}
				if (m == null) {
					m = session.createBytesMessage();
					len = 0;
				}

				m.writeLong(r.seq);
				len += Long.SIZE / Byte.SIZE;
				m.writeByte(r.ver);
				len ++;
				m.writeLong(r.tt);
				len += Long.SIZE / Byte.SIZE;

				len += Integer.SIZE / Byte.SIZE;
				byte[] ff = r.from == null ? null : r.from.getBytes();
				if (ff == null) {
					m.writeInt(0);
				} else {
					m.writeInt(ff.length);
					m.writeBytes(ff);
					len += ff.length;
				}

				m.writeInt(r.type);
				len += Integer.SIZE / Byte.SIZE;

				len += Integer.SIZE / Byte.SIZE;
				if (r.data == null) {
					m.writeInt(0);
				} else {
					m.writeInt(r.data.length);
					m.writeBytes(r.data);
					len += r.data.length;
				}

				ttl = r.ttl;
				priority = r.priority;
				persistent = r.persistent;

				p.notify();
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

		@Override
		public void onExecute() {
			try {
				BytesMessage m = null;
				synchronized (p) {
					while (this.m == null) {
						if (last < System.currentTimeMillis() - X.AMINUTE) {
							break;
						}

						p.wait(10000);
					}
					m = this.m;
					this.m = null;
					this.len = 0;

					p.notify();
				}

				if (m != null) {
					p.send(m, persistent, priority, ttl);
					// p.send(m);

					if (log.isDebugEnabled()) {
						log.debug("Sending:" + group + name);
					}
				} else if (last < System.currentTimeMillis() - X.AMINUTE) {
					senders.remove(name);
					p.close();
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		@Override
		public void onFinish() {
			if (last < System.currentTimeMillis() - X.AMINUTE) {
				if (log.isDebugEnabled())
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

}
