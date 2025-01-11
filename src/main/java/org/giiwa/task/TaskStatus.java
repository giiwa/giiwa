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
package org.giiwa.task;

import java.io.Serializable;
import java.util.List;

import org.giiwa.bean.Node;
import org.giiwa.dao.X;

public class TaskStatus implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public int pending;
	public int running;
	public int cores;

	public List<TaskConfig> list;

	@Override
	public String toString() {
		return "[pending=" + pending + ", running=" + running + ", cores=" + cores + ", list=" + list + "]";
	}

	public TaskStatus(Node n) {

		pending = Task.tasksInQueue(Task.GLOBAL, Task.SYSGLOBAL);
		running = Task.tasksInRunning(Task.GLOBAL, Task.SYSGLOBAL);
		cores = Task.cores;

		list = X.asList(Task.getRunningTask(Task.GLOBAL, Task.SYSGLOBAL), e -> {
			Task t = (Task) e;
			return new TaskConfig(n, t);
		});

	}

}
