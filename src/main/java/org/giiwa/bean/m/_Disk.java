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

import java.io.File;
import java.util.List;

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
import org.giiwa.misc.Host;

@Table(name = "gi_m_disk", memo = "GI-磁盘监测")
public class _Disk extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Disk.class);

	public static BeanDAO<String, _Disk> dao = BeanDAO.create(_Disk.class);

	@Column(memo = "主键", unique = true, size=50)
	String id;

	@Column(memo = "节点", size=50)
	String node;

	@Column(memo = "磁盘", size=50)
	String disk;

	@Column(memo = "路径", size=100)
	public String path;

	@Column(memo = "总空间")
	long total;

	@Column(memo = "已使用")
	long used;

	@Column(memo = "空闲")
	long free;

	public long getUsed() {
		return used;
	}

	public long getFree() {
		return free;
	}

	public synchronized static void update(String node, List<Host._DS> l1) {

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

			if (new File(path).isDirectory()) {
				long total = jo.total;
				if (total < 10 * 1024 * 1024 * 1024L)
					continue;

				String id = UID.id(node, path);
				try {

					name = name.replace("[\\\\]", "/");
					V v = V.create();
					v.append("name", jo.name).append("path", jo.path);
					v.append("total", jo.total).append("free", jo.free).append("used", jo.used);
					v.append("node", node).force("name", name).remove("_id", X.ID);

					// insert
					if (dao.exists2(id)) {
						dao.update(id, v.copy());
					} else {
						dao.insert(v.copy().force(X.ID, id));
					}

					if (!Record.dao.exists(W.create().and("node", node).and("path", path).and("created",
							System.currentTimeMillis() - X.AMINUTE, W.OP.gt))) {
						// save to record per hour
						Record.dao.insert(
								v.copy().force(X.ID, UID.id(id, System.currentTimeMillis())).append("node", node));

					}
				} catch (Exception e) {
					log.error(jo, e);
				}
			}
		}
	}

	@Table(name = "gi_m_disk_record", memo = "GI-磁盘监测历史")
	public static class Record extends _Disk {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static BeanDAO<String, Record> dao = BeanDAO.create(Record.class);

		public void cleanup() {
			dao.delete(W.create().and("created", System.currentTimeMillis() - X.AWEEK, W.OP.lt));
		}

	}

	public static void check() {

		try {

			// disk
			List<Host._DS> l1 = Host.getDisks();
			if (l1 != null && !l1.isEmpty()) {

				_Disk.update(Local.id(), l1);

				// TODO, 性能考虑
//				_DiskIO.update(Local.id(), l1);

				// log.debug("disk=" + l2);
			}
			// log.debug("disk=" + l1);
		} catch (Throwable e) {
			// TODO, ignore
//			log.error(e.getMessage(), e);
		}
	}

}
