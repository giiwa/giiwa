/*
 * Copyright 2015 JIHU, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/
package org.giiwa.bean.m;

import java.util.List;

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
import org.giiwa.json.JSON;
import org.giiwa.misc.Host;

@Table(name = "gi_m_net", memo = "GI-网络监测")
public class _Net extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(_Net.class);

	public static BeanDAO<String, _Net> dao = BeanDAO.create(_Net.class);

	@Column(memo = "主键", unique = true, size = 50)
	String id;

	@Column(memo = "节点", size = 50)
	String node;

	@Column(memo = "名称", size = 50)
	String name;

	@Column(memo = "IPv4", size = 50)
	String inet;

	@Column(memo = "IPv6", size = 50)
	String inet6;

	@Column(name = "_type", size = 50)
	String type;

	@Column(memo = "发送字节")
	long txbytes;

	@Column(memo = "发送包")
	long txpackets;

	@Column(memo = "发送丢失")
	long txdrop;

	@Column(memo = "发送错误")
	long txerr;

	@Column(memo = "接收字节")
	long rxbytes;

	@Column(memo = "接收包")
	long rxpackets;

	@Column(memo = "接收丢失")
	long rxdrop;

	@Column(memo = "接收错误")
	long rxerr;

	public long getTxbytes() {
		return txbytes;
	}

	public long getRxbytes() {
		return rxbytes;
	}

	public synchronized static void update(String node, List<Host._NS> l1) {

		for (Host._NS jo : l1) {
			// insert or update
			String name = jo.name;
			if (X.isEmpty(name)) {
				log.error(jo, new Exception("name missed"));
				continue;
			}
			String inet = jo.inet;
			if (X.isEmpty(inet) || inet.startsWith("127")) {
				continue;
			}

			try {

				String type = "snapshot";

				V v = jo.toV();
				v.remove("_id");

				if (X.isSame("snapshot", type)) {
					v = V.create("name", name).append("_type", "snapshot").append("snapshot", jo.toString());
					v.append("inet", inet).append("inet6", jo.inet6);

					Record r = Record.dao.load(W.create().and("node", node).and("name", name).and("_type", "snapshot")
							.sort("created", -1));
					if (r != null) {
						JSON p = JSON.fromObject(r.get("snapshot"));
						if (p != null) {
							long time = System.currentTimeMillis() - r.getLong("created");
							if (time <= 0) {
								// skip
								continue;
							}

							v.append("rxbytes", X.toLong((jo.rxbytes - p.getLong("rxbytes")) * 1000 / time));
							v.append("rxpackets", X.toLong((jo.rxpackets - p.getLong("rxpackets")) * 1000 / time));
							v.append("rxerr", X.toLong((jo.rxerr - p.getLong("rxerr")) * 1000 / time));
							v.append("rxdrop", X.toLong((jo.rxdrop - p.getLong("rxdrop")) * 1000 / time));
							v.append("txbytes", X.toLong((jo.txbytes - p.getLong("txbytes")) * 1000 / time));
							v.append("txpackets", X.toLong((jo.txpackets - p.getLong("txpackets")) * 1000 / time));
							v.append("txerr", X.toLong((jo.txerr - p.getLong("txerr")) * 1000 / time));
							v.append("txdrop", X.toLong((jo.txdrop - p.getLong("txdrop")) * 1000 / time));
						}
					}

				}
				v.remove("_id", X.ID);

				String id = UID.id(node, name);

				if (dao.exists2(id)) {
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

	@Table(name = "gi_m_net_record", memo = "GI-网络监测历史")
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

	public static void check() {
		try {
			// net
			List<Host._NS> l1 = Host.getIfstats();
			if (l1 != null && !l1.isEmpty()) {
//				List<JSON> l2 = new ArrayList<JSON>();
//				for (JSON j1 : n1) {
//					l2.add(JSON.create().append("name", j1.get("name")).append("inet", j1.get("address")).copy(j1,
//							"rxbytes", "rxdrop", "rxerr", "rxpackets", "txbytes", "txdrop", "txerr", "txpackets")
//							.append("_type", "snapshot"));
//				}
				_Net.update(Local.id(), l1);
			}
		} catch (Throwable e) {
			// TODO ignore
//			log.error(e.getMessage(), e);
		}
	}

}
