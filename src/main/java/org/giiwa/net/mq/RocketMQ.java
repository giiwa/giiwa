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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;

class RocketMQ extends MQ {

	private static Log log = LogFactory.getLog(RocketMQ.class);

	private String group = X.EMPTY;
	private String url;

	/**
	 * Creates the.
	 *
	 * @return the mq
	 */
	public static MQ create() {

		RocketMQ m = new RocketMQ();

		m.url = Global.getString("rocketmq.url", "localhost:9876");

		m.group = Global.getString("site.group", "demo");

		try {

			GLog.applog.info("sys", "startup", "connected ActiveMQ with [" + m.url + "]", null, null);

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("admin.mq", "startup", "failed RocketMQ with [" + m.url + "]", e, null, null);
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
	public class R implements MessageListenerConcurrently {

		long lastime;
		String name;
		IStub cb;
		DefaultMQPushConsumer consumer;
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
				if (log.isDebugEnabled()) {
					log.info("close [" + group + "/" + name + "]");
				}

				consumer.shutdown();
				consumer = null;
			}
		}

		private R(String name, IStub cb, Mode mode) throws Exception {

			this.name = name;
			this.cb = cb;

			consumer = new DefaultMQPushConsumer((group + "_" + name).replaceAll("\\.", "_"));

			// 设置NameServer的地址
			consumer.setNamesrvAddr(url);
			consumer.setInstanceName(Local.label() + "/" + group + "/" + name + "/" + mode);
//			consumer.setInstanceName(UID.random());

			try {
				// 订阅一个或者多个Topic，以及Tag来过滤需要消费的消息
//				name = name + "_" + mode;
				consumer.subscribe(name.replaceAll("\\.", "_"), "*");
				if (Mode.BOTH.equals(mode) || Mode.TOPIC.equals(mode)) {
					consumer.setMessageModel(MessageModel.BROADCASTING);
				} else {
					consumer.setMessageModel(MessageModel.CLUSTERING);
				}
				consumer.registerMessageListener(this);
				consumer.setPullTimeDelayMillsWhenException(0L);

				// 启动消费者实例
				consumer.start();

				cached.add(new WeakReference<R>(this));

				log.info("binded [" + group + "/" + name + "]");
			} catch (Exception e) {
				log.warn(name + "/" + mode, e);
				throw new Exception(e);
			}
		}

		@Override
		public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {

			if (log.isDebugEnabled()) {
				log.debug("got [" + msgs.size() + "] message");
			}

			onMessage(msgs);

			// 标记该消息已经被成功消费
			return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
		}

		public void onMessage(List<MessageExt> msgs) {

			try {

				List<Request> l1 = new LinkedList<Request>();
				for (MessageExt m : msgs) {

					count++;

					Request r = new Request();

					DataInputStream m1 = new DataInputStream(new ByteArrayInputStream(m.getBody()));

					r.seq = m1.readLong();
					r.ver = m1.readByte();
					r.tt = m1.readLong();
					r.type = m1.readInt();
					r.priority = m1.readInt();

					int len = m1.readInt();
					if (len > 0) {
//						log.info("len=" + len);
						byte[] bb = new byte[len];
						m1.read(bb);
						r.from = new String(bb);
					}

					len = m1.readInt();
					if (len > 0) {
//						log.info("len=" + len);
						byte[] bb = new byte[len];
						m1.read(bb);
						r.cmd = new String(bb);
					}

					len = m1.readInt();
					if (len > 0) {
//						log.info("len=" + len);
						r.data = new byte[len];
						m1.read(r.data);
					}

					l1.add(r);

					if (count % 10000 == 0) {
						log.info("process the 10000 messages, cost " + t.reset() + "ms");
					}
				}

				process(name, l1, cb);

//				if (log.isDebugEnabled())
//					log.debug("got: " + l1.size() + " in one packet, name=" + name + ", cb=" + cb);

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {
		new R(name, stub, mode);
	}

	@Override
	protected long _topic(String to, MQ.Request r) throws Exception {

		// if (X.isEmpty(r.data))
		// throw new Exception("message can not be empty");
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

	private Sender getSender(String name, MQ.Mode type) throws Exception {

//		name = name + "_" + type;

		if (senders.containsKey(name)) {
			return senders.get(name);
		}

		synchronized (senders) {

			Sender s = new Sender(name);
			senders.put(name, s);

			return s;
		}

	}

	/**
	 * queue producer cache
	 */
	private Map<String, Sender> senders = new HashMap<String, Sender>();

	class Sender implements Closeable {

		DefaultMQProducer producer;
		long last = Global.now();
		String name;

		public void send(Request r) throws Exception {

			if (log.isDebugEnabled()) {
				log.debug("sending to [" + group + "/" + name + "], r=" + r);
			}

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream m = new DataOutputStream(out);

			m.writeLong(r.seq);
			m.writeByte(r.ver);
			m.writeLong(r.tt);
			m.writeInt(r.type);
			m.writeInt(r.priority);

			byte[] ff = (r.from == null) ? null : r.from.getBytes();
			if (ff == null) {
				m.writeInt(0);
			} else {
				m.writeInt(ff.length);
				m.write(ff);
			}

			ff = (r.cmd == null) ? null : r.cmd.getBytes();
			if (ff == null) {
				m.writeInt(0);
			} else {
				m.writeInt(ff.length);
				m.write(ff);
			}

			if (r.data == null) {
				m.writeInt(0);
			} else {
				m.writeInt(r.data.length);
				m.write(r.data);
			}

			m.close();

			Message msg = new Message(name.replaceAll("\\.", "_"), "*", out.toByteArray());

			producer.send(msg);
		}

		public Sender(String name) throws MQClientException {
			this.name = name;

			producer = new DefaultMQProducer((group + "_" + name).replaceAll("\\.", "_"));
			// 设置NameServer的地址
			producer.setNamesrvAddr(url);
			// 启动Producer实例
			producer.start();
			// 异步
			// producer.setRetryTimesWhenSendAsyncFailed(0);

		}

		public String getName() {
			return "sender." + name;
		}

		@Override
		public void close() throws IOException {
			if (producer != null) {
				try {
					producer.shutdown();
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
				producer = null;
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

	@Override
	public void destroy(String name, Mode mode) throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	protected void _stop() {

	}

}
