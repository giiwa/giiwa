package org.giiwa.app.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.giiwa.bean.App;
import org.giiwa.bean.Data;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Menu;
import org.giiwa.bean.Temp;
import org.giiwa.bean.User;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Counter;
import org.giiwa.dao.Helper;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.SQL;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.Base32;
import org.giiwa.misc.Captcha;
import org.giiwa.misc.GImage;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Url;
import org.giiwa.task.Console;
import org.giiwa.task.Monitor;
import org.giiwa.web.Controller;
import org.giiwa.web.Path;

public class f extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Counter get = new Counter("g");
	private static Counter down = new Counter("d");

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
	@Path(path = "g/(.*)/(.*)")
	public void g(String id, String name) {

		TimeStamp t = TimeStamp.create();

		try {

			if (Global.getInt("f.g.login", 0) == 1) {
				if (this.user() == null) {
					this.set(X.ERROR, lang.get("login.required")).send(201);
					return;
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

			if (f1.isFile()) {

				// GLog.applog.info(f.class, "get", f1.getFilename());

				String mime = Controller.getMimeType(f1.getName());
				if (X.isEmpty(mime)) {
					mime = Controller.getMimeType(name);
				}

				if (log.isDebugEnabled()) {
					log.debug("mime=" + mime + ", name=" + name);
				}

				if (mime != null && !X.isIn(mime, "image/x-icon", "image/gif") && mime.startsWith("image/")
						&& !f1.getName().endsWith(".webp")) {

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

								Temp t2 = Temp.create("a.png");
								long len = IOUtil.copy(f1.getInputStream(), t2.getOutputStream());
								if (len != f1.length())
									throw new IOException("get dfile error");

								GImage.scale1(t2.getInputStream(), t1.getOutputStream(), X.toInt(ss[0]),
										X.toInt(ss[1]));
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

								return;
							} else if (log.isWarnEnabled()) {
								log.debug("the file not exists? " + f.getFilename());
							}
						}
					} else if (X.isIn(mime, "image/tiff")) {
						// TIFF
						Temp t1 = Temp.create(id, "s_" + size);
						DFile f = t1.getDFile();
						if (!f.exists() || f.length() == 0) {

							Temp t2 = Temp.create("a.png");
							long len = IOUtil.copy(f1.getInputStream(), t2.getOutputStream());
							if (len != f1.length()) {
								throw new IOException("get dfile error");
							}

							GImage.scale1(t2.getInputStream(), t1.getOutputStream());
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

							return;
						} else if (log.isWarnEnabled()) {
							log.debug("the file not exists? " + f.getFilename());
						}

					}
				}

				this.setContentType(mime);

				_send(f1.getInputStream(), f1.length());

			} else {
				log.info("not found, [" + f1.getFilename() + "]");
				this.set(X.ERROR, "not found, file=" + f1.getFilename()).send(201);
			}

		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			this.set(X.ERROR, e1.getMessage()).send(201);
		} finally {
			get.add(t.pastms());
		}

	}

	@Path(path = "s/(.*)")
	public void s(String filename) {

		TimeStamp t = TimeStamp.create();

		try {

			if (Global.getInt("f.g.login", 0) == 1) {
				if (this.user() == null) {
					this.set(X.ERROR, lang.get("login.required")).send(201);
					return;
				}
			}

			if (Global.getInt("f.g.online", 1) != 1) {
				this.set(X.ERROR, lang.get("access.deny")).send(201);
				return;
			}

			if (!filename.startsWith("/")) {
				filename = "/" + filename;
			}

			DFile f1 = Disk.seek(filename);
			if (f1 == null) {
				log.warn("bad filename=" + filename);
				return;
			}

			if (f1.isFile()) {

				// GLog.applog.info(f.class, "get", f1.getFilename());

				String mime = Controller.getMimeType(f1.getName());
				if (X.isEmpty(mime)) {
					mime = Controller.getMimeType(filename);
				}

				if (log.isDebugEnabled())
					log.debug("mime=" + mime + ", name=" + filename);

				this.setContentType(mime);

				_send(f1.getInputStream(), f1.length());

			} else {
				log.info("not found, file=" + f1.getFilename());
				this.set(X.ERROR, "not found, file=" + f1.getFilename()).send(201);
			}

		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			this.set(X.ERROR, e1.getMessage()).send(201);
		} finally {
			get.add(t.pastms());
		}

	}

	/**
	 * download file
	 * 
	 * @param id
	 * @param name
	 */
	@Path(path = "d/(.*)/(.*)")
	public void d(String id, String name) {

		TimeStamp t = TimeStamp.create();

		try {
			if (Global.getInt("f.g.login", 0) == 1) {
				if (this.user() == null) {
					this.set(X.ERROR, lang.get("login.required")).send(201);
					return;
				}
			}

			DFile f1 = Disk.get(id);
			if (f1.isFile()) {

				// GLog.applog.info(f.class, "download", f1.getFilename());
				this.send(f1.getName(), f1.getInputStream());// , f1.length());

			}
		} catch (Exception e) {
			this.error(e);
		} finally {
			down.add(t.pastms());
		}

	}

	/**
	 * upload file
	 */
	@Path(path = "upload")
	public void upload() {

		if (Global.getInt("f.upload.login", 1) == 1) {
			if (this.user() == null) {
				// 需要登录
				this.set(X.ERROR, lang.get("login.required")).send(201);
				return;
			}
		}

		String path = this.get("path");

		if (log.isDebugEnabled()) {
			log.debug("json=" + this.json());
		}

		JSON jo = new JSON();

		// String access = Module.home.get("upload.require.access");

		FileItem file = this.file("file");
		if (file != null) {
			String filename = this.getString("filename");
			if (X.isEmpty(filename)) {
				filename = file.getName();
			}

			filename = filename.replaceAll("[ *$?-]", "_");
			if (filename.length() > 128) {
				int i = filename.lastIndexOf(".");
				if (i > 0) {
					filename = filename.substring(0, 100) + "_" + UID.id(filename) + filename.substring(i);
				} else {
					filename = filename.substring(0, 100) + "_" + UID.id(filename);
				}
			}
			_store(file, path, filename, jo);

		} else {
			jo.append(X.STATE, HttpServletResponse.SC_BAD_REQUEST).append(X.ERROR, HttpServletResponse.SC_BAD_REQUEST)
					.append(X.MESSAGE, lang.get("upload.notfound"));
		}

		// /**
		// * test
		// */
		// jo.put("error", "error");
		this.send(jo.append("path", path).append("node", Local.label()));

	}

	private boolean _store(FileItem file, String path, String filename, JSON jo) {
//		String tag = this.getString("tag");

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

			String id = UID.id(filename, total, lastModified);

			if (log.isDebugEnabled()) {
				log.debug("storing, id=" + id + ", path=" + path + ", filename=" + filename + ", position=" + position
						+ ", total=" + total + ", last=" + lastModified);
			}

			DFile f1 = Temp.get(id, filename);

			try {
				if (!f1.exists() || f1.length() == position) {
					f1.upload(position, file.getInputStream());
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
				if (jo == null) {
					this.put("url", url);
					this.put(X.ERROR, 0);
					this.put("repo", url);
					if (total > 0) {
						this.put("name", filename);
						this.put("pos", pos);
						this.put("size", total);
					}
				} else {
					jo.put("url", url);
					jo.put("repo", url);
					jo.put(X.ERROR, 0);
					jo.put("name", filename);
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

			GLog.oplog.error(f.class, "upload", filename, e, login, this.ip());

			if (jo == null) {
				this.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				this.put(X.MESSAGE, e.getMessage());
				this.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			} else {
				jo.put(X.ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
				jo.put(X.MESSAGE, e.getMessage());
				jo.put(X.STATE, HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			}
		}

		return false;
	}

	/**
	 * get temp file
	 */
	@SuppressWarnings("resource")
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
			GLog.oplog.error(f.class, "temp", e.getMessage(), e, login, this.ip());
		}

		GLog.oplog.warn(f.class, "temp", "not found, " + (f1 == null ? "null" : f1.getAbsolutePath()), login,
				this.ip());

		this.notfound(name);

	}

	@Path(path = "alive")
	public void alive() {
		this.print("ok");
	}

	@Path(path = "echo")
	public void echo() {
		StringBuilder sb = new StringBuilder();
		for (NameValue s : this.heads()) {
			sb.append(s.name).append("=").append(s.value).append("<br>");
		}

		this.print(sb.toString());
	}

	void _send(InputStream in, long total) {

		try {

			String range = this.head("range");

			if (log.isDebugEnabled()) {
				log.debug("range=" + range);
			}

//			long start = 0;
//			long end = total;
//			if (!X.isEmpty(range)) {
//				String[] ss = range.split("(=|-)");
//				if (ss.length > 1) {
//					start = X.toLong(ss[1]);
//				}
//
//				if (ss.length > 2) {
//					end = Math.min(total, X.toLong(ss[2]));
//				}
//			}
//
//			if (end <= start) {
//				end = start + 1024 * 1024;
//			}
//
//			if (end > total) {
//				end = total;
//			}
//
//			long length = end - start;
//
//			if (end < total || start > 0) {
//				this.status(206);
//			}
//
//			if (start == 0) {
//				this.head("Accept-Ranges", "bytes");
//			}
//			this.head("Content-Length", Long.toString(length));
//			this.head("Content-Range", "bytes " + start + "-" + (end - 1) + "/" + total);
//
//			log.info("response.stream, bytes " + start + "-" + (end - 1) + "/" + total);
//			if (length > 0) {

			OutputStream out = this.getOutputStream();
			IOUtil.copy(in, out, false);
			out.flush();

//			}
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

		long id = this.getLong("id", -1);
		String access = this.get("access");
		if (X.isEmpty(access)) {
			access = X.EMPTY;
		}

		JSON jo = Monitor.get(id, access);
		int state = jo == null ? 0 : jo.getInt("state");
		this.send(JSON.create().append(X.STATE, state > 0 && state != 200 ? 201 : 200).append("data", jo));
//				.append("params", req.getParameterMap()).append("ct", req.getContentType())
//				.append("url", req.getRequestURI()));

	}

	@Path(path = "captcha")
	public void captcha() {

		JSON jo = new JSON();
		Temp t = Temp.create("code.jpg");
		try {

			Captcha.create(this.sid(true), System.currentTimeMillis() + 5 * X.AMINUTE, 200, 60, t.getOutputStream(), 4);

			String filename = "/temp/" + lang.format(System.currentTimeMillis(), "yyyy/MM/dd/HH/mm/")
					+ System.currentTimeMillis() + "_" + UID.random(10) + ".jpg";

			DFile f1 = Disk.seek(filename);
			f1.upload(t.getInputStream());

			jo.put(X.STATE, 200);
			jo.put("sid", sid(false));
			jo.put("uri", "/f/g/" + f1.getId() + "/code.jpg?" + System.currentTimeMillis());

		} catch (Exception e1) {

			log.error(e1.getMessage(), e1);
			GLog.securitylog.error(f.class, "", e1.getMessage(), e1, login, this.ip());

			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, e1.getMessage());
		}

		this.send(jo);
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
		String name = this.getString("name");

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
				jo.put("id", m.getId());
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
				jo.put("tag", m.getTag());
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

		long tid = System.currentTimeMillis();
		_cached.put(tid, new Object[] { System.currentTimeMillis(), new ArrayList<String>() });

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

		this.set("id", tid).send(200);

	}

	@Path(path = "console/close", login = true)
	public void console_close() {
		long tid = this.getLong("id");
		_cached.remove(tid);
		this.send(200);
	}

	@SuppressWarnings("unchecked")
	@Path(path = "console/list", login = true)
	public void console_list() {

		long tid = this.getLong("id");
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

		this.set("list", l2).send(200);
	}

	public static void clean() {

		log.warn("cleanup f ...");

		if (_cached.isEmpty())
			return;

		for (Long tid : _cached.keySet().toArray(new Long[_cached.size()])) {
			Object[] o = _cached.get(tid);
			long time = X.toLong(o[0]);
			if (System.currentTimeMillis() - time > X.AHOUR) {
				_cached.remove(tid);
			}
		}
	}

	public static void main(String[] args) {

		String id = "f52gk3lqf5tc6rbpmyxuslzvgu3tkmbqgmztcnztgi3tombsguzdml3hnfuxoyk7gmxdelrsgiydimrqgeztank7ovygo4tbmrss46tjoa";
		String filename = new String(Base32.decode(id));
		System.out.println(filename);

	}

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
			int s = param.getInt("s");
			int n = param.getInt("n", 10);
			String sql = param.getString("sql");

			W q = SQL.where2W(sql);
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
			q.sort("created");
			Beans<Data> bs = Helper.load(table, q, s, n, Data.class);
			List<JSON> l1 = bs.asList(d -> {
				JSON j1 = d.json();
				j1.remove("_id", "_node", "password");
				return j1;
			});

			this.set("s", s).set("n", n).set("list", App.encode(JSON.toString(l1), a.getSecret())).send(200);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error(f.class, "data", e.getMessage(), e, null, this.ip());
			this.set(X.ERROR, e.getMessage()).send(201);
		}

	}

}