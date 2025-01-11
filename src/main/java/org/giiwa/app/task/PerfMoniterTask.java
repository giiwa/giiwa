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
import org.giiwa.bean.Disk;
import org.giiwa.bean.m._CPU;
import org.giiwa.bean.m._DB;
import org.giiwa.bean.m._Disk;
import org.giiwa.bean.m._FIO;
import org.giiwa.bean.m._Mem;
import org.giiwa.bean.m._Net;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.task.SysTask;

/**
 * The Class StateTask.
 */
public class PerfMoniterTask extends SysTask {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The log.
	 */
	static Log log = LogFactory.getLog(PerfMoniterTask.class);

	/**
	 * The owner.
	 */
	public static PerfMoniterTask owner = new PerfMoniterTask();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.getName()
	 */
	@Override
	public String getName() {
		return "gi.perf.moniter";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onExecute()
	 */
	@Override
	public void onExecute() {

		if (Global.getInt("perf.moniter", 1) == 0)
			return;

		_CPU.check();

		_Mem.check();

		_Disk.check();

		_Net.check();

		// TODO, 性能考虑
//		_Mem2.check();

		// TODO， 性能考虑
		_FIO.check();

		_DB.check();

		// TODO, 性能考虑
//		_MQ.check();

//		_DFile.check();

		// TODO， 性能考虑
//		_Cache.check();

		// TODO， 性能考虑
//		_File.check();

//		_APP.check();

//		_DFile2.check();

		Disk.stat();

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onFinish()
	 */
	@Override
	public void onFinish() {
		this.schedule(X.AMINUTE);
	}

}
