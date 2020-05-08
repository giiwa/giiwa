package org.giiwa.engine;

import java.util.HashMap;
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
import org.giiwa.task.Task;

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
		Bindings bs = _E.engine.createBindings();
		if (params != null) {
			bs.putAll(params);
		}

		Object r = e.compiled.eval(bs);
		bs.clear();
		
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
		CompiledScript compiled;
	}

	public static void main(String[] args) {

		Task.init(10);

		String s = "a = 0;for(i=0;i<10000;i++) {a+=b;}";
		try {
			Map<String, Object> p1 = new HashMap<String, Object>();
			p1.put("b", 10);

			Map<String, Object> p2 = new HashMap<String, Object>();
			p2.put("b", 5);

			Task.schedule(() -> {
				try {
					System.out.println(run(s, p1));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			Task.schedule(() -> {
				try {
					System.out.println(run(s, p2));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});

			Object r = calculate("2+1.0");
			System.out.println(r + ", " + r.getClass());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
