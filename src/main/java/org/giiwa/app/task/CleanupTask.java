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
package org.giiwa.app.task;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.app.web.f;
import org.giiwa.bean.AuthToken;
import org.giiwa.bean.Code;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Message;
import org.giiwa.bean.S;
import org.giiwa.bean.Stat;
import org.giiwa.bean.Temp;
import org.giiwa.bean.Session.SID;
import org.giiwa.bean.m._CPU;
import org.giiwa.bean.m._Cache;
import org.giiwa.bean.m._DB;
import org.giiwa.bean.m._Disk;
import org.giiwa.bean.m._DiskIO;
import org.giiwa.bean.m._FIO;
import org.giiwa.bean.m._File;
import org.giiwa.bean.m._MQ;
import org.giiwa.bean.m._Mem;
import org.giiwa.bean.m._Mem2;
import org.giiwa.bean.m._Net;
import org.giiwa.conf.Global;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.X;
import org.giiwa.misc.IOUtil;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;
import org.giiwa.web.Language;

/**
 * The Class CleanupTask.
 */
public class CleanupTask extends Task {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The log.
	 */
	private static Log log = LogFactory.getLog(CleanupTask.class);

	public static CleanupTask inst = null;

	/**
	 * The home.
	 */
	String home;

	/**
	 * The count.
	 */
	long count = 0;

	public static void init(Configuration conf) {
		inst = new CleanupTask(conf);
		inst.schedule((long) (X.AMINUTE * Math.random()));
	}

	@Override
	public int getPriority() {
		return Thread.MIN_PRIORITY;
	}

	/**
	 * Instantiates a new cleanup task.
	 * 
	 * @param conf the conf
	 */
	private CleanupTask(Configuration conf) {

		home = Controller.GIIWA_HOME;

//		add("org.giiwa.framework.bean");
//		add("org.giiwa.framework.bean.m");
//		add("org.giiwa.core.conf");

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.getName()
	 */
	@Override
	public String getName() {
		return "gi.cleanup";
	}

	private static List<BeanDAO<?, ?>> bb = new ArrayList<BeanDAO<?, ?>>(Arrays.asList(new BeanDAO<?, ?>[] { GLog.dao,
			_CPU.dao, _CPU.Record.dao, _DB.dao, _DB.Record.dao, _Disk.dao, _Disk.Record.dao, _Mem.dao, _Mem.Record.dao,
			_Net.dao, _Net.Record.dao, _MQ.dao, _MQ.Record.dao, _Cache.dao, _Cache.Record.dao, _DiskIO.dao,
			_DiskIO.Record.dao, _File.dao, _File.Record.dao, _FIO.dao, _FIO.Record.dao, _Mem2.dao, _Mem2.Record.dao,
			Message.dao, SID.dao, AuthToken.dao, S.dao }));

	public static void add(BeanDAO<?, ?>... daos) {
		for (BeanDAO<?, ?> e : daos) {
			if (!bb.contains(e)) {
				bb.add(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onExecute()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void onExecute() {

		/**
		 * clean up the local temp files
		 */
		count = 0;

		log.warn("cleanup starting ...");

		/**
		 * clean up repo
		 */
//			count += Repo.cleanup(X.ADAY);
		count += Temp.cleanup(Global.getInt("temp.max.age", 24) * X.AHOUR);

		f.clean();

		/**
		 * clean temp files in tomcat
		 */
		if (!X.isEmpty(Controller.GIIWA_HOME)) {
			// do it
			count += cleanup(Controller.GIIWA_HOME + "/work/Catalina/localhost/ROOT", X.ADAY, false);
//				count += cleanup(Controller.GIIWA_HOME + "/logs", X.ADAY * 3, false);
		}

		GLog.applog.info("sys", "cleanup", "cleanup temp files: " + count);

		if (this.tryLock()) {
			try {
				int n = 0;

				n += Code.cleanup();

				if (inCleanupTime()) {

					log.warn("clean and repair table");
					GLog.applog.info("sys", "cleanup", "start to clean and repair");

					for (BeanDAO d : bb) {

						try {
							if (!inCleanupTime()) {
								log.warn("out of clean time.");
								GLog.applog.info("sys", "cleanup", "out of clean time. n=" + n);
								break;
							}

							log.warn("cleanup [" + d.tableName() + "] ...");

							int n1 = d.cleanup();
							if (n1 > 0) {
								GLog.applog.info("sys", "cleanup", "table=" + d.tableName() + ", removed=" + n1);
								n += n1;
							}
						} catch (Throwable e1) {
							log.error(e1.getMessage(), e1);
							GLog.applog.error("sys", "cleanup", d.tableName(), e1);
						}
					}

					n += Stat.cleanup();

					GLog.applog.info("sys", "cleanup", "end of cleanup, n= " + n);

				} else {
					GLog.applog.info("sys", "cleanup", "no in cleantime, clean code only, n= " + n);
				}

			} catch (Exception e) {
				// eat the exception
				log.error(e.getMessage(), e);
				GLog.applog.error("sys", "cleanup", e.getMessage(), e);
			} finally {
				this.unlock();
			}

		}

	}

	public static boolean inCleanupTime() {
		String time = Global.getString("gi.clean.time", "00:01-06:00");
		String t = Language.getLanguage().format(System.currentTimeMillis(), "HH:mm");
		String[] ss = X.split(time, "-");
		return t.compareTo(ss[0]) >= 0 && t.compareTo(ss[1]) <= 0;
	}

	/**
	 * Cleanup.
	 *
	 * @param path         the path
	 * @param expired      the expired
	 * @param deletefolder the deletefolder
	 * @return the long
	 */
	private long cleanup(String path, long age, boolean deletefolder) {
		try {

			log.warn("cleanup [" + path + "] ...");

			File f = new File(path);

			File[] ff = f.listFiles();
			if (ff != null) {
				for (File f1 : ff) {
					IOUtil.delete(f1, age, null);
				}
			}

			X.IO.mkdirs(f);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("sys", "cleanup", e.getMessage(), e, null, null);
		}

		return 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onFinish()
	 */
	@Override
	public void onFinish() {
		this.schedule(X.AMINUTE * 10);
	}

}
