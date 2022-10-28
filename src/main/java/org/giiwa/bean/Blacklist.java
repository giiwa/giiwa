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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Table;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * The App bean, used to store appid and secret table="gi_app"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_blacklist", memo = "GI-黑名单")
public final class Blacklist extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Blacklist.class);

	public static final BeanDAO<Long, Blacklist> dao = BeanDAO.create(Blacklist.class);

	@Column(memo = "唯一序号")
	public long id;

	@Column(memo = "IP地址")
	public String ip;

	@Column(memo = "访问链接")
	public String url;

	@Column(memo = "备注")
	public String memo;

	@Column(memo = "拦截次数")
	public long times;

	public static boolean isBlocked(String ip, String url) {
		if (Helper.isConfigured()) {
			Beans<Blacklist> l1 = dao.load(W.create().and("ip", ip), 0, 10);
			if (l1 != null && !l1.isEmpty()) {
				for (Blacklist e : l1) {
					if (X.isEmpty(e.url) || url.matches(e.url)) {
						dao.inc(W.create().and(X.ID, ip), "times", 1, null);
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Creates the.
	 *
	 * @param v the v
	 * @return the int
	 */
	public static long create(V v) {
		try {
			long id = dao.next();
			while (dao.exists(id)) {
				id = dao.next();
			}
			dao.insert(v.set(X.ID, id));
			return id;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

}
