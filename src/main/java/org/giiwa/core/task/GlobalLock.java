package org.giiwa.core.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.json.JSON;
import org.giiwa.mq.MQ;

public class GlobalLock implements Lock {

	private static Log log = LogFactory.getLog(GlobalLock.class);

	private String name;
	private String value;

	public static Lock create(String name) {
		GlobalLock l = new GlobalLock();
		l.name = name;
		l.value = Long.toString(System.currentTimeMillis());
		return l;
	}

	@Override
	public void lock() {
		try {
			tryLock(X.AHOUR, TimeUnit.MILLISECONDS);
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

	@Override
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {

		TimeStamp t = TimeStamp.create();
		long expire = unit.toMillis(time);

		// log.debug("tryLock, global lock=" + name);

		try {
			while (expire == 0 || expire > t.pastms()) {

				if (Cache.trylock(name, value, expire)) {
					heartbeat.add(this);

					if (log.isDebugEnabled())
						log.debug("global locked, name=" + name);

					return true;
				}

				if (expire == 0 || expire < t.pastms()) {
					return false;
				}

				MQ.wait("lock." + name, Math.min(1000, expire - t.pastms()));
			}
		} catch (Exception e) {
			log.error("lock failed, global lock=" + name, e);
		}

		return false;

	}

	@Override
	public String toString() {
		return "GlobalLock [name=" + name + "]";
	}

	public void touch() {
		Cache.expire(name, value, 12000);
	}

	@Override
	public void unlock() {
		heartbeat.remove(this);

		if (Cache.unlock(name, value)) {
			MQ.notify("lock." + name, JSON.create());
			if (log.isDebugEnabled())
				log.debug("global unlocked, name=" + name);
		} else {
			if (log.isDebugEnabled())
				log.debug("what's wrong with the lock=" + name);
		}
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

	private static LockHeartbeat heartbeat = new LockHeartbeat();

	private static class LockHeartbeat extends SysTask {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static List<GlobalLock> locked = new ArrayList<GlobalLock>();

		@Override
		public String getName() {
			return "global.lock.heartbeat";
		}

		private void add(GlobalLock name) {
			synchronized (locked) {
				if (!locked.contains(name)) {
					locked.add(name);
				}
			}

			this.schedule(10);
		}

		public void remove(GlobalLock lock) {
			synchronized (locked) {
				locked.remove(lock);
			}
		}

		@Override
		public void onExecute() {

			synchronized (locked) {
				if (!locked.isEmpty()) {
					for (GlobalLock l : locked) {
						l.touch();
					}
				}
				// log.debug("touch name=" + locked);
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

}
