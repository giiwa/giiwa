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
package org.giiwa.pool;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.X;

public class Delegator implements java.lang.reflect.InvocationHandler {

	private static Log log = LogFactory.getLog(Delegator.class);

	Object obj;
	@SuppressWarnings("rawtypes")
	private Pool pool;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <E> E create(Object obj, Pool pool) throws Exception {

		if (obj == null)
			return null;

		List<Class> cc = allInterfaces(obj);
		if (cc.isEmpty()) {
			throw new Exception("not interface");
		}
		log.info("create delegator for [" + obj.getClass() + "], with interfaces " + cc + "");

		Delegator d = new Delegator(obj, pool);

		Object o = java.lang.reflect.Proxy.newProxyInstance(obj.getClass().getClassLoader(),
				cc.toArray(new Class[cc.size()]), d);

		_cache.put(o, new WeakReference<Delegator>(d));

		return (E) o;

	}

	@SuppressWarnings("rawtypes")
	private static Map<Class, List<Class>> _clazz = new HashMap<Class, List<Class>>();

	@SuppressWarnings("rawtypes")
	private static List<Class> allInterfaces(Object obj) {

		Class c1 = obj.getClass();

		synchronized (_clazz) {
			List<Class> l1 = _clazz.get(c1);
			if (l1 != null) {
				return l1;
			}
			l1 = new ArrayList<Class>();

			Class c = c1;
			while (c != null) {
				Class[] cc = c.getInterfaces();
				if (cc != null && cc.length > 0) {
					l1.addAll(Arrays.asList(cc));
				}
				c = c.getSuperclass();
			}
			_clazz.put(c1, l1);
			return l1;
		}

	}

	@SuppressWarnings("rawtypes")
	private Delegator(Object obj, Pool pool) {
		this.obj = obj;
		this.pool = pool;
	}

	@SuppressWarnings("unchecked")
	public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {

//		log.warn("Delegator.name=" + m.getName());

		if (pool != null && X.isSame(m.getName(), "close")) {
			pool.release(proxy);
			return null;
		} else {
			return m.invoke(obj, args);
		}
	}

	static Map<Object, WeakReference<Delegator>> _cache = new HashMap<Object, WeakReference<Delegator>>();

}
