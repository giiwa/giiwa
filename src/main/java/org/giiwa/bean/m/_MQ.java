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
package org.giiwa.bean.m;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Counter;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.net.mq.MQ;

@Table(name = "gi_m_mq", memo = "GI-MQ监测")
public class _MQ extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_MQ.class);

	public static BeanDAO<String, _MQ> dao = BeanDAO.create(_MQ.class);

	@Column(memo = "主键", unique = true, size = 50)
	String id;

	@Column(memo = "节点", size = 50)
	String node;

	@Column(memo = "名称", size = 50)
	String name;

	@Column(memo = "最大")
	long max;

	@Column(memo = "最小")
	long min;

	@Column(memo = "平均")
	long avg;

	@Column(memo = "次数")
	long times;

	public synchronized static void update(String node, Counter.Stat jo) {
		// insert or update
		try {
			String name = jo.name;

			V v = jo.toV();
			v.append("node", node);
			v.remove("_id");

			String id = UID.id(node, name);
			if (dao.exists2(id)) {
				dao.update(id, v.copy());
			} else {
				dao.insert(v.copy().force(X.ID, id));
			}

			Record.dao.insert(v.force(X.ID, UID.id(node, name, Global.now())));

		} catch (Exception e) {
			log.error(jo, e);
		}
	}

	@Table(name = "gi_m_mq_record", memo = "GI-MQ监测历史")
	public static class Record extends _MQ {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static BeanDAO<String, Record> dao = BeanDAO.create(Record.class);

		public void cleanup() {
			dao.delete(W.create().and("created", Global.now() - X.AWEEK, W.OP.lt));
		}

	}

	public static void check() {
		Counter.Stat r = MQ.statRead();
		r.name = "read";
		Counter.Stat w = MQ.statWrite();
		w.name = " write";

		_MQ.update(Local.id(), r);
		_MQ.update(Local.id(), w);

	}

}
