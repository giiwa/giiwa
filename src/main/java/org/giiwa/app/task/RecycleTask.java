package org.giiwa.app.task;

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.X;
import org.giiwa.core.conf.Local;
import org.giiwa.core.task.Task;
import org.giiwa.framework.web.Model;

public class RecycleTask extends Task {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(RecycleTask.class);

	public static RecycleTask owner = new RecycleTask();

	private RecycleTask() {
	}

	@Override
	public void onExecute() {
		String s = Local.getString("recycle.task", "-1");
		if ((!X.isSame(s, "-1")) && System.currentTimeMillis() - Model.UPTIME > X.AHOUR) {
			/**
			 * recycle.task="-1" or " ", and the server started after 1 hour
			 */
			String[] ss = s.split("\\|");

			Calendar c = Calendar.getInstance();
			c.setTimeInMillis(System.currentTimeMillis());
			int hour = c.get(Calendar.HOUR_OF_DAY);
			for (String s1 : ss) {
				if (hour == X.toInt(s1, -1)) {
					// yes
					recycle();
					break;
				}
			}
		}
	}

	private void recycle() {
		long t = X.toLong(Math.random() * X.AMINUTE, X.AMINUTE);
		log.warn("going to recycle in [" + t / 1000 + "] seconds");

		new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				System.exit(0);
			}

		}.schedule(t);
	}

	@Override
	public void onFinish() {
		this.schedule(X.AHOUR);
	}

}
