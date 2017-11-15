package org.giiwa.mq;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicLong;

import javax.jms.JMSException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.GLog;
import org.giiwa.mq.impl.*;

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
				// } else if (X.isSame(type, "rabbitmq")) {
				// mq = RabbitMQ.create();
			} else if (X.isSame(type, "kafkamq")) {
				mq = KafkaMQ.create();
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
			mq._bind(name, stub, mode);
		}
	}

	public static void bind(String name, IStub stub) throws Exception {
		bind(name, stub, Mode.QUEUE);
	}

	protected abstract void _bind(String name, IStub stub, Mode mode) throws Exception;

	static class Caller extends Task {

		private static Map<String, Caller> caller = new HashMap<String, Caller>();

		String name;
		IStub cb;
		LinkedBlockingDeque<Request> queue = new LinkedBlockingDeque<Request>();

		public static synchronized Caller get(String name, IStub cb) {
			Caller c = caller.get(name);
			if (c == null) {
				c = new Caller();
				c.cb = cb;
				c.name = name;
				caller.put(name, c);
				c.schedule(0);
			}
			return c;
		}

		void call(List<Request> l1) {
			synchronized (queue) {
				for (Request r : l1) {
					log.debug("push, r=" + new String(r.data));
					queue.addLast(r);
				}

				queue.notifyAll();
			}
		}

		@Override
		public void onExecute() {
			synchronized (queue) {
				if (queue.isEmpty()) {
					try {
						queue.wait(10000);
					} catch (InterruptedException e) {
						log.error(e.getMessage(), e);
					}
				}
			}
			while (!queue.isEmpty()) {
				Request r = null;
				synchronized (queue) {
					r = queue.pollFirst();
				}
				if (r != null) {
					cb.onRequest(r.seq, r);
				}
			}
		}

		@Override
		public void onFinish() {
			this.schedule(0);
		}

	}

	// static AtomicInteger caller = new AtomicInteger(0);
	static AtomicLong totalSent = new AtomicLong(0);
	static AtomicLong totalGot = new AtomicLong(0);

	protected static void process(final String name, final List<Request> rs, final IStub cb) {

		totalGot.incrementAndGet();

		Caller c = Caller.get(name, cb);

		log.debug("caller=" + c + ", name=" + name);
		c.call(rs);

	}

	protected abstract long _topic(String to, Request req) throws Exception;

	/**
	 * broadcast the message as "topic" to all "dest:to", and return immediately
	 * 
	 * @param to
	 *            the destination topic
	 * @param message
	 *            the message
	 * @param from
	 *            the source queue
	 * @param type
	 *            the message type
	 * @return the sequence of the message
	 * @throws Exception
	 *             the Exception
	 */

	public static long topic(String to, Request req) throws Exception {
		if (mq == null) {
			throw new Exception("MQ not init yet");
		}

		long s1 = seq.incrementAndGet();
		// if (Logger.isEnabled())
		// Logger.log(s1, "send", to, req);

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
		// if (Logger.isEnabled())
		// Logger.log(req.seq, "send", to, req);

		return mq._send(to, req);
	}

	/**
	 * send the request and wait the response until timeout
	 * 
	 * @param rpcname
	 *            the rpc name
	 * @param req
	 *            the request
	 * @param timeout
	 * @return the response
	 * @throws Exception
	 */
	public static Response call(String rpcname, Request req, int timeout) throws Exception {
		return RPC.call(rpcname, req, timeout);
	}

	public static void log(JSON p) {
		try {
			Request r = new Request();
			r.seq = 0;
			r.from = _node;
			r.type = 0;
			r.setBody(p.toString().getBytes());
			mq._send("logger", r);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	// public static void logger(boolean log) {
	// Logger.logger(log);
	// }

}
