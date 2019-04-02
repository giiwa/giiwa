package org.giiwa.mq;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.DeliveryMode;
import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.task.SysTask;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.GLog;

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
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return mq != null;
	}

	/**
	 * test configured MQ
	 * 
	 * @return
	 */
	public static boolean isConfigured() {
		return mq != null;
	}

	/**
	 * initialize the MQ with the node and url, the default is using ActiveMQ
	 * 
	 * @param node
	 *            the local node name, MUST unique
	 * @param url
	 *            the activemq URL
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
	 * @param name
	 *            the service name
	 * @param stub
	 *            the Stub object
	 * @param mode
	 *            the bind mode (topic, queue)
	 * @throws JMSException
	 *             the Exception
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
						cb.onRequest(r.seq, r);
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

	}

	// static AtomicInteger caller = new AtomicInteger(0);
	static AtomicLong totalSent = new AtomicLong(0);
	static AtomicLong totalGot = new AtomicLong(0);

	protected static void process(final String name, final List<Request> rs, final IStub cb) {

		totalGot.incrementAndGet();

		Caller.call(name, cb, rs);

	}

	protected abstract long _topic(String to, Request req) throws Exception;

	/**
	 * broadcast the message as "topic" to all "dest:to", and return immediately
	 * 
	 * @param to
	 *            the destination topic
	 * @param req
	 *            the message
	 * @return the sequence of the message
	 * @throws Exception
	 *             the Exception
	 */

	public static long topic(String to, Request req) throws Exception {

		// FileClient.notify(to, req.get());

		if (mq == null) {
			throw new Exception("MQ not init yet");
		}

		long s1 = seq.incrementAndGet();
		if (log.isDebugEnabled())
			log.debug("send topic to [" + to + "], seq=" + s1 + ", req=" + req);

		req.seq = s1;
		mq._topic(to, req);
		return s1;
	}

	protected abstract long _send(String to, Request req) throws Exception;

	/**
	 * send the message and return immediately
	 * 
	 * @param to
	 *            the destination queue name
	 * @param from
	 *            the source queue
	 * @param type
	 *            the message type
	 * @param message
	 *            the message
	 * @return the sequence of the payload
	 * @throws Exception
	 *             the Exception
	 */
	public static long send(String to, Request req) throws Exception {
		if (req.seq <= 0) {
			req.seq = seq.incrementAndGet();
		}

		return mq._send(to, req);
	}

	// public static void logger(boolean log) {
	// Logger.logger(log);
	// }

	public static class Request {

		public long seq = -1;
		public int type = 0;

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

		public Request put(Serializable t) {
			if (t != null) {
				ByteArrayOutputStream bb = new ByteArrayOutputStream();
				try {
					ObjectOutputStream out = new ObjectOutputStream(bb);
					out.writeObject(t);
				} catch (Exception e) {
					log.error(t.toString(), e);
				} finally {
					X.close(bb);
				}
				data = bb.toByteArray();
			}
			return this;
		}

		@SuppressWarnings("unchecked")
		public <T> T get() {
			if (data == null || data.length == 0)
				return null;

			ByteArrayInputStream bb = new ByteArrayInputStream(data);
			try {
				ObjectInputStream in = new ObjectInputStream(bb);
				return (T) in.readObject();
			} catch (Exception e) {
				log.error(e.getMessage());
				// log.error(e.getMessage(), e);
			} finally {
				X.close(bb);
			}
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
			seq = r.seq;
			type = r.type;
			data = r.data;
			from = r.from;
		}

	}

	public static void notify(String name, Serializable data) {
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

}
