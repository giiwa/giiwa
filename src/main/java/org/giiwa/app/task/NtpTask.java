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
