
package org.giiwa.core.dfile;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Config;
import org.giiwa.core.dfile.command.DELETE;
import org.giiwa.core.dfile.command.GET;
import org.giiwa.core.dfile.command.HTTP;
import org.giiwa.core.dfile.command.INFO;
import org.giiwa.core.dfile.command.LIST;
import org.giiwa.core.dfile.command.MKDIRS;
import org.giiwa.core.dfile.command.PUT;
import org.giiwa.core.dfile.command.MOVE;
import org.giiwa.core.nio.IoProtocol;
import org.giiwa.core.nio.Server;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Disk;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;

public class FileServer implements IRequestHandler {

	private static Log log = LogFactory.getLog(FileServer.class);

	public static FileServer inst = new FileServer();

	public static String URL = "tcp://127.0.0.1:9091";

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

				URL = Config.getConf().getString("dfile.url", "tcp://127.0.0.1:9091");

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
				public void onRequest(long seq, org.giiwa.mq.MQ.Request req) {
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

		Task.schedule(() -> {
			byte cmd = in.readByte();
			// System.out.println("cmd=" + cmd);

			ICommand c = commands.get(cmd);
			if (c != null) {
				// ICommand.log.debug("cmd=" + cmd + ", processor=" + c);
				c.process(in, handler);
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

}
