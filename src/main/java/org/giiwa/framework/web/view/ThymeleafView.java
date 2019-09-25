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
package org.giiwa.framework.web.view;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.giiwa.core.json.JSON;
import org.giiwa.framework.web.Controller;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.IContext;
import org.thymeleaf.templateresolver.FileTemplateResolver;

public class ThymeleafView extends View {

	private static TemplateEngine _engine;

	@Override
	public synchronized boolean parse(Object file, Controller m, String viewname) {
		// load
		try {

			if (_engine == null) {
				_engine = new TemplateEngine();
				FileTemplateResolver tt = new FileTemplateResolver();
				tt.setCharacterEncoding("UTF-8");
				_engine.setTemplateResolver(tt);
			}

			Writer out = new OutputStreamWriter(m.getOutputStream());

			_engine.process(View.getCanonicalPath(file), _parse(m.context), out);

			out.flush();

			return true;
		} catch (Exception e) {
			log.error(View.getName(file), e);
		}
		return false;
	}

	private IContext _parse(final Map<String, Object> m) {
		return new IContext() {

			@Override
			public boolean containsVariable(String name) {
				return m.containsKey(name);
			}

			@Override
			public Locale getLocale() {
				return Locale.US;
			}

			@Override
			public Object getVariable(String name) {
				return m.get(name);
			}

			@Override
			public Set<String> getVariableNames() {
				return m.keySet();
			}
		};
	}

	@Override
	public String parse(Object file, JSON params) {
		// TODO Auto-generated method stub
		return null;
	}

}
