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

import org.apache.commons.logging.*;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Node;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.net.mq.MQ;
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

	long ms = 0; // schedule ms

	/** The fast. */
	private transient boolean fast;

	private static AtomicLong seq = new AtomicLong(0);

	private transient long delay = -1;

	private transient long _cpuold = 0;

	private transient long duration = -1;
	private int runtimes = 0;

	static final String SYSLOCAL = "S";
	static final String SYSGLOBAL = "SG";

	static final String GLOBAL = "G";
	transient String _t; // the type, "S": sys, "G": global, "": local

	transient long startedtime = 0;
	transient long scheduledtime = 0;
	private String parent;

	public boolean debug = false;

	transient Lock _door;
	transient ScheduledFuture<?> sf;

//	public transient float cpu; // 临时变量， CPU耗用

	State state = State.pending;
	boolean isrunning = false;

	/**
	 * global cores
	 */
	public static int cores = 1;
	public static int computingpower = 1;
	public static double ghz = 1;

	transient Exception e;

	public enum State {
		running, pending, finished, error, delayed
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
	 * @throws Exception
	 */
	protected void result(Object t) {

//		if (!X.isIn(_t, Task.GLOBAL, Task.SYSGLOBAL)) {
		// NOT global task
//			return;
//		}

		try {
			MQ.Request r = MQ.Request.create();
			if (t == null) {
				r.put(null);
			} else if (t instanceof Serializable) {
				r.put(t);
			} else {
				r.put(new Exception("the result is not serializable, class=" + t.getClass()));
			}
			r.cmd = "watch";
			r.from = this.getName();

			if (log.isDebugEnabled()) {
				log.debug("response cmd=" + r.cmd + ", from=" + r.from);
			}
			MQ.topic(Task.MQNAME, r);

			// fire local watch
			Runner.fireWatch(r.from, t);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("sys", "task", "result error", e);
		}

	}

	/**
	 * run the prepare and wait the result
	 * 
	 * @param <T>     the SubClass of Task
	 * @param prepare the pre-task
	 * @return the Object
	 */

	public State getState() {
		try {
//			String name = this.getName();
			if (state == State.running) {
//				if (Runner.runningQueue.containsKey(name)) {
				return State.running;
//				}
//				return State.error;
			} else if (state == State.pending) {
//				if (Runner.pendingQueue.containsKey(name)) {
				if (this.sf != null) {
					long delayed = this.sf.getDelay(TimeUnit.MILLISECONDS);
					if (delayed > 0) {
						return State.pending;
					} else {
						return State.delayed;
					}
				}
//				}
				return State.error;
			}
			return state;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return State.error;
		}
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
//		log.warn("onFinished: " + this.getName());
	}

	public boolean isEnabled() {
		return true;
	}

	public String getPool() {
		return _t;
	}

	/**
	 * On stop.
	 * 
	 * @param fast the fast
	 */
	final public void onStop(boolean fast) {

		log.warn(getName() + " is stoped");

		Runner.remove(this);

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
			// onstop will remove from pendingqueue
			onStop(fast);
			return;
		}

		if (!this.isSys() && Runner.pause) {
			String name = this.getName();
			log.warn("[" + name + "] was removed as pause.");
			Runner.pendingQueue.remove(name);
			return;
		}

		try {

			// prepare
			Thread.currentThread().setPriority(this.getPriority());

			startedtime = System.currentTimeMillis();
			delay = startedtime - scheduledtime;

			_cpuold = _cputime();

			/**
			 * ensure onExecute be executed
			 */

			if (debug || log.isDebugEnabled()) {
				log.info("running task [" + this.getName() + "], _t=" + _t + ", debug=" + debug);
			}

			if (X.isIn(_t, Task.GLOBAL, Task.SYSGLOBAL)) {
				// global
				if (this.tryLock(debug)) {
					try {

						if (!Runner._switch(this)) {
							return;
						}

						// to avoid killed
						sf = null;
						isrunning = true;
						state = State.running;

						runtimes++;

						/**
						 * send command to other node to kill task in queue
						 */
						try {
							Request r = Request.create().put(this.getName());
							r.cmd = "kill";
							MQ.topic(Runner.service.getName(), r);
						} catch (Throwable e) {
							// ignore
						}

						if (debug || log.isDebugEnabled()) {
							log.info("running [" + this.getName() + "], debug=" + debug);
						}

						String old = null;
						Thread who = Thread.currentThread();
						if (who != null) {
							old = who.getName();
							who.setName(this.getName());
							this.who = who;
						}
						try {
							onExecute();
						} finally {
							duration = System.currentTimeMillis() - startedtime;

							isrunning = false;
							state = State.finished;

							Runner.remove(this);

							this.scheduledtime = 0;
							this.startedtime = 0;

							if (!Thread.currentThread().isInterrupted()) {
								onFinish();
							} else {
								log.warn("interrupted: " + this.getName());
							}

							if (who != null && !X.isEmpty(old)) {
								who.setName(old);
							}

						}
					} finally {
						this.unlock();
					}
				} else {
					// can not get lock, running by other node
					// cleanup
					synchronized (Runner.pendingQueue) {
						Runner.pendingQueue.remove(this.getName());
					}

					if (debug || log.isDebugEnabled()) {
						log.info("can not got lock [" + this.getName() + "], debug=" + debug);
					}
				}

			} else {

				if (!Runner._switch(this)) {
					return;
				}

				if (debug || log.isDebugEnabled()) {
					log.info("running [" + this.getName() + "], debug=" + debug);
				}

				sf = null;
				isrunning = true;
				state = State.running;

				runtimes++;

				String old = null;
				Thread who = Thread.currentThread();
				if (who != null) {
					old = who.getName();
					who.setName(this.getName());
					this.who = who;
				}
				try {
					onExecute();
				} finally {

					duration = System.currentTimeMillis() - startedtime;

					isrunning = false;
					state = State.finished;

					Runner.remove(this);

					this.scheduledtime = 0;
					this.startedtime = 0;

					if (!Thread.currentThread().isInterrupted()) {
						onFinish();
					} else {
						log.warn("interrupted: " + this.getName());
					}
					if (who != null && !X.isEmpty(old)) {
						who.setName(old);
					}
				}
			}

		} catch (Throwable e) {
			log.error("failed: " + this.getName(), e);
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

		log.warn("Task init ... [" + usernum + "]");

		Runner.init(usernum);

		log.warn("Task inited.");

	}

	/**
	 * Stop all tasks.
	 *
	 * @param fast the fast
	 */
	@SuppressWarnings("deprecation")
	final public boolean stop(boolean fast) {

		// only set stopping here
		stopping = true;
		this.fast = fast;
		if (who != null) {

			if (interruptable()) {

				result(null); // killed

				// interrupt the thread which may wait a resource or timer;
				log.warn("stop task=" + this.getName());

//				who.interrupt(); //无法停止正在运行的任务
				who.stop();

				// schedule the run a time to clear the resource
				onStop(fast);
				return true;
			}
		} else {
			if (this.isRunning()) {
				log.warn("who is null, stop failed, name=" + this.getName());
			}

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

			if (msec < 0) {
				msec = 0;
			}

			if (global && Runner.inited) {

				try {

//					this.attach("node", Local.id());
//					this.attach("ms", msec);
//					this.attach("g", true);
					this.ms = msec;

					MQ.Request r = MQ.Request.create().put(this);
					r.from = Node.dao.load(Local.id()).label;

					MQ.topic(Task.MQNAME, r);

					if (debug || log.isDebugEnabled()) {
						log.info("send task [" + this.getName() + "] to MQ, debug=" + debug);
					}

				} catch (Throwable e) {
					log.error("schedule [" + this.getName() + "] failed!", e);

					// schedule a local
					Runner.schedule(this, msec, global);
				}

			} else {

				if (debug || log.isDebugEnabled()) {
					log.info("schedule [" + this.getName() + "] in local, debug=" + debug);
				}

				Runner.schedule(this, msec, global);
			}

		} catch (Throwable e) {
			log.error(this, e);
		}

		return this;
	}

	/**
	 * test in local node
	 * 
	 * @return
	 */
	final public boolean isScheduled() {
		return Runner.isScheduled(this);
	}

	/**
	 * test in local node
	 * 
	 * @return
	 */
	final public boolean isRunning() {
		return isrunning;
	}

	final synchronized public boolean cancel() {

		if (_door != null) {
			_door.unlock();
			_door = null;
		}

		if (sf != null) {
			if (!sf.cancel(true)) {
				log.warn("the task can not be canceled, task=" + this);
				return false;
			}
			sf = null;
			return true;
		}

		return false;

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

	final public static void schedule(String name, long ms) {
		try {
			MQ.Request r = MQ.Request.create().put(new Object[] { name, ms });
			r.cmd = "schedule";
			r.from = Node.dao.load(Local.id()).label;
			MQ.topic(Task.MQNAME, r);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	/**
	 * create a task and schedule in ms
	 * 
	 * @param cc the function
	 * @param ms the delay time
	 * @return the Task
	 */
	final public static Task schedule(Consumer<Task> cc, long ms) {

		Task t = _Task2.create(Thread.currentThread().getName() + "." + seq.incrementAndGet(), cc);

		// local
		return t.schedule(ms);
	}

	/**
	 * schedule a task to run the consumer<br>
	 * when reach the maxsize, then wait until a finished
	 * 
	 * @param maxsize max size for same task
	 * @param cc      consumer
	 * @return
	 * @throws Exception
	 */
	final public synchronized static Task schedule(int maxsize, Consumer<Task> cc) throws Exception {

		Class<?> c1 = cc.getClass();
		int n = Runner.number(c1);

		while (n > maxsize) {
			// wait
			Runner.await(1000);
			n = Runner.number(c1);
		}

		Task t = _Task2.create(Thread.currentThread().getName() + "." + seq.incrementAndGet(), cc);

		// local
		t.attach("cc", c1);
		return t.schedule(0);

	}

	final public static Task schedule(final Consumer<Task> cc, final boolean global) {
		Task t = _Task2.create(Thread.currentThread().getName() + "." + seq.incrementAndGet(), cc);
		// local
		return t.schedule(0, global);
	}

	final public static Task schedule(final String name, final Consumer<Task> cc, final boolean global) {

		Task t = _Task2.create(name, cc);

		// local
		if (global) {
			if (!Task.isScheduled(name)) {
				t.schedule(0, true);
			}
		} else if (!t.isScheduled()) {
			t.schedule(0);
		}

		return t;
	}

	/**
	 * Active thread.
	 * 
	 * @return the int
	 */
	public static int activeThread() {
		return Runner.local.getActiveCount();
	}

	/**
	 * Idle thread.
	 * 
	 * @return the int
	 */
	public static int idleThread() {
		return Runner.local.getPoolSize() - Runner.local.getActiveCount();
	}

	/**
	 * Tasks in queue.
	 *
	 * @return the int
	 */
	public static int tasksInQueue() {
		return Runner.pendingQueue.size();
	}

	public static int tasksDelay() {
		int n = 0;

		Object[] ss = null;
		synchronized (Runner.pendingQueue) {
			ss = Runner.pendingQueue.keySet().toArray();
		}

		for (Object name : ss) {
			Task t = Runner.pendingQueue.get(name);
			if (t != null && t.getRemain() < 0) {
				n++;
			}
		}
		return n;
	}

	static int numOfTasks() {
		return Runner.pendingQueue.size() + Runner.runningQueue.size();
	}

	public static int tasksInQueue(String... types) {
		return Runner.tasksInQueue(types);
	}

	/**
	 * Tasks in running.
	 *
	 * @return the int
	 */
	public static int tasksInRunning() {
		return Runner.runningQueue.size();

	}

	public static int tasksInRunning(String... types) {
		return Runner.tasksInRunning(types);
	}

	public static List<Task> getRunningTask(String... types) {
		return Runner.getRunningTask(types);
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

		List<Task> l2 = Runner.getAll();

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
		return Runner.get(name);
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
							t.schedule((long) (X.AMINUTE * Math.random()), GLOBAL.equals(t._t));
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

	public Task watch(Consumer<Object> r) {

		String name = this.getName();
		Runner.watch_map.put(name, r);

		return this;

	}

	public static void stopAll(boolean fast) {
		Runner.stopAll(fast);
	}

	public final boolean tryLock() {
		return tryLock(false);
	}

	/**
	 * lock the task avoid it run in other node
	 * 
	 * @return true if lock success, otherwise false
	 */
	public synchronized final boolean tryLock(boolean debug) {

		try {
			if (_door == null) {
				_door = Global.getLock("global.door." + getName(), debug);
			}
			boolean b = _door.tryLock();
			return b;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("sys", "lock", "failed, lock=" + getName(), e);
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
			GLog.applog.error("sys", "lock", "unlock failed, lock=" + getName(), e);
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
			if (Runner.runningQueue.containsKey(name)) {
				return true;
			}

			W q = Node.dao.query();
			q.and("lastcheck", System.currentTimeMillis() - Node.LOST, W.OP.gte);
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
				if (has.get() <= 0) {
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

		Task t = Runner.isScheduled(name);
		if (t != null && (t.isRunning() || t.getRemain() > 0)) {
			return true;
		}

		W q = Node.dao.query();
		q.and("lastcheck", System.currentTimeMillis() - Node.LOST, W.OP.gte);
		long count = -1;
		AtomicLong has = new AtomicLong(count);

		boolean[] found = new boolean[] { false };
		Set<String> fr = new HashSet<String>();

		try {
			// TODO,这个地方有问题
			count = q.count();
			if (count <= 1) {
				return false;
			}
			has.set(count);

			MQ.callTopic(Task.MQNAME, "ischeduled", name, 5000, req -> {

				String from = req.from;
				fr.add(from);

				try {

					has.decrementAndGet();

					boolean e = req.get();
					if (e) {
						found[0] = true;
//						log.info("from=" + from + ", name=" + name + ", found!");
						// 停止等待消息
						return true;
					}

				} catch (Exception e) {
					log.error("from=" + from + ", name=" + name, e);
					GLog.applog.error("task", "global", "from=" + from + ", error=" + e.getMessage(), e);
				}

				if (has.get() <= 0) {
					// 停止等待消息
					return true;
				} else {
					return false;
				}

			});

			if (found[0] && log.isDebugEnabled()) {
				log.debug("ischeduled, name=" + name + ", nodes=" + count + ", got=" + has);
			}
			return found[0];

		} catch (Exception e) {
			if (has != null && has.get() <= 0) {
				if (found[0] && log.isDebugEnabled()) {
					log.debug("ischeduled, name=" + name + ", nodes=" + count + ", got=" + has);
				}
				return found[0];
			}
			log.error("name=" + name + ", nodes=" + count + ", got=" + fr, e);
			GLog.applog.error("task", "isSchedule", e.getMessage() + ", got=" + fr, e);
		}

		// 直接返回false， 防止消息服务器出现故障后， 任务无法运行
		return false;

	}

	/**
	 * kill global/local task
	 * 
	 * @param name
	 */
	public static void kill(String name) {

		try {
			MQ.callTopic(Task.MQNAME, "kill", name, 0, null);
		} catch (Exception e) {
			log.error(name, e);
			GLog.applog.error("task", "isSchedule", e.getMessage(), e);
		}
	}

	/**
	 * 并发执行
	 * 
	 * @param <E>
	 * @param l1         数据列表
	 * @param numThreads 并发数
	 * @param global     true=全局
	 * @param func       执行代码
	 */
	public static <E> void forEach(List<E> l1, int parallel, boolean global, Consumer<E> func) {

		if (l1.size() > 1) {

			if (global) {
				final String threadname = Thread.currentThread().getName();
				final AtomicInteger seq = new AtomicInteger(1);

				List<Task> l2 = new ArrayList<Task>();
				for (E e : l1) {
					Task t = _Task1.create(threadname + "." + seq.incrementAndGet(), e, func);
					synchronized (l2) {
						try {
							while (l2.size() > parallel) {
								l2.wait(1000);
							}
						} catch (Exception err) {
							log.error(err.getMessage(), err);
						}
					}
					l2.add(t);
					t.watch(r -> {
						synchronized (l2) {
							l2.remove(t);
							l2.notifyAll();
						}
					});
					log.info("global parallel schedule.1 =" + t);
					t.schedule(0, true);
				}
				log.info("global parallel scheduled l2 =" + l2);

				synchronized (l2) {
					try {
						while (l2.size() > 0) {
							l2.wait(1000);
							log.info("global parallel, waiting finished, l2=" + l2);
							for (Task t : l2) {
								if (!Task.isScheduled(t.getName())) {
									log.info("global parallel, schduled.2 =" + t);
									t.schedule(0, true);
								}
							}
						}
					} catch (Exception err) {
						log.error(err.getMessage(), err);
					}
				}
			} else {

				int size = l1.size();
				AtomicInteger idx = new AtomicInteger(0);

				Task[] tt = new Task[Math.min(parallel, size)];
				for (int i = 0; i < tt.length; i++) {
					tt[i] = _Task2.create(Thread.currentThread().getName() + "." + i, t -> {
						int i2 = idx.getAndIncrement();
						while (i2 < size) {
							E e = l1.get(i2);
							try {
								func.accept(e);
							} catch (Exception err) {
								log.error(err.getMessage(), err);
							}
							i2 = idx.getAndIncrement();
						}
						synchronized (tt) {
							tt.notifyAll();
						}
					});
				}

				for (Task t : tt) {
					t.schedule(0);
				}

				synchronized (tt) {
					for (Task t : tt) {
						// waitfor
						try {
							while (t.isScheduled()) {
								tt.wait(1000);
							}
						} catch (Exception err) {
							log.error(err.getMessage(), err);
						}
					}
				}
			}
		} else {
			l1.forEach(func);
		}
	}

	/**
	 * 
	 * @param <E>
	 * @param l1
	 * @param parallel
	 * @param global
	 * @param func
	 */
	public static <E> void forEach(List<E> l1, int parallel, boolean global, IFactory<E> func) {

		if (l1.isEmpty()) {
			return;
		}

		if (l1.size() == 1) {
			Task t = func.create(l1.get(0));
			t.onExecute();
			return;
		}

		if (!global || parallel == 1) {

			// 非全局任务
			if (parallel > 1) {
				Task.forEach(l1, parallel, e -> {
					Task t = func.create(e);
					t.onExecute();
				});

			} else {
				l1.forEach(e -> {
					Task t = func.create(e);
					t.onExecute();
				});
			}

			return;
		}

		List<Task> l2 = new ArrayList<Task>();
		for (E e : l1) {

			synchronized (l2) {
				int n = 0;
				while (l2.size() < parallel) {
					try {
						l2.wait(1000);
					} catch (Exception err) {
						log.error(err.getMessage(), err);
					}
					n++;
					if (n > 10) {
						Task[] tt = l2.toArray(new Task[l2.size()]);
						for (Task t : tt) {
							if (!Task.isScheduled(t.getName())) {
								// 任务已经不存在了，但是没有获取到结果， 重新启动
								t.schedule(0, true);
							}
						}
					}
				}
			}

			Task t = func.create(e);
			t.watch(r -> {
				synchronized (l2) {
					if (log.isInfoEnabled()) {
						log.info("global parallel, removed, t=" + t);
					}
					l2.remove(t);
					l2.notifyAll();
				}
			});
			l2.add(t);
			t.schedule(0, true);

		}
		if (log.isDebugEnabled()) {
			log.debug("global parallel scheduled l2 =" + l2);
		}

		// 等所有结果执行完成
		synchronized (l2) {
			try {
				while (l2.size() > 0) {
					l2.wait(1000);
					if (log.isDebugEnabled()) {
						log.debug("global parallel, waiting finished, l2=" + l2);
					}
					for (Task t : l2) {
						if (!Task.isScheduled(t.getName())) {
							log.info("global parallel, schduled.2 =" + t);
							t.schedule(0, true);
						}
					}
				}
			} catch (Exception err) {
				log.error(err.getMessage(), err);
			}
		}
	}

	/**
	 * 并发执行，本地运行
	 * 
	 * @param <E>
	 * @param l1       数据队列
	 * @param parallel 并发数
	 * @param func     执行代码
	 */
	public static <E> void forEach(List<E> l1, int parallel, Consumer<E> func) {
		forEach(l1, parallel, false, func);
	}

	/**
	 * 并发执行，本地运行
	 * 
	 * @param <E>
	 * @param l1       数据队列
	 * @param parallel 并发数
	 * @param func     执行代码
	 */
	public static <E> void forEach(List<E> l1, int parallel, IFactory<E> factory) {
		forEach(l1, parallel, false, factory);
	}

	/**
	 * 并发执行，本地并发
	 * 
	 * @param <E>
	 * @param l1   数据队列
	 * @param func 执行代码
	 */
	public static <E> void forEach(List<E> l1, Consumer<E> func) {
		forEach(l1, l1.size(), false, func);
	}

	/**
	 * 并发执行
	 * 
	 * @param <E>
	 * @param l1     数据队列
	 * @param global true=全局
	 * @param func   执行代码
	 */
	@Deprecated
	public static <E> void forEach(List<E> l1, boolean global, Consumer<E> func) {
		forEach(l1, l1.size(), global, func);
	}

	public static void pause() {
		log.warn("Task was paused by:", new Exception());
		Runner.pause = true;
	}

	public static void resume() {
		log.warn("Task was resumed by:", new Exception());
		Runner.pause = false;
	}

	public String getSF() {
		return sf == null ? null : ("" + sf.isDone());
	}

	/**
	 * global task
	 * 
	 * @author joe
	 *
	 */
	static class _Task1 extends Task {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Object e;
		@SuppressWarnings("rawtypes")
		Consumer func;
		String name;

		@Override
		public String getName() {
			return this.name;
		}

		public static <E> _Task1 create(String name, E e, Consumer<E> func) {
			_Task1 t = new _Task1();
			t.name = name;
			t.e = e;
			t.func = func;
			return t;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onExecute() {
			try {
				func.accept(e);
				this.result(JSON.create().append("message", "ok"));
			} catch (Throwable err) {
				log.error(err.getMessage(), err);
				this.result(JSON.create().append("error", err.getMessage()));
			}
		}

	}

	/**
	 * local task
	 * 
	 * @author joe
	 *
	 */
	static class _Task2 extends Task {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		Object e;
		@SuppressWarnings("rawtypes")
		Consumer func;
		String name;

		@Override
		public String getName() {
			return this.name;
		}

		public static <E> _Task2 create(String name, Consumer<Task> func) {
			_Task2 t = new _Task2();
			t.name = name;
			t.func = func;
			return t;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void onExecute() {
			try {
				func.accept(this);
			} catch (Throwable err) {
				log.error(err.getMessage(), err);
			}
		}

	}

	public static interface IFactory<E> extends Serializable {
		public Task create(E e);
	}

}
