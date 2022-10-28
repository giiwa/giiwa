package org.giiwa.task;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;

/**
 * used to communicating between multiple thread<br>
 * it contains a semaphore used to control the man in hold <br>
 * 
 * @author joe
 *
 */
public class LiveHand implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(LiveHand.class);

	private boolean live = true;
	private long timeout = 0;
	private int max = 0;
	private int available = 0;

	private TimeStamp created = TimeStamp.create();

	private transient Map<String, Object> attachs = new HashMap<String, Object>();

	public synchronized void max(int n) {
		max = n;
		this.notifyAll();
	}

	public void timeoutAfter(long timeout) {
		this.timeout = created.pastms() + timeout;
	}

	public synchronized void increase() {
		max++;
		available++;

		if (log.isDebugEnabled()) {
			log.debug("max=" + max + ", available=" + available);
		}

		this.notifyAll();
	}

	public synchronized void descrease() {
		max--;
		available--;
		this.notifyAll();
	}

	public void put(String name, Object value) {
		attachs.put(name, value);
	}

	@SuppressWarnings("unchecked")
	public <T> T get(String name) {
		return (T) attachs.get(name);
	}

	public Set<String> keys() {
		return attachs.keySet();
	}

	public long getTimeout() {
		return timeout;
	}

	public static LiveHand create(int max) {
		return create(null, -1L, max, max);
	}

	public static LiveHand create(String name, int max) {
		return create(name, -1L, max, max);
	}

	public static LiveHand create(int max, int init) {
		return create(null, -1L, max, init);
	}

	public static LiveHand create(String name, int max, int init) {
		return create(name, -1L, max, init);
	}

	public static LiveHand create(long timeout, int max) {
		return create(null, timeout, max, max);
	}

	public static LiveHand create(long timeout, int max, int init) {
		return create(null, timeout, max, init);
	}

	private static Map<String, WeakReference<LiveHand>> _cache = new HashMap<String, WeakReference<LiveHand>>();

	public static synchronized LiveHand create(String name, long timeout, int max, int init) {

		LiveHand h = null;
		if (X.isEmpty(name)) {
			h = new LiveHand(timeout, max, init);
		} else {
			WeakReference<LiveHand> e = _cache.get(name);
			if (e != null) {
				h = e.get();
			}

			if (h == null) {
				h = new LiveHand(timeout, max, init);
				_cache.put(name, new WeakReference<LiveHand>(h));
			}

		}
		return h;

	}

	private LiveHand(long timeout, int max, int init) {
		this.timeout = timeout;
		if (max < 0) {
			max = Integer.MAX_VALUE;
		}
		this.max = max;
		this.available = init;
	}

	public boolean isLive() {
		return live && (timeout <= 0 || created.pastms() < timeout);
	}

	public synchronized void stop() {
		live = false;
		available = max;
		this.notifyAll();
	}

	public synchronized boolean tryLock(long timeout) throws InterruptedException {

		TimeStamp t = TimeStamp.create();
		try {

			while (isLive()) {

				if (log.isDebugEnabled())
					log.debug("max=" + max + ", available=" + available);

				if (available > 0) {
					available--;
					return true;
				}

				long waittime = X.AMINUTE;
				if (timeout > 0) {
					waittime = timeout - t.pastms();
				}

				if (waittime > 0) {
					this.wait(waittime);
				} else {
					throw new InterruptedException(
							"timeout [" + timeout + "] for holding the hand, max=" + max + ", available=" + available);
				}
			}
			return false;
		} finally {
			if (log.isDebugEnabled())
				log.debug("hold, cost=" + t.past() + ", available=" + available + ", " + this);
		}

	}

	public synchronized boolean tryLock() {
		if (available > 0) {
			available--;
			return true;
		}
		return false;
	}

	public synchronized boolean lock() throws InterruptedException {

		TimeStamp t = TimeStamp.create();
		try {

			while (isLive()) {

				if (log.isDebugEnabled()) {
					log.debug("max=" + max + ", available=" + available);
				}

				if (available > 0) {
					available--;
					return true;
				}

				long waittime = X.AMINUTE;
				if (timeout > 0) {
					waittime = timeout - created.pastms();
				}

				if (waittime > 0) {
					this.wait(waittime);
				} else {
					throw new InterruptedException("timeout for holding the hand");
				}
			}
			return false;
		} finally {
			if (log.isDebugEnabled())
				log.debug("hold, cost=" + t.past() + ", available=" + available + ", " + this);
		}
	}

	public synchronized void release() {
		available++;
		if (available > max) {
			available = max;
		}

		if (log.isDebugEnabled()) {
			log.debug("release, avaliable=" + available + ", " + this);
		}

		this.notifyAll();
	}

	public boolean await() throws InterruptedException {
		if (timeout < 0) {
			return await(Long.MAX_VALUE - created.pastms());
		}
		return await(timeout - created.pastms());
	}

	public synchronized boolean await(long timeout) throws InterruptedException {

		TimeStamp t = TimeStamp.create();

		long t1 = timeout - t.pastms();
		while (t1 > 0 && isLive()) {
			if (available >= max) {
				if (log.isDebugEnabled()) {
					log.debug("await, cost=" + t.past() + ", " + this);
				}
				return true;
			}

			this.wait(t1);

			t1 = timeout - t.pastms();
		}

		if (log.isDebugEnabled()) {
			log.debug("await timeout, available=" + t.past() + ", " + this);
		}

		return isLive();
	}

	@Override
	public String toString() {
		return "LiveHand [@" + Integer.toHexString(super.hashCode()) + ", " + (live ? "alive" : "over") + ", " + max
				+ ">" + available + ", timeout=" + timeout + ", age=" + created.past() + ", attachs=" + attachs + "]";
	}

}
