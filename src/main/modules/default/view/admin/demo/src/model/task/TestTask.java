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
		return "test.tsk";
	}

	@Override
	public void onFinish() {
		// re-run this task at a minute alter
		if (!stop) {
			this.schedule(X.AMINUTE);
		}
	}

	@Override
	public void onExecute() {
		// do something
		try {
			// mapreduce, create sub task for the stream and dispatch the task to each node
			// and each core
			Task.mapreduce(Arrays.asList(1, 2, 3, 4), e -> {
				return e * 100;
			}, e -> {
				System.out.println(e);
			});
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public static void start() {
		inst.stop = false;
		inst.schedule(0);
	}

	public static void stop() {
		inst.stop = true;
		inst.stop(true);
	}

}
