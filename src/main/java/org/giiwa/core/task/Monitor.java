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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.UID;
import org.giiwa.core.json.JSON;

/**
 * used to monitor a Task
 * 
 * @author wujun
 *
 */
public class Monitor {

	static Log log = LogFactory.getLog(Monitor.class);

	static private Map<Long, Object> cache = new HashMap<Long, Object>();

	public synchronized static void finished(Task t) {
		for (long tid : cache.keySet().toArray(new Long[cache.size()])) {
			if (t == cache.get(tid)) {
				JSON jo = get(tid);
				cache.put(tid, jo);
				return;
			}
		}
	}

	/**
	 * Start.
	 *
	 * @param t  the t
	 * @param ms the ms
	 * @return the long
	 */
	public static long start(Task t, long ms) {
		long tid = UID.next("monitor.id");
		cache.put(tid, t);
		t.schedule(ms);
		return tid;
	}

	/**
	 * Stop.
	 *
	 * @param tid the tid
	 */
	public static void stop(long tid) {
		Object t = cache.remove(tid);
		if (t != null && t instanceof Task) {
			((Task) t).stop(true);
		}
	}

	/**
	 * Gets the.
	 *
	 * @param tid the tid
	 * @return the json
	 */
	public synchronized static JSON get(long tid) {
		Object t = cache.get(tid);
		if (t != null) {
			if (t instanceof JSON) {
				cache.remove(tid);
				return (JSON) t;
			}

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

				return jo;
			}
		}
		return null;
	}

}
