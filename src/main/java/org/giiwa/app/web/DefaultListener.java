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
import java.util.Calendar;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.web.admin.mq;
import org.giiwa.app.web.admin.setting;
import org.giiwa.core.base.Shell;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Optimizer;
import org.giiwa.core.bean.X;
import org.giiwa.core.bean.helper.RDB;
import org.giiwa.core.bean.helper.RDSHelper;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.AuthToken;
import org.giiwa.framework.bean.Menu;
import org.giiwa.framework.bean.OpLog;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.bean.User;
import org.giiwa.framework.web.IListener;
import org.giiwa.framework.web.Model;
import org.giiwa.framework.web.Module;
import org.giiwa.mq.MQ;
import org.giiwa.mq.demo.Echo;

/**
 * default startup life listener.
 * 
 * @author joe
 * 
 */
public class DefaultListener implements IListener {

	public static final DefaultListener owner = new DefaultListener();

	public static class NtpTask extends Task {

		public static NtpTask owner = new NtpTask();

		private NtpTask() {
		}

		@Override
		public void onExecute() {
			String ntp = Global.getString("ntp.server", null);
			if (!X.isEmpty(ntp)) {
				try {
					String r = Shell.run("ntpdate -u " + ntp);
					OpLog.info("ntp", "sync", "NTP syncing: " + r, null, ntp);
				} catch (Exception e) {
					OpLog.error("ntp", "sync", "NTP syncing failed ", e, null, ntp);
				}
			}
		}

		@Override
		public void onFinish() {
			this.schedule(X.AHOUR);
		}
	}

	/**
	 * auto recycle the server, local configuration, recycle.task=时1｜时2
	 * 
	 * @author wujun
	 *
	 */
	private static class RecycleTask extends Task {

		static RecycleTask owner = new RecycleTask();

		private RecycleTask() {
		}

		@Override
		public void onExecute() {
			String s = Local.getString("recycle.task", "-1");
			if ((!X.isSame(s, "-1")) && System.currentTimeMillis() - Model.UPTIME > X.AHOUR) {
				/**
				 * recycle.task="-1" or " ", and the server started after 1 hour
				 */
				String[] ss = s.split("\\|");

				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(System.currentTimeMillis());
				int hour = c.get(Calendar.HOUR_OF_DAY);
				for (String s1 : ss) {
					if (hour == X.toInt(s1, -1)) {
						// yes
						recycle();
						break;
					}
				}
			}
		}

		private void recycle() {
			long t = X.toLong(Math.random() * X.AMINUTE, X.AMINUTE);
			log.warn("going to recycle in [" + t / 1000 + "] seconds");

			new Task() {

				@Override
				public void onExecute() {
					System.exit(0);
				}

			}.schedule(t);
		}

		@Override
		public void onFinish() {
			this.schedule(X.AHOUR);
		}
	}

	static Log log = LogFactory.getLog(DefaultListener.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.IListener#onStart(org.apache.commons.
	 * configuration.Configuration, org.giiwa.framework.web.Module)
	 */
	public void onStart(Configuration conf, Module module) {
		log.info("giiwa is starting...");

		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				// stop all task
				Task.stopAll(true);

				// stop all modules
				List<Module> l1 = Module.getAll(true);
				if (!X.isEmpty(l1)) {
					for (Module m : l1) {
						m.stop();
					}
				}
				log.warn("giiwa is stopped");
			}
		});

		// if (log.isDebugEnabled()) {
		// log.debug("upgrade.enabled=" +
		// Local.getString("upgrade.framework.enabled", "false"));
		// }

		/**
		 * cleanup html
		 */
		File f = new File(Model.GIIWA_HOME + "/html/");
		if (f.exists()) {
			delete(f);
		}

		setting.register(0, "system", setting.system.class);
		setting.register(1, "mq", mq.class);
		setting.register(10, "smtp", setting.mail.class);
		setting.register(11, "counter", setting.counter.class);

		if (Shell.isLinux()) {
			NtpTask.owner.schedule(X.AMINUTE);
		}
		new CleanupTask(conf).schedule(X.AMINUTE);
		// new AppdogTask().schedule(X.AMINUTE);
		RecycleTask.owner.schedule(X.AMINUTE);

		/**
		 * check and initialize
		 */
		User.checkAndInit();

		/**
		 * start the optimizer
		 */
		if (Global.getInt("db.optimizer", 1) == 1) {
			Helper.setOptmizer(new Optimizer());
		}

		if (!X.isEmpty(Global.getString("mq.type", X.EMPTY))) {
			new Task() {

				@Override
				public void onExecute() {
					MQ.init();

					// if (Global.getInt("mq.logger", 0) == 1) {
					// MQ.logger(true);
					// }

					// this is for "echo" service
					Echo e = new Echo("echo");
					try {
						e.bind();
					} catch (Exception e1) {
						log.error(e1.getMessage(), e1);
					}

				}

			}.schedule(10);
		} else {
			OpLog.info(mq.class, "startup", "disabled", null, null);
		}

	}

	private void delete(File f) {
		if (!f.exists()) {
			return;
		}
		if (f.isFile()) {
			f.delete();
		}

		if (f.isDirectory()) {
			File[] list = f.listFiles();
			if (list != null && list.length > 0) {
				for (File f1 : list) {
					delete(f1);
				}
			}
			f.delete();
		}
	}

	/**
	 * Run db script.
	 *
	 * @param f
	 *            the file
	 * @param m
	 *            the module
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 * @throws SQLException
	 *             the SQL exception
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
						if (!"".equals(line) && !line.startsWith("#")) {

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
									OpLog.error(m.getName(), "init", e.getMessage(), e, null, null);

									m.setError(e.getMessage());
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
						OpLog.error(m.getName(), "init", e.getMessage(), e, null, null);
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
			OpLog.error(m.getName(), "init", e.getMessage(), e, null, null);

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
	 * @see org.giiwa.framework.web.IListener#upgrade(org.apache.commons.
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
					String key = module.getName() + ".db.initial." + dbname + "." + f.lastModified();
					int b = Global.getInt(key, 0);
					if (b == 0) {
						if (log.isWarnEnabled()) {
							log.warn("db[" + key + "] has not been initialized! initializing...");
						}

						try {
							runDBScript(f, module);
							Global.setConfig(key, (int) 1);

							if (log.isWarnEnabled()) {
								log.warn("db[" + key + "] has been initialized! ");
							}

						} catch (Exception e) {
							if (log.isErrorEnabled()) {
								log.error(f.getAbsolutePath(), e);
								OpLog.error(module.getName(), "init", e.getMessage(), e, null, null);
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
			File f = module.getFile("../resources/menu.json", false, false);
			if (f == null || !f.exists()) {
				f = module.getFile("../res/menu.json", false, false);
			}
			if (f == null || !f.exists()) {
				f = module.getFile("../init/menu.json", false, false);
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
					Menu.insertOrUpdate(arr, module.getName());

					module.setStatus("menu.json initialized");

				} catch (Exception e) {
					if (log.isErrorEnabled()) {
						log.error(e.getMessage(), e);
						OpLog.error(module.getName(), "init", e.getMessage(), e, null, null);
					}

					module.setError(e.getMessage());
				} finally {
					if (reader != null) {
						try {
							reader.close();
						} catch (IOException e) {
							if (log.isErrorEnabled()) {
								log.error(e);
							}
						}
					}
				}
			} else {
				module.setStatus("no menu.json");
			}
		} else {
			if (log.isErrorEnabled()) {
				log.error("DB is miss configured, please congiure it in [" + Model.GIIWA_HOME
						+ "/giiwa/giiwa.properties]");
			}

			module.setError("DB is miss configured");
			return;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.IListener#uninstall(org.apache.commons.
	 * configuration.Configuration, org.giiwa.framework.web.Module)
	 */
	public void uninstall(Configuration conf, Module module) {
		Menu.remove(module.getName());
	}

	/**
	 * clean up the oplog, temp file in Temp
	 * 
	 * @author joe
	 * 
	 */
	private static class CleanupTask extends Task {

		static Log log = LogFactory.getLog(CleanupTask.class);

		String home;

		/**
		 * Instantiates a new cleanup task.
		 * 
		 * @param conf
		 *            the conf
		 */
		public CleanupTask(Configuration conf) {
			home = Model.GIIWA_HOME;
		}

		@Override
		public String getName() {
			return "cleanup.task";
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.worker.WorkerTask#onExecute()
		 */
		@Override
		public void onExecute() {
			try {
				/**
				 * clean up the local temp files
				 */
				int count = 0;
				for (String f : folders) {
					String path = home + f;
					count += cleanup(path, X.ADAY);
				}

				/**
				 * clean files in Temp
				 */
				if (!X.isEmpty(Temp.ROOT)) {
					count += cleanup(Temp.ROOT, X.ADAY);
				}

				/**
				 * clean temp files in tomcat
				 */
				if (!X.isEmpty(Model.GIIWA_HOME)) {
					// do it
					count += cleanup(Model.GIIWA_HOME + "/work", X.ADAY);
					count += cleanup(Model.GIIWA_HOME + "/logs", X.ADAY * 3);
				}
				if (log.isInfoEnabled()) {
					log.info("cleanup temp files: " + count);
				}

				// OpLog.cleanup();

				// AccessLog.cleanup();

				/**
				 * cleanup repo
				 */
				Repo.cleanup();

				/**
				 * cleanup authtoken
				 */
				AuthToken.cleanup();

			} catch (Exception e) {
				// eat the exception
			}
		}

		private int cleanup(String path, long expired) {
			int count = 0;
			try {
				File f = new File(path);

				/**
				 * test the file last modified exceed the cache time
				 */
				if (f.isFile() && System.currentTimeMillis() - f.lastModified() > expired) {
					f.delete();
					if (log.isInfoEnabled()) {
						log.info("delete file: " + f.getCanonicalPath());
					}
					count++;
				} else if (f.isDirectory()) {
					File[] list = f.listFiles();
					if (list != null) {
						/**
						 * cleanup the sub folder
						 */
						for (File f1 : list) {
							count += cleanup(f1.getAbsolutePath(), expired);
						}
					}
				}
			} catch (Exception e) {
				if (log.isErrorEnabled()) {
					log.error(e.getMessage(), e);
					OpLog.error(this.getName(), "cleanup", e.getMessage(), e, null, null);
				}
			}

			return count;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.worker.WorkerTask#priority()
		 */
		@Override
		public int priority() {
			return Thread.MIN_PRIORITY;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.giiwa.worker.WorkerTask#onFinish()
		 */
		@Override
		public void onFinish() {
			this.schedule(X.AHOUR);
		}

		static String[] folders = { "/temp/_cache", "/temp/_raw" };
	}

}
