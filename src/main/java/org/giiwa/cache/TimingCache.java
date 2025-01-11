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
package org.giiwa.cache;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.dao.X;
import org.giiwa.task.SysTask;

public class TimingCache {

	private static Map<Object, _O> _cache = new HashMap<Object, _O>();

	public static int LIMITED = 1000;

	public static int getCached() {
		return _cache.size();
	}

	public static void set(Class<?> c, Object id, Object o, long expired) {

		String key = c.getName() + "/" + id;

		_O e = _O.create(o, expired);
		if (e == null) {
			synchronized (_cache) {
				_cache.remove(key);
			}
		} else {
			synchronized (_cache) {
				_cache.put(key, e);
			}

			if (_cache.size() >= LIMITED) {
				cleanup.schedule(0);
			}
		}
	}

	private static _Cleanup cleanup = new _Cleanup();

	/**
	 * 
	 * cache the data or remove data if o is null
	 * 
	 * expired after a minute
	 * 
	 * @param id
	 * @param o
	 */
	public static void set(Class<?> c, Object id, Object o) {
		set(c, id, o, X.AMINUTE);
	}

	/**
	 * get the cached data
	 * 
	 * @param <T>
	 * @param id
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T get(Class<?> c, Object id) {

		String key = c.getName() + "/" + id;

		_O e = _cache.get(key);
		if (e != null && !e.expired()) {
			return (T) e.o;
		} else {
			synchronized (_cache) {
				_cache.remove(key);
			}
		}
		return null;
	}

	static class _O {

		Object o;
		long created = System.currentTimeMillis();
		long expired = X.AMINUTE;

		static _O create(Object o, long expired) {
			if (o == null) {
				return null;
			}
			_O e = new _O();
			e.o = o;
			e.expired = expired;
			return e;
		}

		public boolean expired() {
			return System.currentTimeMillis() - created > expired;
		}

	}

	public static boolean exists(Class<?> c, Object id) {
		String id1 = c.getName() + "/" + id;
		return _cache.containsKey(id1);
	}

	public static void remove(Class<?> c, Object id) {
		String id1 = c.getName() + "/" + id;
		synchronized (_cache) {
			_cache.remove(id1);
		}
	}

	public static void remove(Class<?> c) {

		String id1 = c.getName() + "/";

		Object[] tt = null;
		synchronized (_cache) {
			tt = _cache.keySet().toArray();
		}

		for (Object t1 : tt) {
			if (t1 == null || t1.toString().startsWith(id1)) {
				synchronized (_cache) {
					_cache.remove(t1);
				}
			} else {
				_O e1 = _cache.get(t1);
				if (e1 != null && e1.expired()) {
					synchronized (_cache) {
						_cache.remove(t1);
					}
				}
			}
		}

	}

	static class _Cleanup extends SysTask {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		@Override
		public void onExecute() {
			// cleanup, remove expired data
			Object[] tt = null;
			synchronized (_cache) {
				tt = _cache.keySet().toArray();
			}
			for (Object t1 : tt) {
				_O e1 = _cache.get(t1);
				if (e1 != null && e1.expired()) {
					synchronized (_cache) {
						_cache.remove(t1);
					}
				}
			}
		}

		@Override
		public String getName() {
			return "gi.timingcache.cleanup";
		}

		@Override
		public void onFinish() {
			this.schedule(X.AMINUTE);
		}

	}
}
