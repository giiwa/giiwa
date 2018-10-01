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
import org.giiwa.core.base.Host;
import org.giiwa.core.bean.*;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;
import org.giiwa.mq.MQ.Mode;
import org.giiwa.mq.MQ.Request;
import org.hyperic.sigar.CpuPerc;
import org.giiwa.mq.Queue;

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

	/** The log. */
	private static Log log = LogFactory.getLog(Task.class);

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
	private transient long cost = -1;

	private long duration = -1;
	private int runtimes = 0;

	private long startedtime = 0;
	private String node;
	private transient Lock _door;

	private Serializable result;
	private transient Semaphore finished;

	private long scheduledtime = 0;

	public enum State {
		running, pending, finished
	};

	@SuppressWarnings("unchecked")
	public <T> T status(String name) {
		return (T) _params.get(name);
	}

	public void status(String name, Object value) {
		_params.put(name, value);
	}

	/**
	 * set the result and notify
	 * 
	 * @param t
	 */
	protected void result(Serializable t) {

		if (this.getGlobal()) {
			MQ.notify("task.result." + this.getName(), t);
		} else {
			result = t;
			if (finished != null) {
				finished.release();
			}
		}

	}

	/**
	 * run the prepare and wait the result
	 * 
	 * @param r
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T wait(Runnable prepare) {

		result = null;
		finished = new Semaphore(0);

		if (this.getGlobal()) {
			return MQ.wait("task.result." + this.getName(), Integer.MAX_VALUE, prepare);
		} else {
			try {
				if (prepare != null) {
					Task.create(prepare).schedule(0);
				}
				if (finished.tryAcquire(Integer.MAX_VALUE, TimeUnit.MILLISECONDS)) {
					finished.release();
					return (T) result;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return null;
	}

	private State state = State.pending;

	public State getState() {
		return state;
	}

	public String getNode() {
		return node;
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
		if (State.running.equals(state)) {
			return 0;
		} else {
			return scheduledtime - System.currentTimeMillis();
		}
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

	public long getCost() {
		return cost;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getName().hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
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
			if (this.getGlobal()) {
				_name = "task." + UID.uuid();
			} else {
				_name = "task." + seq.incrementAndGet();
			}
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
		// do nothing, it will be die
		if (finished != null) {
			finished.release();
		}
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
	 * @param fast
	 *            the fast
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
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Task [").append(getName()).append("]");
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	final synchronized public void run() {

		try {

			node = Local.id();

			LocalRunner.add(this);

			try {
				if (stop) {
					onStop(fast);
					return;
				}

				state = State.running;

				String name = getName();
				who = Thread.currentThread();
				if (who != null) {
					who.setName(name);
				}
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			}

			/**
			 * ensure onExecute be executed
			 */
			try {
				runtimes++;
				startedtime = System.currentTimeMillis();
				delay = startedtime - scheduledtime;

				_cpuold = _cputime();

				onExecute();
				duration = System.currentTimeMillis() - startedtime;
				cost = getCosting();

			} finally {

				node = null;
				state = State.finished;
				LocalRunner.remove(this);

				if (this.getGlobal()) {
					GlobalRunner.running.decrementAndGet();
				}

				if (_door != null) {
					_door.unlock();
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
	 * @param threadNum
	 *            the thread num
	 */
	public static void init(int threadNum) {
		LocalRunner.init(threadNum);
		GlobalRunner.init();
	}

	/**
	 * Stop all tasks.
	 *
	 * @param fast
	 *            the fast
	 */
	final public void stop(boolean fast) {
		stop = true;
		this.fast = fast;
		if (who != null) {
			if (interruptable()) {

				result(null); // killed

				// interrupt the thread which may wait a resource or timer;
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
	 * @param time
	 *            , hh:mm
	 * @return WorkerTask
	 */
	final public Task schedule(String time) {
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
	 * Schedule the worker task.
	 *
	 * @param msec
	 *            the milliseconds
	 * @return the worker task
	 */
	final public Task schedule(long msec) {

		try {
			if (stop) {
				onStop(fast);
			} else {
				state = State.pending;
				if (this.getGlobal()) {
					GlobalRunner.schedule(this, msec);
				} else {
					scheduledtime = 0;
					LocalRunner.schedule(this, msec);
				}
			}
		} catch (Throwable e) {
			log.error(this, e);
		}
		return this;
	}

	final public static <T> Task schedule(MyFunc2 cc, long ms) {
		Task t = new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				cc.call();
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
		return LocalRunner.executor.getActiveCount();
	}

	/**
	 * Idle thread.
	 * 
	 * @return the int
	 */
	public static int idleThread() {
		return LocalRunner.executor.getPoolSize() - LocalRunner.executor.getActiveCount();
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
	 * default is local task
	 * 
	 * @return
	 */
	public boolean getGlobal() {
		return false;
	}

	/**
	 * create a Task from Runnable.
	 * 
	 * the runnable object
	 * 
	 * @deprecated
	 * @param r
	 *            the runnable
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

		l1.addAll(LocalRunner.pendingQueue);
		l1.addAll(LocalRunner.runningQueue);
		l1.addAll(GlobalRunner.pendingqueue);

		List<Task> l2 = new ArrayList<Task>(l1);
		Collections.sort(l2, new Comparator<Task>() {

			@Override
			public int compare(Task o1, Task o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		return l2;
	}

	/**
	 * get the Task by name
	 * 
	 * @param name
	 *            the name, return null if not find
	 * @return Task
	 */
	public static Task get(String name) {
		Task[] tt = LocalRunner.runningQueue.toArray(new Task[LocalRunner.runningQueue.size()]);
		if (tt != null) {
			for (Task t : tt) {
				if (t == null)
					continue;
				if (X.isSame(name, t.getName())) {
					return t;
				}
			}
		}

		tt = LocalRunner.pendingQueue.toArray(new Task[LocalRunner.runningQueue.size()]);
		if (tt != null) {
			for (Task t : tt) {
				if (t == null)
					continue;
				if (X.isSame(name, t.getName())) {
					return t;
				}
			}
		}

		return null;
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
	 */
	public <T> T join() {
		return wait(new Runnable() {

			@Override
			public void run() {
				Task.this.schedule(0);
			}

		});
	}

	/**
	 * reduce to more task and wait the result
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static <T> T reduce(Task[] tt, boolean global) throws Exception {

		String queueName = "reduce.queue." + UID.id(UID.uuid());
		Queue<Serializable> q = MQ.create(queueName);

		List<String> slices = new ArrayList<String>();

		for (Task t : tt) {
			slices.add(t.getName());
			Reduce.create(queueName, t, global).schedule(0);
		}

		List<Serializable> l2 = new ArrayList<Serializable>();

		while (!slices.isEmpty()) {

			try {
				Object[] e = q.read(X.AMINUTE);

				log.debug("reduce.s=" + e[0]);

				Serializable v = (Serializable) e[1];
				slices.remove(e[0]);
				if (v != null) {
					l2.add(v);
				}
			} catch (Exception e) {
				// looking for the task exists ?
				log.warn("task=" + queueName + ", slices=" + slices);
			}

		}

		log.debug("l2=" + l2);

		return (T) l2;

	}

	/**
	 * reduce the task to more and run the func width the spited list<br/>
	 * the sub task's global is same as this <br/>
	 * 
	 * @param l1
	 * @param f
	 * @return
	 * @throws Exception
	 */
	public static <T, V> List<T> reduce(List<V> l1, MyFunc<T, V> func) throws Exception {

		String name = "reduce." + UID.id(UID.uuid());
		Task[] tt = new Task[l1.size()];
		for (int i = 0; i < l1.size(); i++) {
			final V s = l1.get(i);
			final int ii = i;
			tt[i] = new Task() {
				private static final long serialVersionUID = 1L;

				@Override
				public void onExecute() {
					T o = func.call(s);
					result((Serializable) o);
				}

				@Override
				public String getName() {
					return name + "." + ii;
				}
			};
		}

		return reduce(tt, true);
	}

	private static class Reduce extends Task {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Task t;
		boolean global;
		String queueName;

		static Reduce create(String master, Task t, boolean global) {
			Reduce r = new Reduce();
			r.t = t;
			r.queueName = master;
			r.global = global;
			return r;
		}

		@Override
		public void onExecute() {
			t.onExecute();
		}

		@Override
		public String getName() {
			return t.getName();
		}

		@Override
		public void onFinish() {
			t.onFinish();
			try {
				MQ.send(queueName, Request.create().from(t.getName()).put(t.result));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		@Override
		public void onStop() {
			t.onStop();
			try {
				MQ.send(queueName, Request.create().from(t.getName()).put(t.result));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}

		@Override
		public boolean getGlobal() {
			return global;
		}

	}

	private static class GlobalRunner extends IStub {

		private static Log log = LogFactory.getLog(GlobalRunner.class);

		static final String NAME = "global.runner";

		static PriorityQueue<Task> pendingqueue = new PriorityQueue<Task>(100, new Comparator<Task>() {

			@Override
			public int compare(Task o1, Task o2) {
				if (o2.scheduledtime == o1.scheduledtime) {
					return 0;
				}
				return o1.scheduledtime < o2.scheduledtime ? -1 : 1;
			}
		});

		private static Slot slot = null;
		private static GlobalRunner inst = new GlobalRunner();

		static final int TYPE_SCHEDULE = 1;
		static final int TYPE_RUNNING = 2;

		private static int cpu = 1;
		static AtomicInteger running = new AtomicInteger();
		private boolean inited = false;

		private List<Task> pending = null;

		public GlobalRunner() {
			super(NAME);
		}

		public synchronized static void init() {
			if (slot == null) {
				slot = new Slot();
				slot.schedule(0);
			}

			try {
				inst.bind(Mode.TOPIC);

				CpuPerc[] cc = Host.getCpuPerc();
				if (cc != null) {
					cpu = cc.length;
				}

				inst.inited = true;

				if (inst.pending != null) {
					for (Task t : inst.pending) {
						schedule(t, t.scheduledtime - System.currentTimeMillis());
					}

					inst.pending = null;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);

				Task.schedule(() -> {
					init();
				}, 3000);
			}
		}

		private void schedule() {
			slot.schedule(0);
		}

		@Override
		public void onRequest(long seq, Request req) {
			try {
				if (req.type == TYPE_SCHEDULE) {
					Task t = req.get();

					log.debug("got a task " + t);

					synchronized (pendingqueue) {
						pendingqueue.remove(t);
						pendingqueue.add(t);
					}

					schedule();
				} else if (req.type == TYPE_RUNNING) {
					Task t = req.get();
					pendingqueue.remove(t);
					// GLog.applog.info("globalrunner", "running", t.getName(), null, null);
				}
			} catch (Exception e) {
				// ignore the unrecognized task
			}
		}

		public synchronized static void schedule(Task task, long ms) {

			task.node = null;
			task.scheduledtime = System.currentTimeMillis() + ms;

			if (!inst.inited) {
				if (inst.pending == null) {
					inst.pending = new ArrayList<Task>();
					inst.pending.add(task);
				}
			}

			log.debug("schedule global task=" + task.getName());

			Request req = Request.create();
			req.put(task);
			req.type = TYPE_SCHEDULE;

			try {
				MQ.topic(NAME, req);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

		}

		static class Slot extends Task {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			private long interval;

			public String getName() {
				return NAME;
			}

			@Override
			public void onExecute() {

				interval = X.AMINUTE;

				try {
					Task t = null;
					synchronized (pendingqueue) {
						t = pendingqueue.peek();

						interval = t == null ? X.AMINUTE : (t.scheduledtime - System.currentTimeMillis());
						if (interval <= 0) {
							pendingqueue.remove(t);
						} else {
							t = null;
						}
					}

					if (t != null) {
						interval = 0;

						if (running.get() > cpu) {
							// slow down , lets others do more
							Thread.sleep(running.get() / cpu * 100);

						}

						if (t.tryLock()) {

							MQ.topic(NAME, Request.create().type(TYPE_RUNNING).put(t));

							running.incrementAndGet();

							t.node = Local.id();

							log.debug("schedule the global task=" + t);

							LocalRunner.schedule(t, 0);
						}
					}

				} catch (Exception e) {
					log.error(e.getMessage(), e);
					e.printStackTrace();
				}
			}

			@Override
			public void onFinish() {
				this.schedule(interval);
			}
		}

	}

	private static class LocalRunner {

		private static Log log = LogFactory.getLog(LocalRunner.class);

		/** The is shutingdown. */
		public static boolean isShutingdown = false;

		/**
		 * the max pending task size, default is 1w
		 */
		// public static int MAX_TASK_SIZE = 10000;

		/** The executor. */
		static ScheduledThreadPoolExecutor executor;

		private static Lock lock = new ReentrantLock();

		/** The pending queue. */
		static HashSet<Task> pendingQueue = new HashSet<Task>();

		/** The running queue. */
		static HashSet<Task> runningQueue = new HashSet<Task>();

		public static void remove(Task task) {
			try {
				lock.lock();
				pendingQueue.remove(task);
				runningQueue.remove(task);
			} finally {
				lock.unlock();
			}
		}

		public static void add(Task task) {
			if (isShutingdown)
				return;

			try {
				lock.lock();

				pendingQueue.remove(task);

				if (runningQueue.contains(task)) {
					// there is a copy is running
					log.warn("run duplicated task:" + task.getName());
					return;
				}

				runningQueue.add(task);
				// log.debug(getName() + " is running");
			} finally {
				lock.unlock();
			}
		}

		public static void init(int threadNum) {
			executor = new ScheduledThreadPoolExecutor(threadNum, new ThreadFactory() {

				AtomicInteger i = new AtomicInteger(1);

				@Override
				public Thread newThread(Runnable r) {
					Thread th = new Thread(r);
					th.setContextClassLoader(Thread.currentThread().getContextClassLoader());
					th.setName("task-" + i.incrementAndGet());
					return th;
				}

			});

		}

		public static void schedule(Task task, long ms) {
			if (isShutingdown)
				return;

			if (executor == null)
				return;

			try {
				lock.lock();
				// scheduled
				if (runningQueue.contains(task)) {
					if (log.isDebugEnabled())
						log.warn("the task is running, ignored: " + task.getName());

					return;
				}

				if (pendingQueue.contains(task)) {
					// schedule this task, possible this task is in running
					// queue, if so, while drop one when start this one in
					// thread
					pendingQueue.remove(task);
					// Exception e = new Exception("e");
					log.warn("reschedule the task:" + task.getName() + ", class=" + task.getClass().getName());
				}

				if (ms <= 0) {
					if (task.scheduledtime <= 0) {
						task.scheduledtime = System.currentTimeMillis();
					}
					task.startedtime = 0;
					executor.execute(task);
				} else {
					task.startedtime = 0;
					task.scheduledtime = System.currentTimeMillis() + ms;
					executor.schedule(task, ms, TimeUnit.MILLISECONDS);
				}
				pendingQueue.add(task);

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
					for (Task t : pendingQueue.toArray(new Task[pendingQueue.size()])) {
						t.stop(fast);
					}

					for (Task t : runningQueue.toArray(new Task[runningQueue.size()])) {
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

						for (Task t : runningQueue.toArray(new Task[runningQueue.size()])) {
							t.stop(fast);
						}

						waiter.awaitNanos(TimeUnit.MILLISECONDS.toNanos(1000));
					} catch (InterruptedException e) {
					} finally {
						lock.unlock();
					}
				}

			} catch (Exception e) {
				log.debug("running list=" + runningQueue);
				log.error(e.getMessage(), e);
			}
		}

	}

	public static void stopAll(boolean fast) {
		LocalRunner.stopAll(fast);
	}

	public boolean tryLock() {
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

}
