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

import java.util.Calendar;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Local;
import org.giiwa.dao.X;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;

/**
 * The Class RecycleTask.
 */
public class RecycleTask extends Task {

	/**
	 * The Constant serialVersionUID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The log.
	 */
	private static Log log = LogFactory.getLog(RecycleTask.class);

	/**
	 * The owner.
	 */
	public static RecycleTask owner = new RecycleTask();

	@Override
	protected boolean isSys() {
		return true;
	}

	@Override
	public String getName() {
		return "gi.recycle";
	}

	/**
	 * Instantiates a new recycle task.
	 */
	private RecycleTask() {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onExecute()
	 */
	@Override
	public void onExecute() {
		String s = Local.getString("recycle.task", "-1");
		if ((!X.isSame(s, "-1")) && System.currentTimeMillis() - Controller.UPTIME > X.AHOUR) {
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

	/**
	 * Recycle.
	 */
	private void recycle() {
		long t = X.toLong(Math.random() * X.AMINUTE, X.AMINUTE);
		log.warn("restart by recycle in [" + t / 1000 + "] seconds");

		new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				
				log.warn("recycle ...");
				Task.schedule(t -> {
					System.exit(0);
				}, 1000);
			}

		}.schedule(t);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.core.task.Task.onFinish()
	 */
	@Override
	public void onFinish() {
		this.schedule(X.AHOUR);
	}

}
