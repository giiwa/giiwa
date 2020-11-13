package org.giiwa.engine;

import java.util.HashMap;
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

/**
 * JS utility
 * 
 * @author joe
 *
 */
public class JS {

	static Log log = LogFactory.getLog(JS.class);

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

//			System.out.println(r);
//			System.out.println(bs.keySet());
//			System.out.println(params.keySet());

		} catch (Exception e1) {
			log.error(bindings.keySet() + ", " + (params == null ? "[]" : params.keySet()), e1);
			throw e1;
		}

		return r;

	}

	private static Map<String, CompiledScript> cs_cached = new HashMap<String, CompiledScript>();

	private static CompiledScript compile(String code) throws ScriptException {
		String id = UID.id(code);
		CompiledScript e = cs_cached.get(id);
		if (e == null) {
			e = ((Compilable) engine).compile(code);
			cs_cached.put(id, e);
		}
		return e;
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

		if (f.contains("+") || f.contains("-") || f.contains("*") || f.contains("/") || f.contains("(")) {
			Object o = run(f, null);
			return o;
		}
		return f;

	}

}
