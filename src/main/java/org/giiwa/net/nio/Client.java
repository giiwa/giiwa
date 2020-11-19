package org.giiwa.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.misc.Url;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

public class Client implements Closeable {

	private static Log log = LogFactory.getLog(Client.class);

	protected String url;
	protected String host;
	protected int port;
	protected Bootstrap connector;
	protected Channel session;
	private Consumer<Throwable> error;

	public static Client create() {
		Client c = new Client();

		c.connector = new Bootstrap();
		return c;
	}

	public Client error(Consumer<Throwable> handler) {
		this.error = handler;
		return this;
	}

	public <V> Client option(ChannelOption<V> option, V value) {
		connector.option(option, value);
		return this;
	}

	public Client connect(String server, Consumer<IoRequest> handler) throws IOException {

		try {

			Url u = Url.create(server);
			if (u == null) {
				throw new IOException("bad url, server=" + server);
			}

			connector.group(new NioEventLoopGroup());
			connector.channel(NioSocketChannel.class);
//			connector.option(ChannelOption.TCP_NODELAY, true);

			IoHandler io = new IoHandler() {

				@Override
				public void channelInactive(ChannelHandlerContext ctx) throws Exception {

					log.info("idle, server=" + server);

//					if (error != null) {
//						error.accept(new Exception("inactive"));
//					}
//					if (session != null) {
//						session.close();
//						session = null;
//					}

				}

				@Override
				public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
					if (error != null) {
						error.accept(cause);
					}

					if (session != null) {
						session.close();
						session = null;
					}

					super.exceptionCaught(ctx, cause);
				}

				@Override
				public void process(IoRequest req, IoResponse resp) {
					handler.accept(req);
				}

			};

			connector.handler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(io);
				}
			});
			connector.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000);

			ChannelFuture connFuture = connector.connect(new InetSocketAddress(u.getIp(), u.getPort(9091))).sync();
//				connFuture.awaitUninterruptibly();

			if (connFuture.isDone()) {
				if (!connFuture.isSuccess()) {
					throw new IOException("fail to connect url=" + u);
				}
			}
			session = connFuture.channel();

		} catch (Exception e) {
			log.error(server, e);
			throw new IOException(e);
		}

		return this;
	}

	public void close() {
		if (session != null) {
			session.close();
			session = null;
		}

		if (connector != null) {
			connector.config().group().shutdownGracefully();
			connector = null;
		}
	}

//	public void write(IoBuffer b) {
//		session.write(b);
//	}

	public IoResponse packet() {
		return IoResponse.create(session);
	}

	public boolean isClosed() {
		return session == null;
	}

}