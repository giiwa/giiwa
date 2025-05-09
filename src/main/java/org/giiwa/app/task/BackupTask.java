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

import org.giiwa.bean.AutoBackup;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.task.Task;

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
	public static BackupTask inst = new BackupTask();

	private Object readResolve() {
		return inst;
	}

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

		AutoBackup a = AutoBackup.dao
				.load(W.create().and("enabled", 1).and("nextime", Global.now(), W.OP.lte));
		if (a != null) {
			a.backup();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onFinish()
	 */
	@Override
	public void onFinish() {
		AutoBackup a = AutoBackup.dao.load(W.create().and("enabled", 1));
		if (a != null) {
			this.schedule(X.AMINUTE, true);
		}
	}

	/**
	 * Inits the task.
	 */
	public static void init() {
		AutoBackup a = AutoBackup.dao.load(W.create().and("enabled", 1));
		if (a != null) {
			inst.schedule(X.AMINUTE, true);
		}
	}

}
