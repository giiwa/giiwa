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

import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.stream.Stream;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Node;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.net.mq.IStub;
import org.giiwa.net.mq.MQ;
import org.giiwa.net.mq.MQ.Mode;
import org.giiwa.net.mq.MQ.Request;

/**
 * The {@code Task} Class use for create a runnable distributed Task, and
 * includes schedule method. <br>
 * the sub task class can override getName, onExecute, onFinish;
 * 
 * <pre>
 * getName: default increase number, only can be scheduled one time for same name, 
 *              the later schedule will cancel the previous schedule if not running
 * onExecute: the main entry of the task
 * onFinish: will be invoked when finished the task
 * </pre>
 * 
 * <br>
 * all the task that scheduled by worker task, will be queued and executed by a
 * thread pool, the thread number was configured in giiwa.properties
 * "thread.number";
 * 
 * @author joe
 *
 */
public abstract class Task implements Runnable, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final String MQNAME = "task";

	/** The log. */
	public static Log log = LogFactory.getLog(Task.class);

	/** The stop. */
	protected boolean stopping = false;

	/** The who. */
	private transient Thread who;

	private Map<String, Object> _attached = null;

	private long ms = 0; // schedule ms

	/** The fast. */
	private transient boolean fast;

	private static AtomicLong seq = new AtomicLong(0);

	private transient long delay = -1;

	private transient long _cpuold = 0;

	private transient long duration = -1;
	private int runtimes = 0;

	private transient String _t; // the type, "S": sys, "G": global, ""

	private transient long startedtime = 0;
	private transient long scheduledtime = 0;
	private String parent;
	private static Configuration conf = null;
	private static boolean _inited = false;

	private transient Lock _door;
	private transient ScheduledFuture<?> sf;

	private transient LiveHand finished;

//	public transient float cpu; // 临时变量， CPU耗用

	/**
	 * global cores
	 */
	public static int cores = 1;

	private transient Exception e;

	public enum State {
		running, pending, finished
	};

	public String getTrace() {
		StringBuilder sb = new StringBuilder();
		this.onDump(sb);
		sb.append("\r\nwho=" + who);
		sb.append("\r\n").append(X.toString(e));
		return sb.toString();
	}

	public Object attach(String name) {
		return _attached == null ? null : _attached.get(name);
	}

	public Task attach(String name, Object value) {
		if (_attached == null) {
			_attached = new HashMap<String, Object>();
		}
		_attached.put(name, value);
		return this;
	}

	public String get_t() {
		return _t;
	}

	public String getParent() {
		return parent;
	}

	public int getPriority() {
		return Thread.NORM_PRIORITY;
	}

	/**
	 * set the result and notify
	 * 
	 * @param t
	 */
	protected void result(Object t) {

		if (t instanceof Serializable) {

			attach("result", t);
			if (finished != null) {
				finished.release();
			}
		}
	}

	/**
	 * run the prepare and wait the result
	 * 
	 * @param <T>     the SubClass of Task
	 * @param prepare the pre-task
	 * @return the Object
	 */

	@SuppressWarnings("unchecked")
	public <T> T wait(Runnable prepare) {

		try {
			finished = LiveHand.create(X.AMINUTE, 1, 0);
			if (finished.tryLock()) {
				try {
					return (T) attach("result");
				} finally {
					finished.release();
				}
			}

			if (prepare != null) {
				prepare.run();
			}

			if (finished != null) {
				if (finished.lock()) {
					finished.release();
					return (T) attach("result");
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	private State state = State.pending;

	public State getState() {
		return state;
	}

	public long getDelay() {

		if (State.running.equals(state)) {
			return delay;
		}
		return -1;

	}

	public int getRuntimes() {
		return runtimes;
	}

	public StringBuilder onDump(StringBuilder sb) {
		try {
			Field[] ff = this.getClass().getDeclaredFields();
			if (ff != null) {
				for (Field f : ff) {

					if ((f.getModifiers() & Modifier.PRIVATE) == 0) {
						f.setAccessible(true);
						sb.append(f.getName()).append("=").append(f.get(this)).append("\r\n");
					}

				}
			}

			sb.append("attached=").append(_attached);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return sb;
	}

	public long getRemain() {
		if (State.pending.equals(state)) {
			return scheduledtime - System.currentTimeMillis();
		}
		return 0;
	}

	public long getRuntime() {
		if (startedtime > 0) {
			return System.currentTimeMillis() - startedtime;
		}
		return 0;
	}

	public long getDuration() {
		return duration;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object.hashCode()
	 */
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object.equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Task) {
			String n1 = getName();
			String n2 = ((Task) obj).getName();
			return n1 != null && n1.equals(n2);
		}
		return super.equals(obj);
	}

	/**
	 * Gets the name.
	 * 
	 * @return the name
	 */
	protected transient String _name;

	/**
	 * the name of the task, default is "worker." + seq, only can be scheduled one
	 * time for same name
	 * 
	 * @return String of the name
	 */
	public String getName() {
		if (_name == null) {
			_name = parent + "." + seq.incrementAndGet();
		}
		return _name;
	}

	/**
	 * Interrupt able.
	 * 
	 * @return true, if successful
	 */
	public boolean interruptable() {
		return Boolean.TRUE;
	}

	/**
	 * the main entry of the task.
	 */
	public abstract void onExecute();

	/**
	 * called when the worker finished, either re-schedule or let's die.
	 */
	public void onFinish() {

	}

	public boolean isEnabled() {
		return true;
	}

	/**
	 * called when the worker stopped.
	 */
	public void onStop() {
		// do nothing, it will be die
		if (finished != null) {
			finished.release();
		}

	}

	/**
	 * On stop.
	 * 
	 * @param fast the fast
	 */
	final public void onStop(boolean fast) {

		onStop();

		if (log.isInfoEnabled())
			log.info(getName() + " is stoped");

		LocalRunner.remove(this);

	};

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object.toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Task [").append(getName()).append("]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable.run()
	 */
	final public void run() {

		if (stopping) {
			onStop(fast);
			return;
		}

		try {

			// prepare
			Thread.currentThread().setPriority(this.getPriority());

			state = State.running;

			startedtime = System.currentTimeMillis();
			delay = startedtime - scheduledtime;

			_cpuold = _cputime();

			String old = null;
			Thread who = Thread.currentThread();
			if (who != null) {
				old = who.getName();
				who.setName(this.getName());
				this.who = who;
			}

			boolean _run = false;

			/**
			 * ensure onExecute be executed
			 */
			try {
//				log.debug("running task " + this + ", token=" + attach("token"));

				if (X.isSame(_t, "G")) {
					// global
					if (this.tryLock()) {
						try {

							if (!LocalRunner._switch(this)) {
								return;
							}

							_run = true;

							runtimes++;
							onExecute();
						} finally {
							this.unlock();
						}
					}
				} else {
					if (!LocalRunner._switch(this)) {
						return;
					}

					_run = true;
					runtimes++;
					onExecute();
				}
			} finally {

				duration = System.currentTimeMillis() - startedtime;

				state = State.finished;
				LocalRunner.remove(this);

				sf = null;

				try {
					this.scheduledtime = 0;
					this.startedtime = 0;
					if (finished != null) {
						finished.release();
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

				if (who != null && !X.isEmpty(old)) {
					who.setName(old);
				}

				if (_run) {
					onFinish();
				}

			}

		} catch (Throwable e) {
			log.error(this.getClass().getName(), e);
		} finally {
			who = null;
		}

	}

	/**
	 * initialize the workertask.
	 *
	 * @param usernum the thread num
	 */
	public static void init(int usernum) {

		log.info("Task init ...");

		conf = Config.getConf();

		LocalRunner.init(usernum);

		_initMQ();

	}

	private static void _initMQ() {

		try {
			IStub st = new IStub(Task.MQNAME) {

				@Override
				public void onRequest(long seq, Request req) {

					try {

						Object o = req.get();
						String cmd = req.cmd;

						if (o instanceof Task) {

							Task t = (Task) o;

							if (log.isDebugEnabled()) {
								log.debug("got a task, t=" + t);
							}

							if (!t.isEnabled()) {
								return;
							}

							long ms = t.ms;

							String name = t.getName();
							Task t1 = Task.get(name);
							if (t1 != null) {

								if (t1.isRunning()) {
									return;
								}

								boolean b = t1.cancel();
								if (log.isInfoEnabled()) {

									log.info("[" + req.from + "] reschedule task [" + name + "], remain="
											+ t1.getRemain() + "ms, scheduled=" + ms + "ms, cancel=" + b);
								}

							}

//							t.attach("_from", req.from);

							LocalRunner.schedule(t, ms, true);

						} else {

							// get task state
							try {

//								log.warn("MQ1, task, from=" + req.from + ", cmd=" + cmd);

								if (X.isSame(cmd, "isrunning")) {

									String name = o.toString();
									boolean found = LocalRunner.isRunning(name);

									req.reply(Request.create().put(found));

								} else if (X.isSame(cmd, "isscheduled")) {

									String name = o.toString();

									boolean found = LocalRunner.isScheduled(name);

									req.reply(Request.create().put(found));

								} else if (X.isSame(cmd, "list")) {
//								GLog.applog.info("task", "got", from);

									Node n = Node.dao.load(Local.id());
									_Status s = new _Status(n);

									req.reply(Request.create().from(n.label).put(s));
								}

							} catch (Exception e) {
								log.error(e.getMessage(), e);
								GLog.applog.error("task", "mq", e.getMessage(), e);
							}
						}

					} catch (Throwable e) {
						// ignore
						if (log.isWarnEnabled())
							log.warn("ignore: " + e.getMessage());
					}
				}

			};

			st.bindAs(Mode.TOPIC);

			_inited = true;

			if (log.isInfoEnabled())
				log.info("bind mq[" + st.getName() + "]");

		} catch (Exception e) {

			// eat
//			log.error(e.getMessage(), e);

			Task.schedule(t -> {
				_initMQ();
			}, 3000);
		}

	}

	/**
	 * Stop all tasks.
	 *
	 * @param fast the fast
	 */
	final public boolean stop(boolean fast) {

		stopping = true;
		this.fast = fast;
		if (who != null) {

			if (interruptable()) {

				result(null); // killed

				// interrupt the thread which may wait a resource or timer;
				log.warn("stop task=" + this.getName());

				who.interrupt();

				// schedule the run a time to clear the resource
				onStop(fast);
				return true;
			}
		} else {
			log.warn("who is null, stop failed, name=" + this.getName());
			onStop(fast);
		}
		return false;
	}

	/**
	 * schedule the task by absolute time <br>
	 * the time can be:
	 * 
	 * <pre>
	 * 1, hh:mm
	 * 2, *:00 each hour
	 * </pre>
	 * 
	 * .
	 *
	 * @param time , hh:mm
	 * @return WorkerTask
	 */
	final public Task schedule(String time) {
		return schedule(time, false);
	}

	final public Task schedule(String time, boolean global) {

		try {
			if (time.startsWith("*")) {
				String[] ss = time.split(":");
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(System.currentTimeMillis());

				c.set(Calendar.MINUTE, X.toInt(ss[1], 0));
				c.set(Calendar.SECOND, 0);

				if (c.getTimeInMillis() <= System.currentTimeMillis()) {
					// next hour
					c.add(Calendar.HOUR_OF_DAY, 1);
				}
				return this.schedule(c.getTimeInMillis() - System.currentTimeMillis(), global);

			} else {
				String[] ss = time.split(":");

				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(System.currentTimeMillis());
				c.set(Calendar.HOUR_OF_DAY, X.toInt(ss[0], 0));
				c.set(Calendar.MINUTE, X.toInt(ss[1], 0));
				c.set(Calendar.SECOND, 0);

				if (c.getTimeInMillis() <= System.currentTimeMillis()) {
					// next hour
					c.add(Calendar.DAY_OF_MONTH, 1);
				}
				return this.schedule(c.getTimeInMillis() - System.currentTimeMillis(), global);
			}

		} catch (Throwable e) {
			log.error(this, e);
		}
		return this;
	}

	/**
	 * Schedule the local/global task.
	 *
	 * @param msec the milliseconds
	 * @return the worker task
	 */
	final public Task schedule(long msec) {
		return this.schedule(msec, false);
	}

	public Object reduce(Stream<Object> st, Function<Object, Object> func) {
		// TODO
		return null;
	}

	/**
	 * schedule a task
	 * 
	 * @param msec
	 * @param global true: global task, false: localtask
	 * @return
	 */
	final public synchronized Task schedule(long msec, boolean global) {

		try {

			if (stopping) {
				onStop(fast);
				return this;
			}

			this.parent = Thread.currentThread().getName();
			if (this.parent.length() > 30) {
				this.parent = this.parent.substring(0, 27) + "...";
			}

			if (global && _inited) {

				try {

//					this.attach("node", Local.id());
//					this.attach("ms", msec);
//					this.attach("g", true);
					this.ms = msec;

					MQ.Request r = MQ.Request.create().put(this);
					r.from = Node.dao.load(Local.id()).label;

					MQ.topic(Task.MQNAME, r);

				} catch (Throwable e) {
					log.error(e.getMessage(), e);

					// schedule a local
					LocalRunner.schedule(this, msec, global);
				}

			} else {
				LocalRunner.schedule(this, msec, global);
			}

		} catch (Throwable e) {
			log.error(this, e);
		}
		return this;
	}

	final public boolean isScheduled() {
		return LocalRunner.isScheduled(this);
	}

	final public boolean isRunning() {
		return LocalRunner.isRunning(this);
	}

	final public boolean cancel() {

		String name = this.getName();
		synchronized (LocalRunner.class) {
			if (LocalRunner.pendingQueue.containsKey(name)) {

				Task t = LocalRunner.pendingQueue.remove(name);

				if (t.sf != null) {
					if (!t.sf.cancel(true)) {
						log.warn("the task can not be canceled, task=" + t);
						return false;
					}
					t.sf = null;
				}
			}
		}
		return true;
	}

	public static Task[] schedule(Task[] tt, long ms, boolean global) {
		if (tt == null)
			return null;

		for (Task t : tt) {
			t.schedule(ms, global);
		}
		return tt;
	}

	/**
	 * create a task and schedule it now
	 * 
	 * @param cc the function
	 * @return The Task
	 */
	final public static Task schedule(Consumer<Task> cc) {
		return schedule(cc, 0);
	}

	/**
	 * create a task and schedule in ms
	 * 
	 * @param cc the function
	 * @param ms the delay time
	 * @return the Task
	 */
	final public static Task schedule(Consumer<Task> cc, long ms) {

		Task t = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				cc.accept(this);
			}

		};

		// local
		return t.schedule(ms);
	}

	final public static Task schedule(final Consumer<Task> cc, final boolean global) {

		Task t = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				cc.accept(this);
			}

		};

		// local
		return t.schedule(0, global);
	}

	final public static Task schedule(final String name, final Consumer<Task> cc, final boolean global) {

		Task t = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public String getName() {
				return name;
			}

			@Override
			public void onExecute() {
				cc.accept(this);
			}

		};

		// local
		return t.schedule(0, global);
	}

	/**
	 * Active thread.
	 * 
	 * @return the int
	 */
	public static int activeThread() {
		return LocalRunner.local.getActiveCount();
	}

	/**
	 * Idle thread.
	 * 
	 * @return the int
	 */
	public static int idleThread() {
		return LocalRunner.local.getPoolSize() - LocalRunner.local.getActiveCount();
	}

	/**
	 * Tasks in queue.
	 *
	 * @return the int
	 */
	public static int tasksInQueue() {
		return LocalRunner.pendingQueue.size();
	}

	private static int localTasks() {
		int n = 0;
		try {
			Task[] tt = LocalRunner.pendingQueue.values().toArray(new Task[LocalRunner.pendingQueue.size()]);
			for (Task t : tt) {
				if (X.isEmpty(t._t)) {
					n++;
				}
			}
		} catch (Throwable e) {
			// ignore
		}
		return n;
	}

	public static int tasksInQueue(String type) {
		return LocalRunner.tasksInQueue(type);
	}

	/**
	 * Tasks in running.
	 *
	 * @return the int
	 */
	public static int tasksInRunning() {
		return LocalRunner.tasksInRunning();
	}

	public static int tasksInRunning(String type) {
		return LocalRunner.tasksInRunning(type);
	}

	public static List<Task> getRunningTask(String type) {
		return LocalRunner.getRunningTask(type);
	}

	/**
	 * is sys task
	 * 
	 * @return
	 */
	protected boolean isSys() {
		return false;
	}

	public long getCosting() {
		if (who != null) {
			return (_cputime() - _cpuold) / 1000 / 1000; // ns->ms
		}
		return 0;
	}

	private long _cputime() {
		ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();
		if (who != null) {
			return tmxb.getThreadCpuTime(who.getId());
		}
		return 0;
	}

	/**
	 * get All the task including pending and running
	 * 
	 * @return List of Tasks
	 */
	public static List<Task> getAll() {

		List<Task> l2 = LocalRunner.getAll();

//		List<JSON> l1 = Shell.threads();
//
//		log.info("l1=" + l1);

//		for (Task t : l2) {
//			String name = t.getName();
//			for (JSON j1 : l1) {
//				String s1 = j1.getString("name");
//				if (name.startsWith(s1)) {
//					t.cpu = j1.getFloat("cpu");
//					log.info("name=" + name + ", s1=" + s1 + ", cpu=" + t.cpu);
//					break;
//				}
//			}
//		}

		Collections.sort(l2, new Comparator<Task>() {

			@Override
			public int compare(Task o1, Task o2) {
				if (X.isSame(o1.getName(), o2.getName()))
					return 0;
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		return l2;
	}

	/**
	 * get the Task by name
	 * 
	 * @param name the name, return null if not find
	 * @return Task
	 */
	public static Task get(String name) {
		return LocalRunner.get(name);
	}

	/**
	 * get the Thread who running the task
	 * 
	 * @return Thread
	 */
	public Thread getThread() {
		return who;
	}

	@SuppressWarnings("unused")
	private static void _recover() {

		Temp t1 = Temp.get("/_task");
		ObjectInputStream in = null;

		try {
			if (t1.getFile().exists()) {
				in = new ObjectInputStream(t1.getInputStream());
				Object o = in.readObject();
				while (o != null) {
					if (o instanceof Task) {
						Task t = (Task) o;
						if (!t.isScheduled()) {
							t.schedule((long) (X.AMINUTE * Math.random()), "G".equals(t._t));
						}
					}
					o = in.readObject();
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			X.close(in);
			t1.delete();
		}

	}

	/**
	 * run the task and wait the task complete
	 * 
	 * @param <T> the SubClass of Task
	 * @return The Task
	 */
	public <T> T startAndJoin(boolean global) {
		return wait(new Runnable() {

			@Override
			public void run() {
				Task.this.schedule(0, global);
			}

		});
	}

//	public void watch(Runnable r) {
//		Task.schedule(() -> {
//			wait(null);
//			r.run();
//		});
//	}

	public void watch(Consumer<Object> r) {
		Task.schedule(t -> {
			Object o = wait(new Runnable() {
				@Override
				public void run() {
					Task.this.schedule(0, true);
				}
			});
			r.accept(o);
		});
	}

	private static class LocalRunner {

		private static Log log = LogFactory.getLog(LocalRunner.class);

		/** The is shutingdown. */
		public static boolean isShutingdown = false;

		/**
		 * the max pending task size, default is 1w
		 */
		// public static int MAX_TASK_SIZE = 10000;

		static ScheduledThreadPoolExecutor sys;

		/** The executor. */
		static ScheduledThreadPoolExecutor local;

		/** The executor. */
		static ScheduledThreadPoolExecutor global;

//		private static Lock lock = new ReentrantLock();

		/** The pending queue. */
		private static HashMap<String, Task> pendingQueue = new HashMap<String, Task>();

		/** The running queue. */
		private static HashMap<String, Task> runningQueue = new HashMap<String, Task>();

		public synchronized static boolean remove(Task task) {
			Task t = pendingQueue.remove(task.getName());
			if (t != null) {
				if (t.sf != null)
					t.sf.cancel(true);
//				return true;
			}

			if (runningQueue.remove(task.getName()) != null)
				return true;

			return true;
		}

		public synchronized static boolean isRunning(String name) {
			return runningQueue.containsKey(name);
		}

		public synchronized static int tasksInRunning() {
			return runningQueue.size();
		}

		public synchronized static List<Task> getRunningTask(String type) {

			List<Task> l1 = new ArrayList<Task>();
			runningQueue.values().forEach(e -> {
				if (e._t.equals(type)) {
					l1.add(e);
				}
			});
			return l1;

		}

		public synchronized static int tasksInRunning(String type) {
			int[] n = { 0 };
			runningQueue.values().forEach(e -> {
				if (e._t.equals(type)) {
					n[0]++;
				}
			});
			return n[0];

		}

		public synchronized static int tasksInQueue(String type) {
			int[] n = { 0 };

			pendingQueue.values().forEach(e -> {
				if (e._t.equals(type)) {
					n[0]++;
				}
			});
			return n[0];
		}

		public synchronized static List<Task> getAll() {
			List<Task> l1 = new ArrayList<Task>();
			l1.addAll(LocalRunner.pendingQueue.values());
			l1.addAll(LocalRunner.runningQueue.values());
			return l1;
		}

		public synchronized static Task get(String name) {
			Task t = LocalRunner.runningQueue.get(name);
			if (t != null)
				return t;

			return LocalRunner.pendingQueue.get(name);
		}

		public synchronized static boolean isScheduled(Task t) {

			if (runningQueue.containsKey(t.getName())) {
				return true;
			}

			if (pendingQueue.containsKey(t.getName())) {
				return true;
			}

			return false;
		}

		public static synchronized boolean isRunning(Task t) {

			if (runningQueue.containsKey(t.getName())) {
				return true;
			}

			return false;
		}

		private synchronized static boolean _switch(Task task) {

			if (isShutingdown)
				return false;

			pendingQueue.remove(task.getName());

			if (runningQueue.containsKey(task.getName())) {
				// there is a copy is running
				log.info("run duplicated task:" + task.getName());
				return false;
			}

			runningQueue.put(task.getName(), task);
			// log.debug(getName() + " is running");
			return true;

		}

		public static void init(int usernum) {
			local = new ScheduledThreadPoolExecutor(usernum, new ThreadFactory() {

				AtomicInteger i = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread th = new Thread(r);
					th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
					th.setName("gi-user-" + i.incrementAndGet());
					return th;
				}

			});

			int n = Runtime.getRuntime().availableProcessors();
			cores = n * conf.getInt("global.turbo", 1);

			sys = new ScheduledThreadPoolExecutor(n, new ThreadFactory() {

				AtomicInteger i = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread th = new Thread(r);
					th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
					th.setName("gi-sys-" + i.incrementAndGet());
					th.setPriority(Thread.MAX_PRIORITY);
					return th;
				}

			});

			global = new ScheduledThreadPoolExecutor(cores, new ThreadFactory() {

				AtomicInteger i = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread th = new Thread(r);
					th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
					th.setName("gi-global-" + i.incrementAndGet());
					return th;
				}

			});

//			_recover();

		}

		public static synchronized boolean schedule(Task task, long ms, boolean global) {

			if (isShutingdown)
				return false;

			if (local == null)
				return false;

			String name = task.getName();

			if (!task.isSys()) {

				String forbidden = conf.getString("task.forbidden", X.EMPTY);

				if (!X.isEmpty(forbidden) && name.matches(forbidden)) {
					log.info("the task[" + name + "] is forbidden in this node");
					return false;
				}

				// local task
				if (Task.localTasks() > local.getCorePoolSize()) {
					log.error(
							"too many task less threads, pending=" + Task.tasksInQueue() + ", poolsize="
									+ local.getCorePoolSize() + ", pending=" + LocalRunner.pendingQueue,
							new Exception("the task will not be scheduled"));
					return false;
				}

			}

			// running
			if (runningQueue.containsKey(name)) {

				if (log.isDebugEnabled()) {
					log.warn("the task is running, ignored: " + name);
				}

				return false;
			}

			// scheduled
			if (pendingQueue.containsKey(name)) {
				if (log.isDebugEnabled()) {
					log.warn("the task is scheduled, ignored: [" + name + "]");
				}

				return false;
			}

			task.state = State.pending;

			if (task.scheduledtime <= 0) {
				task.scheduledtime = System.currentTimeMillis();
			}

			task.e = new Exception("lanuch trace");

			Task t1 = pendingQueue.put(name, task);
			if (t1 != null) {
				log.warn("ERROR, why here is a task? task=" + t1);
			}

			if (ms <= 0) {
				if (task.scheduledtime <= 0) {
					task.scheduledtime = System.currentTimeMillis();
				}
				task.startedtime = 0;
				if (task.isSys()) {
					task._t = "S";
					sys.execute(task);
				} else if (global) {
					task._t = "G";
					LocalRunner.global.execute(task);
				} else {
					task._t = "";
					local.execute(task);
				}
			} else {
				task.startedtime = 0;
				task.scheduledtime = System.currentTimeMillis() + ms;

				if (task.isSys()) {
					task._t = "S";
					task.sf = sys.schedule(task, ms, TimeUnit.MILLISECONDS);
				} else if (global) {

					task._t = "G";
					task.sf = LocalRunner.global.schedule(task, ms, TimeUnit.MILLISECONDS);

				} else {
					task._t = "";
					task.sf = local.schedule(task, ms, TimeUnit.MILLISECONDS);
				}
			}

			return true;

		}

		public synchronized static void stopAll(boolean fast) {
			isShutingdown = true;
			/**
			 * start another thread to terminate all the thread
			 */
			try {

				for (Task t : pendingQueue.values().toArray(new Task[pendingQueue.size()])) {
					t.stop(fast);
				}

//				Temp t1 = Temp.get("/_task");
//				ObjectOutputStream out = new ObjectOutputStream(t1.getOutputStream());
//
//				for (Task t : runningQueue.values().toArray(new Task[runningQueue.size()])) {
//					out.writeObject(t);
//					t.stop(fast);
//				}
//				out.close();

				TimeStamp ts = TimeStamp.create();

				while (runningQueue.size() > 0 && ts.pastms() < X.AMINUTE) {
					try {
						log.info("stoping, size=" + runningQueue.size() + ", running task=" + runningQueue);

						for (Task t : runningQueue.values().toArray(new Task[runningQueue.size()])) {
							t.stop(fast);
						}

						LocalRunner.class.wait(1000);
					} catch (InterruptedException e) {
					}
				}

			} catch (Exception e) {
				log.error("running list=" + runningQueue);
				log.error(e.getMessage(), e);
			}
		}

		public synchronized static boolean isScheduled(String name) {
			if (runningQueue.containsKey(name) || pendingQueue.containsKey(name)) {
				return true;
			}
			return false;
		}

	}

	public static void stopAll(boolean fast) {
		LocalRunner.stopAll(fast);
	}

	public boolean remove() {
		//
		stopping = true;
		this.stop(fast);

		return LocalRunner.remove(this);

	}

	/**
	 * lock the task avoid it run in other node
	 * 
	 * @return true if lock success, otherwise false
	 */
	public synchronized final boolean tryLock() {

		try {
			if (_door == null) {
				_door = Global.getLock("global.door." + getName());
			}
			boolean b = _door.tryLock();
			return b;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * unlock the task
	 */
	public synchronized final void unlock() {

		try {
			if (_door != null) {
				_door.unlock();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

//	@SuppressWarnings({ "unchecked", "rawtypes" })
//	public static <T, V, K> T mapreduce(Function<Object, V> map, Function<V, K> reduce, Function<List<K>, T> group)
//			throws Exception {
//
//		String name = Thread.currentThread().getName();
//
//		LiveHand door = LiveHand.create(cores);
//		List<K> l1 = new ArrayList<K>();
//
//		int i = 1;
//		V v = map.apply(null);
//		while (v != null) {
//
//			door.lock();
//
//			Task t1 = new MapreduceTask(name + "_" + (i++), reduce, v);
//
//			t1.watch(r1 -> {
//				if (r1 != null) {
//					l1.add((K) r1);
//				}
//				door.release();
//			});
//
//			v = map.apply(v);
//		}
//
//		door.await();
//
//		T t1 = group.apply(l1);
//		return t1;
//	}

	/**
	 * is running in global
	 * 
	 * @param name
	 * @return
	 */
	public static boolean isRunning(String name) {

		try {
			if (LocalRunner.runningQueue.containsKey(name)) {
				return true;
			}

			W q = Node.dao.query().and("giiwa", null, W.OP.neq);
			q.and("updated", System.currentTimeMillis() - Node.LOST, W.OP.gte);
			AtomicLong has = new AtomicLong(q.count());

			boolean[] found = new boolean[] { false };

			MQ.callTopic(Task.MQNAME, "isrunning", name, 5000, r -> {

				String from = r.from;
				try {

					boolean e = r.get();
					if (e) {
						found[0] = true;
						return true;
					}
				} catch (Exception e) {
					GLog.applog.error("task", "global", "from=" + from + ", error=" + e.getMessage(), e);
				}

				has.decrementAndGet();
				if (has.get() == 0) {
					return true;
				} else {
					return false;
				}

			});

			return found[0];

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("task", "isRunning", e.getMessage(), e);
		}

		return false;

	}

	/**
	 * 
	 * is running or scheduled in global
	 * 
	 * @param name task name
	 * @return
	 */
	public static boolean isScheduled(String name) {

		try {

			if (LocalRunner.isScheduled(name)) {
				return true;
			}

			W q = Node.dao.query().and("giiwa", null, W.OP.neq);
			q.and("updated", System.currentTimeMillis() - Node.LOST, W.OP.gte);
			AtomicLong has = new AtomicLong(q.count());

			boolean[] found = new boolean[] { false };

			MQ.callTopic(Task.MQNAME, "isscheduled", name, 5000, req -> {

				String from = req.from;
				try {

					boolean e = req.get();
					if (e) {
						found[0] = true;
						return true;
					}

				} catch (Exception e) {
					GLog.applog.error("task", "global", "from=" + from + ", error=" + e.getMessage(), e);
				}

				has.decrementAndGet();
				if (has.get() == 0) {
					return true;
				} else {
					return false;
				}

			});

			return found[0];

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("task", "isSchedule", e.getMessage(), e);
		}

		// 直接返回true， 防止消息服务器出现故障后， 任务被重复执行
		return true;

	}

	public static <E> void forEach(List<E> l1, Consumer<E> func) {
		forEach(l1, Math.min(cores, l1.size()), func);
	}

	public static <E> void forEach(List<E> l1, int numThreads, Consumer<E> func) {
		if (numThreads > 1) {
			l1.parallelStream().forEach(func);
		} else {
			l1.forEach(func);
		}
	}

	public static class _Status implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public int pending;
		public int running;
		public int cores;

		public List<_Config> list;

		public _Status(Node n) {

			pending = Task.tasksInQueue("G");
			running = Task.tasksInRunning("G");
			cores = Task.cores;

			list = X.asList(Task.getRunningTask("G"), e -> {
				Task t = (Task) e;
				return new _Config(n, t);
			});

		}

	}

	public static class _Config implements Serializable {

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

		public _Config(Node n, Task t) {

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

}
