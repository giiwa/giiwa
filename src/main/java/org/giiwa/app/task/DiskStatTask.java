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

import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Disk;
import org.giiwa.framework.bean.Stat;

/**
 * The Class DiskStatTask.
 */
public class DiskStatTask extends Task {

	// private static Log log = LogFactory.getLog(AlertStatTask.class);

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The inst.
	 */
	public static DiskStatTask inst = new DiskStatTask();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task#onExecute()
	 */
	@Override
	public void onExecute() {

		Disk.stat();

		long total = Disk.dao.sum("total", W.create());
		long free = Disk.dao.sum("free", W.create());
		long count = Disk.dao.sum("count", W.create());

		Stat.snapshot(System.currentTimeMillis(), "dfile.stat", Stat.SIZE.hour, W.create(), V.create(),
				new long[] { total, free, total - free, count });

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task#getName()
	 */
	@Override
	public String getName() {
		return "dfile.stat.task";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task#onFinish()
	 */
	@Override
	public void onFinish() {
		this.schedule(X.AHOUR);
	}

	/**
	 * Inits the.
	 */
	public static void init() {
		Disk.repair();

		inst.schedule((int) (30000 * Math.random()));
	}

}
