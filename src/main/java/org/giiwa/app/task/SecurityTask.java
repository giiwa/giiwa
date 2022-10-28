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
package org.giiwa.app.task;

import java.io.File;
import java.io.IOException;
import java.util.Stack;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.misc.Shell;
import org.giiwa.task.SysTask;

/**
 * The Class SecurityTask.
 */
public class SecurityTask extends SysTask {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The owner.
	 */
	public static SecurityTask inst = new SecurityTask();

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.getName()
	 */
	@Override
	public String getName() {
		return "gi.security";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onExecute()
	 */
	@SuppressWarnings("deprecation")
	@Override
	public void onExecute() {

		if (Global.getInt("security.task", 0) == 0) {
			return;
		}

		if (Shell.isLinux()) {
			try {
				// "who /var/log/wtmp" 系统登录
				String s = Shell.run("who /var/log/wtmp", X.AMINUTE);
				// root pts/5 2021-04-08 12:13 (58.212.133.175)
				X.lines(s, (s1, r) -> {
					if (!GLog.securitylog.exists(s1)) {
						GLog.securitylog.info("sys", "login", s1);
					}
				});

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			// "top" //进程异常

//			try {
//				// "ss -tun" //链接异常
//				// "tcp ESTAB 0 0 172.17.0.4:443 23.83.225.59:46622"
//				String s = Shell.run("ss -tun", X.AMINUTE);
//				X.lines(s, (s1, r) -> {
//					if (!GLog.securitylog.exists(s1)) {
//						GLog.securitylog.info("sys", "tcp", s1);
//					}
//				});
//			} catch (IOException e) {
//				log.error(e.getMessage(), e);
//			}

			// "/var/log/auth.log" //安全警告
			ReversedLinesFileReader re = null;
			try {
				File f1 = new File("/var/log/auth.log");
				if (f1.exists()) {

					re = new ReversedLinesFileReader(f1);
//					re = new BufferedReader(new InputStreamReader(new FileInputStream(f1)));
					String line = re.readLine();
					int n = 0;
					Stack<String> l1 = new Stack<String>();
					while (line != null && n < 100) {
						l1.push(line);
						n++;
						line = re.readLine();
					}
					line = l1.pop();
					while (line != null) {
						if (!GLog.securitylog.exists(line)) {
							if (line.indexOf("Fail") > -1) {
								GLog.securitylog.warn("sys", "auth", line);
							} else {
								GLog.securitylog.info("sys", "auth", line);
							}
						}
						line = l1.pop();
					}
				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} finally {
				X.close(re);
			}

			// "/var/log/secure"
			try {
				File f1 = new File("/var/log/secure");
				if (f1.exists()) {
					re = new ReversedLinesFileReader(f1);
					String line = re.readLine();
					int n = 0;
					Stack<String> l1 = new Stack<String>();
					while (line != null && n < 100) {
						n++;
						l1.push(line);
						line = re.readLine();
					}

					line = l1.pop();
					while (line != null) {
						if (!GLog.securitylog.exists(line)) {
							if (line.indexOf("Fail") > -1) {
								GLog.securitylog.warn("sys", "auth", line);
							} else {
								GLog.securitylog.info("sys", "auth", line);
							}
						}
						line = l1.pop();
					}

				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} finally {
				X.close(re);
			}

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onFinish()
	 */
	@Override
	public void onFinish() {

		if (Shell.isLinux()) {
			this.schedule(X.AMINUTE);
		}

	}

}
