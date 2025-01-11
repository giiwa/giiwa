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

import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * 短链接
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_s", memo = "GI-短链接")
public final class S extends Bean {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	public final static BeanDAO<String, S> dao = BeanDAO.create(S.class, time -> {
		return W.create().and("updated", System.currentTimeMillis() - X.AWEEK, W.OP.lt);
	});

	@Column(memo = "主键", unique = true, size = 50)
	public String id;

	@Column(memo = "连接", size = 1023)
	public String url;

	public static String create(String url) {
		String id = UID.id(url);
		if (dao.exists2(id)) {
			dao.update(id, V.create().append("updated", System.currentTimeMillis()));
		} else {
			dao.insert(V.create().append("id", id).append("url", url).append("expired", System.currentTimeMillis()));
		}
		return "/s/" + id;
	}

}
