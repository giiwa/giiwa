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
import java.util.stream.Stream;

import org.apache.commons.logging.*;
import org.giiwa.core.bean.*;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.conf.Config;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.dle.JS;
import org.giiwa.core.json.JSON;
import org.giiwa.framework.bean.GLog;
import org.giiwa.mq.IStub;
import org.giiwa.mq.MQ;
import org.giiwa.mq.MQ.Mode;
import org.giiwa.mq.MQ.Request;
import org.giiwa.mq.Result;

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
	public static AtomicInteger globaltask = new AtomicInteger();

	private transient long delay = -1;

	private transient long _cpuold = 0;

	private long duration = -1;
	private int runtimes = 0;

	private long startedtime = 0;
	private String node;
	private transient Lock _door;
	private transient ScheduledFuture<?> sf;

	private Serializable result;
	private transient Semaphore finished;

	private long scheduledtime = 0;

	private String parent;

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

	public String getParent() {
		return parent;
	}

	/**
	 * set the result and notify
	 * 
	 * @param t
	 */
	protected void result(Serializable t) {

		if (this.getGlobal()) {

			MQ.notify("task.result." + this.getName(), t);
			// GlobalRunner.release();

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
	 * @param <T>     the SubClass of Task
	 * @param prepare the pre-task
	 * @return the Object
	 */

	@SuppressWarnings("unchecked")
	public <T> T wait(Runnable prepare) {

		try {
			if (finished.tryAcquire(0, TimeUnit.MILLISECONDS)) {
				try {
					return (T) result;
				} finally {
					finished.release();
				}
			}

			if (this.getGlobal()) {
				// status("mq.wait", "task.result." + this.getName());
				result = MQ.wait("task.result." + this.getName(), Integer.MAX_VALUE, prepare);
				if (finished != null) {
					finished.release();
				}
			} else if (prepare != null) {
				prepare.run();
			}

			if (finished != null) {
				if (finished.tryAcquire(Integer.MAX_VALUE, TimeUnit.MILLISECONDS)) {
					finished.release();
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
			if (this.getGlobal()) {
				_name = parent + "." + UID.uuid();
			} else {
				_name = parent + "." + seq.incrementAndGet();
			}
		}
		return _name;
	}

	/**
	 * the description of this task
	 * 
	 * @return
	 */
	public String getDescription() {
		return this.getName();
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
		if (this.getGlobal()) {
			// MQ.notify("task.result." + this.getName(), result);

			GlobalRunner.schedule(this, 0);

		} else if (finished != null) {
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

		if (stop) {
			onStop(fast);
			return;
		}

		try {

			if (!LocalRunner.add(this)) {
				return;
			}

			// prepare
			node = Local.id();
			who = Thread.currentThread();

			state = State.running;

			runtimes++;
			startedtime = System.currentTimeMillis();
			delay = startedtime - scheduledtime;

			_cpuold = _cputime();

			String old = null;
			if (who != null) {
				old = who.getName();
				who.setName(this.getName());
			}

			/**
			 * ensure onExecute be executed
			 */
			try {

				onExecute();

			} finally {

				duration = System.currentTimeMillis() - startedtime;

				node = null;
				state = State.finished;
				LocalRunner.remove(this);

				if (this.getGlobal()) {
					MQ.topic(GlobalRunner.NAME, Request.create().type(GlobalRunner.TYPE_DONE).put(this));
					this.unlock();
				}

				sf = null;

				try {
					this.scheduledtime = 0;
					this.startedtime = 0;
					if (this.getGlobal()) {

						GlobalRunner.release();
						MQ.notify("task.result." + this.getName(), result);

					} else if (finished != null) {
						finished.release();
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
		GlobalRunner.init();
		powerstate = Global.getInt("node." + Local.id(), 1);
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
		if (powerstate != 1 && !this.getSys()) {
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
	 * Schedule the worker task.
	 *
	 * @param msec the milliseconds
	 * @return the worker task
	 */
	final public Task schedule(long msec) {

		try {
			if (stop) {
				onStop(fast);
			} else {

				if (powerstate != 1 && !this.getSys()) {
					return this;
				}

				this.parent = Thread.currentThread().getName();

				if (this.getGlobal()) {
					GlobalRunner.schedule(this, msec);
				} else {
					LocalRunner.schedule(this, msec);
				}
			}
		} catch (Throwable e) {
			log.error(this, e);
		}
		return this;
	}

	final public boolean isScheduled() {

		if (this.getGlobal()) {
			return GlobalRunner.isScheduled(this);
		} else {
			return LocalRunner.isScheduled(this);
		}

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

	/**
	 * create a task and schedule in ms
	 * 
	 * @param cc the function
	 * @param ms the delay time
	 * @return the Task
	 */
	final public static Task schedule(TaskFunction cc, long ms) {
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
		return LocalRunner.user.getActiveCount();
	}

	/**
	 * Idle thread.
	 * 
	 * @return the int
	 */
	public static int idleThread() {
		return LocalRunner.user.getPoolSize() - LocalRunner.user.getActiveCount();
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
	 * @return the boolean, default false
	 */
	public boolean getGlobal() {
		return false;
	}

	/**
	 * is sys task
	 * 
	 * @return
	 */
	protected boolean getSys() {
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
			l1.addAll(LocalRunner.pendingQueue);
		} catch (Exception e) {

		}
		try {
			l1.addAll(LocalRunner.runningQueue);
		} catch (Exception e) {

		}

		try {
			l1.addAll(GlobalRunner.pendingqueue);
		} catch (Exception e) {

		}

		List<Task> l2 = new ArrayList<Task>(l1);
		while (l2.remove(null))
			;
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

	@SuppressWarnings("unchecked")
	public static <T, V> Result<Integer> mapreduce(Stream<V> l1, String reducecode, String cocode) throws Exception {
		return mapreduce(l1, e -> {
			try {
				Object o = JS.run(reducecode, JSON.create().append("arg", e));
				return (T) o;
			} catch (Exception e1) {
				log.warn(e1.getMessage(), e1);
				return null;
			}
		}, t -> {
			try {
				JS.run(cocode, JSON.create().append("arg", t));
			} catch (Exception e1) {
				log.warn(e1.getMessage(), e1);
			}
		});
	}

	public static <T, V> Result<Integer> mapreduce(List<V> l1, ReduceFunction<T, V> reducefunc,
			CollectionFunction<T> cofunc) throws Exception {
		return mapreduce(l1.stream(), reducefunc, cofunc);
	}

	/**
	 * reduce the task to more and run the func width the spited list<br>
	 * the sub task's global is same as this <br>
	 * @deprecated, please refer reduce(Stream, ReduceFunction, CollectionFunction)
	 * 
	 * @param <T>        The Result Class
	 * @param <V>        The Value Class
	 * @param l1         the stream
	 * @param reducefunc the reduce function
	 * @return the List of result
	 */
	public static <T, V> List<T> mapreduce(Stream<V> l1, ReduceFunction<T, V> reducefunc) {
		List<T> l2 = new ArrayList<T>();

		try {
			Result<Integer> name = mapreduce(l1, reducefunc, e -> {
				l2.add(e);
			});

			// log.debug("l2=" + l2);

			name.read(Long.MAX_VALUE);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return l2;
	}

	/**
	 * reduce the task to more and run the func width the spited list<br>
	 * the sub task's global is same as this <br>
	 * 
	 * @param <T>        The Result Class
	 * @param <V>        The Value Class
	 * @param l1         the Stream
	 * @param reducefunc the Function
	 * @param cofunc     the collection function
	 * @return the Result
	 */
	@SuppressWarnings("unchecked")
	public static <T, V> Result<Integer> mapreduce(Stream<V> l1, ReduceFunction<T, V> reducefunc,
			CollectionFunction<T> cofunc) {

		try {
			// if (this.getGlobal()) {
			// GlobalRunner.release();
			// }

			final String name = "reduce." + UID.id(UID.uuid());

			final String queueName = "reduce.queue." + UID.id(UID.uuid());
			final Result<Serializable> q = MQ.create(queueName);
			final Result<Integer> q1 = MQ.create("state." + name);

			final Map<String, Task> slices = (cofunc == null) ? null : new HashMap<String, Task>();

			AtomicInteger i = new AtomicInteger(0);
			l1.forEach(e -> {

				int ii = i.incrementAndGet();

				Task t = new Task() {
					private static final long serialVersionUID = 1L;

					@Override
					public void onExecute() {
						T o = reducefunc.call(e);
						result((Serializable) o);
					}

					@Override
					public String getName() {
						return name + "." + ii;
					}
				};

				if (slices != null)
					slices.put(t.getName(), t);

				t.status("parent", Thread.currentThread().getName());
				Reduce.create(queueName, t).schedule(0);

			});

			if (slices != null && !slices.isEmpty()) {
				while (!slices.isEmpty()) {

					// status("slices", slices);

					try {
						Object[] e = q.readObject(X.AMINUTE);

						String s1 = (String) e[0];
						if (log.isDebugEnabled())
							log.debug("reduce.s=" + s1);

						Serializable t = (Serializable) e[1];

						slices.remove(s1);

						cofunc.call((T) t);

					} catch (Exception e) {
						// looking for the task exists ?
						log.warn("task=" + queueName + ", slices=" + slices);
						// status("slices", slices);

						if (GlobalRunner.door.tryLock()) {
							try {
								for (Task t1 : slices.values()) {
									if (!GlobalRunner.pendingqueue.contains(t1)) {
										if (t1.tryLock()) {
											// the task was down, reschedule it
											t1.unlock();

											Reduce.create(queueName, t1).schedule(10);
										} else {
											log.warn("the task is locked, tast=" + t1);
										}
									} else {
										if (log.isDebugEnabled())
											log.debug("the task is pending, tast=" + t1);
									}
								}
							} finally {
								GlobalRunner.door.unlock();
							}
						}
					}

				}

				// done
				MQ.send("state." + name, Request.create().put(0));
			}

			return q1;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			return null;
		} finally {
			// if (this.getGlobal()) {
			// GlobalRunner.acquire();
			// }
			l1.close();
		}
	}

	private static class Reduce extends Task {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		Task t;
		String queueName;

		static Reduce create(String master, Task t) {
			Reduce r = new Reduce();
			r.t = t;
			r.queueName = master;
			t.status("parent", t.status("parent"));

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

			GlobalRunner.door.lock();
			try {
				MQ.send(queueName, Request.create().from(t.getName()).put(t.result));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				GlobalRunner.door.unlock();
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
			return true;
		}

		// @Override
		// public boolean interruptable() {
		// return Boolean.FALSE;
		// }

	}

	private static class GlobalRunner extends IStub {

		private static Log log = LogFactory.getLog(GlobalRunner.class);

		static final String NAME = "global.runner";

		static final Lock door = GlobalLock.create(NAME);

		// static List<Task> runningqueue = new ArrayList<Task>(100);
		static List<Task> pendingqueue = new ArrayList<Task>();

		private static Slot slot = null;
		private static GlobalRunner inst = new GlobalRunner();

		static final int TYPE_START = 1;
		static final int TYPE_SCHEDULE = 3;
		static final int TYPE_RUNNING = 4;
		static final int TYPE_DONE = 5;

		private static int mycpu = 1;

		private boolean inited = false;

		private List<Task> pending = null;

		public GlobalRunner() {
			super(NAME);
		}

		public static boolean isScheduled(Task task) {
			if (pendingqueue.contains(task)) {
				return true;
			}

			return LocalRunner.isScheduled(task);
		}

		private static Comparator<Task> sorter = new Comparator<Task>() {
			@Override
			public int compare(Task o1, Task o2) {
				if (o2.scheduledtime == o1.scheduledtime) {
					return 0;
				}
				return o1.scheduledtime < o2.scheduledtime ? -1 : 1;
			}
		};

		public synchronized static void init() {

//			if (!MQ.isConfigured())
//				return;

			if (slot == null) {
				slot = new Slot();
				slot.schedule(0);
			}

			try {
				inst.bind(Mode.TOPIC);

				mycpu = Runtime.getRuntime().availableProcessors();

				inst.inited = true;

				if (log.isDebugEnabled())
					log.debug("globalrunner is binded. init pending=" + inst.pending + ", cpu=" + mycpu);

				if (inst.pending != null) {

					for (Task t : inst.pending) {
						schedule(t, t.scheduledtime - System.currentTimeMillis());
					}
					inst.pending = null;

				}

//				Request r1 = Request.create();
//				r1.type = TYPE_START;
//
//				try {
//					MQ.topic(NAME, r1);
//				} catch (Exception e) {
//					log.error(e.getMessage());
//				}

			} catch (Exception e) {

//				log.error(e.getMessage(), e);

				Task.schedule(() -> {
					init();
				}, 3000);

			}
		}

		/**
		 * acquire a global cpu in pool
		 */
		private static void acquire() {
			globaltask.incrementAndGet();
		}

		/**
		 * release a global cpu to pool
		 */
		private static void release() {
			globaltask.decrementAndGet();
			inst.schedule();
		}

		private void schedule() {
			if (globaltask.get() < mycpu) {
				slot.schedule(0);
			}
		}

		@Override
		public void onRequest(long seq, Request req) {
			try {
				switch (req.type) {
				case TYPE_START: {
//					for (Task t : pendingqueue) {
//						Request r1 = Request.create();
//						r1.put(t);
//						r1.type = TYPE_SCHEDULE;
//
//						try {
//							MQ.topic(NAME, r1);
//						} catch (Exception e) {
//							log.error(e.getMessage(), e);
//						}
//					}
					break;
				}
				case TYPE_SCHEDULE: {
					Task t = req.get();

					if (!LocalRunner.isShutingdown) {
						if (log.isDebugEnabled())
							log.debug("got a schedule task=" + t);

						if (t == null) {
							log.warn("bad task=" + t);
						} else {

							// runningqueue.remove(t);

							int i = pendingqueue.indexOf(t);
							if (i > -1) {
								Task t1 = pendingqueue.get(i);
								if (t.equals(t1) && t.scheduledtime != t1.scheduledtime) {
									pendingqueue.remove(t);
									pendingqueue.add(t);
									Collections.sort(pendingqueue, sorter);
									schedule();
								}
							} else {
								pendingqueue.add(t);
								Collections.sort(pendingqueue, sorter);
								schedule();
							}
						}
					}
					break;
				}
				case TYPE_RUNNING: {
					Task t = req.get();

					if (log.isDebugEnabled())
						log.debug("got a running task " + t);
					if (t != null) {
						int i = pendingqueue.indexOf(t);
						if (i > -1) {
							Task t1 = pendingqueue.get(i);
							if (t.equals(t1) && t1.scheduledtime == t.scheduledtime) {
								pendingqueue.remove(t);
							}
						}

						// runningqueue.remove(t);
						// runningqueue.add(t);
					}
					break;
				}
				case TYPE_DONE: {
					Task t = req.get();

					if (log.isDebugEnabled())
						log.debug("got a done task " + t);

					int i = pendingqueue.indexOf(t);
					if (i > -1) {
						Task t1 = pendingqueue.get(i);
						if (t.equals(t1) && t1.scheduledtime == t.scheduledtime) {
							pendingqueue.remove(t);
						}
					}

					// i = runningqueue.indexOf(t);
					// if (i > 0) {
					// Task t1 = runningqueue.get(i);
					// if (t.equals(t1) && t1.scheduledtime == t.scheduledtime) {
					// runningqueue.remove(t);
					// }
					// }
					break;
				}
				}

			} catch (Exception e) {
				// ignore the unrecognized task
			}
		}

		public synchronized static void schedule(Task task, long ms) {

			// if (pendingqueue.contains(task))
			// return;

			try {
				synchronized (pendingqueue) {
					while (!pendingqueue.isEmpty()) {
						slot.schedule(0);
						pendingqueue.wait(1000);
					}
				}
			} catch (Exception e) {
				log.error(task.toString(), e);
			}

			task.node = null;
			task.scheduledtime = System.currentTimeMillis() + ms;
			task.state = Task.State.pending;

			if (!task._params.containsKey("parent"))
				task.status("parent", Local.id() + "/" + Thread.currentThread().getName());

			if (!inst.inited) {
				if (inst.pending == null) {
					inst.pending = new ArrayList<Task>();
				}
				inst.pending.add(task);
				if (log.isDebugEnabled())
					log.debug("adding to pending, task=" + task.getName() + ", pending=" + inst.pending);
				return;
			}

			if (log.isDebugEnabled())
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

		static class Slot extends SysTask {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			private long interval;

			public String getName() {
				return NAME;
			}

			@Override
			public synchronized void onExecute() {

				interval = X.AMINUTE;

				if (LocalRunner.isShutingdown)
					return;

				if (pendingqueue.isEmpty()) {
					return;
				}

				if (globaltask.get() >= mycpu) {

					Task t = pendingqueue.get(0);

					if (t != null && (t.scheduledtime < System.currentTimeMillis() + X.AMINUTE)) {
						// send to others
						Request req = Request.create();
						req.put(t);
						req.type = TYPE_SCHEDULE;

						try {
							MQ.topic(NAME, req);
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}

					return;
				}

				try {

					Task t = null;

					synchronized (pendingqueue) {
						if (!pendingqueue.isEmpty()) {
							t = pendingqueue.get(0);

							interval = (t == null) ? X.AMINUTE : (t.scheduledtime - System.currentTimeMillis());
							if (interval <= 1) {
								pendingqueue.remove(0);
								pendingqueue.notifyAll();
							} else {
								t = null;
							}
						}
					}

					if (t != null) {

						interval = 0;

						if (t.tryLock()) {
							if (LocalRunner.schedule(t, 0)) {
								GlobalRunner.acquire();

								MQ.topic(NAME, Request.create().type(TYPE_RUNNING).put(t));

								if (log.isDebugEnabled())
									log.debug("run the global task= " + t);

							} else {
								t.unlock();
								log.warn("what's wrong with the task= " + t);
							}
						} else {
							// else running by others, ignore
							if (log.isDebugEnabled())
								log.debug("locked, ignore the global task= " + t);
						}
					} else {
						// checking running task
						// for (Task t1 : runningqueue) {
						// if (t1.tryLock()) {
						// // the task die
						// t1.unlock();
						// GlobalRunner.schedule(t1, 0);
						// }
						// }
					}

				} catch (Exception e) {
					log.error(e.getMessage(), e);
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

		static ScheduledThreadPoolExecutor sys;

		/** The executor. */
		static ScheduledThreadPoolExecutor user;

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

		public static boolean isScheduled(Task t) {
			try {
				lock.lock();

				if (runningQueue.contains(t)) {
					return true;
				}

				if (pendingQueue.contains(t)) {
					return true;
				}

			} finally {
				lock.unlock();
			}

			return false;
		}

		private static boolean add(Task task) {
			if (isShutingdown)
				return false;

			try {
				lock.lock();

				pendingQueue.remove(task);

				if (runningQueue.contains(task)) {
					// there is a copy is running
					log.warn("run duplicated task:" + task.getName());
					return false;
				}

				runningQueue.add(task);
				// log.debug(getName() + " is running");
				return true;
			} finally {
				lock.unlock();
			}
		}

		public static void init(int sysnum, int usernum) {
			user = new ScheduledThreadPoolExecutor(usernum, new ThreadFactory() {

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

		}

		public static boolean schedule(Task task, long ms) {
			if (isShutingdown)
				return false;

			if (user == null)
				return false;

			if (Task.tasksInQueue() > user.getCorePoolSize()) {
				log.error("too many task less threads, pending=" + Task.tasksInQueue() + ", poolsize="
						+ user.getCorePoolSize(), new Exception("the task will not be scheduled"));
				GLog.applog
						.error("task", "schedule",
								"too many task less threads, pending=" + Task.tasksInQueue() + ", poolsize="
										+ user.getCorePoolSize(),
								new Exception("the task will not be scheduled"), null, null);
				task.onFinish();
				return false;
			}

			try {
				lock.lock();
				// scheduled

				if (runningQueue.contains(task)) {
					if (log.isDebugEnabled())
						log.warn("the task is running, ignored: " + task.getName());

					return false;
				}

				if (pendingQueue.contains(task)) {
					// schedule this task, possible this task is in running
					// queue, if so, while drop one when start this one in
					// thread
					pendingQueue.remove(task);
					if (task.sf != null) {
						if (!task.sf.cancel(false)) {
							return false;
						}
						task.sf = null;
					}
					log.warn("reschedule the task:" + task);
				}

				task.node = Local.id();
				task.result = null;
				task.finished = new Semaphore(0);
				task.state = State.pending;

				if (task.scheduledtime <= 0)
					task.scheduledtime = System.currentTimeMillis();
				if (!task._params.containsKey("parent"))
					task.status("parent", Local.id() + "/" + Thread.currentThread().getName());

				if (ms <= 0) {
					if (task.scheduledtime <= 0) {
						task.scheduledtime = System.currentTimeMillis();
					}
					task.startedtime = 0;
					if (task.getSys()) {
						sys.execute(task);
					} else {
						user.execute(task);
					}
				} else {
					task.startedtime = 0;
					task.scheduledtime = System.currentTimeMillis() + ms;

					if (task.getSys()) {
						task.sf = sys.schedule(task, ms, TimeUnit.MILLISECONDS);
					} else {
						task.sf = user.schedule(task, ms, TimeUnit.MILLISECONDS);
					}
				}

				pendingQueue.add(task);

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
				log.error("running list=" + runningQueue);
				log.error(e.getMessage(), e);
			}
		}

	}

	public static void stopAll(boolean fast) {
		LocalRunner.stopAll(fast);
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
		MQ.init();

		Task.init(100);

		try {

			TimeStamp t = TimeStamp.create();
			List<Integer> l1 = Task.mapreduce(Arrays.asList(1, 2, 3).stream(), e -> {
				// TODO
				return e * 10 + e / 2;
			});

			System.out.println("l1=" + l1 + ", cost=" + t.past());

			t.reset();
			l1 = Task.mapreduce(Arrays.asList(1, 2, 3).stream(), e -> {
				// TODO
				return e * 10 + e / 2 + e;
			});

			System.out.println("l1=" + l1 + ", cost=" + t.past());

			// Thread.sleep(X.AHOUR);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
