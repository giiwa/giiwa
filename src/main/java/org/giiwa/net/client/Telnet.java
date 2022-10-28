package org.giiwa.net.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.telnet.TelnetClient;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;

public class Telnet implements Closeable {

	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(Telnet.class);

	private static final long timeout = 30000;// 默认超时为30秒

	private TelnetClient client = null;
	private InputStream in = null;
	private PrintStream out = null;

	private Telnet() {

	}

	public static Telnet create(Url url) throws SocketException, IOException {
		Telnet s = new Telnet();
		s.open(url);
		return s;
	}

	public static Telnet create() {
		return new Telnet();
	}

	public Telnet open(String url) throws SocketException, IOException {
		return open(Url.create(url));
	}

	public Telnet open(Url url) throws SocketException, IOException {

		close();

		try {

			String username = url.get("username");
			if (X.isEmpty(username)) {
				throw new IOException("[username] required");
			}

			String passwd = url.get("passwd");
			if (X.isEmpty(passwd)) {
				throw new IOException("[passwd] required");
			}

			client = new TelnetClient();
			client.connect(url.getIp(), url.getPort(23));
			in = client.getInputStream();
			out = new PrintStream(client.getOutputStream());

			if (!login(username, passwd, out, in)) {
				return null;
			}

		} finally {
			close();
		}

		return this;
	}

	private static boolean login(String username, String passwd, PrintStream out, InputStream in) {

		try {
			// out.write((info + "/n").getBytes());
			// out.flush();
			int i = -1;
			StringBuilder sb = new StringBuilder();
			long startTime = System.currentTimeMillis();
			while (System.currentTimeMillis() - startTime < timeout) {
				while ((i = in.read()) > -1) {
					char ch = (char) i;
					if (ch == '\n' || ch == '\r') {
						sb.delete(0, sb.length());
						continue;
					}
					sb.append((char) ch);

					String s1 = sb.toString();
					if (s1.matches(".*\\]\\$") || s1.matches("^Last login:")) {
						return true;
					}
					if (s1.matches(".*login:$")) {
						out.println(username);
						out.flush();
					} else if (s1.matches(".*Password:$")) {
						out.println(passwd);
						out.flush();
					}
				}
			}
			throw new IllegalArgumentException("超时收不到提示符");
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public String run(String cmd) {

		StringBuilder text = null;
		try {
			while (in.available() > 0)
				in.read();

			out.println(cmd);
			out.flush();
			StringBuilder sb = new StringBuilder();
			long startTime = System.currentTimeMillis();
			int i = -1;

			while (System.currentTimeMillis() - startTime < timeout) {
				while ((i = in.read()) > -1) {
					char ch = (char) i;
					if (text != null)
						text.append(ch);
					if (ch == '\n' || ch == '\r') {
						sb.delete(0, sb.length());
						continue;
					}
					sb.append(ch);
					if (sb.toString().matches(".*\\]\\$$")) {
						if (text != null) {
							return text.toString();
						} else {
							text = new StringBuilder();
						}
					}
				}
			}
			throw new IllegalArgumentException("超时收不到提示符");
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public void close() throws IOException {
		X.close(in, out);
		in = null;
		out = null;

		if (client != null) {
			client.disconnect();
		}
	}

}
