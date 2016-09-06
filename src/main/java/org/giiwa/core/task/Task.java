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

import org.apache.commons.configuration.Configuration;
import org.apache.commons.logging.*;
import org.giiwa.core.bean.*;

/**
 * The {@code WorkerTask} Class use for create a runnable Task, and includes
 * schedule method. <br>
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
  private static Log                         log           = LogFactory.getLog(Task.class);

  /** The is shutingdown. */
  public static boolean                      isShutingdown = false;

  /** The executor. */
  private static ScheduledThreadPoolExecutor executor;

  /** The Configuration of the node */
  protected static Configuration             conf;

  private static Object                      lock          = new Object();

  /** The pending queue. */
  private static ArrayList<Task>             pendingQueue  = new ArrayList<Task>();

  /** The running queue. */
  private static ArrayList<Task>             runningQueue  = new ArrayList<Task>();

  /** The stop. */
  private boolean                            stop          = false;

  /** The sf. */
  private ScheduledFuture<?>                 sf;

  /** The who. */
  private Thread                             who;

  /** The fast. */
  protected boolean                          fast;

  /** The t. */
  private TimeStamp                          t             = new TimeStamp();

  private static AtomicLong                  seq           = new AtomicLong(0);

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
      _name = "tast." + seq.incrementAndGet();
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
   * On stop.
   * 
   * @param fast
   *          the fast
   */
  public void onStop(boolean fast) {
    if (log.isInfoEnabled())
      log.info(getName() + " is stoped");

    synchronized (lock) {
      pendingQueue.remove(this);
      runningQueue.remove(this);
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

    synchronized (lock) {
      pendingQueue.remove(this);

      if (runningQueue.contains(this)) {
        // there is a copy is running
        log.warn("run duplicated task:" + getName());
        return;
      }

      sf = null;

      runningQueue.add(this);
      // log.debug(getName() + " is running");
    }

    try {
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

            if (log.isDebugEnabled())
              log.debug(this.getClass() + " is running ..., delayed: " + t.past() + "ms, tasks:" + tasksInQueue()
                  + ", active:" + Task.activeThread() + ", idle:" + Task.idleThread());

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
        onExecute();
      } finally {
        synchronized (lock) {
          runningQueue.remove(this);

          onFinish();
        }
      }

      synchronized (t) {
        if (who != null) {
          who.setPriority(old);
        }
      }
    } catch (Throwable e) {
      log.error(e.getMessage(), e);

      // while (e != null) {
      // if (e instanceof OutOfMemoryError) {
      // WorkerTask.stopAll(true);
      // }
      // e = e.getCause();
      // }
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
   *          the thread num
   * @param _conf
   *          the _conf
   */
  public static void init(int threadNum, Configuration _conf) {
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
    conf = _conf;
  }

  /**
   * Stop all tasks.
   *
   * @param fast
   *          the fast
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
   * reschedule the workertask, same as schedule.
   *
   * @param msec
   *          the msec
   * @return WrokerTask
   */
  final public Task reschedule(long msec) {
    return schedule(msec);
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
   *          , hh:mm
   * @return WorkerTask
   */
  final public Task schedule(String time) {
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

  }

  /**
   * Schedule the worker task.
   *
   * @param msec
   *          the milliseconds
   * @return the worker task
   */
  final public Task schedule(long msec) {
    if (stop) {
      onStop(fast);
    } else {
      if (executor == null)
        return this;

      synchronized (lock) {
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

        t.set(System.currentTimeMillis() + msec);
        sf = executor.schedule(this, msec, TimeUnit.MILLISECONDS);
        pendingQueue.add(this);
      }
    }

    return this;
  }

  /**
   * Stop all.
   * 
   * @param fast
   *          the fast
   */
  public static void stopAll(final boolean fast) {

    isShutingdown = true;
    /**
     * start another thread to terminate all the thread
     */
    new Thread() {
      public void run() {
        try {
          synchronized (lock) {
            for (int i = pendingQueue.size() - 1; i > -1; i--) {
              Task t = pendingQueue.get(i);
              t.stop(fast);
            }

            for (int i = runningQueue.size() - 1; i > -1; i--) {
              Task t = runningQueue.get(i);
              t.stop(fast);
            }
          }

          while (runningQueue.size() > 0) {
            synchronized (lock) {
              try {
                log.info("stoping, size=" + runningQueue.size() + ", running task=" + runningQueue);

                for (int i = runningQueue.size() - 1; i > -1; i--) {
                  Task t = runningQueue.get(i);
                  t.stop(fast);
                }

                lock.wait(1000);
              } catch (InterruptedException e) {
              }
            }
          }

        } catch (Exception e) {
          log.debug("running list=" + runningQueue);
          log.error(e.getMessage(), e);
        } finally {
          System.exit(0);
        }
      }
    }.start();
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
    return executor.getQueue().size();
  }

  /**
   * create a Task from Runnable
   * 
   * @param r
   *          the runnable object
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

}
