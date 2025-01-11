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
import java.io.Serializable;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiFunction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.giiwa.dao.Comment;
import org.giiwa.dao.X;
import org.giiwa.engine.JS;
import org.giiwa.misc.Base32;
import org.giiwa.misc.Digest;
import org.giiwa.misc.StringFinder;
import org.giiwa.misc.Url;
import org.giiwa.task.BiConsumer;
import org.giiwa.web.Language;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * The Class JSON, simple JSON object, using Gson to parse and format, <br>
 * and find api by xpath <br>
 * 
 * @author wujun
 */
public final class JSON extends HashMap<String, Object> implements Comparable<JSON>, Cloneable {

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
	 * @param lenient the boolean of JsonReader.setLenient(lenient), <br>
	 *                if json is inputstream/reader, true not close, false to close
	 * @return the json
	 */
	public static JSON fromObject(Object json, boolean lenient) {
		return fromObject(json, lenient, 0);
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static JSON fromObject(Object json, boolean lenient, int times) {

		JSON j = null;
		try {
			if (json == null) {
				return null;
			} else if (json instanceof JSON) {
				j = (JSON) json;
			} else if (json instanceof ScriptObjectMirror) {

				ScriptObjectMirror m = (ScriptObjectMirror) json;

				j = JSON.create();
				for (String key : m.keySet()) {
					j.put(key, m.get(key));
				}

			} else if (json.getClass().getName().equals("org.postgresql.util.PGobject")) {

				Object o = json;
				j = JSON.create().append(o.getClass().getMethod("getType").invoke(o).toString(),
						o.getClass().getMethod("getValue").invoke(o));

			} else if (json instanceof Map) {
				j = JSON.create((Map) json);

			} else if (json instanceof String) {

				String s1 = ((String) json).trim();
				if (!X.isEmpty(s1)) {
					if (s1.charAt(0) == '{') {
						Gson g = _gson();
						JsonReader reader = new JsonReader(new StringReader(s1));
						reader.setLenient(lenient);
						j = g.fromJson(reader, JSON.class);
					} else if (s1.charAt(0) == '<') {
						// <link href='a'>
						StringFinder sf = StringFinder.create(s1.substring(1));
						String tag = sf.nextTo(" |>");
						j = JSON.create();
						j.append(tag, JSON.create());
						char c = sf.next();
						if (c == ' ') {

							String name = sf.nextTo(" |>");
							while (name != null) {
								int i = name.indexOf("=");
								if (i > 0) {
									String value = name.substring(i + 1);
									value = value.replaceAll("['\"]", X.EMPTY);
									j.append(tag + "." + name.substring(0, i), value);
								}
								sf.next();
								name = sf.nextTo(" |>");
							}

						}
					} else {
						// a=b&d=a
						s1 = Url.decode(s1);
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
				}
			} else if (json instanceof InputStream) {
				InputStream in = (InputStream) json;
				try {
					Gson g = _gson();
					j = g.fromJson(new InputStreamReader(in), JSON.class);
				} finally {
					if (!lenient) {
						X.close(in);
					}
				}
			} else if (json instanceof File) {
				Reader re = null;
				try {
					re = new FileReader((File) json);
					Gson g = _gson();
					j = g.fromJson(re, JSON.class);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				} finally {
					X.close(re);
				}
			} else if (json instanceof Reader) {
				Reader re = (Reader) json;
				try {
					Gson g = _gson();
					j = g.fromJson(re, JSON.class);
				} finally {
					if (!lenient) {
						X.close(re);
					}
				}
			} else if (json instanceof byte[]) {

				byte[] b1 = (byte[]) json;
				InputStream in = new ByteArrayInputStream(b1);
				try {
					Gson g = _gson();
					JsonReader reader = new JsonReader(new InputStreamReader(in));
					reader.setLenient(lenient);
					j = g.fromJson(reader, JSON.class);
				} finally {
					X.close(in);
				}
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

			_refine(j, 0);

			if (j != null) {
				Set<String> ss = j.keySet();
				for (String name : ss) {
					Object o = j.get(name);
//					if (o == null) {
//						j.remove(name);
//					} else 
					if (o instanceof ScriptObjectMirror) {
						ScriptObjectMirror m = (ScriptObjectMirror) o;
						if (m.isArray()) {
							j.put(name, JSON.fromObjects(m));
						} else {
							if (times < 64) {
								j.put(name, JSON.fromObject(m, lenient, times + 1));
							}
						}
					} else if (o instanceof List) {
						j.put(name, fromObjects(o));
					} else if (o instanceof Map) {
						if (times < 64) {
							j.put(name, fromObject(o, lenient, times + 1));
						}
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
				.serializeSpecialFloatingPointValues().serializeNulls().create();
	}

	public static boolean isArray(Object jsons) {

		if (jsons instanceof Collection) {
			return true;
		} else if (jsons instanceof String) {
			if (((String) jsons).startsWith("{")) {
				return false;
			} else {
				return true;
			}
		} else if (jsons instanceof ScriptObjectMirror) {
			ScriptObjectMirror m = (ScriptObjectMirror) jsons;
			if (m.isArray()) {
				return true;
			} else {
				return false;
			}
		}

		return false;
	}

	/**
	 * parse the jsons to array of JSON.
	 *
	 * @param jsons the jsons
	 * @return the list
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
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

			InputStream in = (InputStream) jsons;
			try {
				Gson g = _gson();
				JsonReader reader = new JsonReader(new InputStreamReader(in));
				list = g.fromJson(reader, List.class);
			} finally {
				X.close(in);
			}
		} else if (jsons instanceof File) {

			Reader re = null;
			try {
				re = new FileReader((File) jsons);
				Gson g = _gson();
				return g.fromJson(re, List.class);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				X.close(re);
			}
		} else if (jsons instanceof Reader) {

			Reader re = (Reader) jsons;
			try {
				Gson g = _gson();
				list = g.fromJson(re, List.class);
			} finally {
				X.close(re);
			}
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
		} else if (jsons instanceof ScriptObjectMirror) {
			ScriptObjectMirror m = (ScriptObjectMirror) jsons;
			list = JSON.createList();
			if (m.isArray()) {
//				JSON j1 = JSON.fromObject(m);
				for (Object o : m.values()) {
					if (o instanceof ScriptObjectMirror) {
						ScriptObjectMirror m1 = (ScriptObjectMirror) o;
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
				} else if (o instanceof Map) {
					list.set(i, fromObject(o));
				} else if (o instanceof String) {
					try {
						String s = (String) o;
						if (s.startsWith("{")) {
							list.set(i, fromObject(s));
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
				T t1 = t.getDeclaredConstructor().newInstance();
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
	public synchronized String toString() {
		Gson g = _gson();
		return g.toJson(this);
	}

	public static String toString(List<JSON> l1) {
		Gson g = _gson();
		return g.toJson(l1);
	}

	public static String toPrettyString(List<JSON> l1) {
		Gson gson = new GsonBuilder().registerTypeAdapterFactory(BeanAdapter.FACTORY)
				.excludeFieldsWithoutExposeAnnotation().serializeSpecialFloatingPointValues().setPrettyPrinting()
				.create();
		return gson.toJson(l1);
	}

	/**
	 * convert the json to a url string
	 * 
	 * @return url string
	 */
	public String toUrl() {
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

	public synchronized String toPrettyString() {
		Gson gson = new GsonBuilder().registerTypeAdapterFactory(BeanAdapter.FACTORY)
				.excludeFieldsWithoutExposeAnnotation().serializeSpecialFloatingPointValues().setPrettyPrinting()
				.serializeNulls().create();
		return gson.toJson(this);
	}

	@SuppressWarnings("deprecation")
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

	@SuppressWarnings("deprecation")
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
	private static void _refine(Map<String, Object> jo, int times) {

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
				} else if (v instanceof JSON) {
					if (times < 64) {
						_refine((JSON) v, times + 1);
					}
//				} else if (v instanceof Map) {
//					v = JSON.fromObject(v);
//					_refine((JSON) v);
//					jo.put(name, v);
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
				} else if (v instanceof JSON) {
					_refine((JSON) v, 0);
				} else if (v instanceof Map) {
					v = JSON.fromObject(v);
					_refine((Map) v, 0);
					l1.set(i, v);
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
		Set<String> m1 = m.keySet();
		for (String name : m1) {
			Object o = m.get(name);
			j.put(name, o);
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
	 * get and format
	 * 
	 * @param name
	 * @param format
	 * @return
	 */
	public Object get(String name, String format) {

		Object v = get(name);
		if (X.isEmpty(format)) {
			return v;
		}

		if (format.matches(".*(yyyy|MM|dd|HH|mm|ss).*")) {
			// 时间日期格式
			return Language.getLanguage().format(v, format);
		}

		return String.format(format, v);

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

	public void test(Object o) {
//		jdk.nashorn.api.scripting.ScriptObjectMirror m = (jdk.nashorn.api.scripting.ScriptObjectMirror) o;

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

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JSON merge2(String name, Object value) {
		if (value == null)
			return this;

		Object o = this.get(name);
		if (o == null) {
			return this.append(name, value);
		}

		if (o instanceof List) {

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
			List l1 = new ArrayList();
			l1.add(o);
			l1.add(value);
			this.append(name, l1);
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
		if (X.isEmpty(name))
			return this;

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
//			if (value == null) {
//				this.remove(name);
//			} else {
			put(name, value);
//			}
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
			Set<String> m1 = this.keySet();
			for (String s : m1) {
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
			Set<String> m1 = m.keySet();
			for (String s : m1) {
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

			Set<String> m1 = jo.keySet();

			for (String k : m1) {
				Object v2 = jo.get(k);
				if (!this.containsKey(k)) {
					this.put(k, v2);
				} else {
					Object v1 = this.get(k);
					if (X.isSame(v1, v2))
						continue;

					if (v1 instanceof Map && v2 instanceof Map) {
						((Map) v1).putAll((Map) v2);
					} else {
						v1 = v2;
					}

					this.put(k, v1);
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
							synchronized (this) {
								super.remove(k);
							}
						}
					}
				} else {
					synchronized (this) {
						super.remove(name);
					}
				}
			}
		}

		return this;
	}

	@Override
	public synchronized Object get(Object key) {

		Object o = super.get(key);
		if (o != null) {
			return o;
		}

		String name = key.toString();

		int i = name.indexOf(".");
		if (i > 0) {
			String s0 = name.substring(0, i);
			o = get(s0);
			if (o instanceof JSON) {
				JSON m = (JSON) o;
				return m.get(name.substring(i + 1));
			} else if (o instanceof Map) {
				JSON m = JSON.fromObject(o);
				return m.get(name.substring(i + 1));
			}
		}

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
//			e.printStackTrace();
		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object _fromXml(Element r1) {

		JSON jo = JSON.create();
		for (int i = 0; i < r1.attributeCount(); i++) {
			Attribute a = r1.attribute(i);
			jo.append(a.getName(), a.getValue());
		}

		List<Element> l1 = r1.elements();
		if (l1 != null && !l1.isEmpty()) {
			for (Element e : l1) {

				Object j1 = _fromXml(e);
				Object o = jo.get(e.getName());
				if (o == null) {
					List l2 = new ArrayList();
					l2.add(j1);
					jo.put(e.getName(), l2);
				} else {
					if (o instanceof List) {
						((List) o).add(j1);
					} else {
						List l2 = new ArrayList<Object>();
						l2.add(o);
						l2.add(j1);
						jo.put(e.getName(), l2);
					}
				}
			}
		} else {
			String s = r1.getText();
			if (!X.isEmpty(s)) {
				jo.put("body", s);
			}
		}

		return jo;

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

	private void _scan_list(Object o, BiConsumer<JSON, String> func) {
		X.asList(o, e -> {
			if (e == null)
				return null;

			if (e instanceof JSON) {
				((JSON) e).scan(func);
			} else if (X.isArray(e)) {
				_scan_list(e, func);
			}
			return null;
		});
	}

	public JSON scan(BiConsumer<JSON, String> func) {

		String[] ss = this.keySet().toArray(new String[this.size()]);
		for (String s : ss) {

			func.accept(this, s);

			Object o = this.get(s);
			if (o != null) {
				if (o instanceof JSON) {
					((JSON) o).scan(func);
				} else if (X.isArray(o)) {
					_scan_list(o, func);
				}
			}
		}
		return this;
	}

	public JSON scan(ScriptObjectMirror m) {
		return this.scan((p, e) -> {
			m.call(p, e);
		});
	}

	public JSON json() {
		return this;
	}

	public String parse(String template) {

		if (log.isDebugEnabled()) {
			log.debug("template=" + template);
			log.debug("json=" + this.toString());
		}

		StringFinder sf = StringFinder.create(template);
		while (sf.find("$") >= 0) {
			int pos = sf.pos;
			sf.skip(1);
			sf.mark();
			String name = sf.word("[a-zA-Z0-9_]+");
			if (!X.isEmpty(name)) {

				Object o = this.get(name);
				if (o != null) {

					String s = sf.word("[.a-zA-Z0-9_]+");
					while (X.isIn(s, ".split", ".replace", ".join")) {

						String s1 = sf.get("(", ")");

						if (X.isIn(s, ".split")) {
							o = X.split(o.toString(), s1.trim());
						} else if (X.isIn(s, ".join")) {
							o = X.join(o, s1);
						} else if (X.isIn(s, ".replace")) {
							String[] ss = X.split(s1, "->");
							if (ss != null && ss.length == 2) {
								o = X.asList(o, s2 -> {
									try {
										return JS.run("return " + ss[1] + ";", JSON.create().append(ss[0], s2));
									} catch (Exception e) {
										log.error(e.getMessage(), e);
									}
									return s2;
								});
							}
						}

						s = sf.word("[.a-zA-Z0-9_]+");
					}
					if (s.startsWith(".")) {
						Object o1 = this.get(name + s);
						if (o1 != null) {
							o = o1;
						}
					}
//					System.out.println(s);

					int end = sf.pos;
					if (o == null) {
						o = X.EMPTY;
					}
					sf.replace(pos, end, o.toString());
				}
			} else {
				sf.reset();
			}
		}
		return sf.toString();
	}

	@Override
	public synchronized Object clone() {
		JSON j1 = JSON.create();
		j1.putAll(this);
		return j1;
	}

	@Override
	public synchronized Object put(String key, Object value) {
		return super.put(key, value);
	}

	@Override
	public synchronized void putAll(Map<? extends String, ? extends Object> m) {
		synchronized (m) {
			super.putAll(m);
		}
	}

	@Override
	public synchronized Set<String> keySet() {
		return new HashSet<String>(super.keySet());
	}

	@Override
	public synchronized Collection<Object> values() {
		return new ArrayList<Object>(super.values());
	}

	@Override
	public synchronized boolean replace(String key, Object oldValue, Object newValue) {
		return super.replace(key, oldValue, newValue);
	}

	@Override
	public synchronized Object replace(String key, Object value) {
		return super.replace(key, value);
	}

	@Override
	public synchronized void replaceAll(BiFunction<? super String, ? super Object, ? extends Object> function) {
		super.replaceAll(function);
	}

	/**
	 * returne treemap of this
	 * 
	 * @return
	 */
	public TreeMap<String, Object> treemap() {
		TreeMap<String, Object> m = new TreeMap<String, Object>();
		m.putAll(this);
		return m;
	}

//	public byte[] getBytes(String name) {
//		String s = this.getString(name);
//		if (s != null) {
//			return Base64.getDecoder().decode(s);
//		}
//		return null;
//	}

//	public void put(String name, byte[] data) {
//		if (data == null) {
//			this.put(name, (Object) null);
//		} else {
//			this.put(name, new String(Base64.getEncoder().encode(data)));
//		}
//	}

//	public JSON append(String name, byte[] data) {
//		this.put(name, data);
//		return this;
//	}

	@Override
	public int compareTo(JSON o) {
		if (this == o) {
			return 0;
		}

		if (o == null) {
			return 1;
		}

		Set<String> k1 = this.keySet();
		Set<String> k2 = o.keySet();

		if (k1.containsAll(k2)) {
			if (k2.containsAll(k1)) {
				for (String name : k1) {
					Object o1 = this.get(name);
					Object o2 = o.get(name);
					int e = X.compareTo(o1, o2);
					if (e != 0) {
						return e;
					}
				}
			} else {
				return 1;
			}
		}

		if (k2.containsAll(k1)) {
			return -1;
		}

		return 0;
	}

	@Comment(text = "键转大写")
	@SuppressWarnings("rawtypes")
	public JSON uppercase() {
		JSON j1 = JSON.create();
		for (String name : this.keySet()) {
			Object o = this.get(name);
			if (o instanceof Map) {
				o = _uppercase((Map) o);
			}
			j1.put(name.toUpperCase(), o);
		}
		return j1;
	}

	@SuppressWarnings("rawtypes")
	private Object _uppercase(Map o) {
		JSON j1 = JSON.create();
		for (Object name : o.keySet()) {
			Object o1 = this.get(name);
			if (o1 instanceof Map) {
				o1 = _uppercase((Map) o1);
			}
			j1.put(name.toString().toUpperCase(), o1);
		}
		return j1;
	}

	@SuppressWarnings("rawtypes")
	@Comment(text = "键转小写")
	public JSON lowercase() {
		JSON j1 = JSON.create();
		for (String name : this.keySet()) {
			Object o = this.get(name);
			if (o instanceof Map) {
				o = _lowercase((Map) o);
			}
			j1.put(name.toLowerCase(), o);
		}
		return j1;
	}

	@SuppressWarnings("rawtypes")
	private Object _lowercase(Map o) {
		JSON j1 = JSON.create();
		for (Object name : o.keySet()) {
			Object o1 = this.get(name);
			if (o1 instanceof Map) {
				o1 = _lowercase((Map) o1);
			}
			j1.put(name.toString().toLowerCase(), o1);
		}
		return j1;
	}

	@Comment(text = "修改键名")
	public void move(@Comment(text = "from") String from, @Comment(text = "to") String to) {
		this.append(to, this.get(from));
		this.remove(from);
	}

	@Comment(text = "转换为可序列化的JSON对象")
	public JSON serializable() {
		JSON j1 = JSON.create();
		for (Entry<String, Object> e : this.entrySet()) {
			Object v = _serialize(e.getValue());
			if (v != null) {
				j1.put(e.getKey(), v);
			}
		}
		return j1;
	}

	@SuppressWarnings("rawtypes")
	private Object _serialize(Object v) {
		if (X.isArray(v)) {
			List l1 = X.asList(v, s -> s);
			for (int i = l1.size() - 1; i >= 0; i--) {
				Object v1 = _serialize(l1.get(i));
				if (v1 == null) {
					l1.remove(i);
				}
			}
			return l1;
		} else if (v instanceof Map) {
			JSON j1 = JSON.fromObject(v);
			JSON j2 = JSON.create();
			for (Entry<String, Object> e : j1.entrySet()) {
				Object v1 = _serialize(e.getValue());
				if (v1 != null) {
					j2.put(e.getKey(), v1);
				}
			}
			return j2;
		} else if (v instanceof Serializable) {
			return v;
		}
		return null;
	}

}
