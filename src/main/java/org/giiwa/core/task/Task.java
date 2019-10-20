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
package org.giiwa.core.task;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.*;
import org.giiwa.core.bean.*;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.web.Language;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;
import org.giiwa.mq.MQ.Mode;
import org.giiwa.mq.MQ.Request;

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

	private static final String MQNAME = "task";

	/** The log. */
	public static Log log = LogFactory.getLog(Task.class);

	/**
	 * power state <br>
	 * 1: power on <br>
	 * 0: power off
	 */
	public static int powerstate = 1;

	/** The stop. */
	private transient boolean stop = false;

	/** The who. */
	private transient Thread who;

	private Map<String, Object> _params = new HashMap<String, Object>();

	/** The fast. */
	private boolean fast;

	private static AtomicLong seq = new AtomicLong(0);

	private transient long delay = -1;

	private transient long _cpuold = 0;

	private long duration = -1;
	private int runtimes = 0;

	private String _t; // the type, "S": sys, "G": global, ""
	private long startedtime = 0;
	private Serializable result;
	private long scheduledtime = 0;
	private String parent;

	private transient Lock _door;
	private transient ScheduledFuture<?> sf;

	private transient LiveHand finished;

	private transient Exception e;

	public enum State {
		running, pending, finished
	};

	public String getTrace() {
		StringBuilder sb = new StringBuilder();
		this.onDump(sb);
		sb.append("\r\n").append(X.toString(e));
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public <T> T status(String name) {
		return (T) _params.get(name);
	}

	public void status(String name, Object value) {
		_params.put(name, value);
	}

	public String get_t() {
		return _t;
	}

	public String getParent() {
		return parent;
	}

	/**
	 * set the result and notify
	 * 
	 * @param t
	 */
	protected void result(Serializable t) {

		result = t;
		if (finished != null) {
			finished.drop();
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
			finished = LiveHand.create(X.AMINUTE, 1, -1);
			if (finished.tryHold()) {
				try {
					return (T) result;
				} finally {
					finished.drop();
				}
			}

			if (prepare != null) {
				prepare.run();
			}

			if (finished != null) {
				if (finished.hold()) {
					finished.drop();
					return (T) result;
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

			sb.append("_params=").append(_params);

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

	/**
	 * called when the worker stopped.
	 */
	public void onStop() {
		// do nothing, it will be die
		if (finished != null) {
			finished.drop();
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

		if (stop) {
			onStop(fast);
			return;
		}

		try {

			if (!LocalRunner._switch(this)) {
				return;
			}

			// prepare

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

			/**
			 * ensure onExecute be executed
			 */
			try {
				runtimes++;
				onExecute();
			} finally {

				duration = System.currentTimeMillis() - startedtime;

				state = State.finished;
				LocalRunner.remove(this);

				sf = null;

				try {
					this.scheduledtime = 0;
					this.startedtime = 0;
					if (finished != null) {
						finished.drop();
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

				if (who != null && !X.isEmpty(old)) {
					who.setName(old);
				}

				onFinish();

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
		LocalRunner.init(8, usernum);

		powerstate = Global.getInt("node." + Local.id(), 1);

		_initMQ();

	}

	private static void _initMQ() {

		try {
			IStub st = new IStub(Task.MQNAME) {

				@Override
				public void onRequest(long seq, Request req) {

					Task t = req.get();
					if (t == null)
						return;

					String node = (String) t._params.get("node");
					if (X.isSame(node, Local.id())) {
						// ignore
						return;
					}

					long ms = (Long) t._params.get("ms");
					LocalRunner.schedule(t, ms, true);
				}

			};

			st.bind(Mode.TOPIC);

		} catch (Exception e) {
			Task.schedule(() -> {
				_initMQ();
			}, 3000);
		}

	}

	/**
	 * Stop all tasks.
	 *
	 * @param fast the fast
	 */
	final public void stop(boolean fast) {
		stop = true;
		this.fast = fast;
		if (who != null) {
			if (interruptable()) {

				result(null); // killed

				// interrupt the thread which may wait a resource or timer;
				log.warn("stop task=" + this.getName());

				who.interrupt();

				// schedule the run a time to clear the resource
				onStop(fast);
			}
		} else {
			onStop(fast);
		}
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
		if (powerstate != 1 && !this.isSys()) {
			return this;
		}

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
				return this.schedule(c.getTimeInMillis() - System.currentTimeMillis());
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
				return this.schedule(c.getTimeInMillis() - System.currentTimeMillis());
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

	/**
	 * schedule a task
	 * 
	 * @param msec
	 * @param global true: global task, false: localtask
	 * @return
	 */
	final public Task schedule(long msec, boolean global) {

		try {

			if (stop) {
				onStop(fast);
			} else {

				if (powerstate != 1 && !this.isSys()) {
					return this;
				}

				this.parent = Thread.currentThread().getName();
				if (Language.getLanguage() != null)
					this.parent = (String) Language.getLanguage().truncate(this.parent, 30);

				if (global) {

					try {

						this._params.put("node", Local.id());
						this._params.put("ms", msec);
						MQ.Request r = MQ.Request.create().put(this);
						MQ.topic(Task.MQNAME, r);

					} catch (Throwable e) {
						log.error(e.getMessage(), e);
					}

				}

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
	final public static Task schedule(TaskFunction cc) {
		return schedule(cc, 0);
	}

	final public static Task schedule(String name, TaskFunction cc) {
		return schedule(name, cc, 0);
	}

	final public static Task schedule(TaskFunction cc, long ms) {
		return schedule(null, cc, ms);
	}

	/**
	 * create a task and schedule in ms
	 * 
	 * @param cc the function
	 * @param ms the delay time
	 * @return the Task
	 */
	final public static Task schedule(String name, TaskFunction cc, long ms) {
		Task t = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public String getName() {
				if (X.isEmpty(name)) {
					return super.getName();
				} else {
					return name;
				}
			}

			@Override
			public void onExecute() {
				if (X.isEmpty(name)) {
					cc.call();
				} else {
					if (this.tryLock()) {
						try {
							cc.call();
						} finally {
							this.unlock();
						}
					}
				}
			}

		};
		return t.schedule(ms);
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

	/**
	 * Tasks in running.
	 *
	 * @return the int
	 */
	public static int tasksInRunning() {
		return LocalRunner.runningQueue.size();
	}

	/**
	 * is sys task
	 * 
	 * @return
	 */
	protected boolean isSys() {
		return false;
	}

	/**
	 * create a Task from Runnable.
	 * 
	 * the runnable object
	 * 
	 * @deprecated
	 * @param r the runnable
	 * @return Task
	 */
	public static Task create(final Runnable r) {
		Task t = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				r.run();
			}

		};
		return t;
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
		HashSet<Task> l1 = new HashSet<Task>();

		try {
			LocalRunner.lock.lock();
			l1.addAll(LocalRunner.pendingQueue.values());
			l1.addAll(LocalRunner.runningQueue.values());
		} catch (Exception e) {

		} finally {
			LocalRunner.lock.unlock();
		}

		List<Task> l2 = new ArrayList<Task>(l1);
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
		try {
			LocalRunner.lock.lock();

			Task t = LocalRunner.runningQueue.get(name);
			if (t != null)
				return t;

			return LocalRunner.pendingQueue.get(name);

		} finally {
			LocalRunner.lock.unlock();
		}
	}

	/**
	 * get the Thread who running the task
	 * 
	 * @return Thread
	 */
	public Thread getThread() {
		return who;
	}

	/**
	 * run the task and wait the task complete
	 * 
	 * @param <T> the SubClass of Task
	 * @return The Task
	 */
	public <T> T join() {
		return wait(new Runnable() {

			@Override
			public void run() {
				Task.this.schedule(0);
			}

		});
	}

	public void watch(Runnable r) {
		Task.schedule(() -> {
			wait(null);
			r.run();
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

		private static Lock lock = new ReentrantLock();

		/** The pending queue. */
		private static HashMap<String, Task> pendingQueue = new HashMap<String, Task>();

		/** The running queue. */
		private static HashMap<String, Task> runningQueue = new HashMap<String, Task>();

		public static boolean remove(Task task) {
			try {
				lock.lock();
				Task t = pendingQueue.remove(task.getName());
				if (t != null) {
					if (t.sf != null)
						t.sf.cancel(true);
					return true;
				}

				if (runningQueue.remove(task.getName()) != null)
					return true;
			} finally {
				lock.unlock();
			}
			return false;
		}

		public static boolean isScheduled(Task t) {
			try {
				lock.lock();

				if (runningQueue.containsKey(t.getName())) {
					return true;
				}

				if (pendingQueue.containsKey(t.getName())) {
					return true;
				}

			} finally {
				lock.unlock();
			}

			return false;
		}

		private static boolean _switch(Task task) {
			if (isShutingdown)
				return false;

			try {
				lock.lock();

				pendingQueue.remove(task.getName());

				if (runningQueue.containsKey(task.getName())) {
					// there is a copy is running
					log.warn("run duplicated task:" + task.getName());
					return false;
				}

				runningQueue.put(task.getName(), task);
				// log.debug(getName() + " is running");
				return true;
			} finally {
				lock.unlock();
			}
		}

		public static void init(int sysnum, int usernum) {
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

			sys = new ScheduledThreadPoolExecutor(sysnum, new ThreadFactory() {

				AtomicInteger i = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread th = new Thread(r);
					th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
					th.setName("gi-sys-" + i.incrementAndGet());
					return th;
				}

			});

			global = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactory() {

				AtomicInteger i = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread th = new Thread(r);
					th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
					th.setName("gi-global-" + i.incrementAndGet());
					return th;
				}

			});

		}

		public static boolean schedule(Task task, long ms, boolean global) {
			if (isShutingdown)
				return false;

			if (local == null)
				return false;

			String name = task.getName();

			String forbidden = Config.getConf().getString("task.forbidden", X.EMPTY);

			if (!X.isEmpty(forbidden) && name.matches(forbidden)) {
				log.info("the task[" + name + "] is forbidden in this node");
				return false;
			}

			if (Task.tasksInQueue() > local.getCorePoolSize()) {
				log.error(
						"too many task less threads, pending=" + Task.tasksInQueue() + ", poolsize="
								+ local.getCorePoolSize() + ", pending=" + LocalRunner.pendingQueue,
						new Exception("the task will not be scheduled"));
				return false;
			}

			try {
				lock.lock();
				// scheduled

				if (runningQueue.containsKey(name)) {
					if (log.isDebugEnabled())
						log.warn("the task is running, ignored: " + name);

					return false;
				}

				if (pendingQueue.containsKey(name)) {
					// schedule this task, possible this task is in running
					// queue, if so, while drop one when start this one in
					// thread

					log.info("reschedule the task:" + task);

					Task t = pendingQueue.remove(name);
					if (t.sf != null) {
						if (!t.sf.cancel(false)) {
							return false;
						}
						t.sf = null;
					}
//					task.runtimes = t.runtimes;
				}

				task.result = null;
				task.state = State.pending;

				if (task.scheduledtime <= 0)
					task.scheduledtime = System.currentTimeMillis();
//				if (!task._params.containsKey("parent"))
//					task.status("parent", Local.id() + "/" + Thread.currentThread().getName());

				task.e = new Exception("Trace");

				pendingQueue.put(name, task);

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
			} finally {
				lock.unlock();
			}

		}

		public static void stopAll(boolean fast) {
			isShutingdown = true;
			/**
			 * start another thread to terminate all the thread
			 */
			try {

				try {
					lock.lock();
					for (Task t : pendingQueue.values().toArray(new Task[pendingQueue.size()])) {
						t.stop(fast);
					}

					for (Task t : runningQueue.values().toArray(new Task[runningQueue.size()])) {
						t.stop(fast);
					}
				} finally {
					lock.unlock();
				}

				Condition waiter = lock.newCondition();
				while (runningQueue.size() > 0) {
					try {
						lock.lock();
						log.info("stoping, size=" + runningQueue.size() + ", running task=" + runningQueue);

						for (Task t : runningQueue.values().toArray(new Task[runningQueue.size()])) {
							t.stop(fast);
						}

						waiter.awaitNanos(TimeUnit.MILLISECONDS.toNanos(1000));
					} catch (InterruptedException e) {
					} finally {
						lock.unlock();
					}
				}

			} catch (Exception e) {
				log.error("running list=" + runningQueue);
				log.error(e.getMessage(), e);
			}
		}

	}

	public static void stopAll(boolean fast) {
		LocalRunner.stopAll(fast);
	}

	public boolean remove() {
		//
		stop = true;
		this.stop(fast);

		return LocalRunner.remove(this);

	}

	/**
	 * lock the task avoid it run in other node
	 * 
	 * @return true if lock success, otherwise false
	 */
	public final boolean tryLock() {
		try {
			if (_door == null) {
				_door = Global.getLock("global.door." + getName());
			}
			return _door.tryLock();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	/**
	 * unlock the task
	 */
	public final void unlock() {
		try {
			if (_door != null) {
				_door.unlock();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void main(String[] ss) {

		Config.init();
		Global.setConfig("site.group", "demo");

		Cache.init(null);

		Task.init(100);

		MQ.init();

		try {

			TimeStamp t = TimeStamp.create();

			t.reset();

			// Thread.sleep(X.AHOUR);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
