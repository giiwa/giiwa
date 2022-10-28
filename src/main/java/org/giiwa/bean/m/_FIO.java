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
import org.giiwa.misc.Shell;

@Table(name = "gi_m_fio", memo = "GI-文件句柄")
public class _FIO extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_FIO.class);

	public static BeanDAO<String, _FIO> dao = BeanDAO.create(_FIO.class);

	@Column(memo = "唯一序号")
	String id;

	@Column(memo = "节点")
	String node;

	@Column(memo = "总内存")
	public long total;

	@Column(memo = "已使用")
	public long used;

	public synchronized static void update(String node, long used) {
		// insert or update
		try {
			V v = V.create();
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

	@Table(name = "gi_m_fio_record", memo = "GI-文件句柄历史")
	public static class Record extends _FIO {

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
		long used = Shell.getFIO();
		_FIO.update(Local.id(), used);
	}

}
