package org.giiwa.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.giiwa.task.Task;

/**
 * JS utility
 * 
 * @author joe
 *
 */
public class JS {

	static Log log = LogFactory.getLog(JS.class);

	public static int MAX_CACHED_COMPILED = 2000;

	private static ScriptEngine engine = null;

	public static Object run(String js) throws Exception {
		return run(js, null);
	}

	public static Object run(String js, Map<String, Object> params) throws Exception {

		if (engine == null) {
			ScriptEngineManager manager = new ScriptEngineManager();
			engine = manager.getEngineByName("JavaScript");
		}

		CompiledScript cs = compile(js);
		Bindings bindings = new SimpleBindings();

		Object r = null;
		try {

			if (params != null) {
				bindings.putAll(params);
			}

			r = cs.eval(bindings);

		} catch (Exception e1) {
			if (params != null && !params.isEmpty()) {
				log.error(bindings.keySet() + ", " + (params == null ? "[]" : params.keySet()), e1);
			}
			throw e1;
		}

		return r;

	}

	private static Map<String, _E> cached = new HashMap<String, _E>();

	private static CompiledScript compile(String code) throws ScriptException {
		String id = UID.id(code);
		_E e = cached.get(id);
		if (e == null) {
			e = new _E();
			e.id = id;
			e.cs = ((Compilable) engine).compile(code);
			cached.put(id, e);

			if (cached.size() > MAX_CACHED_COMPILED) {
				Task.schedule(() -> {
					List<_E> l1 = new ArrayList<_E>(cached.values());
					Collections.sort(l1);
					for (int i = 0; i < l1.size() / 4; i++) {
						_E e1 = l1.get(i);
						cached.remove(e1.id);
					}

				});
			}
		}
		e.last = System.currentTimeMillis();

		return e.cs;
	}

	static class _E implements Comparable<_E> {
		CompiledScript cs;
		String id;
		long last;

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
