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
package org.giiwa.mq;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Local;

/**
 * The RPC Class, used to remote method call, and got result until finish
 * execution.
 * 
 * @author wujun
 *
 */
class RPC {

	private static Log log = LogFactory.getLog(RPC.class);

	private static IStub REPLY = null;
	public static long TIMEOUT = X.AMINUTE;
	private static Map<Long, Object> waits = new HashMap<Long, Object>();
	private static AtomicLong seq = new AtomicLong(0);

	/**
	 * Client side api, Call the class.method(args), and got the result until finish
	 * execution.
	 *
	 * @param rpcname
	 *            the rpc service name
	 * @param method
	 *            the method
	 * @param args
	 *            the args object[]
	 * @return the object
	 * @throws Exception
	 *             the exception
	 */
	public synchronized static Response call(String rpcname, Request req, int timeout) throws Exception {
		if (REPLY == null) {
			REPLY = new IStub("client-" + Local.id()) {

				@Override
				public void onRequest(long seq, Request req) {
					Object o = waits.get(seq);
					if (o != null) {
						try {
							synchronized (o) {
								waits.put(seq, req);
								o.notifyAll();
							}
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
				}
			};
			REPLY.bind();
		}

		long s = seq.incrementAndGet();
		Object lock = new Object();

		try {
			req.seq = s;
			waits.put(s, lock);

			req.from = REPLY.name;
			req.type = 0;
			s = MQ.send(rpcname, req);

			synchronized (lock) {
				if (waits.get(s) == lock)
					lock.wait(timeout);
			}
			Response resp = new Response();
			Object o = waits.remove(s);
			if (o != lock) {
				if (o instanceof Request) {
					Request r = (Request) o;
					resp.copy(r);
					resp.state = 200;
					return resp;
				} else {
					resp.state = 500;
					resp.error = "unknow object, o=" + o;
				}
			} else {
				resp.state = 500;
				resp.error = "timeout";
			}
			return resp;
			// throw new Exception("timeout");
		} finally {
			waits.remove(s);
		}
	}

}
