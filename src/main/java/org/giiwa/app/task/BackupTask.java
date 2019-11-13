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

import org.giiwa.app.web.admin.backup;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.task.Task;

/**
 * The Class BackupTask.
 */
public class BackupTask extends Task {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The owner.
	 */
	public static BackupTask owner = new BackupTask();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.getName()
	 */
	@Override
	public String getName() {
		return "gi.backup";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onExecute()
	 */
	@Override
	public void onExecute() {
		if (Global.getInt("backup.clean", 0) == 1) {
			// clean up
			int days = Global.getInt("backup.keep.days", 7);
			backup.BackupTask.clean(days);
		}

		if (Global.getInt("backup.auto", 0) == 1) {
			new backup.BackupTask(null, Global.getString("backup.url", null)).schedule(0);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onFinish()
	 */
	@Override
	public void onFinish() {
		if (Global.getInt("backup.auto", 0) == 1) {
			this.schedule(Global.getString("backup.point", "2:00"), true);
		}
	}

	/**
	 * Inits the task.
	 */
	public static void init() {
		owner.onFinish();
	}

}
