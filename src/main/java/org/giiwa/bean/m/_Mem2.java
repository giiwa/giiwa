package org.giiwa.bean.m;

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
 * JVM内存使用
 * 
 * @author joe
 *
 */
@Table(name = "gi_m_mem2", memo = "GI-内存使用")
public class _Mem2 extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Mem2.class);

	public static BeanDAO<String, _Mem2> dao = BeanDAO.create(_Mem2.class);

	@Column(memo = "唯一序号")
	String id;

	@Column(memo = "节点")
	String node;

	@Column(memo = "总内存")
	public long total;

	@Column(memo = "已使用")
	public long used;

	public synchronized static void update(String node, long total, long used) {
		// insert or update
		try {
			V v = V.create();

			v.append("total", total);
			v.append("used", used);

			String id = UID.id(node);
			if (dao.exists2(id)) {
				dao.update(id, v.copy().force("node", node));
			} else {
				// insert
				dao.insert(v.copy().force(X.ID, id).force("node", node));
			}

			Record.dao.insert(v.force(X.ID, UID.id(node, System.currentTimeMillis())).force("node", node));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

	@Table(name = "gi_m_mem2_record", memo = "GI-内存使用历史")
	public static class Record extends _Mem2 {

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
		Runtime t = Runtime.getRuntime();
		_Mem2.update(Local.id(), t.totalMemory(), t.totalMemory() - t.freeMemory());
	}

}
