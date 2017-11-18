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
import org.giiwa.core.bean.Table;
import org.giiwa.core.bean.X;

/**
 * The code bean, used to store special code linked with s1 and s2 fields
 * table="gi_code"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_message")
public class Message extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static BeanDAO<Message> dao = BeanDAO.create(Message.class);

	@Column(name = X.ID, index = true)
	private long id;

	@Column(name = "touid")
	private long touid;

	@Column(name = "fromuid")
	private long fromuid;

	@Column(name = "priority")
	private int priority;

	@Column(name = "flag")
	private int flag;

	@Column(name = "title")
	private String title;

	@Column(name = "content")
	private String content;

	@Column(name = "attachment")
	private List<String> attachment;

}
