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
import org.giiwa.dao.UID;
import org.giiwa.dao.X;
import org.giiwa.task.Task;

/**
 * JS utility
 * 
 * @author joe
 *
 */
public class JS {

	static Log log = LogFactory.getLog(JS.class);

	public static int MAX_CACHED_COMPILED = 4000;

	private static ScriptEngine engine = null;

	public static Object run(String js) throws Exception {
		return run(js, null);
	}

	public static Object run(String js, Map<String, Object> params) throws Exception {

//		TimeStamp t1 = TimeStamp.create();

		synchronized (JS.class) {
			if (engine == null) {

				ScriptEngineManager manager = new ScriptEngineManager();
//				log.debug("factories=" + manager.getEngineFactories());
				engine = manager.getEngineByName("nashorn");

//				System.out.println(engine);
//				jdk.nashorn.api.scripting.NashornScriptEngineFactory fa = null;
//				for (ScriptEngineFactory f : manager.getEngineFactories()) {
//					System.out.println(f.getEngineName());
//					if (X.isIn(f.getEngineName(), "nashorn", "Oracle Nashorn")) {
//						fa = (jdk.nashorn.api.scripting.NashornScriptEngineFactory) f;
//						break;
//					}
//				}
//				String[] stringArray = new String[] { "-doe", "--global-per-engine" };
//				engine = fa.getScriptEngine(stringArray);

				if (engine == null) {
					log.error("can not get nashorn engine! factories=" + manager.getEngineFactories());
				}
			}
		}

		if (params != null && !params.isEmpty()) {

			List<String> l1 = X.asList(params.keySet(), s -> {
				if (!X.isEmpty(s)) {
					String s1 = (String) s;
					char c = s1.charAt(0);
					if ((c == '_') || (c >= 'A' && c <= 'z')) {
						return s1;
					}
				}
				return null;
			});

			if (l1 == null || X.isEmpty(l1)) {

			} else {
				js = "function __aaa(args, " + X.join(l1, ",") + ") {" + js + "\n};__aaa(__p, "
						+ X.join(X.asList(l1, s -> "__p." + s), ",") + ");";
			}

		}

		_E e = compile(js, params != null);
		Bindings bindings = e.get();

		Object r = null;
		try {

			bindings.put("__p", params);

			r = e.cs.eval(bindings);

			e.release(bindings);

		} catch (Exception e1) {
			if (params != null && !params.isEmpty()) {
				log.error((params == null ? "[]" : params.keySet()) + "\r\n" + js, e1);
			}
			throw e1;
		}

		return r;

	}

	private static Map<String, _E> cached = new HashMap<String, _E>();

	private static _E compile(String code, boolean cache) throws ScriptException {

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
			if (e.id == null || e.cs == null) {

				if (log.isDebugEnabled()) {
					log.debug("no cache for js code, code=" + code);
				}

				e.id = id;
				e.cs = ((Compilable) engine).compile(code);

				if (cache && code.indexOf(";") > 0 || code.indexOf(" ") > 0) {
					cached.put(id, e);

					if (cached.size() > MAX_CACHED_COMPILED) {

						if (log.isWarnEnabled()) {
							log.warn("js cache exceed max, max = " + MAX_CACHED_COMPILED);
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

		e.last = System.currentTimeMillis();

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
//			bs.remove("p");
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
