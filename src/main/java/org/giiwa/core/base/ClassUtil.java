package org.giiwa.core.base;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.X;

public class ClassUtil {

	private static Log log = LogFactory.getLog(ClassUtil.class);

	public static void main(String[] args) {
		System.out.println(listSubType("org.giiwa.core.base", Bean.class));
	}

	public static <T> List<Class<T>> listSubType(String packname, Class<T> t) {

		List<Class<T>> l1 = new ArrayList<Class<T>>();

		ClassLoader loader = Thread.currentThread().getContextClassLoader();

		_load(l1, loader, packname, t);

		return l1;
	}

	@SuppressWarnings("unchecked")
	private static <T> void _load(List<Class<T>> rlist, ClassLoader loader, String packname, Class<T> t) {

		try {

			String name = packname.replaceAll("\\.", "/");
			Enumeration<URL> en = loader.getResources(name);

			while (en.hasMoreElements()) {
				URL u = en.nextElement();

				if (X.isSame("jar", u.getProtocol())) {

					String jarPath = u.getPath().substring(5, u.getPath().indexOf("!"));
					JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
					Enumeration<JarEntry> entries = jar.entries();

					while (entries.hasMoreElements()) {
						String s = entries.nextElement().getName();
						// System.out.println("s=" + s);

						if (s.startsWith(name) && s.endsWith(".class")) {
							try {
								Class<?> c = Class.forName(s.replaceAll("\\.class$", "").replaceAll("/", "."));
								if (t.isAssignableFrom(c)) {
									rlist.add((Class<T>) c);
								}
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
						}
					}
					jar.close();

				} else {
					File f = new File(u.getFile());

					// System.out.println("name=" + f.getAbsolutePath() + ", protocol=" +
					// u.getProtocol() + ", dir="
					// + f.isDirectory() + ", file=" + f.isFile());

					// JarFile f = new JarFile(u.getFile());

					if (f.isDirectory()) {
						File[] ff = f.listFiles();
						if (ff != null) {
							for (File f1 : ff) {
								if (f1.isFile()) {
									try {
										Class<?> c = Class
												.forName(packname + "." + f1.getName().replaceAll("\\.class$", ""));
										if (t.isAssignableFrom(c)) {
											rlist.add((Class<T>) c);
										}
									} catch (Exception e) {
										log.error(e.getMessage(), e);
									}
								} else if (f1.isDirectory()) {
									_load(rlist, loader, packname + "." + f1.getName(), t);
								}
							}
						}
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
