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
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_userconfig")
public class UserConfig extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(UserConfig.class);

	public static final BeanDAO<String, UserConfig> dao = BeanDAO.create(UserConfig.class);

	@Column(name = X.ID, index = true)
	private String id;

	@Column(name = "uid", index = true)
	private long uid;

	@Column(name = "name", index = true)
	private String name;

	@Column(name = "data")
	private String data;

	public static void set(long uid, String name, String value) {

		String id = UID.id(uid, name);
		try {
			V v = V.create("data", value);
			if (dao.exists(id)) {
				// update
				dao.update(id, v);
			} else {
				dao.insert(v.force(X.ID, id).append("uid", uid).append("name", name));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static String get(long uid, String name) {
		UserConfig c = dao.load(W.create("uid", uid).and("name", name));
		if (c != null) {
			return c.data;
		}
		return null;
	}

	public static void delete(long uid, String name) {
		dao.delete(W.create("uid", uid).and("name", name));
	}

}
