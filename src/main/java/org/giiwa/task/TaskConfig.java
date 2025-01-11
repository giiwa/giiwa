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

import org.giiwa.bean.Node;
import org.giiwa.json.JSON;

public class TaskConfig implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String name;
	public String clazzname;
	public String node;
	public String state;
	public long remain;
	public long delay;
	public long runtime;
	public long costing;
	public long duration;
	public int runtimes;

	@Override
	public String toString() {
		return "[name=" + name + ", clazzname=" + clazzname + ", node=" + node + ", state=" + state + ", remain="
				+ remain + ", delay=" + delay + ", runtime=" + runtime + ", costing=" + costing + ", duration="
				+ duration + ", runtimes=" + runtimes + "]";
	}

	public TaskConfig(Node n, Task t) {

		name = t.getName();
		clazzname = t.getClass().getName();
		node = n.label;
		state = t.getState().toString();
		remain = t.getRemain();
		delay = t.getDelay();
		runtime = t.getRuntime();
		costing = t.getCosting();
		duration = t.getDuration();
		runtimes = t.getRuntimes();

	}

	public JSON json() {

		JSON j1 = JSON.create();

		j1.put("name", name);
		j1.put("clazzname", clazzname);
		j1.put("node", node);
		j1.put("state", state);
		j1.put("remain", remain);
		j1.put("delay", delay);
		j1.put("runtime", runtime);
		j1.put("costing", costing);
		j1.put("duration", duration);
		j1.put("runtimes", runtimes);

		return j1;

	}

}
