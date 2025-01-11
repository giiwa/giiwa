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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * access token class, it's Bean and mapping to "gi_access" table, it mapping
 * the "access" method in @Path interface. <br>
 * table="gi_access"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_access", memo = "GI-权限令牌")
public final class Access extends Bean {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Access.class);

	public final static BeanDAO<String, Access> dao = BeanDAO.create(Access.class);

	@Column(name = X.ID, memo = "名称", unique = true, size=50)
	private String name;

	@Column(memo = "备注", size = 512)
	private String memo;

	/**
	 * get the group name of the access name
	 * 
	 * @return the string
	 */
	public String groupName() {
		int i = name.indexOf(".");
		if (i > 0) {
			int j = name.indexOf(".", i + 1);
			if (j > 0) {
				return name.substring(0, j);
			} else {
				return name.substring(0, i);
			}
		}
		return "access";
	}

	/**
	 * get the access name
	 * 
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Add a access name, the access name MUST fit with "access.[group].[name]" .
	 * 
	 * @param name the name
	 */
	public static void set(String name) {
		if (Helper.isConfigured()) {
			if (X.isEmpty(name) || !name.startsWith("access.")) {
				log.error("error access.name: " + name, new Exception("error access name:" + name));
			} else {
				String[] ss = X.split(name, "[\\|｜]");
				if (ss != null) {
					for (String s : ss) {
						if (!exists(s)) {
							dao.insert(V.create(X.ID, s).append("memo", Thread.currentThread().getName()));
						}
					}
				}
			}
		}
	}

	static private Set<String> cache = new HashSet<String>();

	/**
	 * check exists of the name
	 *
	 * @param name the name
	 * @return true, if successful
	 */
	public static boolean exists(String name) {
		if (cache.contains(name)) {
			return true;
		}

		try {
			if (dao.exists(name)) {
				cache.add(name);
				return true;
			}
		} catch (Exception e1) {
			log.error(e1.getMessage(), e1);
		}
		return false;
	}

	/**
	 * Load all access and group by [group] name.
	 *
	 * @return the map
	 */
	public static Map<String, List<Access>> load() {
		Beans<Access> bs = dao.load(W.create(), 0, Integer.MAX_VALUE);
		List<Access> list = bs;
		Map<String, List<Access>> r = new TreeMap<String, List<Access>>();
		String group = null;
		List<Access> last = null;
		for (Access a : list) {
			if (!X.isEmpty(a.name)) {
				String name = a.groupName();
				if (!r.containsKey(name)) {
					group = name;
					last = new ArrayList<Access>();
					last.add(a);
					r.put(group, last);
				} else {
					r.get(name).add(a);
				}
				// last.add(a);
			}
		}

		return r;
	}

}
