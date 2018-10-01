package org.giiwa.framework.bean.m;

import java.util.List;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.json.JSON;

@Table(name = "gi_m_disk")
public class _Disk extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static BeanDAO<String, _Disk> dao = BeanDAO.create(_Disk.class);

	@Column(name = X.ID)
	String id;

	@Column(name = "node")
	String node;

	@Column(name = "disk")
	String disk;

	@Column(name = "path")
	String path;

	@Column(name = "total")
	long total;

	@Column(name = "used")
	long used;

	@Column(name = "free")
	long free;

	public long getUsed() {
		return used;
	}

	public long getFree() {
		return free;
	}

	public static void update(String node, List<JSON> l1) {

		dao.delete(W.create("node", node));

		for (JSON jo : l1) {
			// insert or update
			String path = jo.getString("path");
			if (X.isEmpty(path)) {
				log.error(jo, new Exception("path missed"));
				break;
			}

			String id = UID.id(node, path);
			try {

				V v = V.fromJSON(jo).append("node", node).remove("_id", X.ID);

				// insert
				dao.insert(v.copy().force(X.ID, id));

				if (!Record.dao.exists(W.create("node", node).and("path", path).and("created",
						System.currentTimeMillis() - X.AHOUR, W.OP.gt))) {
					// save to record per hour
					Record.dao
							.insert(v.copy().force(X.ID, UID.id(id, System.currentTimeMillis())).append("node", node));
				}
			} catch (Exception e) {
				log.error(jo, e);
			}
		}
	}

	@Table(name = "gi_m_disk_record")
	public static class Record extends _Disk {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static BeanDAO<String, Record> dao = BeanDAO.create(Record.class);

		public void cleanup() {
			dao.delete(W.create().and("created", System.currentTimeMillis() - X.AMONTH, W.OP.lt));
		}

	}

	@Override
	public void cleanup() {
		dao.cleanup();
		Record.dao.cleanup();
	}

}
