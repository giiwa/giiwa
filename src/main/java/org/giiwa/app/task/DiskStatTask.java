package org.giiwa.app.task;

import org.giiwa.core.bean.X;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.task.Task;
import org.giiwa.framework.bean.Disk;
import org.giiwa.framework.bean.Stat;

public class DiskStatTask extends Task {

	// private static Log log = LogFactory.getLog(AlertStatTask.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static DiskStatTask inst = new DiskStatTask();

	@Override
	public void onExecute() {

		Disk.stat();

		long total = Disk.dao.sum("total", W.create());
		long free = Disk.dao.sum("free", W.create());
		long count = Disk.dao.sum("count", W.create());

		Stat.snapshot("dfile.stat", Stat.SIZE.hour, W.create(), V.create(),
				new long[] { total, free, total - free, count });

	}

	@Override
	public String getName() {
		return "dfile.stat.task";
	}

	@Override
	public void onFinish() {
		this.schedule(X.AHOUR);
	}

	public static void init() {
		inst.schedule((int) (30000 * Math.random()));
	}

}
