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
package org.giiwa.web.view;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.giiwa.conf.Local;
import org.giiwa.dao.TimeStamp;
import org.giiwa.json.JSON;
import org.giiwa.web.Controller;

public class VelocityView extends View {

	private static Log log = LogFactory.getLog(VelocityView.class);

	public VelocityView() {

		try {
			// TODO Auto-generated constructor stub
			Properties p = new Properties();
			p.setProperty("resource.default_encoding", "utf-8");
			p.setProperty("output.encoding", "utf-8");
			p.setProperty("log4j.logger.org.apache.velocity", "ERROR");
			p.setProperty("directive.set.null.allowed", "true");
			p.setProperty("resource.loader.file.class", "org.giiwa.web.view.VelocityTemplateLoader");
			Velocity.init(p);
		} catch (Exception e) {
			log.warn(e.getMessage(), e);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public boolean parse(Object file, Controller m, String viewname) throws IOException {

		TimeStamp t = TimeStamp.create();
		Template template = getTemplate(viewname, file);

		// System.out.println(viewname + "=>" + template);
		if (template != null) {
			m.resp.setContentType(m.getResponseContentType());

			BufferedWriter writer = new BufferedWriter(m.resp.getWriter());

			template.merge(new VelocityContext(m.data), writer);
			writer.flush();

			if (log.isDebugEnabled())
				log.debug("cost t=" + t.past() + ", file=" + file);

			return true;
		}
		return false;
	}

	/**
	 * parse the file to string with the json
	 * 
	 * @param file the file of the template
	 * @param m    the json
	 * @return the string of the results
	 */
	public String parse(Object file, JSON m) {

		try {
			Template template = getTemplate(View.getCanonicalPath(file), file);

			if (template != null) {

				StringWriter w = new StringWriter();
				BufferedWriter writer = new BufferedWriter(w);

				template.merge(new VelocityContext(m), writer);
				writer.flush();
				writer.close();
				return w.toString();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Gets the template by viewname, the viewname is relative path
	 * 
	 * @param viewname   the relative view path name
	 * @param allowEmpty if not presented, allow using a empty
	 * @return Template
	 * @throws IOException
	 */
	private Template getTemplate(String viewname, Object file) throws IOException {

		// String fullname = View.getCanonicalPath(f);
		T t = cache.get(viewname);

		if (t == null || t.last != View.lastModified(file)) {

			/**
			 * get the template from the top
			 */
			Template t1 = Velocity.getTemplate(viewname, "UTF-8");
			t = T.create(t1, View.lastModified(file));

			if (Local.getInt("web.debug", 0) == 0) {
				// not debug
				if (cache.size() >= 100) {
					for (int i = 0; i < 10; i++) {
						String s1 = cache.keySet().iterator().next();
						cache.remove(s1);
					}
				}
				cache.put(viewname, t);
			}
		}

		return t == null ? null : t.template;

	}

	private static class T {
		Template template;
		long last;

		static T create(Template t, long last) {
			T t1 = new T();
			t1.template = t;
			t1.last = last;
			return t1;
		}
	}

	/**
	 * cache the template by viewname, the template will be override in child module
	 */
	private static LinkedHashMap<String, T> cache = new LinkedHashMap<String, T>();

}
