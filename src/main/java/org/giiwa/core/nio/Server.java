package org.giiwa.core.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.giiwa.core.base.Url;
import org.giiwa.core.task.Task;

public class Server implements Closeable {

	private static Log log = LogFactory.getLog(Server.class);

	private AbstractIoAcceptor acceptor;
	private Url url;

	public Server(Url url, IRequestHandler handler) {
		this.url = url;
		if (url.isProtocol("tcp")) {
			acceptor = new NioSocketAcceptor();
			acceptor.setHandler(new RequestHandler(url.getUrl(), handler));
		} else if (url.isProtocol("udp")) {
			acceptor = new NioDatagramAcceptor();
			acceptor.setHandler(new RequestHandler(url.getUrl(), handler));
		}

	}

	public static Server bind(String url, IRequestHandler handler) {
		Server s = new Server(Url.create(url), handler);
		s._bind();
		log.info("nio server bind on [" + url + "]");
		return s;
	}

	private void _bind() {
		if (acceptor == null)
			return;

		try {
			acceptor.bind(new InetSocketAddress(url.getIp(), url.getPort(9091)));

		} catch (IOException e) {
			log.error(url.toString(), e);
			Task.schedule(() -> {
				_bind();
			}, 3000);
		}

	}

	@Override
	public void close() throws IOException {
		acceptor.dispose(true);
		acceptor = null;
	}

	static class RequestHandler extends IoHandlerAdapter {

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
		public void messageReceived(IoSession session, Object message) throws Exception {

			IoBuffer b = (IoBuffer) message;

			IoBuffer bb = (IoBuffer) session.getAttribute("bb");
			if (bb == null) {
				bb = IoBuffer.allocate(Request.BUFFER_SIZE);
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

	public static void main(String[] args) {

		Task.init(10);
		Server.bind("udp://127.0.0.1:9091", new IRequestHandler() {

			@Override
			public void process(Request r, IResponseHandler handler) {
				// TODO Auto-generated method stub
				System.out.println(r.seq + "=" + r.readString());
				Response r1 = Response.create(r.seq);
				r1.writeString("ok");
				handler.send(r1);
			}

			@Override
			public void closed(String name) {
				// TODO Auto-generated method stub

			}

		});
	}

}
