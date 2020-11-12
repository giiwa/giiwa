package org.giiwa.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.UID;

/**
 * JS utility
 * 
 * @author joe
 *
 */
public class JS {

	static Log log = LogFactory.getLog(JS.class);

	public static Object run(String js) throws Exception {
		return run(js, null);
	}

	public static Object run(String js, Map<String, Object> params) throws Exception {

		_E e = compile(js);

		Object r = null;
		Bindings bs = e.get();
		try {
			if (params != null) {
				bs.putAll(params);
			}

			r = e.compiled.eval(bs);
		} catch (Exception e1) {
			log.error(bs.keySet(), e1);
		} finally {
			e.release(bs);
		}

		return r;

	}

	private synchronized static _E compile(String code) throws ScriptException {
		String id = UID.id(code);
		_E e = cached.get(id);
		if (e == null) {
			if (_E.engine == null) {
				ScriptEngineManager manager = new ScriptEngineManager();
				_E.engine = manager.getEngineByName("JavaScript");
			}
			e = new _E();
			e.compiled = ((Compilable) _E.engine).compile(code);
//			e.engine = engine;
			cached.put(id, e);
		}
		return e;
	}

	private static Map<String, _E> cached = new HashMap<String, _E>();

	static class _E {
		static ScriptEngine engine;
		static List<Bindings> l1 = new ArrayList<Bindings>();

		CompiledScript compiled;

		public synchronized Bindings get() {

			if (l1.isEmpty()) {
				return engine.createBindings();
			} else {
				return l1.remove(0);
			}

		}

		public synchronized void release(Bindings bs) {

			bs.clear();
			l1.add(bs);

		}

	}

	/**
	 * 
	 * @param f the string such as: 10*20
	 * @return the Object
	 * @throws Exception the Exception
	 */
	public static Object calculate(String f) throws Exception {
		Object o = run(f, null);
		return o;
	}
}
