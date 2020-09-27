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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.License;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.web.view.View;

public class GiiwaFilter implements Filter {

	static Log log = LogFactory.getLog(GiiwaFilter.class);

	@Override
	public void destroy() {

	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
			throws IOException, ServletException {

		if (!X.INITED)
			throw new IOException("not inited");

		TimeStamp t = TimeStamp.create();

		HttpServletRequest r1 = (HttpServletRequest) req;
		HttpServletResponse r2 = (HttpServletResponse) resp;

		String uri = r1.getRequestURI();
		while (uri.indexOf("//") > -1) {
			uri = uri.replaceAll("//", "/");
		}

		try {

			/**
			 * rewrite uri
			 */
//			uri = URL.rewrite(uri);

			String method = r1.getMethod();

			log.info("method=" + method);

			String domain = Global.getString("cross.domain", "");

			if ("GET".equalsIgnoreCase(method)) {
				if (!X.isEmpty(domain)) {
					r2.addHeader("Access-Control-Allow-Origin", domain);
				}
				Controller.process(uri, r1, r2, "GET", t);

			} else if ("POST".equalsIgnoreCase(method)) {
				if (!X.isEmpty(domain)) {
					r2.addHeader("Access-Control-Allow-Origin", domain);
				}

				Controller.process(uri, r1, r2, "POST", t);

			} else if ("OPTIONS".equals(method)) {
				r2.setStatus(200);
				r2.addHeader("Access-Control-Allow-Origin", domain);
				r2.addHeader("Access-Control-Allow-Headers", Global.getString("cross.header", "forbidden"));
				r2.getOutputStream().write(domain.getBytes());
			} else if ("HEAD".equals(method)) {
				log.warn("HEAD - uri=" + uri);
			}

			// chain.doFilter(req, resp);
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		} finally {
			if (log.isInfoEnabled())
				log.info(r1.getMethod() + " - " + uri + ", cost=" + t.past());
		}

	}

	@Override
	public synchronized void init(FilterConfig c1) throws ServletException {
		
		try {
			
			if (log.isDebugEnabled())
				log.debug("filter initing model ...");
			GiiwaServlet.s️ervletContext = c1.getServletContext();

			if (log.isDebugEnabled())
				log.debug("filter initing view ...");
			
			View.init();

//			License.init();

		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		log.info("giiwa is ready for service, modules=" + Module.getAll(true) + ", top=" + Module.getHome());

	}

}
