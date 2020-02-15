package org.giiwa.demo.task;

import java.util.Arrays;

import org.giiwa.core.bean.X;
import org.giiwa.core.task.Task;

public class TestTask extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static TestTask inst = new TestTask();

	private boolean stop = false;

	@Override
	public String getName() {
		// the task name, MUST global unique in JVM
		return "test.task";
	}

	@Override
	public void onFinish() {
		// re-run this task at a minute alter
		if (!stop) {
			this.schedule(X.AMINUTE, true);
		}
	}

	@Override
	public void onExecute() {
		
		//TODO
		
	}

	public static void start() {
		inst.stop = false;
		inst.schedule(0, true);
	}

	public static void stop() {
		inst.stop = true;
		inst.stop(true);
	}

}
