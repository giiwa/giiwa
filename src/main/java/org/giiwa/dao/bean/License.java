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
package org.giiwa.dao.bean;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.*;
import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.misc.Digest;
import org.giiwa.misc.RSA;
import org.giiwa.task.Task;
import org.giiwa.web.Module;

/**
 * access token class, it's Bean and mapping to "gi_access" table, it mapping
 * the "access" method in @Path interface. <br>
 * table="gi_access"
 * 
 * @author yjiang
 * 
 */
@Table(name = "gi_license")
public class License extends Bean {
	/**
	* 
	*/
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(License.class);

	public final static BeanDAO<String, License> dao = BeanDAO.create(License.class);

	public static enum LICENSE {
		free, trial, limited, licensed, issue
	};

	@Column(name = X.ID)
	private String id; // name

	@Column(name = "code")
	private String code;

	@Column(name = "content")
	private String content;

	public static void init() {

		if (log.isDebugEnabled())
			log.debug("init license ...");

		new Task() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void onExecute() {
				int s = 0;
				W q = W.create().sort("created", 1);
				Beans<License> bs = dao.load(q, s, 10);
				while (bs != null && !bs.isEmpty()) {
					for (License e : bs) {
						e.decode();
					}

					s += bs.size();
					bs = dao.load(q, s, 10);
				}
			}

			@Override
			public void onFinish() {
				this.schedule(X.AMINUTE);
			}

		}.schedule(0);
	}

	public boolean decode() {
		Module m = Module.module(id);
		if (m != null) {
			String key = m.getKey();
			try {
				String code = new String(RSA.decode(Base64.getDecoder().decode(this.code), key));

				if (!X.isEmpty(code)) {
					JSON jo = JSON.fromObject(Digest.aes_decrypt(Base64.getDecoder().decode(this.content), code));
					if (jo != null) {
						keys.put(id, jo);
						m.setLicense(LICENSE.valueOf(jo.getString("type")), jo.getString("code"));
						return true;
					}
				}

			} catch (Exception e) {
				// log.error(e.getMessage(), e);
				// GLog.applog.error("license", "decode", id, e, null, null);
			}
		}
		return false;
	}

	public static String get(String name, String key) {
		JSON j = keys.get(name);
		if (j != null) {
			return j.getString(key);
		}
		return null;
	}

	public static void remove(String name) {
		keys.remove(name);
	}

	public static JSON get(String name) {
		if (keys.containsKey(name)) {
			return keys.get(name).copy().remove("company", "type", "code");
		} else {
			return JSON.create();
		}
	}

	private final static Map<String, JSON> keys = new HashMap<String, JSON>();

	public void store() {
		try {
			if (dao.exists(id)) {
				dao.update(id, V.create("content", content).append("code", code));
			} else {
				dao.insert(V.create(X.ID, id).append("content", content).append("code", code));
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
