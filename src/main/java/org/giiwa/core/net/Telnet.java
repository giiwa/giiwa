package org.giiwa.core.net;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.telnet.TelnetClient;
import org.giiwa.core.base.Url;
import org.giiwa.core.bean.X;

public class Telnet implements Closeable {

	private static Log log = LogFactory.getLog(Telnet.class);

	private static final long timeout = 30000;// 默认超时为30秒

	private TelnetClient client = null;
	private InputStream in = null;
	private PrintStream out = null;

	private Telnet() {

	}

	public static Telnet create(Url url) throws SocketException, IOException {

		Telnet s = new Telnet();
		try {
			s.client = new TelnetClient();
			s.client.connect(url.getIp(), url.getPort(23));
			s.in = s.client.getInputStream();
			s.out = new PrintStream(s.client.getOutputStream());

			if (!login(url.get("username"), url.get("passwd"), s.out, s.in)) {
				return null;
			}
		} finally {
			s.close();
		}

		return s;
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
					// System.out.println(sb.toString());

					String s1 = sb.toString();
					if (s1.matches(".*\\]\\$") || s1.matches("^Last login:")) {
						// System.out.println("logined");
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
					// System.out.println(sb.toString());
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
