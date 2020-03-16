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

@Table(name = "gi_m_mem", memo = "GI-内存监测")
public class _Memory extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Memory.class);

	public static BeanDAO<String, _Memory> dao = BeanDAO.create(_Memory.class);

	@Column(memo = "唯一序号")
	String id;

	@Column(memo = "节点")
	String node;

	@Column(memo = "总空间")
	long total;

	@Column(memo = "已使用")
	long used;

	@Column(memo = "使用率")
	int usage;

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

	@Table(name = "gi_m_mem_record", memo = "GI-内存监测历史")
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
