package org.giiwa.bean.m;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

@Table(name = "gi_m_diskio", memo = "GI-磁盘IO监测")
public class _DiskIO extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_DiskIO.class);

	public static BeanDAO<String, _DiskIO> dao = BeanDAO.create(_DiskIO.class);

	@Column(memo = "唯一序号")
	String id;

	@Column(memo = "节点")
	String node;

	@Column(memo = "路径")
	public String path;

	@Column(memo = "读字节总数")
	public long readbytes;

	@Column(memo = "读速度")
	public long reads;

	@Column(memo = "写字节总数")
	public long writebytes;

	@Column(memo = "写速度")
	public long writes;

	@Column(memo = "队列")
	public double queue;

	public synchronized static void update(String node, List<JSON> l1) {

//		Map<String, JSON> l2 = new HashMap<String, JSON>();

		for (JSON jo : l1) {
			// insert or update
			String path = jo.getString("path");
			String name = jo.getString("name");
			if (X.isIn(name, "tmpfs", "devtmpfs")) {
				continue;
			}

			if (X.isEmpty(path) || X.isEmpty(name)) {
				log.error(jo, new Exception("name or path missed"));
				break;
			}

			String id = UID.id(node, path);
			try {

				name = name.replace("[\\\\]", "/");
				V v = V.fromJSON(jo).append("node", node).force("name", name).remove("_id", X.ID);

				Record r1 = Record.dao.load(W.create().and("node", node).and("path", path).sort("created", -1));
				if (r1 != null && jo.getLong("readbytes") > r1.readbytes) {
					long time = (System.currentTimeMillis() - r1.getCreated()) / 1000;
					v.force("reads", (jo.getLong("readbytes") - r1.readbytes) / time);
					v.force("writes", (jo.getLong("writebytes") - r1.writebytes) / time);
				}

				// insert
				if (dao.exists(id)) {
					dao.update(id, v.copy());
				} else {
					dao.insert(v.copy().force(X.ID, id));
				}

				if (!Record.dao.exists(W.create("node", node).and("path", path).and("created",
						System.currentTimeMillis() - X.AMINUTE, W.OP.gt))) {
					// save to record per hour
					Record.dao
							.insert(v.copy().force(X.ID, UID.id(id, System.currentTimeMillis())).append("node", node));
				}
			} catch (Exception e) {
				log.error(jo, e);
			}
		}
	}

	@Table(name = "gi_m_diskio_record", memo = "GI-磁盘IO监测历史")
	public static class Record extends _DiskIO {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public static BeanDAO<String, Record> dao = BeanDAO.create(Record.class);

		public void cleanup() {
			dao.delete(W.create().and("created", System.currentTimeMillis() - X.AMONTH, W.OP.lt));
		}

	}

}
