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
@Table(name = "gi_message")
public class Message extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public static BeanDAO<Message> dao = BeanDAO.create(Message.class);

	public static final int FLAG_UNREAD = 0;
	public static final int FLAG_READ = 1;
	public static final int FLAG_REPLY = 2;
	public static final int FLAG_FORWARD = 3;

	@Column(name = X.ID, index = true)
	private long id;

	@Column(name = "refer")
	private long refer;

	@Column(name = "tag")
	private String tag;

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

	public static long create(V v) {
		try {
			long id = UID.next("message.id");
			while (dao.exists(id)) {
				id = UID.next("message.id");
			}
			dao.insert(v.force(X.ID, id).append("flag", Message.FLAG_UNREAD));
			return id;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return -1;
	}

	public long getId() {
		return id;
	}

	public long getRefer() {
		return refer;
	}

	public String getTag() {
		return tag;
	}

	public long getTouid() {
		return touid;
	}

	private transient User to_obj;

	public User getTo_obj() {
		if (to_obj == null && touid > 0) {
			to_obj = User.dao.load(touid);
		}
		return to_obj;
	}

	public long getFromuid() {
		return fromuid;
	}

	private transient User from_obj;

	public User getFrom_obj() {
		if (from_obj == null && fromuid > 0) {
			from_obj = User.dao.load(fromuid);
		}
		return from_obj;
	}

	public int getPriority() {
		return priority;
	}

	public int getFlag() {
		return flag;
	}

	public String getTitle() {
		return title;
	}

	public String getContent() {
		return content;
	}

	public List<String> getAttachment() {
		return attachment;
	}

}
