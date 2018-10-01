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
import org.giiwa.core.cache.RedisCache;
import org.giiwa.core.json.JSON;
import org.giiwa.mq.MQ;

public class GlobalLock implements Lock {

	private static Log log = LogFactory.getLog(GlobalLock.class);

	private String name;
	private RedisCache redis;

	public static Lock create(String name) throws Exception {
		if (Cache.cacheSystem instanceof RedisCache) {

			log.debug("create global lock=" + name);
			GlobalLock l = new GlobalLock();
			l.name = name;
			l.redis = (RedisCache) Cache.cacheSystem;
			if (l.redis != null) {
				return l;
			}
		}

		throw new Exception("need redis");
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
		long expires = unit.toMillis(time);

		log.debug("tryLock, global lock=" + name);

		try {
			while (expires == 0 || expires < t.pastms()) {
				long value = System.currentTimeMillis() + expires;
				long status = redis.setnx(name, String.valueOf(value));

				if (status == 1) {
					touch();
					heartbeat.add(this);

					log.debug("locked, global lock=" + name);

					return true;
				}

				if (expires == 0 || expires > t.pastms()) {
					return false;
				}
				MQ.wait("lock." + name, expires - t.pastms());
			}
		} catch (Exception e) {
			log.warn("lock failed, global lock=" + name, e);
		}

		return false;

	}

	public void touch() {
		redis.expire(name, 12);
	}

	@Override
	public void unlock() {
		heartbeat.remove(this);

		redis.delete(name);

		MQ.notify("lock." + name, JSON.create());

		log.debug("unlocked, global lock=" + name);

	}

	@Override
	public Condition newCondition() {
		return null;
	}

	private static LockHeartbeat heartbeat = new LockHeartbeat();

	private static class LockHeartbeat extends Task {

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

		public void remove(GlobalLock name) {
			synchronized (locked) {
				locked.remove(name);
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
