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
package org.giiwa.core.base;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.framework.web.Language;
import org.giiwa.framework.web.Module;

// TODO: Auto-generated Javadoc
/**
 * The {@code Shell} Class lets run shell command
 * 
 * @author joe
 *
 */
public class Shell {

  /** The log. */
  static Log log = LogFactory.getLog(Shell.class);

  public static enum Logger {
    error("ERROR"), warn("WARN"), info("INFO");

    String level;

    /**
     * Instantiates a new logger.
     *
     * @param s
     *          the s
     */
    Logger(String s) {
      this.level = s;
    }

  };

  /**
   * Run.
   * 
   * @param command
   *          the command
   * @return the string
   * @throws Exception
   *           the exception
   */
  public static String run(String command) throws Exception {
    return run(command, null, null);
  }

  /**
   * Run.
   *
   * @param command
   *          the command
   * @param passwd
   *          the passwd
   * @param print
   *          the print
   * @return the string
   * @throws Exception
   *           the exception
   */
  public static String run(String command, String passwd, IPrint print) throws Exception {
    StringBuilder sb = new StringBuilder();
    BufferedReader input = null;
    BufferedReader err = null;
    if (log.isDebugEnabled())
      log.debug("shell.run: " + command);

    try {
      Process p = Runtime.getRuntime().exec(new String[] { "/bin/bash", "-c", command });

      err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

      input = new BufferedReader(new InputStreamReader(p.getInputStream()));

      String line = input.readLine();
      while (line != null) {
        if (line.toLowerCase().indexOf("password") > 0 && !X.isEmpty(passwd)) {
          p.getOutputStream().write((passwd + "\n").getBytes());
        }
        if (print != null) {
          print.print(line);
        } else {
          sb.append(line).append("\r\n");
        }
        line = input.readLine();
      }

      line = err.readLine();
      while (line != null) {
        if (line.toLowerCase().indexOf("password") > 0 && !X.isEmpty(passwd)) {
          p.getOutputStream().write((passwd + "\n").getBytes());
        }
        if (print != null) {
          print.print(line);
        } else {
          sb.append(line).append("\r\n");
        }
        line = err.readLine();
      }

      if (sb.length() > 0)
        return sb.toString();

      p.destroy();

      if (log.isDebugEnabled()) {
        log.debug("result: " + sb.toString());
      }
      return sb.toString();
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    } finally {
      if (input != null) {
        input.close();
      }
      if (err != null) {
        err.close();
      }
    }
  }

  /**
   * Log.
   *
   * @param ip
   *          the ip
   * @param level
   *          the level
   * @param module
   *          the module
   * @param message
   *          the message
   */
  // 192.168.1.1#系统名称#2014-10-31#ERROR#日志消息#程序名称
  public static void log(String ip, Logger level, String module, String message) {
    String deli = Module.home.get("log_deli", "#");
    StringBuilder sb = new StringBuilder();
    sb.append(ip).append(deli);
    sb.append("support").append(deli);
    sb.append(Language.getLanguage().format(System.currentTimeMillis(), "yyyy-MM-dd hh:mm:ss"));
    sb.append(deli).append(level.name()).append(deli).append(message).append(deli).append(module);

    try {
      Shell.run("logger " + level.level + deli + sb.toString());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public interface IPrint {

    /**
     * Prints the.
     *
     * @param line
     *          the line
     */
    void print(String line);
  }

  private static int _linux = -1;

  public static boolean isLinux() {
    if (_linux == -1) {
      try {
        String uname = Shell.run("uname -a");
        _linux = uname.indexOf("Linux") > -1 || uname.indexOf("Darwin") > -1 ? 1 : 0;
      } catch (Exception e) {
        _linux = 0;
      }
    }
    return _linux == 1;
  }

  private static int _ubuntu = -1;

  public static boolean isUbuntu() {
    if (_ubuntu == -1) {
      try {
        String uname = Shell.run("uname -a");
        _ubuntu = uname.indexOf("Ubuntu") > -1 ? 1 : 0;
      } catch (Exception e) {
        return false;
      }
    }
    return _ubuntu == 1;
  }

}
