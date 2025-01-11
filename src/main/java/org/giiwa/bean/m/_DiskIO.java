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

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.misc.Host;

@Table(name = "gi_m_diskio", memo = "GI-磁盘IO监测")
public class _DiskIO extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_DiskIO.class);

	public static BeanDAO<String, _DiskIO> dao = BeanDAO.create(_DiskIO.class);

	@Column(memo = "主键", unique = true, size = 50)
	String id;

	@Column(memo = "节点", size = 50)
	String node;

	@Column(memo = "路径", size = 100)
	public String path;

	@Column(memo = "读字节总数")
	public long readbytes;

	@Column(memo = "读速度")
	public long _reads;

	@Column(memo = "写字节总数")
	public long writebytes;

	@Column(memo = "写速度")
	public long writes;

	@Column(memo = "队列")
	public double queue;

	public synchronized static void update(String node, List<Host._DS> l1) {

//		Map<String, JSON> l2 = new HashMap<String, JSON>();

		for (Host._DS jo : l1) {

			// insert or update
			String path = jo.path;
			String name = jo.name;
			if (X.isIn(name, "tmpfs", "devtmpfs")) {
				continue;
			}

			if (X.isEmpty(path) || X.isEmpty(name)) {
				log.error(jo, new Exception("name or path missed"));
				break;
			}

			_Disk d1 = _Disk.dao.load(W.create().and("node", node).and("path", path));
			if (d1 == null)
				continue;

			String id = UID.id(node, path);
			try {

//				name = name.replace("[\\\\]", "/");
				V v = V.create();

				v.append("node", node).force("name", name).remove("_id", X.ID);

				Record r1 = Record.dao.load(W.create().and("node", node).and("path", path).sort("created", -1));
				if (r1 != null && jo.readbytes > r1.readbytes) {
					long time = (System.currentTimeMillis() - r1.getCreated()) / 1000;
					v.force("_reads", (jo.readbytes - r1.readbytes) / time);
					v.force("writes", (jo.writebytes - r1.writebytes) / time);
				} else {
					v.force("_reads", v.value("reads"));
				}
				v.remove("reads");

				// insert
				if (dao.exists2(id)) {
					dao.update(id, v.copy());
				} else {
					dao.insert(v.copy().force(X.ID, id));
				}

				if (!Record.dao.exists2(W.create().and("node", node).and("path", path).and("created",
						System.currentTimeMillis() - X.AMINUTE, W.OP.gt))) {
					// save to record per hour
					Record.dao
							.insert(v.copy().force(X.ID, UID.id(id, System.currentTimeMillis())).append("node", node));
				}
			} catch (Exception e) {
				log.error(jo, e);
			}
		}
	}

	@Table(name = "gi_m_diskio_record", memo = "GI-磁盘IO监测历史")
	public static class Record extends _DiskIO {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static BeanDAO<String, Record> dao = BeanDAO.create(Record.class);

		public void cleanup() {
			dao.delete(W.create().and("created", System.currentTimeMillis() - X.AMONTH, W.OP.lt));
		}

	}

}
