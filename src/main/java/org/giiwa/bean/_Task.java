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
package org.giiwa.bean;

import java.util.Base64;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.task.Task;
import org.giiwa.web.Controller;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_task", memo = "GI-全局任务")
public class _Task extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Task.class);

	public static final BeanDAO<String, _Task> dao = BeanDAO.create(_Task.class);

	public static final long LOST = 10 * 1000;

	@Column(memo = "id")
	private String id;

	@Column(memo = "task name")
	private String name;

	@Column(memo = "heartbeat")
	private long lasttime;

	@Column(memo = "running node")
	private String _node;

	@Column(memo = "task body")
	private String body;

	public static void update(Task t) {

		Task.schedule(() -> {
			try {
				String name = t.getName();
				if (dao.exists(name)) {
					dao.delete(name);
				}
				V v = V.create();
				v.append(X.ID, name);
				v.append("name", name);
				v.append("lasttime", System.currentTimeMillis());
				v.append("body", _body(t));

				dao.insert(v);

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		});

	}

	public static void remove(Task t) {
		dao.delete(t.getName());
	}

	private static String _body(Task t) throws Exception {
		byte[] bb = X.getBytes(t, true);
		return Base64.getEncoder().encodeToString(bb);
	}

	private Task getTask_obj() {
		try {
			byte[] bb = Base64.getDecoder().decode(body);
			return X.fromBytes(bb, true);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public static void touch() {
		// 6 seconds
		try {
			List<?> l1 = dao.query().and("_node", Local.id()).distinct(X.ID);
			if (l1 == null || l1.isEmpty()) {
				return;
			}

			for (Object o : l1) {
				String name = o.toString();
				if (Task.isRunning(name)) {
					dao.update(name, V.create("lasttime", System.currentTimeMillis()));
				}
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	public static void recover() {

		try {

			List<?> l1 = dao.query().and("updated", System.currentTimeMillis() - LOST, W.OP.lte).distinct(X.ID);
			if (l1 == null || l1.isEmpty())
				return;

			for (Object o : l1) {
				String name = o.toString();

				_Task t = dao.load(name);
				if (System.currentTimeMillis() - t.lasttime > X.ADAY || (X.isSame(t._node, Local.id())
						&& System.currentTimeMillis() - Controller.UPTIME > X.AMINUTE)) {
					dao.delete(name);
					continue;
				}

				Task t1 = t.getTask_obj();
				if (t1 != null) {
					if (t1.isScheduled()) {
						dao.delete(name);
					} else {
						// recover
						t1.schedule(0, true);
					}
				}
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

}
