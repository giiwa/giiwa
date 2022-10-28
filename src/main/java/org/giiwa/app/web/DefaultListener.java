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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.task.BackupTask;
import org.giiwa.app.task.CleanupTask;
import org.giiwa.app.task.NtpTask;
import org.giiwa.app.task.RecycleTask;
import org.giiwa.app.task.SecurityTask;
import org.giiwa.app.task.PerfMoniterTask;
import org.giiwa.app.web.admin.dashboard;
import org.giiwa.app.web.admin.mq;
import org.giiwa.app.web.admin.profile;
import org.giiwa.app.web.admin.setting;
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.bean.License;
import org.giiwa.bean.Menu;
import org.giiwa.bean.Node;
import org.giiwa.bean.User;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Helper;
import org.giiwa.dao.RDSHelper;
import org.giiwa.dao.Schema;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.RDB;
import org.giiwa.json.JSON;
import org.giiwa.misc.AES;
import org.giiwa.misc.Host;
import org.giiwa.misc.IOUtil;
import org.giiwa.misc.Shell;
import org.giiwa.net.mq.MQ;
import org.giiwa.snmp.SampleAgent;
import org.giiwa.task.SysTask;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.IListener;
import org.giiwa.web.Module;

/**
 * default startup life listener.
 * 
 * @author joe
 * 
 */
@SuppressWarnings("deprecation")
public class DefaultListener implements IListener {

	public static final DefaultListener owner = new DefaultListener();

	static Log log = LogFactory.getLog(DefaultListener.class);

	public void onInit(Configuration conf, Module module) {

		log.warn("giiwa is initing...");

		try {

			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					// stop all task

					log.warn("giiwa is stopping by hook");

					Node.dao.update(Local.id(), V.create().append("lastcheck", 0));

					MQ.stop();

					Task.stopAll(true);

					log.warn("task is stopped.");

					// stop all modules
					{
						List<Module> l1 = Module.getAll(true);
						if (!X.isEmpty(l1)) {
							for (Module m : l1) {
								m.stop();
							}
						}
					}

					{
						try {
							List<JSON> l1 = Host.getProcess(Host.getPid());
							if (l1 != null && !l1.isEmpty()) {
								l1.forEach(e -> {
									long pid = e.getLong("pid");
									if (pid > 0) {
										Shell.kill(pid);
									}
								});
							}
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}

					log.warn("giiwa is stopped.");
				}
			});

			new SysTask() {

				/**
				 * 
				 */
				private static final long serialVersionUID = 1L;

				@Override
				public void onExecute() {
					if (log.isDebugEnabled()) {
						log.debug("initing mq");
					}

					MQ.init();
					Local.init();

				}

			}.schedule(0);

			Disk.repair();

			/**
			 * start the optimizer
			 */
			Helper.enableOptmizer();

			module.setLicense(License.LICENSE.licensed,
					"Dz/noswbChPlrVmbT7wNupVME5wHlpJ9YixhEEwQjj6kKmrOrVWXqJ24DHbdLatSDzTePhnehBQgKwxmUXZsEXq452PV1pi64h3wAxW8IGjq5YkPVpPdbXnh90s+6NILWUbmOKJgroDnYh4+/b1ZsWkv9Fe4u7VTb7eAFVq6P8E=");

			dashboard.desk("/admin/dashboard");
			dashboard.desk("/admin/home.html");

//			napp.add("MongoDB", "/portlet/db/conns");
//			napp.add("MongoDB", "/portlet/db/strw");
//			napp.add("MongoDB", "/portlet/db/netio");

			setting.register(0, "system", setting.system.class);
			setting.register(1, "mq", mq.class);
			setting.register(2, "snmp", setting.snmp.class);
			setting.register(10, "smtp", setting.smtp.class);
			// setting.register(11, "counter", setting.counter.class);
			profile.register(0, "my", profile.my.class);

			/**
			 * check and initialize
			 */
			User.checkAndInit();

			User.repair();

			/**
			 * cleanup html
			 */
			File f = new File(Controller.GIIWA_HOME + "/html/");
			IOUtil.delete(f);

			f = new File(Controller.GIIWA_HOME + "/temp/");
			if (!f.exists()) {
				X.IO.mkdirs(f);
			}

			IOUtil.cleanup(f);

//			Schema.add("org.giiwa.bean");
//			Schema.add("org.giiwa.conf");

			AES.init();

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		log.warn("giiwa is inited");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.IListener.onStart(org.apache.commons.
	 * configuration.Configuration, org.giiwa.framework.web.Module)
	 */
	public void onStart(Configuration conf, Module module) {

		log.warn("giiwa is starting...");

		if (log.isInfoEnabled()) {
			log.info(
					"global.id: " + Global.id() + "\n\tlocal.id: " + Local.id() + "\n\tlocal ip: " + Host.getLocalip());
		}

		Task.schedule(t -> {

			CleanupTask.init(Config.getConf());
			NtpTask.inst.schedule(X.toLong(X.AMINUTE * Math.random()));
			RecycleTask.owner.schedule(X.toLong(X.AMINUTE * Math.random()));
			SecurityTask.inst.schedule(X.toLong(X.AMINUTE * Math.random()));
			PerfMoniterTask.owner.schedule(X.toLong(X.AMINUTE * Math.random()));
			BackupTask.init();

			Schema.add("org.giiwa");

			Node.init();

//			Schema.init();

			SampleAgent.start();

		});

		log.warn("giiwa is started");

	}

	@Override
	public void onStop() {
	}

	/**
	 * Run db script.
	 *
	 * @param f the file
	 * @param m the module
	 * @throws IOException  Signals that an I/O exception has occurred.
	 * @throws SQLException the SQL exception
	 */
	public static void runDBScript(File f, Module m) throws IOException, SQLException {

		int count = 0;

		BufferedReader in = null;
		Connection c = null;
		Statement s = null;
		try {
			c = RDSHelper.inst.getConnection();
			if (c != null) {
				in = new BufferedReader(new InputStreamReader(new FileInputStream(f), "utf-8"));
				StringBuilder sb = new StringBuilder();
				try {
					String line = in.readLine();
					while (line != null) {
						line = line.trim();
						if (log.isDebugEnabled())
							log.debug("line=" + line);
						if (!"".equals(line) && !line.startsWith(".")) {

							sb.append(line).append("\r\n");

							if (line.endsWith(";")) {
								String sql = sb.toString().trim();
								sql = sql.substring(0, sql.length() - 1);

								try {
									if (!X.isEmpty(sql)) {
										s = c.createStatement();
										s.executeUpdate(sql);
										s.close();
										count++;
									}
								} catch (Exception e) {
									log.error(sb.toString(), e);
									// GLog.applog.error(m.getName(), "init", e.getMessage(), e, null, null);
									// m.setError(e.getMessage());
								}
								s = null;
								sb = new StringBuilder();
							}
						}
						line = in.readLine();
					}

					String sql = sb.toString().trim();
					if (!"".equals(sql)) {
						s = c.createStatement();
						s.executeUpdate(sql);
					}
				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error(sb.toString(), e);
						GLog.applog.error(m.getName(), "init", e.getMessage(), e, null, null);
					}

					m.setError(e.getMessage());
				}
			} else {
				if (log.isWarnEnabled()) {
					log.warn("database not configured !");
				}

			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error(m.getName(), "init", e.getMessage(), e, null, null);

			m.setError(e.getMessage());

		} finally {
			if (in != null) {
				in.close();
			}
			RDSHelper.inst.close(s, c);
		}

		if (count > 0) {
			// TODO
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.IListener.upgrade(org.apache.commons.
	 * configuration.Configuration, org.giiwa.framework.web.Module)
	 */
	public void upgrade(Configuration conf, Module module) {

		if (log.isDebugEnabled()) {
			log.debug(module + " upgrading...");
		}

		/**
		 * test database connection has configured?
		 */
		try {
			/**
			 * test the database has been installed?
			 */
			String dbname = RDB.getDriver();

			if (!X.isEmpty(dbname) && RDSHelper.inst.isConfigured()) {
				/**
				 * initial the database
				 */
				String filename = "../resources/install/" + dbname + "/initial.sql";
				File f = module.getFile(filename, false, false);
				if (f == null || !f.exists()) {
					f = module.getFile("../res/install/" + dbname + "/initial.sql", false, false);
				}
				if (f == null || !f.exists()) {
					f = module.getFile("../init/install/" + dbname + "/initial.sql", false, false);
				}
				if (f == null || !f.exists()) {
					f = module.getFile("../init/install/default/initial.sql", false, false);
				}

				if (f != null && f.exists()) {
					String key = module.getName() + ".db.initial." + dbname;
					// + "." + f.lastModified();
					// int b = Global.getInt(key, 0);
					if (Local.getLong(key, 0) != f.lastModified()) {
						if (log.isWarnEnabled()) {
							log.warn("db[" + key + "] has not been initialized! initializing...");
						}

						try {
							runDBScript(f, module);
							Local.setConfig(key, f.lastModified());

							if (log.isWarnEnabled()) {
								log.warn("db[" + key + "] has been initialized! ");
							}

						} catch (Exception e) {
							if (log.isErrorEnabled()) {
								log.error(f.getAbsolutePath(), e);
								GLog.applog.error(module.getName(), "init", e.getMessage(), e, null, null);
							}
							module.setError(e.getMessage());
						}
					} else {
						module.setStatus("db script initialized last time");
					}
				} else {
					if (log.isWarnEnabled()) {
						log.warn("db[" + module.getName() + "." + dbname + "] not exists ! " + filename);
					}
					module.setStatus("RDS configured, db script not exists!");
				}

			} else {

				module.setStatus("no RDS configured, ignore the db script");
			}
		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error("no database configured!", e);
			}

			module.setError(e.getMessage());

			return;
		}

		if (Helper.isConfigured()) {
			/**
			 * check the menus
			 * 
			 */
			File f = module.getFile("../init/menu.json", false, false);
			if (f == null || !f.exists()) {
				f = module.getFile("../res/menu.json", false, false);
			}
			if (f == null || !f.exists()) {
				f = module.getFile("../resources/menu.json", false, false);
			}

			if (f != null && f.exists()) {
				BufferedReader reader = null;
				try {
					if (log.isDebugEnabled()) {
						log.debug("initialize [" + f.getCanonicalPath() + "]");
					}

					reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
					StringBuilder sb = new StringBuilder();
					String line = reader.readLine();
					while (line != null) {
						sb.append(line).append("\r\n");
						line = reader.readLine();
					}

					/**
					 * convert the string to json array
					 */
					List<JSON> arr = JSON.fromObjects(sb.toString());
					Menu.insertOrUpdate(arr, module);

					module.setStatus("menu.json initialized");

				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error(e.getMessage(), e);
						GLog.applog.error(module.getName(), "init", e.getMessage(), e, null, null);
					}

					module.setError(e.getMessage());
				} finally {
					X.close(reader);
				}
			} else {
				module.setStatus("no menu.json");
			}

		} else {
			if (log.isErrorEnabled()) {
				log.error("DB is miss configured, please congiure it in [" + Controller.GIIWA_HOME
						+ "/giiwa.properties]");
			}

			module.setError("DB is miss configured");
			return;
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.IListener.uninstall(org.apache.commons.
	 * configuration.Configuration, org.giiwa.framework.web.Module)
	 */
	public void uninstall(Configuration conf, Module module) {
		Menu.remove(module.getName());
	}

}
