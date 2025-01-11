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
import java.util.concurrent.atomic.AtomicLong;

import org.giiwa.bean.GLog;
import org.giiwa.bean.Node;
import org.giiwa.cache.GlobalLock;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.net.mq.MQ;
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

		W q = Node.dao.query().and("giiwa", null, W.OP.neq);
		q.and("lastcheck", System.currentTimeMillis() - Node.LOST, W.OP.gte);

		List<GlobalLock._Lock> lock = new ArrayList<GlobalLock._Lock>();

		try {

			AtomicLong has = new AtomicLong(q.count());

			GLog.applog.info("sys", "lock", "MQ1, has=" + has.get() + ", q=" + q);

			MQ.callTopic(Task.MQNAME, "list_lock", "", 5000, req -> {

				String from = req.from;

				try {

					List<GlobalLock._Lock> l1 = req.get();

					synchronized (lock) {
						lock.addAll(l1);
					}

				} catch (Exception e) {
					GLog.applog.error("sys", "task", "from=" + from + ", error=" + e.getMessage(), e);
				}

				has.decrementAndGet();
				if (has.get() <= 0) {
					return true;
				} else {
					return false;
				}
			});
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			GLog.applog.error("task", "checking", e.getMessage(), e);
		}

		Collections.sort(lock, new Comparator<GlobalLock._Lock>() {

			@Override
			public int compare(GlobalLock._Lock o1, GlobalLock._Lock o2) {
				return X.compareTo(o1.name, o2.name);
			}

		});

		this.set("list", lock);

		this.show("/admin/lock.index.html");

	}

	@Path(path = "kill", login = true, access = "access.config.admin", oplog = true)
	public void kill() {
		String name = this.getString("name");
		GlobalLock.kill(name);

		this.send(JSON.create().append(X.STATE, 200).append(X.MESSAGE, "killed"));

	}

	@Path(path = "trace", login = true, access = "access.config.admin", oplog = true)
	public void trace() {
		String name = this.getString("name");

		GlobalLock._Lock e = GlobalLock.getLock(name);
		if (e != null) {
			this.set(X.MESSAGE, e.trace);
		}

		this.send(200);

	}

}
