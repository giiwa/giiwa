package org.giiwa.mq;

import java.io.Closeable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.mq.MQ.Request;

public class Queue<T> extends IStub implements Closeable {

	static Log log = LogFactory.getLog(Queue.class);

	LinkedBlockingQueue<Request> l1 = new LinkedBlockingQueue<Request>();

	public static <T> Queue<T> create(String name) {
		return new Queue<T>(name);
	}

	private Queue(String name) {
		super(name);
	}

	@Override
	public void onRequest(long seq, Request req) {
		try {
			l1.put(req);
		} catch (InterruptedException e) {
			log.error(e.getMessage(), e);
		}
	}

	public void close() {
		try {
			this.unbind();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public Object[] read(long timeout) throws InterruptedException {

		Request r = l1.poll(timeout, TimeUnit.MILLISECONDS);

		return new Object[] { r.from, r.get() };
	}

}
