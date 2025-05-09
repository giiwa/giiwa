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

		try {

			TPS.add();

			Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
			Thread.currentThread().setName("http." + _seq.incrementAndGet());

			TimeStamp t = TimeStamp.create();

			RequestHelper r1 = RequestHelper.create((HttpServletRequest) req);
			HttpServletResponse r2 = (HttpServletResponse) resp;

			String uri = r1.getRequestURI();

			if (log.isDebugEnabled()) {
				log.debug(req.getMethod() + " - " + uri + " - " + _ip(req));
			}

			String _domain = Global.getString("cross.domain", "");

			if (!X.isEmpty(_domain)) {
				r2.addHeader("Access-Control-Allow-Origin", _domain);
			}

			Controller mo = null;
			try {
				mo = Controller.process(uri, r1, r2, req.getMethod(), t);
			} catch (Exception e) {
				throw new IOException(e);
			} finally {
				if (t.pastms() > 3000) {
					// 超过3秒
					log.warn(r1.getMethod() + " - " + uri + ", cost=" + t.past() + " - " + _ip(req) + ", body="
							+ (mo == null ? null : mo.json()));
				} else if (log.isInfoEnabled()) {
					log.info(r1.getMethod() + " - " + uri + ", cost=" + t.past() + " - " + _ip(req));
				}
			}
		} finally {
			TPS.dec();
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

		static long total;
		static long last;
		static long now;
		static long online;

		static long started = 0;

		static boolean inited = false;

		public synchronized static void add() {
			if (!inited) {
				Node n = Local.node();
				total = n.totalrequest;
				inited = true;
			}
			now++;
			online++;
			total++;
			if (Global.now() - started > X.AMINUTE) {
				last = now;
				now = 0;
				started = Global.now();

				Node.dao.update(Local.id(), V.create().append("totalrequest", total));

			}
		}

		public synchronized static void dec() {
			online--;
		}

		public synchronized static long get() {
			return last / 60;
		}

	}

	public static long tps() {
		return TPS.get();
	}

	public static long total() {
		return TPS.total;
	}

}
