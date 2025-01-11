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
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;

/**
 * access token class, it's Bean and mapping to "gi_usergroup" table, it mapping
 * the "access" method in @Path interface. <br>
 * table="gi_usergroup"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_unit", memo = "GI-用户单位")
public final class Unit extends Bean {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Unit.class);

	public final static BeanDAO<Long, Unit> dao = BeanDAO.create(Unit.class);

	@Column(memo = "主键", unique = true)
	public long id;

	@Column(memo = "编码", size=100)
	public String no;

	@Column(memo = "名称", size=100)
	public String name;

	@Column(memo = "备注", size = 100)
	private String memo;

	@Column(memo = "父节点")
	private long parent;

	public static long create(V v) {
		try {
			long id = dao.next();
			while (dao.exists(id)) {
				id = dao.next();
			}
			v.force(X.ID, id);
			dao.insert(v);
			return id;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	transient Unit parent_obj;

	public Unit getParent_obj() {
		if (parent_obj == null && parent > 0) {
			parent_obj = dao.load(parent);
		}
		return parent_obj;
	}

}
