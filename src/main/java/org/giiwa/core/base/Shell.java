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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.TimeStamp;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.framework.bean.Temp;
import org.giiwa.framework.web.Language;

/**
 * The {@code Shell} Class lets run shell command.
 *
 * @author joe
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
		 * @param s the s
		 */
		Logger(String s) {
			this.level = s;
		}

	};

	/**
	 * Log.
	 *
	 * @param ip      the ip
	 * @param level   the level
	 * @param module  the module
	 * @param message the message
	 */
	// 192.168.1.1.系统名称.2014-10-31.ERROR.日志消息.程序名称
	public static void log(String ip, Logger level, String module, String message) {
		String deli = Global.getString("log_deli", ".");
		StringBuilder sb = new StringBuilder();
		sb.append(ip).append(deli);
		sb.append("support").append(deli);
		sb.append(Language.getLanguage().format(System.currentTimeMillis(), "yyyy-MM-dd hh:mm:ss"));
		sb.append(deli).append(level.name()).append(deli).append(message).append(deli).append(module);

		try {
			Shell.run("logger " + level.level + deli + sb.toString(), 10 * 1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * test is Linux
	 * 
	 * @return true if linux
	 */
	public static boolean isLinux() {
		if (_linux == -1) {
			try {
				String uname = Shell.run("uname -a", 5 * 1000);
				if (log.isDebugEnabled())
					log.debug("uname -a=" + uname);
				_linux = uname.indexOf("Linux") > -1 ? 1 : 0;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return _linux == 1;
	}

	public static boolean isMac() {
		if (_mac == -1) {
			try {
				String uname = Shell.run("uname -a", 5 * 1000);
				if (log.isDebugEnabled())
					log.debug("uname -a=" + uname);
				_mac = uname.indexOf("Darwin") > -1 ? 1 : 0;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return _mac == 1;
	}

	private static int _mac = -1;
	private static int _linux = -1;
	private static int _ubuntu = -1;

	/**
	 * test is ubuntu
	 * 
	 * @return true if is ubuntu
	 */
	public static boolean isUbuntu() {
		if (_ubuntu == -1) {
			try {
				String uname = Shell.run("uname -a", 5 * 1000);
				_ubuntu = uname.indexOf("Ubuntu") > -1 ? 1 : 0;
			} catch (Exception e) {
				return false;
			}
		}
		return _ubuntu == 1;
	}

	/**
	 * run a command with the out, err and in.
	 */
	public static String run(String cmd, long timeout) throws IOException {

		TimeStamp t = TimeStamp.create();
		BufferedReader re = null;
		StringBuilder sb = new StringBuilder();
		Process p = null;
		try {

			p = Runtime.getRuntime().exec(cmd);

			re = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = re.readLine();
			while (line != null && t.pastms() < timeout) {
				sb.append(line).append("\r\n");
				line = re.readLine();
			}
			re.close();

			re = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			line = re.readLine();
			while (line != null && t.pastms() < timeout) {
				sb.append(line).append("\r\n");
				line = re.readLine();
			}

		} finally {
			X.close(re);
			if (p != null) {
				p.destroy();
			}
		}
		return sb.toString();
	}

	public static String bash(String cmd, long timeout) throws IOException {

		TimeStamp t = TimeStamp.create();
		BufferedReader re = null;
		StringBuilder sb = new StringBuilder();
		Process p = null;
		Temp t1 = Temp.create("a");
		try {

			File f = t1.getFile();
			f.getParentFile().mkdirs();
			PrintStream out = new PrintStream(new FileOutputStream(f));
			out.println("#!/bin/bash");
			out.println(cmd);
			out.close();

			Runtime.getRuntime().exec("chmod ugo+x " + f.getAbsolutePath());

			p = Runtime.getRuntime().exec(f.getAbsolutePath());

			re = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = re.readLine();
			while (line != null && t.pastms() < timeout) {
				sb.append(line).append("\r\n");
				line = re.readLine();
			}
			re.close();

			re = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			line = re.readLine();
			while (line != null && t.pastms() < timeout) {
				sb.append(line).append("\r\n");
				line = re.readLine();
			}

		} finally {
			t1.delete();
			X.close(re);
			if (p != null) {
				p.destroy();
			}
		}
		return sb.toString();
	}

	/**
	 * get the status of the processname.
	 *
	 * @param processname the process name
	 * @return the status of the process
	 * @throws IOException throw IOException if occur error
	 */
	public static String getProcessStatus(String processname) throws IOException {
		if (isLinux() || isMac()) {

			String line = "ps -ef";
			String r = run(line, 5 * 1000);
			StringBuilder sb = new StringBuilder();
			String[] ss = r.split("\n");
			if (ss != null && ss.length > 0) {
				for (String s : ss) {
					if (s.contains(processname)) {
						sb.append(s).append("\n");
					}
				}
			}

			return sb.toString();
		} else {

			String cmd = "tasklist /nh /FI \"IMAGENAME eq " + processname + "\"";
			return run(cmd, 5 * 1000);

		}

	}

	/**
	 * Kill.
	 *
	 * @param processname the processname
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void kill(String processname) throws IOException {
		if (isLinux() || isMac()) {
			String line = "kill -9 `ps -ef | grep " + processname + " | awk '{print $2}'`;";

			run(line, X.AMINUTE);

		} else {

			String cmd = "tasklist /nh /FI \"IMAGENAME eq " + processname + "\"";

			String line = run(cmd, 5 * 1000);
			String[] lineArray = line.split(" ");
			String pid = lineArray[17].trim();
			run("taskkill /F /PID " + pid, 5 * 1000);

		}
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		try {
//			String s1 = "/usr/local/bin/node /Users/joe/d/temp/zong.js http://dicom.giisoo.com:19999/ehrview/pageControlDC.action?disease=zyjl&eventId=320106466000838|C0002|01015210201807260000711&zjhm=320111111111111111&zjlx=01&zxwyh=01@320111111111111111@457 .ser_wrap /Users/joe/d/temp/a1.jpg";
//			String s2 = "/usr/local/bin/node /Users/joe/d/temp/zong.js \"http://dicom.giisoo.com:19999/ehrview/pageControlDC.action?disease=mzjl&eventId=cs320116426061230|C0001|7627686&zjhm=320111111111111111&zjlx=01&zxwyh=01@320111111111111111@457\" \"tr:nth-child(2)\" /Users/joe/d/temp/a2.jpg";
			String s2 = "/usr/local/bin/node /Users/joe/d/temp/zong.js \"http://dicom.giisoo.com:19999/ehrview/pageControlDC.action?disease=mzjl&eventId=cs320116426061230|C0001|7627686&zjhm=320111111111111111&zjlx=01&zxwyh=01@320111111111111111@457\" \"tr:nth-child(2)\" /Users/joe/d/temp/a3.jpg";
//			System.out.println(run(s1, X.AMINUTE));
			System.out.println(run(s2, X.AMINUTE));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
