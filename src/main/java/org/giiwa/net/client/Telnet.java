/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.net.client;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.telnet.TelnetClient;
import org.giiwa.conf.Global;
import org.giiwa.dao.Comment;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;

@Comment(text = "Telnet工具")
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
		s.open(url, url.get("username"), url.get("passwd"));
		return s;
	}

	public static Telnet create() {
		return new Telnet();
	}

	@Comment(text = "打开远程链接, url=telnet://ip:port?username=&passwd=")
	public Telnet open(@Comment(text = "url") String url) throws SocketException, IOException {
		Url u = Url.create(url);
		return open(u, u.get("username"), u.get("passwd"));
	}

	@Comment(text = "打开远程链接, url=telnet://ip:port", demo = "..open('telnet://ip:port', username, passwd)")
	public Telnet open(@Comment(text = "url") String url, String username, String passwd)
			throws SocketException, IOException {
		return open(Url.create(url), username, passwd);
	}

	public Telnet open(@Comment(text = "url") Url url, String username, String passwd)
			throws SocketException, IOException {

		close();

		try {

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
			long startTime = Global.now();
			while (Global.now() - startTime < timeout) {
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

	@Comment(text = "执行命令")
	public String run(@Comment(text = "command") String cmd) {

		StringBuilder text = null;
		try {
			while (in.available() > 0)
				in.read();

			out.println(cmd);
			out.flush();
			StringBuilder sb = new StringBuilder();
			long startTime = Global.now();
			int i = -1;

			while (Global.now() - startTime < timeout) {
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
	@Comment(hide = true)
	public void close() throws IOException {
		X.close(in, out);
		in = null;
		out = null;

		if (client != null) {
			client.disconnect();
		}
	}

}
