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

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.X;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_code")
public class Code extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final BeanDAO<Code> dao = BeanDAO.create(Code.class);

	@Column(name = "s1", index = true)
	private String s1;

	@Column(name = "s2", index = true)
	private String s2;

	@Column(name = "expired")
	private long expired;

	@Column(name = X.CREATED)
	private long created;

	public long getExpired() {
		return expired;
	}

	public static int create(String s1, String s2, V v) {
		W q = W.create("s1", s1).and("s2", s2);
		try {
			if (dao.exists(q)) {
				dao.delete(q);
			}
			return dao.insert(v.set("s1", s1).set("s2", s2));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	public static Code load(String s1, String s2) {
		return dao.load(W.create("s1", s1).and("s2", s2));
	}

	public static void delete(String s1, String s2) {
		dao.delete(W.create("s1", s1).and("s2", s2));
	}

}
