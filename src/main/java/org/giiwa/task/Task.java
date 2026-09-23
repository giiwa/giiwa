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

import java.io.IOException;
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
import org.giiwa.conf.Config;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.Stream;
import org.giiwa.dao.Helper.V;
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
	public final static Log log = LogFactory.getLog(Task.class);

	/**
	 * 停止
	 */
	protected transient volatile boolean stopping = false;

	/**
	 * 当前线程
	 */
	private volatile transient Thread who;

	/**
	 * 任务类别
	 */
	transient String _type;

	/**
	 * 临时附件
	 */
	private Map<String, Serializable> _attached = null;

	/**
	 * 计划时间
	 */
	long ms = 0; // schedule ms

	private static AtomicLong seq = new AtomicLong(0);

	/**
	 * 延时， 毫秒
	 */
	private volatile transient long delay = -1;

	/**
	 * CPU耗时
	 */
	private transient long _cpuold = 0;

	/**
	 * 运行时长，毫秒
	 */
	private transient long duration = -1;

	/**
	 * 任务运行时长，毫秒， 超过自动kill，-1=不超时
	 */
	public long timeout = -1;

	/**
	 * 运行时间， 可以被重置
	 */
	private long runningtime = Global.now();

	/**
	 * 重置运行时间， 重新计算超时
	 * 
	 * @return
	 */
	public Task reset() {
		runningtime = Global.now();
		return this;
	}

	/**
	 * 检测任务是否运行超时
	 * 
	 * @return - true=超时
	 */
	boolean expired() {
		if (timeout > 0 && Global.now() - runningtime > timeout) {
			return true;
		}
		return false;
	}

	/**
	 * 运行次数
	 */
	private int runtimes = 0;

	static final String SYSLOCAL = "S"; // sys
	static final String SYSGLOBAL = "SG"; // sys global
	static final String GLOBAL = "G"; // global

	/**
	 * 任务类型: "", S, G, SG: 普通，系统，全局，全局系统
	 */
	transient String _t; // the type, "":local, "S": sys, "G": global, "SG": global sys

	/**
	 * 开始运行时间
	 */
	volatile transient long startedtime = 0;

	/**
	 * 计划运行时间
	 */
	volatile transient long scheduledtime = 0;

	/**
	 * 父线程名称
	 */
	private String parent;

	/**
	 * 调试
	 */
	public volatile boolean debug = false;

	/**
	 * 全局锁
	 */
	volatile transient Lock _door;

	/**
	 * 任务结果数据
	 */
	volatile transient ScheduledFuture<?> sf;

	/**
	 * 任务状态
	 */
	volatile State state = State.pending;

	/**
	 * 任务状态: running, pending, finished, error, delayed
	 * 
	 * @author joe
	 *
	 */
	public static enum State {
		/**
		 * 运行
		 */
		running,

		/**
		 * 排队
		 */
		pending,

		/**
		 * 完成
		 */
		finished,

		/**
		 * 发生错误
		 */
		error,

		/** 延时 */
		delayed
	};

	/**
	 * 获取附件
	 * 
	 * @param name - 名称
	 * @return
	 */
	public Object attach(String name) {
		return _attached == null ? null : _attached.get(name);
	}

	/**
	 * 设置附件
	 * 
	 * @param name  - 名称
	 * @param value - 附件
	 * @return
	 */
	public Task attach(String name, Serializable value) {
		if (_attached == null) {
			synchronized (this) {
				if (_attached == null) {
					_attached = new ConcurrentHashMap<>();
				}
			}
		}
		if (value == null) {
			_attached.remove(name);
		} else {
			_attached.put(name, value);
		}
		return this;
	}

	/**
	 * 获取父线程名称
	 * 
	 * @return
	 */
	public String getParent() {
		return parent;
	}

	/**
	 * 线程优先级
	 * 
	 * @return
	 */
	public int getPriority() {
		// 默认普通优先级
		return Thread.NORM_PRIORITY;
	}

	/**
	 * 全局任务，回传结果数据
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
	 * 获取任务状态
	 * 
	 * @return the Object
	 */

	public State getState() {
		// 任务状态
		try {
			if (state == State.running) {
				return State.running;
			} else if (state == State.pending) {
				if (this.sf != null) {
					long delayed = this.sf.getDelay(TimeUnit.MILLISECONDS);
					if (delayed > 0) {
						return State.pending;
					} else {
						return State.delayed;
					}
				}
				return State.error;
			}
			return state;
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
			return State.error;
		}
	}

	/**
	 * 获取延时时长
	 * 
	 * @return
	 */
	public long getDelay() {

		if (State.running.equals(state)) {
			return delay;
		}
		return -1;

	}

	/**
	 * 获取运行次数
	 * 
	 * @return
	 */
	public int getRuntimes() {
		return runtimes;
	}

	/**
	 * 获取运行栈
	 * 
	 * @param sb
	 * @return
	 */
	public StringBuilder getState(StringBuilder sb) {
		try {
			Field[] ff = this.getClass().getDeclaredFields();
			if (ff != null) {
				for (Field f : ff) {
					if ((f.getModifiers() & Modifier.PRIVATE) == 0) {
						f.setAccessible(true);
						Object v = f.get(this);
						if (v != null) {
							if (v instanceof Map) {
								String s = JSON.fromObject(v).toString();
								if (s.length() > 20) {
									v = s.substring(0, 20) + "...";
								} else {
									v = s;
								}
							} else if (v instanceof List) {
								String s = v.toString();
								if (s.length() > 20) {
									v = s.substring(0, 20) + "...";
								} else {
									v = s;
								}
							} else if (v instanceof String) {
								String s = (String) v;
								if (s.length() > 20) {
									v = s.substring(0, 20) + "...";
								}
							}
						}
						sb.append(f.getName()).append("=").append(v).append("\r\n");
					}
				}
			}

			sb.append("attached=").append(_attached);
			sb.append("\r\ntimeout=" + timeout);
			sb.append("\r\nwho=" + who);
			sb.append("\t\n");

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return sb;
	}

	/**
	 * 获取剩余计划时长
	 * 
	 * @return 毫秒
	 */
	public long getRemain() {
		if (State.pending.equals(state)) {
			return scheduledtime - Global.now();
		}
		return 0;
	}

	/**
	 * 获取 运行时长 （总运行时长，包括CPU暂停时长）
	 * 
	 * @return 毫秒
	 */
	public long getRuntime() {
		if (startedtime > 0) {
			return Global.now() - startedtime;
		}
		return 0;
	}

	/**
	 * 获取运行时长
	 * 
	 * @return 毫秒
	 */
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
		if (this == obj) {
			return Boolean.TRUE;
		}

		if (obj instanceof Task) {
			String n1 = getName();
			String n2 = ((Task) obj).getName();
			return n1 != null && n1.equals(n2);
		}

		return super.equals(obj);
	}

	/**
	 * 任务名称
	 * 
	 * @return 字符串
	 */
	protected volatile String _name;

	/**
	 * 获取任务名称， 任务名称作为标识，在同一个任务槽中，需要保持唯一性
	 * 
	 * @return 字符串， 缺省自动增长
	 */
	public String getName() {
		if (_name == null) {
			synchronized (this) {
				if (_name == null) {
					_name = parent + "." + seq.incrementAndGet();
				}
			}
		}
		return _name;
	}

	/**
	 * 是否可中断 <br>
	 * 缺省 True，可中断
	 * 
	 * @return True - 缺省
	 */
	public boolean interruptable() {
		// 默认可中断
		return Boolean.TRUE;
	}

	/**
	 * 任务主体.
	 */
	public abstract void onExecute();

	/**
	 * 执行完成后.
	 */
	public void onFinish() {
//		log.warn("onFinished: " + this.getName());
	}

	/**
	 * 是否开启？
	 * 
	 * @return True - 开启
	 */
	public boolean isEnabled() {
		// 默认开启
		return Boolean.TRUE;
	}

	/**
	 * 获取运行队列
	 * 
	 * @return 任务槽名称： “”， “S”， “SG”， “G”
	 * 
	 */
	public String getPool() {
		return _t;
	}

	/**
	 * 被停止.
	 * 
	 */
	public void onStop() {

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
			// 正在停止 ..., <br>
			// 清理资源， 然后退出
			onStop();
			return;
		}

		if (!this.isSys() && Runner.pause) {
			// 不是系统任务，任务池暂停
			String name = this.getName();
			log.warn("[" + name + "] was removed as pause.");

			synchronized (Runner.pendingQueue) {
				Runner.pendingQueue.remove(name);
			}
			return;
		}

		try {

			// 设置优先级
			if (this.getPriority() != Thread.currentThread().getPriority()) {
				Thread.currentThread().setPriority(this.getPriority());
			}

			// 设置运行时间
			startedtime = Global.now();
			delay = startedtime - scheduledtime;

			_cpuold = _cputime();

			if (debug || log.isDebugEnabled()) {
				log.info("running task [" + this.getName() + "], _t=" + _t + ", debug=" + debug);
			}

			if (X.isIn(_t, Task.GLOBAL, Task.SYSGLOBAL)) {

				/**
				 * CPU使用率高于10%，延缓全局普通任务的执行 <br>
				 * 可配置： giiwa.properties, task.cpusage = 10
				 * 
				 */
				int cpusage = Config.getConf().getInt("task.cpusage", 10);
				if (Node.cpusage > cpusage && X.isIn(_t, Task.GLOBAL)) {
					synchronized (this) {
						/**
						 * 负载越高，延时越长
						 */
						this.wait(Node.cpusage * 10);
					}
				}

				/**
				 * 全局任务，加锁, 如果不能加锁，则可能被别的节点抢走了 <br>
				 * 
				 * TODO: 如果任务执行很快，别的节点已经执行完成了，怎么办 ？
				 * 
				 */
				if (this.tryLock(debug)) {
					try {
						/**
						 * 切换任务队列
						 */
						if (!Runner._switch(this)) {
							return;
						}

						// to avoid killed
						sf = null;
						state = State.running;

						// 运行次数
						runtimes++;

						/**
						 * send command to other node to kill task in queue
						 */
						try {
							// 广播kill 其他节点的同名全局任务
							Request r = Request.create().put(this.getName());
							r.cmd = "kill";
							MQ.topic(Runner.service.getName(), r);
						} catch (Throwable e) {
							// ignore
						}

						if (debug || log.isDebugEnabled()) {
							log.info("running [" + this.getName() + "], debug=" + debug);
						}

						// 设置任务名称
						String old = null;
						Thread who = Thread.currentThread();
						if (who != null) {
							old = who.getName();
							who.setName(this.getName());
							this.who = who;
						}

						try {
							_currentask.set(this);
							// 执行任务
							onExecute();
						} finally {
							// 执行完成
							_currentask.remove();

							duration = Global.now() - startedtime;

							state = State.finished;

							Runner.remove(this);

							this.scheduledtime = 0;
							this.startedtime = 0;

							if (!Thread.currentThread().isInterrupted()) {
								// 任务不是被中断的
								onFinish();
							} else {
								log.warn("interrupted: " + this.getName());
							}

							if (who != null && !X.isEmpty(old)) {
								who.setName(old);
							}

						}
					} finally {
						// 解锁
						this.unlock();
					}
				} else {
					// can not get lock, running by other node
					// cleanup
					// 加锁失败
					synchronized (Runner.pendingQueue) {
						Runner.pendingQueue.remove(this.getName());
					}

					if (debug || log.isDebugEnabled()) {
						log.info("can not got lock [" + this.getName() + "], debug=" + debug);
					}
				}

			} else {
				// 本地任务，切换任务队列
				if (!Runner._switch(this)) {
					return;
				}

				if (debug || log.isDebugEnabled()) {
					log.info("running [" + this.getName() + "], debug=" + debug);
				}

				sf = null;
				state = State.running;

				runtimes++;

				// 设置任务名称
				String old = null;
				Thread who = Thread.currentThread();
				if (who != null) {
					old = who.getName();
					who.setName(this.getName());
					this.who = who;
				}

				_currentask.set(this);

				try {
					// 开始执行
					onExecute();
				} finally {
					// 执行完成
					duration = Global.now() - startedtime;

					state = State.finished;

					_currentask.remove();

					Runner.remove(this);

					this.scheduledtime = 0;
					this.startedtime = 0;

					if (!Thread.currentThread().isInterrupted()) {
						// 任务不是被中断的
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
	 * 初始化任务槽
	 *
	 * @param usernum - 普通线程数
	 */
	public static void init(int usernum) {

		Runner.init(usernum);

	}

	/**
	 * 停止所有任务
	 *
	 * @param fast - 是否快速停止， True=yes， 否则等待任务执行完
	 */
	final public boolean stop(boolean fast) {

		// only set stopping here
		stopping = true;
//		this.fast = fast;
		if (who != null) {

			if (interruptable()) {

				result(null); // killed

				// interrupt the thread which may wait a resource or timer;
				log.warn("stop task=" + this.getName());

				StringBuilder sb = new StringBuilder();

				Thread t1 = this.who;
				if (t1 != null) {
					StackTraceElement[] ss = t1.getStackTrace();
					sb.append("ID: ").append(t1.getId()).append("(0x").append(Long.toHexString(t1.getId()))
							.append("), Thread: ").append(t1.getName()).append(", State: ").append(t1.getState())
							.append(", Task:").append(this.getClass().getName()).append("\r");
					this.getState(sb);

					sb.append(t1.getState() + "\r");
					if (ss != null && ss.length > 0) {
						for (StackTraceElement e : ss) {
							sb.append("  ").append(e.toString()).append("\r");
						}
					}
				} else {
					this.getState(sb);
					sb.append("------\rscheduled=" + this.isScheduled() + "\risrunning=" + this.isRunning()
							+ "\rruntimes=" + this.getRuntimes() + "\rsf=" + this.getSF());
				}

				GLog.applog.warn("sys", "stop", "task=" + this.getName(), sb.toString(), null, null);

				try {
					// thread.interrupt()只是设置一个中断标志位，或者唤醒正在阻塞（如 sleep, wait）中的线程，而不是直接杀死它。
					who.interrupt();
					// 不要使用who.stop()
//					stop() 是一种“暴力”的强制终止方式。当调用该方法时，JVM 会立即抛出 ThreadDeath 错误来强行中断线程执行流。这会带来三个致命的并发安全问题：
//					锁不会被释放：如果线程在被杀死前持有同步锁，这些锁不会正常释放，会导致其他等待该锁的线程永远阻塞，引发死锁。
//					对象状态不一致：线程可能在执行关键操作（如转账、文件写入）进行到一半时被杀掉，导致数据损坏或资源处于破坏状态。
//					资源泄漏：线程持有的文件句柄、网络连接等无法得到正常的清理和关闭。

				} catch (Throwable err) {
					log.error(this.getName(), err);
				}

				// schedule the run a time to clear the resource
				onStop();
				return true;
			}
		} else {
			if (this.isRunning()) {
				log.warn("who is null, stop failed, name=" + this.getName());
			}

			onStop();
		}
		return false;
	}

	/**
	 * 调度任务， 本地调度
	 * 
	 * @param time - 时间， hh:mm
	 * @return 任务对象
	 */
	final public Task schedule(String time) {
		return schedule(time, false);
	}

	/**
	 * 调度任务
	 * 
	 * @param time   - 定时时间，比如：02:00， 2点钟执行
	 * @param global - 是否全局， True=yes
	 * @return 任务对象
	 */
	final public Task schedule(String time, boolean global) {

		try {
			if (time.startsWith("*")) {
				String[] ss = time.split(":");
				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(Global.now());

				c.set(Calendar.MINUTE, X.toInt(ss[1], 0));
				c.set(Calendar.SECOND, 0);

				if (c.getTimeInMillis() <= Global.now()) {
					// next hour
					c.add(Calendar.HOUR_OF_DAY, 1);
				}
				return this.schedule(c.getTimeInMillis() - Global.now(), global);

			} else {
				String[] ss = time.split(":");

				Calendar c = Calendar.getInstance();
				c.setTimeInMillis(Global.now());
				c.set(Calendar.HOUR_OF_DAY, X.toInt(ss[0], 0));
				c.set(Calendar.MINUTE, X.toInt(ss[1], 0));
				c.set(Calendar.SECOND, 0);

				if (c.getTimeInMillis() <= Global.now()) {
					// next hour
					c.add(Calendar.DAY_OF_MONTH, 1);
				}
				return this.schedule(c.getTimeInMillis() - Global.now(), global);
			}

		} catch (Throwable e) {
			log.error(this, e);
		}
		return this;
	}

	/**
	 * 调度任务，本地调度
	 *
	 * @param ms - 延时毫秒
	 * @return 任务对象
	 */
	final public Task schedule(long msec) {
		return this.schedule(msec, false);
	}

	/**
	 * 调度任务
	 * 
	 * @param ms     - 延时毫秒
	 * @param global - 是否全集， True=yes
	 * @return 任务对象
	 */
	final public synchronized Task schedule(long ms, boolean global) {

		try {

			if (stopping) {
				onStop();
				return this;
			}

			this.parent = Thread.currentThread().getName();
			if (this.parent.length() > 30) {
				this.parent = this.parent.substring(0, 27) + "...";
			}

			if (ms < 0) {
				ms = 0;
			}

			if (global && Runner.inited) {

				try {

//					this.attach(X.NODE, Local.id());
//					this.attach("ms", msec);
//					this.attach("g", true);
					this.ms = ms;

					MQ.Request r = MQ.Request.create().put(this);
					r.from = Node.dao.load(Local.id()).label;

					MQ.topic(Task.MQNAME, r);

					if (debug || log.isDebugEnabled()) {
						log.info("send task [" + this.getName() + "] to MQ, debug=" + debug);
					}

				} catch (Throwable e) {
					log.error("schedule [" + this.getName() + "] failed!", e);

					// schedule a local
					Runner.schedule(this, ms, global);
				}

			} else {

				if (debug || log.isDebugEnabled()) {
					log.info("schedule [" + this.getName() + "] in local, debug=" + debug);
				}

				Runner.schedule(this, ms, global);
			}

		} catch (Throwable e) {
			log.error(this, e);
		}

		return this;
	}

	/**
	 * 检测任务是否正在被调度
	 * 
	 * @return True， 正在被调度
	 */
	final public boolean isScheduled() {
		return Runner.isScheduled(this);
	}

	/**
	 * 任务是否正在运行
	 * 
	 * @return True，正在运行
	 */
	final public boolean isRunning() {
		return state == State.running;
	}

	/**
	 * 取消任务，如果已经开始，则取消跳过
	 * 
	 * @return True， 取消成功
	 */
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

	/**
	 * 调度几个任务
	 * 
	 * @param tt            - 任务体数组
	 * @param ms            - 延时毫秒
	 * @param global        - 是否全局， True=yes
	 * @param interruptable - 是否可被中断， False = 不可被中断
	 * @return 任务对象数组
	 */
	public static Task[] schedule(Task[] tt, long ms, boolean global, boolean interruptable) {
		if (tt == null)
			return null;

		for (Task t : tt) {
			t.schedule(ms, global);
		}
		return tt;
	}

	/**
	 * 调度任务， 无延时
	 * 
	 * @param cc - 任务体
	 * @return 任务对象
	 */
	final public static Task schedule(Consumer<Task> cc) {
		return schedule(cc, 0, true);
	}

	/**
	 * 重新调度一个任务
	 * 
	 * @param name - 任务名称
	 * @param ms   - 延时毫秒
	 */
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
	 * 计划调度任务
	 * 
	 * @param cc - 任务体
	 * @param ms - 延时毫秒
	 * @return 任务对象
	 */
	final public static Task schedule(Consumer<Task> cc, long ms) {
		return schedule(cc, ms, true);
	}

	/**
	 * 调度任务
	 * 
	 * @param cc            - 任务体
	 * @param ms            - 延时毫秒
	 * @param interruptable - 是否可被中断， False=不能中断
	 * @return 任务对象
	 */
	final public static Task schedule(Consumer<Task> cc, long ms, boolean interruptable) {

		Task t = _Task2.create(_shortname(Thread.currentThread().getName()) + "." + seq.incrementAndGet(), cc,
				interruptable);

		// local
		return t.schedule(ms);
	}

	private static String _shortname(String name) {
		if (name.length() >= 30) {
			name = name.substring(0, 27) + "...";
		}
		return name;
	}

	/**
	 * 并发执行任务<br>
	 * 当达到最大并发数时，需等待其中任务完成
	 * 
	 * @param taskids - 任务标识
	 * @param maxsize - 相同任务并发数
	 * @param cc      - 任务代码
	 * @return
	 * @throws Exception
	 */
	final public static Task schedule(String taskids, int maxsize, Consumer<Task> cc) throws Exception {
		return schedule(taskids, maxsize, cc, true);
	}

	/**
	 * 并发执行任务 <br>
	 * 
	 * 当达到最大并发数时，需等待其中任务完成
	 * 
	 * @param taskids       - 任务类型
	 * @param maxsize       - 相同任务最大并发数
	 * @param cc            - 任务
	 * @param interruptable - True=可中断
	 * @return
	 * @throws Exception
	 */
	final public static Task schedule(String taskids, int maxsize, Consumer<Task> cc, boolean interruptable)
			throws Exception {

		// 检测相同类型的线程数
		int n = Runner.number(taskids);

		while (n >= maxsize) {
			// 运行的比最大限定大， 等1秒钟
			Runner.await(X.ASECOND);
			n = Runner.number(taskids);
		}

		Task t = _Task2.create(_shortname(Thread.currentThread().getName()) + "." + seq.incrementAndGet(), cc,
				interruptable);

		// local
		t._type = taskids;
		return t.schedule(0);

	}

	/**
	 * 执行任务
	 * 
	 * @param cc     任务
	 * @param global true=全局
	 * @return
	 */
	final public static Task schedule(final Consumer<Task> cc, final boolean global) {
		return schedule(cc, global, true);
	}

	/**
	 * 执行任务
	 * 
	 * @param cc            任务
	 * @param global        true=全局
	 * @param interruptable true=可中断
	 * @return
	 */
	final public static Task schedule(final Consumer<Task> cc, final boolean global, boolean interruptable) {
		Task t = _Task2.create(_shortname(Thread.currentThread().getName()) + "." + seq.incrementAndGet(), cc,
				interruptable);
		// local
		return t.schedule(0, global);
	}

	/**
	 * 执行任务
	 * 
	 * @param name   任务名称，全局唯一，相同名称任务会取消之前的
	 * @param cc     任务
	 * @param global true=全局
	 * @return
	 */
	final public static Task schedule(final String name, final Consumer<Task> cc, final boolean global) {
		return schedule(name, cc, global, true);
	}

	/**
	 * 执行任务
	 * 
	 * @param name          - 任务名称，全局唯一
	 * @param cc            - 任务
	 * @param global        - true=全局
	 * @param interruptable - true=可中断
	 * @return
	 */
	final public static Task schedule(final String name, final Consumer<Task> cc, final boolean global,
			boolean interruptable) {

		Task t = _Task2.create(name, cc, interruptable);

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
	 * 活动线程数
	 * 
	 * @return 线程数
	 */
	public static int activeThread() {
		return Runner.local.getActiveCount();
	}

	/**
	 * 本地空闲任务槽线程数， 没有太多意义，总共有4个任务槽
	 * 
	 * @return 空闲线程数
	 */
	public static int idleThread() {
		return Runner.local.getPoolSize() - Runner.local.getActiveCount();
	}

	/**
	 * 调度任务数，不包括正在运行的
	 *
	 * @return 调度任务数
	 */
	public static int tasksInQueue() {
		return Runner.pendingQueue.size();
	}

	/**
	 * 获取被延时的任务数
	 * 
	 * @return 延时任务数
	 */
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

	/**
	 * 获取调度的任务数
	 * 
	 * @return 任务数
	 */
	static int numOfTasks() {
		return Runner.pendingQueue.size() + Runner.runningQueue.size();
	}

	/**
	 * 获取正在被调度的任务数，按照任务类型
	 * 
	 * @param types - 任务类型， S：系统级， G：全局，SG：全集系统接， “”：本地
	 * @return 任务数
	 */
	public static int tasksInQueue(String... types) {
		return Runner.tasksInQueue(types);
	}

	/**
	 * 正在运行的任务数
	 *
	 * @return 正在运行的任务数
	 */
	public static int tasksInRunning() {
		return Runner.runningQueue.size();

	}

	/**
	 * 获取正在运行的任务数，按照任务类型
	 * 
	 * @param types - 任务类型， S：系统级， G：全局，SG：全集系统接， “”：本地
	 * @return 任务数
	 */
	public static int tasksInRunning(String... types) {
		return Runner.tasksInRunning(types);
	}

	/**
	 * 获取所有的任务， 按照任务类型
	 * 
	 * @param types - 任务类型， S：系统级， G：全局，SG：全集系统接， “”：本地
	 * @return 任务对象列表
	 */
	public static List<Task> getRunningTask(String... types) {
		return Runner.getRunningTask(types);
	}

	/**
	 * 检测是否系统任务
	 * 
	 * @return True，是系统任务
	 */
	protected boolean isSys() {
		// 默认非系统任务
		return Boolean.FALSE;
	}

	/**
	 * 获取CPU运行时间，去除waiting时间
	 * 
	 * @return 毫秒
	 */
	public long getCosting() {
		if (who != null) {
			return (_cputime() - _cpuold) / 1000 / 1000; // ns->ms
		}
		return 0;
	}

	// 临时记录CPU耗时、当前时间，用于卡死检测，不参与序列化
	transient long[] hungCheckRecord;

	/**
	 * 判断当前任务是否已卡死 判定规则：连续10分钟CPU耗时无变化，则视为任务卡死
	 * 
	 * @return true=任务卡死，false=运行正常
	 */
	public boolean isHunging() {

		// 初始化检测记录：存储[当前CPU耗时, 当前系统时间戳]
		if (hungCheckRecord == null) {
			hungCheckRecord = new long[] { _cputime(), Global.now() };
			return false;
		}

		long currentCpuTime = _cputime();
		long recordCpuTime = hungCheckRecord[0];
		long recordTime = hungCheckRecord[1];
		long now = Global.now();

		// CPU耗时无变动且间隔超10分钟，判定卡死
		if (currentCpuTime == recordCpuTime && now - recordTime > HUNG_TIMEOUT) {
			return true;
		}

		// 更新检测记录，重置计时
		hungCheckRecord[0] = currentCpuTime;
		hungCheckRecord[1] = now;
		return false;
	}

	private static final long HUNG_TIMEOUT = X.AMINUTE * 10;

	private transient ThreadMXBean tmxb;

	private long _cputime() {
		if (who != null) {
			if (tmxb == null) {
				tmxb = ManagementFactory.getThreadMXBean();
			}
			return tmxb.getThreadCpuTime(who.getId());
		}
		return 0;
	}

	/**
	 * 获取所有调度中的任务对象
	 * 
	 * @return 任务对象列表
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
	 * 获取任务对象
	 * 
	 * @param name - 任务名称
	 * @return 任务对象
	 */
	public static Task get(String name) {
		return Runner.get(name);
	}

	/**
	 * 获取任务执行线程
	 * 
	 * @return 执行线程对象
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

	/**
	 * 观测任务结果
	 * 
	 * @param r - 结果通知
	 * @return
	 */
	public Task watch(Consumer<Object> r) {

		String name = this.getName();
		Runner.watch_map.put(name, r);

		return this;

	}

	/**
	 * 停止所有任务
	 * 
	 * @param fast - 快速停止， True = yes
	 */
	public static void stopAll(boolean fast) {
		Runner.stopAll(fast);
	}

	/**
	 * 尝试加全局锁
	 * 
	 * @return True， 加锁成功
	 */
	public final boolean tryLock() {
		return tryLock(false);
	}

	/**
	 * 尝试 加全局锁
	 * 
	 * @return True，加锁成功
	 */
	public final boolean tryLock(boolean debug) {

		try {
			if (_door == null) {
				synchronized (this) {
					if (_door == null) {
						_door = Global.getLock("global.door." + getName(), debug);
					}
				}
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
	 * 解锁，只有全局任务才有锁
	 */
	public final void unlock() {

		try {
			if (_door != null) {
				_door.unlock();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("sys", "lock", "unlock failed, lock=" + getName(), e);
		}
	}

	/**
	 * 参考Runner.cores
	 */
	@Deprecated
	public static int cores = 1;

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
	 * 任务是否正在运行
	 * 
	 * @param name - 任务名称
	 * @return True，正在运行
	 */
	public static boolean isRunning(String name) {

		W q = Node.dao.query();
		List<String> has = new ArrayList<String>();
		try {
			if (Runner.runningQueue.containsKey(name)) {
				return true;
			}

			q.and("tag", "giiwa");
			q.and("lastcheck", Global.now() - Node.LOST, W.OP.gte);
//			q.and("type", 0); // 本地节点

			@SuppressWarnings("deprecation")
			long n = q.count();

			boolean[] found = new boolean[] { false };

			MQ.callTopic(Task.MQNAME, "isrunning", name, RPC_TIMEOUT, r -> {

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

				has.add(from);
				if (has.size() >= n) {
					// 结束
					return true;
				} else {
					return false;
				}

			});

			return found[0];

		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("task", "isRunning", e.getMessage(), e);

			Task.schedule(t1 -> {
				try {
					// mark the miss node as lost
					Node.dao.stream(q, e1 -> {
						if (!X.isEmpty(e1.label) && !has.contains(e1.label)) {
							Node.dao.update(e1.id, V.create().append("mq_error", 1));
						}
						return true;
					});
				} catch (Exception err) {
					// ignore
				}

			});

		}

		return false;

	}

	private final static long RPC_TIMEOUT = 5 * X.ASECOND;

	/**
	 * 
	 * 检测任务是否被调度，包括正在运行
	 * 
	 * @param name - 任务名称
	 * @return True，正在被调度
	 */
	public static boolean isScheduled(String name) {

		Task t = Runner.isScheduled(name);
		if (t != null && (t.isRunning() || t.getRemain() > 0)) {
			return true;
		}

		W q = Node.dao.query();
		q.and("tag", "giiwa");
		q.and("lastcheck", Global.now() - Node.LOST, W.OP.gte);
//		q.and("type", 0); // 本地节点

		List<String> has = new ArrayList<String>();

		boolean[] found = new boolean[] { false };

		try {
			// TODO,这个地方有问题
			@SuppressWarnings("deprecation")
			long n = q.count();
			if (n <= 1) {
				return false;
			}

			TimeStamp cost = TimeStamp.create();

			MQ.callTopic(Task.MQNAME, "ischeduled", name, RPC_TIMEOUT, req -> {

				String from = req.from;

				try {

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

				has.add(from);
				if (has.size() >= n) {
					// 停止等待消息
					return true;
				} else {
					return false;
				}

			});

			if (log.isDebugEnabled()) {
				log.debug("ischeduled, cost=" + cost.past() + ", name=" + name + ", nodes=" + n + ", got=" + has
						+ ", found=" + found[0]);
			}

			return found[0];

		} catch (Exception e) {
			if (has != null && has.size() <= 0) {
				if (found[0] && log.isDebugEnabled()) {
					log.debug("ischeduled, name=" + name + ", got=" + has);
				}
				return found[0];
			}
			log.error("name=" + name + ", got=" + has, e);
			GLog.applog.error("task", "isSchedule", e.getMessage() + ", got=" + has, e);

			Task.schedule(t1 -> {
				try {
					// mark the miss node as lost
					Node.dao.stream(q, e1 -> {
						if (!X.isEmpty(e1.label) && !has.contains(e1.label)) {
							Node.dao.update(e1.id, V.create().append("mq_error", 1));
						}
						return true;
					});
				} catch (Exception err) {
					// ignore
				}

			});

		}

		// 直接返回false， 防止消息服务器出现故障后， 任务无法运行
		return false;

	}

	/**
	 * 取消任务，如果任务没有运行， 兼容以前的kill方法， 强制停止请用force
	 * 
	 * @param name - 任务名称
	 */
	public static void kill(String name) {
		cancel(name);
	}

	/**
	 * 取消任务，如果任务没有运行
	 * 
	 * @param name - 任务名称
	 */
	public static void cancel(String name) {

		try {
			MQ.callTopic(Task.MQNAME, "cancel", name, 0, null);
		} catch (Exception e) {
			log.error(name, e);
			GLog.applog.error("task", "cancel", e.getMessage(), e);
		}
	}

	/**
	 * 强制停止任务
	 * 
	 * @param name - 任务名称
	 */
	public static void force(String name) {

		try {
			MQ.callTopic(Task.MQNAME, "force", name, 0, null);
		} catch (Exception e) {
			log.error(name, e);
			GLog.applog.error("task", "force", e.getMessage(), e);
		}
	}

	/**
	 * 并发执行
	 * 
	 * @param l1       - 数据列表
	 * @param parallel - 并发数
	 * @param global   - 全局，True = yes
	 * @param func     - 执行代码
	 */
	public static <E> void forEach(List<E> l1, int parallel, boolean global, Consumer<E> func) {
		forEach(l1, parallel, global, func, true);
	}

	/**
	 * 并发执行
	 * 
	 * @param l1            - 数据队列
	 * @param parallel      - 并发数
	 * @param global        - 全局， True = yes
	 * @param func          - 执行体
	 * @param interruptable - 是否可中断， False = no，不可中断
	 */
	public static <E> void forEach(List<E> l1, int parallel, boolean global, Consumer<E> func, boolean interruptable) {

		if (l1 == null || l1.isEmpty()) {
			// 没有数据，直接退出
			return;
		}

		if (l1.size() == 1 || parallel <= 1) {
			// 单个，不需要并发
			l1.forEach(func);
		} else {

			try {
				if (global) {
					// 全局
					final String threadname = _shortname(Thread.currentThread().getName());
					final AtomicInteger seq = new AtomicInteger(1);

					List<Task> l2 = new ArrayList<Task>();
					for (E e : l1) {
						Task t = _Task1.create(threadname + "." + seq.incrementAndGet(), e, func);
						synchronized (l2) {
							while (l2.size() > parallel) {
								l2.wait(X.ASECOND);
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
								l2.wait(X.ASECOND);
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
					// 本地
					int size = l1.size();
					AtomicInteger idx = new AtomicInteger(0);

					Task[] tt = new Task[Math.min(parallel, size)];
					for (int i = 0; i < tt.length; i++) {
						tt[i] = _Task2.create(_shortname(Thread.currentThread().getName()) + "." + i, t -> {
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
						}, interruptable);
					}

					for (Task t : tt) {
						t.schedule(0);
					}

					synchronized (tt) {
						for (Task t : tt) {
							// wait-for
							while (t.isScheduled()) {
								tt.wait(X.ASECOND);
							}
						}
					}
				}
			} catch (Exception err) {
				log.error(err.getMessage(), err);
			}
		}
	}

	/**
	 * 并发执行数据流
	 * 
	 * @param <E>
	 * @param l1       - 数据流
	 * @param parallel - 并发数
	 * @param global   - True= 全局
	 * @param func     - 回调任务体
	 * @throws IOException
	 * @since 3.4
	 */
	public static <E> void forEach(Stream<E> l1, int parallel, boolean global, Consumer<E> func) throws IOException {
		forEach(l1, parallel, global, func, true);
	}

	/**
	 * 并发执行 数据流
	 * 
	 * @param <E>
	 * @param l1            - 数据流
	 * @param parallel      - 并发数
	 * @param global        - True， 全局
	 * @param func          - 回调任务函数
	 * @param interruptable - True， 可中断
	 * @throws IOException
	 * @since 3.4
	 */
	public static <E> void forEach(Stream<E> l1, int parallel, boolean global, Consumer<E> func, boolean interruptable)
			throws IOException {

		if (l1 == null || !l1.hasNext()) {
			// 没有数据
			return;
		}

		if (parallel <= 1) {
			// 本地执行，非并发
			while (l1.hasNext()) {
				E e = l1.next();
				func.accept(e);
			}
			return;
		}

		try {
			if (global) {
				/**
				 * 全局需要监测任务是否执行完成， 并负责重启任务
				 */
				final String threadname = _shortname(Thread.currentThread().getName());
				final AtomicInteger seq = new AtomicInteger(1);

				List<Task> l2 = new ArrayList<Task>();
				while (l1.hasNext()) {
					E e = l1.next();
					Task t = _Task1.create(threadname + "." + seq.incrementAndGet(), e, func);

					/**
					 * 检查等待队列没有达到最大并发数，否则等待
					 */
					_wait(l2, parallel);

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

				/**
				 * 等待所有任务执行完
				 */
				_wait(l2, -1);

			} else {

				/**
				 * 本地任务直接执行本地通知 <br>
				 * TODO
				 * 
				 */
				Task[] tt = new Task[parallel];
				for (int i = 0; i < tt.length; i++) {
					tt[i] = _Task2.create(_shortname(Thread.currentThread().getName()) + "." + i, t -> {
						E e = null;
						synchronized (l1) {
							while (e == null && l1.hasNext()) {
								e = l1.next();
							}
						}
						if (e != null) {
							try {
								func.accept(e);
							} catch (Exception err) {
								log.error(err.getMessage(), err);
							}
							synchronized (tt) {
								tt.notifyAll();
							}
						}
					}, interruptable);
				}

				/**
				 * 开始调度
				 */
				for (Task t : tt) {
					t.schedule(0);
				}

				synchronized (tt) {
					for (Task t : tt) {
						// wait-for
						while (t.isScheduled()) {
							tt.wait(X.ASECOND);
						}
					}
				}
			}
		} catch (Exception err) {
			log.error(err.getMessage(), err);
		} finally {
			X.close(l1);
		}

	}

	/**
	 * 检查任务执行结果
	 * 
	 * @param l2       - 任务队列
	 * @param parallel - 最大数量
	 * @throws InterruptedException
	 */
	private static void _wait(List<Task> l2, int parallel) throws InterruptedException {

		synchronized (l2) {
			/**
			 * 已经达到最大并发数，等待
			 */
			int i = 0;
			while (l2.size() >= parallel) {
				l2.wait(X.ASECOND);
				i++;
				if (i >= 10) {
					/**
					 * 检查一下任务 是否 没有被调度起来 <br>
					 * 1: 节点死掉了 <br>
					 * 2: 消息服务器问题
					 */
					for (Task t : l2) {
						if (!Task.isScheduled(t.getName())) {
							log.warn("global parallel, schduled.2 =" + t);
							t.schedule(0, true);
							i = 0;
						}
					}
				}
			}
		}
	}

	/**
	 * 并发 数据流
	 * 
	 * @param <E>
	 * @param l1       - 数据流
	 * @param parallel - 并发数
	 * @param global   - True， 全局调度
	 * @param func     - 回调任务函数
	 * @throws IOException
	 * @since 3.4
	 */
	public static <E> void forEach(Stream<E> l1, int parallel, boolean global, IFactory<E> func) throws IOException {

		if (l1 == null || !l1.hasNext()) {
			return;
		}

		if (!global || parallel <= 1) {

			// 非全局任务, 本地并发
			Task.forEach(l1, parallel, global, e -> {
				Task t = func.create(e);
				t.onExecute();
			});

			return;
		}

		/**
		 * 等待计划所有任务
		 */
		List<Task> l2 = new ArrayList<Task>();
		try {
			while (l1.hasNext()) {

				/**
				 * 检查是否已经达到并发数
				 */
				synchronized (l2) {
					int n = 0;
					while (l2.size() >= parallel) {
						try {
							l2.wait(X.ASECOND);
						} catch (Exception err) {
							log.error(err.getMessage(), err);
						}
						n++;
						if (n > 10) {
							/**
							 * 是否有“死”掉的任务
							 */
							Task[] tt = l2.toArray(new Task[l2.size()]);
							for (Task t : tt) {
								if (!Task.isScheduled(t.getName())) {
									/**
									 * 重新计划任务，防止节点掉线后，任务执行不完整
									 */
									t.schedule(0, true);
								}
							}
						}
					}
				}

				/**
				 * 开始计划1个任务
				 */
				E e = l1.next();
				Task t = func.create(e);
				t.watch(r -> {
					synchronized (l2) {
						if (log.isInfoEnabled()) {
							log.info("global parallel, removed, t=" + t);
						}
						/**
						 * 任务执行完成后， 从队列中删除，并通知可以计划下一个任务
						 */
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

			/**
			 * 等所有结果执行完成
			 */
			synchronized (l2) {
				try {
					while (l2.size() > 0) {
						l2.wait(X.ASECOND);
						if (log.isDebugEnabled()) {
							log.debug("global parallel, waiting finished, l2=" + l2);
						}
						for (Task t : l2) {
							if (!Task.isScheduled(t.getName())) {
								/**
								 * 重新计划任务， 防止节点掉线后，任务执行不完整
								 */
								log.info("global parallel, schduled.2 =" + t);
								t.schedule(0, true);
							}
						}
					}
				} catch (Exception err) {
					log.error(err.getMessage(), err);
				}
			}
		} finally {
			X.close(l1);
		}

	}

	/**
	 * 并发执行
	 * 
	 * @param l1       - 待执行对象列表
	 * @param parallel - 并发数
	 * @param global   - 全局， True=yes
	 * @param func     - 执行体
	 */
	public static <E> void forEach(List<E> l1, int parallel, boolean global, IFactory<E> func) {

		if (l1 == null || l1.isEmpty()) {
			return;
		}

		if (l1.size() == 1 || parallel <= 1) {
			l1.forEach(e -> {
				Task t = func.create(e);
				t.onExecute();
			});
			return;
		}

		if (!global) {
			// 非全局任务, 本地并发
			Task.forEach(l1, parallel, e -> {
				Task t = func.create(e);
				t.onExecute();
			});
			return;
		}

		/**
		 * 全局并发，需要等待计划所有任务
		 */
		List<Task> l2 = new ArrayList<Task>();
		for (E e : l1) {

			/**
			 * 检查是否已经达到并发数
			 */
			synchronized (l2) {
				int n = 0;
				while (l2.size() >= parallel) {
					try {
						l2.wait(X.ASECOND);
					} catch (Exception err) {
						log.error(err.getMessage(), err);
					}
					n++;
					if (n > 10) {
						/**
						 * 是否有“死”掉的任务
						 */
						Task[] tt = l2.toArray(new Task[l2.size()]);
						for (Task t : tt) {
							if (!Task.isScheduled(t.getName())) {
								/**
								 * 重新计划任务，防止节点掉线后，任务执行不完整
								 */
								t.schedule(0, true);
							}
						}
					}
				}
			}

			/**
			 * 开始计划1个任务
			 */
			Task t = func.create(e);
			t.watch(r -> {
				synchronized (l2) {
					if (log.isInfoEnabled()) {
						log.info("global parallel, removed, t=" + t);
					}
					/**
					 * 任务执行完成后， 从队列中删除，并通知可以计划下一个任务
					 */
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

		/**
		 * 等所有结果执行完成
		 */
		synchronized (l2) {
			try {
				while (l2.size() > 0) {
					l2.wait(X.ASECOND);
					if (log.isDebugEnabled()) {
						log.debug("global parallel, waiting finished, l2=" + l2);
					}
					for (Task t : l2) {
						if (!Task.isScheduled(t.getName())) {
							/**
							 * 重新计划任务， 防止节点掉线后，任务执行不完整
							 */
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
	 * @param l1       - 数据队列
	 * @param parallel - 并发数
	 * @param func     - 执行代码
	 */
	public static <E> void forEach(List<E> l1, int parallel, Consumer<E> func) {
		forEach(l1, parallel, false, func);
	}

	/**
	 * 并发执行，本地运行
	 * 
	 * @param l1       - 数据队列
	 * @param parallel - 并发数
	 * @param func     - 执行代码
	 */
	public static <E> void forEach(List<E> l1, int parallel, IFactory<E> factory) {
		forEach(l1, parallel, false, factory);
	}

	/**
	 * 并发执行，本地并发
	 * 
	 * @param <E>
	 * @param l1   - 数据队列
	 * @param func - 执行代码
	 */
	public static <E> void forEach(List<E> l1, Consumer<E> func) {
		forEach(l1, l1.size(), false, func);
	}

	/**
	 * 并发执行
	 * 
	 * @param <E>
	 * @param l1     - 数据队列
	 * @param global - 全局， True=yes
	 * @param func   - 执行代码
	 */
	@Deprecated
	public static <E> void forEach(List<E> l1, boolean global, Consumer<E> func) {
		forEach(l1, l1.size(), global, func);
	}

	/**
	 * 暂停任务调度，已经开始的任务，会继续执行
	 */
	public static void pause() {
		log.warn("Task was paused by:", new Exception());
		Runner.pause = true;
	}

	/**
	 * 恢复任务调度
	 */
	public static void resume() {
		log.warn("Task was resumed by:", new Exception());
		Runner.pause = false;
	}

	public String getSF() {
		// 任务结果
		return sf == null ? null : ("" + sf.isDone());
	}

	/**
	 * 创建任务借口
	 * 
	 * @author joe
	 *
	 * @param <E>
	 */
	public static interface IFactory<E> extends Serializable {
		public Task create(E e);
	}

	/**
	 * 全局任务
	 * 
	 * @author joe
	 *
	 */
	final static class _Task1 extends Task {
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
	 * 本地任务
	 * 
	 * @author joe
	 *
	 */
	final static class _Task2 extends Task {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@SuppressWarnings("rawtypes")
		Consumer func;
		String name;
		boolean interruptable;

		@Override
		public boolean interruptable() {
			return interruptable;
		}

		@Override
		public String getName() {
			return this.name;
		}

		public static <E> _Task2 create(String name, Consumer<Task> func, boolean interruptable) {
			_Task2 t = new _Task2();
			t.name = name;
			t.func = func;
			t.interruptable = interruptable;
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

	/**
	 * 全局广播调用
	 * 
	 * @param cmd    - 命令
	 * @param params - 参数
	 * @param func   - 回调函数
	 */
	public static void call(String cmd, Serializable params, Function<Request, Boolean> func) {

		W q = Node.dao.query().and("tag", "giiwa");
		q.and("lastcheck", Global.now() - Node.LOST, W.OP.gte);
		Node.dao.optimize(q);

		List<String> has = new ArrayList<String>();

		try {

			@SuppressWarnings("deprecation")
			long n = q.count();

			MQ.callTopic(Task.MQNAME, cmd, params, RPC_TIMEOUT, req -> {

				if (!func.apply(req)) {
					// 被终止
					return false;
				}

				has.add(req.from);
				if (has.size() >= n) {
					// 结束
					return true;
				} else {
					return false;
				}
			});
		} catch (Exception e) {
			log.error("got " + has.toString(), e);
		}

	}

	/**
	 * 获取当前任务
	 * 
	 * @return
	 */
	public static Task currentTask() {
		return _currentask.get();
	}

	private static final ThreadLocal<Task> _currentask = ThreadLocal.withInitial(() -> null);

}
