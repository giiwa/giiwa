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

import org.giiwa.bean.Disk;
import org.giiwa.bean.Node;
import org.giiwa.bean.m._CPU;
import org.giiwa.bean.m._CPU._Stat;
import org.giiwa.bean.m._DB;
import org.giiwa.bean.m._Disk;
import org.giiwa.bean.m._FIO;
import org.giiwa.bean.m._Mem;
import org.giiwa.bean.m._Net;
import org.giiwa.conf.Global;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.X;
import org.giiwa.task.SysTask;
import org.giiwa.task.Task;

/**
 * 性能监测
 */
public class PerfMonitorTask extends SysTask {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The log.
	 */
//	private static Log log = LogFactory.getLog(PerfMonitorTask.class);

	/**
	 * The owner.
	 */
	public static PerfMonitorTask inst = new PerfMonitorTask();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.getName()
	 */
	@Override
	public String getName() {
		return "gi.perf.monitor";
	}

	@Override
	public boolean isEnabled() {
		if (Global.getInt("perf.moniter", 1) == 0) {
			return Boolean.FALSE;
		}
		return Boolean.TRUE;
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

		Disk.check();

		if (!Task.isScheduled(node.getName())) {
			node.schedule(0, true);
		}

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

	private _Node node = new _Node();

	static class _Node extends Task {

		private static final long serialVersionUID = 1L;

		@Override
		public String getName() {
			return "perf.monitor.node";
		}

		@Override
		public void onExecute() {
			W q = W.create().and("tag", "giiwa", W.OP.neq);
			try {
				Node.dao.stream(q, e -> {
					// cpu
					_Stat s = new _Stat();
					s.name = "cpu";
					s.cores = e.cores;
					s.user = 0;
					s.sys = 0;
					s.usage = e.usage;
					_CPU.update(e.id, s);

					// mem
					_Mem.update(e.id, e.mem, e.mem - e.mem * e.mem_usage / 100);

					return true;
				});
			} catch (Exception err) {
				log.error(err.getMessage(), err);
			}
		}

		@Override
		public void onFinish() {
			this.schedule(X.AMINUTE, true);
		}

	}

}
