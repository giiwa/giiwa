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
package org.giiwa.framework.bean;

import org.giiwa.core.base.Host;
import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.conf.Local;
import org.giiwa.framework.web.Model;
import org.hyperic.sigar.CpuInfo;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.X;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_node")
public class Node extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final BeanDAO<Node> dao = new BeanDAO<Node>();

	@Column(name = X.ID, index = true)
	private String id;

	@Column(name = "ip")
	private String ip;

	@Column(name = "uptime")
	private long uptime;

	public static void touch(boolean force) {
		try {
			if (dao.exists(Local.id())) {
				// update
				if (force) {
					dao.update(Local.id(), getNodeInfo());
				} else {
					dao.update(Local.id(), V.create());
				}
			} else {
				// create
				dao.insert(getNodeInfo().append(X.ID, Local.id()));
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private static V getNodeInfo() {
		V v = V.create("uptime", Model.UPTIME).append("ip", Host.getLocalip());

		try {
			CpuInfo[] cc = Host.getCpuInfo();
			if (cc != null && cc.length > 0) {
				v.append("cores", cc[0].getTotalCores());
			}
			v.append("os", Host.getOS().getName());
			v.append("mem", Host.getMem().getTotal());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return v;
	}

}
