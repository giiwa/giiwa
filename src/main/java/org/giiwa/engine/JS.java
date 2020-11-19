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
import org.giiwa.bean.GLog;
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

		synchronized (JS.class) {
			if (engine == null) {
				ScriptEngineManager manager = new ScriptEngineManager();
				log.debug("factories=" + manager.getEngineFactories());
				engine = manager.getEngineByName("nashorn");
//				engine = manager.getEngineByName("graal.js");
				/**
				 * graal.js & nashorn, big different between dynamic api, be careful
				 */
				log.debug("engine=" + engine);
			}
		}

		if (params != null && !params.isEmpty()) {
			js = "function aaa(" + X.join(params.keySet(), ",") + ") {" + js + "};aaa("
					+ X.join(X.asList(params.keySet(), s -> "p." + s), ",") + ");";
		}

		_E e = compile(js, params != null);
		Bindings bindings = e.get();

		Object r = null;
		try {

			if (params != null) {
				bindings.put("p", params);
			}

			r = e.cs.eval(bindings);

		} catch (Exception e1) {
			if (params != null && !params.isEmpty()) {
				log.error(bindings.keySet() + ", " + (params == null ? "[]" : params.keySet()), e1);
			}
			throw e1;
		} finally {
			e.release(bindings);
		}

		return r;

	}

	private static Map<String, _E> cached = new HashMap<String, _E>();

	private static synchronized _E compile(String code, boolean cache) throws ScriptException {

		String id = UID.id(code);
		_E e = cached.get(id);
		if (e == null) {

			log.debug("no cache for js code, code=" + code);

			e = new _E();
			e.id = id;
			e.cs = ((Compilable) engine).compile(code);
			if (cache) {
				cached.put(id, e);

				if (cached.size() > MAX_CACHED_COMPILED) {

					GLog.applog.warn("js", "run", "js cache exceed max, max = " + MAX_CACHED_COMPILED);

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

		}
		e.last = System.currentTimeMillis();

		return e;
	}

	static class _E implements Comparable<_E> {

		CompiledScript cs;
		List<Bindings> _cached = new ArrayList<Bindings>();

		String id;
		long last;

		public synchronized Bindings get() {
			if (_cached.isEmpty()) {
				return new SimpleBindings();
			}

			return _cached.remove(_cached.size() - 1);
		}

		public synchronized void release(Bindings bindings) {
			_cached.add(bindings);
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
