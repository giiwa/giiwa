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
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.cache.Cache;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Counter;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.MD5;
import org.giiwa.task.Function;

/**
 * the distribute message system, <br>
 * the performance: sending 1w/300ms <br>
 * recving 1w/1500ms<br>
 * 
 * @author joe
 *
 */
public abstract class MQ {

	private static Log log = LogFactory.getLog(MQ.class);

	static String _node = X.EMPTY;
	private static AtomicLong seq = new AtomicLong(0);

	/**
	 * the message stub type <br>
	 * TOPIC: all stub will read it <br>
	 * QUEUE: only one will read it
	 * 
	 * @author joe
	 *
	 */
	public static enum Mode {
		TOPIC, QUEUE, BOTH
	};

	private static MQ mq = null;

	/**
	 * initialize the MQ with the configuration in DB
	 * 
	 * @return true if success or false if failed.
	 */
	public synchronized static boolean init() {

		if (mq == null) {

			_node = Local.id();

			String type = Global.getString("mq.type", X.EMPTY);
			if (X.isSame(type, "activemq")) {
				mq = ActiveMQ.create();
			} else if (X.isSame(type, "rocketmq")) {
				mq = RocketMQ.create();
			} else if (X.isSame(type, "mqtt")) {
				mq = MQTT.create();
			} else {
				mq = LocalMQ.create();
			}

			try {
				new Notify().bindAs(Mode.TOPIC);
				RPC.inst.bind();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			log.info("MQ.inited, mq=" + mq);

		}

		return mq != null;
	}

	/**
	 * test configured MQ
	 * 
	 * @return true if configured
	 */
	public static boolean isConfigured() {
		return mq != null;
	}

	/**
	 * initialize the MQ with the node and url, the default is using ActiveMQ
	 * 
	 * @param node  the local node name, MUST unique
	 * @param group the group name
	 * @param url   the activemq URL
	 * @return true if success or false if failed.
	 */
	public static boolean init(String node, String group, String url) {

		if (url.startsWith("failover:")) {
			Global.setConfig("mq.type", "activemq");
			Global.setConfig("activemq.url", url);
		} else {
			Global.setConfig("mq.type", "kafkamq");
			Global.setConfig("kafkamq.url", url);
		}
		Config.getConf().setProperty("node.name", node);
		Global.setConfig("site.group", group);
		MQ._node = node;
		return init();
	}

	/**
	 * listen on the name
	 * 
	 * @param name the service name
	 * @param stub the Stub object
	 * @param mode the bind mode (topic, queue)
	 * @throws JMSException the Exception
	 */
	public static void bind(String name, IStub stub, Mode mode) throws Exception {
		if (mq == null) {
//			GLog.applog.warn(org.giiwa.app.web.admin.mq.class, "bind",
//					"failed bind, [" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);

			throw new Exception("MQ not init yet");
		} else {
			// if (Mode.TOPIC.equals(mode)) {
			// NOTIFY.bind(name, stub);
			// return;
			// }
			mq._bind(name, stub, mode);
		}
	}

	public static void bind(String name, IStub stub) throws Exception {
		bind(name, stub, Mode.QUEUE);
	}

	public static void unbind(IStub stub) throws Exception {
		mq._unbind(stub);
	}

	protected abstract void _bind(String name, IStub stub, Mode mode) throws Exception;

	protected abstract void _unbind(IStub stub) throws Exception;

	protected abstract void _stop();

	protected static void process(final String name, final List<Request> rs, final IStub cb) {
		if (cb == null) {
			return;
		}

		for (Request r : rs) {
			try {
				if (r.tt > 0)
					read.add(Global.now() - r.tt, null);

				r._from = cb;

				cb.onRequest(r.seq, r);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

	}

	protected abstract long _topic(String to, Request req) throws Exception;

	/**
	 * send message to topic and waiting response
	 * 
	 * @param to
	 * @param req
	 * @param timeout
	 * @param func
	 * @return
	 * @throws Exception
	 */
//	public static long topic(String to, JSON req, long timeout, Function<Request, Boolean> func) throws Exception {
//
//		LiveHand door = LiveHand.create(1, 0);
//		String name = to + "_" + Global.now();
//		IStub st = new IStub(name) {
//			@Override
//			public void onRequest(long seq, Request req) {
//				try {
//					if (func.apply(req)) {
//						door.release();
//					}
//				} catch (Exception e) {
//					log.error(e.getMessage(), e);
//				}
//			}
//		};
//
//		try {
//			st.bind();
//
//			MQ.topic(Task.MQNAME, Request.create().put(req.append("from", name)));
//
//			door.await(timeout);
//		} finally {
//			st.destroy();
//		}
//		return 0;
//
//	}

	/**
	 * broadcast the message as "topic" to all "dest:to", and return immediately
	 * 
	 * @param to  the destination topic
	 * @param req the message
	 * @return the sequence of the message
	 * @throws Exception the Exception
	 */
	public static long topic(String to, Request req) throws Exception {

		if (mq == null) {
			throw new Exception("MQ not init yet");
		}

		TimeStamp t = TimeStamp.create();
		try {
			if (req.seq <= 0) {
				long s1 = seq.incrementAndGet();
				req.seq = s1;
			}

			req.tt = Global.now();
			mq._topic(to, req);
			return req.seq;
		} finally {
			write.add(t.pastms(), null);
		}
	}

	protected abstract long _send(String to, Request req) throws Exception;

	/**
	 * send the message and return immediately
	 * 
	 * @param to  the destination queue name
	 * @param req the message
	 * @return the sequence of the payload
	 * @throws Exception the Exception
	 */
	public static long send(String to, Request req) throws Exception {
		TimeStamp t = TimeStamp.create();
		try {

			if (req.seq <= 0) {
				req.seq = seq.incrementAndGet();
			}
			req.tt = Global.now();

			return mq._send(to, req);
		} catch(Exception e) {
			GLog.applog.error("mq", "send", "send failed", e);
			throw e;
		} finally {
			write.add(t.pastms(), null);
		}
	}

	/**
	 * send message to MQ and wait response
	 * 
	 * @param <T>
	 * @param name
	 * @param cmd
	 * @param obj
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public static <T> T callQueue(String name, String cmd, Serializable obj, long timeout) throws Exception {
		Request req = Request.create().put(obj);
		req.cmd = cmd;
		return RPC.call(name, req, timeout);
	}

	/**
	 * send message to MQ and wait response
	 * 
	 * @param <T>
	 * @param name
	 * @param req
	 * @param timeout
	 * @return
	 * @throws Exception
	 */
	public static <T> T callQueue(String name, Request req, long timeout) throws Exception {
		return RPC.call(name, req, timeout);
	}

	/**
	 * @deprecated
	 * 
	 * @param name
	 * @param req
	 * @param timeout
	 * @param func
	 * @return
	 * @throws Exception
	 */
	public static boolean callTopic(String name, JSON req, long timeout, Function<Request, Boolean> func)
			throws Exception {
		return callTopic(name, null, req, timeout, func);
	}

	/**
	 * 
	 * @param name
	 * @param cmd
	 * @param obj
	 * @param timeout
	 * @param func
	 * @return
	 * @throws Exception
	 */
	public static boolean callTopic(String name, String cmd, Serializable obj, long timeout,
			Function<Request, Boolean> func) throws Exception {
		Request r = Request.create().put(obj);
		r.cmd = cmd;
		return callTopic(name, r, timeout, func);
	}

	public static boolean callTopic(String name, Request req, long timeout, Function<Request, Boolean> func)
			throws Exception {
		return RPC.call(name, req, timeout, func);
	}

	public static class Request {

		public byte ver = 1;
		public long seq = -1;
		public int type = 0;

		public long tt = -1; // timestamp
		public String from; // reply path
		public String cmd; // command
		public int priority = 1;
		public final int ttl = (int) X.AMINUTE * 10;
		public int persistent = DeliveryMode.PERSISTENT;// NON_PERSISTENT;
		public byte[] data;

		transient IStub _from;

		@Override
		public String toString() {
			return "Request [seq=" + seq + ", from=" + from + "]";
		}

		public DataInputStream getInput() {
			return new DataInputStream(new ByteArrayInputStream(data));
		}

		public DataOutputStream getOutput() {
			final ByteArrayOutputStream bb = new ByteArrayOutputStream();
			return new DataOutputStream(bb) {

				@Override
				public void close() throws IOException {
					super.close();
					data = bb.toByteArray();
				}

			};
		}

		public static Request create() {
			return new Request();
		}

		public Request seq(long seq) {
			this.seq = seq;
			return this;
		}

		public Request type(int type) {
			this.type = type;
			return this;
		}

		public Request from(String from) {
			this.from = from;
			return this;
		}

//		public Request ttl(long t) {
//			this.ttl = (int) t;
//			return this;
//		}

		public Request put(Object obj) throws Exception {

			this.data = X.getBytes(obj);

			if (this.data != null && this.data.length > 1024) {
				if (log.isDebugEnabled()) {
					log.debug("data.length=" + data.length + ", class=" + obj.getClass());
				}
				String dataid = "mq/=" + MD5.md5(this.data);
				Cache.set(dataid, this.data, X.AMINUTE);
				this.data = dataid.getBytes();
			}

			return this;

		}

		public <T> T get() throws Exception {

			if (data == null || data.length == 0) {
				return null;
			}

			if (data.length == 36 && data[0] == 'm' && data[1] == 'q' && data[2] == '/' && data[3] == '=') {
				// 6D712F3D
				String dataid = new String(data);

				Object o = Cache.get(dataid);
				T t = X.fromBytes((byte[]) o);
				return t;
			}
			try {
				return X.fromBytes(data);
			} catch (Exception e) {
				log.error("data=" + data.length + ", data=" + new String(data), e);
				throw e;
			}

		}

		public void response(Object data) throws Exception {

			Request r = Request.create();
			r.seq = seq;
			r.ver = ver;
			r.type = 200;

			r.put(data);

			if (this._from == null) {
				MQ.send(this.from, r);
			} else {
				this._from.send(this.from, r);
			}

		}

		public void responseError(Object data) throws Exception {

			Request r = Request.create();
			r.seq = seq;
			r.ver = ver;
			r.type = 201;

			r.put(data);

			MQ.send(from, r);

		}

		public void error(Throwable e) {

			Request r = Request.create();
			r.seq = seq;
			r.ver = ver;
			r.type = 201;

			try {
				r.put(e.getMessage());

				MQ.send(this.from, r);
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
		}

		public void reply(Request req) throws Exception {
			req.seq = seq;
			req.from = Local.label();
			MQ.send(this.from, req);
		}

		public byte[] packet() {

			return null;
		}

	}

	public static class Response extends Request {

		int state;
		String error;

		@Override
		public String toString() {
			return "Response [seq=" + seq + "]";
		}

		public void copy(Request r) {
			ver = r.ver;
			seq = r.seq;
			tt = r.tt;
			type = r.type;
			data = r.data;
			from = r.from;
		}

	}

	public static void notify(String name, Object data) {
		try {
			MQ.topic(Notify.name, Request.create().from(name).put(data));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static <T> T wait(String name, long timeout) {
		return Notify.wait(name, timeout, null);
	}

	public static <T, R> R wait(String name, long timeout, Function<T, R> prepare) {
		return Notify.wait(name, timeout, prepare);
	}

	public static <T> Result<T> create(String name) throws Exception {
		Result<T> q = Result.create(name);
		q.bind();
		return q;
	}

	private static Counter read = new Counter("read");
	private static Counter write = new Counter("write");

	public static Counter.Stat statRead() {
		return read.get();
	}

	public static Counter.Stat statWrite() {
		return write.get();
	}

	public static void stop() {
		if (mq != null) {
			mq._stop();
			mq = null;
		}
	}

	static void destroy0(String name, Mode mode) throws Exception {
		mq.destroy(name, mode);
	}

	public abstract void destroy(String name, Mode mode) throws Exception;

}
