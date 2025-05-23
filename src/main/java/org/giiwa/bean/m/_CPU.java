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

@Table(name = "gi_m_cpu", memo = "GI-CPU监测")
public class _CPU extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_CPU.class);

	public static BeanDAO<String, _CPU> dao = BeanDAO.create(_CPU.class);

	@Column(memo = "主键", unique = true, size = 50)
	String id;

	@Column(memo = "节点", size = 50)
	String node;

	@Column(memo = "名称", size = 50)
	String name;

	@Column(memo = "核心数")
	long cores;

	@Column(memo = "系统")
	double sys;

	@Column(name = "_user", memo = "用户")
	double user;

	@Column(name = "_usage", memo = "使用率")
	double usage;

	@Column(memo = "等待")
	double wait;

	@Column(memo = "NICE")
	double nice;

	@Column(memo = "空闲")
	double idle;

	@Column(memo = "温度", size = 50)
	public String temp;

	public int getUsage() {
		return X.toInt(usage);
	}

	public synchronized static void update(String node, _Stat jo) {

		// insert or update
		String name = jo.name;
		if (X.isEmpty(name)) {
			log.error(jo, new Exception("name missed"));
			return;
		}

		try {

			V v = jo.toV();
			v.append("_user", v.value("user"));
			v.append("_usage", v.value("usage"));
			v.remove("_id", X.ID, "user", "usage");

			String id = UID.id(node, name);
			if (dao.exists2(id)) {
				dao.update(id, v.copy().force("node", node));
			} else {
				// insert
				dao.insert(v.copy().force(X.ID, id).force("node", node));
			}

			String id1 = UID.id(node, name, Global.now() / X.AMINUTE);
			if (!Record.dao.exists(id1)) {
				Record.dao.insert(v.copy().force(X.ID, id1).force("node", node));
			}

		} catch (Exception e) {
			log.error(jo, e);
		}
	}

	/**
	 * @Deprecated
	 * @param c1 the CPU
	 */
	public void plus(_CPU c1) {
		user += c1.sys;
		user += c1.user;
		wait += c1.wait;
		nice += c1.nice;
		idle += c1.idle;
	}

	@Table(name = "gi_m_cpu_record", memo = "GI-CPU监测历史")
	public static class Record extends _CPU {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		public static BeanDAO<String, Record> dao = BeanDAO.create(Record.class);

		public void cleanup() {
			dao.delete(W.create().and("created", Global.now() - X.AWEEK, W.OP.lt));
		}

	}

	public static synchronized float usage() {

		OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		
		if (log.isDebugEnabled()) {
			log.debug("monitor cpu, got whole");
		}

		return X.toFloat(os.getSystemLoadAverage());

	}

	public static void check() {

		OperatingSystemMXBean os = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

		int cores = os.getAvailableProcessors();

		_Stat e = new _Stat();

		e.user = 0;
		e.sys = 0;
		e.usage = usage();
//		e.temp = Host.getCpuTemp();
		e.name = "cpu";
		e.cores = cores;

		_CPU.update(Local.id(), e);

	}

	public static class _Stat {

		String name;
		int cores;
		double user;
		double sys;
		double usage;
		String temp;

		public V toV() {

			V e = V.create();
			e.append("name", name);
			e.append("cores", cores);
			e.append("user", user);
			e.append("sys", sys);
			e.append("usage", usage);
			e.append("temp", temp);
			return e;

		}

	}

//	public static void check0() {
//
//		try {
//			// cpu
//			CpuPerc[] cc = Host.getCpuPerc();
//			if (cc != null && cc.length > 0) {
//
//				JSON jo = JSON.create();
//				// summary all
//				double user = 0;
//				double sys = 0;
//				for (CpuPerc c : cc) {
//					/**
//					 * user += c1.sys; <br/>
//					 * user += c1.user;<br/>
//					 * wait += c1.wait;<br/>
//					 * nice += c1.nice;<br/>
//					 * idle += c1.idle;<br/>
//					 */
//					user += c.getUser();
//					sys += c.getSys();
//				}
//				jo.append("user", user * 100 / cc.length);
//				jo.append("sys", sys * 100 / cc.length);
//				jo.append("usage", (int) (jo.getDouble("user") + jo.getDouble("sys")));
//				jo.append("temp", Host.getCpuTemp());
//				// log.debug("cpu=" + jo);
//
//				jo.append("name", "cpu").append("cores", cc.length);
//
//				_CPU.update(Local.id(), jo);
//			}
//		} catch (Exception e) {
//			log.error(e.getMessage(), e);
//		}
//	}

}
