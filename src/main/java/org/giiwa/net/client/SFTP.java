package org.giiwa.net.client;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

public class SFTP implements Closeable {

	private static Log log = LogFactory.getLog(SFTP.class);

	private Session session = null;
	private ChannelSftp sftp = null;

	public void close() {
		if (sftp != null) {
			sftp.disconnect();
			sftp = null;
		}

		if (session != null) {
			session.disconnect();
			session = null;
		}

	}

	private SFTP() {

	}

	public static SFTP create(Url url) throws JSchException {
		SFTP s = new SFTP();
		s.session = getSession(url);
		s.sftp = (ChannelSftp) s.session.openChannel("sftp");
		s.sftp.connect();
		return s;
	}

	/**
	 * 
	 * @param url      sftp://g01:22?username=,passwd=
	 * 
	 * @param filename
	 * @param in
	 * @throws SftpException
	 */
	public void put(String filename, InputStream in) throws SftpException {

		log.debug("sftp put, filename=" + filename);

		File f = new File(filename);
		String path = f.getParent();
		log.debug("cd " + path);

		sftp.cd(path);
		sftp.put(in, f.getName());

	}

	/**
	 * 
	 * @param url
	 * @param src
	 * @param dest
	 * @throws SftpException
	 * @throws FileNotFoundException
	 */
	public void get(String src, String dest) throws SftpException, FileNotFoundException {
		OutputStream out = null;
		try {
			new File(dest).getParentFile().mkdirs();
			out = new FileOutputStream(dest);
			sftp.get(src, out);
		} finally {
			X.close(out);
		}

	}

	@SuppressWarnings("unchecked")
	public Vector<LsEntry> list(String src) throws SftpException {
		return sftp.ls(src);
	}

	public void rm(String src) throws SftpException {
		sftp.rm(src);
	}

	public void rmdir(String src) throws SftpException {
		sftp.rmdir(src);
	}

	private static Session getSession(Url url) throws JSchException {

		JSch jsch = new JSch();

		Session session = jsch.getSession(url.get("username"), url.getIp(), url.getPort(22));
		session.setPassword(url.get("passwd"));

		UserInfo ui = new MyUserInfo() {

			@Override
			public boolean promptPassphrase(String message) {
				if (log.isDebugEnabled())
					log.debug("promptPassphrase:" + message);
				return true;
			}

			@Override
			public void showMessage(String message) {
				if (log.isDebugEnabled())
					log.debug("showMessage:" + message);
			}

			@Override
			public boolean promptYesNo(String message) {
				if (log.isDebugEnabled())
					log.debug("promptYesNo:" + message);
				return true;
			}

		};

		session.setUserInfo(ui);
		// session.connect();
		session.connect(30 * 1000);

		if (session.isConnected()) {
			return session;
		} else {
			throw new JSchException("connect failed!");
		}
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
