package org.giiwa.core.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;

public class LiveHand {

	private boolean live = true;
	private long count = 0;
	private long max = 0;
	private long timeout = 0;

	private Lock lock = new ReentrantLock();
	private Condition door = lock.newCondition();

	private TimeStamp created = TimeStamp.create();

	private transient Map<String, Object> attachs = new HashMap<String, Object>();

	public void timeoutAfter(long timeout) {
		this.timeout = created.pastms() + timeout;
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

	public LiveHand(long timeout, long max) {
		this.timeout = timeout;
		this.max = max;
	}

	public boolean isLive() {
		return live && (timeout <= 0 || created.pastms() < timeout);
	}

	public void stop() {
		try {
			live = false;
			lock.lock();
			door.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public void hold() throws InterruptedException {
		try {
			lock.lock();
			while (max > 0 && count >= max) {
				long waittime = X.AMINUTE;
				if (timeout > 0) {
					waittime = timeout - created.pastms();
				}
				if (waittime > 0) {
					door.awaitNanos(TimeUnit.MILLISECONDS.toNanos(waittime));
				} else {
					throw new InterruptedException("timeout for hold the hand");
				}
			}

			count++;

		} finally {
			lock.unlock();
		}
	}

	public void drop() {
		try {
			lock.lock();
			count--;
			door.signalAll();
		} finally {
			lock.unlock();
		}
	}

	public boolean await() throws InterruptedException {
		if (timeout < 0) {
			return await(Long.MAX_VALUE - created.pastms());
		}
		return await(timeout - created.pastms());
	}

	public boolean await(long timeout) throws InterruptedException {

		TimeStamp t = TimeStamp.create();

		long t1 = timeout - t.pastms();
		try {
			lock.lock();
			while (t1 > 0 && isLive()) {
				if (count <= 0)
					return true;

				// log.debug("thread=" + Thread.interrupted());
				door.awaitNanos(TimeUnit.MILLISECONDS.toNanos(timeout));
				t1 = timeout - t.pastms();
				// System.out.println("count=" + count);
			}
		} finally {
			lock.unlock();
		}
		return count <= 0;
	}

	@Override
	public String toString() {
		return "LiveHand [live=" + live + ", count=" + count + ", timeout=" + timeout + ", past=" + created.pastms()
				+ "ms, attachs=" + attachs + "]";
	}

	public static void main(String[] args) {
		LiveHand h = new LiveHand(-1, 20);
		System.out.println("holding");
		try {
			h.hold();
			h.await(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("done");

	}
}
