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
package org.giiwa.bean;

import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.conf.Local;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;

/**
 * The web access log bean. <br>
 * table="gi_accesslog"
 * 
 * @author joe
 * 
 */
@Table(name = "gi_accesslog", memo = "GI-访问日志")
public final class AccessLog extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static Log log = LogFactory.getLog(AccessLog.class);

	public static final BeanDAO<String, AccessLog> dao = BeanDAO.create(AccessLog.class);

	static AtomicLong seq = new AtomicLong(0);

	@Column(memo = "主键", size = 50)
	private String id;

	@Column(memo = "链接", size = 255)
	private String url;

	@Column(name = "访问IP", size = 100)
	private String ip;

	@Column(name = "节点", size = 100)
	private String node;

	@Column(name = X.CREATED)
	private long created;

	/**
	 * Creates the AccessLog.
	 * 
	 * @param ip  the ip address
	 * @param url the url
	 */
	public static void create(final String url, final String ip) {
		if (Global.getInt("accesslog.on", 0) == 1) {
			long created = Global.now();
			String node = Local.id();
			String id = UID.id(url, ip, created, node);
			V v = V.create();
			v.append("id", id);
			dao.insert(v.set(X.ID, id).set("ip", ip).append("node", node).append("url", url));
		}
	}

}
