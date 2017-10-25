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
package org.giiwa.core.conf;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.giiwa.core.bean.*;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.task.Task;

/**
 * The Class Global is extended of Config, it can be "overrided" by module or
 * configured, it stored in database
 * 
 * @author yjiang
 */
@Table(name = "gi_config")
public final class Global extends Bean {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	@Column(name = X.ID)
	String id;

	@Column(name = "s")
	String s;

	@Column(name = "i")
	int i;

	@Column(name = "l")
	long l;

	private static String instanceid = UID.uuid();

	private static Global owner = new Global();

	public static Global getInstance() {
		return owner;
	}

	/**
	 * get the int value.
	 *
	 * @param name
	 *            the name
	 * @param defaultValue
	 *            the default value
	 * @return the int
	 */
	public static int getInt(String name, int defaultValue) {

		Global c = cached.get("global/" + name);
		if (c == null || c.expired()) {
			c = Helper.load(W.create(X.ID, name), Global.class);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				c.setExpired(System.currentTimeMillis() + X.AMINUTE);
				cached.put("global/" + name, c);
				return X.toInt(c.i, defaultValue);
			}
		}

		return c != null ? X.toInt(c.i, defaultValue) : Config.getConf().getInt(name, defaultValue);

	}

	private static Map<String, Global> cached = new HashMap<String, Global>();

	/**
	 * get the string value.
	 *
	 * @param name
	 *            the name
	 * @param defaultValue
	 *            the default value
	 * @return the string
	 */
	public static String getString(String name, String defaultValue) {

		Global c = cached.get("global/" + name);
		if (c == null || c.expired()) {
			c = Helper.load(W.create(X.ID, name), Global.class);
			if (c != null) {
				/**
				 * avoid restarted, can not load new config
				 */
				c.setExpired(System.currentTimeMillis() + X.AMINUTE);
				cached.put("global/" + name, c);

				return c.s != null ? c.s : defaultValue;
			}
		}

		return c != null && c.s != null ? c.s : Config.getConf().getString(name, defaultValue);

	}

	// private static Map<String, Object> cache = new HashMap<String, Object>();

	/**
	 * get the long value.
	 *
	 * @param name
	 *            the name
	 * @param defaultValue
	 *            the default value
	 * @return the long
	 */
	public static long getLong(String name, long defaultValue) {

		try {
			Global c = cached.get("global/" + name);
			if (c == null || c.expired()) {
				c = Helper.load(W.create(X.ID, name), Global.class);
				if (c != null) {
					/**
					 * avoid restarted, can not load new config
					 */
					c.setExpired(System.currentTimeMillis() + X.AMINUTE);
					cached.put("global/" + name, c);

					return X.toLong(c.l, defaultValue);
				}
			}
			return c != null ? X.toLong(c.l, defaultValue) : Config.getConf().getLong(name, defaultValue);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return defaultValue;
	}

	/**
	 * get the current time.
	 *
	 * @return long of current time
	 */
	public static long now() {
		return System.currentTimeMillis();
	}

	/**
	 * Sets the value of the name in database, it will remove the configuration
	 * value if value is null.
	 *
	 * @param name
	 *            the name
	 * @param o
	 *            the value
	 */
	public synchronized static void setConfig(String name, Object o) {
		if (X.isEmpty(name)) {
			return;
		}

		if (o == null) {
			Helper.delete(W.create(X.ID, name), Global.class);
			return;
		}

		try {
			Global g = new Global();
			V v = V.create();
			if (o instanceof Integer) {
				v.set("i", o);
				g.i = X.toInt(o);
			} else if (o instanceof Long) {
				v.set("l", o);
				g.l = X.toLong(o);
			} else {
				v.set("s", o.toString());
				g.s = o.toString();
			}

			cached.put("global/" + name, g);

			if (Helper.isConfigured()) {
				if (Helper.exists(W.create(X.ID, name), Global.class)) {
					Helper.update(W.create(X.ID, name), v, Global.class);
				} else {
					Helper.insert(v.set(X.ID, name), Global.class);
				}
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
	}

	/**
	 * Gets the string value
	 *
	 * @param name
	 *            the name
	 * @return the string
	 */
	public String get(String name) {
		return getString(name, null);
	}

	public static Lock getLock(String name) {
		return new GlobalLock(name);
	}

	/**
	 * Lock a global lock
	 *
	 * @param name
	 *            the name of lock
	 * @param timeout
	 *            the timeout
	 * @return true, if successful
	 */
	private static boolean lock(String name, long timeout) {

		name = "lock." + name;

		Thread th = locked.get(name);
		if (th != null && th.getId() == Thread.currentThread().getId()) {
			return true;
		}

		try {
			TimeStamp t = TimeStamp.create();

			while (timeout == 0 || timeout >= t.pastms()) {

				Global f = Helper.load(name, Global.class);

				if (f == null) {
					String linkid = UID.random();

					Helper.insert(V.create(X.ID, name).set("s", instanceid).set("linkid", linkid), Global.class);
					f = Helper.load(name, Global.class);
					if (f == null) {
						log.error("occur error when create unique id, name=" + name);
						return false;
					} else if (!X.isSame(f.getString("linkid"), linkid)) {
						if (timeout <= t.pastms()) {
							return false;
						}
						synchronized (locked) {
							locked.wait(1000);
						}

						continue;
					}

					locked.put(name, Thread.currentThread());
					heartbeat.schedule(10);

					return true;
				} else {
					String s = f.getString("s");
					// 10 seconds
					if (X.isEmpty(s) || System.currentTimeMillis() - f.getUpdated() > 10000) {
						if (Helper.update(W.create(X.ID, name).and("s", s), V.create("s", instanceid),
								Global.class) > 0) {
							locked.put(name, Thread.currentThread());
							heartbeat.schedule(10);

							return true;
						} else {
							if (timeout <= t.pastms()) {
								return false;
							}
							synchronized (locked) {
								locked.wait(1000);
							}
							continue;
						}
					}
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		return false;
	}

	/**
	 * Release the global lock
	 *
	 * @param name
	 *            the name of lock
	 * @return true, if successful
	 */
	private static boolean unlock(String name) {
		name = "lock." + name;

		synchronized (locked) {
			Thread t = locked.remove(name);
			if (t != null && t.getId() == Thread.currentThread().getId()) {
				locked.remove(name);
				Helper.update(W.create(X.ID, name).and("s", instanceid), V.create("s", X.EMPTY), Global.class);
				locked.notifyAll();
				return true;
			}

		}
		return false;
	}

	private static Task heartbeat = new LockHeartbeat();

	private static class LockHeartbeat extends Task {

		@Override
		public void onExecute() {

			if (locked.size() > 0) {
				String[] names = locked.keySet().toArray(new String[locked.size()]);

				for (String name : names) {
					if (Helper.update(W.create(X.ID, name).and("s", instanceid),
							V.create(X.UPDATED, System.currentTimeMillis()), Global.class) <= 0) {

						// the lock has been acquired by other
						Thread t = locked.get(name);
						if (t != null) {
							// interrupt this thread
							log.warn("lock[" + name + "] was locked by others, interrupt the thread=" + t);
							t.interrupt();
						}
						locked.remove(name);
					}
				}
			}
		}

		@Override
		public void onFinish() {
			if (!locked.isEmpty()) {
				this.schedule(3000);
			}
		}

	}

	private static Map<String, Thread> locked = new HashMap<String, Thread>();

	private static class GlobalLock implements Lock {

		private String name;
		private Lock lock = new ReentrantLock();

		private GlobalLock(String name) {
			this.name = name;
		}

		@Override
		public void lock() {
			lock.lock();
			Global.lock(name, Long.MAX_VALUE);
		}

		@Override
		public void lockInterruptibly() throws InterruptedException {
			lock();
		}

		@Override
		public boolean tryLock() {
			if (lock.tryLock()) {
				return Global.lock(name, 0);
			}
			return false;
		}

		@Override
		public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
			if (lock.tryLock(time, unit)) {
				return Global.lock(name, unit.toMillis(time));
			}
			return false;
		}

		@Override
		public void unlock() {
			lock.unlock();
			Global.unlock(name);
		}

		@Override
		public Condition newCondition() {
			return lock.newCondition();
		}

	}

}
