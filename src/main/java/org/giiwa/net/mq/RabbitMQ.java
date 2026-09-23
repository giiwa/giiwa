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
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.giiwa.task.Task;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import com.rabbitmq.client.Delivery;

/**
 * RabbitMQ， 性能好， 单节点支持5-8万TPS
 * 
 * @author joe
 *
 */
class RabbitMQ extends MQ {

	private static Log log = LogFactory.getLog(RabbitMQ.class);

	private String group = X.EMPTY;
	private Connection connection;
	private String virtualhost;
	private ConnectionFactory factory;

//	private String exchange;
//	private String routingkey;

	/**
	 * Creates the.
	 *
	 * @return the mq
	 */
	public static MQ create() {

		RabbitMQ m = new RabbitMQ();

		String url = Global.getString("rabbitmq.url", null);
		String username = Global.getString("rabbitmq.user", null);
		String passwd = Global.getString("rabbitmq.passwd", null);

		m.group = Global.getString("site.group", "demo");
		if (!m.group.endsWith(".")) {
			m.group += ".";
		}

		try {

			Url u = Url.create(url);
			m.virtualhost = u.get("virtualhost");
			if (X.isEmpty(m.virtualhost)) {
				m.virtualhost = "/";
			}

			m.factory = new ConnectionFactory();
			m.factory.setHost(u.getHost());
			m.factory.setPort(u.getPort(5672));
			if (!X.isEmpty(username)) {
				m.factory.setUsername(username);
				m.factory.setPassword(passwd);
			}

			m.factory.setVirtualHost(m.virtualhost);

			// 1. 心跳设置：大于中间代理空闲超时（代理30s超时则设90s）
			m.factory.setRequestedHeartbeat(90);
			// 2. 连接建立超时
			m.factory.setConnectionTimeout(5000);
			// 3. 开启连接自动恢复、通道自动重建
			m.factory.setAutomaticRecoveryEnabled(true);
			m.factory.setTopologyRecoveryEnabled(true);
			// 4. 握手超时，避免阻塞
			m.factory.setHandshakeTimeout(10000);
			m.factory.setNetworkRecoveryInterval(1000); // 断连后1s重试重连

			m.connection = m.factory.newConnection();

			GLog.applog.info("sys", "startup", "connected RabbitMQ with [" + url + "]", null, null);

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("sys", "startup", "failed RabbitMQ with [" + url + "]", e, null, null);
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
	public class R implements DeliverCallback {

		public String name;
		IStub cb;
		Mode mode;

		Channel channel;
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
			if (channel != null) {
				try {
					channel.close();
					channel = null;
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		private R(String name, IStub cb, Mode mode) {
			this.name = name;
			this.cb = cb;
			this.mode = mode;

			synchronized (cached) {
				cached.add(new WeakReference<>(this));
			}

		}

		private void init() throws IOException {
			if (connection != null) {

//				if (mode == Mode.QUEUE || mode == Mode.BOTH) {

				channel = connection.createChannel();
				if (mode == Mode.TOPIC || mode == Mode.BOTH) {
					// 一个exchange的消息广播到多个队列中

					// 广播模式，定义一个exchange，不持久化，自动删除， 广播是在一个exchange内
					channel.exchangeDeclare(name + ".topic", BuiltinExchangeType.FANOUT, false, true, null);

					// 定义队列，自动删除
					String name1 = name + "." + UID.random();
					channel.queueDeclare(name1, false, false, true, null);

					// 绑定队列
					channel.queueBind(name1, name + ".topic", "");
					channel.basicConsume(name1, true, this, consumerTag -> {
					});
				}

				if (mode == Mode.QUEUE || mode == Mode.BOTH) {
					// 一个队列可以有多个消费者，顺序消费

					String name1 = name + ".queue";
					// 为队列定义一个统一的 exchange, 不持久化，自动删除
					channel.exchangeDeclare(group + "direct", BuiltinExchangeType.DIRECT, false, true, null);
					// 定义队列，自动删除
					channel.queueDeclare(name1, false, false, true, null);

					// 绑定exchange与队列的routingkey，用于路由
					channel.queueBind(name1, group + "direct", name);
					channel.basicConsume(name1, true, this, consumerTag -> {
					});
				}

				if (log.isWarnEnabled()) {
					log.warn("bind queue [" + name + "]");
				}

			} else {
				if (log.isDebugEnabled()) {
					log.debug("MQ not init yet!");
				}
				throw new IOException("MQ not init yet!");
			}
		}

		@Override
		public void handle(String tag, Delivery delivery) throws IOException {

			byte[] body = delivery.getBody();

			List<Request> l1 = new LinkedList<Request>();
			DataInputStream m1 = new DataInputStream(new ByteArrayInputStream(body));

			try {
				int pos = 0;
				while (pos < body.length) {

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
						m1.read(bb);
						r.from = new String(bb);
					}
					pos += Integer.SIZE / Byte.SIZE;
					pos += len;

					r.type = m1.readInt();
					pos += Integer.SIZE / Byte.SIZE;

					len = m1.readInt();
					if (len > 0) {
						byte[] bb = new byte[len];
						m1.read(bb);
						r.cmd = new String(bb);
					}
					pos += Integer.SIZE / Byte.SIZE;
					pos += len;

					len = m1.readInt();
					if (len > 0) {
						r.data = new byte[len];
						m1.read(r.data);
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
			} finally {
				if (m1 != null) {
					m1.close();
				}
			}

		}

	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {
		new R(name, stub, mode).init();
	}

	@Override
	protected long _topic(String name, MQ.Request r) throws Exception {

		if (connection == null) {
			throw new Exception("MQ not init yet");
		}

		/**
		 * get the message producer by destination name
		 */
		Sender p = getSender(name, MQ.Mode.TOPIC);
		if (p == null) {
			throw new Exception("MQ not ready yet");
		}

		p.send(r);

		return r.seq;
	}

	@Override
	protected long _send(String routingkey, MQ.Request r) throws Exception {

		if (connection == null) {
			throw new Exception("MQ not init yet");
		}

		/**
		 * get the message producer by destination name
		 */
		Sender p = getSender(routingkey, MQ.Mode.QUEUE);
		if (p == null) {
			throw new Exception("MQ not ready yet");
		}
		p.send(r);

		return r.seq;

	}

	private Sender getSender(String name, MQ.Mode type) throws IOException {

		String name1 = name + ":" + type;
		if (connection == null) {
			return null;
		}

		if (senders.containsKey(name1)) {
			return senders.get(name1);
		}

		Sender s = null;
		if (MQ.Mode.QUEUE.equals(type)) {
			s = new Sender(name, group + "direct");
		} else {
			s = new Sender(name, name + ".topic");
		}
		s.init();

		synchronized (senders) {
			senders.put(name1, s);
		}

		if (log.isInfoEnabled()) {
			log.info("create a new producer, =>" + name + ", mode=" + type + ", senders=" + senders.keySet());
		}

		return s;

	}

	/**
	 * queue producer cache
	 */
	private static Map<String, Sender> senders = new HashMap<>();

	class Sender {

		long last = Global.now();
		String routingkey;
		String exchange;
		Channel p;
		boolean inited = false;

		public Sender(String name, String exchange) {
			this.exchange = exchange;
			this.routingkey = name;
		}

		private void init() throws IOException {
			if (connection == null || !connection.isOpen()) {
				throw new IOException(connection.toString());
			}
			p = connection.createChannel();
		}

		public void send(Request r) throws Exception {

			last = Global.now();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			DataOutputStream m = new DataOutputStream(out);

			try {
				m.writeLong(r.seq);
				m.writeByte(r.ver);
				m.writeLong(r.tt);

				byte[] ff = r.from == null ? null : r.from.getBytes();
				if (ff == null) {
					m.writeInt(0);
				} else {
					m.writeInt(ff.length);
					m.write(ff);
				}

				m.writeInt(r.type);

				ff = r.cmd == null ? null : r.cmd.getBytes();
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

				if (r.data != null && r.data.length > 1024 * 1000 * 1) {
					// > 1M
					Exception e = new Exception();
					GLog.applog.warn("mq", "send", "message body[" + r.data.length + "] exceed 1M", e, null, null);
					log.warn("message body[" + r.data.length + "] exceed 1M", e);
				}
			} finally {
				m.close();
			}

			try {
				p.basicPublish(exchange, routingkey, null, out.toByteArray());

				if (log.isDebugEnabled()) {
					log.debug("Sending: " + exchange + ":" + routingkey + ", size="
							+ (r.data == null ? 0 : r.data.length));
				}
			} catch (IOException err) {

				// 捕获连接重置、通道关闭异常
				log.error("MQ发送异常，重建连接", err);
				GLog.applog.error("mq", "send", err.getMessage(), err);

				// 关闭旧失效连接
				// 初始化整个MQ
				_repair();

				// 重试一下
				p.basicPublish(exchange, routingkey, null, out.toByteArray());

			}

		}

//		public Sender init1(String name, String exchange, Channel p) throws IOException {
//
//			this.exchange = exchange;
//			this.routingkey = name;
//			this.p = p;
//
//			return this;
//		}
//
//		public Sender init2(String name, String exchange, Channel p) throws IOException {
//
//			this.exchange = exchange;
//			this.routingkey = name;
//			this.p = p;
//
//			return this;
//		}

		public String getName() {
			return "sender." + routingkey;
		}

		public void close() {
			try {
//				p.queueUnbind(exchange, exchange, routingkey);
				log.warn("close [" + routingkey + "], dest=" + p + ", senders=" + senders.keySet());
				p.close();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

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
				if (s1 != null && Global.now() - s1.last > X.AMINUTE) {
					senders.remove(s);
					s1.close();
				}
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

	private long last_repair = 0;

	private synchronized void _repair() throws Exception {

		if (Global.now() - last_repair < X.AMINUTE) {
			return;
		}

		last_repair = Global.now();

		GLog.applog.warn("mq", "repair", "conn=" + connection + "\n" + X.toString(new Exception()));

		// 关闭原有连接
		if (connection != null) {
			try {
				if (connection.isOpen()) {
					connection.close();
				}
			} catch (Exception ex) {
				log.warn("close old connection fail", ex);
			}
			connection = null;
		}

		if (connection == null) {
			connection = factory.newConnection();
		}

		// 重建消费者R，过滤GC回收的对象
		synchronized (cached) {
			List<WeakReference<R>> removeList = new ArrayList<>();
			for (WeakReference<R> ref : cached) {
				R r = ref.get();
				if (r == null) {
					removeList.add(ref);
					continue;
				}
				try {
					r.close(); // 先关闭旧失效channel
					r.init();
				} catch (Exception ex) {
					log.error("repair consumer R fail, name=" + r.name, ex);
				}
			}
			// 清理已GC的弱引用
			cached.removeAll(removeList);
		}

		// 重建生产者Sender，关闭旧channel
		synchronized (senders) {
			for (Sender s : senders.values()) {
				try {
					s.close(); // 释放旧失效通道
					s.init(); // 创建新channel
				} catch (Exception ex) {
					log.error("repair sender fail, routingkey=" + s.routingkey, ex);
				}
			}
		}
	}

	@Override
	protected void _stop() {
		if (connection != null) {
			try {
				connection.close();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			connection = null;
		}
	}

	@Override
	public void destroy(String name, Mode mode) {
		if (connection != null) {

			try {

				// TODO

				if (log.isInfoEnabled()) {
					log.info("destory dest, name=" + name + ", mode=" + mode);
				}

			} catch (Exception e) {
				log.error("destory dest, name=" + name + ", mode=" + mode, e);
			}
		}
	}

}
