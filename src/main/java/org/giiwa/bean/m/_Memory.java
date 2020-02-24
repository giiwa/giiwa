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

@Table(name = "gi_m_mem")
public class _Memory extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Memory.class);

	public static BeanDAO<String, _Memory> dao = BeanDAO.create(_Memory.class);

	@Column(name = X.ID)
	String id;

	@Column(name = "node")
	String node;

	@Column(name = "total")
	long total;

	@Column(name = "used")
	long used;

	@Column(name = "usage")
	int usage;

	@Column(name = "free")
	long free;

	@Column(name = "swaptotal")
	long swaptotal;

	@Column(name = "swapfree")
	long swapfree;

	public long getUsed() {
		return used;
	}

	public long getFree() {
		return free;
	}

	public synchronized static void update(String node, JSON jo) {
		// insert or update
		try {
			V v = V.fromJSON(jo).remove("_id", X.ID);

			String id = UID.id(node);
			if (dao.exists(id)) {
				dao.update(id, v.copy().force("node", node));
			} else {
				// insert
				dao.insert(v.copy().force(X.ID, id).force("node", node));
			}

			Record.dao.insert(v.force(X.ID, UID.id(node, System.currentTimeMillis())).force("node", node));

		} catch (Exception e) {
			log.error(jo, e);
		}
	}

	@Table(name = "gi_m_mem_record")
	public static class Record extends _Memory {

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
