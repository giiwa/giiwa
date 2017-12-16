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

import java.util.List;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.BeanDAO;
import org.giiwa.core.bean.Column;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_porlet")
public class Porlet extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final BeanDAO<Porlet> dao = BeanDAO.create(Porlet.class);

	@Column(name = X.ID)
	private long id;

	@Column(name = "uid")
	private long uid;

	@Column(name = "seq")
	private int seq;

	@Column(name = "tag")
	private String tag;

	@Column(name = "style")
	private String style;

	@Column(name = "w")
	private int w;

	@Column(name = "h")
	private int h;

	@Column(name = "uri")
	private String uri;

	public static List<Porlet> load(long uid, String tag) {
		return dao.load(W.create("uid", uid).and("tag", tag), 0, 100);
	}

	public static int create(long uid, String tag, V v) {

		try {
			long id = UID.next("porlet.id");

			if (dao.exists(id)) {
				id = UID.next("porlet.id");
			}
			return dao.insert(v.force(X.ID, id).append("tag", tag).append("uid", uid));
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	public static void create(long uid, String tag, String uri) {
		try {
			if (!dao.exists(W.create("uid", uid).and("uri", uri))) {
				create(uid, tag, V.create("uri", uri));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
