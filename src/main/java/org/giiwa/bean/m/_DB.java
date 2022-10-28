package org.giiwa.bean.m;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Stat;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Counter;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;

@Table(name = "gi_m_db", memo = "GI-数据库监测")
public class _DB extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_DB.class);

	public static BeanDAO<String, _DB> dao = BeanDAO.create(_DB.class);

	@Column(memo = "唯一序号")
	String id;

	@Column(memo = "节点")
	String node;

	@Column(memo = "名称")
	String name;

	@Column(memo = "最大")
	long max;

	@Column(memo = "最小")
	long min;

	@Column(memo = "平均")
	long avg;

	@Column(memo = "次数")
	long times;

	long time;

	long _reads;
	long _writes;
	long _netin;
	long _netout;

	public synchronized static void update(String node, Counter.Stat jo) {
		// insert or update
		try {
			String name = jo.name;

			V v = jo.toV();
			v.append("node", node);
			v.remove("_id");

			String id = UID.id(node, name);
			if (dao.exists2(id)) {
				dao.update(id, v.copy());
			} else {
				dao.insert(v.copy().force(X.ID, id));
			}

			Record.dao.insert(v.force(X.ID, UID.id(node, name, System.currentTimeMillis())));

		} catch (Exception e) {
			log.error(jo, e);
		}
	}

	public synchronized static void snapshot(String node, JSON jo) {
		// insert or update
		try {
			String name = jo.getString("name");

			V v = V.fromJSON(jo);
			v.append("node", node);

			String id = UID.id(node, name);
			if (dao.exists2(id)) {
				dao.update(id, v.copy());
			} else {
				dao.insert(v.copy().force(X.ID, id));
			}

			long time = Stat.tomin(System.currentTimeMillis());

			Record e = Record.dao
					.load(W.create().and("node", node).and("name", name).and("time", time, W.OP.lt).sort("time", -1));
			if (e != null) {
				// reads, writes, netio, netout
				v.append("_reads", X.toLong(v.value("reads")) - e.getLong("reads"));
				v.append("_writes", X.toLong(v.value("writes")) - e.getLong("writes"));
				v.append("_netin", X.toLong(v.value("netin")) - e.getLong("netin"));
				v.append("_netout", X.toLong(v.value("netout")) - e.getLong("netout"));
			}
			id = UID.id(node, name, time);
			v.append("time", time);
			if (Record.dao.exists(id)) {
				// update
				Record.dao.update(id, v);
			} else {
				Record.dao.insert(v.force(X.ID, id));
			}

		} catch (Exception e) {
			log.error(jo, e);
		}
	}

	@Table(name = "gi_m_db_record", memo = "GI-数据库监测历史")
	public static class Record extends _DB {

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

		Counter.Stat r = Helper.statRead();
		r.name = "read";
		Counter.Stat w = Helper.statWrite();
		w.name = "write";

		_DB.update(Local.id(), r);
		_DB.update(Local.id(), w);

		JSON j1 = Helper.status();
		if (j1 != null) {
			_DB.snapshot(Global.id(), j1.append("name", "status"));
		}

	}

}
