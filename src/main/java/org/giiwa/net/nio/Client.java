package org.giiwa.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.misc.Url;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client implements Closeable {

	private static Log log = LogFactory.getLog(Client.class);

	protected String url;
	protected String host;
	protected int port;
//	protected IoConnector connector;
//	protected IoSession session;

	private Bootstrap client;
	private Channel ch;

	public static Client connect(String server, Consumer<IoRequest> handler) throws IOException {

		Url u = Url.create(server);
		if (u == null) {
			throw new IOException("bad url, server=" + server);
		}

		Client c = new Client();

		c.client = new Bootstrap();

		EventLoopGroup group = new NioEventLoopGroup();

		try {

			c.client.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true)
					.handler(new IoHandler() {

						@Override
						public void process(IoRequest req, IoResponse resp) {
							handler.accept(req);
						}

					});

			// Start the client.
			c.ch = c.client.connect(u.getIp(), u.getPort(8092)).sync().channel();

//			c.connector.setHandler(handler);
//			c.connector.setConnectTimeoutMillis(10000);
//
//			ConnectFuture connFuture = c.connector.connect(new InetSocketAddress(u.getIp(), u.getPort(9091)));
//			connFuture.awaitUninterruptibly();
//
//			if (connFuture.isDone()) {
//				if (!connFuture.isConnected()) {
//					throw new IOException("fail to connect url=" + u);
//				}
//			}
//			c.session = connFuture.getSession();
//
			return c;

		} catch (Exception e) {
//			c.connector.dispose(true);
//			c.connector = null;
//			log.error(server, e);
			throw new IOException(e);
		}

	}

	public void close() {
		if (ch != null) {
			ch.close();
			ch = null;
		}
		if (client != null) {
			client.config().group().shutdownGracefully();
			client = null;
		}

//		if (session != null) {
//			session.closeNow();
//			session = null;
//		}
//		if (connector != null) {
//			connector.dispose();
//			connector = null;
//		}
	}

	public void write(ByteBuf b) {
		ch.write(b);
		ch.flush();
	}

	public IoResponse create() {
		return IoResponse.create(ch);
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