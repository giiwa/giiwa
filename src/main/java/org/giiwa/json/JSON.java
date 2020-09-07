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
package org.giiwa.json;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.engine.JS;
import org.giiwa.misc.Base32;
import org.giiwa.misc.Digest;
import org.giiwa.misc.StringFinder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * The Class JSON, simple JSON object, using Gson to parse and format, <br>
 * and find api by xpath <br>
 * 
 * @author wujun
 */
public final class JSON extends HashMap<String, Object> implements Cloneable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(JSON.class);

	/**
	 * parse the json object to JSON
	 * 
	 * @param json the json object
	 * @return JSON, null if failed
	 */
	public static JSON fromObject(Object json) {
		return fromObject(json, false);
	}

	/**
	 * parse the object to JSON object
	 *
	 * @param json    the json
	 * @param lenient the boolean of JsonReader.setLenient(lenient)
	 * @return the json
	 */
	@SuppressWarnings({ "unchecked", "rawtypes", "restriction" })
	public static JSON fromObject(Object json, boolean lenient) {

		JSON j = null;
		try {
			if (json instanceof JSON) {
				j = (JSON) json;
			} else if (json instanceof jdk.nashorn.api.scripting.ScriptObjectMirror) {

				jdk.nashorn.api.scripting.ScriptObjectMirror m = (jdk.nashorn.api.scripting.ScriptObjectMirror) json;

				j = JSON.create();
				for (String key : m.keySet()) {
					j.put(key, m.get(key));
				}

			} else if (json instanceof Map) {
				j = JSON.create((Map) json);
			} else if (json instanceof String) {

				String s1 = ((String) json).trim();

				if (!X.isEmpty(s1) && s1.charAt(0) == '{') {
					Gson g = _gson();
					JsonReader reader = new JsonReader(new StringReader(s1));
					reader.setLenient(lenient);
					j = g.fromJson(reader, JSON.class);
				} else {
					// a=b&d=a
					String[] ss = X.split(s1, "[&\r\n]");
					if (ss != null && ss.length > 0) {
						j = JSON.create();
						for (String s : ss) {
							int i = s.indexOf("=");
							if (i > 0) {
								j.put(s.substring(0, i), s.substring(i + 1));
							}
						}
					}
				}
			} else if (json instanceof InputStream) {
				Gson g = _gson();
				j = g.fromJson(new InputStreamReader((InputStream) json), JSON.class);
			} else if (json instanceof File) {
				try {
					Gson g = _gson();
					j = g.fromJson(new FileReader((File) json), JSON.class);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			} else if (json instanceof Reader) {
				Gson g = _gson();
				j = g.fromJson((Reader) json, JSON.class);
			} else if (json instanceof byte[]) {
				Gson g = _gson();
				byte[] b1 = (byte[]) json;
				JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(b1)));
				reader.setLenient(lenient);
				j = g.fromJson(reader, JSON.class);
			} else if (json instanceof ResultSet) {

				ResultSet r = (ResultSet) json;
				ResultSetMetaData rmd = r.getMetaData();
				j = JSON.create();
				for (int i = 0; i < rmd.getColumnCount(); i++) {
					j.put(rmd.getColumnName(i + 1), r.getObject(i + 1));
				}

			} else if (json != null) {
				// from a object
				Field[] ff = json.getClass().getDeclaredFields();
				if (ff != null && ff.length > 0) {
					j = JSON.create();
					for (Field f : ff) {
						int m = f.getModifiers();
						if ((m & (Modifier.TRANSIENT | Modifier.STATIC | Modifier.FINAL)) == 0) {
							try {
								f.setAccessible(true);
								j.put(f.getName(), f.get(json));
							} catch (Exception e) {
								log.error(e.getMessage(), e);
							}
						}
					}
				}
			}

			_refine(j);

			if (j != null) {
				for (String name : j.keySet().toArray(new String[j.size()])) {
					Object o = j.get(name);
					if (o == null) {
						j.remove(name);
					} else if (o instanceof jdk.nashorn.api.scripting.ScriptObjectMirror) {
						jdk.nashorn.api.scripting.ScriptObjectMirror m = (jdk.nashorn.api.scripting.ScriptObjectMirror) o;
						if (m.isArray()) {
							j.put(name, JSON.fromObjects(m));
						} else {
							j.put(name, JSON.fromObject(m));
						}
					} else if (o instanceof List) {
						j.put(name, fromObjects(o));
					} else if (o instanceof Map) {
						j.put(name, fromObject(o));
					}
				}
			}
		} catch (Exception e) {
			log.error(json, e);
		}
		return j;
	}

	private static Gson _gson() {
		return new GsonBuilder().registerTypeAdapterFactory(BeanAdapter.FACTORY).excludeFieldsWithoutExposeAnnotation()
				.serializeSpecialFloatingPointValues().create();
	}

	/**
	 * parse the jsons to array of JSON.
	 *
	 * @param jsons the jsons
	 * @return the list
	 */
	@SuppressWarnings({ "rawtypes", "unchecked", "restriction" })
	public static List<JSON> fromObjects(Object jsons) {
		List list = null;
		if (jsons instanceof Collection) {

			list = new ArrayList();
			list.addAll((Collection) jsons);

		} else if (jsons instanceof String) {
			Gson g = _gson();
			if (((String) jsons).startsWith("{")) {
				list = JSON.createList();
				list.add(JSON.fromObject(jsons));
			} else {
				list = g.fromJson((String) jsons, List.class);
			}
		} else if (jsons instanceof InputStream) {
			Gson g = _gson();
			JsonReader reader = new JsonReader(new InputStreamReader((InputStream) jsons));
			list = g.fromJson(reader, List.class);
		} else if (jsons instanceof File) {
			try {
				Gson g = _gson();
				return g.fromJson(new FileReader((File) jsons), List.class);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else if (jsons instanceof Reader) {
			Gson g = _gson();
			list = g.fromJson((Reader) jsons, List.class);
		} else if (jsons instanceof byte[]) {
			Gson g = _gson();
			byte[] b1 = (byte[]) jsons;
			list = g.fromJson(new String(b1), List.class);
		} else if (jsons instanceof ResultSet) {

			try {
				ResultSet r = (ResultSet) jsons;
				ResultSetMetaData rmd = r.getMetaData();
				list = JSON.createList();
				while (r.next()) {
					JSON j = JSON.create();
					for (int i = 0; i < rmd.getColumnCount(); i++) {
						j.append(rmd.getColumnName(i + 1), r.getObject(i + 1));
					}
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else if (jsons instanceof jdk.nashorn.api.scripting.ScriptObjectMirror) {
			jdk.nashorn.api.scripting.ScriptObjectMirror m = (jdk.nashorn.api.scripting.ScriptObjectMirror) jsons;
			list = JSON.createList();
			if (m.isArray()) {
//				JSON j1 = JSON.fromObject(m);
				for (Object o : m.values()) {
					if (o instanceof jdk.nashorn.api.scripting.ScriptObjectMirror) {
						jdk.nashorn.api.scripting.ScriptObjectMirror m1 = (jdk.nashorn.api.scripting.ScriptObjectMirror) o;
						if (m1.isArray()) {
							List<?> l1 = JSON.fromObjects(m1);
							list.add(l1);
						} else {
							list.add(JSON.fromObject(m1));
						}
					} else {
						list.add(o);
					}
				}
			} else {
				list.add(JSON.fromObject(jsons));
			}
		} else if (jsons instanceof JSON) {
			list = JSON.createList();
			list.add(jsons);
		} else if (jsons instanceof Map) {
			list = JSON.createList();
			list.add(JSON.create((Map) jsons));
		}

		if (list != null) {
			for (int i = 0; i < list.size(); i++) {
				Object o = list.get(i);
				if (o instanceof List) {
					list.set(i, fromObjects(o));
				} else {
					try {
						if (o.toString().startsWith("{")) {
							list.set(i, fromObject(o));
						}
					} catch (Throwable e) {
						// ignore
					}
				}
			}
		}
		return list;
	}

	/**
	 * parse the json object to Class object
	 * 
	 * @param <T>  the Class
	 * @param json the json object or string
	 * @param t    the Class
	 * @return the object of Class
	 */
	@SuppressWarnings("rawtypes")
	public static <T> T fromObject(Object json, Class<T> t) {
		if (json instanceof String) {
			Gson g = _gson();
			return g.fromJson((String) json, t);
		} else if (json instanceof File) {
			try {
				Gson g = _gson();
				return g.fromJson(new FileReader((File) json), t);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else if (json instanceof Reader) {
			Gson g = _gson();
			return g.fromJson((Reader) json, t);
		} else if (json instanceof byte[]) {
			Gson g = _gson();
			byte[] b1 = (byte[]) json;
			return g.fromJson(new String(b1), t);
		} else if (json instanceof Map) {
			try {
				Map m = (Map) json;
				T t1 = t.newInstance();
				Field[] fs = t.getDeclaredFields();

				for (Field f : fs) {
					String name = f.getName();
					if (m.containsKey(name)) {
						f.setAccessible(true);
						f.set(t1, m.get(name));
					}
				}
				return t1;
			} catch (Exception e) {
				log.error(json, e);
			}
		}
		return null;
	}

	/**
	 * Creates a empty JSON object
	 *
	 * @return the json
	 */
	public static JSON create() {
		return new JSON();
	}

	public static List<JSON> createList() {
		return new ArrayList<JSON>();
	}

	/**
	 * create the JSON from the args
	 * 
	 * @param args the object pair, eg,: new Object[]{"aa", 1}
	 * @return the JSON
	 */
	public static JSON create(Object[]... args) {
		JSON j = new JSON();
		if (args != null && args.length > 0) {
			for (Object[] ss : args) {
				if (ss.length == 2) {
					j.put(ss[0].toString(), ss[1]);
				}
			}
		}
		return j;
	}

	/**
	 * create a json string
	 */
	public String toString() {
		Gson g = _gson();
		return g.toJson(this);
	}

	/**
	 * convert the json to url string
	 * 
	 * @deprecated
	 * @return url string
	 */
	public String toUrl() {
		return url();
	}

	/**
	 * convert the json to a url string
	 * 
	 * @return url string
	 */
	public String url() {
		StringBuilder sb = new StringBuilder();
		for (String name : this.keySet()) {
			if (sb.length() > 0)
				sb.append("&");
			sb.append(name).append("=");
			Object o = this.get(name);
			if (o != null) {
				sb.append(o);
			}
		}
		return sb.toString();
	}

	public String toPrettyString() {
		Gson gson = new GsonBuilder().registerTypeAdapterFactory(BeanAdapter.FACTORY)
				.excludeFieldsWithoutExposeAnnotation().serializeSpecialFloatingPointValues().setPrettyPrinting()
				.create();
		return gson.toJson(this);
	}

	public static JSON decodeBycode(String str, String code) {
		try {
			byte[] bb = Base32.decode(str);
			bb = Digest.des_decrypt(bb, code);
			return JSON.fromObject(bb);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	public String encodeBycode(String code) {
		try {
			String s = this.toString();
			byte[] bb = Digest.des_encrypt(s.getBytes(), code);
			return Base32.encode(bb);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private static void _refine(Map<String, Object> jo) {
		if (jo == null) {
			return;
		}
		if (jo.size() > 0) {
			for (String name : jo.keySet()) {
				Object v = jo.get(name);
				if (v instanceof Double) {
					Double d = (Double) v;
					if (d == d.intValue()) {
						jo.put(name, d.intValue());
					} else if (d == d.longValue()) {
						jo.put(name, d.longValue());
					}
				} else if (v instanceof Map) {
					v = JSON.fromObject(v);
					_refine((JSON) v);
				} else if (v instanceof List) {
					_refine((List<Object>) v);
				}
			}
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static List<Object> _refine(List<Object> l1) {
		if (l1 == null) {
			return null;
		}
		if (l1.size() > 0) {
			for (int i = 0; i < l1.size(); i++) {
				Object v = l1.get(i);
				if (v instanceof Double) {
					Double d = (Double) v;
					if (d == d.intValue()) {
						l1.set(i, d.intValue());
					} else if (d == d.longValue()) {
						l1.set(i, d.longValue());
					}
				} else if (v instanceof Map) {
					v = JSON.fromObject(v);
					_refine((Map) v);
				} else if (v instanceof List) {
					_refine((List) v);
				}
			}
		}
		return l1;
	}

	/**
	 * Creates a JSON from the map
	 *
	 * @param m the map
	 * @return the json
	 */
	public static JSON create(Map<String, Object> m) {
		JSON j = create();
		for (String name : m.keySet()) {
			j.put(name, m.get(name));
		}
		return j;
	}

	/**
	 * Checks for exists of name
	 *
	 * @param name the name
	 * @return true, if exists
	 */
	public boolean has(String name) {
		return this.containsKey(name);
	}

	/**
	 * Gets the string.
	 *
	 * @param name the name
	 * @return the string
	 */
	public String getString(String name) {
		return getString(name, X.EMPTY);
	}

	/**
	 * Gets the string.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the string
	 */
	public String getString(String name, String defaultValue) {
		Object v = this.get(name);
		if (v == null) {
			return defaultValue;
		}
		return v.toString();
	}

	/**
	 * Gets the int.
	 *
	 * @param name the name
	 * @return the int
	 */
	public int getInt(String name) {
		return getInt(name, 0);
	}

	/**
	 * Gets the int.
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the int
	 */
	public int getInt(String name, int defaultValue) {
		return X.toInt(this.get(name), defaultValue);
	}

	/**
	 * Gets the long，return 0 if the name not presented
	 *
	 * @param name the name
	 * @return the long
	 */
	public long getLong(String name) {
		return getLong(name, 0);
	}

	/**
	 * Gets the long,return defaultValue if the name not presented
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the long
	 */
	public long getLong(String name, long defaultValue) {
		return X.toLong(this.get(name), defaultValue);
	}

	/**
	 * Gets the float，return 0 if the name not presented
	 *
	 * @param name the name
	 * @return the float
	 */
	public float getFloat(String name) {
		return getFloat(name, 0f);
	}

	/**
	 * Gets the float，return defaultValue if the name not presented
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the float
	 */
	public float getFloat(String name, float defaultValue) {
		return X.toFloat(this.get(name), defaultValue);
	}

	/**
	 * Gets the double，return 0 if the name not presented
	 *
	 * @param name the name
	 * @return the double
	 */
	public double getDouble(String name) {
		return getDouble(name, 0);
	}

	/**
	 * Gets the double，return defaultValue if the name not presented
	 *
	 * @param name         the name
	 * @param defaultValue the default value
	 * @return the double
	 */
	public double getDouble(String name, double defaultValue) {
		return X.toDouble(this.get(name), defaultValue);
	}

	/**
	 * Gets the list，return null if the name not presented
	 *
	 * @param name the name
	 * @return the list, or null if not exists
	 */
	@SuppressWarnings({ "unchecked" })
	public List<JSON> getList(String name) {
		Object o = this.get(name);
		if (o != null && o instanceof List) {
			return (List<JSON>) o;
		}
		return null;
	}

	/**
	 * Gets the objects，return null if the name not presented
	 *
	 * @param name the name
	 * @return the objects, or null if not exists
	 */
	@SuppressWarnings({ "rawtypes" })
	public List<?> getObjects(String name) {
		Object o = this.get(name);
		if (o != null && o instanceof List) {
			return (List) o;
		}
		return null;
	}

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 */
	public static void main(String[] args) {
		String ss = "{a:'a',b:1}";
		JSON j = JSON.fromObject(ss);
		System.out.println(j);

		ss = "{a:'a',b:1, c:{a:1, b:'a'}}";
		j = JSON.fromObject(ss);
		j.remove("a");

		System.out.println(j);

		System.out.println(j.get("b").getClass());
		ss = "[{a:'a',b:1}]";

		List<JSON> l1 = JSON.fromObjects(ss);
		System.out.println(l1);
		System.out.println(l1.get(0).get("b").getClass());

		ss = "{\"list\":['333',1.0,2.0,3.0,5.0,7.0,11.0,13.0,17.0,19.0,23.0,29.0,31.0,37.0,41.0,43.0,47.0,53.0,59.0,61.0,67.0,71.0,73.0,79.0,83.0,89.0,97.0]}";
		j = JSON.fromObject(ss);
		System.out.println(j.getObjects("list").iterator().next().getClass());
		System.out.println(j.getObjects("list").iterator().next().getClass());

		ss = "{a:1}";
		System.out.println(JSON.fromObjects(ss));

		String code = "d.test([{a:1, b:[{a:2}, {b:2}]}, {c:[1,2,3]}])";
		JSON d = JSON.create();
		try {
			JS.run(code, JSON.create().append("d", d));
		} catch (Exception e) {
			e.printStackTrace();
		}

		String s = "n=1&filter=b='2009087010230000'";
		JSON j1 = JSON.fromObject(s);

		System.out.println(j1);

		W q = W.create();
		String s1 = j1.getString("filter");
		System.out.println(s1);

		q.and(s1);

		q = W.create();
		q.and(" a='or' and b != 'and' AND c='java'");

		System.out.println(q.toString());

		j1 = JSON.create();
		j1.append("ret.aaa", 1);
		System.out.println(j1.toPrettyString());

		System.out.println("ret.aaa=" + j1.get("ret.aaa"));

		System.out.println("ret1.aaa=" + j1.get("ret1.aaa"));

		j1.append("a", 11);
		String js = "print('j1.a=' + j1.ret.aaa);j1.b = 'aaaa';";
		try {

			JS.run(js, JSON.create().append("j1", j1));
			Object o = j1.get("b");
			JSON j2 = JSON.fromObject(o);
			System.out.println(j2);
			System.out.println(o);

			j1.scan((p, e) -> {
				System.out.println(e.getKey() + "=" + e.getValue());
			});
		} catch (Exception e) {
			e.printStackTrace();
		}

		j1 = JSON.create();
		j1.append("a", 1);
		j1.append("x.b", 2);
		s = "$a a$$x.b aa";
		System.out.println("s=" + s);
		s = j1.parse(s);
		System.out.println("s=" + s);

		j1 = JSON.create();
		j1.append("ret.a", new ArrayList<String>());
		System.out.println(j1.toPrettyString());
		JSON j2 = j1.copy();
		j1.append("ret.a", "1");
		j1.append("ret.a", "2");
		System.out.println(j2.toPrettyString());
		System.out.println(j1.toPrettyString());

	}

	public void test(Object o) {
//		System.out.println(o.getClass());
//		jdk.nashorn.api.scripting.ScriptObjectMirror m = (jdk.nashorn.api.scripting.ScriptObjectMirror) o;
//		System.out.println(m.isArray());
//		System.out.println(m.get("0").getClass());

		System.out.println(JSON.fromObjects(o));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JSON merge(String name, Object value) {
		if (value == null)
			return this;

		Object o = this.get(name);
		if (o == null) {
			return this.append(name, value);
		}

		if (o instanceof Map && value instanceof Map) {

			((Map) o).putAll((Map) value);

		} else if (o instanceof List) {

			List l1 = (List) o;

			if (value instanceof List) {
				for (Object o1 : (List) value) {
					if (!l1.contains(o1)) {
						l1.add(o1);
					}
				}
			} else if (!l1.contains(value)) {
				l1.add(value);
			}

		} else if (value instanceof List) {
			List l1 = (List) value;
			if (!l1.contains(o)) {
				l1.add(o);
			}
			this.append(name, l1);
		} else {
			// replace the old value
			this.append(name, value);
		}

		return this;
	}

	/**
	 * put the value, the different with "put" is it will split the key by "."
	 * 
	 * @param name  the name
	 * @param value the value
	 * @return the JSON
	 */
	public JSON append(String name, Object value) {
		int i = name.indexOf(".");
		if (i > 0) {
			String s1 = name.substring(0, i);
			JSON j1 = (JSON) this.get(s1);
			if (j1 == null) {
				j1 = JSON.create();
				this.put(s1, j1);
			}
			j1.append(name.substring(i + 1), value);
		} else {
			if (value == null) {
				this.remove(name);
			} else {
				put(name, value);
			}
		}
		return this;
	}

	/**
	 * copy this json, and return a new one
	 * 
	 * @return
	 */
	public JSON copy(String... names) {
		JSON j = JSON.create();
		if (names == null || names.length == 0) {
			for (String s : this.keySet()) {
				Object o = this.get(s);
				if (o instanceof JSON) {
					o = ((JSON) o).copy();
				}
				j.put(s, o);
			}
		} else {
			for (String s : names) {
				Object o = this.get(s);
				if (o instanceof JSON) {
					o = ((JSON) o).copy();
				}
				j.put(s, o);
			}
		}
		return j;
	}

	/**
	 * copy the data in map to this json
	 * 
	 * @param m
	 * @param name
	 * @return
	 */
	public JSON copy(Map<String, Object> m, String... name) {
		if (m == null)
			return this;

		if (name == null || name.length == 0) {
			for (String s : m.keySet()) {
				this.append(s, m.get(s));
			}
		} else {
			for (String s : name) {
				this.append(s, m.get(s));
			}
		}
		return this;
	}

	/**
	 * merge to JSON, if both has number for A key, the add them as one
	 * 
	 * @param jo
	 * @return
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JSON merge(JSON jo) {
		if (jo != null && !jo.isEmpty()) {
			for (String k : jo.keySet()) {
				Object v2 = jo.get(k);
				if (!this.containsKey(k)) {
					this.put(k, v2);
				} else {
					Object v1 = this.get(k);
					if (X.isSame(v1, v2))
						continue;

					List<Object> l1 = new ArrayList<Object>();
					if (v1 instanceof List) {
						l1.addAll((List) v1);
					} else if (v1.getClass().isArray()) {
						l1.addAll(Arrays.asList(v1));
					} else {
						l1.add(v1);
					}
					if (v2 instanceof List) {
						List l2 = (List) v2;
						for (Object o2 : l2) {
							if (!l1.contains(o2)) {
								l1.add(o2);
							}
						}
					} else if (v2.getClass().isArray()) {
						List l2 = Arrays.asList(v2);
						for (Object o2 : l2) {
							if (!l1.contains(o2)) {
								l1.add(o2);
							}
						}
					} else {
						l1.add(v2);
					}
					this.put(k, l1);
				}
			}
		}
		return this;
	}

	@Override
	public boolean remove(Object key1, Object key2) {
		remove(new String[] { key1.toString(), key2.toString() });
		return true;
	}

	@Override
	public Object remove(Object key) {
		Object o = this.get(key);
		remove(new String[] { key.toString() });
		return o;
	}

	public JSON remove(String... names) {
		if (names != null && names.length > 0) {
			for (String name : names) {
				if (name.indexOf("*") > -1) {
					String[] ss = this.keySet().toArray(new String[this.size()]);
					for (String k : ss) {
						if (k.matches(name)) {
							super.remove(k);
						}
					}
				} else {
					super.remove(name);
				}
			}
		}

		return this;
	}

	@Override
	public Object get(Object key) {
//		if (X.isEmpty(key))
//			return null;

		String name = key.toString();

		Object o = super.get(key);
		if (o != null) {
			return o;
		}

		int i = name.indexOf(".");
		if (i > 0) {
			String s0 = name.substring(0, i);
			o = get(s0);
			if (o instanceof JSON) {
				JSON m = (JSON) o;
				return m.get(name.substring(i + 1));
			}
		}

//		i = name.indexOf("[");
//		if (i > 0) {
//			String s0 = name.substring(0, i);
//			o = get(s0);
//			if (o != null) {
//				int j = name.indexOf("]", i + 1);
//				if (j > 0) {
//					String filter = name.substring(i + 1, j);
//					StringFinder sf = StringFinder.create(filter);
//					String na1 = sf.get(">|=|<");
//					String op = null;
//					
//					char c1 = sf.next();
//					
//				}
//			}
//
//		}

		return null;
	}

	public JSON json(String name) {
		JSON j = (JSON) get(name);
		if (j == null) {
			j = JSON.create();
			append(name, j);
		}
		return j;
	}

	public String toXml(String encoding) {
		StringBuilder sb = new StringBuilder();
		if (!X.isEmpty(encoding)) {
			sb.append("<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>").append("\r\n");
		}

		for (String name : this.keySet()) {
			Object o = this.get(name);
			_toxml(sb, name, o);
		}

		return sb.toString();
	}

	@SuppressWarnings("rawtypes")
	private void _toxml(StringBuilder sb, String name, Object o) {
		if (o instanceof JSON) {
			sb.append("<").append(name).append(">");
			sb.append(((JSON) o).toXml(null));
			sb.append("</").append(name).append(">");
		} else if (o instanceof List) {
			List l1 = (List) o;
			for (int i = 0; i < l1.size(); i++) {
				Object o1 = l1.get(i);
				sb.append("<").append(name).append(" id='").append(i).append("'>");

				if (o1 instanceof JSON) {
					sb.append(((JSON) o1).toXml(null));
				} else {
					sb.append(o1);
				}
				sb.append("</").append(name).append(">");
			}
		} else {
			sb.append("<").append(name).append(">");
			sb.append(o);
			sb.append("</").append(name).append(">");
		}
	}

	public static JSON fromXml(String xml) {

		try {

			SAXReader reader = new SAXReader();
			Document document = reader.read(new StringReader(xml));
			Element r1 = document.getRootElement();

			return JSON.create().append(r1.getName(), _fromXml(r1));

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static JSON _fromXml(Element r1) {

		List<Element> l1 = r1.elements();
		if (l1 == null || l1.isEmpty()) {
			JSON j1 = JSON.create();
			for (int i = 0; i < r1.attributeCount(); i++) {
				Attribute a = r1.attribute(i);
				j1.append(a.getName(), a.getValue());
			}
			j1.append("body", r1.getText());
			return j1;
		} else {
			JSON jo = JSON.create();
			for (Element e : l1) {

				for (int i = 0; i < e.attributeCount(); i++) {
					Attribute a = e.attribute(i);
					jo.append(a.getName(), a.getValue());
				}

				String name = e.getName();
				Object o = jo.get(name);
				if (o == null) {
					// add it
					jo.append(name, _fromXml(e));
				} else {
					// add to list
					Object o1 = _fromXml(e);
					if (o instanceof List) {
						((List) o).add(o1);
					} else {
						List<Object> l2 = new ArrayList<Object>();
						l2.add(o);
						l2.add(o1);
						jo.append(name, l2);
					}
				}
			}
			return jo;
		}

	}

	public JSON merge(JSON source, String sourcename, String destname) {
		return merge(source, sourcename, destname, null);
	}

	public JSON merge(JSON source, String sourcename, String destname, String type) {
		if (X.isSame(type, "int")) {
			this.append(destname, X.toInt(source.get(sourcename)));
		} else if (X.isSame(type, "long")) {
			this.append(destname, X.toLong(source.get(sourcename)));
		} else if (X.isSame(type, "double")) {
			this.append(destname, X.toDouble(source.get(sourcename)));
		} else {
			this.append(destname, source.get(sourcename));
		}
		return this;
	}

	/**
	 * create a json, and append
	 * 
	 * @param name
	 * @return
	 */
	public JSON create(String name) {
		JSON j = JSON.create();
		this.append(name, j);
		return j;
	}

	/**
	 * create a list and append
	 * 
	 * @param name
	 * @return
	 */
	public List<JSON> createList(String name) {
		List<JSON> l1 = JSON.createList();
		this.append(name, l1);
		return l1;
	}

	@SuppressWarnings({ "rawtypes" })
	private void _scan_list(Object o, BiConsumer<JSON, Entry> func) {
		X.asList(o, e -> {
			if (e instanceof JSON) {
				((JSON) e).scan(func);
			} else if (e instanceof Collection || e.getClass().isArray()) {
				_scan_list(e, func);
			}
			return null;
		});
	}

	@SuppressWarnings("rawtypes")
	public JSON scan(BiConsumer<JSON, Entry> func) {

		Entry[] ee = this.entrySet().toArray(new Entry[this.size()]);
		for (Entry e : ee) {
			func.accept(this, e);

			if (e.getValue() instanceof JSON) {
				((JSON) e.getValue()).scan(func);
			} else if (e.getValue() instanceof List || e.getValue().getClass().isArray()) {
				_scan_list(e.getValue(), func);
			}
		}
		return this;
	}

	@SuppressWarnings("restriction")
	public JSON scan(jdk.nashorn.api.scripting.ScriptObjectMirror m) {
		return this.scan((p, e) -> {
			m.call(p, e);
		});
	}

	public JSON json() {
		return this;
	}

	/**
	 * @deprecated
	 */
	transient JSON _test;

	/**
	 * @deprecated
	 * @param name
	 * @param display
	 * @return
	 */
	public Object test(String name, String display) {
		return test(name, display, null, null);
	}

	/**
	 * @deprecated
	 * @param name
	 * @param display
	 * @param defaultvalue
	 * @return
	 */
	public Object test(String name, String display, Object defaultvalue) {
		return test(name, display, defaultvalue, null);
	}

	/**
	 * @deprecated
	 * @param name
	 * @param display
	 * @param defaultvalue
	 * @param options
	 * @return
	 */
	public Object test(String name, String display, Object defaultvalue, String options) {

		if (_test == null)
			_test = JSON.create();

		_test.put(name, JSON.create().append("name", name).append("display", display).append("value", defaultvalue)
				.append("options", options));

		if (this.containsKey(name)) {
			return this.get(name);
		}
		return defaultvalue;
	}

	/**
	 * @deprecated
	 * @return
	 */
	public List<JSON> test() {
		List<JSON> l1 = JSON.createList();
		if (_test != null) {
			for (Object o : _test.values()) {
				l1.add((JSON) o);
			}
		}
		return l1;
	}

	public String parse(String template) {

		log.debug("template=" + template);
		log.debug("json=" + this.toString());

		StringFinder sf = StringFinder.create(template);
		while (sf.find("$") >= 0) {
			sf.skip(1);
			sf.mark();
			String name = sf.word("[a-zA-Z0-9._]+");
			if (!X.isEmpty(name)) {
				Object o = this.get(name);
				if (o != null) {
					sf.reset();
					sf.skip(-1);
					sf.replace("\\$" + name, o.toString());
				}
			} else {
				sf.reset();
			}
		}
		return sf.toString();
	}

	@Override
	public Object clone() {
		return super.clone();
	}

}
