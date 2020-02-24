package org.giiwa.net.client;

import java.io.Closeable;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.misc.Url;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class SSH implements Closeable {

	private static Log log = LogFactory.getLog(SSH.class);

	private Session session = null;
	private ChannelExec exec = null;

	public void close() {

		if (exec != null) {
			exec.disconnect();
			exec = null;
		}

		if (session != null) {
			session.disconnect();
			session = null;
		}

	}

	private SSH() {

	}

	public static SSH create(Url url) throws JSchException {
		SSH s = new SSH();
		s.session = getSession(url);
		return s;
	}

	private static Session getSession(Url url) throws JSchException {

		JSch jsch = new JSch();

		Session session = jsch.getSession(url.get("username"), url.getIp(), url.getPort(22));
		session.setPassword(url.get("passwd"));

		UserInfo ui = new MyUserInfo() {
			public void showMessage(String message) {
				if (log.isDebugEnabled())
					log.debug("showMessage:" + message);
			}

			public boolean promptYesNo(String message) {
				if (log.isDebugEnabled())
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
			if (log.isDebugEnabled())
				log.debug("getPassword");
			return null;
		}

		public boolean promptYesNo(String str) {
			if (log.isDebugEnabled())
				log.debug("promptYesNo:" + str);
			return false;
		}

		public String getPassphrase() {
			if (log.isDebugEnabled())
				log.debug("getPassphrase");
			return null;
		}

		public boolean promptPassphrase(String message) {
			if (log.isDebugEnabled())
				log.debug("promptPassphrase:" + message);
			return false;
		}

		public boolean promptPassword(String message) {
			if (log.isDebugEnabled())
				log.debug("promptPassword:" + message);
			return false;
		}

		public void showMessage(String message) {
			if (log.isDebugEnabled())
				log.debug("showMessage:" + message);
		}

		public String[] promptKeyboardInteractive(String destination, String name, String instruction, String[] prompt,
				boolean[] echo) {
			if (log.isDebugEnabled())
				log.debug("promptKeyboardInteractive:" + name);
			return null;
		}
	}

	public String run(String cmd) {

		try {

			exec = (ChannelExec) session.openChannel("exec");
			exec.connect();

			exec.setInputStream(null);

			exec.setCommand(cmd);

			InputStream in = exec.getInputStream();
			InputStream ext = exec.getExtInputStream();
			exec.connect(3 * 1000);

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

				if (exec.isClosed()) {
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
			if (exec != null) {
				exec.disconnect();
			}
		}
		return null;
	}

}
