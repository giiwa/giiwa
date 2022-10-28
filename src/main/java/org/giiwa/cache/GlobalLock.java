package org.giiwa.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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

	protected GlobalLock(String name) {
		this.name = name;
		this.value = Long.toString(System.currentTimeMillis());
	}

	public static Lock create(String name) {
		return new GlobalLock(name);
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

				if (Cache.trylock(name)) {
					heartbeat.add(this);

					if (log.isDebugEnabled())
						log.debug("global locked, name=" + name + ", cost=" + t.past());

					_time.reset();
					locked = true;
					return true;
				}

				if (expire == 0 || expire < t.pastms()) {
					if (log.isDebugEnabled()) {
						log.debug("global lock failed, name=" + name + ", cost=" + t.past() + ", locked="
								+ X.asList(LockHeartbeat.locked, e -> ((GlobalLock) e).name));
					}

					locked = false;
					return false;
				}

				MQ.wait("lock." + name, Math.min(1000, expire - t.pastms()));
			}
		} catch (Exception e) {
			log.error("lock failed, global lock=" + name, e);
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

			locked = false;

			heartbeat.remove(this);

			if (Cache.unlock(name, value)) {
				MQ.notify("lock." + name, 0);
				if (log.isDebugEnabled())
					log.debug("global unlocked, name=" + name + ", locked=" + _time.past());
			} else {
				if (log.isInfoEnabled())
					log.info("what's wrong with the lock=" + name);
			}
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

	public static LockHeartbeat heartbeat = new LockHeartbeat();

	static class LockHeartbeat extends SysTask {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private static List<Lock> locked = new ArrayList<Lock>();

		@Override
		public String getName() {
			return "gi.lock.hb";
		}

		public void add(Lock lock) {
			synchronized (locked) {
				if (!locked.contains(lock)) {
					locked.add(lock);
				}
			}

			this.schedule(10);
		}

		public void remove(Lock lock) {
			synchronized (locked) {
				locked.remove(lock);
			}
		}

		@Override
		public void onExecute() {

			synchronized (locked) {
				if (!locked.isEmpty()) {
					for (Lock l : locked) {
						if (l instanceof GlobalLock) {
							((GlobalLock) l).touch();
						}
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
