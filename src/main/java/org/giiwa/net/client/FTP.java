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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;
import org.giiwa.bean.Temp;
import org.giiwa.dao.Comment;
import org.giiwa.dao.X;
import org.giiwa.misc.Url;

@Comment(text = "FTP工具")
public class FTP implements Closeable {

	private static Log log = LogFactory.getLog(FTP.class);

	private FTPClient client;

	@Comment(hide = true)
	public FTPClient getClient() {
		return client;
	}

	/**
	 * 设置超时时间
	 * 
	 * @param timeout 毫秒
	 * @return
	 */
	@Comment(text = "设置超时")
	public FTP timeout(@Comment(text = "timeout") int timeout) {
		client.setDataTimeout(timeout);
		client.setConnectTimeout(timeout);
		return this;
	}

	/**
	 * 设置本地数据缓存大小
	 * 
	 * @param size 字节
	 * @return
	 * @throws SocketException
	 */
	@Comment(text = "设置buffer")
	public FTP buffer(@Comment(text = "size") int size) throws SocketException {

		client.setBufferSize(size);
		client.setReceiveBufferSize(size);

		return this;
	}

	/**
	 * 关闭链接
	 */
	@Comment(hide = true)
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

	/**
	 * 上传文件
	 * 
	 * @param remote 文件路径
	 * @param in     输入流
	 * @return
	 * @throws IOException
	 */
	@Comment(text = "上传文件")
	public boolean put(@Comment(text = "filename") String remote, @Comment(text = "in") InputStream in)
			throws IOException {
		return client.appendFile(remote, in);
	}

	/**
	 * 下载文件
	 * 
	 * @param remote 文件路径
	 * @return 临时文件
	 * @throws IOException
	 */
	@Comment(text = "下载文件")
	public Temp get(@Comment(text = "filename") String remote) throws IOException {
		return get(new File(remote));
	}

	/**
	 * 下载文件
	 * 
	 * @param remote 文件路径
	 * @return 临时文件
	 * @throws IOException
	 */
	@Comment(text = "下载文件")
	public Temp get(@Comment(text = "filename") File remote) throws IOException {

		Temp t = Temp.create(remote.getName());

//		client.enterRemotePassiveMode();
		client.enterLocalPassiveMode();
		client.setFileType(FTPClient.BINARY_FILE_TYPE);

		OutputStream out = t.getOutputStream();
		client.retrieveFile(remote.getAbsolutePath(), out);
//		out.flush();
		out.close();

		return t;
	}

	/**
	 * 查询文件列表
	 * 
	 * @param path 目录路径
	 * @return 文件数组
	 * @throws IOException
	 */
	@Comment(text = "列表目录")
	public File[] list(@Comment(text = "path") String path) throws IOException {

		if (log.isDebugEnabled())
			log.debug("list path=" + path);

		List<File> l1 = new ArrayList<File>();

		client.changeWorkingDirectory(path);
		client.enterLocalPassiveMode();
//		client.enterRemotePassiveMode();

		FTPFile[] ff = client.listFiles();

		_toFile(path, l1, ff);

//		ff = client.listDirectories();
//		_toFile(path, l1, ff);

		if (log.isDebugEnabled())
			log.debug("get files =" + l1);

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
	 * @throws IOException
	 * @throws SocketException
	 */
	public static FTP create(Url url) throws IOException {
		FTP f = new FTP();
		f.open(url, null);
		return f;
	}

	public static FTP create() {
		return new FTP();
	}

	/**
	 * 打开远程连接
	 * 
	 * @param url 远程链接，ftp://[host:port]/[path]?username=xxx&passwd=xxx
	 * @return
	 * @throws IOException
	 */
	@Comment(text = "链接服务器, ftp://[host:port]/[path]?username=xxx&passwd=xxx")
	public FTP open(@Comment(text = "url") String url) throws IOException {
		return open(url, null);
	}

	@Comment(text = "链接服务器, ftp://[host:port]/[path]?username=xxx&passwd=xxx")
	public FTP open(@Comment(text = "url") String url, @Comment(text = "charset") String charset) throws IOException {
		return open(Url.create(url), charset);
	}

	/**
	 * 打开远程链接
	 * 
	 * @param url 远程链接
	 * @return
	 * @throws IOException
	 */
	public FTP open(Url url, String charset) throws IOException {
		return open(url, url.get("username"), url.get("passwd"), charset);
	}

	@Comment(text = "链接服务器, ftp://[host:port]/[path]", demo = "..open('ftp://[host:port]/[path]', username, passwd, 'utf8')")
	public FTP open(@Comment(text = "url") String url, @Comment(text = "username") String username,
			@Comment(text = "passwd") String passwd, @Comment(text = "charset") String charset) throws IOException {
		return open(Url.create(url), username, passwd, charset);
	}

	public FTP open(Url url, String username, String passwd, String charset) throws IOException {

		close();

		FTPClient ftp = new FTPClient();
		FTPClientConfig config = new FTPClientConfig();
		// config.setXXX(YYY); // change required options
		// for example config.setServerTimeZoneId("Pacific/Pitcairn")
//		if (!X.isEmpty(charset)) {
//			ftp.setControlEncoding(charset);
//			config.setServerLanguageCode("zh");
//		}
		ftp.configure(config);
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

		if (ftp.login(username, passwd)) {
			client = ftp;
			client.setConnectTimeout(10000);
			if (!X.isEmpty(charset)) {
				client.setControlEncoding(charset);
			} else {
				client.setControlEncoding("UTF-8");
			}
			timeout(300 * 1000);
			buffer(1024 * 1024);

			if (log.isDebugEnabled())
				log.debug("logined");

			return this;
		}

		throw new IOException("login failed");

	}

	/**
	 * 创建目录
	 * 
	 * @param path 文件路径
	 * @throws IOException
	 */
	@Comment(text = "新建目录")
	public void mkdirs(@Comment(text = "path") String path) throws IOException {
		client.mkd(path);
	}

	@Comment(text = "删除文件")
	public void rm(@Comment(text = "filename") String filename) throws IOException {
		client.deleteFile(filename);
	}

	/**
	 * 移动文件
	 * 
	 * @param filename1 原始文件
	 * @param filename2 目标文件
	 * @throws IOException
	 */
	@Comment(text = "移动文件")
	public void mv(@Comment(text = "filename1") String filename1, @Comment(text = "filename2") String filename2)
			throws IOException {
		if (log.isDebugEnabled())
			log.debug("move file, " + filename1 + "=>" + filename2);

		client.rename(filename1, filename2);
	}

	/**
	 * 复制文件
	 * 
	 * @param filename1 原始文件
	 * @param filename2 目标文件
	 * @throws IOException
	 */
	@Comment(text = "复制文件")
	public void cp(@Comment(text = "filename1") String filename1, @Comment(text = "filename2") String filename2)
			throws IOException {

		if (log.isDebugEnabled())
			log.debug("copy file, " + filename1 + "=>" + filename2);

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		client.retrieveFile(filename1, out);
		ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());

		client.makeDirectory(new File(filename2).getParent());
		client.storeFile(filename2, in);
		out.flush();
		out.close();
		in.close();

	}

}
