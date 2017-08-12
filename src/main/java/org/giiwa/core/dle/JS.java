package org.giiwa.core.dle;

import java.util.HashMap;
import java.util.Map;

import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class JS {

	static Log log = LogFactory.getLog(JS.class);

	public static Object run(String js, Map<String, Object> params) throws Exception {

		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine engine = manager.getEngineByName("JavaScript");

		Bindings bs = engine.createBindings();
		if (params != null) {
			bs.putAll(params);
		}
		bs.put("log", log);

		engine.setBindings(bs, ScriptContext.ENGINE_SCOPE);

		return engine.eval(js);

	}

	public static void main(String[] args) {
		String s = "log.debug(\"aaaaaa\");a = b + 1";
		Map<String, Object> p = new HashMap<String, Object>();
		try {
			p.put("b", 10);
			System.out.println(run(s, p));

			Object r = calculate("2+1.0");
			System.out.println(r + ", " + r.getClass());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param f
	 *            such as: 10*20
	 * @return
	 * @throws Exception
	 */
	public static Object calculate(String f) throws Exception {
		Object o = run(f, null);
		return o;
	}
}
