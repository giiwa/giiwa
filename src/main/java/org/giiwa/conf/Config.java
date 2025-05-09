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
package org.giiwa.conf;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.giiwa.dao.Helper;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.web.Controller;

/**
 * The Class Config is whole configuration of system, usually is a copy of
 * "giiwa.properties"
 */
public final class Config {

	private static Log log = LogFactory.getLog(Config.class);

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

	public static java.util.logging.Logger getLogger() {
		return java.util.logging.Logger.getLogger("giiwa");
	}

	public static void initLog() {

		if (confFile != null && new File(confFile.getParent() + File.separator + "log4j.properties").exists()) {
			PropertyConfigurator.configureAndWatch(confFile.getParent() + File.separator + "log4j.properties",
					X.AMINUTE);
		} else {
			File f1 = new File(Controller.GIIWA_HOME + "/conf/log4j.properties");
			if (f1.exists()) {
				PropertyConfigurator.configureAndWatch(f1.getAbsolutePath(), X.AMINUTE);
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

	}

	/**
	 * initialize the conf with the file
	 * 
	 * @param file the conf file
	 */
	public static void init(File file) {

		System.out.println("init [" + (file == null ? null : file.getAbsolutePath()) + "]");

		Reader re = null;
		try {

			confFile = file;
			if (confFile != null) {
				Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				Files.setPosixFilePermissions(Paths.get(confFile.getAbsolutePath()), perms);
			}

			initLog();

			PropertiesConfiguration c1 = null;

			if (file != null && file.exists()) {
				c1 = new PropertiesConfiguration();
				re = new InputStreamReader(new FileInputStream(file), "UTF-8");
				c1.read(re);
				re.close();
				re = null;
//				c1.setEncoding("utf-8");
			}

			if (c1 != null) {
				if (conf == null) {
					conf = c1;
				} else {
					conf.append(c1);
				}
			} else if (conf == null) {
				conf = new PropertiesConfiguration();
			}

			// conf.addProperty("home", home);

			/**
			 * set some default value
			 */
			if (!conf.containsKey("site.name")) {
				conf.setProperty("site.name", "default");
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

		boolean c = false;

		if (!conf.containsKey("db.url")) {
			if (conf.containsKey("mongo[default].url")) {
				conf.setProperty("db.url",
						conf.getString("mongo[default].url") + "/" + conf.getString("mongo[default].db"));
				conf.setProperty("mongo[default].url", null);
				if (conf.containsKey("mongo[default].user")) {
					conf.setProperty("db.user", conf.getString("mongo[default].user"));
					conf.setProperty("mongo[default].user", null);
				}
				if (conf.containsKey("mongo[default].passwd")) {
					conf.setProperty("db.passwd", conf.getString("mongo[default].passwd"));
					conf.setProperty("mongo[default].passwd", null);
				}
				if (conf.containsKey("mongo[default].conns")) {
					conf.setProperty("db.conns", conf.getString("mongo[default].conns"));
					conf.setProperty("mongo[default].conns", null);
				}
			} else if (conf.containsKey("db[default].url")) {
				conf.setProperty("db.url", conf.getString("db[default].url") + "/" + conf.getString("db[default].db"));
				conf.setProperty("db[default].url", null);
				if (conf.containsKey("db[default].user")) {
					conf.setProperty("db.user", conf.getString("db[default].user"));
					conf.setProperty("db[default].user", null);
				}
				if (conf.containsKey("db[default].passwd")) {
					conf.setProperty("db.passwd", conf.getString("db[default].passwd"));
					conf.setProperty("db[default].passwd", null);
				}
				if (conf.containsKey("db[default].conns")) {
					conf.setProperty("db.conns", conf.getString("db[default].conns"));
					conf.setProperty("db[default].conns", null);
				}
			}
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
		// possible null, let's throw exception
		return conf;
	}

	/**
	 * set the configuration back to the file.
	 */
	@Deprecated
	public static void save() {

		if (conf != null && confFile != null && !Helper.isConfigured()) {

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

				String id = c1.getString("node.id");
				if (X.isEmpty(id)) {
					id = UID.uuid();
					c1.setProperty("node.id", id);
				}

				// backup old
				SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
				File back = new File(confFile.getCanonicalFile() + "." + sdf.format(Global.now()));
				confFile.renameTo(back);

				Writer out = new FileWriter(confFile);
				c1.getLayout().save(c1, out);
				out.close();

				Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
				perms.add(PosixFilePermission.OWNER_READ);
				perms.add(PosixFilePermission.OWNER_WRITE);
				Files.setPosixFilePermissions(Paths.get(confFile.getAbsolutePath()), perms);

				log.warn("write giiwa.properties by, conf=" + confFile.getAbsolutePath(), new Exception());

//				c1.save(confFile);
			} catch (Exception e) {
				e.printStackTrace();
				log.error("write giiwa.properties error! ", e);
			}
		}
	}

	/**
	 * rename the old properties, and save the new
	 */
	public synchronized static void save2() {

		conf.setProperty("home", null);

		try {
			PropertiesConfiguration c1 = new PropertiesConfiguration();
//				PropertiesConfiguration c1 = new PropertiesConfiguration(confFile);
//				c1.setEncoding("utf-8");

			Iterator<String> it = conf.getKeys();
			while (it.hasNext()) {
				String name = it.next();
				Object v1 = conf.getProperty(name);
				c1.setProperty(name, v1);
			}

			String id = c1.getString("node.id");
			if (X.isEmpty(id)) {
				id = UID.uuid();
				c1.setProperty("node.id", id);
				conf.setProperty("node.id", id);
			}

			// backup old
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
			File back = new File(confFile.getCanonicalFile() + "." + sdf.format(Global.now()));
			confFile.renameTo(back);

			Writer out = new FileWriter(confFile);
			c1.getLayout().save(c1, out);
			out.close();

//				c1.save(confFile);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
