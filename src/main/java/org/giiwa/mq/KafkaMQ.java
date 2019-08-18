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
package org.giiwa.mq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.SysTask;
import org.giiwa.framework.bean.GLog;

class KafkaMQ extends MQ {

	private static Log log = LogFactory.getLog(KafkaMQ.class);

	private Properties props = new Properties();
	private String group;

	private KafkaMQ(Properties p) {
		props = p;

	}

	/**
	 * Creates the.
	 *
	 * @return the mq
	 */
	public static MQ create() {

		String url = Global.getString("kafkamq.url", "localhost:9092");
		String group = Global.getString("site.group", "demo");

		Properties props = new Properties();
		props.put("bootstrap.servers", url);
		// props.put("acks", "all");
		props.put("retries", 0);
		props.put("group.id", "0");
		props.put("batch.size", 16384);
		props.put("auto.create.topics.enable", true);
		// props.put("linger.ms", 1);
		// props.put("buffer.memory", 33554432);
		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.ByteArraySerializer");

		// for (int i = 0; i < 100; i++)
		// producer.send(new ProducerRecord<String, String>("my-topic",
		// Integer.toString(i), Integer.toString(i)));

		props.put("enable.auto.commit", "true");
		props.put("auto.commit.interval.ms", "1000");
		props.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
		props.put("value.deserializer", "org.apache.kafka.common.serialization.ByteArrayDeserializer");

		// consumer.subscribe(Arrays.asList("foo", "bar"));
		//
		// m.producer.close();

		KafkaMQ m = new KafkaMQ(props);
		m.group = group;

		GLog.applog.info(org.giiwa.app.web.admin.mq.class, "startup", "connected KafkaMQ with [" + url + "]", null,
				null);

		return m;
	}

	private transient List<WeakReference<R>> cached = new ArrayList<WeakReference<R>>();

	/**
	 * QueueTask
	 * 
	 * @author joe
	 * 
	 */
	public class R extends SysTask {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public String name;
		IStub cb;
		Consumer<String, byte[]> consumer;
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
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		private R(String name, IStub cb, Mode mode) throws JMSException {
			this.name = name;
			this.cb = cb;

			consumer = new KafkaConsumer<>(props);
			consumer.subscribe(Arrays.asList(group + "." + name.replaceAll(":", ".")));

			cached.add(new WeakReference<R>(this));

		}

		@Override
		public void onExecute() {
			ConsumerRecords<String, byte[]> r = consumer.poll(1000);
			r.forEach(m -> process(m.value()));

		}

		@Override
		public void onFinish() {
			this.schedule(0);
		}

		private void process(byte[] data) {
			DataInputStream in = null;
			try {
				// System.out.println("got a message.., " + t.reset() +
				// "ms");

				in = new DataInputStream(new ByteArrayInputStream(data));

				List<Request> l1 = new LinkedList<Request>();

				while (in.available() > 0) {

					count++;

					Request r = new Request();
					r.seq = in.readLong();
					int len = in.readInt();
					if (len > 0) {
						byte[] bb = new byte[len];
						in.read(bb);
						r.from = new String(bb);
					}

					r.type = in.readInt();
					len = in.readInt();
					if (len > 0) {
						r.data = new byte[len];
						in.read(r.data);
					}

					l1.add(r);

					if (count % 10000 == 0) {
						log.debug("process the 10000 messages, cost " + t.reset() + "ms");
					}
				}

				MQ.process(name, l1, cb);

				if (log.isDebugEnabled())
					log.debug("got: " + l1.size() + " in one packet, name=" + name + ", cb=" + cb);

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {

		GLog.applog.info(org.giiwa.app.web.admin.mq.class, "bind",
				"[" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);

		R r = new R(name, stub, mode);
		r.schedule(0);

	}

	@Override
	protected long _topic(String to, MQ.Request r) throws Exception {
		return _send(to, r);
	}

	@Override
	protected long _send(String to, MQ.Request r) throws Exception {

		// if (X.isEmpty(r.data))
		// throw new Exception("message can not be empty");

		to = to.replaceAll(":", ".");

		/**
		 * get the message producer by destination name
		 */
		Sender p = getSender(to, 0);
		if (p == null) {
			throw new Exception("MQ not ready yet");
		}
		p.send(r);

		return r.seq;

	}

	private Sender getSender(String name, int type) {
		String name1 = name + ":" + type;
		synchronized (senders) {
			if (senders.containsKey(name1)) {
				return senders.get(name1);
			}

			try {

				Sender s = new Sender(name1, name);
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

	class Sender extends SysTask {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		long last = System.currentTimeMillis();
		String name;
		String to;
		Producer<String, byte[]> p;
		ByteArrayOutputStream m = null;
		DataOutputStream out = null;
		int len = 0;

		public void send(Request r) throws Exception {
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
					m = new ByteArrayOutputStream();
					out = new DataOutputStream(m);
					len = 0;
				}

				out.writeLong(r.seq);
				len += Long.SIZE / Byte.SIZE;

				len += Integer.SIZE / Byte.SIZE;
				byte[] ff = r.from == null ? null : r.from.getBytes();
				if (ff == null) {
					out.writeInt(0);
				} else {
					out.writeInt(ff.length);
					out.write(ff);
					len += ff.length;
				}

				out.writeInt(r.type);
				len += Integer.SIZE / Byte.SIZE;

				len += Integer.SIZE / Byte.SIZE;
				if (r.data == null) {
					out.writeInt(0);
				} else {
					out.writeInt(r.data.length);
					out.write(r.data);
					len += r.data.length;
				}

				p.notify();
			}
		}

		public Sender(String name, String to) {
			this.name = name;
			this.to = to;
			p = new KafkaProducer<>(props);
		}

		public String getName() {
			return "sender." + name;
		}

		@Override
		public void onExecute() {
			try {
				byte[] m = null;
				synchronized (p) {
					while (this.m == null) {
						if (last < System.currentTimeMillis() - X.AMINUTE) {
							break;
						}

						p.wait(10000);
					}
					if (this.m != null) {
						m = this.m.toByteArray();
						this.m = null;
						this.out = null;

						this.len = 0;
						p.notify();
					}
				}

				if (m != null) {

					p.send(new ProducerRecord<String, byte[]>(group + "." + to, to, m));

					if (log.isDebugEnabled())
						log.debug("Sending:" + group + "." + to);
				} else if (last < System.currentTimeMillis() - X.AMINUTE) {
					// the name has mode in the end
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
