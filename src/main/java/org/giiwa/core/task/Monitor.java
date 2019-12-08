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
package org.giiwa.core.task;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.UID;
import org.giiwa.core.bean.X;
import org.giiwa.core.cache.Cache;
import org.giiwa.core.json.JSON;

/**
 * used to monitor a Task
 * 
 * @author wujun
 *
 */
public class Monitor {

	static Log log = LogFactory.getLog(Monitor.class);

	/**
	 * Start.
	 *
	 * @param t  the t
	 * @param ms the ms
	 * @return the long
	 */
	public static long start(Task t, long ms) {
		long tid = UID.next("monitor.id");
		t.attach("tid", tid);
		flush(t);

		t.schedule(ms);
		return tid;
	}

	private static String _name(long tid) {
		return "task/monitor/" + tid;
	}

	public static void flush(Task t) {

		long tid = t.attach("tid");
		String name = _name(tid);

		Field[] fs = t.getClass().getDeclaredFields();
		if (fs != null) {
			JSON jo = JSON.create();
			for (Field f : fs) {
				int p = f.getModifiers();
				if ((p & Modifier.TRANSIENT) != 0 || (p & Modifier.STATIC) != 0 || (p & Modifier.FINAL) != 0)
					continue;

				try {
					if (log.isDebugEnabled())
						log.debug(f.getName() + "=" + f.getType());
					f.setAccessible(true);
					jo.put(f.getName(), f.get(t));
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}

			Cache.set(name, jo, X.AMINUTE * 2);
		}
	}

	/**
	 * Gets the.
	 *
	 * @param tid the tid
	 * @return the json
	 */
	public static JSON get(long tid) {

		String name = _name(tid);

		Object t = Cache.get(name);
		if (t != null) {
			if (t instanceof JSON) {
//				Cache.remove(name);
				return (JSON) t;
			}
		}

		return null;
	}

}
