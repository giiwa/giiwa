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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Temp;
import org.giiwa.dao.Comment;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;
import org.giiwa.task.Function;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.UIKeyboardInteractive;
import com.jcraft.jsch.UserInfo;

@Comment(text = "SFTP工具")
public class SFTP implements Closeable {

	private static Log log = LogFactory.getLog(SFTP.class);

	private Session session = null;
	private ChannelSftp sftp = null;

	/**
	 * 关闭链接
	 */
	@Comment(hide = true)
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
	 * 设置超时时间
	 * 
	 * @param timeout 毫秒
	 * @return
	 * @throws JSchException
	 */
	@Comment(text = "设置超时")
	public SFTP timeout(@Comment(text = "timeout") int timeout) throws JSchException {
		session.setTimeout(timeout);
		return this;
	}

	/**
	 * 设置本地缓存大小
	 * 
	 * @param size 字节
	 * @return
	 * @throws JSchException
	 */
	@Comment(text = "设置缓存")
	public SFTP buffer(@Comment(text = "size") int size) throws JSchException {
		return this;
	}

	/**
	 * 
	 * @param url, sftp://g01:22?username=,passwd=
	 * @return
	 * @throws JSchException
	 */
	public static SFTP create(Url url) throws IOException {
		SFTP s = new SFTP();
		s.open(url);
		return s;
	}

	public static SFTP create() {
		return new SFTP();
	}

	/**
	 * 打开远程链接
	 * 
	 * @param url 远程链接，sftp://[host:port]/[path]?username=xxx&passwd=xxx
	 * @return
	 * @throws IOException
	 */
	@Comment(text = "远程链接，sftp://[host:port]/[path]?username=xxx&passwd=xxx")
	public SFTP open(@Comment(text = "url") String url) throws IOException {
		return open(Url.create(url));
	}

	/**
	 * 打开远程链接
	 * 
	 * @param url 远程链接， sftp://[host:port]/[path]?username=xxx&passwd=xxx
	 * @return
	 * @throws IOException
	 */
	@Comment(text = "远程链接，sftp://[host:port]/[path]?username=xxx&passwd=xxx")
	public SFTP open(@Comment(text = "url") Url url) throws IOException {

		close();

		try {
			session = getSession(url);
			timeout(300 * 1000);

			sftp = (ChannelSftp) session.openChannel("sftp");
			sftp.connect();

			return this;
		} catch (Exception e) {
			String s = url.toString();
			int i = s.indexOf("?");
			if (i > 0) {
				s = s.substring(0, i);
			}
			throw new IOException(s, e);
		}
	}

	/**
	 * 上传文件
	 * 
	 * @param filename 文件路径
	 * @param in       输入字节流
	 * @return
	 * @throws IOException
	 */
	@Comment(text = "上传文件")
	public boolean put(@Comment(text = "filename") String filename, @Comment(text = "in") InputStream in)
			throws IOException {
		return put(new File(filename), in);
	}

	@Comment(text = "上传文件")
	public boolean put(@Comment(text = "fileorpath") File src, @Comment(text = "dest") String dest,
			@Comment(text = "callback") Function<String, Boolean> func) throws IOException {

		try {
			if (func.apply(src.getAbsolutePath())) {
				if (src.isDirectory()) {
					File[] ff = src.listFiles();
					if (ff != null) {
						String dest1 = X.getCanonicalPath(dest + "/" + src.getName());
						this.mkdir(dest1);

						for (File f : ff) {
							if (X.isIn(f.getName(), ".", ".."))
								continue;

							put(new FileInputStream(f), dest1 + "/" + f.getName());
						}
					}
				} else if (src.isFile()) {
					InputStream in = new FileInputStream(src);
					try {
						sftp.put(in, X.getCanonicalPath(dest + "/" + src.getName()));
					} finally {
						X.close(in);
					}
				}
			}

			return true;
		} catch (Exception e) {
			throw new IOException(e);
		}

	}

	@Comment(text = "上传文件")
	public boolean put(@Comment(text = "in") InputStream in, @Comment(text = "dest") String dest) throws IOException {

		try {
			sftp.put(in, X.getCanonicalPath(dest));
			return true;
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			X.close(in);
		}

	}

	@Comment(text = "新建目录")
	public boolean mkdir(@Comment(text = "dest") String dest) {

		try {
			sftp.mkdir(dest);
			return true;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return false;
		}

	}

	@Comment(text = "移动文件")
	public boolean mv(@Comment(text = "src") String src, @Comment(text = "dest") String dest) throws IOException {

		try {
			this.mkdir(new File(dest).getParent());
			sftp.rename(src, dest);
			return true;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("sftp", "mv", "src=" + src + ", dest=" + dest, e);
			throw new IOException(e);
		}
	}

	/**
	 * 上传文件
	 * 
	 * @param f  文件对象
	 * @param in 输入字节流
	 * @throws IOException
	 */
	@Comment(text = "上传文件")
	public boolean put(@Comment(text = "file") File f, @Comment(text = "in") InputStream in) throws IOException {

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

	/**
	 * 下载文件
	 * 
	 * @param filename 文件路径
	 * @return 临时文件对象
	 * @throws IOException
	 */
	@Comment(text = "下载文件")
	public Temp get(@Comment(text = "filename") String filename) throws IOException {
		File f = new File(filename);
		Temp t = Temp.create(f.getName());
		get(f, t.getOutputStream());
		return t;
	}

	/**
	 * 下载文件
	 * 
	 * @param file 文件对象
	 * @return
	 * @throws IOException
	 */
	@Comment(text = "下载文件")
	public Temp get(@Comment(text = "file") File file) throws IOException {
		Temp t = Temp.create(file.getName());
		get(file, t.getOutputStream());
		return t;
	}

	/**
	 * 下载文件
	 * 
	 * @param filename 文件路径
	 * @param dest     本地文件路径
	 * @throws IOException
	 */
	@Comment(text = "下载文件")
	public void get(@Comment(text = "filename") String filename, @Comment(text = "out") OutputStream out)
			throws IOException {
		try {
			sftp.get(filename, out);
		} catch (Exception e) {
			throw new IOException(e);
		} finally {
			X.close(out);
		}
	}

	/**
	 * 下载文件
	 * 
	 * @param filename 文件对象
	 * @param dest     本地文件路径
	 * @throws IOException
	 */
	@Comment(text = "下载文件")
	public void get(@Comment(text = "filename") File filename, @Comment(text = "out") OutputStream dest)
			throws IOException {
		get(filename.getAbsolutePath(), dest);
	}

	/**
	 * 查询文件列表
	 * 
	 * @param src 目录路径
	 * @return 文件数组
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	@Comment(text = "列表目录")
	public File[] list(@Comment(text = "src") String src) throws IOException {

		try {
			List<File> l1 = new ArrayList<File>();
			Vector<LsEntry> l2 = sftp.ls(src);
//			log.info("sftp list, size=" + l2.size() + ", vector=" + l2);
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
				String name = f.getFilename();
				if ("..".equals(name) || ".".equals(name)) {
					continue;
				}

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

	/**
	 * 删除文件
	 * 
	 * @param src 文件路径
	 * @throws IOException
	 */
	@Comment(text = "删除文件")
	public void rm(@Comment(text = "filename") String src) throws IOException {
		try {
			sftp.rm(src);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	/**
	 * 删除目录
	 * 
	 * @param src 目录路径
	 * @throws IOException
	 */
	@Comment(text = "删除目录")
	public void rmdir(@Comment(text = "path") String src) throws IOException {
		try {
			sftp.rmdir(src);
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	private static Session getSession(Url url) throws IOException {

		JSch jsch = new JSch();

		try {

			String username = url.get("username");
			if (X.isEmpty(username)) {
				throw new IOException("[username] required");
			}

			String passwd = url.get("passwd");
			if (X.isEmpty(passwd)) {
				throw new IOException("[passwd] required");
			}

			Session session = jsch.getSession(username, url.getIp(), url.getPort(22));
			session.setPassword(passwd);

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
