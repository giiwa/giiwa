package org.giiwa.app.task;

import org.giiwa.app.web.admin.backup;
import org.giiwa.core.conf.Global;
import org.giiwa.core.conf.Local;
import org.giiwa.core.task.Task;

public class BackupTask extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static BackupTask owner = new BackupTask();

	@Override
	public String getName() {
		return "backup.auto.task";
	}

	@Override
	public void onExecute() {
		if (Global.getInt("backup.clean", 0) == 1) {
			// clean up
			int days = Global.getInt("backup.keep.days", 7);
			backup.BackupTask.clean(days);
		}

		if (Local.getInt("backup.auto", 0) == 1) {
			new backup.BackupTask(null).schedule(0);
		}

	}

	@Override
	public void onFinish() {
		if (Local.getInt("backup.auto", 0) == 1) {
			this.schedule(Local.getString("backup.point", "2:00"));
		}
	}

	public static void init() {
		owner.onFinish();
	}

}
