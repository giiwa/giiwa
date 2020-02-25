package org.giiwa.net.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.giiwa.task.Task;

public class Server implements Closeable {

	private static Log log = LogFactory.getLog(Server.class);

	private AbstractIoAcceptor acceptor;
	private Map<Key<?>, Object> _option = new HashMap<Key<?>, Object>();

	public static class Key<V> {

		public static final Key<Integer> PROCESSOR = new Key<>();
		public static final Key<Integer> BACKLOG = new Key<>();
		public static final Key<Boolean> REUSEPORT = new Key<>();
		public static final Key<String> SSL = new Key<>();
		public static final Key<String> ADDRESS = new Key<>();
		public static final Key<Integer> PORT = new Key<>();
		public static final Key<String> PROTOCOL = new Key<>();

	}

	@SuppressWarnings({ "unchecked" })
	private <V> V _get(Key<V> name, V value) {
		V o = (V) _option.get(name);
		if (o == null) {
			return value;
		} else {
			return o;
		}
	}

	public static Server create() {
		return new Server();
	}

	public <V> Server option(Key<V> name, V value) {
		_option.put(name, value);
		return this;
	}

	public Server bind(String url) {
		Url u = Url.create(url);
		option(Key.PROTOCOL, u.getProtocol());
		option(Key.ADDRESS, u.getIp());
		option(Key.PORT, u.getPort(9091));
		return this;
	}

	public Server bind(String address, int port) {
		option(Key.ADDRESS, address);
		option(Key.PORT, port);
		return this;
	}

	public Server bind(int port) {
		option(Key.PORT, port);
		return this;
	}

	public Server start() {

		if (acceptor == null)
			return this;

		InetSocketAddress addr = new InetSocketAddress(_get(Key.ADDRESS, "0.0.0.0"), _get(Key.PORT, 9091));
		try {
			acceptor.bind(addr);
		} catch (IOException e) {
			log.error(addr.toString(), e);
			Task.schedule(() -> {
				start();
			}, 3000);
		}

		return this;
	}

	public Server handler(BiConsumer<IoRequest, IoResponse> handler) {

		try {

			String protocol = _get(Key.PROTOCOL, "tcp");

			if (X.isSame("tcp", protocol)) {
				acceptor = new NioSocketAcceptor(_get(Key.PROCESSOR, 10));
				((NioSocketAcceptor) acceptor).setReuseAddress(_get(Key.REUSEPORT, true));
				((NioSocketAcceptor) acceptor).setBacklog(_get(Key.BACKLOG, 128));

				acceptor.setHandler(new IoHandler() {

					@Override
					public void process(IoRequest req, IoResponse resp) {
						handler.accept(req, resp);
					}

				});
			} else if (X.isSame("udp", protocol)) {

				acceptor = new NioDatagramAcceptor();
				acceptor.setHandler(new IoHandler() {

					@Override
					public void process(IoRequest req, IoResponse resp) {
						handler.accept(req, resp);
					}

				});

			} else if (X.isSame("ssl", protocol)) {

				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, null, null);

				SslFilter sslFilter = new SslFilter(sslContext);
				sslFilter.setUseClientMode(false);

				acceptor = new NioSocketAcceptor(_get(Key.PROCESSOR, 10));
				((NioSocketAcceptor) acceptor).setReuseAddress(_get(Key.REUSEPORT, true));
				((NioSocketAcceptor) acceptor).setBacklog(_get(Key.BACKLOG, 128));

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

		return this;
	}

	@Override
	public void close() throws IOException {
		acceptor.dispose(true);
		acceptor = null;
	}

	public static void main(String[] args) {

		Task.init(10);

		Server.create().option(Key.PROCESSOR, 8).bind("127.0.0.1", 9092).handler((req, resp) -> {

			String s = "HTTP/1.1 200\n" + "Access-Control-Allow-Origin: no\n"
					+ "Content-Type: text/html;charset=UTF-8\n" + "Vary: Accept-Encoding\n"
					+ "Date: Mon, 24 Feb 2020 21:22:24 GMT\n" + "\n" + "hello world!\n";

			resp.write(s.getBytes());
			resp.flush();

			resp.close();

		}).start();

	}

}