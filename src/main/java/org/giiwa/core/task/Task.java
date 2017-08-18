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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.*;
import org.giiwa.core.bean.*;

/**
 * The {@code Task} Class use for create a runnable Task, and includes schedule
 * method. <br>
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
 * all the task that scheduled by workertask, will be queued and executed by a
 * thread pool, the thread number was configured in giiwa.properties
 * "thread.number";
 * 
 * @author joe
 *
 */
public abstract class Task implements Runnable {

	/** The log. */
	private static Log log = LogFactory.getLog(Task.class);

	/** The is shutingdown. */
	public static boolean isShutingdown = false;

	/**
	 * the max pending task size, default is 1w
	 */
	public static int MAX_TASK_SIZE = 10000;

	/** The executor. */
	private static ScheduledThreadPoolExecutor executor;

	private static Lock lock = new ReentrantLock();
	// private static Condition door = lock.newCondition();

	/** The pending queue. */
	private static HashSet<Task> pendingQueue = new HashSet<Task>();

	/** The running queue. */
	private static HashSet<Task> runningQueue = new HashSet<Task>();

	/** The stop. */
	private boolean stop = false;

	/** The sf. */
	private ScheduledFuture<?> sf;

	/** The who. */
	private Thread who;

	/** The fast. */
	protected boolean fast;

	/** The t. */
	private TimeStamp t = new TimeStamp();

	private static AtomicLong seq = new AtomicLong(0);

	private long delay = -1;
	private long cost = -1;
	private int runtimes = 0;

	public enum State {
		running, pending
	};

	public State getState() {
		if (runningQueue.contains(this)) {
			return State.running;
		} else {
			return State.pending;
		}
	}

	public long getDelay() {
		return delay;
	}

	public int getRuntimes() {
		return runtimes;
	}

	public long getRemain() {
		return sf == null ? -1 : sf.getDelay(TimeUnit.MILLISECONDS);
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
	private transient String _name;

	/**
	 * the name of the task, default is "worker." + seq, only can be scheduled one
	 * time for same name
	 * 
	 * @return String of the name
	 */
	public String getName() {
		if (_name == null) {
			_name = "task." + seq.incrementAndGet();
		}
		return _name;
	}

	/**
	 * Interruptable.
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
	}

	/**
	 * called when the worker stopped.
	 */
	public void onStop() {
		// do nothing, it will be die
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

		try {
			lock.lock();
			pendingQueue.remove(this);
			runningQueue.remove(this);
		} finally {
			lock.unlock();
		}

	};

	/**
	 * Priority of the task, default is normal.
	 *
	 * @return the int
	 */
	public int priority() {
		return Thread.NORM_PRIORITY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		synchronized (t) {
			if (who != null) {
				sb.append(who.getName()).append("(").append(getName()).append(")").append(":").append(who.getState());
			} else
				sb.append("null").append("(").append(getName()).append(")");
		}
		return sb.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Runnable#run()
	 */
	final public void run() {
		int old = Thread.NORM_PRIORITY;
		try {

			try {
				lock.lock();

				pendingQueue.remove(this);

				if (runningQueue.contains(this)) {
					// there is a copy is running
					log.warn("run duplicated task:" + getName());
					return;
				}

				sf = null;

				runningQueue.add(this);
				// log.debug(getName() + " is running");
			} finally {
				lock.unlock();
			}

			try {
				if (stop) {
					onStop(fast);
					return;
				}

				String name = getName();
				synchronized (t) {
					who = Thread.currentThread();
					if (who != null) {
						who.setName(name);

						// if (log.isDebugEnabled()) {
						// log.debug(this.getClass() + " is running ..., delayed: " +
						// t.past() + "ms, tasks:" + tasksInQueue()
						// + ", active:" + Task.activeThread() + ", idle:" +
						// Task.idleThread());
						// }

						old = who.getPriority();
						who.setPriority(priority());
					}
				}
			} catch (Throwable e) {
				log.error(e.getMessage(), e);
			}

			/**
			 * ensure onExecute be executed
			 */
			try {
				delay = t.pastms();
				runtimes++;
				onExecute();
				cost = t.pastms() - delay;
			} finally {

				try {
					lock.lock();
					runningQueue.remove(this);

					onFinish();
				} finally {
					lock.unlock();
				}
			}

			synchronized (t) {
				if (who != null) {
					who.setPriority(old);
				}
			}

		} catch (Throwable e) {
			log.error(this.getClass().getName(), e);
		} finally {
			synchronized (t) {
				who = null;
			}
		}

	}

	/**
	 * initialize the workertask.
	 *
	 * @param threadNum
	 *            the thread num
	 */
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

	/**
	 * Stop all tasks.
	 *
	 * @param fast
	 *            the fast
	 */
	final public void stop(boolean fast) {
		stop = true;
		this.fast = fast;
		synchronized (t) {
			if (who != null) {
				if (interruptable()) {
					// interrupt the thread which may wait a resource or timer;
					who.interrupt();

					// schedule the run a time to clear the resource
					onStop(fast);
				}
			} else {
				onStop(fast);
			}
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
				if (executor == null)
					return this;

				try {
					lock.lock();
					// scheduled
					if (runningQueue.contains(this)) {
						if (log.isDebugEnabled())
							log.warn("the task is running, ignored: " + getName());

						return this;
					}

					if (pendingQueue.contains(this)) {
						// schedule this task, possible this task is in running
						// queue, if so, while drop one when start this one in
						// thread
						if (sf != null) {
							sf.cancel(false);
						}
						// Exception e = new Exception("e");
						log.warn("reschedule the task:" + getName());
					}

					// if (pendingQueue.size() > MAX_TASK_SIZE) {
					// log.error("too many tasks, pending=" + pendingQueue.size() + ",
					// ignore the task=" + this.getName());
					// }
					if (msec <= 0) {
						t.set(System.nanoTime());
						executor.execute(this);
					} else {
						t.set(System.nanoTime() + msec * 1000000);
						sf = executor.schedule(this, msec, TimeUnit.MILLISECONDS);
					}
					pendingQueue.add(this);

				} finally {
					lock.unlock();
				}
			}
		} catch (Throwable e) {
			log.error(this, e);
		}
		return this;
	}

	/**
	 * Stop all.
	 * 
	 * @param fast
	 *            the fast
	 */
	public final static void stopAll(final boolean fast) {

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

	/**
	 * Active thread.
	 * 
	 * @return the int
	 */
	public static int activeThread() {
		return executor.getActiveCount();
	}

	/**
	 * Idle thread.
	 * 
	 * @return the int
	 */
	public static int idleThread() {
		return executor.getPoolSize() - executor.getActiveCount();
	}

	/**
	 * Tasks in queue.
	 *
	 * @return the int
	 */
	public static int tasksInQueue() {
		return pendingQueue.size();
	}

	/**
	 * Tasks in running.
	 *
	 * @return the int
	 */
	public static int tasksInRunning() {
		return runningQueue.size();
	}

	/**
	 * create a Task from Runnable.
	 * 
	 * the runnable object
	 *
	 * @param r
	 *            the runnable
	 * @return Task
	 */
	public static Task create(final Runnable r) {
		Task t = new Task() {

			@Override
			public void onExecute() {
				r.run();
			}

		};
		return t;
	}

	/**
	 * Creates a Task with the name prefix and runnable.
	 *
	 * @param nameprefix
	 *            the name prefix
	 * @param r
	 *            the runnable
	 * @return the task
	 */
	public static Task create(final String nameprefix, final Runnable r) {
		Task t = new Task() {

			String s = nameprefix + "." + seq.incrementAndGet();

			@Override
			public String getName() {
				return s;
			}

			@Override
			public void onExecute() {
				r.run();
			}

		};
		return t;
	}

	/**
	 * get All the task including pending and running
	 * 
	 * @return List of Tasks
	 */
	public static List<Task> getAll() {
		HashSet<Task> l1 = new HashSet<Task>();

		try {
			lock.lock();
			l1.addAll(pendingQueue);
			l1.addAll(runningQueue);
		} finally {
			lock.unlock();
		}

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
		Task[] tt = runningQueue.toArray(new Task[runningQueue.size()]);
		if (tt != null) {
			for (Task t : tt) {
				if (X.isSame(name, t.getName())) {
					return t;
				}
			}
		}

		tt = pendingQueue.toArray(new Task[runningQueue.size()]);
		if (tt != null) {
			for (Task t : tt) {
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

}
