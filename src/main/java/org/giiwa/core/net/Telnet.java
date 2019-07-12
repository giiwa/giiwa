package org.giiwa.core.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.telnet.TelnetClient;
import org.giiwa.core.base.Url;

public class Telnet {

	private static Log log = LogFactory.getLog(Telnet.class);

	private static final long timeout = 30000;// 默认超时为30秒

	/**
	 * 
	 * @param url     telnet://g01:22?username=&passwd=
	 * 
	 * @param command the command
	 * @return the String
	 */
	public static String run(Url url, String command) {

		TelnetClient t = new TelnetClient();
		InputStream in = null;
		PrintStream out = null;

		try {

			t.connect(url.getIp(), url.getPort(23));
			in = t.getInputStream();
			out = new PrintStream(t.getOutputStream());

			if (!login(url.get("username"), url.get("passwd"), out, in)) {
				return null;
			}

			String s = run(command, out, in);

			// System.out.println(s);

			log.debug("ssh.run, url=" + url + ", command=" + command + ", result=" + s);

			return s;
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {
			// X.close(in, out);
			try {
				t.disconnect();
			} catch (IOException e) {
				log.error(url, e);
			}
		}

		return null;
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

	private static String run(String cmd, PrintStream out, InputStream in) {

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

	public static void main(String[] args) {
		String s = Telnet.run(Url.create("telnet://172.20.10.5:23?username=joe&passwd=123123"), "ls -al /");
		System.out.println(s);
	}

}
