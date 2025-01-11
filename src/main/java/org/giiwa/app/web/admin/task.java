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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.giiwa.bean.GLog;
import org.giiwa.bean.Node;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.Host;
import org.giiwa.misc.Shell;
import org.giiwa.net.mq.MQ;
import org.giiwa.task.Task;
import org.giiwa.task.TaskConfig;
import org.giiwa.task.TaskStatus;
import org.giiwa.web.*;

/**
 * web api: /admin/task <br>
 * used to manage task,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class task extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin", oplog = true)
	public void onGet() {

		this.set("pending", Task.tasksInQueue());
		this.set("running", Task.tasksInRunning());
//		this.set("idle", Task.idleThread());
//		this.set("active", Task.activeThread());

		this.set("list", Task.getAll());

		this.show("/admin/task.index.html");
	}

	@Path(path = "global", login = true, access = "access.config.admin", oplog = true)
	public void global() {

		AtomicLong pending = new AtomicLong(0);
		AtomicLong running = new AtomicLong(0);
		AtomicLong cores = new AtomicLong(0);

		W q = Node.dao.query().and("giiwa", null, W.OP.neq);
		q.and("lastcheck", System.currentTimeMillis() - Node.LOST, W.OP.gte);

		List<JSON> task = new ArrayList<JSON>();

		try {

			AtomicLong has = new AtomicLong(q.count());

			GLog.applog.info("sys", "task", "MQ1, has=" + has.get() + ", q=" + q);

			MQ.callTopic(Task.MQNAME, "list", "", 5000, req -> {

				String from = req.from;

				try {

					TaskStatus s = req.get();

					pending.set(s.pending);
					running.addAndGet(s.running);
					cores.addAndGet(s.cores);
					List<TaskConfig> l1 = s.list;
					if (l1 != null) {

						List<JSON> l2 = X.asList(l1, e -> {

							TaskConfig e1 = (TaskConfig) e;
							return e1.json();

						});
						synchronized (task) {
							task.addAll(l2);
						}
					}

				} catch (Exception e) {
					GLog.applog.error("sys", "task", "from=" + from + ", error=" + e.getMessage(), e);
				}

				has.decrementAndGet();
				if (has.get() <= 0) {
					return true;
				} else {
					return false;
				}
			});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("task", "checking", e.getMessage(), e);
//			this.set(X.MESSAGE, e.getMessage());
		}

		Collections.sort(task, new Comparator<JSON>() {

			@Override
			public int compare(JSON o1, JSON o2) {
				String name1 = o1.getString("name");
				String name2 = o2.getString("name");
				return X.compareTo(name1, name2);
			}

		});
		this.set("pending", pending.get());
		this.set("running", running.get());
		this.set("cores", cores.get());

		this.set("list", task);

		this.show("/admin/task.global.html");

	}

	@Path(path = "kill", login = true, access = "access.config.admin", oplog = true)
	public void kill() {
		String name = this.getString("name");
		Task t = Task.get(name);
		t.stop(false);

		this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "killed"));

	}

	@Path(path = "dump", login = true, access = "access.config.admin", oplog = true)
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
							.append(t1.getState()).append("</i>, Task:").append(t.getClass().getName()).append("\r");

					sb.append("<div style='color: .888;'>").append(t.onDump(new StringBuilder()).toString())
							.append("</div>");

					if (ss != null && ss.length > 0) {
						for (StackTraceElement e : ss) {

							sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(e.toString()).append("\r");
						}
					}
				} else {
					sb.append("thread is null, scheduled=" + t.isScheduled() + ", isrunning=" + t.isRunning()
							+ ", runtimes=" + t.getRuntimes() + ", sf=" + t.getSF());
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

		this.send(j);

	}

	@Path(path = "trace", login = true, access = "access.config.admin", oplog = true)
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

		this.send(j);

	}

	@Path(path = "dumpall", login = true, access = "access.config.admin", oplog = true)
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
						.append(t.getState()).append("</i>").append("\r");

				if (ss != null && ss.length > 0) {
					for (StackTraceElement e : ss) {
						sb.append("&nbsp;&nbsp;&nbsp;&nbsp;").append(e.toString()).append("\r");
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

		this.send(j);
	}

	@Path(login = true, path = "thread", access = "access.config.admin", oplog = true)
	public void thread() {

		try {
			String s = Shell.run("jstack " + Host.getPid(), X.AMINUTE);

			this.set("dump", s);
			this.show("/admin/task.thread.html");
		} catch (Exception e) {
			this.error(e);
		}

	}

	@SuppressWarnings("deprecation")
	@Path(login = true, path = "thread/kill", access = "access.config.admin", oplog = true)
	public void thread_kill() {

		long id = this.getLong("id");

		try {
			Map<Thread, StackTraceElement[]> dumps = Thread.getAllStackTraces();
			for (Thread t : dumps.keySet()) {
				if (t.getId() == id) {
//					t.interrupt();
					t.stop();
					break;
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		this.send(JSON.create().append(X.STATE, 200));

	}

	@Path(login = true, path = "thread/deadlock", access = "access.config.admin", oplog = true)
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
							sb2.append(e.toString()).append("\r");
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
