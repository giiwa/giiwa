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

import java.io.IOException;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.task.SysTask;
import org.giiwa.task.Task;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.StreamEntryID;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.jedis.params.XAddParams;
import redis.clients.jedis.params.XReadGroupParams;
import redis.clients.jedis.params.XTrimParams;
import redis.clients.jedis.resps.StreamEntry;

/**
 * RabbitMQ， 性能好, 单节点支持10-20万TPS
 * 
 * @author joe
 *
 */
class RedisMQ extends MQ {

	private static Log log = LogFactory.getLog(RedisMQ.class);

	private String group = X.EMPTY;
	@SuppressWarnings("deprecation")
	private JedisPool pool;
	public String url;

	/**
	 * Creates the.
	 *
	 * @return the mq
	 */
	@SuppressWarnings({ "deprecation" })
	public static MQ create() {

		RedisMQ m = new RedisMQ();

		JedisPoolConfig config = new JedisPoolConfig();
		config.setTestOnBorrow(true);
		config.setMaxTotal(60);
		config.setMaxIdle(15);
		config.setMinIdle(5);
		config.setTestWhileIdle(true);
		config.setTimeBetweenEvictionRunsMillis(30000);
		config.setMaxWaitMillis(3000);

		// 127.0.0.1:6379
		String url = Global.getString("redismq.url", null);
		String[] ss = url.split(":");
		String host = ss[0];
		int port = 6379;
		if (ss.length > 1) {
			port = X.toInt(ss[1], 0);
		}

		String user = Global.getString("redismq.user", null);
		String passwd = Global.getString("redismq.passwd", null);

		if (X.isEmpty(user)) {
			// 本地 Redis，根据实际修改地址/端口
			m.pool = new JedisPool(config, host, port, 1000);
		} else {
			// 本地 Redis，根据实际修改地址/端口/密码
			m.pool = new JedisPool(config, host, port, 1000, user, passwd);
		}

		m.group = Global.getString("site.group", "demo");
		if (!m.group.endsWith(".")) {
			m.group += ".";
		}

		try {

			GLog.applog.info("sys", "startup", "connected RedisMQ with [" + url + "]", null, null);

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("sys", "startup", "failed RedisMQ with [" + url + "]", e, null, null);
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
	public class R extends SysTask {

		private static final long serialVersionUID = 1L;

//		private String name;
		private String stream;
		private String group;

		IStub cb;
		TimeStamp t = TimeStamp.create();

		long total = 0;
		long drop = 0;
		long error = 0;

		private boolean stopped = false;

		@Override
		public String toString() {
			return "R [name=" + stream + "]";
		}

		/**
		 * Close.
		 */
		public void close() {
			stopped = true;
		}

		@Override
		public String getName() {
			return "mq.r." + stream;
		}

		private R(String name, IStub cb, Mode mode) throws IOException {

//			this.mode = mode;
//			this.name = name;
			if (mode.equals(Mode.QUEUE)) {
				this.group = name;
				this.stream = name + ":" + Mode.QUEUE;
			} else {
				this.stream = name + ":" + Mode.TOPIC;
				this.group = Local.id();
			}

			this.cb = cb;

			if (pool != null) {

				if (log.isWarnEnabled()) {
					log.warn("bind queue [" + name + "]");
				}
				this.schedule(0);

				cached.add(new WeakReference<R>(this));

			} else {
				if (log.isDebugEnabled()) {
					log.debug("MQ not init yet!");
				}
				throw new IOException("MQ not init yet!");
			}

		}

		public void handle(Map<String, String> data) throws IOException {

			total++;

			Request r = new Request();
			r.seq = X.toLong(data.get("seq"));
			r.ver = Byte.parseByte(data.get("ver"));
			r.tt = Long.parseLong(data.get("tt"));
			if (Global.now() - r.tt > X.AMINUTE * 5) {
				// 1分钟前的消息，过期，不处理
				drop++;
				return;
			}

			r.from = data.get("from");
			r.type = Integer.parseInt(data.get("type"));
			r.cmd = data.get("cmd");
			if (data.containsKey("data")) {
				r.data = Base64.getDecoder().decode(data.get("data"));
			}

			process(stream, Arrays.asList(r), cb);

			if (log.isDebugEnabled()) {
				log.debug("got: 1 in one packet, stream=" + stream + ", cb=" + cb);
			}

		}

		@SuppressWarnings("deprecation")
		@Override
		public void onExecute() {

			boolean created = false;

			Map<String, StreamEntryID> streamMap = new HashMap<>();
			// 从上一次消费位置继续读取
			streamMap.put(stream, StreamEntryID.UNRECEIVED_ENTRY);

			XReadGroupParams params = XReadGroupParams.xReadGroupParams().count(1).block(5000);

			String consumer = Local.id();

			while (!stopped) {

				try (Jedis jedis = pool.getResource()) {
					if (!created) {
						// 没有创建，则创建消费组，在一个消费组中，可以有很多消费者，轮流消费数据
						try {
							log.info("binding: " + stream + ":" + group);
							jedis.xgroupCreate(stream, group, StreamEntryID.LAST_ENTRY, true);
						} catch (JedisDataException e) {
							GLog.applog.error("mq", "r", e.getMessage(), e);
							if (e.getMessage().contains("BUSYGROUP")) {
								// 无需处理
							} else {
								// 其它错误继续抛出
								throw e;
							}
						}
						created = true;
					}

					List<Map.Entry<String, List<StreamEntry>>> result = jedis.xreadGroup(group, consumer, params,
							streamMap);
//					log.warn("read=" + result);
					if (result == null || result.isEmpty()) {
						// 清除5分钟前的消息
						long expireTime = Global.now() - 300 * 1000;
						String minId = expireTime + "-0";
						XTrimParams trimParams = XTrimParams.xTrimParams().minId(minId);
						jedis.xtrim(stream, trimParams);
						continue;
					}

					for (Map.Entry<String, List<StreamEntry>> entry : result) {
						for (StreamEntry streamEntry : entry.getValue()) {
							Map<String, String> data = streamEntry.getFields();
							handle(data);
							// 手动ACK
							jedis.xack(stream, group, streamEntry.getID());
						}
					}
				} catch (Exception e) {
					GLog.applog.error("mq", "r", e.getMessage(), e);

					error++;
					log.error(e.getMessage(), e);
					try {
						// 出现等待100ms，防止死循环
						synchronized (e) {
							e.wait(100);
						}
					} catch (Exception err) {
						// ignore
					}
				}
			}
		}
	}

	@Override
	protected void _bind(String name, IStub stub, Mode mode) throws Exception {
		if (pool == null) {
			throw new Exception("MQ not init yet");
		}
		new R(name, stub, mode);
	}

	@Override
	protected long _topic(String name, MQ.Request r) throws Exception {

		if (pool == null) {
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
	protected long _send(String to, MQ.Request r) throws Exception {

		if (pool == null) {
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

	private Sender getSender(String name, MQ.Mode type) throws IOException {

		String name1 = name + ":" + type;
		if (pool == null) {
			return null;
		}

		if (senders.containsKey(name1)) {
			return senders.get(name1);
		}

		Sender s = null;
		s = new Sender(name1);
		senders.put(name1, s);

		if (log.isInfoEnabled()) {
			log.info("create a new producer, =>" + name + ", mode=" + type + ", senders=" + senders.keySet());
		}
		return s;
	}

	/**
	 * queue producer cache
	 */
	private static Map<String, Sender> senders = new HashMap<String, Sender>();

	class Sender {

		long last = Global.now();
		String name;
		boolean inited = false;

		// 保留最新 2000 条，旧消息自动淘汰
		XAddParams addParams = XAddParams.xAddParams().maxLen(2000).approximateTrimming();

		public void send(Request r) throws IOException {

			last = Global.now();

			Map<String, String> data = new HashMap<>();

			data.put("seq", Long.toString(r.seq));
			data.put("ver", Byte.toString(r.ver));
			data.put("tt", Long.toString(Global.now()));// Long.toString(r.tt));
			if (r.from != null) {
				data.put("from", r.from);
			}
			data.put("type", Integer.toString(r.type));
			if (r.cmd != null) {
				data.put("cmd", r.cmd);
			}

			if (r.data != null && r.data.length > 0) {
				data.put("data", Base64.getEncoder().encodeToString(r.data));
			}

			try (Jedis p = pool.getResource()) {
				// XADD 写入 Stream
				StreamEntryID msgId = p.xadd(name, addParams, data);
				if (log.isDebugEnabled()) {
					log.debug("send to [" + name + "], id=" + msgId);
				}
			} catch (Exception err) {
				log.error(err.getMessage(), err);
				GLog.applog.error("mq", "send", err.getMessage(), err);
			}
		}

		public Sender(String name) {
			this.name = name;
		}

		public String getName() {
			return "sender." + name;
		}

		public void close() {
			try {
//				p.queueUnbind(exchange, exchange, routingkey);
				log.warn("close [" + name + "], senders=" + senders.keySet());
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

	@Override
	protected void _stop() {
		if (pool != null) {
			try {
				pool.close();
				pool.destroy();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
			pool = null;
		}
	}

	@Override
	public void destroy(String name, Mode mode) {
		if (pool != null) {

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
