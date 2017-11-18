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
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.conf.Local;
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;

/**
 * Internal used bean, used for module management. <br>
 * table="gi_jar"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_jar")
public class Jar extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static final BeanDAO<Jar> dao = new BeanDAO<Jar>();

	/**
	 * Update.
	 *
	 * @param module
	 *            the module
	 * @param name
	 *            the name
	 */
	public static void update(String module, String name) {
		String node = Local.id();
		String id = UID.id(module, name, node);

		try {
			if (!dao.exists(W.create(X.ID, id))) {
				V v = V.create(X.ID, id).set("module", module).set("name", name).set("node", node).set("reset", 1);
				dao.insert(v);
			} else {
				dao.update(id, V.create("reset", 1));
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}

	}

	/**
	 * Removes the.
	 *
	 * @param module
	 *            the module
	 * @param name
	 *            the name
	 */
	public static void remove(String module, String name) {
		String node = Local.id();

		dao.delete(W.create("module", module).and("name", name).and("node", node));
	}

	/**
	 * Removes the.
	 *
	 * @param name
	 *            the name
	 */
	public static void remove(String name) {
		dao.delete(W.create("name", name));
	}

	/**
	 * Load all.
	 *
	 * @param q
	 *            the q
	 * @return the list
	 */
	public static List<String> loadAll(W q) {
		String node = Local.id();
		return dao.distinct("name", q.and("node", node), String.class);
	}

	/**
	 * Load.
	 *
	 * @param name
	 *            the name
	 * @return the list
	 */
	public static List<String> load(String name) {
		String node = Local.id();

		return dao.distinct("module", W.create("name", name).and("node", node), String.class);
	}

	/**
	 * remove all jars.
	 *
	 * @param module
	 *            the module
	 */
	public static void reset(String module) {
		String node = Local.id();
		dao.delete(W.create("module", module).and("node", node));
	}

}
