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
package org.giiwa.web;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Node;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.task.AtomicShort;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class GiiwaServlet extends HttpServlet {

	public static ServletContext s️ervletContext;

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static Log log = LogFactory.getLog(GiiwaServlet.class);

	public static boolean INITED = false;

	private static AtomicShort _seq = new AtomicShort();

	@Override
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		if (!INITED) {
			throw new IOException("not inited");
		}

		TimeStamp t = TimeStamp.create();

		try {

			TPS.add();

			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			Thread.currentThread().setName("http." + _seq.incrementAndGet());

			RequestHelper r1 = RequestHelper.create((HttpServletRequest) req);
			HttpServletResponse r2 = (HttpServletResponse) resp;

			final String uri = r1.getRequestURI();

			if (log.isDebugEnabled()) {
				log.debug(req.getMethod() + " - " + uri + " - " + _ip(req));
			}

			String _domain = Global.getString("cross.domain", "");

			if (!X.isEmpty(_domain)) {
				r2.addHeader("Access-Control-Allow-Origin", _domain);
			}

			Controller mo = null;
			try {
				log.info(r1.getMethod() + " - " + uri + ", incoming - " + _ip(req));
				mo = Controller.process(uri, r1, r2, req.getMethod(), t);
			} catch (Exception e) {
				throw new IOException(e);
			} finally {
				if (t.pastms() > 3000) {
					// 超过3秒
					String body = mo == null ? null : mo.json().toString();
					if (body != null && body.length() > 100) {
						body = body.substring(0, 97) + "...";
					}
					log.warn(r1.getMethod() + " - " + uri + ", cost=" + t.past() + " - " + _ip(req) + ", body=" + body);
				} else if (log.isInfoEnabled()) {
					log.info(r1.getMethod() + " - " + uri + ", cost=" + t.past() + " - " + _ip(req));
				}
			}
		} finally {
			TPS.dec(t.pastms());
		}

	}

	public static long online() {
		return TPS.online;
	}

	private String _ip(HttpServletRequest req) {

		StringBuilder sb = new StringBuilder();

		String remote = req.getHeader("X-Real-IP");
		if (!X.isEmpty(remote)) {
			sb.append(remote);
		}

		remote = req.getHeader("X-Forwarded-For");
		if (!X.isEmpty(remote)) {
			if (sb.length() > 0)
				sb.append("->");
			sb.append(remote);
		}

		remote = req.getRemoteAddr();
		if (!X.isEmpty(remote)) {
			if (sb.length() > 0)
				sb.append("->");
			sb.append(remote);
		}

		return sb.toString();
	}

	static class TPS {

		static long total; // 总请求
		static long last; // 上1分钟请求数
		static long lastduration; // 上一次时长
		static long lastcost; // 上1分钟耗时

		static long now; // 当前分钟请求数
		static long nowcost; // 当前分钟耗时
		static long online; // 当前处理的请求

		static long started = 0;

		private static final ReentrantLock door = new ReentrantLock();

		static {
			door.lock();
			try {
				Node n = Local.node();
				total = (n != null) ? n.totalrequest : 0;
			} finally {
				door.unlock();
			}
		}

		public static void add() {
			door.lock();
			try {
				now++;
				online++;
				total++;
			} finally {
				door.unlock();
			}
		}

		public static void dec(long cost) {
			door.lock();
			try {
				online--;
				nowcost += cost;
			} finally {
				door.unlock();
			}
		}

		public static long get() {
			door.lock();
			try {
				if (Global.now() - started > X.AMINUTE) {
					last = now;
					lastcost = nowcost;
					lastduration = Global.now() - started;

					now = 0;
					started = Global.now();
					nowcost = 0;

					Node.dao.update(Local.id(), V.create().append("totalrequest", total));
				}
				return last * 1000 / lastduration;
			} finally {
				door.unlock();
			}
		}

		public static long latency() {
			if (last > 0) {
				return lastcost / last;
			}
			return 0;
		}

	}

	public static long tps() {
		return TPS.get();
	}

	public static long total() {
		return TPS.total;
	}

	public static long latency() {
		return TPS.latency();
	}

}
