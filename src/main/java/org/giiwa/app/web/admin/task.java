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

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
public class task extends Controller {

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		this.set("pending", Task.tasksInQueue());
		this.set("running", Task.tasksInRunning());
		this.set("idle", Task.idleThread());
		this.set("active", Task.activeThread());

		this.set("list", Task.getAll());

		this.show("/admin/task.index.html");
	}

	@Path(path = "kill", login = true, access = "access.config.admin")
	public void kill() {
		String name = this.getString("name");
		Task t = Task.get(name);
		t.stop(false);

		this.response(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "killed"));

	}

	@Path(path = "dump", login = true, access = "access.config.admin")
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

					sb.append("<div style='color: .888;'>")
							.append(t.onDump(new StringBuilder()).toString().replaceAll("\r\n", "<br/>"))
							.append("</div>");

					if (ss != null && ss.length > 0) {
						for (StackTraceElement e : ss) {

							sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(e.toString()).append("<br/>");
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

	@Path(path = "trace", login = true, access = "access.config.admin")
	public void trace() {
		String name = this.getString("name");
		Task t = Task.get(name);
		JSON j = JSON.create();
		StringBuilder sb = new StringBuilder();
		try {
			if (t != null) {
				sb.append("<pre>" + t.getTrace() + "</pre>");
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

	@Path(path = "dumpall", login = true, access = "access.config.admin")
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
					for (StackTraceElement e : ss) {
						sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(e.toString()).append("<br/>");
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

	@Path(login = true, path = "thread", access = "access.config.admin")
	public void thread() {

		List<JSON> l1 = JSON.createList();
		TreeMap<Thread.State, Long> states = new TreeMap<Thread.State, Long>();

		try {
			Map<Thread, StackTraceElement[]> dumps = Thread.getAllStackTraces();
			for (Thread t : dumps.keySet()) {

				Long n = states.get(t.getState());
				if (n == null) {
					states.put(t.getState(), 1L);
				} else {
					states.put(t.getState(), n + 1);
				}

				JSON j = JSON.create();
				j.append("name", t.getName());
				j.append("priority", t.getPriority());
				j.append("id", t.getId());
				j.append("state", t.getState());

				StackTraceElement[] ss = dumps.get(t);
				StringBuilder sb1 = new StringBuilder();
				StringBuilder sb2 = new StringBuilder();
				if (ss != null && ss.length > 0) {
					int ii = 0;
					for (StackTraceElement e : ss) {
						if (ii < 2) {
							sb1.append(e.toString()).append("<br/>");
							ii++;
						}
						sb2.append(e.toString()).append("<br/>");
					}
				}
				j.append("trace1", sb1.toString());
				j.append("trace2", sb2.toString());

				l1.add(j);
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		Collections.sort(l1, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				long id1 = o1.getLong("id");
				long id2 = o2.getLong("id");
				if (id1 == id2)
					return 0;

				return id1 < id2 ? -1 : 1;
			}

		});

		this.set("states", states);
		this.set("list", l1);

		this.show("/admin/task.thread.html");

	}

	@Path(login = true, path = "thread/kill", access = "access.config.admin")
	public void thread_kill() {

		long id = this.getLong("id");

		try {
			Map<Thread, StackTraceElement[]> dumps = Thread.getAllStackTraces();
			for (Thread t : dumps.keySet()) {
				if (t.getId() == id) {
					t.interrupt();
					break;
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		this.response(JSON.create().append(X.STATE, 200));

	}

	@Path(login = true, path = "thread/deadlock", access = "access.config.admin")
	public void thread_deadlock() {

		List<JSON> l1 = JSON.createList();

		try {

			ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
			long[] tt = tmxb.findDeadlockedThreads();

			if (tt != null && tt.length > 0) {
				Map<Thread, StackTraceElement[]> dumps = Thread.getAllStackTraces();
				for (Thread t : dumps.keySet()) {

					if (!X.isIn(t.getId(), tt)) {
						continue;
					}

					JSON j = JSON.create();
					j.append("name", t.getName());
					j.append("priority", t.getPriority());
					j.append("id", t.getId());
					j.append("state", t.getState());

					StackTraceElement[] ss = dumps.get(t);
					StringBuilder sb2 = new StringBuilder();
					if (ss != null && ss.length > 0) {
						for (StackTraceElement e : ss) {
							sb2.append(e.toString()).append("<br/>");
						}
					}
					j.append("trace2", sb2.toString());

					l1.add(j);
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		Collections.sort(l1, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				long id1 = o1.getLong("id");
				long id2 = o2.getLong("id");
				if (id1 == id2)
					return 0;

				return id1 < id2 ? -1 : 1;
			}

		});

		this.set("list", l1);

		this.show("/admin/task.thread.deadlock.html");

	}

}
