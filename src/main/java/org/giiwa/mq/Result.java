package org.giiwa.mq;

import java.io.Closeable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.mq.MQ.Request;

public class Result<T> extends IStub implements Closeable {

	static Log log = LogFactory.getLog(Result.class);

	LinkedBlockingQueue<Request> l1 = new LinkedBlockingQueue<Request>();

	public static <T> Result<T> create(String name) {
		return new Result<T>(name);
	}

	private Result(String name) {
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

	public Object[] readObject(long timeout) throws Exception {

		Request r = l1.poll(timeout, TimeUnit.MILLISECONDS);

		return new Object[] { r.from, r.get() };
	}

	public T read(long timeout) throws Exception {
		Request r = l1.poll(timeout, TimeUnit.MILLISECONDS);
		return r.get();
	}

}
