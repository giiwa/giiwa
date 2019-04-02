package org.giiwa.core.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.AbstractIoAcceptor;
import org.apache.mina.filter.ssl.SslFilter;
import org.apache.mina.transport.socket.nio.NioDatagramAcceptor;
import org.apache.mina.transport.socket.nio.NioSocketAcceptor;
import org.giiwa.core.base.Url;
import org.giiwa.core.task.Task;

public class Server implements Closeable {

	private static Log log = LogFactory.getLog(Server.class);

	private AbstractIoAcceptor acceptor;
	private Url url;

	public Server(Url url, IoProtocol handler) throws IOException {
		try {
			this.url = url;
			if (url.isProtocol("tcp")) {
				acceptor = new NioSocketAcceptor();
				((NioSocketAcceptor) acceptor).setReuseAddress(true);

				acceptor.setHandler(handler);
			} else if (url.isProtocol("udp")) {

				acceptor = new NioDatagramAcceptor();
				acceptor.setHandler(handler);

			} else if (url.isProtocol("ssl")) {

				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, null, null);

				SslFilter sslFilter = new SslFilter(sslContext);
				sslFilter.setUseClientMode(false);

				acceptor = new NioSocketAcceptor();
				((NioSocketAcceptor) acceptor).setReuseAddress(true);

				DefaultIoFilterChainBuilder chain = acceptor.getFilterChain();
				chain.addFirst("sslFilter", sslFilter);

				acceptor.setHandler(handler);
			}
		} catch (Exception e) {
			throw new IOException(e);
		}

	}

	public static Server bind(String url, IoProtocol handler) throws IOException {
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

	// public static void main(String[] args) {
	//
	// Task.init(10);
	// try {
	// Server.bind("ssl://127.0.0.1:9092", new IRequestHandler() {
	//
	// @Override
	// public void process(Request r, IResponseHandler handler) {
	// // TODO Auto-generated method stub
	// System.out.println(r.seq + "=" + r.readString());
	// Response r1 = Response.create(r.seq);
	// r1.writeString("ok");
	// handler.send(r1);
	// }
	//
	// @Override
	// public void closed(String name) {
	// // TODO Auto-generated method stub
	//
	// }
	//
	// });
	// } catch (Exception e) {
	// e.printStackTrace();
	// }
	// }

}
