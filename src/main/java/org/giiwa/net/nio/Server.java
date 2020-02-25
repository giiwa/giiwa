package org.giiwa.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.BiConsumer;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.giiwa.misc.Url;
import org.giiwa.task.Task;

public class Server implements Closeable {

	private static Log log = LogFactory.getLog(Server.class);

	private AbstractIoAcceptor acceptor;

	public static Server create() {
		return new Server();
	}

	public Server bind(String url, BiConsumer<IoRequest, IoResponse> handler) {

		Url u = Url.create(url);

		try {

			if (u.isProtocol("tcp")) {
				acceptor = new NioSocketAcceptor();
				((NioSocketAcceptor) acceptor).setReuseAddress(true);
				
				acceptor.setHandler(new IoHandler() {

					@Override
					public void process(IoRequest req, IoResponse resp) {
						handler.accept(req, resp);
					}

				});
			} else if (u.isProtocol("udp")) {

				acceptor = new NioDatagramAcceptor();
				acceptor.setHandler(new IoHandler() {

					@Override
					public void process(IoRequest req, IoResponse resp) {
						handler.accept(req, resp);
					}

				});

			} else if (u.isProtocol("ssl")) {

				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, null, null);

				SslFilter sslFilter = new SslFilter(sslContext);
				sslFilter.setUseClientMode(false);

				acceptor = new NioSocketAcceptor();
				((NioSocketAcceptor) acceptor).setReuseAddress(true);

				DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
				chain.addFirst("sslFilter", sslFilter);

				acceptor.setHandler(new IoHandler() {

					@Override
					public void process(IoRequest req, IoResponse resp) {
						handler.accept(req, resp);
					}

				});
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		if (acceptor == null)
			return this;

		try {
			acceptor.bind(new InetSocketAddress(u.getIp(), u.getPort(9091)));
		} catch (IOException e) {
			log.error(url.toString(), e);
			Task.schedule(() -> {
				bind(url, handler);
			}, 3000);
		}

		return this;
	}

	@Override
	public void close() throws IOException {
		acceptor.dispose(true);
		acceptor = null;
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