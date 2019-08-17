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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.base.ClassUtil;
import org.giiwa.core.base.IOUtil;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.X;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.GLog;
import org.giiwa.framework.bean.Repo;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Model;

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

	public static CleanupTask inst = null;// new CleanupTask();

	/**
	 * The home.
	 */
	String home;

	/**
	 * The count.
	 */
	long count = 0;

	/**
	 * The file.
	 */
	String file;

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

		home = Model.GIIWA_HOME;

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
		return "cleanup.task";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onExecute()
	 */
	@Override
	public void onExecute() {
		try {
			/**
			 * clean up the local temp files
			 */
			count = 0;
//			for (String f : folders) {
//				String path = home + f;
//				cleanup(path, X.ADAY, true);
//			}

			/**
			 * clean up repo
			 */
			Repo.cleanup(X.ADAY);
			Temp.cleanup(X.ADAY);

			/**
			 * clean temp files in tomcat
			 */
			if (!X.isEmpty(Model.GIIWA_HOME)) {
				// do it
				cleanup(Model.GIIWA_HOME + "/work", X.ADAY, false);
				cleanup(Model.GIIWA_HOME + "/logs", X.ADAY * 3, false);
			}
			if (log.isInfoEnabled()) {
				log.info("cleanup temp files: " + count);
			}

			if (log.isDebugEnabled())
				log.debug("cleanup, beans=" + beans);

			// for (Class<? extends Bean> c : beans) {
			// try {
			// Bean b = c.newInstance();
			// b.cleanup();
			// } catch (Exception e) {
			// log.error(e.getMessage(), e);
			// }
			// }

		} catch (Exception e) {
			// eat the exception
		}
	}

	/**
	 * Cleanup.
	 *
	 * @param path         the path
	 * @param expired      the expired
	 * @param deletefolder the deletefolder
	 * @return the long
	 */
	private long cleanup(String path, long expired, boolean deletefolder) {
		try {
			File f = new File(path);
			file = f.getCanonicalPath();

			/**
			 * test the file last modified exceed the cache time
			 */
			if (f.isFile()) {
				if (System.currentTimeMillis() - f.lastModified() > expired) {
					IOUtil.delete(f);
				}
			} else if (f.isDirectory()) {
				File[] list = f.listFiles();
				if (list == null || list.length == 0) {
					if (deletefolder) {
						IOUtil.delete(f);
					}
				} else if (list != null) {
					/**
					 * cleanup the sub folder
					 */
					for (File f1 : list) {
						cleanup(f1.getAbsolutePath(), expired, deletefolder);
					}
				}
			}
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
	 * The folders.
	 */
//	static String[] folders = { "/temp/_raw" };
//	static String[] folders = { "/temp/_cache", "/temp/_raw" };

	/**
	 * The beans.
	 */
	public static List<Class<? extends Bean>> beans = new ArrayList<Class<? extends Bean>>();

	/**
	 * Adds the.
	 *
	 * @param packname the packname
	 */
	public static void add(String packname) {

		List<Class<Bean>> l1 = ClassUtil.listSubType(packname, Bean.class);
		if (l1 != null) {
			for (Class<Bean> t : l1) {
				if (!beans.contains(t)) {
					beans.add(t);
				}
			}

			Collections.sort(beans, new Comparator<Class<? extends Bean>>() {

				@Override
				public int compare(Class<? extends Bean> o1, Class<? extends Bean> o2) {
					return o1.getName().compareTo(o2.getName());
				}

			});
		}

	}

}
