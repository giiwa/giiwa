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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.giiwa.conf.Global;
import org.giiwa.dao.X;

public class TimingCache {

	private static Map<Object, _O> _cache = new HashMap<Object, _O>();

	public static int LIMITED = 1000;

	public static int getCached() {
		return _cache.size();
	}

	public static void set(Class<?> c, Object id, Object o, long expired) {

		String key = c.getName() + "/" + id;

		_O e = _O.create(key, o, expired);
		if (e == null) {
			synchronized (_cache) {
				e = _cache.remove(key);
			}
		} else {
			synchronized (_cache) {
				_cache.put(key, e);

				if (_cache.size() >= LIMITED) {
					List<_O> _queue = new ArrayList<_O>();
					_queue.addAll(_cache.values());
					Collections.sort(_queue);

					int S = LIMITED / 5;
					for (int i = 0; i < S; i++) {
						// remove oldest
						e = _queue.get(i);
						_cache.remove(e.key);
					}
				}
			}

		}
	}

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

	static class _O implements Comparable<_O> {

		String key;
		Object o;
		long created = Global.now();
		long expired = created;

		static _O create(String key, Object o, long expired) {
			if (o == null) {
				return null;
			}
			_O e = new _O();
			e.key = key;
			e.o = o;
			e.expired = Global.now() + expired;
			return e;
		}

		public boolean expired() {
			return Global.now() > expired;
		}

		@Override
		public int compareTo(_O o) {
			if (expired < o.expired) {
				return -1;
			} else if (expired > o.expired) {
				return 1;
			}
			return 0;
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

}
