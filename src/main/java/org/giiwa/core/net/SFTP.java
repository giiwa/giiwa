package org.giiwa.core.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.Url;
import org.giiwa.core.bean.X;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class SFTP {

	private static Log log = LogFactory.getLog(SFTP.class);

	/**
	 * 
	 * @param url      sftp://g01:22?username=,passwd=
	 * 
	 * @param filename
	 * @param in
	 */
	public static void put(Url url, String filename, InputStream in) {

		log.debug("sftp put, filename=" + filename);

		Session session = null;
		Channel channel = null;
		try {
			session = getSession(url);

			channel = session.openChannel("sftp");
			channel.connect();

			_put((ChannelSftp) channel, filename, in);

		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {

			if (channel != null) {
				channel.disconnect();
			}

			if (session != null) {
				session.disconnect();
			}
		}

	}

	/**
	 * 
	 * @param url
	 * @param src
	 * @param dest
	 */
	public static void download(Url url, String src, String dest) {
		Session session = null;
		Channel channel = null;
		try {
			session = getSession(url);

			channel = session.openChannel("sftp");
			channel.connect();

			_get((ChannelSftp) channel, src, dest);

		} catch (Exception e) {
			log.error(e.getMessage(), e);

		} finally {

			if (channel != null) {
				channel.disconnect();
			}

			if (session != null) {
				session.disconnect();
			}
		}

	}

	private static void _get(ChannelSftp ch, String src, String dest) throws Exception {
		OutputStream out = null;
		try {
			out = new FileOutputStream(dest);
			ch.get(src, out);
		} finally {
			X.close(out);
		}

	}

	private static void _put(ChannelSftp ch, String filename, InputStream in) throws SftpException {
		try {
			File f = new File(filename);
			String path = f.getParent();
			log.debug("cd " + path);

			ch.cd(path);
			ch.put(in, f.getName());
		} finally {
			X.close(in);
		}
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

}
