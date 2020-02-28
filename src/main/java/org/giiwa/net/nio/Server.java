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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

public class Server implements Closeable {

	private static Log log = LogFactory.getLog(Server.class);

	private ServerBootstrap boot = new ServerBootstrap();
	private BiConsumer<Channel, Throwable> error;

	public static Server create() {
		Server s = new Server();
		s.option(ChannelOption.SO_REUSEADDR, true).option(ChannelOption.SO_BACKLOG, 128);
		return s;
	}

	public <V> Server option(ChannelOption<V> name, V value) {
		boot.option(name, value);
		return this;
	}

	public Server error(BiConsumer<Channel, Throwable> handler) {
		this.error = handler;
		return this;
	}

	public Server bind(String url) throws Exception {
		Url u = Url.create(url);
		return bind(u.getIp(), u.getPort(9091));
	}

	public Server bind(String address, int port) throws Exception {
		if (boot.config().group() == null) {
			group(1, 2);
		}

		boot.bind(address, port).sync();
		return this;
	}

	public Server bind(int port) throws Exception {
		return bind("0.0.0.0", port);
	}

	public Server group(int parent, int child) {

		if (Epoll.isAvailable()) {

			EpollEventLoopGroup parentGroup = new EpollEventLoopGroup(parent);
			EpollEventLoopGroup childGroup = new EpollEventLoopGroup(child);

			boot.channel(EpollServerSocketChannel.class);
			boot.option(EpollChannelOption.SO_REUSEPORT, true);
			boot.group(parentGroup, childGroup);

		} else {

			EventLoopGroup parentGroup = new NioEventLoopGroup(parent);
			EventLoopGroup childGroup = new NioEventLoopGroup(child);
			boot.channel(NioServerSocketChannel.class);
			boot.group(parentGroup, childGroup);

		}

		return this;
	}

	public Server handler(BiConsumer<IoRequest, IoResponse> handler) {

		try {

			IoHandler io = new IoHandler() {

				@Override
				public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
					if (error != null) {
						error.accept(ctx.channel(), cause);
					}

					super.exceptionCaught(ctx, cause);

				}

				@Override
				public void process(IoRequest req, IoResponse resp) {
					handler.accept(req, resp);
				}

			};

			boot.childHandler(new ChannelInitializer<SocketChannel>() {
				@Override
				public void initChannel(SocketChannel ch) throws Exception {
					ch.pipeline().addLast(io);
				}
			});

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return this;
	}

	@Override
	public void close() throws IOException {
//		acceptor.dispose(true);
//		acceptor = null;
	}

	public static void main(String[] args) {

		try {

			Task.init(100);

			Server.create().group(2, 8).handler((req, resp) -> {

				try {
//				System.out.println(req.readBytes(new byte[128]));
					String s = "HTTP/1.1 200\n" + "Access-Control-Allow-Origin: no\n"
							+ "Content-Type: text/html;charset=UTF-8\n" + "Vary: Accept-Encoding\n"
							+ "Date: Mon, 24 Feb 2020 21:22:24 GMT\n" + "\n" + "hello world!\n";

					resp.write(s.getBytes());
					resp.send();

				} finally {
					resp.release();
					resp.close();
				}

			}).bind("127.0.0.1", 9092);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}