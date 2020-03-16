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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_counter", memo = "GI-计数器")
public class Counter extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Counter.class);

	public static final BeanDAO<String, Counter> dao = BeanDAO.create(Counter.class);

	@Column(memo = "唯一序号")
	private String id;

	@Column(memo = "节点")
	private String node;

	@Column(memo = "名称")
	private String name;

	@Column(memo = "计数")
	private long count;

	public synchronized static long set(String name, long count) {
		try {
			String id = UID.id(Local.id(), name);
			if (dao.exists(id)) {
				dao.update(id, V.create("count", count));
			} else {
				dao.insert(V.create(X.ID, id).append("node", Local.id()).append("name", name).append("count", count));
			}
			return count;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	public static void increase(String name) {
		String id = UID.id(Local.id(), name);
		dao.inc(W.create(X.ID, id), "count", 1, null);
	}

	public static void release(String name) {
		String id = UID.id(Local.id(), name);
		dao.inc(W.create(X.ID, id), "count", -1, null);
	}

}
