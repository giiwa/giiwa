package org.giiwa.core.task;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;

public class LiveHand {

	private boolean live = true;
	private long count = 0;
	private long max = 0;
	private long timeout = 0;
	private TimeStamp created = TimeStamp.create();

	private transient Map<String, Object> attachs = new HashMap<String, Object>();

	public void timeoutAfter(long timeout) {
		this.timeout = created.pastms() + timeout;
	}

	public void put(String name, Object value) {
		attachs.put(name, value);
	}

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

	public synchronized void stop() {
		live = false;
		this.notifyAll();
	}

	public synchronized void hold() {
		count++;
		if (max > 0 && count > max) {
			try {
				this.wait(X.AMINUTE);
			} catch (InterruptedException e) {
				// ignore
			}
		}
	}

	public synchronized void drop() {
		count--;
		this.notifyAll();
	}

	public synchronized boolean await() throws InterruptedException {
		if (timeout <= 0) {
			return await(Long.MAX_VALUE - created.pastms());
		}
		return await(timeout - created.pastms());
	}

	public synchronized boolean await(long timeout) throws InterruptedException {

		TimeStamp t = TimeStamp.create();

		long t1 = timeout - t.pastms();
		while (isLive()) {
			if (count <= 0)
				return true;
			this.wait(t1);
			t1 = timeout - t.pastms();
			// System.out.println("count=" + count);
		}
		return count <= 0;

	}

	@Override
	public String toString() {
		return "LiveHand [live=" + live + ", count=" + count + ", timeout=" + timeout + ", past=" + created.pastms()
				+ "ms, attachs=" + attachs + "]";
	}

}
