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
package org.giiwa.core.json;

import java.io.ByteArrayInputStream;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.giiwa.core.base.Base32;
import org.giiwa.core.base.Digest;
import org.giiwa.core.bean.X;
import org.giiwa.core.dle.JS;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import com.jayway.jsonpath.JsonPath;

/**
 * The Class JSON, simple JSON object, using Gson to parse and format, <br>
 * and find api by xpath <br>
 * 
 * @author wujun
 */
public final class JSON extends LinkedHashMap<String, Object> {

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
				Gson g = new Gson();
				JsonReader reader = new JsonReader(new StringReader((String) json));
				reader.setLenient(lenient);
				j = g.fromJson(reader, JSON.class);
			} else if (json instanceof InputStream) {
				Gson g = new Gson();
				j = g.fromJson(new InputStreamReader((InputStream) json), JSON.class);
			} else if (json instanceof Reader) {
				Gson g = new Gson();
				j = g.fromJson((Reader) json, JSON.class);
			} else if (json instanceof byte[]) {
				Gson g = new Gson();
				byte[] b1 = (byte[]) json;
				JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(b1)));
				reader.setLenient(lenient);
				j = g.fromJson(reader, JSON.class);
			} else if (json instanceof ResultSet) {

				ResultSet r = (ResultSet) json;
				ResultSetMetaData rmd = r.getMetaData();
				j = JSON.create();
				for (int i = 0; i < rmd.getColumnCount(); i++) {
					j.append(rmd.getColumnName(i + 1), r.getObject(i + 1));
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
			Gson g = new Gson();
			if (((String) jsons).startsWith("{")) {
				list = JSON.createList();
				list.add(JSON.fromObject(jsons));
			} else {
				list = g.fromJson((String) jsons, List.class);
			}
		} else if (jsons instanceof InputStream) {
			Gson g = new Gson();
			JsonReader reader = new JsonReader(new InputStreamReader((InputStream) jsons));
			list = g.fromJson(reader, List.class);
		} else if (jsons instanceof Reader) {
			Gson g = new Gson();
			list = g.fromJson((Reader) jsons, List.class);
		} else if (jsons instanceof byte[]) {
			Gson g = new Gson();
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
			Gson g = new Gson();
			return g.fromJson((String) json, t);
		} else if (json instanceof Reader) {
			Gson g = new Gson();
			return g.fromJson((Reader) json, t);
		} else if (json instanceof byte[]) {
			Gson g = new Gson();
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
		Gson g = new Gson();
		return g.toJson(this);
	}

	public String toPrettyString() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
				} else if (v instanceof JSON) {
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
		j.putAll(m);
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
		System.out.println(j);
		System.out.println("$.c=" + j.find("$.c"));
		System.out.println(j.set("$.c", 2));

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
//		System.out.println(d);

	}

	public void test(Object o) {
//		System.out.println(o.getClass());
//		jdk.nashorn.api.scripting.ScriptObjectMirror m = (jdk.nashorn.api.scripting.ScriptObjectMirror) o;
//		System.out.println(m.isArray());
//		System.out.println(m.get("0").getClass());

		System.out.println(JSON.fromObjects(o));
	}

	/**
	 * find the object by the xpath
	 * 
	 * @param <T>   the object
	 * @param xpath the xpath expressions can use the dot–notation
	 * 
	 *              <pre>
	$.store.book[0].title
	or the bracket–notation
	$['store']['book'][0]['title']
	 *              </pre>
	 * 
	 * @return the object, or null if not exists
	 */
	public <T> T find(String xpath) {
		return find(this, xpath);
	}

	/**
	 * find the object by the xpath in json
	 * 
	 * @param <T>   the object returned
	 * @param json  the json object
	 * @param xpath the xpath expressions can use the dot–notation
	 * 
	 *              <pre>
	$.store.book[0].title
	or the bracket–notation
	$['store']['book'][0]['title']
	 *              </pre>
	 * 
	 * @return the object, or null if not exists
	 */
	public static <T> T find(Object json, String xpath) {
		return JsonPath.parse(json).read(xpath);
	}

	/**
	 * set the value by xpath
	 * 
	 * @param xpath the xpath expressions can use the dot–notation
	 * 
	 *              <pre>
	$.store.book[0].title
	or the bracket–notation
	$['store']['book'][0]['title']
	 *              </pre>
	 * 
	 * @param value the object
	 * @return JSON the new JSON object
	 */
	public JSON set(String xpath, Object value) {
		set(this, xpath, value);
		return this;
	}

	/**
	 * put the value
	 * 
	 * @param name  the name
	 * @param value the value
	 * @return the JSON
	 */
	public JSON append(String name, Object value) {
		put(name, value);
		return this;
	}

	/**
	 * copy this json, and return a new one
	 * 
	 * @return
	 */
	public JSON copy() {
		JSON j = JSON.create();
		for (String s : this.keySet()) {
			j.put(s, this.get(s));
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
		if (m == null || name == null || name.length == 0)
			return this;

		for (String s : name) {
			this.append(s, m.get(s));
		}
		return this;
	}

	/**
	 * set the value by xpath
	 * 
	 * @param json  the source json object
	 * @param xpath the xpath string <br>
	 *              xpath expressions can use the dot–notation
	 * 
	 *              <pre>
	$.store.book[0].title
	or the bracket–notation
	$['store']['book'][0]['title']
	 *              </pre>
	 * 
	 * @param value the object
	 */
	public static void set(Object json, String xpath, Object value) {
		JsonPath.parse(json).set(xpath, value);
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

	public JSON remove(String... name) {
		if (name != null && name.length > 0) {
			for (String s : name) {
				remove(s);
			}
		}
		return this;
	}

	@Override
	public Object get(Object key) {
		if (X.isEmpty(key))
			return null;

		String name = key.toString();

		if (this.containsKey(name)) {
			return super.get(key);
		}

		int i = name.indexOf(".");
		if (i > 0) {
			String s0 = name.substring(0, i);
			Object o = this.get(s0);
			if (o instanceof JSON) {
				JSON m = (JSON) o;
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
		}

		return null;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static Object _fromXml(Element r1) {

		List<Element> l1 = r1.elements();
		if (l1 == null || l1.isEmpty()) {
			return r1.getText();
		} else {
			JSON jo = JSON.create();
			for (Element e : l1) {
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

}
