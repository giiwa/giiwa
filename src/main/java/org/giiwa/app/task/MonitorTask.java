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

import java.util.ArrayList;
import java.util.List;

import org.giiwa.bean.GLog;
import org.giiwa.bean.Node;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.X;
import org.giiwa.task.Runner;
import org.giiwa.task.SysTask;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;

/**
 * 任务监测
 */
public class MonitorTask extends SysTask {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The log.
	 */
//	private static Log log = LogFactory.getLog(MonitorTask.class);

	/**
	 * The owner.
	 */
	private static MonitorTask owner = new MonitorTask();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.getName()
	 */
	@Override
	public String getName() {
		return "gi.monitor";
	}

	private static List<Task> _task = new ArrayList<Task>();

	/**
	 * 添加监测任务
	 * 
	 * @param e
	 */
	public static void add(Task e) {
		synchronized (_task) {
			if (!_task.contains(e)) {
				_task.add(e);
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.worker.WorkerTask.onExecute()
	 */
	@Override
	public void onExecute() {

		Node n = Local.node();
		if (Global.now() - Controller.UPTIME > X.AMINUTE * 10 && Global.now() - n.lastcheck > X.AMINUTE * 10) {
			// 已经启动10分钟，并且10分钟离线， 重启
			log.warn("node lost exceed 10 minutes, restart.");
			System.exit(0);
		}

		if (_task.isEmpty()) {
			return;
		}

		Task[] tt = null;
		synchronized (_task) {
			tt = _task.toArray(new Task[_task.size()]);
		}

		for (Task e : tt) {
			if (e.isEnabled() && !e.isScheduled()) {
				e.schedule(X.toLong(X.AMINUTE * Math.random()));
				GLog.applog.info("sys", "monitor", "starting [" + e.getName() + "]");
			}
		}

		// 定时巡检运行中队列任务 清理超时/卡死任务，中断并终止任务执行
		Runner.checkAndKill();

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

	public static void init() {
		owner.schedule(X.AMINUTE);
	}

}
