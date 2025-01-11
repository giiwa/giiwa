/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.net.mq;

import java.io.Closeable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.net.mq.MQ.Request;

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
			this.destroy();
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
