package org.giiwa.core.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.service.IoHandlerAdapter;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.giiwa.core.base.Url;
import org.giiwa.core.bean.X;
import org.giiwa.core.nio.IRequestHandler;
import org.giiwa.core.nio.Server.RequestHandler;
import org.giiwa.core.task.Task;

public class Client extends IoHandlerAdapter implements Closeable, IResponseHandler, IRequestHandler {

	private static Log log = LogFactory.getLog(Client.class);

	protected String url;
	protected String host;
	protected int port;
	protected IoConnector connector;
	protected IoSession session;
	protected IRequestHandler handler;

	public static Client connect(String server, IRequestHandler handler) throws IOException {

		Url u = Url.create(server);
		Client c = new Client();

		if (X.isSame("tcp", u.getProtocol())) {

			c.connector = new NioSocketConnector();

			try {
				c.connector.setHandler(new RequestHandler(server, handler));
				ConnectFuture connFuture = c.connector.connect(new InetSocketAddress(u.getIp(), u.getPort(9091)));
				connFuture.awaitUninterruptibly();
				c.session = connFuture.getSession();
				return c;

			} catch (Exception e) {
				c.connector.dispose(true);
				c.connector = null;
				log.error(server, e);
				throw e;
			}

		} else {

			c.connector = new NioDatagramConnector();

			try {
				c.connector.setHandler(new RequestHandler(server, handler));

				ConnectFuture connFuture = c.connector.connect(new InetSocketAddress(u.getIp(), u.getPort(9091)));

				connFuture.awaitUninterruptibly();

				c.session = connFuture.getSession();

				return c;
			} catch (Exception e) {
				c.connector.dispose(true);
				c.connector = null;

				log.error(server, e);
				throw e;
			}

		}
	}

	public void close() {
		if (session != null) {
			session.closeNow();
			session = null;
		}
	}

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

	@Override
	public void process(Request r, IResponseHandler handler) {
		this.handler.process(r, handler);
	}

	@Override
	public void closed(String name) {
		close();
		handler.closed(name);
	}

	public static void main(String[] args) {

		Task.init(10);
		try {
			Client c = Client.connect("udp://127.0.0.1:9091", new IRequestHandler() {

				@Override
				public void process(Request r, IResponseHandler handler) {
					// TODO Auto-generated method stub
					System.out.println(r.seq + "=" + r.readString());
				}

				@Override
				public void closed(String name) {
					// TODO Auto-generated method stub

				}

			});

			Response r = Response.create(0);
			r.writeString("aaa");
			c.send(r);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}