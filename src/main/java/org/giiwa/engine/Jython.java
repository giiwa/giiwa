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

import java.io.File;
import java.io.InputStream;
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
import org.giiwa.web.Controller;

public class Jython {

	static Log log = LogFactory.getLog(Jython.class);

	public static int MAX_CACHED_COMPILED = 4000;

	private static ScriptEngine engine = null;

	public static Object run(String code) throws Exception {
		return run(code, null);
	}

	public static boolean check(String name) {
		return true;
	}

	public static boolean remove(String name) {
		return true;
	}

	public static void install(String name, InputStream packages) {
		// copy file to sys.path
		File path = new File(Controller.GIIWA_HOME + "/jython/packages/");
		if (!path.exists()) {
			X.IO.mkdirs(path);
		}
		// copy

	}

	public static Object run(String code, Map<String, Object> params) throws Exception {

		synchronized (JS.class) {
			if (engine == null) {

				ScriptEngineManager manager = new ScriptEngineManager();
				engine = manager.getEngineByName("jython");

				if (engine == null) {
					log.error("can not get jython engine! factories=" + manager.getEngineFactories());
				}
			}
		}

		String code1 = "def __aaa():\n";
		int i = 0;
		while (i > -1) {
			i = code.indexOf("\n");
			code1 += "    " + code.substring(0, i + 1);
			code = code.substring(i + 1);
			if (!X.isEmpty(code)) {
				if (code.charAt(0) == '\r') {
					code = code.substring(1);
				}
			}
			i = code.indexOf("\n");
		}
		if (code.length() > 0) {
			code1 += "    " + code;
		}
		code1 += "\nresult=__aaa()";

		_E e = compile(code1, params != null);
		Bindings bindings = e.get();

		Object r = null;
		try {

			if (params != null) {
				bindings.putAll(params);
			}

			e.cs.eval(bindings);

			r = bindings.get("result");
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
			if (e.id == null) {

				if (log.isDebugEnabled()) {
					log.debug("no cache for jython code, code=" + code);
				}

				e.id = id;
				e.cs = ((Compilable) engine).compile(code);
				if (cache && code.indexOf(";") > 0 || code.indexOf(" ") > 0) {

					if (cached.size() > MAX_CACHED_COMPILED) {

						if (log.isWarnEnabled()) {
							log.warn("jython cache exceed max, max = " + MAX_CACHED_COMPILED);
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
