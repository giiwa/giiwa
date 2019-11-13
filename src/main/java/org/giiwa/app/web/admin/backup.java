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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.giiwa.app.task.CleanupTask;
import org.giiwa.core.base.Exporter;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.base.Url;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.MongoHelper;
import org.giiwa.core.bean.RDSHelper;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Global;
import org.giiwa.core.dfile.DFile;
import org.giiwa.core.json.JSON;
import org.giiwa.core.net.FTP;
import org.giiwa.core.net.SFTP;
import org.giiwa.core.task.Monitor;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Disk;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.bean.Repo.Entity;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Language;
import org.giiwa.framework.web.Controller;
import org.giiwa.framework.web.Path;

/**
 * backup management,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class backup extends Controller {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	@Override
	public void onGet() {
		DFile root = Disk.seek(BackupTask.ROOT);

		List<DFile> list = new ArrayList<DFile>();
		try {
			DFile[] fs = root.listFiles();
			if (fs != null) {
				for (DFile f : fs) {
					if (f.isFile()) {
						list.add(f);
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		Collections.sort(list, new Comparator<DFile>() {

			@Override
			public int compare(DFile o1, DFile o2) {
				String n1 = o1.getName();
				String n2 = o2.getName();
				return n2.compareToIgnoreCase(n1);
			}

		});
		this.set("list", list);
		this.query.path("/admin/backup");

		this.show("/admin/backup.index.html");

	}

	/**
	 * Delete.
	 */
	@Path(path = "delete", login = true, access = "access.config.admin")
	public void delete() {

		try {
			String name = this.getString("name");
			DFile f = Disk.seek(BackupTask.ROOT + "/" + name);

			if (log.isDebugEnabled())
				log.debug("delete: " + f.getCanonicalPath());
			f.delete();

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.oplog.error(backup.class, "delete", e.getMessage(), e, login, this.getRemoteHost());
		}

	}

	@Path(path = "download", login = true, access = "access.config.admin")
	public void download() {
		JSON jo = JSON.create();
		try {
			String name = this.getString("name");
			DFile f = Disk.seek(BackupTask.ROOT + "/" + name);

			jo.put(X.STATE, 200);
			jo.put("url", f.getFilename());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.oplog.error(backup.class, "delete", e.getMessage(), e, login, this.getRemoteHost());
			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, e.getMessage());
		}
		this.response(jo);

	}

	@Path(path = "upload", login = true, access = "access.config.admin")
	public void upload() {
		String repo = this.getString("repo");

		Entity e = Repo.load(repo);

		JSON jo = JSON.create();
		try {
			DFile f = Disk.seek(BackupTask.ROOT + "/" + e.getName());

			if (f.exists()) {
				f.delete();
			} else {
				f.getParentFile().mkdirs();
			}
			IOUtil.copy(e.getInputStream(), f.getOutputStream());

			jo.put(X.STATE, 200);
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
			GLog.oplog.error(backup.class, "upload", e1.getMessage(), e1, login, this.getRemoteHost());
			jo.put(X.STATE, 201);
			jo.put(X.MESSAGE, e1.getMessage());
		} finally {
			e.delete();
		}

		this.response(jo);

	}

	@Path(path = "create", login = true, access = "access.config.admin")
	public void create() {

		// GLog.applog.info(backup.class, "create", "method=" + method, login,
		// this.getRemoteHost());

		if (method.isPost()) {
			String[] ss = this.getStrings("name");
			if (ss != null && ss.length > 0) {

				new BackupTask(ss, Global.getString("backup.url", null)).schedule(0);
				this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, lang.get("backup.started")));

			} else {

				this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("backup.error.notable")));
			}
			return;
		}

		List<Class<? extends Bean>> l1 = CleanupTask.beans;
		Map<String, JSON> l2 = new TreeMap<String, JSON>();
		for (Class<? extends Bean> c : l1) {
			String table = Helper.getTable(c);
			if (!X.isEmpty(table) && !l2.containsKey(table)) {
				JSON j = JSON.create().append("name", c.getName()).append("table", table).append("size",
						Helper.count(W.create(), c));
				l2.put(table, j);
			}
		}
		this.set("list", l2.values());
		this.show("/admin/backup.create.html");

	}

	@Path(path = "er", login = true, access = "access.config.admin")
	public void er() {

		// GLog.applog.info(backup.class, "create", "method=" + method, login,
		// this.getRemoteHost());

		if (method.isPost()) {
			String[] ss = this.getStrings("name");
			if (ss != null && ss.length > 0) {
				new BackupTask(ss, null).schedule(0);
				Temp t = Temp.create("er.csv");
				Exporter<Bean> e = t.export("GBK", Exporter.FORMAT.csv);

				for (String s : ss) {

					Class<? extends Bean> c = _getBean(s);
					if (c == null)
						continue;

					Map<String, Class<?>> st = new TreeMap<String, Class<?>>();
					Beans<Bean> bs = Helper.load(s, W.create().sort("created", -1), 0, 10, Bean.class, Helper.DEFAULT);
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

					// TODO
					try {
						e.print(new String[] { "" });
						e.print(new String[] { s });
						e.print(new String[] { lang.get("name." + c.getName()) });
						e.print(new String[] { "Field", "Type", "Memo" });
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
							e.print(new String[] { s1, t1 });
						}
					} catch (Exception e1) {
						log.error(e1.getMessage(), e1);
					}
				}
				e.close();

				this.response(JSON.create().append(X.STATE, 200).append("file", t.getUri()));

			} else {

				this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("nonselect.error")));
			}
			return;
		}

		List<Class<? extends Bean>> l1 = CleanupTask.beans;
		Map<String, JSON> l2 = new TreeMap<String, JSON>();
		for (Class<? extends Bean> c : l1) {
			String table = Helper.getTable(c);
			if (!X.isEmpty(table) && !l2.containsKey(table)) {
				JSON j = JSON.create().append("name", c.getName()).append("table", table).append("size",
						Helper.count(W.create(), c));
				l2.put(table, j);
			}
		}
		this.set("list", l2.values());
		this.show("/admin/backup.er.html");

	}

	private Class<? extends Bean> _getBean(String table) {
		List<Class<? extends Bean>> l1 = CleanupTask.beans;
		for (Class<? extends Bean> c : l1) {
			if (X.isSame(table, Helper.getTable(c))) {
				return c;
			}
		}
		return null;
	}

	@Path(path = "auto", login = true, access = "access.config.admin")
	public void auto() {

		if (method.isPost()) {
			Global.setConfig("backup.auto", X.isSame("on", this.getString("backup.auto")) ? 1 : 0);
			Global.setConfig("backup.point", this.getString("backup.point"));
			Global.setConfig("backup.url", this.getString("backup.url"));

			Global.setConfig("backup.clean", X.isSame("on", this.getString("backup.clean")) ? 1 : 0);
			Global.setConfig("backup.keep.days", this.getInt("backup.keep.days"));

			org.giiwa.app.task.BackupTask.init();

			this.response(JSON.create().append(X.STATE, 201).append(X.MESSAGE, lang.get("save.success")));
			return;
		}

		this.show("/admin/backup.auto.html");

	}

	private static RecoverTask rtask = null;

	/**
	 * Restore.
	 */
	@Path(path = "restore", login = true, access = "access.config.admin")
	public synchronized void restore() {

		JSON jo = new JSON();

		String name = this.getString("name");
		if (rtask == null || rtask.finished) {
			rtask = new RecoverTask(name);
			long id = Monitor.start(rtask, 10);
			jo.put("id", id);
		}

		this.response(jo);
	}

	/**
	 * backup.
	 * 
	 * @author joe
	 *
	 */
	public static class BackupTask extends Task {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static String ROOT = "/_backup/";

		String[] cc; // collections
		String url;

		public BackupTask(String[] l1, String url) {
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
			String name = Language.getLanguage().format(System.currentTimeMillis(), "yyyyMMddHHmm") + ".zip";

			try {

				Global.setConfig("backup/" + name, 1); // starting backup

				Temp t = Temp.create(name);

				File f = t.getFile();
				f.getParentFile().mkdirs();

				ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));

				try {
					/**
					 * 1, backup db
					 */
					if (MongoHelper.inst.isConfigured()) {
						Global.setConfig("backup/" + name, 2); // backup mongo
						MongoHelper.inst.backup(out, cc);
					}
					if (RDSHelper.inst.isConfigured()) {
						Global.setConfig("backup/" + name, 3); // backup RDS
						RDSHelper.inst.backup(out, cc);
					}

					if (log.isDebugEnabled())
						log.debug("zipping, dir=" + out);

					out.close();
				} finally {
					X.close(out);
				}

				t.save(Disk.seek(ROOT + name));

				Global.setConfig("backup/" + name, 100); // done

				if (!X.isEmpty(url)) {
					// store in other
					if (url.startsWith("ftp://")) {

						Url u = Url.create(url);
						FTPClient f1 = FTP.login(u);
						if (f1 != null) {
							InputStream in = new FileInputStream(t.getFile());
							try {
								log.debug("ftp put, filename=" + u.get("path") + "/" + name);

								f1.appendFile(u.get("path") + "/" + name, in);

								GLog.applog.info("backup", "auto", "backup success, name=" + name + ".zip", null, null);
							} finally {
								X.close(in);
							}
						}

					} else if (url.startsWith("sftp://")) {

						InputStream in = new FileInputStream(t.getFile());
						try {
							Url u = Url.create(url);
							SFTP.put(u, u.get("path") + "/" + name, in);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						} finally {
							X.close(in);
						}

					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				GLog.oplog.error(backup.class, "backup", e.getMessage(), e, null, null);

				Global.setConfig("backup/" + name, -1); // error

			}
		}

		public static void clean(int days) {

			DFile f = Disk.seek(BackupTask.ROOT);
			if (f.exists()) {
				try {
					DFile[] ff = f.listFiles();
					if (ff != null && ff.length > 0) {
						for (DFile f1 : ff) {
							if (f1.lastModified() < System.currentTimeMillis() - days * X.ADAY) {
								f1.delete();
							}
						}
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

	};

	static class RecoverTask extends Task {

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

		public RecoverTask(String name) {
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

				DFile f = Disk.seek(BackupTask.ROOT + name);

				ZipInputStream in = new ZipInputStream(f.getInputStream());

				/**
				 * 1, recover if configured
				 */
				ZipEntry e = in.getNextEntry();
				while (e != null) {
					String name = e.getName();
					if (X.isSame(name, "mongo.db") && MongoHelper.inst.isConfigured()) {
						Global.setConfig("backup/" + name, "正在恢复 mongo"); // recovering
						MongoHelper.inst.recover(in);
					} else if (X.isSame(name, "rds.db") && RDSHelper.inst.isConfigured()) {
						Global.setConfig("backup/" + name, "正在恢复 rds"); // recovering
						RDSHelper.inst.recover(in);
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
				GLog.oplog.error(backup.class, "recover", e.getMessage(), e, null, null);

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
			Monitor.finished(this);
			finished = true;
		}

	}
}
