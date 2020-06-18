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

import java.util.*;

import org.giiwa.bean.Role.RoleAccess;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.W;

/**
 * group roles.
 * 
 * @author yjiang
 * 
 */
public class Roles extends Bean implements IRole {

	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static List<IRole> handlers = new ArrayList<IRole>();

	private List<String> access;

	transient Beans<Role> list;

	public static void addHandler(IRole r) {
		handlers.add(r);
	}

	public Beans<Role> getList() {
		return list;
	}

	public List<String> getAccesses() {
		return access;
	}

	public Roles() {

	}

	/**
	 * Instantiates a new roles.
	 * 
	 * @param roles the roles
	 */
	@SuppressWarnings("unchecked")
	public Roles(List<Long> roles) {
		if (roles != null && !roles.isEmpty()) {
			list = Role.dao.load(W.create().and("id", roles), 0, 100);
			access = (List<String>) RoleAccess.dao.distinct("name", W.create().and("rid", roles));
		}
	}

	/**
	 * Checks for access.
	 * 
	 * @param name the name
	 * @return true, if successful
	 */
	public boolean hasAccess(long uid, String... name) throws Exception {
		if (name == null) {
			return true;
		}
		if (handlers != null && !handlers.isEmpty()) {
			for (IRole r : handlers) {
				if (r.hasAccess(uid, name)) {
					return true;
				}
			}
		}

		for (String s : name) {
			if (access != null && !access.isEmpty()) {
				if (access.contains(s)) {
					return true;
				}
			}

			/**
			 * test the name exists in while access? if not then add it in DB
			 */
			if (!X.isEmpty(s)) {
				Access.set(s);
			}

			/**
			 * check if has admin ?
			 */
			int i = s.lastIndexOf(".");
			if (i > 0) {
				String s1 = s.substring(0, i) + ".admin";
				if (access != null && access.contains(s1)) {
					return true;
				}
			}
		}

		return access != null && access.contains("access.config.admin");
		// }
		// return false;
	}

}
