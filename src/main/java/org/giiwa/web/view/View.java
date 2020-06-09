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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.app.Velocity;
import org.giiwa.dao.X;
import org.giiwa.dfile.DFile;
import org.giiwa.json.JSON;
import org.giiwa.web.Controller;

public abstract class View {

	static Log log = LogFactory.getLog(View.class);

	/**
	 * parse the file with the model
	 * 
	 * @param file     the file
	 * @param m        the model
	 * @param viewname the template name
	 * @return true: successful,
	 * @throws Exception if occur error
	 */
	protected abstract boolean parse(Object file, Controller m, String viewname) throws Exception;

	/**
	 * parse the file with the params
	 * 
	 * @param file
	 * @param params
	 * @return
	 */
	public abstract String parse(Object file, JSON params);

	/**
	 * init the views by config
	 * 
	 * @param config the config
	 */
	public static void init(Map<String, String> config) {

		try {

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

		for (String name : config.keySet()) {
			if (name.startsWith(".")) {
				String value = config.get(name);
				try {
					View v = (View) Class.forName(value).newInstance();
					views.put(name, v);
				} catch (Exception e1) {
					log.error(value, e1);
				}
			}
		}

		if (log.isDebugEnabled()) {
			log.debug("View Parser: ");
			for (String name : views.keySet()) {
				log.debug("\t" + name + "=" + views.get(name).getClass().getName());
			}
		}
	}

	public static void add(String name, Class<? extends View> clazz) throws Exception {
		View v = clazz.newInstance();
		views.put(name, v);
	}

	/**
	 * parse the file with the model
	 * 
	 * @param file     the file
	 * @param m        the model
	 * @param viewname the template name
	 * @throws Exception if occur error
	 */
	public static void merge(File file, Controller m, String viewname) throws Exception {

		String name = file.getName();
		for (String suffix : views.keySet()) {
			if (name.endsWith(suffix)) {
				View v = views.get(suffix);
				v.parse(file, m, viewname);
				return;
			}
		}

		fileview.parse(file, m, viewname);
	}

	public static void merge(DFile f, Controller m, String viewname) throws Exception {

		for (String suffix : views.keySet()) {
			if (viewname.endsWith(suffix)) {
				View v = views.get(suffix);
				v.parse(f, m, viewname);
				return;
			}
		}

		fileview.parse(f, m, viewname);
	}

	private static Map<String, View> views = new HashMap<String, View>();
	private static FileView fileview = new FileView();

	protected static InputStream getInputStream(Object file) throws IOException {
		if (file instanceof DFile) {
			return ((DFile) file).getInputStream();
		} else {
			return new FileInputStream((File) file);
		}
	}

	public static String getName(Object file) {
		if (file instanceof DFile) {
			return ((DFile) file).getFilename();
		} else {
			return X.getCanonicalPath(((File) file).getAbsolutePath());
		}
	}

	public static long lastModified(Object file) {
		if (file instanceof DFile) {
			return ((DFile) file).lastModified();
		} else {
			return ((File) file).lastModified();
		}
	}

	public static long length(Object file) {
		if (file instanceof DFile) {
			return ((DFile) file).length();
		} else {
			return ((File) file).length();
		}
	}

	public static String getCanonicalPath(Object file) throws IOException {
		if (file instanceof DFile) {
			return ((DFile) file).getCanonicalPath();
		} else {
			return ((File) file).getCanonicalPath();
		}
	}

	/**
	 * get velocity view parser
	 * 
	 * @return
	 */
	public static View getVelocity() {
		return new VelocityView();
	}

}
