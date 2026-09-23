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
package org.giiwa.app.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.fileupload2.core.FileItem;
import org.giiwa.auth.ICaptcha;
import org.giiwa.bean.App;
import org.giiwa.bean.Policy;
import org.giiwa.bean.Data;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Menu;
import org.giiwa.bean.Message;
import org.giiwa.bean.Node;
import org.giiwa.bean.Temp;
import org.giiwa.bean.User;
import org.giiwa.cache.TimingCache;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.crypto.Base32;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Comment;
import org.giiwa.dao.Counter;
import org.giiwa.dao.Helper;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.sql.SQL;
import org.giiwa.dao.Helper.W;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.Captcha;
import org.giiwa.misc.GImage;
import org.giiwa.misc.Url;
import org.giiwa.task.Console;
import org.giiwa.task.Monitor;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.GiiwaServlet;
import org.giiwa.web.Path;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 资源接口
 * 
 * @author joe
 *
 */
@Comment(text = "文件资源与平台基础工具 - 提供文件存储上传、在线预览、文件下载，同时集成系统健康探测、调试回显、验证码、公钥获取等平台基础辅助功能")
public class f extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Counter get = new Counter("g");
	private static Counter down = new Counter("d");

	public void onGet() {
		String t = this.get("t");
		if (X.isSame(t, "d")) {
			// download
			// : f/id/name
			String[] ss = X.split(path, "/");
			d(ss[0], ss[1]);
		} else {
			// view
			String[] ss = X.split(path, "/");
			g(ss[0], ss[1]);
		}
	}

	public static Counter.Stat statGet() {
		return get.get();
	}

	public static Counter.Stat statDown() {
		return down.get();
	}

	/**
	 * get file
	 * 
	 * @param id
	 * @param name
	 */
	@Path(path = "g/(.*)/(.*)", memo = """
			在线预览文件仓库内的资源文件，可直接在浏览器或播放器打开图片、视频、文档；访问链接：/f/g/[id]/[name]。图片支持指定宽高自动缩放。访问受权限管控：部分仓库要求登录或访问来源在白名单；仓库关闭预览功能时会自动切换为文件下载；系统安全策略根据访问来源控制是否放行。
			""", in = """
			id：必须，文件内部唯一ID，拼接在预览链接首段；
			name：必须，带扩展名的文件名，拼接在预览链接第二段。
			size：可选，仅图片生效，自动缩放图片，格式：宽度x高度，示例：size=100x50，输出100×50尺寸图片。
			""", out = """
			返回Content-Type与文件二进制流，供浏览器/播放器在线预览资源，或直接下载文件。
			""")
	public void g(String id, String name) {

		TimeStamp t = TimeStamp.create();

		try {

			if (Global.getInt("f.g.login", 0) == 1) {
				if (this.user() == null) {

					String whiteip = Global.getString("f.g.whitelist", "");
					if (X.isEmpty(whiteip) || !this.ip().matches(whiteip)) {
						// 需要登录
						this.set(X.ERROR, lang.get("login.required")).send(201);
						return;
					}
					// 满足白名单
				}
			}

			if (Global.getInt("f.g.online", 1) != 1) {
				d(id, name);
				return;
			}

			DFile f1 = Disk.get(id);
			if (f1 == null) {
				log.warn("bad id, [" + id + "]");
				return;
			}

			// check security
			Policy p = Policy.matches(this, "dfile:" + f1.getFilename());
			if (p != null && !p.allow(true)) {
				log.warn("blacklist, [" + id + "]");
				return;
			}

			if (f1.exists() && f1.isFile()) {

				// GLog.applog.info(f.class, "get", f1.getFilename());

				String mime = null;
				if (f1.getName().endsWith(".ppd")) {
					mime = "text/plain;charset=UTF-8";
				} else {
					mime = Controller.getMimeType(f1.getName());
					if (X.isEmpty(mime)) {
						if (f1.getName().endsWith(".wsf")) {
							mime = "zip/wsf";
						} else {
							mime = Controller.getMimeType(name);
						}
					}

					if (X.isEmpty(mime)) {
						// read file
						mime = mime(f1.getInputStream());
					}
				}

				if (_image(mime, id, f1)) {
					return;
				}

				if (_video(mime, f1)) {
					return;
				}

				if (_wsf(f1, name)) {
					return;
				}

				if (X.isIn(mime, "text/html")) {
					mime = "text/html;charset=UTF-8";
				}
				if (X.isEmpty(mime)) {
					// download
					d(id, name);
					return;
				}
				this.setContentType(mime);

				log.warn("mime=" + mime + ", filename=" + f1.getFilename());
				_send(f1.getInputStream(), f1.length());

			} else {
				log.info("not found, [" + f1.getFilename() + "]");
				this.set(X.ERROR, "not found, file=" + f1.getFilename()).send(201);
			}

		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			GLog.oplog.error(this, path, e1.getMessage(), e1);
			this.set(X.ERROR, e1.getMessage()).send(201);
		} finally {
			get.add(t.pastms(), "id=%s, name=%s", id, name);
		}

	}

	private boolean _video(String mime, DFile f1) throws IOException {

		if (mime != null && mime.startsWith("video/")) {
			this.setContentType(mime);
			_send(f1.getInputStream(), f1.length());
			return true;
		}

		return false;
	}

	private boolean _wsf(DFile f1, String name) throws IOException {

		if (f1.getName().endsWith(".wsf")) {

			if (name.endsWith(".wsf")) {
				name = "index.html";
			}

			log.info("finding [" + name + "]");

			ZipInputStream in = new ZipInputStream(f1.getInputStream());
			try {
				ZipEntry e = in.getNextEntry();
				while (e != null) {
					if (X.isSame(name, e.getName())) {
						String mime = Controller.getMimeType(name);
						this.setContentType(mime);
						log.info("found [" + name + "], size=" + e.getSize());
						OutputStream out = this.getOutputStream();
						X.IO.copy(in, out, false);
						out.flush();
						return true;
					}
					e = in.getNextEntry();
				}
			} finally {
				X.close(in);
			}

			return true;
		}

		return false;
	}

	private boolean _image(String mime, String id, DFile f1) throws IOException {
		if (mime != null && !X.isIn(mime, "image/x-icon", "image/gif") && mime.startsWith("image/")) {

			// scale to largest w/h by "size"
			String size = this.getString("size");
			if (!X.isEmpty(size)) {
				String[] ss = size.split("x");

				if (ss.length == 2) {

					Temp t1 = Temp.create(id, "s_" + size);
					DFile f = t1.getDFile();

					if (log.isDebugEnabled()) {
						log.debug("file=" + f.getFilename() + ", exists=" + f.exists());
					}

					if (!f.exists() || f.length() == 0) {

//						Temp t2 = Temp.create("a.png");
//						long len = IOUtil.copy(f1.getInputStream(), t2.getOutputStream());
//						if (len != f1.length())
//							throw new IOException("get dfile error");
//
//						GImage.scale1(t2.getInputStream(), t1.getOutputStream(), X.toInt(ss[0]), X.toInt(ss[1]));
						GImage.scale(f1.getInputStream(), t1.getOutputStream(), X.toInt(ss[0]), X.toInt(ss[1]),
								f1.getExt());
						t1.upload();

					} else if (log.isDebugEnabled()) {
						log.debug("load the image from the temp cache, file=" + f.getFilename());
					}

					f.refresh();
					if (f.exists()) {
						if (log.isDebugEnabled()) {
							log.debug("load the scaled image from " + f.getFilename());
						}

						this.setContentType(Controller.getMimeType("a.jpg"));

						_send(f.getInputStream(), f.length());

						return true;
					} else if (log.isWarnEnabled()) {
						log.debug("the file not exists? " + f.getFilename());
					}
				}
			} else if (X.isIn(mime, "image/tiff")) {
				// TIFF
				Temp t1 = Temp.create(id, "s_" + size);
				DFile f = t1.getDFile();
				if (!f.exists() || f.length() == 0) {

//					Temp t2 = Temp.create("a.png");
//					long len = IOUtil.copy(f1.getInputStream(), t2.getOutputStream());
//					if (len != f1.length()) {
//						throw new IOException("get dfile error");
//					}

					GImage.scale1(f1.getInputStream(), t1.getOutputStream());
					t1.upload();

				} else if (log.isDebugEnabled()) {
					log.debug("load the image from the temp cache, file=" + f.getFilename());
				}

				f.refresh();

				if (f.exists()) {
					if (log.isDebugEnabled()) {
						log.debug("load the scaled image from " + f.getFilename());
					}

					this.setContentType(Controller.getMimeType("a.png"));

					_send(f.getInputStream(), f.length());

					return true;
				} else if (log.isWarnEnabled()) {
					log.debug("the file not exists? " + f.getFilename());
				}

			}

			// scale to smallest w/h by "size2"
			String size2 = this.getString("size2");
			if (!X.isEmpty(size2)) {
				String[] ss = size2.split("x");

				if (ss.length == 2) {

					Temp t1 = Temp.create(id, "s2_" + size2);
					DFile f = t1.getDFile();

					if (log.isDebugEnabled()) {
						log.debug("file=" + f.getFilename() + ", exists=" + f.exists());
					}

					if (!f.exists() || f.length() == 0) {

//						Temp t2 = Temp.create("a.png");
//						long len = IOUtil.copy(f1.getInputStream(), t2.getOutputStream());
//						if (len != f1.length())
//							throw new IOException("get dfile error");

						GImage.scale3(f1.getInputStream(), t1.getOutputStream(), X.toInt(ss[0]), X.toInt(ss[1]));
						t1.upload();

					} else if (log.isDebugEnabled()) {
						log.debug("load the image from the temp cache, file=" + f.getFilename());
					}

					f.refresh();
					if (f.exists()) {
						if (log.isDebugEnabled()) {
							log.debug("load the scaled image from " + f.getFilename());
						}

						this.setContentType(Controller.getMimeType("a.png"));

						_send(f.getInputStream(), f.length());

						return true;
					} else if (log.isWarnEnabled()) {
						log.debug("the file not exists? " + f.getFilename());
					}
				}
			} else if (X.isIn(mime, "image/tiff")) {
				// TIFF
				Temp t1 = Temp.create(id, "s2_" + size2);
				DFile f = t1.getDFile();
				if (!f.exists() || f.length() == 0) {

//					Temp t2 = Temp.create("a.png");
//					long len = IOUtil.copy(f1.getInputStream(), t2.getOutputStream());
//					if (len != f1.length()) {
//						throw new IOException("get dfile error");
//					}

					GImage.scale1(f1.getInputStream(), t1.getOutputStream());
					t1.upload();

				} else if (log.isDebugEnabled()) {
					log.debug("load the image from the temp cache, file=" + f.getFilename());
				}

				f.refresh();

				if (f.exists()) {
					if (log.isDebugEnabled()) {
						log.debug("load the scaled image from " + f.getFilename());
					}

					this.setContentType(Controller.getMimeType("a.png"));

					_send(f.getInputStream(), f.length());

					return true;
				} else if (log.isWarnEnabled()) {
					log.debug("the file not exists? " + f.getFilename());
				}

			}

		}
		return false;
	}

	/**
	 * original file path
	 * 
	 * @param filename file path
	 */
	@Path(path = "s/(.*)")
	public void s(String filename) {

		TimeStamp t = TimeStamp.create();

		try {

			if (Global.getInt("f.g.login", 0) == 1) {
				if (this.user() == null) {

					String whiteip = Global.getString("f.g.whitelist", "");
					if (X.isEmpty(whiteip) || !this.ip().matches(whiteip)) {
						// 需要登录
						this.set(X.ERROR, lang.get("login.required")).send(201);
						return;
					}
					// 满足白名单
				}
			}

			if (!filename.startsWith("/")) {
				filename = "/" + filename;
			}

			DFile f1 = Disk.seek(filename);

			// check security
			Policy p = Policy.matches(this, "dfile:" + f1.getFilename());
			if (p != null && !p.allow(true)) {
				log.warn("blacklist, [" + filename + "]");
				return;
			}

			if (f1 != null && f1.exists() && f1.isFile()) {
				// GLog.applog.info(f.class, "download", f1.getFilename());
				if (Global.getInt("f.g.online", 0) == 1) {
					// online view
					// TODO
					String mime = Controller.getMimeType(f1.getName());
					if (X.isEmpty(mime)) {
						mime = Controller.getMimeType(f1.getName());
					}

					if (X.isEmpty(mime)) {
						// read file
						mime = mime(f1.getInputStream());
					}

//					log.info("mime=" + mime);
					if (_image(mime, f1.getId(), f1)) {
						return;
					}

					if (_video(mime, f1)) {
						return;
					}

					if (X.isIn(mime, "text/html")) {
						mime = "text/html;charset=UTF-8";
					}
					this.setContentType(mime);

					_send(f1.getInputStream(), f1.length());

				} else {
					this.send(f1.getName(), f1.getInputStream(), f1.length());
				}

			} else {
				this.set(X.ERROR, "not found [" + filename + "]").send(201);
			}

		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			this.set(X.ERROR, e1.getMessage()).send(201);
		} finally {
			get.add(t.pastms(), "filename=%s", filename);
		}

	}

	/**
	 * download file
	 * 
	 * @param id
	 * @param name
	 */
	@Path(path = "d/(.*)/(.*)", memo = """
			下载文件仓库里的资源文件，生成文件下载链接/f/d/[id]/[name]；支持图片缩放，传入size参数可自动调整图片尺寸，返回二进制文件流供浏览器执行下载。访问受权限控制，部分资源需要用户登录或者访问来源在白名单内；系统安全策略会依据访问凭证与来源放行或拦截请求。
			""", in = """
			id：必须，拼接在链接中的第一个参数， 文件的内部ID；
			name：必须，拼接在链接中的第二个参数，文件名，包含扩展名。
			size：可选，用于图片资源自动缩放，size=宽度x高度，比如：size=100x50，输出的图片大小为100x50。
			""", out = """
			Content-Type=application/octet-stream和文件二进制内容，用于指导浏览器下载。
			""")
	public void d(String id, String name) {

		TimeStamp t = TimeStamp.create();

		try {
			if (Global.getInt("f.g.login", 0) == 1) {
				if (this.user() == null) {

					String whiteip = Global.getString("f.g.whitelist", "");
					if (X.isEmpty(whiteip) || !this.ip().matches(whiteip)) {
						// 需要登录
						this.set(X.ERROR, lang.get("login.required")).send(201);
						return;
					}
					// 满足白名单
				}
			}

			DFile f1 = Disk.get(id);

			// check security
			Policy p = Policy.matches(this, "dfile:" + f1.getFilename());
			if (p != null && !p.allow(true)) {
				log.warn("blacklist, [" + id + "]");
				return;
			}

			if (f1 != null && f1.exists() && f1.isFile()) {

				GLog.oplog.warn(this, "download", f1.getFilename());
				this.send(f1.getName(), f1.getInputStream(), f1.length());

			}
		} catch (Exception e) {
			this.error(e);
		} finally {
			down.add(t.pastms(), "id=%s, name=%s", id, name);
		}

	}

	/**
	 * upload file
	 */
	@Path(path = "upload", memo = """
			上传文件至临时文件仓库，支持断点续传、大文件分片上传。可比对本地文件修改时间做断点检测：首次上传数据包，若服务端已有同文件且修改时间匹配，则返回已上传偏移位置，客户端直接续传剩余分片。上传权限受系统配置控制，支持匿名上传，或要求登录、访问来源在白名单内。
			""", in = """
			file： 必须，multiple-body的part，包含文件名和二进制数据；
			Content-Range：可选，range, position/total， 用于断点上传；
			lastModified：可选，本地文件最后修改时间，如果与服务器上文件一致，传递第一个数据包的时候，服务器会返回当前文件大小，告诉客户端直接上传后续数据包，实现断点上传。
			""", out = """
			state：200，正常，否则错误；
			error：0，正常，否则错误，与state类似，兼容不同插件设计；
			repo：文件仓库id；
			url：访问链接；
			preview：预览链接；
			pos：服务器当前文件大小；
			size：客户端文件大小；
			name：文件名。
			""")
	public void upload() {

		if (Global.getInt("f.upload.login", 1) == 1) {

			if (this.user() == null) {

				String whiteip = Global.getString("f.upload.whitelist", "");
				if (X.isEmpty(whiteip) || !this.ip().matches(whiteip)) {
					// 需要登录
					this.set(X.ERROR, lang.get("login.required")).send(201);
					return;
				}
				// 满足白名单
			}
		}

		String path = this.get("path");

		if (log.isDebugEnabled()) {
			log.debug("json=" + this.json());
		}

		JSON jo = new JSON();

		// String access = Module.home.get("upload.require.access");

		FileItem<?> file = this.file("file");
		if (file != null) {

			if (log.isInfoEnabled()) {
				log.info("save file, file=" + file);
			}

			String filename = this.getString("filename");
			if (X.isEmpty(filename)) {
				filename = file.getName();
			}
			if (X.isEmpty(filename)) {
				filename = "image.jpg";
			}

			filename = Url.decode(filename).trim();

			filename = filename.replaceAll("[ *$?\\%]", "_");
			if (filename.length() > 64) {
				int i = filename.lastIndexOf(".");
				if (i > 0) {
					String ext = filename.substring(i);
					if (ext.length() > 10) {
						ext = ext.substring(0, 10);
					}
					filename = filename.substring(0, Math.min(64, i)) + "_" + UID.id(filename) + ext;
				} else {
					filename = filename.substring(0, 64) + "_" + UID.id(filename);
				}
			}
			_store(file, path, filename, jo);

		} else {

			if (log.isInfoEnabled()) {
				log.info("file=null， trying base64 ...");
			}

			String encoder = this.get("encoder");
			if (X.isIn(encoder, "base64")) {
				String filename = this.get("filename");
				if (X.isEmpty(filename)) {
					filename = "image.jpg";
				}

				filename = Url.decode(filename).trim();

				filename = filename.replaceAll("[ *$?\\-%]", "_");
				if (filename.length() > 64) {
					int i = filename.lastIndexOf(".");
					if (i > 0) {
						String ext = filename.substring(i);
						if (ext.length() > 10) {
							ext = ext.substring(0, 10);
						}
						filename = filename.substring(0, Math.min(64, i)) + "_" + UID.id(filename) + ext;
					} else {
						filename = filename.substring(0, 64) + "_" + UID.id(filename);
					}
				}
				String data = this.getHtml("data");
				byte[] bb = Base64.getDecoder().decode(data);
				_store(bb, path, filename, jo);

			} else {

				log.warn("读取文件失败, param=" + this.json());

				jo.append(X.STATE, HttpServletResponse.SC_BAD_REQUEST)
						.append(X.ERROR, HttpServletResponse.SC_BAD_REQUEST)
						.append(X.MESSAGE, lang.get("upload.notfound"));
			}
		}

		this.send(jo.append("path", path).append(X.NODE, Local.label()));

	}

	private boolean _store(FileItem<?> file, String path, String filename, JSON jo) {
//		String tag = this.getString("tag");

		TimeStamp t = TimeStamp.create();
		DFile f1 = null;

		try {

			String range = this.head("Content-Range");
			if (range == null) {
				range = this.getString("Content-Range");
			}
			long position = 0;
			long total = 0;
			String lastModified = this.head("lastModified");
			if (X.isEmpty(lastModified)) {
				lastModified = this.getString("lastModified");
			}
			if (X.isEmpty(lastModified)) {
				lastModified = this.getString("lastModifiedDate");
			}

			if (range != null) {

				// bytes 0-9999/22775650
				String[] ss = range.split(" ");
				if (ss.length > 1) {
					range = ss[1];
				}
				ss = range.split("-|/");
				if (ss.length == 3) {
					position = X.toLong(ss[0]);
					total = X.toLong(ss[2]);
				}

				// log.debug(range + ", " + position + "/" + total);
			}

			String id = X.isEmpty(range) ? UID.uuid() : UID.id(filename, total, lastModified);

			if (log.isDebugEnabled()) {
				log.debug("storing, id=" + id + ", path=" + path + ", filename=" + filename + ", position=" + position
						+ ", total=" + total + ", last=" + lastModified);
			}

			TimeStamp t1 = TimeStamp.create();
			f1 = _get(id, filename);
			if (log.isDebugEnabled()) {
				log.debug("seek file, cost=" + t1.past() + ", filename=" + f1.getFilename());
				t1.reset();
			}

			try {
				if (!f1.exists() || f1.length() == position) {
					f1.upload(position, file.getInputStream());
					if (log.isDebugEnabled()) {
						log.debug("upload file, cost=" + t1.past() + ", filename=" + f1.getFilename());
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			f1.refresh();
			long pos = f1.length();

//			long pos = e1.store(position, file.getInputStream(), total);
			// Repo.append(id, filename, position, total,file.getInputStream(),
			// login.getId(), this.ip());
			if (pos >= 0) {
				String url = "/f/d/" + f1.getId() + "/" + filename;
				String view = "/f/g/" + f1.getId() + "/" + filename;
				if (jo == null) {
					this.put("url", url);
					this.put(X.ERROR, 0);
					this.put("repo", url);
					this.put("preview", view);
					if (total > 0) {
						this.put(X.NAME, filename);
						this.put("pos", pos);
						this.put("size", total);
					}
				} else {
					jo.put("url", url);
					jo.put("repo", url);
					this.put("preview", view);
					jo.put(X.ERROR, 0);
					jo.put(X.NAME, filename);
					jo.put("type", Controller.getMimeType(filename));
					if (total > 0) {
						jo.put("pos", pos);
						jo.put("size", total);
					}
					jo.put(X.STATE, 200);
					jo.put("site", Global.getString("site.url", X.EMPTY));
				}

				// Session.load(sid()).set("access.repo." + id, 1).store();
			} else {
				log.warn("file=" + f1 + ", filename=" + filename);
				if (jo == null) {
					this.put(X.ERROR, HttpServletResponse.SC_BAD_REQUEST);
					this.put(X.MESSAGE, lang.get("repo.locked"));
					this.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
				} else {
					jo.put(X.ERROR, HttpServletResponse.SC_BAD_REQUEST);
					jo.put(X.MESSAGE, lang.get("repo.locked"));
					jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
				}
				return false;
			}
			return true;
		} catch (Exception e) {

			log.error(filename, e);

			GLog.oplog.error(this, "upload", filename, e);

			if (jo == null) {
				this.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				this.put(X.MESSAGE, e.getMessage());
				this.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				jo.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				jo.put(X.MESSAGE, e.getMessage());
				jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		} finally {
			if (t.pastms() > 1000) {
				// upload 很慢
				GLog.oplog.warn(getClass(), "upload", "cost=" + t.past() + ",filename=" + f1 + ", result=" + jo, null,
						this.ip());
			}
		}

		return false;
	}

	private boolean _store(byte[] bb, String path, String filename, JSON jo) {
//		String tag = this.getString("tag");

		TimeStamp t = TimeStamp.create();
		DFile f1 = null;

		try {

			String range = this.head("Content-Range");
			if (range == null) {
				range = this.getString("Content-Range");
			}
			long position = 0;
			long total = 0;
			String lastModified = this.head("lastModified");
			if (X.isEmpty(lastModified)) {
				lastModified = this.getString("lastModified");
			}
			if (X.isEmpty(lastModified)) {
				lastModified = this.getString("lastModifiedDate");
			}

			if (range != null) {

				// bytes 0-9999/22775650
				String[] ss = range.split(" ");
				if (ss.length > 1) {
					range = ss[1];
				}
				ss = range.split("-|/");
				if (ss.length == 3) {
					position = X.toLong(ss[0]);
					total = X.toLong(ss[2]);
				}

				// log.debug(range + ", " + position + "/" + total);
			}

			String id = X.isEmpty(range) ? UID.uuid() : UID.id(filename, total, lastModified);

			if (log.isDebugEnabled()) {
				log.debug("storing, id=" + id + ", path=" + path + ", filename=" + filename + ", position=" + position
						+ ", total=" + total + ", last=" + lastModified);
			}

			f1 = _get(id, filename);

			try {
				if (!f1.exists() || f1.length() == position) {
					f1.upload(position, bb);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			f1.refresh();
			long pos = f1.length();

//			long pos = e1.store(position, file.getInputStream(), total);
			// Repo.append(id, filename, position, total,file.getInputStream(),
			// login.getId(), this.ip());
			if (pos >= 0) {
				String url = "/f/d/" + f1.getId() + "/" + filename;
				String view = "/f/d/" + f1.getId() + "/" + filename;
				if (jo == null) {
					this.put("url", url);
					this.put(X.ERROR, 0);
					this.put("repo", url);
					this.put("preview", view);
					if (total > 0) {
						this.put(X.NAME, filename);
						this.put("pos", pos);
						this.put("size", total);
					}
				} else {
					jo.put("url", url);
					jo.put("repo", url);
					this.put("preview", view);
					jo.put(X.ERROR, 0);
					jo.put(X.NAME, filename);
					jo.put("type", Controller.getMimeType(filename));
					if (total > 0) {
						jo.put("pos", pos);
						jo.put("size", total);
					}
					jo.put(X.STATE, 200);
					jo.put("site", Global.getString("site.url", X.EMPTY));
				}

				// Session.load(sid()).set("access.repo." + id, 1).store();
			} else {
				if (jo == null) {
					this.put(X.ERROR, HttpServletResponse.SC_BAD_REQUEST);
					this.put(X.MESSAGE, lang.get("repo.locked"));
					this.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
				} else {
					jo.put(X.ERROR, HttpServletResponse.SC_BAD_REQUEST);
					jo.put(X.MESSAGE, lang.get("repo.locked"));
					jo.put(X.STATE, HttpServletResponse.SC_BAD_REQUEST);
				}
				return false;
			}
			return true;
		} catch (Exception e) {

			log.error(filename, e);

			GLog.oplog.error(this, "upload", filename, e);

			if (jo == null) {
				this.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				this.put(X.MESSAGE, e.getMessage());
				this.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				jo.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				jo.put(X.MESSAGE, e.getMessage());
				jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		} finally {
			if (t.pastms() > 1000) {
				// upload 很慢
				GLog.oplog.warn(getClass(), "upload", "cost=" + t.past() + ", filename=" + f1 + ", result=" + jo, null,
						this.ip());
			}
		}

		return false;
	}

	/**
	 * get temp file
	 */
	@Path(path = "temp/(.*)/(.*)", login = true)
	public void temp(String id, String name) {

		name = Url.decode(name);
		Temp t = Temp.create(id, name);
		File f1 = null;

		try {
			f1 = t.getFile();
			if (f1.exists()) {
				this.send(name, new FileInputStream(f1));// , f1.length());
				return;
			}

			DFile f2 = t.getDFile();

			if (f2 != null && f2.exists()) {
				this.send(name, f2.getInputStream());// , f2.length());
				return;
			}

		} catch (Exception e) {
			GLog.oplog.error(this, "temp", e.getMessage(), e);
		}

		GLog.oplog.warn(this, path, "not found, " + (f1 == null ? "null" : f1.getAbsolutePath()));

		this.notfound(name);

	}

	@Path(path = "alive", memo = """
			探测系统/服务器实时运行健康状态，查询服务负载、请求统计、缓存等监控指标，用来判断服务是否可用、查看当前服务压力。
			""", out = """
			state：200，正常，否则系统异常；
			online：当前在线请求数；
			uptime：启动时间；
			tps：请求/秒；
			latency：延时；
			total：总服务次数；
			ip：来源IP；
			cached：临时cached对象大小。
			""")
	public void alive() {
		if (Local.node().isAlive()) {
			this.set("online", GiiwaServlet.online());
			this.set("uptime", Controller.UPTIME);
			this.set("tps", GiiwaServlet.tps());
			this.set("latency", GiiwaServlet.latency());
			this.set("total", GiiwaServlet.total());
			this.set(X.NODE, Local.label());
			this.set(X.IP, this.ip());
			this.set("cached", TimingCache.getCached());
			this.send(200);
		} else {
			log.error("isAlive=" + Local.node().isAlive() + ", tasksDelay=" + Task.tasksDelay());
			this.set(X.MESSAGE, "dead!").error(500);
		}
	}

	@Path(path = "ping", memo = """
			Ping检测系统/服务器连通性，快速校验网络是否通畅、服务能否正常访问，用于简易的服务可用性测试。
			""", out = """
			ok - 正常
			（其他） - 错误
			""")
	public void ping() {
		print("ok");
	}

	@SuppressWarnings("rawtypes")
	@Path(path = "echo", memo = """
			调试专用工具，回显并反射展示请求头部与全部入参信息，查看原始输入数据，用于排查参数、请求内容问题。
			""", in = """
			任意
			""", out = """
			反射输入头和参数信息
			""")
	public void echo() {

		StringBuilder sb = new StringBuilder();
		sb.append("=======node=======\n");
		sb.append("node: " + Local.label() + "\n");

		sb.append("=======head=======\n");
		for (NameValue s : this.heads()) {
			if (!X.isSame(s.name, "Host")) {
				sb.append(s.name).append(": ").append(s.value).append("\n");
			}
		}
		sb.append("\n=======body:1=======\n");
		for (String name : this.names()) {
			String s = this.getHtml(name);
			if (s != null) {
				sb.append(name).append(": ").append(s).append("\n");
			} else {
				FileItem f = this.file(name);
				if (f != null) {
					sb.append(name).append(": ").append("{name=" + f.getName() + ", size=" + f.getSize() + "}")
							.append("\n");
				} else {
					sb.append(name).append(": null\n");
				}
			}
		}
		sb.append("\n=======body:2=======\n");
		sb.append(this.body());
		sb.append("\n");
		this.print(sb.toString());
	}

	@Path(path = "info")
	public void info() {

		W q = W.create().sort("label");
		Beans<Node> bs = Node.dao.load(q, 0, 1024);

		StringBuilder sb = new StringBuilder();
		sb.append("=== global ===\n");
		sb.append("global.id: " + Global.id() + "\n");
		sb.append("global.code: " + Global.getString("global.code", "") + "\n");

		sb.append("=== nodes ===\n");
		for (Node e : bs) {
			sb.append(e.label).append(": ").append(e.id).append("\n");
			sb.append("\t").append(e.ip).append("\n");
			sb.append("\t").append(e.giiwa).append("\n");
			sb.append("\t").append(e.modules).append("\n");
			sb.append("\n");
		}

		sb.append("=== license ===\n");
		org.giiwa.web.Module m = org.giiwa.web.Module.home;
		while (m != null) {
			sb.append(m.getName() + ": " + (X.isEmpty(m.get("company")) ? "" : ("//" + m.get("company")))).append("\n");
			JSON j1 = m.license();
			for (String name : j1.keySet()) {
				sb.append("\t" + name + "=" + j1.get(name)).append("\n");
			}
			m = m.floor();
		}

		this.print(sb.toString());

	}

//	void _send(InputStream in, long total) {
//
//		try {
//
//			String range = this.head("range");
//
//			if (log.isDebugEnabled()) {
//				log.debug("range=" + range);
//			}
//
//			long start = 0;
//			long end = total - 1;
//			if (!X.isEmpty(range)) {
//
////				Range: bytes=0-499 表示第 0-499 字节范围的内容 
////				Range: bytes=500-999 表示第 500-999 字节范围的内容 
////				Range: bytes=-500 表示最后 500 字节的内容 
////				Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容 
////				Range: bytes=0-0,-1 表示第一个和最后一个字节 
////				Range: bytes=500-600,601-999 同时指定几个范围
//				int i = range.indexOf("=");
//				if (i > 0) {
//					range = range.substring(i + 1).trim();
//				}
//				i = range.indexOf("-");
//				if (i > 0) {
//					start = X.toLong(range.substring(0, i));
//				} else {
//					// total - end
//					start = -1;
//				}
//				String s2 = range.substring(i + 1);
//				if (!X.isEmpty(s2)) {
//					end = Math.min(total, X.toLong(s2));
//					if (start == -1) {
//						start = total - end;
//						end = total - 1;
//					}
//				}
//			}
//
//			if (end <= start) {
//				end = start + 1024 * 32;
//			}
//
//			if (end > total - 1) {
//				end = total - 1;
//			}
//
//			long length = end - start + 1;
//
//			if (start == 0) {
//				this.head("Accept-Ranges", "bytes");
//			}
//			this.head("Content-Length", Long.toString(length));
//
//			if (end < total - 1) {
//				this.status(206);
//			}
//			this.head("Content-Range", "bytes " + start + "-" + (end) + "/" + total);
//
//			if (length > 0) {
//
//				OutputStream out = this.getOutputStream();
//				X.IO.copy(in, out, start, end, false);
//				out.flush();
//
//			}
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//		} finally {
//			X.close(in);
//		}
//
//	}

	void _send(InputStream in, long total) {

		try {

			String range = this.head("range");

			if (log.isDebugEnabled()) {
				log.debug("range=" + range);
			}

			long start = 0;
			long end = total - 1;
			if (!X.isEmpty(range)) {

//				Range: bytes=0-499 表示第 0-499 字节范围的内容 
//				Range: bytes=500-999 表示第 500-999 字节范围的内容 
//				Range: bytes=-500 表示最后 500 字节的内容 
//				Range: bytes=500- 表示从第 500 字节开始到文件结束部分的内容 
//				Range: bytes=0-0,-1 表示第一个和最后一个字节 
//				Range: bytes=500-600,601-999 同时指定几个范围
				int i = range.indexOf("=");
				if (i > 0) {
					range = range.substring(i + 1).trim();
				}
				i = range.indexOf("-");
				if (i > 0) {
					start = X.toLong(range.substring(0, i));
				} else {
					// total - end
					start = -1;
				}
				String s2 = range.substring(i + 1);
				if (!X.isEmpty(s2)) {
					end = Math.min(total, X.toLong(s2));
					if (start == -1) {
						start = total - end;
						end = total - 1;
					}
				}

				if (end <= start) {
					end = start + 1024 * 32;
				}

				if (end > total - 1) {
					end = total - 1;
				}

				long length = end - start + 1;

				if (start == 0) {
					this.head("Accept-Ranges", "bytes");
				}
				this.head("Content-Length", Long.toString(length));

//				if (X.isEmpty(range)) {
//					this.status(200);
//				} else {
				this.status(206);
//					this.head("range2", range);
//				}

				this.head("Content-Range", "bytes " + start + "-" + (end) + "/" + total);

				if (length > 0) {

					OutputStream out = this.getOutputStream();
					X.IO.copy(in, out, start, end, false);
					out.flush();

				}

			} else {
				// 200
				if (end <= start) {
					end = start + 1024 * 32;
				}

				if (end > total - 1) {
					end = total - 1;
				}

				long length = end - start + 1;

//				if (start == 0) {
//					this.head("Accept-Ranges", "bytes");
//				}
				this.head("Content-Length", Long.toString(length));

				this.status(200);

//				this.head("Content-Range", "bytes " + start + "-" + (end) + "/" + total);

				if (length > 0) {

					OutputStream out = this.getOutputStream();
					X.IO.copy(in, out, start, end, false);
					out.flush();

				}

			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in);
		}

	}

	/**
	 * check the task state in monitor<br>
	 * #/f/t/state?id=&access=
	 * 
	 */
	@Path(path = "t/state", login = true)
	public void t_state() {

		long id = this.getLong(X.ID, -1);
		String access = this.get("access");
		if (X.isEmpty(access)) {
			access = X.EMPTY;
		}

		JSON jo = Monitor.get(id, access);
		int state = jo == null ? 0 : jo.getInt(X.STATE);
		this.send(JSON.create().append(X.STATE, state > 0 && state != 200 ? 201 : 200).append("data", jo));
//				.append("params", req.getParameterMap()).append("ct", req.getContentType())
//				.append("url", req.getRequestURI()));

	}

	private static boolean _message_optimized = false;

	/**
	 * 获取前端通知消息
	 */
	@Path(path = "message")
	public void message() {

		String sid = sid();
		long t = this.getLong("t");
		if (t == 0) {
			t = Global.now();
		}

		W q = W.create();
		q.and("sid", sid);
		q.and(X.CREATED, t, W.OP.gt);
		q.sort(X.CREATED, -1);

		if (!_message_optimized) {
			Helper.primary.getOptimizer().query("gi_message", q);
			_message_optimized = true;
		}

//		TimeStamp t1 = TimeStamp.create();
		Beans<Message> bs = Message.dao.load(q, 0, 10);
//		try {
//			while ((bs == null || bs.isEmpty()) && t1.pastms() < 30000) {
//				synchronized (t1) {
//					t1.wait(1000);
//				}
//				bs = Message.dao.load(q, 0, 10);
//			}
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//		}

		if (bs != null && !bs.isEmpty()) {
			t = bs.get(0).getCreated();
		}

		this.set("t", t);
		if (bs != null) {
			this.set(X.LIST, bs.asList(e -> {
				JSON j1 = e.json();
				j1.remove(X.ID, "_id", "updated", X.CREATED, "_node", "sid");
				return j1;
			}));
		}
		this.send(200);

	}

	@Path(path = "captcha")
	public void captcha() {

		String option = Global.getString("user.captcha.option", "");

		if (X.isSame(option, "image")) {
			JSON jo = new JSON();
			Temp t = Temp.create("code.jpg");
			try {

				Captcha.create(this.sid(true), Global.now() + 5 * X.AMINUTE, 200, 60, t.getOutputStream(), 4);

				String filename = "/temp/" + lang.format(Global.now(), "yyyy/MM/dd/HH/mm/") + Global.now() + "_"
						+ UID.random(10) + ".jpg";

				DFile f1 = Disk.seek(filename);
				f1.upload(t.getInputStream());

				jo.put(X.STATE, 200);
				jo.put("sid", sid(false));
				jo.put("uri", "/f/g/" + f1.getId() + "/code.jpg?" + Global.now());

			} catch (Exception e1) {

				log.error(e1.getMessage(), e1);
				GLog.securitylog.error(f.class, "", e1.getMessage(), e1, login, this.ip());

				jo.put(X.STATE, 201);
				jo.put(X.MESSAGE, e1.getMessage());
			}

			this.send(jo);
		} else {

			String name = this.get(X.NAME);
			if (!X.isEmpty(name)) {

				User user = User.load(name);
				if (user != null) {
					ICaptcha c = ICaptcha.get(option);
					if (c.init(user)) {
						this.set(X.MESSAGE, lang.get("captcha." + option + ".sent")).send(200);
					} else {
						this.set(X.MESSAGE, lang.get("captcha." + option + ".failed")).send(201);
					}
				}
			}

		}
	}

	@Path(path = "verify")
	public void verify() {
		String code = this.getString("code").toLowerCase();
		Captcha.Result r = Captcha.verify(this.sid(false), code);

		JSON jo = new JSON();
		if (Captcha.Result.badcode == r) {
			jo.put(X.STATE, 202);
			jo.put(X.MESSAGE, "bad code");
		} else if (Captcha.Result.expired == r) {
			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, "expired");
		} else {
			jo.put(X.STATE, 200);
			jo.put(X.MESSAGE, "ok");
		}

		this.send(jo);

	}

	@Path(path = "menu")
	public void menu() {
		User me = this.user();

		long id = this.getLong("root");
		String name = this.getString(X.NAME);

		Beans<Menu> bs = null;
		Menu m = null;
		if (!X.isEmpty(name)) {
			/**
			 * load the menu by id and name
			 */
			m = Menu.load(id, name);

			if (m != null) {

				/**
				 * load the submenu of the menu
				 */
				bs = m.submenu();
			}
		} else {
			/**
			 * load the submenu by id
			 */
			bs = Menu.submenu(id);

		}
		List<Menu> list = bs;

		/**
		 * filter out the item which no access
		 */
		Collection<Menu> ll = Menu.filterAccess(list, me);

		if (log.isDebugEnabled())
			log.debug("load menu: id=" + id + ", size=" + (list == null ? 0 : list.size()) + ", filtered="
					+ (ll == null ? 0 : ll.size()));

		/**
		 * convert the list to json array
		 */
		List<JSON> arr = new ArrayList<JSON>();

		if (ll != null) {
			Iterator<Menu> it = ll.iterator();

			while (it.hasNext()) {
				JSON jo = new JSON();
				m = it.next();

				/**
				 * set the text width language
				 */
				jo.put("text", lang.get(m.getName()));
				jo.put(X.ID, m.getId());
				if (!X.isEmpty(m.getClasses())) {
					jo.put("classes", m.getClasses());
				}

				if (!X.isEmpty(m.getStyle())) {
					jo.put("style", m.getStyle());
				}

				/**
				 * set the url
				 */
				if (!X.isEmpty(m.getUrl())) {
					jo.put(X.URL, m.getUrl());
				}

				/**
				 * set children
				 */
				if (m.getChilds() > 0) {
					jo.put("hasChildren", true);
				}

				jo.put("seq", m.getSeq());
				jo.put(X.TAG, m.getTag());
				if (!X.isEmpty(m.getLoad1())) {
					jo.put("load", m.getLoad1() + "?__node=" + this.getString("__node"));
				}

				if (!X.isEmpty(m.getClick())) {
					jo.put("click", m.getClick());
				}

				if (!X.isEmpty(m.getContent())) {
					jo.put("content", m.getContent());
				}

				arr.add(jo);
			}
		}

		this.send(arr);
	}

	private static Map<Long, Object[]> _cached = new HashMap<Long, Object[]>();

	@SuppressWarnings("unchecked")
	@Path(path = "console/open", login = true)
	public void console_open() {

		String console = this.get("console");

		if (X.isEmpty(console)) {
			this.set(X.ERROR, "参数错误, [console]").send(201);
			return;
		}

		long tid = Global.now();
		_cached.put(tid, new Object[] { Global.now(), new ArrayList<String>() });

		Console.open(X.split(console, "[, ]"), msg -> {
			Object[] oo = _cached.get(tid);
			if (oo == null) {
				return false;
			}

			List<String> l1 = (List<String>) oo[1];
			if (l1 == null) {
				return false;
			}
			synchronized (l1) {
				if (l1.size() < 1000) {
					l1.add(msg);
				}
				l1.notifyAll();
			}

			return true;
		});

		this.set(X.ID, tid).send(200);

	}

	@Path(path = "console/close", login = true)
	public void console_close() {
		long tid = this.getLong(X.ID);
		_cached.remove(tid);
		this.send(200);
	}

	@SuppressWarnings("unchecked")
	@Path(path = "console/list", login = true)
	public void console_list() {

		long tid = this.getLong(X.ID);
		Object[] oo = _cached.get(tid);
		List<String> l1 = (List<String>) oo[1];
		List<String> l2 = new ArrayList<String>();

		try {
			synchronized (l1) {
				l1.wait(1000);
				if (l1.isEmpty()) {
					l1.wait(10000);
				}
				if (!l1.isEmpty()) {
					l2.addAll(l1);
					l1.clear();
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		this.set(X.LIST, l2).send(200);
	}

	public static void clean() {

		log.warn("cleanup f ...");

		if (_cached.isEmpty())
			return;

		for (Long tid : _cached.keySet().toArray(new Long[_cached.size()])) {
			Object[] o = _cached.get(tid);
			long time = X.toLong(o[0]);
			if (Global.now() - time > X.AHOUR) {
				_cached.remove(tid);
			}
		}
	}

	public static void main(String[] args) {

		String id = "f52gk3lqf5tc6rbpmyxuslzvgu3tkmbqgmztcnztgi3tombsguzdml3hnfuxoyk7gmxdelrsgiydimrqgeztank7ovygo4tbmrss46tjoa";
		String filename = new String(Base32.decode(id));
		System.out.println(filename);

	}

	@Deprecated
	@Path(path = "data/(.*)")
	public void data(String appid) {

		try {

			App a = App.load(appid);
			if (a == null) {
				this.set(X.ERROR, "bad appid [" + appid + "]").send(201);
				return;
			}

			String str = this.getHtml("d");
			JSON param = JSON.fromObject(App.decode(str, a.getSecret()));
			if (param == null || param.isEmpty()) {
				this.set(X.ERROR, "bad d [" + str + "]").send(201);
				return;
			}

			String table = param.getString("table");
			int s = param.getInt(X.S);
			int n = param.getInt(X.N, 10);
			String sql = param.getString("sql");

			W q = SQL.where(sql);
			if (q == null) {
				q = W.create();
			}
			if (X.isIn(table, "gi_user")) {
				if (q.isEmpty()) {
					q.and("id>0");
				} else {
					q = W.create().and(q).and("id>0");
				}
			}
			q.sort(X.CREATED);
			Beans<Data> bs = Helper.primary.load(table, q, s, n, Data.class);
			List<JSON> l1 = bs.asList(d -> {
				JSON j1 = d.json();
				j1.remove("_id", "_node", "password");
				return j1;
			});

			this.set(X.S, s).set(X.N, n).set(X.LIST, App.encode(JSON.toString(l1), a.getSecret())).send(200);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error(f.class, "data", e.getMessage(), e, null, this.ip());
			this.set(X.ERROR, e.getMessage()).send(201);
		}

	}

	public static String mime(InputStream in) throws Exception {

		String head = null;

		try {

			byte[] bb = new byte[16];
			int len = in.read(bb, 0, bb.length);

			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < len; i++) {
				byte b = bb[i];
				String s = Integer.toHexString(b & 0xFF).toUpperCase();
				if (s.length() < 2) {
					sb.append(0);
				}
				sb.append(s);
			}
			head = sb.toString();

		} finally {
			X.close(in);
		}

//		log.info("head=" + head);

		if (head != null) {
			for (int i = head.length(); i >= 4; i -= 2) {
				String s = head.substring(0, i);

				if (_head.containsKey(s)) {
					return _head.get(s);
				}
			}
		}

		return null;
	}

	private static Map<String, String> _head = new HashMap<>();
	static {
		// images
		_head.put("FFD8FF", "image/jpeg");
		_head.put("89504E47", "image/png");
		_head.put("47494638", "image/gif");
		_head.put("49492A00", "image/tif");
		_head.put("424D", "image/bmp");

		_head.put("494433", "audio/mp3");
		_head.put("000001BA", "video/mpg");
		_head.put("000001B3", "video/mpg");
		_head.put("0000001C66747970", "video/mp4");
		_head.put("0000002066747970", "video/mp4");

	}

	@Path(path = "release/notes", login = true)
	public void release_notes() {

		// TODO

	}

	private static DFile _get(String id, String name) throws IOException {

		String filename = _path(id, name);
		DFile f1 = Disk.seek(filename);
		if (f1 != null) {
			f1.limit(Global.getString("f.upload.path", "/temp"));
		}
		return f1;

	}

	private static String _path(String path, String name) {

		long id = Math.abs(UID.hash(path));
		char p1 = (char) (id % 23 + 'a');
		char p2 = (char) (id % 19 + 'A');
		char p3 = (char) (id % 17 + 'a');
		char p4 = (char) (id % 13 + 'A');

		StringBuilder sb = new StringBuilder(Global.getString("f.upload.path", "/temp"));

		sb.append("/").append(p1).append("/").append(p2).append("/").append(p3).append("/").append(p4).append("/")
				.append(id);

		if (name != null) {
			sb.append("/").append(name);
		}

		return sb.toString();
	}

	@Path(path = "pubkey", memo = """
			获取系统RSA公钥，供客户端进行数据加密；同时返回服务端系统时钟，用来和客户端时钟做比对，时间敏感的加密场景可直接使用该服务端时间。
			""", out = """
			state：200 正常，否则错误;
			key：公钥；
			time：系统时钟，用于比对客户端时钟，对时间敏感的加密，可以直接使用这个时间。
			""")
	public void pubkey() {

		this.set("key",
				"MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCNom9BdQZ4z6YyiqijBpR3LsG9Q2Pqd3KyMX/zzBMrDbe5gEKiocHB2R86pH6TiU6LXxK4BRF7RtYrtiw5scgNs2xjBJi7pTQzKqHF04jkyjtwbCnc5edkUFcez3awHVX0ntBphVd07CwLJVgKHUdEb4UClluqGv3ocXe6Of5c/QIDAQAB");
		this.set("time", Global.now());
		this.send(200);

	}

}