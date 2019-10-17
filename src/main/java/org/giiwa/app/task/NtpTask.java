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

import org.giiwa.core.base.Shell;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.GLog;

/**
 * The Class NtpTask.
 */
public class NtpTask extends Task {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The owner.
	 */
	public static NtpTask inst = new NtpTask();

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
					String r = Shell.run("ntpdate -u " + ntp, X.AMINUTE);
					GLog.applog.info("ntp", "sync", "NTP syncing: " + r, null, ntp);
				} catch (Exception e) {
					GLog.applog.error("ntp", "sync", "NTP syncing failed ", e, null, ntp);
				}
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

		if (Shell.isLinux() || Shell.isMac()) {
			this.schedule(X.AHOUR);
		}

	}

}
