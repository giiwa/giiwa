package org.giiwa.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.future.ConnectFuture;
import org.apache.mina.core.service.IoConnector;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioDatagramConnector;
import org.apache.mina.transport.socket.nio.NioSocketConnector;
import org.giiwa.misc.Url;

public class Client implements Closeable {

	private static Log log = LogFactory.getLog(Client.class);

	protected String url;
	protected String host;
	protected int port;
	protected IoConnector connector;
	protected IoSession session;

	public static Client connect(String server, IoProtocol handler) throws IOException {

		Url u = Url.create(server);
		if (u == null) {
			throw new IOException("bad url, server=" + server);
		}

		Client c = new Client();

		if (u.isProtocol("tcp")) {

			c.connector = new NioSocketConnector();

			try {
				c.connector.setHandler(handler);
				c.connector.setConnectTimeoutMillis(10000);

				ConnectFuture connFuture = c.connector.connect(new InetSocketAddress(u.getIp(), u.getPort(9091)));
				connFuture.awaitUninterruptibly();

				if (connFuture.isDone()) {
					if (!connFuture.isConnected()) {
						throw new IOException("fail to connect url=" + u);
					}
				}
				c.session = connFuture.getSession();

				return c;

			} catch (IOException e) {
				c.connector.dispose(true);
				c.connector = null;
				log.error(server, e);
				throw e;
			}

		} else if (u.isProtocol("ssl")) {

			c.connector = new NioSocketConnector();

			try {

				c.connector.setHandler(handler);
				c.connector.setConnectTimeoutMillis(10000);

				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, null, null);

				SslFilter sslFilter = new SslFilter(sslContext);
				sslFilter.setUseClientMode(true);

				DefaultIoFilterChainBuilder chain = c.connector.getFilterChain();
				chain.addFirst("sslFilter", sslFilter);

				ConnectFuture connFuture = c.connector.connect(new InetSocketAddress(u.getIp(), u.getPort(9091)));
				connFuture.awaitUninterruptibly();

				if (connFuture.isDone()) {
					if (!connFuture.isConnected()) {
						throw new IOException("fail to connect url=" + u);
					}
				}
				c.session = connFuture.getSession();

				return c;

			} catch (Exception e) {
				c.connector.dispose(true);
				c.connector = null;
				log.error(server, e);
				throw new IOException(e);
			}

		} else if (u.isProtocol("udp")) {

			c.connector = new NioDatagramConnector();

			try {
				c.connector.setHandler(handler);

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
			throw new IOException("unknown protocol, url=" + u);
		}
	}

	public void close() {
		if (session != null) {
			session.closeNow();
			session = null;
		}
		if (connector != null) {
			connector.dispose();
			connector = null;
		}
	}

	public void write(IoBuffer b) {
		session.write(b);
	}

	// public static void main(String[] args) {
	//
	// Task.init(10);
	// try {
	// Client c = Client.connect("ssl://127.0.0.1:9092", new IRequestHandler() {
	//
	// @Override
	// public void process(Request r, IResponseHandler handler) {
	// // TODO Auto-generated method stub
	// System.out.println(r.seq + "=" + r.readString());
	// }
	//
	// @Override
	// public void closed(String name) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// });
	//
	// Response r = Response.create(0);
	// r.writeString("aaa");
	// c.send(r);
	//
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }

}