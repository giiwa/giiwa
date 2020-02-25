package org.giiwa.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.misc.Url;
import org.giiwa.task.Task;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server implements Closeable {

	private static Log log = LogFactory.getLog(Server.class);

	private ServerBootstrap server;
	private Channel cf;

	public static Server create() {
		Server s = new Server();
		s.server = new ServerBootstrap();

		s.server.option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.SO_BACKLOG, 1024)
				.option(ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true);

		if (epollIsAvailable()) {
			s.server.option(EpollChannelOption.SO_REUSEPORT, true);
			s.server.channel(EpollServerSocketChannel.class);
		} else {
			s.server.channel(NioServerSocketChannel.class);
		}

		return s;
	}

	private static boolean epollIsAvailable() {
		try {
			Object obj = Class.forName("io.netty.channel.epoll.Epoll").getMethod("isAvailable").invoke(null);
			return null != obj && Boolean.valueOf(obj.toString())
					&& System.getProperty("os.name").toLowerCase().contains("linux");
		} catch (Exception e) {
			return false;
		}
	}

	public Server group(int parent, int child) {
		EventLoopGroup bossGroup = new NioEventLoopGroup(parent);
		EventLoopGroup workerGroup = new NioEventLoopGroup(child);
		server.group(bossGroup, workerGroup);
		return this;
	}

	public <T> Server option(ChannelOption<T> option, T value) {
		server.option(option, value);
		return this;
	}

	public Server bind(String url, BiConsumer<IoRequest, IoResponse> handler) throws IOException {

		if (server.config().group() == null) {
			this.group(1, 2);
		}

		IoHandler h1 = new IoHandler() {

			@Override
			public void process(IoRequest req, IoResponse resp) {
				handler.accept(req, resp);
			}
		};

		server.childHandler(h1);

		Url u = Url.create(url);

		try {

			cf = server.bind(u.getIp(), u.getPort(9091)).sync().channel();

			System.out.println("started");

			if (log.isInfoEnabled())
				log.info("nio server bind on [" + url + "]");

		} catch (Exception e) {
			throw new IOException(e);
		}

		return this;

	}

	@Override
	public void close() throws IOException {
		try {
			cf.closeFuture().sync();
			server.config().group().shutdownGracefully();
			server.config().childGroup().shutdownGracefully();
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public static void main(String[] args) {

		Task.init(10);
		try {

			Server.create().bind("tcp://127.0.0.1:9092", (req, resp) -> {

				String s = "HTTP/1.1 200\n" + "Access-Control-Allow-Origin: no\n"
						+ "Content-Type: text/html;charset=UTF-8\n" + "Vary: Accept-Encoding\n"
						+ "Date: Mon, 24 Feb 2020 21:22:24 GMT\n" + "\n" + "hello world!\n";

				resp.write(s.getBytes());
				resp.flush();
				resp.close();

			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
