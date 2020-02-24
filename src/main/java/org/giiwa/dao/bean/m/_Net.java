package org.giiwa.dao.bean.m;

import java.util.List;

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

@Table(name = "gi_m_net")
public class _Net extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Net.class);

	public static BeanDAO<String, _Net> dao = BeanDAO.create(_Net.class);

	@Column(name = X.ID)
	String id;

	@Column(name = "node")
	String node;

	@Column(name = "name")
	String name;

	@Column(name = "inet")
	String inet;

	@Column(name = "inet6")
	String inet6;

	@Column(name = "txbytes")
	long txbytes;

	@Column(name = "txpackets")
	long txpackets;

	@Column(name = "txdrop")
	long txdrop;

	@Column(name = "txerr")
	long txerr;

	@Column(name = "rxbytes")
	long rxbytes;

	@Column(name = "rxpackets")
	long rxpackets;

	@Column(name = "rxdrop")
	long rxdrop;

	@Column(name = "rxerr")
	long rxerr;

	public long getTxbytes() {
		return txbytes;
	}

	public long getRxbytes() {
		return rxbytes;
	}

	public synchronized static void update(String node, List<JSON> l1) {

		for (JSON jo : l1) {
			// insert or update
			String name = jo.getString("name");
			if (X.isEmpty(name)) {
				log.error(jo, new Exception("name missed"));
				continue;
			}
			String inet = jo.getString("inet");
			if (X.isEmpty(inet) || inet.startsWith("127")) {
				continue;
			}

			try {

				String type = jo.getString("_type");

				V v = V.fromJSON(jo);
				if (X.isSame("snapshot", type)) {
					v = V.create("name", name).append("_type", "snapshot").append("snapshot", jo.toString());
					v.append("inet", inet).append("inet6", jo.getString("inet6"));

					Record r = Record.dao.load(
							W.create("node", node).and("name", name).and("_type", "snapshot").sort("created", -1));
					if (r != null) {
						JSON p = JSON.fromObject(r.get("snapshot"));
						if (p != null) {
							long time = System.currentTimeMillis() - r.getLong("created");

							v.append("rxbytes", X.toLong((jo.getLong("rxbytes") - p.getLong("rxbytes")) * 1000 / time));
							v.append("rxpackets",
									X.toLong((jo.getLong("rxpackets") - p.getLong("rxpackets")) * 1000 / time));
							v.append("rxerr", X.toLong((jo.getLong("rxerr") - p.getLong("rxerr")) * 1000 / time));
							v.append("rxdrop", X.toLong((jo.getLong("rxdrop") - p.getLong("rxdrop")) * 1000 / time));
							v.append("txbytes", X.toLong((jo.getLong("txbytes") - p.getLong("txbytes")) * 1000 / time));
							v.append("txpackets",
									X.toLong((jo.getLong("txpackets") - p.getLong("txpackets")) * 1000 / time));
							v.append("txerr", X.toLong((jo.getLong("txerr") - p.getLong("txerr")) * 1000 / time));
							v.append("txdrop", X.toLong((jo.getLong("txdrop") - p.getLong("txdrop")) * 1000 / time));
						}
					}

				}
				v.remove("_id", X.ID);

				String id = UID.id(node, name);

				if (dao.exists(id)) {
					dao.update(id, v.copy().force("node", node));
				} else {
					// insert
					dao.insert(v.copy().force(X.ID, id).force("node", node));
				}

				Record.dao.insert(v.force(X.ID, UID.id(id, System.currentTimeMillis())).force("node", node));

			} catch (Exception e) {
				log.error(jo, e);
			}
		}
	}

	@Table(name = "gi_m_net_record")
	public static class Record extends _Net {

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
