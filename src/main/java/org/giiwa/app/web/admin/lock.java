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
package org.giiwa.app.web.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.giiwa.bean.GLog;
import org.giiwa.cache.GlobalLock;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;
import org.giiwa.task.Task;
import org.giiwa.web.*;

/**
 * web api: /admin/lock <br>
 * used to manage task,<br>
 * required "access.config.admin"
 * 
 * @author joe
 *
 */
public class lock extends Controller {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.giiwa.framework.web.Model.onGet()
	 */
	@Path(login = true, access = "access.config.admin")
	public void onGet() {

		List<GlobalLock._Lock> lock = new ArrayList<GlobalLock._Lock>();

		try {

			Task.call("list_lock", "", req -> {

				try {

					List<GlobalLock._Lock> l1 = req.get();

					synchronized (lock) {
						lock.addAll(l1);
					}

				} catch (Exception e) {
					GLog.applog.error("sys", "task", "from=" + req.from + ", error=" + e.getMessage(), e);
				}
				return true;

			});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		Collections.sort(lock, new Comparator<GlobalLock._Lock>() {

			@Override
			public int compare(GlobalLock._Lock o1, GlobalLock._Lock o2) {
				return X.compareTo(o1.name, o2.name);
			}

		});

		this.set(X.LIST, lock);

		this.show("/admin/lock.index.html");

	}

	@Path(path = "kill", login = true, access = "access.config.admin", oplog = true, loglevel = "warn")
	public void kill() {
		String name = this.getString(X.NAME);
		GlobalLock.kill(name);

		this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "killed"));

	}

	@Path(path = "trace", login = true, access = "access.config.admin", oplog = true)
	public void trace() {
		String name = this.getString(X.NAME);

		GlobalLock._Lock e = GlobalLock.getLock(name);
		if (e != null) {
			this.set(X.MESSAGE, e.trace);
		}

		this.send(200);

	}

}
