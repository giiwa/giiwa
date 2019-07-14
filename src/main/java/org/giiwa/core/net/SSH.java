package org.giiwa.core.net;

import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Url;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class SSH {

	private static Log log = LogFactory.getLog(SSH.class);

	/**
	 * 
	 * @param url     ssh://g01:22?username=,passwd=
	 * 
	 * @param command the command
	 * @return the String
	 */
	public static String run(Url url, String command) {
		Session session = null;
		try {
			session = getSession(url);

			String s = run(session, command);

			log.debug("ssh.run, url=" + url + ", command=" + command + ", result=" + s);

			return s;
		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {

			if (session != null) {
				session.disconnect();
			}
		}

		return null;
	}

	private static Session getSession(Url url) throws JSchException {

		JSch jsch = new JSch();

		Session session = jsch.getSession(url.get("username"), url.getIp(), url.getPort(22));
		session.setPassword(url.get("passwd"));

		UserInfo ui = new MyUserInfo() {
			public void showMessage(String message) {
				log.debug("showMessage:" + message);
			}

			public boolean promptYesNo(String message) {
				log.debug("promptYesNo:" + message);
				return true;
			}

		};

		session.setUserInfo(ui);
		// session.connect();
		session.connect(30 * 1000);
		return session;
	}

	private static abstract class MyUserInfo implements UserInfo, UIKeyboardInteractive {
		public String getPassword() {
			log.debug("getPassword");
			return null;
		}

		public boolean promptYesNo(String str) {
			log.debug("promptYesNo:" + str);
			return false;
		}

		public String getPassphrase() {
			log.debug("getPassphrase");
			return null;
		}

		public boolean promptPassphrase(String message) {
			log.debug("promptPassphrase:" + message);
			return false;
		}

		public boolean promptPassword(String message) {
			log.debug("promptPassword:" + message);
			return false;
		}

		public void showMessage(String message) {
			log.debug("showMessage:" + message);
		}

		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
				boolean[] echo) {
			log.debug("promptKeyboardInteractive:" + name);
			return null;
		}
	}

	private static String run(Session session, String cmd) {

		Channel channel = null;
		try {
			channel = session.openChannel("exec");

			channel.setInputStream(null);

			((ChannelExec) channel).setCommand(cmd);

			InputStream in = channel.getInputStream();
			InputStream ext = channel.getExtInputStream();
			channel.connect(3 * 1000);

			StringBuilder sb = new StringBuilder();
			byte[] tmp = new byte[1024];
			while (true) {
				while (in.available() > 0) {
					int i = in.read(tmp, 0, 1024);
					if (i < 0)
						break;
					sb.append(new String(tmp, 0, i));
				}

				while (ext.available() > 0) {
					int i = ext.read(tmp, 0, 1024);
					if (i < 0)
						break;
					sb.append(new String(tmp, 0, i));
				}

				if (channel.isClosed()) {
					if (in.available() > 0)
						continue;
					break;
				}
				try {
					Thread.sleep(1000);
				} catch (Exception ee) {
				}
			}
			return sb.toString();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			e.printStackTrace();
		} finally {
			if (channel != null) {
				channel.disconnect();
			}
		}
		return null;
	}

}
