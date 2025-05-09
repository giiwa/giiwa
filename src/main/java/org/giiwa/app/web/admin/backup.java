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
package org.giiwa.app.web.admin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.giiwa.app.task.BackupTask;
import org.giiwa.bean.AutoBackup;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper;
import org.giiwa.dao.MongoHelper;
import org.giiwa.dao.Schema;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.misc.Exporter;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Url;
import org.giiwa.net.client.FTP;
import org.giiwa.net.client.SFTP;
import org.giiwa.task.Monitor;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Language;
import org.giiwa.web.Path;

/**
 * backup management,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class backup extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	@Override
	public void onGet() {

		try {
//			DFile root = Disk.seek(BackupTask.ROOT);

			List<DFile> list = new ArrayList<DFile>();
			Collection<DFile> fs = Disk.list(_BackupTask.ROOT);// root.listFiles();
			if (fs != null) {
				for (DFile f : fs) {
					if (f.isFile()) {
						list.add(f);
					}
				}
			}

			Collections.sort(list, new Comparator<DFile>() {

				@Override
				public int compare(DFile o1, DFile o2) {
					String n1 = o1.getName();
					String n2 = o2.getName();
					return n2.compareToIgnoreCase(n1);
				}

			});

			this.set("root", _BackupTask.ROOT);
			this.set("list", list);

			this.show("/admin/backup.index.html");

		} catch (Exception e) {
			this.error(e);
			return;
		}

	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin", oplog = true)
	public void delete() {

		try {
			String name = this.getString("name");
			DFile f = Disk.seek(_BackupTask.ROOT + "/" + name);

			if (log.isDebugEnabled()) {
				log.debug("delete: " + f.getFilename());
			}

			f.delete();
			this.set(X.MESSAGE, lang.get("delete.success")).send(201);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.oplog.error(this, "delete", e.getMessage(), e);
			this.set(X.MESSAGE, e.getMessage()).send(201);
		}

	}

	@Path(path = "upload", login = true, access = "access.config.admin", oplog = true)
	public void upload() {

		String repo = this.getString("repo");

//		if (repo.startsWith("/f/g/")) {
//			repo = repo.substring(4);
//		}
//
//		Entity e = Repo.load(repo);

		DFile f = null;

		try {

			f = Disk.seek(repo);

			DFile f1 = Disk.seek(_BackupTask.ROOT + "/" + f.getName());

			if (f1.exists()) {
				f1.delete();
			} else {
				f1.getParentFile().mkdirs();
			}
			IOUtil.copy(f.getInputStream(), f1.getOutputStream());

			this.send(200);
			return;
		} catch (Exception e1) {

			log.error(e1.getMessage(), e1);
			GLog.oplog.error(this, "upload", e1.getMessage(), e1);

			this.set(X.MESSAGE, e1.getMessage()).send(201);
			return;
		} finally {
			if (f != null)
				f.delete();
		}

	}

	@Path(path = "create", login = true, access = "access.config.admin", oplog = true)
	public void create() {

		// GLog.applog.info(backup.class, "create", "method=" + method, login,
		// this.getRemoteHost());

		if (method.isPost()) {

			String[] ss = this.getStrings("name");
			if (ss != null && ss.length > 0) {

				new _BackupTask(ss, Global.getString("backup.url", null)).schedule(0);
				this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("backup.started")));

			} else {

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("backup.error.notable")));
			}
			return;
		}

		// TODO
//		List<JSON> l2 = Cache.get("db.schema");
//		List<Class<? extends Bean>> l1 = Schema.beans;
//		Map<String, JSON> l2 = new TreeMap<String, JSON>();
//		for (Class<? extends Bean> c : l1) {
//			try {
//				String table = Helper.getTable(c);
//				if (!X.isEmpty(table) && !l2.containsKey(table)) {
//					JSON j = JSON.create().append("name", c.getName()).append("table", table).append("size",
//							Helper.count(table, W.create()));
//					l2.put(table, j);
//				}
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		}
//		this.set("list", l2.values());

		List<JSON> l2 = Schema.load(lang);
		this.set("list", l2);

		this.show("/admin/backup.create.html");

	}

	@SuppressWarnings("unused")
	@Path(path = "er", login = true, access = "access.config.admin")
	public void er() {

		// GLog.applog.info(backup.class, "create", "method=" + method, login,
		// this.getRemoteHost());

		if (method.isPost()) {
			String[] ss = this.getStrings("name");
			if (ss != null && ss.length > 0) {

				new _BackupTask(ss, null).schedule(0);
				Temp t = Temp.create("er.csv");
				Exporter<Bean> ex = null;

				// TODO
				try {

					ex = Exporter.create(t.getOutputStream(), Exporter.FORMAT.csv, true);

					for (String s : ss) {

						Class<? extends Bean> c = null;// Schema.bean(s);
						if (c == null)
							continue;

						Map<String, Class<?>> st = new TreeMap<String, Class<?>>();
						Beans<Bean> bs = Helper.primary.load(s, W.create().sort("created", -1), 0, 10, Bean.class);
						for (Bean b : bs) {
							Map<String, Object> m = b.getAll();
							for (String name : m.keySet()) {
								Class<?> c1 = m.get(name).getClass();
								Class<?> c2 = st.get(name);
								if (c2 == null) {
									st.put(name, c1);
								} else if (!X.isSame(c1, c2)) {
									st.put(name, Object.class);
								}
							}
						}

						ex.print(new String[] { "" });
						ex.print(new String[] { s });
						ex.print(new String[] { lang.get("name." + c.getName()) });
						ex.print(new String[] { "Field", "Type", "Memo" });
						for (String s1 : st.keySet()) {
							Class<?> c1 = st.get(s1);
							String t1 = "text";
							if (c1.equals(Integer.class)) {
								t1 = "int";
							} else if (c1.equals(Long.class)) {
								t1 = "bigint";
							} else if (c1.equals(Float.class)) {
								t1 = "float";
							} else if (c1.equals(Double.class)) {
								t1 = "double";
							} else if (c1.isArray()) {
								t1 = "list";
							}
							ex.print(new String[] { s1, t1 });
						}
					}
				} catch (Exception e1) {
					log.error(e1.getMessage(), e1);
				} finally {
					X.close(ex);
				}

				this.send(JSON.create().append(X.STATE, 200).append("file", t.getUri(lang)));

			} else {

				this.send(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("nonselect.error")));
			}
			return;
		}

		Schema.init();

		List<Class<? extends Bean>> l1 = Schema.beans;
		Map<String, JSON> l2 = new TreeMap<String, JSON>();
		for (Class<? extends Bean> c : l1) {

			try {
				String table = Helper.getTable(c);
				if (!X.isEmpty(table) && !l2.containsKey(table)) {
					JSON j = JSON.create().append("name", c.getName()).append("table", table).append("size",
							Helper.primary.count(table, W.create()));
					l2.put(table, j);
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		this.set("list", l2.values());
		this.show("/admin/backup.er.html");

	}

	@Path(path = "auto", login = true, access = "access.config.admin")
	public void auto() {

		String name = this.get("name");
		W q = W.create();
		if (!X.isEmpty(name)) {
			q.and("name", name, W.OP.like);
			this.put("name", name);
		}

		int s = this.getInt("s");
		int n = this.getInt("n", X.ITEMS_PER_PAGE);

		Beans<AutoBackup> bs = AutoBackup.dao.load(q, s, n);
		if (bs != null) {
			bs.count();
		}
		this.pages(bs, s, n);

		this.show("/admin/backup.auto.html");

	}

	@Path(path = "auto/innertask", login = true, access = "access.config.admin")
	public void auto_innertask() {

		List<JSON> l2 = Schema.load(lang);
		this.set("list", l2);
		this.show("/admin/backup.auto.innertask.html");

	}

	@Path(path = "auto/create", login = true, access = "access.config.admin", oplog = true)
	public void auto_create() {

		if (method.isPost()) {

			String[] ss = this.getStrings("table");
			String name = X.join(ss, ",");

			V v = V.create();
			v.append("table", name);

			v.append("enabled", 1);
			v.append("time", this.get("time"));

			ss = this.getStrings("days");
			String days = X.join(ss, ",");
			v.append("days", days);

//			v.append("months", this.get("months"));
//			v.append("dates", this.get("dates"));
			v.append("type", this.getInt("type"));
			v.append("nodes", this.getString("nodes"));
			v.append("command", this.getString("command"));

			String url = this.getHtml("url");
			if (url != null) {
				url = url.trim();
			}
			v.append("url", url);
			v.append("name", this.get("name"));
			v.append("clean", X.isSame("on", this.getString("clean")) ? 1 : 0);
			v.append("keeps", this.get("keeps"));

			long id = AutoBackup.create(v);
			AutoBackup a = AutoBackup.dao.load(id);
			a.next(null);

			BackupTask.init();

			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;

		}

		this.show("/admin/backup.auto.create.html");

	}

	@Path(path = "auto/delete", login = true, access = "access.config.admin", oplog = true)
	public void auto_delete() {

		long id = this.getLong("id");

		AutoBackup.dao.delete(id);

		this.send(200);

	}

	@Path(path = "auto/edit", login = true, access = "access.config.admin", oplog = true)
	public void auto_edit() {

		long id = this.getLong("id");

		if (method.isPost()) {

			String[] ss = this.getStrings("table");
			String name = X.join(ss, ",");

			V v = V.create();
			v.append("table", name);
			v.append("enabled", X.isSame("on", this.get("enabled")) ? 1 : 0);
			v.append("time", this.get("time"));
			v.append("state", 0);

			ss = this.getStrings("days");
			String days = X.join(ss, ",");
			v.append("days", days);

//			v.append("months", this.get("months"));
//			v.append("dates", this.get("dates"));
//			v.append("type", this.getInt("type"));
			v.append("nodes", this.getString("nodes"));
			v.append("command", this.getString("command"));

			String url = this.getHtml("url");
			if (url != null) {
				url = url.trim();
			}
			v.append("url", url);
			v.append("name", this.get("name"));
			v.append("clean", X.isSame("on", this.get("clean")) ? 1 : 0);
			v.append("keeps", this.get("keeps"));

			AutoBackup.dao.update(id, v);
			AutoBackup a = AutoBackup.dao.load(id);
			a.next(null);

			BackupTask.init();

			this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("save.success")));
			return;
		}

		AutoBackup a = AutoBackup.dao.load(id);
		this.set("a", a);

		if (a.type != 2) {
			// 不是外部命令
			List<JSON> l2 = Schema.load(lang);
			this.set("list", l2);
		}

		this.show("/admin/backup.auto.edit.html");

	}

	private static _RecoverTask rtask = null;

	/**
	 * Restore.
	 */
	@Path(path = "restore", login = true, access = "access.config.admin", oplog = true)
	public synchronized void restore() {

		JSON jo = new JSON();

		String name = this.getString("name");
		if (rtask == null || rtask.finished) {
			rtask = new _RecoverTask(name);
			try {
				long id = Monitor.start(rtask, 10);
				jo.put("id", id);
			} catch (Exception e) {
				this.error(e);
				return;
			}
		}

		this.send(jo);
	}

	/**
	 * backup.
	 * 
	 * @author joe
	 *
	 */
	public static class _BackupTask extends Task {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static String ROOT = "/backup/";

		String[] cc; // collections
		String url;

		public _BackupTask(String[] l1, String url) {
			cc = l1;
			this.url = url;
		}

		@Override
		public String getName() {
			return "backup.task";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.core.task.Task.onExecute()
		 */
		@Override
		public void onExecute() {
			// Module m = Module.home;
			String name = Language.getLanguage().format(Global.now(), "yyyyMMddHHmm") + ".zip";

			try {

				Global.setConfig("backup/" + name, 1); // starting backup

				Temp t = Temp.create(name);

				ZipOutputStream out = t.getZipOutputStream();

				try {
					/**
					 * 1, backup db
					 */
//					if (MongoHelper.inst.isConfigured()) {
					Global.setConfig("backup/" + name, 2); // backup mongo
					MongoHelper.inst.backup(out, cc);
//					}
					if (log.isDebugEnabled())
						log.debug("zipping, dir=" + out);

					out.close();
				} finally {
					X.close(out);
				}

				Disk.seek(ROOT + name).upload(t.getInputStream());

				Global.setConfig("backup/" + name, 100); // done

				if (!X.isEmpty(url)) {
					// store in other
					if (url.startsWith("ftp://")) {

						Url u = Url.create(url);
						FTP f1 = FTP.create(u);
						if (f1 != null) {
							InputStream in = t.getInputStream();
							try {
								if (log.isDebugEnabled())
									log.debug("ftp put, filename=" + u.get("path") + "/" + name);

								f1.put(u.get("path") + "/" + name, in);

								GLog.applog.info("backup", "auto", "backup success, name=" + name + ".zip", null, null);
							} finally {
								X.close(in);
								f1.close();
							}
						}

					} else if (url.startsWith("sftp://")) {

						InputStream in = t.getInputStream();
						SFTP s1 = null;
						try {
							Url u = Url.create(url);
							s1 = SFTP.create(u);
							s1.put(u.get("path") + "/" + name, in);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						} finally {
							X.close(in, s1);
						}

					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.applog.error("backup", "backup", e.getMessage(), e);

				Global.setConfig("backup/" + name, -1); // error

			}
		}

		public static void clean(int days) {

			try {
				DFile f = Disk.seek(_BackupTask.ROOT);
				if (f.exists()) {
					DFile[] ff = f.listFiles();
					if (ff != null && ff.length > 0) {
						for (DFile f1 : ff) {
							if (f1.lastModified() < Global.now() - days * X.ADAY) {
								f1.delete();
							}
						}
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

	};

	public static class _RecoverTask extends Task {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		long tid;
		String name;
		boolean done;
		String message;
		int state = 201;
		boolean finished = false;

		public _RecoverTask(String name) {
			this.name = name;
			message = "正在恢复：" + name;
		}

		@Override
		public String getName() {
			return "recover.task";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.core.task.Task.onExecute()
		 */
		@Override
		public void onExecute() {
			// Module m = Module.home;

			try {
				Global.setConfig("backup/" + name, "正在恢复"); // recovering

				DFile f = Disk.seek(_BackupTask.ROOT + name);

				ZipInputStream in = new ZipInputStream(f.getInputStream());

				/**
				 * 1, recover if configured
				 */
				ZipEntry e = in.getNextEntry();
				while (e != null) {
					String name = e.getName();
//					if (X.isSame(name, "mongo.db") && MongoHelper.inst.isConfigured()) {
					if (X.isSame(name, "mongo.db")) {
						Global.setConfig("backup/" + name, "正在恢复 mongo"); // recovering
						MongoHelper.inst.recover(in);
					}
					e = in.getNextEntry();
				}

				/**
				 * 2, recover repo, TODO
				 */

				Global.setConfig("backup/" + name, "完成恢复"); // recovering

			} catch (Exception e) {
				log.error(e.getMessage(), e);
				message = e.getMessage();
				GLog.applog.error("backup", "recover", e.getMessage(), e);

				Global.setConfig("backup/" + name, "恢复错误，error=" + e.getMessage()); // recovering

			}

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.core.task.Task.onFinish()
		 */
		@Override
		public void onFinish() {
			done = true;
			state = 200;
			message = "已经恢复：" + name;
			Monitor.flush(this);
			finished = true;
		}

	}
}
