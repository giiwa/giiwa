package org.giiwa.task;

import java.io.Serializable;
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
	private int door = 0;

	private TimeStamp created = TimeStamp.create();

	private transient Map<String, Object> attachs = new HashMap<String, Object>();

	public void timeoutAfter(long timeout) {
		this.timeout = created.pastms() + timeout;
	}

	public synchronized void increase() {
		max++;
		door++;

		log.debug("max=" + max + ", door=" + door);
		this.notifyAll();
	}

	public synchronized void descrease() {
		max--;
		door--;
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
		return create(max, max);
	}

	public static LiveHand create(int max, int init) {
		return new LiveHand(-1L, max, init);
	}

	public static LiveHand create(long timeout, int max, int init) {
		return new LiveHand(timeout, max, init);
	}

	public static LiveHand create(long timeout, int max) {
		return new LiveHand(timeout, max);
	}

	public LiveHand(long timeout, int max) {
		this(timeout, max, max);
	}

	public LiveHand(long timeout, int max, int init) {
		this.timeout = timeout;
		if (max < 0)
			max = Integer.MAX_VALUE;
		this.max = max;
		this.door = init;
	}

	public boolean isLive() {
		return live && (timeout <= 0 || created.pastms() < timeout);
	}

	public synchronized void stop() {
		live = false;
		door = max;
		this.notifyAll();
	}

	/**
	 * @return
	 */
	public boolean tryHold() {
		return tryLock();
	}

	public synchronized boolean tryLock() {
		if (door > 0) {
			door--;
			return true;
		}
		return false;
	}

	/**
	 * @return
	 * @throws InterruptedException
	 */
	public boolean hold() throws InterruptedException {
		return lock();
	}

	public synchronized boolean lock() throws InterruptedException {

		TimeStamp t = TimeStamp.create();
		try {

			while (isLive()) {

				log.debug("max=" + max + ", door=" + door);

				if (door > 0) {
					door--;
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
				log.debug("hold, cost=" + t.past() + ", door=" + door + ", " + this);
		}
	}

	/**
	 * release the hand
	 */
	public void drop() {
		release();
	}

	public synchronized void release() {
		door++;
		if (door > max)
			door = max;

		if (log.isDebugEnabled())
			log.debug("drop, door=" + door + ", " + this);

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
			if (door >= max) {
				if (log.isDebugEnabled())
					log.debug("await, cost=" + t.past() + ", " + this);
				return true;
			}

			this.wait(t1);

			t1 = timeout - t.pastms();
		}

		if (log.isDebugEnabled())
			log.debug("await timeout, door=" + t.past() + ", " + this);

		return isLive();
	}

	@Override
	public String toString() {
		return "LiveHand [@" + Integer.toHexString(super.hashCode()) + ", " + (live ? "alive" : "over") + ", " + max
				+ ">" + door + ", timeout=" + timeout + ", age=" + created.past() + ", attachs=" + attachs + "]";
	}

	public static void main(String[] args) {
		LiveHand h = LiveHand.create(5);
		System.out.println("holding");
		try {
			h.lock();
			h.release();
			h.await(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("done");

	}
}
