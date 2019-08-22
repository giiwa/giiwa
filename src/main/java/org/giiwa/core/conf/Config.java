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
package org.giiwa.core.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.*;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.log4j.PropertyConfigurator;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

/**
 * The Class Config is whole configuration of system, usually is a copy of
 * "giiwa.properties"
 */
public final class Config {

	/** The conf. */
	private static PropertiesConfiguration conf;

	/** The conf name. */
	private static File confFile;

	/**
	 * Initializes the conf file
	 * 
	 * @param confFile the conf name
	 */
	public static void init(String confFile) {
		init(new File(confFile));
	}

	public static void initLog() {

		if (confFile != null && new File(confFile.getParent() + File.separator + "log4j.properties").exists()) {
			PropertyConfigurator.configure(confFile.getParent() + File.separator + "log4j.properties");
		} else {
			Properties prop = new Properties();
			prop.setProperty("log4j.rootLogger", "error, stdout");
			prop.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
			prop.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
			prop.setProperty("log4j.appender.stdout.layout.ConversionPattern", "%p [%t] [%d] - %m - [%l]%n");
			prop.setProperty("log4j.logger.org.giiwa", "debug");

			PropertyConfigurator.configure(prop);
		}

	}

	/**
	 * initialize the conf with the file
	 * 
	 * @param file the conf file
	 */
	@SuppressWarnings({ "rawtypes" })
	public static void init(File file) {

		Reader re = null;
		try {
			confFile = file;

			initLog();

			PropertiesConfiguration c1 = null;

			if (file != null && file.exists()) {
				c1 = new PropertiesConfiguration();
				re = new InputStreamReader(new FileInputStream(file), "UTF-8");
				c1.read(re);
				re.close();
				re = null;
//				c1.setEncoding("utf-8");
//				System.out.println("load config: " + file);
			}

			if (c1 != null) {
				if (conf == null) {
					conf = c1;
				} else {
					conf.append(c1);
				}
			}

			if (conf == null) {
				conf = new PropertiesConfiguration();
			}

			// conf.addProperty("home", home);

			List<?> list = conf.getList("@include");
			if (list != null && !list.isEmpty()) {
				Set<String> ss = new HashSet<String>();
				ss.addAll(X.toString(list));
//				System.out.println("include:" + ss);

				for (String s : ss) {
					if (s.startsWith(File.separator)) {
						if (new File(s).exists()) {
							PropertiesConfiguration c2 = new PropertiesConfiguration();
							Reader r2 = new InputStreamReader(new FileInputStream(file), "UTF-8");
							c2.read(r2);
//							PropertiesConfiguration c = new PropertiesConfiguration(s);
//							c.setEncoding("utf-8");
							// reloader.add(s);

							conf.append(c2);
						} else {
							System.out.println("Can't find the configuration file, file=" + s);
						}
					} else {
						String s1 = file.getParent() + "/conf/" + s;
						if (new File(s1).exists()) {

							PropertiesConfiguration c2 = new PropertiesConfiguration();
							Reader r2 = new InputStreamReader(new FileInputStream(s1), "UTF-8");
							c2.read(r2);
//
//							PropertiesConfiguration c = new PropertiesConfiguration(s1);
//							c.setEncoding("utf-8");
							// reloader.add(s1);

							conf.append(c2);
						} else {
							System.out.println("Can't find the configuration file, file=" + s1);
						}

					}
				}
			}

			/**
			 * set some default value
			 */
			if (!conf.containsKey("site.name")) {
				conf.setProperty("site.name", "default");
			}

//			System.out.println("conf=" + conf);

			Iterator it = conf.getKeys();
			while (it.hasNext()) {
				Object name = it.next();
				Object v = conf.getProperty(name.toString());
				if (v != null && v instanceof String) {
					String s = (String) v;

					int i = s.indexOf("${");
					while (i > -1) {
						int j = s.indexOf("}", i + 2);
						String n = s.substring(i + 2, j);
						String s1 = System.getProperty(n);

						if (s1 != null) {
							s = s.substring(0, i) + s1 + s.substring(j + 1);
							i = s.indexOf("${");
						}
					}
					conf.setProperty(name.toString(), s);
				}
			}

			// check and upgrade
			checkAndUpgrade();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			X.close(re);
		}
	}

	/**
	 * initialize the conf with the prop
	 * 
	 * @param prop the properties
	 */
	public static void init(Properties prop) {

		init();

		for (Object k : prop.keySet()) {
			conf.addProperty(k.toString(), prop.getProperty(k.toString()));
		}
	}

	/**
	 * initialize a empty conf
	 * 
	 */
	public static void init() {
		init((File) null);
	}

	private static void checkAndUpgrade() {

//		System.out.println("check and update");

		boolean c = false;

		String s = conf.getString("db[default].url", X.EMPTY);
		if (X.isEmpty(s)) {
			conf.setProperty("db[default].url", conf.getString("db.url", X.EMPTY));
			conf.setProperty("db.url", null);
			c = true;
		}

		s = conf.getString("db[default].user", X.EMPTY);
		if (X.isEmpty(s)) {
			conf.setProperty("db[default].user", conf.getString("db.user", X.EMPTY));
			conf.setProperty("db.user", null);
			c = true;
		}

		s = conf.getString("db[default].passwd", X.EMPTY);
		if (X.isEmpty(s)) {
			conf.setProperty("db[default].passwd", conf.getString("db.passwd", X.EMPTY));
			conf.setProperty("db.passwd", null);
			c = true;
		}

		s = conf.getString("mongo[default].url", X.EMPTY);
		if (X.isEmpty(s)) {
			conf.setProperty("mongo[default].url", conf.getString("mongo[prod].url", X.EMPTY));
			conf.setProperty("mongo[prod].url", null);
			c = true;
		}

		s = conf.getString("mongo[default].db", X.EMPTY);
		if (X.isEmpty(s)) {
			conf.setProperty("mongo[default].db", conf.getString("mongo[prod].db", X.EMPTY));
			conf.setProperty("mongo[prod].db", null);
			c = true;
		}

		if (!conf.containsKey("node.name")) {
			conf.setProperty("node.name", conf.getString("node", X.EMPTY));
			conf.setProperty("node", null);
			c = true;
		}

		if (!conf.containsKey("cluster.code")) {
			conf.setProperty("cluster.code", conf.getInt("system.code", 0));
			conf.setProperty("system.code", null);
			c = true;
		}

		if (c) {
			save();
		}
	}

	/**
	 * Gets the config.
	 * 
	 * @return the config
	 */
	public static Configuration getConf() {
		if (conf == null) {
			conf = new PropertiesConfiguration();
		}
		return conf;
	}

	/**
	 * set the configuration back to the file.
	 */
	public static void save() {

		if (conf != null && confFile != null) {
			conf.setProperty("home", null);

			try {
				PropertiesConfiguration c1 = new PropertiesConfiguration();
//				PropertiesConfiguration c1 = new PropertiesConfiguration(confFile);
//				c1.setEncoding("utf-8");

				Iterator<String> it = conf.getKeys();
				while (it.hasNext()) {
					String name = it.next();
					Object v1 = conf.getProperty(name);
					String v2 = c1.getString(name, X.EMPTY);
					if (v2.indexOf("${") == -1) {
						c1.setProperty(name, v1);
					}
				}

				Writer out = new FileWriter(confFile);
				c1.getLayout().save(c1, out);
				out.close();
//				c1.save(confFile);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static String _id = null;

	/**
	 * get the name of this node in cluster.
	 *
	 * @return string of name
	 */
	public static String id() {
		if (X.isEmpty(_id)) {
			_id = getConf().getString("node.id", X.EMPTY);
			if (X.isEmpty(_id)) {
				_id = UID.uuid();
				getConf().setProperty("node.id", _id);
				Config.save();
			}
		}
		return _id;
	}

}
