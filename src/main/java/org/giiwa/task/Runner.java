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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Node;
import org.giiwa.cache.GlobalLock;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.misc.Host;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ.Mode;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.task.Task.State;

public final class Runner {

	private static Log log = LogFactory.getLog(Runner.class);

	/** The is shutingdown. */
	public static boolean isShutingdown = false;

	/**
	 * the max pending task size, default is 1w
	 */
	// public static int MAX_TASK_SIZE = 10000;

	static ScheduledThreadPoolExecutor syslocal;

	static ScheduledThreadPoolExecutor sysglobal;

	/** The executor. */
	static ScheduledThreadPoolExecutor local;

	/** The executor. */
	static ScheduledThreadPoolExecutor global;

//	private static Lock door = new ReentrantLock();

	/** The pending queue. */
	static HashMap<String, Task> pendingQueue = new HashMap<String, Task>();

	/** The running queue. */
	static HashMap<String, Task> runningQueue = new HashMap<String, Task>();

	static Configuration conf = null;
	static boolean inited = false;

	static boolean remove(Task task) {

		try {
			synchronized (pendingQueue) {

				String name = task.getName();
				Task t = pendingQueue.remove(name);
				if (t != null) {
					if (!t.stopping) {
						log.error("bug, [" + t.getName() + "] in pendingqueue, sf=" + t.sf, new Exception());
					}
					if (t.sf != null) {
						t.sf.cancel(true);
					}
				}

				runningQueue.remove(name);
				pendingQueue.notifyAll();

			}
		} catch (Throwable e) {
			log.error("bug, [" + task.getName() + "]", e);
		}

		return true;

	}

	/**
	 * remove from pending queue only
	 * 
	 * @param name
	 * @return
	 */
	static boolean remove(String name, long ms) {

		boolean b = true;

		synchronized (pendingQueue) {

			if (pendingQueue.containsKey(name)) {

				Task t = pendingQueue.remove(name);
				if (t != null) {

					if (log.isDebugEnabled()) {
						log.debug("removing task [" + name + "], remain=" + t.getRemain() + ", ms=" + ms);
					}

					if (t.sf != null) {
						b = t.sf.cancel(false);
					}

					if (log.isDebugEnabled()) {
						log.debug("removed task [" + name + "], cancel=" + b);
					}
				}

				return b;
			}

			return true;
		}

	}

	public static boolean isRunning(String name) {
		return runningQueue.containsKey(name);
	}

	public static List<Task> getRunningTask(String... types) {

		synchronized (pendingQueue) {

			List<Task> l1 = new ArrayList<Task>();
			for (String name : runningQueue.keySet()) {
				Task t = runningQueue.get(name);

				for (String type : types) {
					if (t._t.equals(type)) {
						l1.add(t);
						break;
					}
				}
			}
			return l1;
		}

	}

	public static int tasksInRunning(String... types) {

		int n = 0;

		synchronized (pendingQueue) {

			for (String name : runningQueue.keySet()) {
				Task t = runningQueue.get(name);
				for (String type : types) {
					if (t._t.equals(type)) {
						n++;
						break;
					}
				}
			}
		}
		return n;

	}

	public static int tasksInQueue(String... types) {
		int n = 0;

		synchronized (pendingQueue) {

			for (String name : pendingQueue.keySet()) {
				Task t = pendingQueue.get(name);
				if (t != null) {
					for (String type : types) {
						if (t._t.equals(type)) {
							n++;
							break;
						}
					}
				}
			}

		}
		return n;
	}

	public static List<Task> getAll() {

		synchronized (pendingQueue) {

			List<Task> l1 = new ArrayList<Task>();
			for (String name : pendingQueue.keySet()) {
				l1.add(pendingQueue.get(name));
			}

			l1.remove(null);

			for (String name : runningQueue.keySet()) {
				l1.add(runningQueue.get(name));
			}

			l1.remove(null);

			return l1;
		}

	}

	public static Task get(String name) {
		Task t = Runner.runningQueue.get(name);
		if (t != null)
			return t;

		return Runner.pendingQueue.get(name);
	}

	public static boolean isScheduled(Task t) {

		if (runningQueue.containsKey(t.getName())) {
			return true;
		}

		synchronized (pendingQueue) {
			Task t1 = pendingQueue.get(t.getName());
			if (t1 != null) {
				if (t1.sf == null || t1.sf.isDone()) {
					pendingQueue.remove(t.getName());
				} else {
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isRunning(Task t) {

		if (runningQueue.containsKey(t.getName())) {
			return true;
		}

		return false;
	}

	static boolean _switch(Task task) {

		if (isShutingdown) {
			return false;
		}

		synchronized (pendingQueue) {
			String name = task.getName();
			pendingQueue.remove(name);

			if (runningQueue.containsKey(name)) {
				// there is a copy is running
				if (task.debug || log.isDebugEnabled()) {
					log.info("running duplicated task:" + name);
				}
				return false;
			}

			runningQueue.put(name, task);

		}

		// log.debug(getName() + " is running");
		return true;

	}

	public static void init(int usernum) {

		conf = Config.getConf();

		local = new ScheduledThreadPoolExecutor(usernum, new ThreadFactory() {

			AtomicInteger i = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread th = new Thread(r);
				th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
				th.setName("gi-local-" + i.incrementAndGet());
				return th;
			}

		});

		OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		int n = os.getAvailableProcessors();

		Task.cores = n * (conf != null ? conf.getInt("global.turbo", 1) : 1);
		Task.ghz = Host.getCpuGHz();
		Task.computingpower = (int) (Task.cores * Task.ghz);

		syslocal = new ScheduledThreadPoolExecutor(n, new ThreadFactory() {

			AtomicInteger i = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread th = new Thread(r);
				th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
				th.setName("gi-syslocal-" + i.incrementAndGet());
				th.setPriority(Thread.MAX_PRIORITY);
				return th;
			}

		});

		sysglobal = new ScheduledThreadPoolExecutor(n, new ThreadFactory() {

			AtomicInteger i = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread th = new Thread(r);
				th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
				th.setName("gi-sysglobal-" + i.incrementAndGet());
				th.setPriority(Thread.MAX_PRIORITY);
				return th;
			}

		});

		global = new ScheduledThreadPoolExecutor(Task.computingpower / 2, new ThreadFactory() {

			AtomicInteger i = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread th = new Thread(r);
				th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
				th.setName("gi-global-" + i.incrementAndGet());
				return th;
			}

		});

//		_recover();

		_initMQ();

	}

	static boolean pause = false;

	public static boolean schedule(Task task, long ms, boolean g) {

		String name = task.getName();

		try {
			if (isShutingdown)
				return false;

			if (local == null)
				return false;

//			_cleanup();

			synchronized (pendingQueue) {

				// running
				if (runningQueue.containsKey(name)) {
					log.warn("the task is running, ignored: " + name);
					return false;
				}

				if (!task.isSys()) {

					// not system task, check pause state
					if (pause) {
						return false;
					}

					String forbidden = conf.getString("task.forbidden", X.EMPTY);

					if (!X.isEmpty(forbidden) && name.matches(forbidden)) {
						log.info("the task[" + name + "] is forbidden in this node");
						return false;
					}

					// check number of task
					if (Task.numOfTasks() > local.getCorePoolSize() * 10) {
						// ignore
						log.error(
								"too many task, pending=" + Task.tasksInQueue() + ", poolsize="
										+ local.getCorePoolSize() + ", pending=" + Runner.pendingQueue.keySet(),
								new Exception("the task[" + name + "] will not be scheduled"));
						return false;
					}

				}

				// scheduled
				if (pendingQueue.containsKey(name)) {
					if (task.debug || log.isDebugEnabled()) {
						log.info("the task is scheduled, rescheduling [" + name + "]");
					}
				}

				task.state = State.pending;

				if (task.scheduledtime <= 0) {
					task.scheduledtime = Global.now();
				}

				task.e = new Exception("lanuch trace");

				Task old = pendingQueue.put(name, task);
				if (old != null && old != task) {
					if (old.sf != null) {
						old.sf.cancel(true);
						if (log.isInfoEnabled()) {
							log.info("cancel the old, task=" + name + ", remaining=" + old.getRemain() + "ms");
						}
					}
				}

				// 全局时差, 不用减去时差，因为 ms 只是 计划时间
//				ms -= System.currentTimeMillis() - Global.now();
//				if (ms < 0) {
//					ms = 0;
//				}

				task.startedtime = 0;
				task.scheduledtime = Global.now() + ms;

				if (g) {
					if (task.isSys()) {
						task._t = Task.SYSGLOBAL;
						task.sf = sysglobal.schedule(task, ms, TimeUnit.MILLISECONDS);
					} else {
						task._t = Task.GLOBAL;
						task.sf = global.schedule(task, ms, TimeUnit.MILLISECONDS);
					}
				} else {
					if (task.isSys()) {
						task._t = Task.SYSLOCAL;
						task.sf = syslocal.schedule(task, ms, TimeUnit.MILLISECONDS);
					} else {
						task._t = X.EMPTY;
						task.sf = local.schedule(task, ms, TimeUnit.MILLISECONDS);
					}
				}

				if (task.debug || log.isDebugEnabled()) {
					log.debug("scheduled task [" + name + "], ms=" + ms + ", debug=" + task.debug);
				}

			}

		} catch (Throwable e) {
			log.error("scheduled failed, task=" + name, e);
		}

		return true;

	}

//	private static void _cleanup() {
//		synchronized (pendingQueue) {
//			// why this, here has bug
//			for (Task t : pendingQueue.values().toArray(new Task[pendingQueue.size()])) {
//				if (t.sf == null || t.sf.isDone()) {
//					pendingQueue.remove(t.getName());
//					log.error("bug, t=" + t.getName() + ", startedtime="
//							+ Language.getLanguage().format(t.startedtime, "yyyy-MM-dd HH:mm:ss,S") + ", trace=\n"
//							+ t.getTrace());
//				}
//			}
//		}
//	}

	public static void stopAll(boolean fast) {

		isShutingdown = true;
		/**
		 * start another thread to terminate all the thread
		 */
		try {

			synchronized (pendingQueue) {

				Object[] tt = pendingQueue.values().toArray();

				for (Object t : tt) {
					if (t != null) {
						((Task) t).stop(fast);
					}
				}

				TimeStamp ts = TimeStamp.create();

				while (runningQueue.size() > 0 && ts.pastms() < 10000) {
					try {
						log.info("stoping, size=" + runningQueue.size() + ", running task=" + runningQueue);

						tt = pendingQueue.values().toArray();

						for (Object t : tt) {
							((Task) t).stop(fast);
						}

						pendingQueue.wait(1000);
					} catch (InterruptedException e) {
						// ignore
					}
				}
			}

		} catch (Exception e) {
			log.error("running list=" + runningQueue);
			log.error(e.getMessage(), e);
		}
	}

	public static Task isScheduled(String name) {
		synchronized (pendingQueue) {

			Task t = runningQueue.get(name);
			if (t == null) {
				t = pendingQueue.get(name);
				if (t != null) {
					if (t.sf == null || t.sf.isDone()) {
						pendingQueue.remove(name);
						t = null;
					}
				}
			}
			return t;
		}
	}

	private static void _initMQ() {

		try {

			service.bindAs(Mode.TOPIC);

			inited = true;

			if (log.isInfoEnabled())
				log.info("bind mq[" + service.getName() + "]");

		} catch (Exception e) {

			// eat
//			log.error(e.getMessage(), e);

			Task.schedule(t -> {
				_initMQ();
			}, 3000);
		}

	}

	static IStub service = new IStub(Task.MQNAME) {

		@Override
		public void onRequest(long seq, Request req) {

			try {

				Object o = null;
				try {
					o = req.get();
				} catch (Exception e) {
					// ignore
					// un-support object
					return;
				}

				String cmd = req.cmd;

				if (o instanceof Task) {

					Task t = (Task) o;

					String name = t.getName();

					if (!t.isEnabled()) {
						if (t.debug || log.isDebugEnabled()) {
							log.info("disabled, t=" + name);
						}
						return;
					} else if (t.debug || log.isDebugEnabled()) {
						log.info("got a task from[" + req.from + "], t=" + t.getName() + ", debug=" + t.debug);
					}

					long ms = t.ms;

					if (Runner.runningQueue.containsKey(name)) {
						if (log.isDebugEnabled()) {
							log.info("isrunning, t=" + t);
						}
						return;
					}

//					log.warn("got task: " + name + ", ms=" + ms);
					if (Runner.remove(name, ms)) {
						Runner.schedule(t, ms, true);
					}

				} else {

					// get task state
					try {

						if (log.isDebugEnabled()) {
							log.debug("MQ.task, from=" + req.from + ", cmd=" + cmd + ", o=" + o);
						}

						if (X.isSame(cmd, "isrunning")) {
							// 检测是否运行
							boolean found = false;
							try {
								String name = o.toString();
								found = Runner.isRunning(name);
							} finally {
								req.reply(Request.create().put(found));
							}

						} else if (X.isSame(cmd, "ischeduled")) {
							// 检测是否scheduled
							if (log.isDebugEnabled()) {
								log.debug("ischeduled, MQ.task, from=" + req.from + ", cmd=" + cmd + ", o=" + o);
							}

							boolean found = false;

							try {
								String name = o.toString();
								Task t = Runner.isScheduled(name);
								if (t != null && (t.isRunning() || t.getRemain() > 0)) {
									found = true;
								}
							} finally {
								req.reply(Request.create().put(found));
							}

						} else if (X.isSame(cmd, "schedule")) {
							// schedule一个任务
							Object[] oo = (Object[]) o;
							String name = oo[0].toString();
							long ms = X.toLong(oo[1]);

							Task t = Runner.isScheduled(name);
							if (t != null && !t.isRunning()) {
								t.schedule(ms, t._t.indexOf("G") > -1);
							}

						} else if (X.isSame(cmd, "watch")) {
							// 得到一个任务结果
							String name = req.from;
							// o
							fireWatch(name, o);

						} else if (X.isSame(cmd, "list")) {
							// 任务列表
							Node n = Node.dao.load(Local.id());
							TaskStatus s = new TaskStatus(n);

							req.reply(Request.create().put(s));

						} else if (X.isSame(cmd, "list_lock")) {
							// 全局列表
							List<GlobalLock._Lock> l1 = GlobalLock.getLocks();
							req.reply(Request.create().put(l1));

						} else if (X.isSame(cmd, "kill")) {
							// 杀掉一个任务
							String name = o.toString();
							Runner.remove(name, -1);

						} else {
							log.warn("error command [" + cmd + "] and task, from=" + req.from);
						}

					} catch (Exception e) {
						log.error(e.getMessage(), e);
						GLog.applog.error("task", "mq", e.getMessage(), e);
					}
				}

			} catch (Throwable e) {
				// ignore
				log.warn(e.getMessage(), e);
			}
		}

	};

	static Map<String, Consumer<Object>> watch_map = new HashMap<String, Consumer<Object>>();

	public static int number(Class<?> cc) {
		List<Task> l1 = Runner.getAll();
		int n = 0;

		for (Task t : l1) {
			Object o = t.attach("cc");
			n += (X.isSame(o, cc) ? 1 : 0);
		}

		return n;
	}

	public static void fireWatch(String name, Object o) {

		Consumer<Object> r = watch_map.remove(name);
		if (r != null) {
			r.accept(o);
		}

	}

	public synchronized static void await(long timeout) throws InterruptedException {
		synchronized (pendingQueue) {
			pendingQueue.wait(timeout);
		}
	}

}
