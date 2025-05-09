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
package org.giiwa.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.dao.UID;
import org.giiwa.task.Task;

public class Groovy {

	static Log log = LogFactory.getLog(Groovy.class);

	public static int MAX_CACHED_COMPILED = 4000;

	private static ScriptEngine engine = null;

	public static Object run(String code) throws Exception {
		return run(code, null);
	}

	public static Object run(String code, Map<String, Object> params) throws Exception {

		synchronized (JS.class) {
			if (engine == null) {

				ScriptEngineManager manager = new ScriptEngineManager();
				engine = manager.getEngineByName("groovy");

				if (engine == null) {
					log.error("can not get groovy engine! factories=" + manager.getEngineFactories());
				}
			}
		}

		_E e = compile(code, params != null);
		Bindings bindings = e.get();

		Object r = null;
		try {

			if (params != null) {
				bindings.putAll(params);
			}

			r = e.cs.eval(bindings);

			e.release(bindings);

		} catch (Exception e1) {
			if (params != null && !params.isEmpty()) {
				log.error((params == null ? "[]" : params.keySet()) + "\r\n" + code, e1);
			}
			throw e1;
		}

		return r;

	}

	private static Map<String, _E> cached = new HashMap<String, _E>();

	private static synchronized _E compile(String code, boolean cache) throws ScriptException {

		String id = UID.id(code);
		_E e = null;
		synchronized (cached) {
			e = cached.get(id);
			if (e == null) {
				e = new _E();
				cached.put(id, e);
			}
		}
		synchronized (e) {
			if (e.id == null) {

				if (log.isDebugEnabled()) {
					log.debug("no cache for java code, code=" + code);
				}

				e.id = id;
				e.cs = ((Compilable) engine).compile(code);

				if (cache && code.indexOf(";") > 0 || code.indexOf(" ") > 0) {

					if (cached.size() > MAX_CACHED_COMPILED) {

						if (log.isWarnEnabled()) {
							log.warn("java cache exceed max, max = " + MAX_CACHED_COMPILED);
						}

						Task.schedule(t -> {
							List<_E> l1 = new ArrayList<_E>(cached.values());
							Collections.sort(l1);
							for (int i = 0; i < l1.size() / 4; i++) {
								_E e1 = l1.get(i);
								cached.remove(e1.id);
							}

						});
					}
				}

			} else {
				e.cached = true;
			}
		}

		e.last = Global.now();

		return e;
	}

	static class _E implements Comparable<_E> {

		CompiledScript cs;
		Stack<Bindings> _cached = new Stack<Bindings>();

		String id;
		long last;
		boolean cached;

		public synchronized Bindings get() {
			if (_cached.isEmpty()) {
				return new SimpleBindings();
			}

			Bindings bs = _cached.pop();
			bs.clear();
			return bs;
		}

		public synchronized void release(Bindings bindings) {
			if (_cached.size() < 100) {
				_cached.push(bindings);
			}
		}

		@Override
		public int compareTo(_E o) {
			if (last < o.last) {
				return -1;
			}
			return 1;
		}

	}

	/**
	 * 
	 * @param f the string such as: 10*20
	 * @return the Object
	 * @throws Exception the Exception
	 */
	public static Object calculate(String f) throws Exception {

		if (f.startsWith("'") || f.startsWith("\"")) {
			return f;
		}

		Object o = run(f, null);
		return o;

	}

}
