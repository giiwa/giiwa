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
package org.giiwa.dao;

import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.BSONObject;
import org.bson.Document;
import org.giiwa.dao.Helper.V;
import org.giiwa.json.JSON;
import org.giiwa.misc.Digest;
import org.giiwa.web.Language;
import org.openjdk.nashorn.api.scripting.ScriptObjectMirror;

/**
 * The {@code Bean} Class is entity class that mapping to a table,<br>
 * work with {@code Helper}, you can load/update/delete data from DB(RDS/Mongo),
 * almost includes all methods that need for database <br>
 * 
 */
public class Bean implements Map<String, Object>, Serializable, Cloneable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4L;

	/** The log utility */
	private static Log log = LogFactory.getLog(Bean.class);

	@Column(no = true)
	private Object _id;

	@Column(no = true)
	private boolean _readonly = false;

	public boolean isReadonly() {
		return _readonly;
	}

	/**
	 * set the bean as read only mode
	 */
	public void readonly() {
		_readonly = true;
	}

	/**
	 * the row number
	 * 
	 */
	@Column(no = true)
	public long _rowid;

	@Column(memo = "更新时间")
	private long updated;

	@Column(memo = "创建时间")
	private long created;

	/**
	 * get the created timestamp of the data
	 * 
	 * @return long of the created
	 */
	public long getCreated() {
		return created;
	}

	/**
	 * get the updated timestamp of the data
	 * 
	 * @return long the of the updated
	 */
	public long getUpdated() {
		return updated;
	}

	/**
	 * 
	 * @deprecated
	 * 
	 *             replace by from(JSON jo)
	 * 
	 * @param jo
	 * @return
	 */
	public boolean fromJSON(JSON jo) {
		return from(jo);

	}

	/**
	 * refill the bean from json.
	 *
	 * @param jo the JSON object
	 * @return true if all successful
	 */
	public boolean from(JSON jo) {

		if (_readonly)
			return false;

		for (String name : jo.keySet()) {
			set(name, jo.get(name));
		}
		return true;
	}

	/**
	 * get the key-value in the bean to json.<br>
	 * 
	 * @deprecated
	 * 
	 *             replace by json()
	 * @param jo the JSON object
	 */
	public void toJSON(JSON jo) {
		/**
		 * get the extra data, and putall in json
		 */
		jo.putAll(getAll());

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object.toString()
	 */
	public String toString() {
		return this.getClass().getSimpleName() + "@{ID=" + this.get(X.ID) + "}";
	}

	/**
	 * set the value to extra data, or the field annotation by @Column.
	 *
	 * @param name  the name of the data or the column
	 * @param value the value, if the value=null, then remove the name from the data
	 * @return Object of the old value
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public final Object set(String name, Object value) {

		if (_readonly) {
			return null;
		}

		if (value == V.ignore) {
//		if (value == V.ignore || value == null) {
			return null;
		}

		Object old = null;

		// change all to lower case avoid some database auto change to upper case
		name = name.toLowerCase();
//		log.warn("field=" + name + ", value=" + value.getClass());

		if (value != null) {
			if (value instanceof java.sql.Clob) {
				try {
					Clob c = (Clob) value;
					Reader re = c.getCharacterStream();
					char[] cc = new char[(int) c.length()];
					re.read(cc);
					value = new String(cc);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			} else if (value instanceof java.sql.NClob) {
				try {
					NClob c = (NClob) value;
					Reader re = c.getCharacterStream();
					char[] cc = new char[(int) c.length()];
					re.read(cc);
					value = new String(cc);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			} else if (value instanceof java.sql.Blob) {
				try {
					Blob c = (Blob) value;
					value = c.getBytes(0, (int) c.length());
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			} else if (value instanceof Array) {
				try {
					java.sql.Array c = (Array) value;
					value = X.asList(c.getArray(), s -> s);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			} else if (value instanceof ScriptObjectMirror) {
				// TODO, data.a = []
				// data.a.push 会报错
//			ScriptObjectMirror m = (ScriptObjectMirror) value;
//			if (m.isArray()) {
//				value = X.asList(m, s -> s);
//			} else {
//				value = JSON.fromObject(value);
//			}
			} else if (value instanceof Map) {
				value = JSON.fromObject(value);
			} else if (value.getClass().getName().equals("org.postgresql.util.PGobject")) {
				Object o = value;
				try {
					value = JSON.create().append(o.getClass().getMethod("getType").invoke(o).toString(),
							o.getClass().getMethod("getValue").invoke(o));
				} catch (Exception e) {
					// ignore
				}
			} else if (value instanceof org.bson.BsonTimestamp) {
				org.bson.BsonTimestamp b = (org.bson.BsonTimestamp) value;
				value = b.asDateTime().getValue();
			} else if (value instanceof Date) {
				value = ((Date) value).getTime();
//		} else {
//			log.warn("field=" + name + ", value=" + value.getClass());
			}
		}

		// looking for all the fields
		_F f1 = _getField(name);

		if (f1 != null) {

			try {

//				f1.setAccessible(true);

				// log.debug("f1=" + f1 + ", value=" + value);
				old = f1.get(this);

				Class<?> t1 = f1.getType();
				// log.debug("t1=" + t1 + ", f1.name=" + f1.getName());
				if (t1.equals(value.getClass())) {
					f1.set(this, value);
				} else if (t1 == long.class) {
					f1.set(this, X.toLong(value));
				} else if (t1 == int.class) {
					f1.set(this, X.toInt(value));
				} else if (t1 == double.class) {
					f1.set(this, X.toDouble(value, 0));
				} else if (t1 == float.class) {
					f1.set(this, X.toFloat(value, 0));
				} else if (t1 == String.class) {
//					if (value != null) {
//						value = value.toString();
//					}
					// allow uuid
//					if (List.class.isAssignableFrom(f1.getClass())) {
//						f1.set(this, Arrays.asList(value));
//					} else {
					f1.set(this, value);
//					}
				} else if (List.class.isAssignableFrom(t1) || t1.isArray()) {
					// change the value to list
					if (value != null) {
						List<Object> l1 = new ArrayList<Object>();
						if (value instanceof List) {
							l1.addAll((List<Object>) value);
						} else if (value.getClass().isArray()) {
							l1.add(Arrays.asList(value));
						} else if (value instanceof String) {
							String s = (String) value;
							l1.addAll(X.asList(X.split(s, "[\\[\\],]"), s1 -> s1));
						} else {
							l1.add(value);
						}
						if (t1.isArray()) {
							f1.set(this, l1.toArray());
						} else {
							f1.set(this, l1);
						}
					}
				} else if (Map.class.isAssignableFrom(t1)) {
					// change the value to map
					Map<?, ?> l1 = new HashMap<>();
					if (value instanceof Map) {
						l1.putAll((Map) value);
					}
					f1.set(this, l1);
				} else {
					if (value instanceof Date) {
						f1.set(this, ((Date) value).getTime());
					} else if (value instanceof Timestamp) {
						f1.set(this, ((Timestamp) value).getTime());
					} else if (value instanceof LocalDateTime) {
						Instant d = ((LocalDateTime) value).atZone(ZoneOffset.ofHours(8)).toInstant();
						f1.set(this, d.toEpochMilli());
					} else {
						f1.set(this, value);
					}
				}
			} catch (Exception e) {
				// ignore
//				log.error(name + "=" + value, e);
			}
		} else {

			if (data == null) {
				data = new HashMap<String, Object>();
			}

			old = data.get(name);

			if (value instanceof Date) {
				data.put(name, ((Date) value).getTime());
			} else if (value instanceof Timestamp) {
				data.put(name, ((Timestamp) value).getTime());
			} else if (value instanceof LocalDateTime) {
				java.util.Date d = Date.from(((LocalDateTime) value).atZone(ZoneOffset.ofHours(8)).toInstant());
				data.put(name, d.getTime());
			} else if (value instanceof Number) {
				Number n = (Number) value;
				if (n.toString().indexOf(".") > -1) {
					data.put(name, n.doubleValue());
				} else {
					data.put(name, n.longValue());
				}
			} else {
				data.put(name, value);
			}
		}

//		if (X.isIn(name, "_type", "type") && this.getClass().getSimpleName().equals("Page")) {
//			try {
//				log.warn(name + "=" + value + ", field=" + f1 + ", value="
//						+ ((f1 == null) ? data.get(name) : f1.get(this)), new Exception());
//			} catch (Exception e) {
//				log.error(e.getMessage(), e);
//			}
//		}

		return old;
	}

	/**
	 * get the Field by the colname
	 * 
	 * @param columnname the colname
	 * @return the Field
	 */
	public Field getField(String columnname) {
		_F f = _getField(columnname);
		return f == null ? null : f.f;
	}

	private _F _getField(String columnname) {

		Map<String, _F> m = _getFields();
		return m == null ? null : m.get(columnname);

	}

	public Map<String, Field> getFields() {

		Map<String, _F> m = _getFields();
		Map<String, Field> m1 = new HashMap<String, Field>();
		if (m != null) {
			for (String name : m.keySet()) {
				_F f = m.get(name);
				m1.put(name, f.f);
			}
		}
		return m1;
	}

	private Map<String, _F> _getFields() {

		Class<?> c1 = this.getClass();
		Map<String, _F> m = _fields.get(c1);
		if (m == null) {
			m = new HashMap<String, _F>();

			int i = 0;
			for (; c1 != null;) {
				i++;
				if (log.isDebugEnabled()) {
					log.debug("c1=" + c1);
				}

				Field[] ff = c1.getDeclaredFields();
				for (Field f : ff) {

					Column f1 = f.getAnnotation(Column.class);
					if (f1 != null && f1.no()) {
						continue;
					}

//					if (log.isDebugEnabled())
//						log.debug("f1=" + f1);

					if (f1 != null && !X.isEmpty(f1.name())) {
						f.setAccessible(true);
						String name = f1.name().toLowerCase();

						_F f2 = m.get(name);
						if (f2 == null) {
							f2 = _F.create(f);
							m.put(name, f2);
						} else {
							f2.link(f);
						}

						String name1 = f.getName().toLowerCase();
						if (!X.isSame(name, name1)) {

							f2 = m.get(name1);
							if (f2 == null) {
								f2 = _F.create(f);
								m.put(name1, f2);
							} else {
								f2.link(f);
							}
						}
					} else if ((f.getModifiers() & (Modifier.FINAL | Modifier.STATIC | Modifier.TRANSIENT)) == 0) {
						f.setAccessible(true);
						String name = f.getName().toLowerCase();

						_F f2 = m.get(name);
						if (f2 == null) {
							f2 = _F.create(f);
							m.put(name, f2);
						} else {
							f2.link(f);
						}

					}
				}
//				if (log.isDebugEnabled())
//					log.debug("c1=" + c1);

				if (i > 5) {
					log.error("c1=" + c1);
				}

				c1 = c1.getSuperclass();

			}

			_fields.put(this.getClass(), m);
		}
		return m;
	}

	private final static Map<Class<? extends Bean>, Map<String, _F>> _fields = new HashMap<Class<? extends Bean>, Map<String, _F>>();

	/**
	 * get the value by name from bean <br>
	 * 
	 * @param <T>  the Object
	 * @param name the name of the data or the column
	 * @return Object the value of the name, return null if the name not exists
	 */
	@Override
	public Object get(Object name) {
		if (name == null) {
			return null;
		}

		String s = name.toString().toLowerCase();
		_F f = _getField(s);
		if (f != null) {
			try {
				return f.get(this);
			} catch (Exception e) {
				log.error(name, e);
			}
		}

		if (data == null) {
			return null;
		}

		return data.get(s);

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
	 * get the size of the names.
	 *
	 * @return the int
	 */
	public final int size() {

		int n = 0;
		if (data != null) {
			n += data.size();
		}

		Map<String, _F> m2 = _getFields();
		if (m2 != null) {
			n += m2.size();
		}

		return n;
	}

	/**
	 * test is empty bean
	 * 
	 * @return the boolean, true if empty
	 */
	public final boolean isEmpty() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map.containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {

		if (data != null && data.size() > 0) {
			if (data.containsKey(key)) {
				return true;
			}
		}

		Map<String, _F> m2 = _getFields();
		if (m2 != null) {
			return m2.containsKey(key);
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map.containsValue(java.lang.Object)
	 */
	public final boolean containsValue(Object value) {

		if (data != null && data.size() > 0) {
			if (data.containsValue(value)) {
				return true;
			}
		}

		Map<String, _F> m2 = _getFields();
		if (m2 != null && m2.size() > 0) {
			for (String name : m2.keySet()) {
				_F f = m2.get(name);
				try {
					Object o = f.get(this);
					if (X.isSame(value, o)) {
						return true;
					}
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}

			}
		}
		return false;
	}

	/**
	 * put the key-value in bean.
	 *
	 * @param key   the key
	 * @param value the value
	 * @return the object of old data
	 */
	public final Object put(String key, Object value) {
		return set(key, value);
	}

	/**
	 * put all the data in the map to Bean.
	 *
	 * @param m the data map
	 */
	public final void putAll(Map<? extends String, ? extends Object> m) {

		if (_readonly)
			return;

		for (String s : m.keySet()) {
			set(s, m.get(s));
		}
	}

	/**
	 * remove all data from the bean, <br>
	 * set the fields to null that annotation by @Column.
	 */
	public final void clear() {

		if (_readonly)
			return;

		/**
		 * clear data in data
		 */
		if (data != null) {
			data.clear();
		}

		/**
		 * clear data Annotation by @Column
		 */
		Map<String, _F> m1 = this._getFields();
		if (m1 != null && m1.size() > 0) {
			for (_F f : m1.values()) {
				try {
					f.set(this, null);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

	}

	/**
	 * get the names from the bean, <br>
	 * the names in the "data" map, and the field annotation by @column.
	 *
	 * @return the sets of the names
	 */
	public final Set<String> keySet() {
		Set<String> l1 = new TreeSet<String>();
		if (data != null && !data.isEmpty()) {
			l1.addAll(data.keySet());
		}

		Map<String, _F> m2 = _getFields();
		if (m2 != null && m2.size() > 0) {
			l1.addAll(m2.keySet());
		}
		return l1;
	}

	/**
	 * get all the values, include the field annotation by @Column.
	 *
	 * @return the collection
	 */
	public final Collection<Object> values() {
		return getAll().values();
	}

	/**
	 * by default, get integer from the map.
	 *
	 * @param name the name
	 * @return the int of value, default 0
	 */
	public final int getInt(String name) {
		return X.toInt(get(name), 0);
	}

	/**
	 * by default, get long from the map.
	 *
	 * @param name the name
	 * @return long of the value, default 0
	 */
	public long getLong(String name) {
		return X.toLong(get(name), 0);
	}

	/**
	 * by default, get the string from the map.
	 *
	 * @param name the name
	 * @return String of the value, null if the name not exists
	 */
	public final String getString(String name) {
		Object o = get(name);
		if (o == null) {
			return null;
		} else if (o instanceof String) {
			return (String) o;
		} else {
			return o.toString();
		}
	}

	/**
	 * by default, get the float from the map.
	 *
	 * @param name the name
	 * @return float of the value, default 0
	 */
	public final float getFloat(String name) {
		return X.toFloat(get(name), 0);
	}

	/**
	 * by default, get the double from the map.
	 *
	 * @param name the name
	 * @return double of the value, default 0
	 */
	public final double getDouble(String name) {
		return X.toDouble(get(name), 0);
	}

	/**
	 * get all data, include the field annotation by @Column
	 * 
	 * @return Map of data
	 */
	public Map<String, Object> getAll() {

		Map<String, Object> map_obj = new HashMap<String, Object>();
		if (data != null && data.size() > 0) {
			map_obj.putAll(data);
		}

		Map<String, _F> m2 = _getFields();
		if (m2 != null && m2.size() > 0) {
			for (String name : m2.keySet()) {
				_F f = m2.get(name);
				try {
					Object o = f.get(this);
					String name1 = f.getName().toLowerCase();
					map_obj.put(name1, o);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}

		for (Object name : map_obj.keySet().toArray()) {
			if (X.isIn(name, "created", "updated")) {
				Object o = map_obj.get(name);
				if (X.toLong(o) == 0) {
					map_obj.remove(name);
				}
			}
		}

		return map_obj;
	}

	/**
	 * remove all value, same as clear.
	 */
	public final void removeAll() {
		if (_readonly)
			return;

		clear();
	}

	/**
	 * remove value by names.
	 *
	 * @param names the names
	 */
	public final void remove(String... names) {

		if (_readonly)
			return;

		if (data != null && names != null) {
			for (String name : names) {
				if (name.indexOf("*") > -1) {

					if (data != null) {
						String[] ss = data.keySet().toArray(new String[data.size()]);
						for (String k : ss) {
							if (k.matches(name)) {
								this._remove(k);
							}
						}
					}

					Map<String, _F> m2 = _getFields();
					if (m2 != null && m2.size() > 0) {
						for (String k : m2.keySet()) {
							if (k.matches(name)) {
								this._remove(k);
							}
						}
					}
				} else {
					this._remove(name);
				}
			}
		}
	}

	private void _remove(String name) {
		try {
			_F f1 = _getField(name);
			if (f1 != null) {
				f1.set(this, null);
			} else if (data != null) {
				data.remove(name);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Column(no = true)
	private Map<String, Object> data = null;

//	private transient JSON json_obj;

	/**
	 * create the data as json.<br>
	 * 
	 * @return JSON
	 */
	@Comment(text = "转换为json")
	public JSON json() {

		JSON json_obj = JSON.fromObject(getAll());

		json_obj.scan((j1, name) -> {
			Object o = j1.get(name);
			if (o != null && !(o instanceof Serializable)) {
				log.info("bad data, name=" + name, new Exception());
				j1.remove(name);
			}
		});

		return json_obj;
	}

	@Comment(text = "转换为json", demo = ".json('a', 'b', 'c')")
	public JSON json(String... names) {

		if (X.isEmpty(names)) {
			return json();
		}

		JSON json_obj = JSON.create();

		for (String name : names) {
			json_obj.put(name, this.get(name));
		}

		return json_obj;
	}

	/**
	 * replace by json()
	 * 
	 * @return
	 */
	@Deprecated
	public final JSON getJSON() {
		return json();
	}

	/**
	 * Load by default, get all columns to a map<br>
	 * it will invoked when load data from the MongoDB<br>
	 * by default, will load all data in Bean Map.
	 * 
	 * @param d      the Document
	 * @param fields the String[]
	 */
	public void load(Document d) {
		for (String name : d.keySet()) {
			Object o = d.get(name);
			this.set(name, o);
		}
	}

	public void load(BSONObject d) {
		for (String name : d.keySet()) {
			Object o = d.get(name);
			this.set(name, o);
		}
	}

	public void load(Document d, String[] ss) {
		if (ss == null || ss.length == 0 || X.isIn("*", ss)) {
			load(d);
		} else {
			for (String name : ss) {
				Object o = d.get(name);
				this.set(name, o);
			}
		}
	}

	public void load(BSONObject d, String[] ss) {
		if (ss == null || ss.length == 0 || X.isIn("*", ss)) {
			load(d);
		} else {
			for (String name : ss) {
				Object o = d.get(name);
				this.set(name, o);
			}
		}
	}

	/**
	 * Load data by default, get all fields and set in map.<br>
	 * it will be invoked when load data from RDBS DB <br>
	 * By default, it will load all data in Bean Map.
	 * 
	 * @param r      the ResultSet of RDBS
	 * @param fields the String[] of fields
	 * @throws SQLException the SQL exception
	 */
	public void load(ResultSet r) throws SQLException {
		ResultSetMetaData m = r.getMetaData();
		int cols = m.getColumnCount();
		for (int i = 1; i <= cols; i++) {
			try {
				Object o = r.getObject(i);
				if (o != null) {
					if (o instanceof Date) {
						Date d = ((Date) o);
						o = d.getTime();
					} else if (o.getClass().getName().equals("oracle.sql.TIMESTAMP")) {
						o = o.toString();
					}
				}

				String name = m.getColumnName(i);

				this.set(name, o);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public boolean contains(V v) {
		for (String name : v.names()) {
			Object v0 = get(name);
			Object v1 = v.value(name);
			if (!X.isSame(v0, v1)) {
				return false;
			}
		}
		return true;
	}

	public boolean contains(JSON v) {
		for (Map.Entry<String, Object> e : v.entrySet()) {
			Object v0 = get(e.getKey());
			Object v1 = e.getValue();
			if (!X.isSame(v0, v1)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * clean same value from V
	 * 
	 * @param v values
	 * @return True if removed
	 */
	public boolean clean(V v) {

		boolean removed = false;
		for (String name : v.names()) {
			Object v0 = get(name);
			Object v1 = v.value(name);
			if (X.isSame(v0, v1)) {
				v.remove(name);
				removed = true;
			}
		}
		return removed;

	}

	@SuppressWarnings("unchecked")
	public boolean clean(JSON v) {

		boolean removed = false;
		for (Map.Entry<String, Object> e : v.entrySet().toArray(new Map.Entry[v.size()])) {
			Object v0 = get(e.getKey());
			Object v1 = e.getValue();
			if (X.isSame(v0, v1)) {
				v.remove(e.getKey());
				removed = true;
			}
		}
		return removed;

	}

	@Override
	public Object remove(Object key) {
		return set(key.toString(), null);
	}

	@Override
	public Set<Entry<String, Object>> entrySet() {
		return getAll().entrySet();
	}

	@Override
	public Object clone() {
		try {
			return super.clone();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return null;
	}

	@Override
	public int hashCode() {

		Object id = this.get(X.ID);

		if (id == null)
			return 0;
		return id.hashCode();

	}

	@Override
	public boolean equals(Object obj) {

		if (this == obj)
			return true;

		if (!this.getClass().equals(obj.getClass()))
			return false;

		Object id1 = this.get(X.ID);
		Object id2 = ((Bean) obj).get(X.ID);

		return X.isSame(id1, id2);
	}

	/**
	 * get the md5 for attributes except ( _.*, created, updated)
	 * 
	 * @return
	 */
	public String md5() {
		JSON j1 = this.json().copy();
		j1.remove("_.*", "created", "updated");
		String s1 = j1.toString();

		if (log.isDebugEnabled())
			log.debug("s1=" + s1);

		return Digest.md5(s1);
	}

	private static class _F {

		Field f;
		_F link;

		void link(Field f) {
			if (link == null) {
				link = new _F();
				link.f = f;
			}
		}

		String getName() {
			return f.getName();
		}

		Class<?> getType() {
			return f.getType();
		}

		static _F create(Field f) {
			_F e = new _F();
			e.f = f;
			return e;
		}

		Object get(Object that) throws IllegalArgumentException, IllegalAccessException {
//			f.setAccessible(true);
			return f.get(that);
		}

		void set(Object that, Object val) throws IllegalArgumentException, IllegalAccessException {
//			f.setAccessible(true);
			f.set(that, val);
			if (link != null) {
				link.set(that, val);
			}
		}
	}

	public void filter(V v) {
		for (String name : this.keySet()) {
			Object v1 = this.get(name);
			Object v2 = v.value(name);
			if (X.isIn2(v2, v1, V.ignore)) {
				v.remove(name);
			}
		}
	}

	public void filter(JSON jo) {
		for (String name : this.keySet()) {
			Object v1 = this.get(name);
			Object v2 = jo.get(name);
			if (X.isSame2(v1, v2)) {
				jo.remove(name);
			}
		}
	}

}
