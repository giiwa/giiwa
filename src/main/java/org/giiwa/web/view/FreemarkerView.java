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

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import org.giiwa.conf.Local;
import org.giiwa.json.JSON;
import org.giiwa.web.Controller;

import freemarker.template.Template;

public class FreemarkerView extends View {

	@Override
	public boolean parse(Object file, Controller m, String viewname) {
		// load
		try {

			Template template = getTemplate(file);
			if (template != null) {

				Writer out = new OutputStreamWriter(m.getOutputStream());
				template.process(m.context, out);
				out.flush();

				return true;
			}
		} catch (Exception e) {
			log.error(View.getName(file), e);
		}
		return false;
	}

	Template getTemplate(Object f) throws IOException {

		String fullname = View.getCanonicalPath(f);
		T t = cache.get(fullname);
		if (t == null || t.last != View.lastModified(f)) {
			if (cache.size() == 0) {
				cfg.setDirectoryForTemplateLoading(new File(Controller.HOME));
			}

			Template t1 = cfg.getTemplate(View.getCanonicalPath(f).substring(Controller.HOME.length()), "UTF-8");
			t = T.create(t1, View.lastModified(f));

			if (Local.getInt("web.debug", 0) == 0) {
				// not debug
				cache.put(fullname, t);
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

	private static Map<String, T> cache = new HashMap<String, T>();
	private static freemarker.template.Configuration cfg = new freemarker.template.Configuration(
			freemarker.template.Configuration.VERSION_2_3_24);
	@Override
	public String parse(Object file, JSON params) {
		// TODO Auto-generated method stub
		return null;
	}

}
