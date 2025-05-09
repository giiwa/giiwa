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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
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
 * 主机内存使用
 * 
 * @author joe
 *
 */
@Table(name = "gi_m_mem", memo = "GI-内存监测")
public class _Mem extends Bean {

	/**
	 * x
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Mem.class);

	public static BeanDAO<String, _Mem> dao = BeanDAO.create(_Mem.class);

	@Column(memo = "主键", unique = true, size = 50)
	String id;

	@Column(memo = "节点", size = 50)
	String node;

	@Column(memo = "总空间")
	long total;

	@Column(memo = "已使用")
	long used;

	@Column(name = "_usage", memo = "使用率")
	public int usage;

	@Column(memo = "空余")
	long free;

	@Column(memo = "总交换空间")
	long swaptotal;

	@Column(memo = "交换空间空余")
	long swapfree;

	public long getUsed() {
		return used;
	}

	public long getFree() {
		return free;
	}

	public synchronized static void update(String node, long total, long free) {
		// insert or update
		try {

			V v = V.create();

			v.append("total", total);
			v.append("used", total - free);
			v.append("free", free);
			v.append("_usage", (int) ((total - free) * 100 / total));

			String id = UID.id(node);
			if (dao.exists2(id)) {
				dao.update(id, v.copy().force("node", node));
			} else {
				// insert
				dao.insert(v.copy().force(X.ID, id).force("node", node));
			}

			String id1 = UID.id(node, Global.now() / X.AMINUTE);
			if (!Record.dao.exists(id1)) {
				Record.dao.insert(v.force(X.ID, id1).force("node", node));
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	@Table(name = "gi_m_mem_record", memo = "GI-内存监测历史")
	public static class Record extends _Mem {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static BeanDAO<String, Record> dao = BeanDAO.create(Record.class);

		public void cleanup() {
			dao.delete(W.create().and("created", Global.now() - X.AWEEK, W.OP.lt));
		}

	}

	@SuppressWarnings("deprecation")
	public static void check() {
		try {
			// mem
			OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

			// 获取物理内存总量（以字节为单位）
			long total = ((com.sun.management.OperatingSystemMXBean) osBean).getTotalPhysicalMemorySize();

			// 获取空闲物理内存（以字节为单位）
			long free = ((com.sun.management.OperatingSystemMXBean) osBean).getFreePhysicalMemorySize();

			_Mem.update(Local.id(), total, free);

		} catch (Throwable e) {
			// ignore
//			log.error(e.getMessage(), e);
		}
	}

}
