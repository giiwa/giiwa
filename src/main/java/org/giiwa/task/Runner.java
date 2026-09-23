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
import org.giiwa.bean.Disk;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Node;
import org.giiwa.cache.GlobalLock;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Helper;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.misc.Host;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ.Mode;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.task.Task.State;
import org.giiwa.web.Processing;

public final class Runner {

	private static Log log = LogFactory.getLog(Runner.class);

	/**
	 * 正中关闭
	 */
	public static boolean isShutingdown = false;

	/**
	 * the max pending task size, default is 1w
	 */
	// public static int MAX_TASK_SIZE = 10000;

	/**
	 * 本地系统线程池
	 */
	static ScheduledThreadPoolExecutor syslocal;

	/**
	 * 全局系统线程池
	 */
	static ScheduledThreadPoolExecutor sysglobal;

	/**
	 * 本地用户线程池
	 */
	static ScheduledThreadPoolExecutor local;

	/**
	 * 全局用户线程池
	 */
	static ScheduledThreadPoolExecutor global;

//	private static Lock door = new ReentrantLock();

	/**
	 * 等待队列
	 */
	static HashMap<String, Task> pendingQueue = new HashMap<String, Task>();

	/**
	 * 运行队列
	 */
	static HashMap<String, Task> runningQueue = new HashMap<String, Task>();

	/**
	 * CPU核心数量
	 */
	public static int cores = 1;

	/**
	 * CPU算力， 核心数 * 主频
	 */
	public static int computingpower = 1;

	/**
	 * CPU主频基数
	 */
	public static double ghz = 1;

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

	static void kill(String name) {

		synchronized (pendingQueue) {

			if (pendingQueue.containsKey(name)) {

				Task t = pendingQueue.remove(name);
				if (t != null) {

					if (log.isDebugEnabled()) {
						log.debug("removing task [" + name + "]");
					}

					if (t.sf != null) {
						t.sf.cancel(true);
					}
					t.stop(true);
				}
			}

			synchronized (runningQueue) {

				if (runningQueue.containsKey(name)) {

					Task t = runningQueue.remove(name);
					if (t != null) {

						if (log.isDebugEnabled()) {
							log.debug("killing task [" + name + "]");
						}

						if (t.sf != null) {
							t.sf.cancel(true);
						}

						t.stop(true);
					}
				}
			}
		}

	}

	/**
	 * 检测任务是否运行状态
	 * 
	 * @param name - 任务名称
	 * @return True - 正在运行
	 */
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

	/**
	 * 任务在运行状态的数量
	 * 
	 * @param types - 任务类型
	 * @return
	 */
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

			for (String name : runningQueue.keySet()) {
				l1.add(runningQueue.get(name));
			}

			l1.remove(null);

			return l1;
		}

	}

	/**
	 * 获取任务对象
	 * 
	 * @param name - 任务名称
	 * @return 任务对象
	 */
	public static Task get(String name) {
		Task t = Runner.runningQueue.get(name);
		if (t != null)
			return t;

		return Runner.pendingQueue.get(name);
	}

	/**
	 * 检测任务属否在被调度中，包括正在运行
	 * 
	 * @param t - 任务对象
	 * @return True - 正在本被调度中
	 */
	public static boolean isScheduled(Task t) {

		synchronized (pendingQueue) {

			if (runningQueue.containsKey(t.getName())) {
				return true;
			}

			return pendingQueue.containsKey(t.getName());

//			Task t1 = pendingQueue.get(t.getName());
//			if (t1 != null) {
//				if (t1.sf == null || t1.sf.isDone()) {
//					// 执行句柄丢失，或已经执行完成，从pending中移除
//					pendingQueue.remove(t.getName());
//				} else {
//					return true;
//				}
//			}
		}

//		return false;
	}

	/**
	 * 检测任务是否正在运行
	 * 
	 * @param t - 任务对象
	 * @return True - 正在运行
	 */
	public static boolean isRunning(Task t) {

		if (runningQueue.containsKey(t.getName())) {
			return true;
		}

		return false;
	}

	/**
	 * 切换一个任务的队列， 从pending -> running
	 * 
	 * @param task
	 * @return
	 */
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

			pendingQueue.notifyAll();

		}

		// log.debug(getName() + " is running");
		return true;

	}

	private static boolean _inited = false;

	@SuppressWarnings("deprecation")
	public static synchronized void init(int usernum) {

		if (_inited) {
			return;
		}

		log.warn("Task init ... [" + usernum + "]");

		_inited = true;
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

		Runner.cores = n * (conf != null ? conf.getInt("global.turbo", 1) : 1);
		Task.cores = Runner.cores;

		Runner.ghz = Host.getCpuGHz();
		Runner.computingpower = (int) (Runner.cores * Runner.ghz);

		syslocal = new ScheduledThreadPoolExecutor(n, new ThreadFactory() {

			AtomicInteger i = new AtomicInteger(1);

			@Override
			public Thread newThread(Runnable r) {
				Thread th = new Thread(r);
				th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
				th.setName("gi-syslocal-" + i.incrementAndGet());
				// 设置系统线程为最高优先级
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
				// 设置系统线程为最高优先级
				th.setPriority(Thread.MAX_PRIORITY);
				return th;
			}

		});

		global = new ScheduledThreadPoolExecutor(Runner.computingpower / 2, new ThreadFactory() {

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

		log.warn("Task inited.");

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
					/**
					 * 非系统任务，检查任务槽
					 */

					if (pause) {
						return false;
					}

					/**
					 * 是否本地禁止该类型的任务 ？
					 */
					String forbidden = conf.getString("task.forbidden", X.EMPTY);
					if (!X.isEmpty(forbidden) && name.matches(forbidden)) {
						log.warn("the task[" + name + "] is forbidden in this node");
						return false;
					}

					/**
					 * 本地任务队列 > 本地任务槽线程数 * 10
					 */
					if (pendingQueue.size() > local.getPoolSize() * 10) {
						// 如果队列大于 本地执行池的10倍
						if (task.interruptable()) {
							// 允许中断的线程
							throw new Exception("Too many task, task=" + task.getName() + ", pending="
									+ pendingQueue.size() + ", cores=" + local.getPoolSize());
						} else {
							// waiting ...
							// TODO, 有bug，可能会导致整个锁死
							// 放慢1分钟
							TimeStamp t = TimeStamp.create();
							while (pendingQueue.size() > local.getPoolSize() && t.pastms() < X.AMINUTE) {
//								synchronized (pendingQueue) {
								pendingQueue.wait(1000);
//								}
							}
						}
					}

				}

				// scheduled
				if (pendingQueue.containsKey(name)) {
					if (task.debug || log.isDebugEnabled()) {
						log.info("The task is scheduled, rescheduling [" + name + "]");
					}
				}

				task.state = State.pending;

				if (task.scheduledtime <= 0) {
					task.scheduledtime = Global.now();
				}

//				task.e = new Exception("lanuch trace");

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
					// 全局任务
					if (task.isSys()) {
						// 全局系统级， 使用全局系统槽来执行
						task._t = Task.SYSGLOBAL;
						task.sf = sysglobal.schedule(task, ms, TimeUnit.MILLISECONDS);
					} else {
						// 全局普通， 使用全局普通任务槽来执行
						task._t = Task.GLOBAL;
						task.sf = global.schedule(task, ms, TimeUnit.MILLISECONDS);
					}
				} else {
					if (task.isSys()) {
						// 本地系统级，使用本地系统任务槽来执行
						task._t = Task.SYSLOCAL;
						task.sf = syslocal.schedule(task, ms, TimeUnit.MILLISECONDS);
					} else {
						// 本地普通，使用本地普通任务槽来执行
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

	/**
	 * 检测任务是否被调度中，包括运行状态
	 * 
	 * @param name - 任务名称
	 * @return True - 被调度中
	 */
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

						} else if (X.isSame(cmd, X.LIST)) {
							// 任务列表
							Node n = Node.dao.load(Local.id());
							TaskStatus s = new TaskStatus(n);

							req.reply(Request.create().put(s));

						} else if (X.isSame(cmd, "list_lock")) {
							// 全局列表
							List<GlobalLock._Lock> l1 = GlobalLock.getLocks();
							req.reply(Request.create().put(l1));

						} else if (X.isSame(cmd, "kill") || X.isSame(cmd, "cacnel")) {
							// 取消一个任务, 如果还没运行
							String name = o.toString();
							Runner.remove(name, -1);

						} else if (X.isSame(cmd, "force")) {
							// 强制杀掉一个任务，即使在运行
							String name = o.toString();
							Runner.kill(name);

						} else if (X.isSame(cmd, "stat/read")) {
							// 数据库读性能监测
							String table = o.toString();
							req.reply(Request.create().put(Helper.Stat.read0(table)));

						} else if (X.isSame(cmd, "stat/write")) {
							// 数据库写性能监测
							String table = o.toString();
							req.reply(Request.create().put(Helper.Stat.write0(table)));

						} else if (X.isSame(cmd, "processing")) {
							// 请求任务列表
							List<JSON> l1 = Processing.getAll();
							req.reply(Request.create().put(l1));
						} else if (X.isSame(cmd, "disk/counter")) {
							// 文件仓库读写性能
							long id = X.toLong(o);
//							log.info("disk/counter reply:" + req.from + ", seq=" + req.seq);

							req.reply(Request.create()
									.put(new long[] { Disk.Counter.read(id).avg(), Disk.Counter.write(id).avg() }));
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

	/**
	 * 获取本地节点相同任务类型的数量
	 * 
	 * @param cc - 任务类型
	 * @return
	 */
	public static int number(String cc) {

		int n = 0;

		synchronized (pendingQueue) {
			for (Task t : pendingQueue.values()) {
				Object o = t._type;
				if (o != null && cc.equals(o)) {
					n++;
				}
			}
			for (Task t : runningQueue.values()) {
				Object o = t._type;
				if (o != null && cc.equals(o)) {
					n++;
				}
			}
		}

		return n;
	}

	public static void fireWatch(String name, Object o) {

		Consumer<Object> r = watch_map.remove(name);
		if (r != null) {
			r.accept(o);
		}

	}

	public static void await(long timeout) throws InterruptedException {
		synchronized (pendingQueue) {
			pendingQueue.wait(timeout);
		}
	}

	/**
	 * 定时巡检运行中队列任务 清理超时/卡死任务，中断并终止任务执行
	 */
	public static void checkAndKill() {

		Task[] taskArr = null;
		// 加锁读取运行中队列快照，缩短锁持有时长
		synchronized (pendingQueue) {
			if (!runningQueue.isEmpty()) {
				taskArr = runningQueue.values().toArray(new Task[runningQueue.size()]);
			}
		}

		// 遍历处理超时、卡死任务
		if (taskArr != null) {
			for (Task t : taskArr) {
				// 任务超时 或 任务卡死
				boolean expired = t.expired();
				boolean hung = t.isHunging();
				if (expired || hung) {

					GLog.applog.warn("sys", "kill",
							"kill task=" + t.getName() + ", expired=" + expired + ", hung=" + hung);

					// 取消底层调度任务
					if (t.sf != null) {
						t.sf.cancel(true);
					}
					// 强制终止当前任务
					t.stop(true);
				}
			}
		}
	}

}
