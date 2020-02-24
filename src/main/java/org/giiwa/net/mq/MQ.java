package org.giiwa.net.mq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Counter;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.dao.bean.GLog;
import org.giiwa.json.JSON;
import org.giiwa.task.SysTask;
import org.giiwa.task.Task;

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
		TOPIC, QUEUE
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
			} else if (X.isSame(type, "kafkamq")) {
				mq = KafkaMQ.create();
			} else {
				mq = LocalMQ.create();
			}

			try {
				new Notify().bind(Mode.TOPIC);
				new RPC().bind();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			log.debug("MQ.inited, mq=" + mq);
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
			GLog.applog.warn(org.giiwa.app.web.admin.mq.class, "bind",
					"failed bind, [" + name + "], stub=" + stub.getClass().toString() + ", mode=" + mode, null, null);

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

	static class Caller extends SysTask {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		String name;
		IStub cb;
		List<Request> queue;

		@Override
		public String toString() {
			return "Caller [name=" + name + ", cb=" + cb + "]";
		}

		static Task call(String name, IStub cb, List<Request> l1) {
			Caller c = new Caller();
			c.cb = cb;
			c.name = name;
			c.queue = l1;
			c.schedule(0);
			return c;
		}

		@Override
		public void onExecute() {
			while (!queue.isEmpty()) {
				try {
					Request r = queue.remove(0);
					if (r != null) {

						if (r.tt > 0)
							read.add(System.currentTimeMillis() - r.tt);

						cb.onRequest(r.seq, r);
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

	}

	// static AtomicInteger caller = new AtomicInteger(0);
//	static AtomicLong totalSent = new AtomicLong(0);
//	static AtomicLong totalGot = new AtomicLong(0);

	protected static void process(final String name, final List<Request> rs, final IStub cb) {

		Caller.call(name, cb, rs);

	}

	protected abstract long _topic(String to, Request req) throws Exception;

	/**
	 * broadcast the message as "topic" to all "dest:to", and return immediately
	 * 
	 * @param to  the destination topic
	 * @param req the message
	 * @return the sequence of the message
	 * @throws Exception the Exception
	 */

	public static long topic(String to, Request req) throws Exception {

		// FileClient.notify(to, req.get());

		if (mq == null) {
			throw new Exception("MQ not init yet");
		}

		TimeStamp t = TimeStamp.create();
		try {
			long s1 = seq.incrementAndGet();
			if (log.isDebugEnabled())
				log.debug("send topic to [" + to + "], seq=" + s1);

			req.seq = s1;
			req.tt = System.currentTimeMillis();
			mq._topic(to, req);
			return s1;
		} finally {
			write.add(t.pastms());
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
			req.tt = System.currentTimeMillis();

			return mq._send(to, req);
		} finally {
			write.add(t.pastms());
		}
	}

	public static <T> T call(String name, Request req, long timeout) throws Exception {
		return RPC.call(name, req, timeout);
	}

	public static class Request {

		public byte ver = 1;
		public long seq = -1;
		public int type = 0;

		public long tt = -1; // timestamp
		public String from;
		public int priority = 1;
		public int ttl = (int) X.AMINUTE;
		public int persistent = DeliveryMode.NON_PERSISTENT;
		public byte[] data;

		@Override
		public String toString() {
			return "Request [seq=" + seq + "]";
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

		public void setBody(byte[] bb) {
			data = bb;
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

		public Request data(byte[] data) {
			this.data = data;
			return this;
		}

		public Request ttl(long t) {
			this.ttl = (int) t;
			return this;
		}

		public Request put(Object obj) throws Exception {

			data = X.getBytes(obj, true);
			return this;
		}

		public <T> T get() throws Exception {
			return X.fromBytes(data, true);
		}

		public void response(Object data) throws Exception {

			Request r = Request.create();
			r.seq = seq;
			r.ver = ver;
			r.type = 200;

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

				MQ.send(from, r);
			} catch (Exception e1) {
				log.error(e1.getMessage(), e1);
			}
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

	public static <T> T wait(String name, long timeout, Runnable prepare) {
		return Notify.wait(name, timeout, prepare);
	}

	public static <T> Result<T> create(String name) throws Exception {
		Result<T> q = Result.create(name);
		q.bind();
		return q;
	}

	private static Counter read = new Counter("read");
	private static Counter write = new Counter("write");

	public static JSON statRead() {
		return read.get();
	}

	public static JSON statWrite() {
		return write.get();
	}

}
