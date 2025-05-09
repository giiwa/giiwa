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

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicLong;

import org.giiwa.bean.GLog;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.net.mq.MQ.Request;
import org.giiwa.task.Function;

class RPC extends IStub {

//	private static Log log = LogFactory.getLog(RPC.class);

	public static String name = "rpc_" + Local.id();

	private static Map<Long, Stack<Request>> waiter = new HashMap<Long, Stack<Request>>();

	public static RPC inst = new RPC();

	private static AtomicLong seq = new AtomicLong(0);

	private RPC() {
		super(name);
	}

	@Override
	public void onRequest(long seq, Request req) {

//		log.warn("got seq=" + seq + ", from=" + req.from);

		Stack<Request> l1 = waiter.get(seq);
		if (l1 != null) {
			synchronized (l1) {
				l1.push(req);
				l1.notifyAll();
			}
		} else {
			// ignore, may caller return once got one result
//			log.warn("MQ1, not waiter! seq=" + seq + ", " + waiter.keySet());
		}

	}

	public static <T> T call(String name, Request req, final long timeout) throws Exception {

		TimeStamp t = TimeStamp.create();

		req.from = RPC.name;
		req.seq = seq.incrementAndGet();

		Stack<Request> l1 = new Stack<Request>();
		waiter.put(req.seq, l1);

		MQ.send(name, req);

		try {
			while (timeout > t.pastms()) {
				synchronized (l1) {
					if (l1.isEmpty()) {
						l1.wait(timeout - t.pastms());
					}
				}
				if (!l1.isEmpty()) {
					Request r = l1.pop();
					return r.get();
				}
			}
		} catch (Exception e) {
			GLog.applog.error("mq", "call", "call failed", e);
			throw e;
		} finally {
			waiter.remove(req.seq);
		}

		throw new Exception("timeout(" + timeout + "ms)");
	}

	public static boolean call(String name, Request req, final long timeout, Function<Request, Boolean> func)
			throws Exception {

		req.from = RPC.name;
		req.seq = seq.incrementAndGet();
		
		Stack<Request> l1 = null;
		if (func != null) {
			l1 = new Stack<Request>();
			waiter.put(req.seq, l1);
		}

		MQ.topic(name, req);

		try {

			TimeStamp t = TimeStamp.create();

			if (func != null) {
				while (timeout > t.pastms()) {

					Request e = null;
					synchronized (l1) {
						if (l1.isEmpty()) {
							l1.wait(timeout - t.pastms());
						}
						if (!l1.isEmpty()) {
							e = l1.pop();
						}
					}

					if (e != null) {
						boolean r = func.apply(e);
						if (r) {
							// 完成， 结束
							return true;
						} // 继续获取下一个
					}
				}

				throw new Exception("Timeout " + timeout + ", name=" + name);

			} else {
				return true;
			}
		} finally {
			waiter.remove(req.seq);
		}
	}

}
