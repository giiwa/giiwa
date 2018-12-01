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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Local;
import org.giiwa.core.bean.X;
import org.giiwa.core.task.SysTask;
import org.giiwa.framework.bean.Disk;

/**
 * The Class DiskHeartbeat.
 */
public class DiskHeartbeat extends SysTask {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The log.
	 */
	private static Log log = LogFactory.getLog(DiskHeartbeat.class);

	/**
	 * The inst.
	 */
	public static DiskHeartbeat inst = new DiskHeartbeat();

	/**
	 * The path.
	 */
	// private List<Disk> path = new ArrayList<Disk>();

	/**
	 * The interval.
	 */
	long interval = X.AHOUR;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task#onExecute()
	 */
	@Override
	public void onExecute() {
		interval = X.AHOUR;

		// int mask = JNotify.FILE_CREATED | JNotify.FILE_DELETED |
		// JNotify.FILE_RENAMED;

		W q = W.create().and("node", Local.id()).sort("created", 1);
		Beans<Disk> l1 = Disk.dao.load(q, 0, 10);
		if (l1 != null && !l1.isEmpty()) {
			interval = 3000;
			for (Disk d : l1) {

				Disk.dao.update(d.getId(), V.create("bad", 0).append("lasttime", System.currentTimeMillis()));

				if (System.currentTimeMillis() - d.getLong("checktime") > X.AMINUTE) {
					d.check();
				}

			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task#getName()
	 */
	@Override
	public String getName() {
		return "dfile.disk.heartbeat";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task#onFinish()
	 */
	@Override
	public void onFinish() {
		this.schedule(interval);
	}

	/**
	 * Inits the.
	 */
	public static void init() {
		inst.schedule(0);
	}

}
