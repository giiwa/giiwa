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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Temp;
import org.giiwa.conf.Global;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.task.Console;
import org.giiwa.web.Language;

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
		_checkOS();
		return _linux == 1;
	}

	public static boolean isMac() {
		_checkOS();
		return _mac == 1;
	}

	private static int _mac = -1;
	private static int _linux = -1;
	private static int _ubuntu = -1;
	private static int _centos = -1;

	/**
	 * test is ubuntu
	 * 
	 * @return true if is ubuntu
	 */
	public static boolean isUbuntu() {
		_checkOS();
		return _ubuntu == 1;
	}

	public static boolean isCentOS() {
		_checkOS();
		return _ubuntu == 1;
	}

	private static void _checkOS() {
		if (_ubuntu == -1 && _centos == -1) {
			try {
				String uname = Shell.run("uname -a", 5 * 1000);
				_ubuntu = uname.indexOf("Ubuntu") > -1 ? 1 : 0;
				_centos = uname.indexOf("centos") > -1 ? 1 : 0;
				_mac = uname.indexOf("Darwin") > -1 ? 1 : 0;
				_linux = uname.indexOf("Linux") > -1 ? 1 : 0;

			} catch (Exception e) {
				// ignore
			}
		}
	}

	/**
	 * run a command with the out, err and in.
	 */
	@SuppressWarnings("deprecation")
	public static String run(String cmd, long timeout) throws IOException {

		TimeStamp t = TimeStamp.create();
		BufferedReader out = null;
		BufferedReader err = null;

		StringBuilder sb = new StringBuilder();
		Process p = null;
		try {

			p = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmd });

			out = new BufferedReader(new InputStreamReader(p.getInputStream()));
			err = new BufferedReader(new InputStreamReader(p.getErrorStream()));

			String line = out.readLine();
			while (line != null && t.pastms() < timeout) {
				Console.inst.log(line);
				sb.append(line).append("\r\n");
				line = out.readLine();
			}

			line = err.readLine();
			while (line != null && t.pastms() < timeout) {
				Console.inst.log(line);
				sb.append(line).append("\r\n");
				line = err.readLine();
			}

		} finally {

			X.close(out, err);

			if (p != null) {

				IOUtils.closeQuietly(p.getOutputStream());
				IOUtils.closeQuietly(p.getErrorStream());
				IOUtils.closeQuietly(p.getInputStream());

				p.destroy();
			}
		}
		return sb.toString();
	}

	public static String bash(String cmd, long timeout) {

		StringBuilder sb = new StringBuilder();
		Temp t1 = Temp.create("a");
		try {

			PrintStream out = new PrintStream(t1.getOutputStream());
			out.println("#!/bin/bash");
			out.println(cmd);
			out.close();

			File f = t1.getFile();
			Shell.run("chmod ugo+x " + f.getAbsolutePath(), X.AMINUTE);

			sb.append(Shell.run(f.getAbsolutePath(), timeout));

			t1.delete();
		} catch (Exception e) {
			log.error(cmd, e);
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

	public static long getFIO() {
		if (isLinux()) {

			String pid = Shell.pid();
			String line = "lsof -p " + pid + " -n|wc -l";
			try {
				String r = run(line, X.AMINUTE);
				return X.toLong(r);
			} catch (Exception e) {
				// ignore
			}
		}
		return 0;
	}

	public static double[] usage(String pid) {
		String s = Shell.bash("top -b -n 1 -p " + pid, 3000);
		if (log.isDebugEnabled()) {
			log.debug("monitor cpu, top=" + s);
		}

		String[] s1 = X.split(s, "\n");
		if (s1 != null) {
			for (String s3 : s1) {
				if (s3.startsWith(pid)) {
					s1 = X.split(s3, "[ ]");
					// CPU, MEM
					return new double[] { X.toDouble(s1[8]), X.toDouble(s1[9]) };
				}
			}
		}
		return null;
	}

	public static List<JSON> process(List<String[]> apps) {

		List<JSON> l1 = JSON.createList();
		for (String[] ss : apps) {

			try {
				String s = Shell.bash("ps -ef |grep " + ss[1], 3000);
				// user pid
				if (log.isDebugEnabled()) {
					log.info("s=" + s);
				}

				String[] s1 = X.split(s, "\n");
				if (s1 != null) {
					boolean found = false;
					double cpu = 0;
					double mem = 0;
					for (String s2 : s1) {
						try {
							if (!s2.contains("grep ")) {

								StringFinder sf = StringFinder.create(s2);
								sf.trim().nextTo(" "); // skip user
								String pid = sf.trim().nextTo(" ");

								double[] usage = usage(pid);
								if (usage != null) {
									cpu += usage[0];
									mem += usage[1];
								}
							}
						} catch (Exception e) {
							log.error(s, e);
						}
					}

					if (found) {
						JSON j1 = JSON.create();
						j1.append("name", ss[0]);

						j1.append("cpu", cpu);
						j1.append("mem", mem);
						l1.add(j1);
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return l1;

	}

	private static String _pid = null;

	public static String pid() {

		if (_pid == null) {
			/**
			 * @author joe
			 * 
			 *         this may cause hung, make sure the /etc/hosts [host] and hostname
			 *         [host1] same
			 */

			String name = ManagementFactory.getRuntimeMXBean().getName();
			_pid = X.split(name, "[@]")[0];
		}
		return _pid;
	}

	public static List<JSON> threads() {

		String pid = pid();

		String s = Shell.bash("top -H -b -n 1 -p " + pid, 3000);
		if (log.isDebugEnabled())
			log.debug("s=" + s);

		/**
		 * PID USER PR NI VIRT RES SHR S %CPU %MEM TIME+ COMMAND <br>
		 * 1751183 root 20 0 21.4g 11.8g 44832 R 35.3 30.3 10:36.79 dformat2.42.8.h <br>
		 * 1751128 root 20 0 21.4g 11.8g 44832 S 17.6 30.3 5:05.41 dformat2.29.11. <br>
		 */

		List<JSON> l1 = JSON.createList();

		String[] ss = X.split(s, "\n");
		if (ss != null) {
			for (String s1 : ss) {
				String[] ss1 = X.split(s1.trim(), "[ ]");
				if (ss1 != null && ss1.length == 12 && X.isNumber(ss1[0])) {
					l1.add(JSON.create().append("pid", ss1[0]).append("cpu", X.toFloat(ss1[8]))
							.append("mem", X.toFloat(ss1[9])).append("name", ss1[11]));
				}
			}
		}

		return l1;

	}

	public static void setTime(long t) {
		if (Shell.isLinux()) {
			String cmd = "date -s " + Language.getLanguage().format(t, "yyyy-MM-dd HH:mm:ss");
			try {
				Runtime.getRuntime().exec(cmd);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static void kill(long pid) {

		try {
			if (isLinux() || isMac()) {
				String line = "kill -9 " + pid + ";";

				run(line, X.AMINUTE);

			} else {

				run("taskkill /F /PID " + pid, 5 * 1000);

			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
