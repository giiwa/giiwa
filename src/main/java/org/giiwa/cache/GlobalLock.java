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
package org.giiwa.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.net.mq.MQ;
import org.giiwa.task.SysTask;

public class GlobalLock implements Lock, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(GlobalLock.class);

	private boolean locked = false;
	protected String name;
	private String value;
	private boolean debug = false;

	protected GlobalLock(String name, boolean debug) {
		this.name = name;
		this.value = Long.toString(Global.now());
		this.debug = debug;
	}

	public static Lock create(String name) {
		return create(name, false);
	}

	public static Lock create(String name, boolean debug) {
		return new GlobalLock(name, debug);
	}

	@Override
	public void lock() {
		try {
			if (tryLock(X.AHOUR, TimeUnit.MILLISECONDS)) {
				locked = true;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void lockInterruptibly() throws InterruptedException {
		lock();
	}

	@Override
	public boolean tryLock() {
		try {
			if (tryLock(0, TimeUnit.MILLISECONDS)) {
				return true;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return false;
	}

	private TimeStamp _time = TimeStamp.create();

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {

		TimeStamp t = TimeStamp.create();
		long expire = unit.toMillis(time);

		// log.debug("tryLock, global lock=" + name);

		try {
			while (expire == 0 || expire > t.pastms()) {

				if (Cache.trylock(name, debug)) {
					heartbeat.add(name, this);

					if (debug || log.isDebugEnabled()) {
						log.info("global locked, name=" + name + ", cost=" + t.past());
					}

					_time.reset();
					locked = true;
					return true;
				}

				// make sure
				synchronized (LockHeartbeat.locked) {
					if (LockHeartbeat.locked.containsKey(name)) {
						return true;
					}
				}

				if (expire == 0 || expire < t.pastms()) {
					locked = false;
					return false;
				}

				MQ.wait("lock." + name, Math.min(1000, expire - t.pastms()));
			}
		} catch (Exception e) {
			log.error("lock failed, global lock=" + name, e);
			GLog.applog.error("sys", "lock", "failed, name=" + name, e);
		}

		locked = false;
		return false;

	}

	@Override
	public String toString() {
		return "GlobalLock [name=" + name + "]";
	}

	public void touch() {
		Cache.expire(name, 12000);
	}

	@Override
	public synchronized void unlock() {
		if (locked) {

			try {
				locked = false;

				heartbeat.remove(name);

				if (Cache.unlock(name, value)) {
					MQ.notify("lock." + name, 0);
					if (log.isDebugEnabled()) {
						log.debug("global unlocked, name=" + name + ", locked=" + _time.past());
					}
				} else {
					GLog.applog.warn("sys", "unlock", "failed, lock=" + name);
					log.error("what's wrong with the lock=" + name);
				}
			} catch (Throwable e) {
				GLog.applog.error("sys", "unlock", "failed, lock=" + name, e);
			}
		}
	}

	public static List<_Lock> getLocks() {
		List<_Lock> l1 = new ArrayList<_Lock>();
		synchronized (LockHeartbeat.locked) {
			l1.addAll(LockHeartbeat.locked.values());
		}
		return l1;
	}

	public static void kill(String name) {
		heartbeat.remove(name);
	}

	@Override
	public Condition newCondition() {
		return null;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		GlobalLock other = (GlobalLock) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	public static LockHeartbeat heartbeat = new LockHeartbeat();

	static class LockHeartbeat extends SysTask {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static Map<String, _Lock> locked = new HashMap<String, _Lock>();

		@Override
		public String getName() {
			return "gi.lock.hb";
		}

		public void add(String name, Lock lock) {
			synchronized (locked) {
				if (!locked.containsKey(name)) {
					_Lock e = new _Lock();
					e.name = name;
					e.lock = lock;
					e.thread = Thread.currentThread().getName();
//					e.trace = X.toString(new Exception("trace"));
					locked.put(name, e);
				}
			}

			if (!this.isScheduled()) {
				this.schedule(10);
			}
		}

		public void remove(String name) {
			synchronized (locked) {
				locked.remove(name);
			}
		}

		@Override
		public void onExecute() {

			_Lock[] ll = null;
			synchronized (locked) {
				if (!locked.isEmpty()) {
					ll = locked.values().toArray(new _Lock[locked.size()]);
				}
			}
			if (ll != null) {
				for (_Lock e : ll) {
					((GlobalLock) e.lock).touch();
				}
			}
		}

		@Override
		public void onFinish() {
			synchronized (locked) {
				if (!locked.isEmpty()) {
					this.schedule(4000);
				}
			}
		}

	}

	public static class _Lock implements Serializable {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		transient Lock lock;

		public long created = Global.now();
		public String name;
		public String thread;
		public String trace;
		public String node = Local.label();

		public long getCreated() {
			return created;
		}

		public String getName() {
			return name;
		}

		public String getThread() {
			return thread;
		}

		public String getNode() {
			return node;
		}

	}

	public static _Lock getLock(String name) {
		synchronized (LockHeartbeat.locked) {
			return LockHeartbeat.locked.get(name);
		}
	}
}
