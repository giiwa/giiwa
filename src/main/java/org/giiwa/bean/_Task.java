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

	@Column(memo = "唯一序号")
	private String id;

	@Column(memo = "任务全局唯一名称")
	private String name;

	@Column(memo = "任务状态心跳")
	private long lasttime;

	@Column(name = "运行节点")
	private String _node;

	@Column(name = "任务体")
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

	private Task getTask_obj() throws Exception {
		byte[] bb = Base64.getDecoder().decode(body);
		return X.fromBytes(bb, true);
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

			List<?> l1 = dao.query().and("lasttime", System.currentTimeMillis() - LOST, W.OP.lte).distinct(X.ID);
			if (l1 == null || l1.isEmpty())
				return;

			for (Object o : l1) {
				String name = o.toString();
				_Task t = dao.load(name);
				Task t1 = t.getTask_obj();
				t1.schedule(0, true);
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

}
