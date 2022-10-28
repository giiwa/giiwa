package org.giiwa.cache;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;

public class LocalLock implements Lock, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(LocalLock.class);

	private boolean locked = false;
	protected String name;
	private String value;

	protected LocalLock(String name) {
		this.name = name;
		this.value = Long.toString(System.currentTimeMillis());
	}

	public static Lock create(String name) {
		return new LocalLock(name);
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

				if (FileCache.inst.trylock(name)) {

					if (log.isDebugEnabled()) {
						log.debug("local locked, name=" + name + ", cost=" + t.past());
					}

					_time.reset();
					locked = true;
					return true;
				}

				if (expire == 0 || expire < t.pastms()) {
					if (log.isDebugEnabled()) {
						log.debug("local lock failed, name=" + name + ", cost=" + t.past());
					}

					locked = false;
					return false;
				}

				synchronized (FileCache.inst) {
					FileCache.inst.wait(Math.min(1000, expire - t.pastms()));
				}
			}
		} catch (Exception e) {
			log.error("lock failed, global lock=" + name, e);
		}

		locked = false;
		return false;

	}

	@Override
	public String toString() {
		return "LocalLock [name=" + name + "]";
	}

	@Override
	public synchronized void unlock() {
		if (locked) {

			locked = false;

			if (FileCache.inst.unlock(name, value)) {
				synchronized (FileCache.inst) {
					FileCache.inst.notifyAll();
				}
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
		LocalLock other = (LocalLock) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

}
