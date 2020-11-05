package org.giiwa.net.client;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.giiwa.bean.Temp;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;

public class FTP {

	private static Log log = LogFactory.getLog(FTP.class);

	private FTPClient client;

	public FTPClient getClient() {
		return client;
	}

	public void close() {
		if (client != null) {
			try {
				client.disconnect();
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
			client = null;
		}
	}

	public boolean put(String remote, InputStream in) throws IOException {
		return client.appendFile(remote, in);
	}

	public Temp get(String remote) throws IOException {
		return get(new File(remote));
	}

	public Temp get(File remote) throws IOException {
		Temp t = Temp.create(remote.getName());
		OutputStream out = t.getOutputStream();
		client.retrieveFile(remote.getAbsolutePath(), out);
		return t;
	}

	public File[] list(String path) throws IOException {

		List<File> l1 = new ArrayList<File>();

		FTPFile[] ff = client.listFiles(path);

		_toFile(path, l1, ff);

		ff = client.listDirectories(path);
		_toFile(path, l1, ff);

		return l1.toArray(new File[l1.size()]);
	}

	private void _toFile(String path, List<File> l1, FTPFile[] ff) {

		while (path.endsWith("/") && path.length() > 1) {
			path = path.substring(0, path.length() - 1);
		}

		if (ff != null) {
			for (FTPFile f : ff) {
				if (X.isIn(f.getName(), ".", ".."))
					continue;

				File f1 = new File(path + "/" + f.getName()) {

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
						return f.isDirectory();
					}

					@Override
					public boolean isFile() {
						return f.isFile();
					}

					@Override
					public long length() {
						return f.getSize();
					}

					@Override
					public boolean delete() {
						try {
							return client.deleteFile(this.getAbsolutePath());
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
							return FTP.this.list(this.getAbsolutePath());
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
						return null;
					}

					@Override
					public boolean renameTo(File dest) {
						try {
							return client.rename(this.getAbsolutePath(), dest.getAbsolutePath());
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

	/**
	 * 
	 * @param url ftp://g01:21?username=，passwd=
	 * 
	 * @param url the command
	 * @return the FTPClient
	 */
	public static FTP create(Url url) {

		FTPClient ftp = new FTPClient();
		FTPClientConfig config = new FTPClientConfig();
		// config.setXXX(YYY); // change required options
		// for example config.setServerTimeZoneId("Pacific/Pitcairn")
		ftp.configure(config);
		try {
			int reply;

			ftp.connect(url.getIp(), url.getPort(21));
			if (log.isDebugEnabled())
				log.debug("replaystring=" + ftp.getReplyString());

			// After connection attempt, you should check the reply code to verify
			// success.
			reply = ftp.getReplyCode();

			if (!FTPReply.isPositiveCompletion(reply)) {
				ftp.disconnect();
				return null;
			}

			if (ftp.login(url.get("username"), url.get("passwd"))) {
				FTP f = new FTP();
				f.client = ftp;
				return f;
			}
		} catch (IOException e) {
			log.error(url, e);
		}
		return null;
	}

}
