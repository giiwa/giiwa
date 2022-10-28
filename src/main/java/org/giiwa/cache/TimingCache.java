package org.giiwa.cache;

import java.util.HashMap;
import java.util.Map;

import org.giiwa.dao.X;
import org.giiwa.task.Task;

public class TimingCache {

	private static Map<Object, _O> _cache = new HashMap<Object, _O>();

	public static int LIMITED = 1000;

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
				Task.schedule(t -> {
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
				});
			}
		}
	}

	/**
	 * 
	 * cache the data or remove data if o is null
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
			if (t1.toString().startsWith(id1)) {
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
