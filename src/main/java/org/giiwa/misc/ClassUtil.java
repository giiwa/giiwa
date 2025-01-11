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
package org.giiwa.misc;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;

public class ClassUtil {

	private static Log log = LogFactory.getLog(ClassUtil.class);

	public static <T> List<Class<T>> listSubType(List<String> packname, Class<T> t) {

		if (log.isDebugEnabled()) {
			log.debug("finding class for [" + t + "] in [" + packname + "]");
		}
		List<Class<T>> l1 = new ArrayList<Class<T>>();

		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		int n = _load(l1, loader, packname, t);

		log.warn("found class/" + n + " for [" + t + "] in [" + packname + "], list=" + l1,
				new Exception("trace only!"));

		return l1;
	}

	@SuppressWarnings("unchecked")
	private static <T> int _load(List<Class<T>> rlist, ClassLoader loader, List<String> packname, Class<T> t) {

		int n = 0;

		try {

			for (String name : packname) {
				name = name.replaceAll("\\.", "/");

				Enumeration<URL> en = loader.getResources(name);

				while (en.hasMoreElements()) {
					URL u = en.nextElement();

					if (X.isSame("jar", u.getProtocol())) {

						String jarPath = u.getPath().substring(5, u.getPath().indexOf("!"));
						JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
						Enumeration<JarEntry> entries = jar.entries();

						while (entries.hasMoreElements()) {
							String s = entries.nextElement().getName();

							if (s.startsWith(name) && s.endsWith(".class")) {

								if (log.isDebugEnabled()) {
									log.debug("s=" + s);
								}

								try {
									synchronized (Class.class) {
										n++;
										Class<?> c = Class.forName(s.replaceAll("\\.class$", "").replaceAll("/", "."));
										if (t.isAssignableFrom(c)) {
											rlist.add((Class<T>) c);
										}
									}
								} catch (Throwable e) {
									log.error(e.getMessage(), e);
								}
							}
						}
						jar.close();

					} else {
						File f = new File(u.getFile());

						if (f.isDirectory()) {
							File[] ff = f.listFiles();
							if (ff != null) {
								for (File f1 : ff) {
									if (f1.isFile()) {
										try {
											n++;
											String clazzname = packname + "."
													+ f1.getName().replaceAll("\\.class$", "");
											clazzname = clazzname.replaceAll("[\\[\\]]", "");
											Class<?> c = Class.forName(clazzname);
											if (t.isAssignableFrom(c)) {
												rlist.add((Class<T>) c);
											}
										} catch (Throwable e) {
											log.error(e.getMessage(), e);
										}
									} else if (f1.isDirectory()) {
										String ch = f1.getName();
										if (!X.isEmpty(packname)) {
											ch = packname + "." + ch;
										}
										n += _load(rlist, loader, Arrays.asList(ch), t);
									}
								}
							}
						}
					}
				}
			}
		} catch (Throwable e) {
			log.error(e.getMessage(), e);
		}

		return n;

	}

}
