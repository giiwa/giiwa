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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.Session.SID;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.BeanDAO;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Column;
import org.giiwa.dao.Table;
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.task.BiConsumer;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;

/**
 * The Message bean, table="gi_messagee"
 * 
 * @author wujun
 *
 */
@Table(name = "gi_message", memo = "GI-消息")
public final class Message extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Message.class);

	public static final BeanDAO<String, Message> dao = BeanDAO.create(Message.class, time -> {
		// return cleanup query
		return W.create().and("created", Global.now() - X.AHOUR, W.OP.lte);
	});

	@Column(memo = "主键", unique = true, size = 50)
	private String id;

	@Column(memo = "会话ID", size = 50)
	private String sid;

	@Column(memo = "命令", size = 50)
	public String command;

	@Column(memo = "消息", size = 2048)
	public String message;

	private static Map<String, List<BiConsumer<String, String>>> listeners = new HashMap<String, List<BiConsumer<String, String>>>();

	public static void addListener(String[] command, BiConsumer<String, String> func) {
		for (String s : command) {
			List<BiConsumer<String, String>> e = listeners.get(s);
			if (e == null) {
				e = new ArrayList<BiConsumer<String, String>>();
				listeners.put(s, e);
			}
			if (!e.contains(func)) {
				e.add(func);
			}
		}
	}

	public static void create(long uid, String command, String message) {
		create(uid, command, message, V.create());
	}

	public static void create(long uid, String command, String message, V v) {
		try {
			if (uid <= 0) {
				return;
			}

			W q = W.create().and("uid", uid).sort("updated", -1);
			Beans<SID> bs = SID.dao.load(q, 0, 16);
			if (bs != null) {
				bs.forEach(e -> {
					create(e.sid, command, message, v);
				});
			}

			List<BiConsumer<String, String>> l1 = listeners.get(command);
			if (l1 != null) {
				for (BiConsumer<String, String> e : l1) {
					e.accept(command, message);
				}
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	public static void create(String sid, String command, String message) {
		create(sid, command, message, V.create());
	}

	static private Lock lock = new ReentrantLock();

	public static void create(String sid, String command, String message, V v) {

		try {
			if (X.isEmpty(sid)) {
				return;
			}

			String id = UID.id(Global.now(), sid, command, message, v.toString());
			if (lock.tryLock()) {
				try {
					if (dao.exists(id)) {
						return;
					}

					V v1 = v.copy();
					v1.append(X.ID, id);
					v1.append("sid", sid);
					v1.append("command", command);
					v1.append(X.MESSAGE, message);

					dao.insert(v1);

					if (log.isDebugEnabled()) {
						log.debug("create message, command=" + command + ", message=" + message);
					}
				} finally {
					lock.unlock();
				}
			}

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

	}

}
