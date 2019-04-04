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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.OS;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
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
		 * @param s
		 *            the s
		 */
		Logger(String s) {
			this.level = s;
		}

	};

	/**
	 * Log.
	 *
	 * @param ip
	 *            the ip
	 * @param level
	 *            the level
	 * @param module
	 *            the module
	 * @param message
	 *            the message
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
				if (OS.isFamilyUnix()) {
					String uname = Shell.run("uname -a", 5 * 1000);
					log.debug("uname -a=" + uname);
					_linux = uname.indexOf("Linux") > -1 ? 1 : 0;
				} else {
					_linux = 0;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
		return _linux == 1;
	}

	public static boolean isMac() {
		if (_mac == -1) {
			_mac = OS.isFamilyMac() ? 1 : 0;
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
	 *
	 * @param cmd
	 *            the command line
	 * @param out
	 *            the console outputstream
	 * @param err
	 *            the error outputstream
	 * @param in
	 *            the inputstream
	 * @param workdir
	 *            the working dir
	 * @return the result
	 * @throws IOException
	 *             throw IOException if error
	 */
	public static int run(String cmd, OutputStream out, OutputStream err, InputStream in, String workdir, long timeout)
			throws IOException {

		DefaultExecutor executor = new DefaultExecutor();
		ExecuteStreamHandler stream = new PumpStreamHandler(out, err, in);

		try {

			CommandLine cmdLine = CommandLine.parse(cmd);

			int[] exit = new int[513];
			for (int i = 0; i < 512; i++) {
				exit[i] = i - 256;
			}
			// TODO, should ignore all
			exit[512] = -559038737;

			executor.setExitValues(exit);
			executor.setStreamHandler(stream);
			if (!X.isEmpty(workdir)) {
				executor.setWorkingDirectory(new File(workdir));
			}

			// ExecuteWatchdog watchdog = new ExecuteWatchdog(timeout);
			// executor.setWatchdog(watchdog);
			// watchdog.destroyProcess();

			return executor.execute(cmdLine);
		} catch (IOException e) {
			log.error("cmd=" + cmd, e);
			throw e;
		} finally {
			stream.stop();
		}
	}

	public static int run(String cmd, ExecuteWatchdog dog) throws IOException {

		try {

			CommandLine cmdLine = CommandLine.parse(cmd);

			int[] exit = new int[513];
			for (int i = 0; i < 512; i++) {
				exit[i] = i - 256;
			}
			// TODO, should ignore all
			exit[512] = -559038737;

			DefaultExecutor executor = new DefaultExecutor();
			executor.setExitValues(exit);

			executor.setWatchdog(dog);

			return executor.execute(cmdLine);
		} catch (IOException e) {
			log.error("cmd=" + cmd, e);
			throw e;
		}
	}

	/**
	 * using bash to execute the lines.
	 *
	 * @param lines
	 *            the command lines
	 * @param out
	 *            the output stream
	 * @param err
	 *            the error stream
	 * @param in
	 *            the input stream
	 * @param workdir
	 *            the workdir
	 * @return the int
	 * @throws IOException
	 *             throw IOException if error
	 */
	public static int bash(String lines, OutputStream out, OutputStream err, InputStream in, String workdir)
			throws IOException {

		File f = new File(UID.id(lines).toLowerCase() + ".bash");

		try {
			if (!X.isEmpty(workdir)) {
				lines = "cd " + workdir + ";" + lines;
			}
			FileUtils.writeStringToFile(f, lines);

			// Execute the file we just creted. No flags are due if it is
			// executed with bash directly
			CommandLine commandLine = CommandLine.parse("bash " + f.getCanonicalPath());

			ExecuteStreamHandler stream = new PumpStreamHandler(out, err, in);

			DefaultExecutor executor = new DefaultExecutor();
			int[] exit = new int[3];
			for (int i = 0; i < exit.length; i++) {
				exit[i] = i - 1;
			}
			executor.setExitValues(exit);

			executor.setStreamHandler(stream);
			if (!X.isEmpty(workdir)) {
				executor.setWorkingDirectory(new File(workdir));
			}

			return executor.execute(commandLine);

		} catch (IOException e) {
			log.error("lines=" + lines, e);
			throw e;
		} finally {
			f.delete();
		}
	}

	/**
	 * run command, and return the console output.
	 *
	 * @param cmd
	 *            the command line
	 * @return the output of console and error
	 * @throws IOException
	 *             throw IOException if error
	 */
	public static String run(String cmd, long timeout) throws IOException {
		return run(cmd, (String) null, timeout);
	}

	/**
	 * using bash to execute the command.
	 *
	 * @param cmd
	 *            the command
	 * @return the console output
	 * @throws IOException
	 *             throw IOException if error
	 */
	public static String bash(String cmd) throws IOException {
		return bash(cmd, null);
	}

	/**
	 * run the command in workdir.
	 *
	 * @param cmd
	 *            the command
	 * @param workdir
	 *            the path
	 * @return the output of console and error
	 * @throws IOException
	 *             throw IOException if error
	 */
	public static String run(String cmd, String workdir, long timeout) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			run(cmd, out, out, null, workdir, timeout);
			out.flush();
		} finally {
			X.close(out);
		}
		return out.toString();
	}

	/**
	 * using bash to execute the cmd.
	 *
	 * @param cmd
	 *            the command
	 * @param workdir
	 *            the workdir
	 * @return the output string
	 * @throws IOException
	 *             throw IOException if error
	 */
	public static String bash(String cmd, String workdir) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		bash(cmd, out, out, null, workdir);
		out.close();
		return out.toString();
	}

	/**
	 * get the status of the processname.
	 *
	 * @param processname
	 *            the process name
	 * @return the status of the process
	 * @throws IOException
	 *             throw IOException if occur error
	 */
	public static String getProcessStatus(String processname) throws IOException {
		if (isLinux() || OS.isFamilyMac()) {

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
		} else if (OS.isFamilyWindows()) {

			String cmd = "tasklist /nh /FI \"IMAGENAME eq " + processname + "\"";
			return run(cmd, 5 * 1000);

		} else {
			throw new IOException("not support");
		}

	}

	/**
	 * Kill.
	 *
	 * @param processname
	 *            the processname
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public static void kill(String processname) throws IOException {
		if (isLinux() || OS.isFamilyMac()) {
			String line = "kill -9 `ps -ef | grep " + processname + " | awk '{print $2}'`;";

			// Create a tmp file. Write permissions!?
			File f = new File(UID.random(10) + ".bash");
			FileUtils.writeStringToFile(f, line);

			// Execute the file we just creted. No flags are due if it is
			// executed with bash directly
			CommandLine commandLine = CommandLine.parse("bash " + f.getName());

			DefaultExecutor executor = new DefaultExecutor();
			executor.execute(commandLine);
			f.delete();
		} else if (OS.isFamilyWindows()) {

			String cmd = "tasklist /nh /FI \"IMAGENAME eq " + processname + "\"";

			String line = run(cmd, 5 * 1000);
			String[] lineArray = line.split(" ");
			String pid = lineArray[17].trim();
			run("taskkill /F /PID " + pid, 5 * 1000);

		} else {
			throw new IOException("not support");
		}
	}

	/**
	 * The main method.
	 *
	 * @param args
	 *            the arguments
	 */
	public static void main(String[] args) {
		try {
			System.out.println(run("uname -a", 5 * 1000));

			System.out.println();

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
