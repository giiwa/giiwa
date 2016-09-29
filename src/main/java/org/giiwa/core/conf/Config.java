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
import java.util.*;

import org.apache.commons.configuration.*;
import org.apache.log4j.PropertyConfigurator;

/**
 * The Class Config is whole configuration of system, usually is a copy of
 * "giiwa.properties"
 */
public final class Config {

  /** The conf. */
  private static PropertiesConfiguration conf;

  /** The home. */
  private static String                  home;

  /** The conf name. */
  private static String                  confName;

  /**
   * Inits the.
   * 
   * @param homePropName
   *          the home prop name
   * @param confName
   *          the conf name
   * @throws Exception
   *           the exception
   */
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public static void init(String homePropName, String confName) throws Exception {

    Config.confName = confName;

    home = System.getProperty(homePropName);
    if (home == null) {
      home = System.getenv(homePropName);
    }

    if (home == null)
      throw new Exception(homePropName + " does not exist.");

    if (new File(home + File.separator + "log4j.properties").exists()) {
      PropertyConfigurator.configure(home + File.separator + "log4j.properties");
    } else {
      Properties prop = new Properties();
      prop.setProperty("log4j.rootLogger", "error, stdout");
      prop.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
      prop.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
      prop.setProperty("log4j.logger.com.giiwa", "info");

      PropertyConfigurator.configure(prop);
    }

    PropertiesConfiguration c1 = null;
    String file = home + File.separator + confName + ".properties";
    if (new File(file).exists()) {
      c1 = new PropertiesConfiguration(file);
      c1.setEncoding("utf-8");

      System.out.println("load config: " + file);
    } else {
      System.out.println(file + " no found!");
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

    conf.addProperty("home", home);

    List<String> list = conf.getList("@include");
    Set<String> ss = new HashSet<String>();
    ss.addAll(list);
    // System.out.println("include:" + ss);

    for (String s : ss) {
      if (s.startsWith(File.separator)) {
        if (new File(s).exists()) {
          PropertiesConfiguration c = new PropertiesConfiguration(s);
          c.setEncoding("utf-8");
          // reloader.add(s);

          conf.append(c);
        } else {
          System.out.println("Can't find the configuration file, file=" + s);
        }
      } else {
        String s1 = home + "/conf/" + s;
        if (new File(s1).exists()) {
          PropertiesConfiguration c = new PropertiesConfiguration(s1);
          c.setEncoding("utf-8");
          // reloader.add(s1);

          conf.append(c);
        } else {
          System.out.println("Can't find the configuration file, file=" + s1);
        }

      }
    }

    /**
     * set some default value
     */
    if (!conf.containsKey("site.name")) {
      conf.setProperty("site.name", "default");
    }

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

          if (s1 == null) {
            System.out.println("did not set -D" + n + ", but required in " + home + ".properites");
            break;
          } else {
            s = s.substring(0, i) + s1 + s.substring(j + 1);
            i = s.indexOf("${");
          }
        }
        conf.setProperty(name.toString(), s);
      }
    }

  }

  /**
   * Gets the config.
   * 
   * @return the config
   */
  public static Configuration getConfig() {
    return conf;
  }

  /**
   * set the configuration back to the file.
   */
  public static void save() {

    if (conf != null) {
      String file = home + File.separator + confName + ".properties";

      try {
        conf.save(file);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

  }

}
