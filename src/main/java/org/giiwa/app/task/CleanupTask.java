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
import java.util.concurrent.locks.Lock;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Schema;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Code;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.bean.Stat;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.bean.m._CPU;
import org.giiwa.framework.bean.m._DB;
import org.giiwa.framework.bean.m._Disk;
import org.giiwa.framework.bean.m._Memory;
import org.giiwa.framework.bean.m._Net;
import org.giiwa.framework.web.Controller;
import org.giiwa.framework.web.Language;

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

	/**
	 * Instantiates a new cleanup task.
	 * 
	 * @param conf the conf
	 */
	private CleanupTask(Configuration conf) {

		home = Controller.GIIWA_HOME;

		add("org.giiwa.framework.bean");
		add("org.giiwa.framework.bean.m");
		add("org.giiwa.core.conf");

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

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onExecute()
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public void onExecute() {
		try {
			/**
			 * clean up the local temp files
			 */
			count = 0;

			/**
			 * clean up repo
			 */
			count += Repo.cleanup(X.ADAY);
			count += Temp.cleanup(X.ADAY);

			/**
			 * clean temp files in tomcat
			 */
			if (!X.isEmpty(Controller.GIIWA_HOME)) {
				// do it
				count += cleanup(Controller.GIIWA_HOME + "/work/Catalina/localhost/ROOT", X.ADAY, false);
				count += cleanup(Controller.GIIWA_HOME + "/logs", X.ADAY * 3, false);
			}

			GLog.applog.info("sys", "cleanup", "cleanup temp files: " + count);

			Lock door = Global.getLock("cleanup.glog");
			if (door.tryLock()) {
				try {
					int n = 0;

					n += Code.cleanup();

					for (BeanDAO d : new BeanDAO[] { GLog.dao, _CPU.dao, _CPU.Record.dao, _DB.dao, _DB.Record.dao,
							_Disk.dao, _Disk.Record.dao, _Memory.dao, _Memory.Record.dao, _Net.dao, _Net.Record.dao }) {
						if (!inCleanupTime())
							break;
						n += d.cleanup();
					}

					n += Stat.cleanup();

					GLog.applog.info("sys", "cleanup", "cleanup data: " + n);
				} finally {
					door.unlock();
				}
			}
		} catch (Exception e) {
			// eat the exception
		}
	}

	public static boolean inCleanupTime() {
		String time = Global.getString("gi.clean.time", "02:00-04:00");
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

			File f = new File(path);

			File[] ff = f.listFiles();
			if (ff != null) {
				for (File f1 : ff) {
					IOUtil.delete(f1, age);
				}
			}

			f.mkdirs();

		} catch (Exception e) {
			if (log.isErrorEnabled()) {
				log.error(e.getMessage(), e);
				GLog.applog.error(this.getName(), "cleanup", e.getMessage(), e, null, null);
			}
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
		this.schedule(X.AHOUR);
	}

	/**
	 * Please refer Schema.add
	 *
	 * @deprecated
	 * @param packname the packname
	 */
	public static void add(String packname) {

		Schema.add(packname);

	}

	public static void main(String[] args) {
		System.out.println(CleanupTask.inCleanupTime());
	}
}
