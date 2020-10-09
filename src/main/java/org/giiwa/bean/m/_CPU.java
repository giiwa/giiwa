package org.giiwa.bean.m;

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
import org.giiwa.json.JSON;

@Table(name = "gi_m_cpu", memo = "GI-CPU监测")
public class _CPU extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_CPU.class);

	public static BeanDAO<String, _CPU> dao = BeanDAO.create(_CPU.class);

	@Column(memo = "唯一序号")
	String id;

	@Column(memo = "节点")
	String node;

	@Column(memo = "名称")
	String name;

	@Column(memo = "系统")
	double sys;

	@Column(name = "user1", memo = "用户")
	double user;

	@Column(memo = "使用率")
	double usage;

	@Column(memo = "等待")
	double wait;

	@Column(memo = "NICE")
	double nice;

	@Column(memo = "空闲")
	double idle;

	@Column(memo = "温度")
	public String temp;

	public int getUsage() {
		return X.toInt(usage);
	}

	public synchronized static void update(String node, JSON jo) {
		// insert or update
		String name = jo.getString("name");
		if (X.isEmpty(name)) {
			log.error(jo, new Exception("name missed"));
			return;
		}

		try {

			V v = V.fromJSON(jo);
			v.append("user1", v.value("user"));
			v.remove("_id", X.ID, "user");

			String id = UID.id(node, name);
			if (dao.exists(id)) {
				dao.update(id, v.copy().force("node", node));
			} else {
				// insert
				dao.insert(v.copy().force(X.ID, id).force("node", node));
			}

			Record.dao.insert(v.copy().force(X.ID, UID.id(node, name, System.currentTimeMillis())).force("node", node));

		} catch (Exception e) {
			log.error(jo, e);
		}
	}

	/**
	 * @deprecated
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
			dao.delete(W.create().and("created", System.currentTimeMillis() - X.AWEEK, W.OP.lt));
		}

	}

}
