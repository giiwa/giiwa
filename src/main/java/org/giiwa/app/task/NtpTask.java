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

import org.giiwa.bean.GLog;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.misc.Shell;
import org.giiwa.task.Task;

public class NtpTask extends Task {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The owner.
	 */
	public static NtpTask inst = new NtpTask();

	public boolean ok = false;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.getName()
	 */
	@Override
	public String getName() {
		return "gi.ntp";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onExecute()
	 */
	@Override
	public void onExecute() {

		if (Shell.isLinux() || Shell.isMac()) {
			String ntp = Global.getString("ntp.server", "cn.ntp.org.cn");
			if (!X.isEmpty(ntp)) {
				try {

					String r = Shell.bash("ntpdate -u " + ntp, X.AMINUTE);
					if (r.contains("Can't ")) {
						throw new Exception(r);
					}

					GLog.applog.info("ntp", "sync", "NTP syncing: " + r, null, ntp);

					ok = true;

				} catch (Exception e) {
					GLog.applog.error("ntp", "sync", "NTP syncing failed ", e, null, ntp);
					ok = false;
				}
			} else {
				ok = false;
			}
		} else {
			ok = false;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onFinish()
	 */
	@Override
	public void onFinish() {

		if (Shell.isLinux() || Shell.isMac()) {
			this.schedule(X.AHOUR);
		}

	}

}
