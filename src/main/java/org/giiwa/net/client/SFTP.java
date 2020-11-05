package org.giiwa.net.client;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Temp;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
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

	/**
	 * 
	 * @param url, sftp://g01:22?username=,passwd=
	 * @return
	 * @throws JSchException
	 */
	public static SFTP create(Url url) throws IOException {
		try {
			SFTP s = new SFTP();
			s.session = getSession(url);
			s.sftp = (ChannelSftp) s.session.openChannel("sftp");
			s.sftp.connect();
			return s;
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public boolean put(String filename, InputStream in) throws IOException {
		return put(new File(filename), in);
	}

	/**
	 * 
	 * @param url      sftp://g01:22?username=,passwd=
	 * 
	 * @param filename
	 * @param in
	 * @throws IOException
	 */
	public boolean put(File f, InputStream in) throws IOException {

		try {
			log.debug("sftp put, filename=" + f.getAbsolutePath());

			String path = f.getParent();
			log.debug("cd " + path);

			sftp.cd(path);
			sftp.put(in, f.getName());
			return true;
		} catch (Exception e) {
			throw new IOException(e);
		}

	}

	public Temp get(String filename) throws IOException {
		File f = new File(filename);
		Temp t = Temp.create(f.getName());
		get(f, t.getFile().getAbsolutePath());
		return t;
	}

	public void get(String filename, String dest) throws IOException {
		OutputStream out = null;
		try {
			new File(dest).getParentFile().mkdirs();
			out = new FileOutputStream(dest);
			sftp.get(filename, out);
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			X.close(out);
		}
	}

	public void get(File filename, String dest) throws IOException {
		get(filename.getAbsolutePath(), dest);
	}

	@SuppressWarnings("unchecked")
	public File[] list(String src) throws IOException {

		try {
			List<File> l1 = new ArrayList<File>();
			Vector<LsEntry> l2 = sftp.ls(src);
			_toFile(src, l1, l2);
			return l1.toArray(new File[l1.size()]);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private void _toFile(String path, List<File> l1, Vector<LsEntry> ff) {

		while (path.endsWith("/") && path.length() > 1) {
			path = path.substring(0, path.length() - 1);
		}

		if (ff != null) {
			for (LsEntry f : ff) {
				File f1 = new File(path + "/" + f.getFilename()) {

					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public boolean exists() {
						return true;
					}

					@Override
					public boolean isDirectory() {
						SftpATTRS att = f.getAttrs();
						return att.isDir();
					}

					@Override
					public boolean isFile() {
						SftpATTRS att = f.getAttrs();
						return !att.isDir();
					}

					@Override
					public long length() {
						SftpATTRS att = f.getAttrs();
						return att.getSize();
					}

					@Override
					public boolean delete() {
						try {
							rm(this.getAbsolutePath());
							return true;
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
						return false;
					}

					@Override
					public String[] list() {
						return null;
					}

					@Override
					public File[] listFiles() {
						try {
							return SFTP.this.list(this.getAbsolutePath());
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
						return null;
					}

					@Override
					public boolean renameTo(File dest) {
						try {
							sftp.rename(this.getAbsolutePath(), dest.getAbsolutePath());
							return true;
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
						return false;
					}

				};
				l1.add(f1);
			}
		}
	}

	public void rm(String src) throws IOException {
		try {
			sftp.rm(src);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	public void rmdir(String src) throws IOException {
		try {
			sftp.rmdir(src);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private static Session getSession(Url url) throws IOException {

		JSch jsch = new JSch();

		try {
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
			}
		} catch (Exception e) {
			throw new IOException(e);
		}

		throw new IOException("connect failed!");
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
