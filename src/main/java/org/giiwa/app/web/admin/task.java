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
package org.giiwa.app.web.admin;

import java.util.Map;

import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;
import org.giiwa.core.task.Task;
import org.giiwa.framework.web.*;

/**
 * web api: /admin/task <br>
 * used to manage task,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class task extends Model {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model#onGet()
	 */
	@Path(login = true, access = "access.config.admin|access.config.system.admin")
	public void onGet() {

		this.set("pending", Task.tasksInQueue());
		this.set("running", Task.tasksInRunning());
		this.set("idle", Task.idleThread());
		this.set("active", Task.activeThread());

		this.set("list", Task.getAll());

		this.query.path("/admin/task");
		this.show("/admin/task.index.html");
	}

	@Path(path = "dump", login = true, access = "access.config.admin|access.config.system.admin")
	public void dump() {
		String name = this.getString("name");
		Task t = Task.get(name);
		JSON j = JSON.create();
		StringBuilder sb = new StringBuilder();
		try {
			if (t != null) {
				Thread t1 = t.getThread();
				if (t1 != null) {
					StackTraceElement[] ss = t1.getStackTrace();
					sb.append("ID: ").append(t1.getId()).append("(0x").append(Long.toHexString(t1.getId()))
							.append("), Thread: ").append(t1.getName()).append(", State: <i style='color:green'>")
							.append(t1.getState()).append("</i>, Task:").append(t.getClass().getName()).append("<br/>");

					sb.append("<div style='color: #888;'>")
							.append(t.onDump(new StringBuilder()).toString().replaceAll("\r\n", "<br/>"))
							.append("</div>");

					if (ss != null && ss.length > 0) {
						sb.append(ss[0].toString()).append("<br/>");
						for (StackTraceElement e : ss) {
							sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(e.getClassName()).append(".")
									.append(e.getMethodName()).append("(").append(e.getLineNumber()).append(")")
									.append("<br/>");
						}
					}
				}
			}
			if (sb.length() > 0) {
				j.put(X.STATE, 200);
				j.put(X.MESSAGE, sb.toString());
			} else {
				j.put(X.STATE, 201);
				j.put(X.ERROR, lang.get("task.notfound"));
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			j.put(X.STATE, 201);
			j.put(X.ERROR, lang.get("task.notfound"));
		}

		this.response(j);

	}

	@Path(path = "dumpall", login = true, access = "access.config.admin|access.config.system.admin")
	public void dumpall() {
		JSON j = JSON.create();
		StringBuilder sb = new StringBuilder();
		try {
			int i = 0;
			Map<Thread, StackTraceElement[]> dumps = Thread.getAllStackTraces();
			for (Thread t : dumps.keySet()) {
				StackTraceElement[] ss = dumps.get(t);
				i++;
				sb.append(i).append(") ID: ").append(t.getId()).append("(0x").append(Long.toHexString(t.getId()))
						.append("), Thread: ").append(t.getName()).append(", State: <i style='color:green'>")
						.append(t.getState()).append("</i>").append("<br/>");

				if (ss != null && ss.length > 0) {
					sb.append(ss[0].toString()).append("<br/>");
					for (StackTraceElement e : ss) {
						sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(e.getClassName()).append(".")
								.append(e.getMethodName()).append("(").append(e.getLineNumber()).append(")")
								.append("<br/>");
					}
				}
			}
			if (sb.length() > 0) {
				j.put(X.STATE, 200);
				j.put(X.MESSAGE, sb.toString());
			} else {
				j.put(X.STATE, 201);
				j.put(X.ERROR, lang.get("task.notfound"));
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			j.put(X.STATE, 201);
			j.put(X.ERROR, lang.get("task.notfound"));
		}

		this.response(j);
	}

}
