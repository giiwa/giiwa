package org.giiwa.framework.bean.m;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;

@Table(name = "gi_m_cpu")
public class _CPU extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_CPU.class);

	public static BeanDAO<String, _CPU> dao = BeanDAO.create(_CPU.class);

	@Column(name = X.ID)
	String id;

	@Column(name = "node")
	String node;

	@Column(name = "name")
	String name;

	@Column(name = "sys")
	double sys;

	@Column(name = "user1")
	double user;

	@Column(name = "usage")
	double usage;

	@Column(name = "wait")
	double wait;

	@Column(name = "nice")
	double nice;

	@Column(name = "idle")
	double idle;

	public double getUsage() {
		return usage;
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

	@Table(name = "gi_m_cpu_record")
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
