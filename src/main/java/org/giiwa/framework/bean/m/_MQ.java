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

@Table(name = "gi_m_mq")
public class _MQ extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_MQ.class);

	public static BeanDAO<String, _MQ> dao = BeanDAO.create(_MQ.class);

	@Column(name = X.ID)
	String id;

	@Column(name = "node")
	String node;

	@Column(name = "name")
	String name;

	@Column(name = "max")
	long max;

	@Column(name = "min")
	long min;

	@Column(name = "avg")
	long avg;

	@Column(name = "times")
	long times;

	public synchronized static void update(String node, JSON jo) {
		// insert or update
		try {
			String name = jo.getString("name");

			V v = V.fromJSON(jo);
			v.append("node", node);

			String id = UID.id(node, name);
			if (dao.exists(id)) {
				dao.update(id, v.copy());
			} else {
				dao.insert(v.copy().force(X.ID, id));
			}

			Record.dao.insert(v.force(X.ID, UID.id(node, name, System.currentTimeMillis())));

		} catch (Exception e) {
			log.error(jo, e);
		}
	}

	@Table(name = "gi_m_mq_record")
	public static class Record extends _MQ {

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
