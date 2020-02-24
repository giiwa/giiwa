
package org.giiwa.dfile;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.giiwa.bean.Disk;
import org.giiwa.conf.Config;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dfile.command.DELETE;
import org.giiwa.dfile.command.GET;
import org.giiwa.dfile.command.HTTP;
import org.giiwa.dfile.command.INFO;
import org.giiwa.dfile.command.LIST;
import org.giiwa.dfile.command.MKDIRS;
import org.giiwa.dfile.command.MOVE;
import org.giiwa.dfile.command.PUT;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ;
import org.giiwa.net.nio.IoProtocol;
import org.giiwa.net.nio.Server;
import org.giiwa.task.Task;

public class FileServer implements IRequestHandler {

	private static Log log = LogFactory.getLog(FileServer.class);

	public static FileServer inst = new FileServer();

	public static String URL = "tcp://0.0.0.0:9091";

	/**
	 * the number of call times
	 */
	public static AtomicLong times = new AtomicLong(0);

	/**
	 * the total cost of calling
	 */
	public static AtomicLong costs = new AtomicLong(0);

	/**
	 * the max cost
	 */
	public static long maxcost = Long.MIN_VALUE;

	/**
	 * the min cost
	 */
	public static long mincost = Long.MAX_VALUE;

	private static Map<Byte, ICommand> commands = new HashMap<Byte, ICommand>();

	private Server serv;

	public void stop() {
		X.close(serv);
	}

	public void start() {

		if (serv == null) {
			try {
				commands.put(ICommand.CMD_DELETE, new DELETE());
				commands.put(ICommand.CMD_INFO, new INFO());
				commands.put(ICommand.CMD_GET, new GET());
				commands.put(ICommand.CMD_PUT, new PUT());
				commands.put(ICommand.CMD_LIST, new LIST());
				commands.put(ICommand.CMD_MKDIRS, new MKDIRS());
				commands.put(ICommand.CMD_MOVE, new MOVE());
				commands.put(ICommand.CMD_HTTP, new HTTP());

				URL = Config.getConf().getString("dfile.bind", URL);

				serv = Server.bind(URL, new RequestHandler(URL, this));

			} catch (Exception e) {

				log.error(URL, e);

				Task.schedule(() -> {
					start();
				}, 3000);
			}

			_bind();

		}
	}

	private void _bind() {
		try {
			// listen the reset
			new IStub("disk.reset") {

				@Override
				public void onRequest(long seq, org.giiwa.net.mq.MQ.Request req) {
					Disk.reset();
				}

			}.bind(MQ.Mode.TOPIC);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			Task.schedule(() -> {
				_bind();
			}, 3000);
		}
	}

	public static void reset() {
		// listen the reset
		try {
			MQ.topic("disk.reset", MQ.Request.create().put("reset"));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public void process(Request in, IResponseHandler handler) {

		TimeStamp t = TimeStamp.create();
		Task.schedule(() -> {
			byte cmd = in.readByte();
			// System.out.println("cmd=" + cmd);

			ICommand c = commands.get(cmd);
			if (c != null) {
				// ICommand.log.debug("cmd=" + cmd + ", processor=" + c);
				c.process(in, handler);

				costs.addAndGet(t.pastms());
				times.incrementAndGet();
				if (maxcost < t.pastms()) {
					maxcost = t.pastms();
				}
				if (mincost > t.pastms()) {
					mincost = t.pastms();
				}
				
//				if (log.isDebugEnabled())
//					log.debug("process, cmd=" + cmd + ", cost=" + t.past());
				
			} else {
				Response out = Response.create(in.seq, Request.SMALL);
				out.writeString("unknown cmd");
				handler.send(out);
			}
		});

	}

	@Override
	public void closed(String name) {
		// do nothing
	}

	public void shutdown() {
		if (serv != null) {
			X.close(serv);
			serv = null;
		}
	}

	static class RequestHandler extends IoProtocol {

		private String name;
		private IRequestHandler handler;

		public RequestHandler(String name, IRequestHandler handler) {
			this.name = name;
			this.handler = handler;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
			session.closeNow();
			log.error(session.getRemoteAddress(), cause);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void messageReceived(IoSession session, IoBuffer message) throws Exception {

			IoBuffer b = message;

			IoBuffer bb = (IoBuffer) session.getAttribute("bb");
			if (bb == null) {
				bb = IoBuffer.allocate(Request.BIG);
				bb.setAutoExpand(true);
				session.setAttribute("bb", bb);
			}
			bb.put(b.buf());

			bb.flip();

			Request r = Request.read(bb);
			while (r != null) {
				handler.process(r, new IResponseHandler() {
					@Override
					public void send(Response resp) {
						resp.out.flip();
						IoBuffer b = IoBuffer.allocate(resp.out.remaining() + 4);
						b.putInt(resp.out.remaining());
						b.put(resp.out);
						b.flip();
						session.write(b);
						b.free();
						resp.out.free();
					}

				});
				r = Request.read(bb);
			}

			bb.compact();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void sessionClosed(IoSession session) throws Exception {
			session.closeNow();
			log.info("closed, session=" + session.getRemoteAddress());
			handler.closed(name);
		}

		@Override
		public void sessionOpened(IoSession session) throws Exception {
			session.getConfig().setBothIdleTime(180);
			super.sessionOpened(session);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void sessionCreated(IoSession session) throws Exception {
			log.info("created, session=" + session.getRemoteAddress());
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void sessionIdle(IoSession session, IdleStatus status) throws Exception {
			log.info("idle, session=" + session.getRemoteAddress());
		}

	}

	public static void measures(V v) {
		v.append("dfiletimes", FileServer.times.get());

		if (FileServer.times.get() > 0) {
			v.append("dfileavgcost", FileServer.costs.get() / FileServer.times.get());
			v.append("dfilemaxcost", FileServer.maxcost);
			v.append("dfilemincost", FileServer.mincost);
		} else {
			v.append("dfileavgcost", 0);
			v.append("dfilemaxcost", 0);
			v.append("dfilemincost", 0);
		}

	}

	public static void resetm() {

		FileServer.times.set(0);
		FileServer.costs.set(0);
		FileServer.maxcost = Long.MIN_VALUE;
		FileServer.mincost = Long.MAX_VALUE;

	}

}
