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

import java.sql.SQLException;

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
@Table(name = "gi_userconfig", memo = "GI-用户设置")
public final class UserConfig extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

//	private static Log log = LogFactory.getLog(UserConfig.class);

	public static final BeanDAO<String, UserConfig> dao = BeanDAO.create(UserConfig.class);

	@Column(memo = "主键", unique = true, size = 50)
	private String id;

	@Column(memo = "用户ID")
	private long uid;

	@Column(memo = "会话ID", size = 50)
	private String sid;

	@Column(memo = "数据名称", size=50)
	private String name;

	@Column(memo = "数据", size = 2048)
	private String data;

	public static void set(long uid, String sid, String name, String value) throws SQLException {

		String id = UID.id(uid, sid, name);
		V v = V.create("data", value);
		if (dao.exists(id)) {
			// update
			dao.update(id, v);
		} else {
			dao.insert(v.force(X.ID, id).append("uid", uid).append("sid", sid).append("name", name));
		}
	}

	public static String get(long uid, String sid, String name) {
		UserConfig c = dao.load(W.create().and("uid", uid).and("sid", sid).and("name", name));
		if (c != null) {
			return c.data;
		}
		return null;
	}

	public static void delete(long uid, String name) {
		dao.delete(W.create().and("uid", uid).and("name", name));
	}

}
