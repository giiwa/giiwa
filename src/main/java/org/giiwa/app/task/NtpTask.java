package org.giiwa.app.task;

import org.giiwa.core.base.Shell;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Global;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.GLog;

public class NtpTask extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static NtpTask owner = new NtpTask();

	@Override
	public String getName() {
		return "ntp.task";
	}

	private NtpTask() {
	}

	@Override
	public void onExecute() {
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

	@Override
	public void onFinish() {
		if (Shell.isLinux() || Shell.isMac()) {
			this.schedule(X.AHOUR);
		}
	}

}
