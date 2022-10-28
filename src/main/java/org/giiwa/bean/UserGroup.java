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

/**
 * access token class, it's Bean and mapping to "gi_usergroup" table, it mapping
 * the "access" method in @Path interface. <br>
 * table="gi_usergroup"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_usergroup", memo = "GI-用户组")
public final class UserGroup extends Bean {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	public final static BeanDAO<String, UserGroup> dao = BeanDAO.create(UserGroup.class);

	@Column(memo = "名称")
	private String name;

	@Column(memo = "备注")
	private String memo;

	@Column(memo = "父节点")
	private String parent;

	
}
