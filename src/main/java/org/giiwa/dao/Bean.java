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

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.BSONObject;
import org.bson.Document;
import org.giiwa.bean.Key;
import org.giiwa.bean.Temp;
import org.giiwa.crypto.MD5;
import org.giiwa.crypto.SM4;
import org.giiwa.dao.Helper.V;
import org.giiwa.json.JSON;
import org.giiwa.misc.Exporter;
import org.giiwa.misc.StringFinder;
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
	private volatile boolean _readonly = false;

	/**
	 * 元数据
	 */
	@Column(no = true)
	private Map<String, JSON> _meta = null;

	/**
	 * 选择列表
	 */
	@Column(no = true)
	private String[] _selected;

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
	private long _rowid;

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
	 * 修改data的值，需要加锁 <br>
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
		String lowername = name.toLowerCase();
//		log.warn("field=" + name + ", value=" + value.getClass());

		value = _parse(value);

//		log.info(X.NAME=" + name + ", value=" + value);

		// looking for all the fields
		_F f1 = _getField(lowername);

		if (f1 != null) {

			try {
				if (f1.password) {
					value = _decode((String) value);
				}
//				if (X.isSame(name, "password")) {
//					log.warn("password=" + f1.password + ", value=" + value + ", class=" + this.getClass());
//				}

//				f1.setAccessible(true);

				// log.debug("f1=" + f1 + ", value=" + value);
				old = f1.get(this);

				Class<?> t1 = f1.getType();
				// log.debug("t1=" + t1 + ", f1.name=" + f1.getName());
				if (value == null) {
					if (!t1.isPrimitive()) {
						// 不是基础类型
						f1.set(this, null);
					}
				} else if (t1.equals(value.getClass())) {
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
					if (value != null) {
						value = value.toString();
					}
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
							l1.addAll(Arrays.asList(value));
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
				log.error(name + "=" + value, e);
			}
		} else {

			if (_data == null) {
				_data = new HashMap<>();
			}

			old = _data.get(lowername);
			if (value == null) {
				_data.remove(lowername);
			} else if (value instanceof Date) {
				_data.put(lowername, ((Date) value).getTime());
			} else if (value instanceof Timestamp) {
				_data.put(lowername, ((Timestamp) value).getTime());
			} else if (value instanceof LocalDateTime) {
				java.util.Date d = Date.from(((LocalDateTime) value).atZone(ZoneOffset.ofHours(8)).toInstant());
				_data.put(lowername, d.getTime());
			} else if (value instanceof Number) {
				Number n = (Number) value;
				if (n.toString().indexOf(".") > -1) {
					_data.put(lowername, n.doubleValue());
				} else {
					_data.put(lowername, n.longValue());
				}
			} else {
				_data.put(lowername, value);
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

	private Object _parse(Object value) {

		if (value == null)
			return null;

		if (value instanceof java.sql.Clob) {
			// TODO, 大字段怎么办？ ？GB
			Clob c = (Clob) value;
			Reader re = null;
			try {
				re = c.getCharacterStream();
				char[] cc = new char[(int) c.length()];
				re.read(cc);
				value = new String(cc);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				X.close(re);
				try {
					c.free();
				} catch (Exception err) {
					log.error(err.getMessage(), err);
				}
			}
		} else if (value instanceof java.sql.NClob) {
			// TODO, 大字段怎么办？ GB
			NClob c = (NClob) value;
			Reader re = null;
			try {
				re = c.getCharacterStream();
				char[] cc = new char[(int) c.length()];
				re.read(cc);
				value = new String(cc);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				X.close(re);
				try {
					c.free();
				} catch (Exception err) {
					log.error(err.getMessage(), err);
				}
			}
		} else if (value instanceof java.sql.Blob) {
			Blob c = (Blob) value;
			try {
				value = c.getBytes(0, (int) c.length());
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			} finally {
				try {
					c.free();
				} catch (Exception err) {
					log.error(err.getMessage(), err);
				}
			}
		} else if (value instanceof Array) {
			try {
				java.sql.Array c = (Array) value;
				value = X.asList(c.getArray(), s -> _parse(s));
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else if (value instanceof ScriptObjectMirror) {
			// TODO, data.a = []
			// data.a.push 会报错
//		ScriptObjectMirror m = (ScriptObjectMirror) value;
//		if (m.isArray()) {
//			value = X.asList(m, s -> s);
//		} else {
//			value = JSON.fromObject(value);
//		}
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
			value = b.getValue();
		} else if (value instanceof org.bson.types.Binary) {
			org.bson.types.Binary b = (org.bson.types.Binary) value;
			value = b.getData();
		} else if (value instanceof Date) {
			value = ((Date) value).getTime();
		} else if (X.isArray(value)) {
			value = X.asList(value, o -> _parse(o));
		}
		return value;
	}

	/**
	 * get the Field by the colname
	 * 
	 * @param columnname the colname
	 * @return the Field
	 */
//	public Field getField(String columnname) {
//		_F f = _getField(columnname);
//		return f == null ? null : f.f;
//	}

	private _F _getField(String columnname) {

		Map<String, _F> m = _getFields();
		return m == null ? null : m.get(columnname);

	}

	/**
	 * 注意有权限， readonly后，通过这个还能修改数据
	 * 
	 * @return
	 */
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

	private volatile transient Map<String, _F> _ff;

	private Map<String, _F> _getFields() {

		if (_ff == null) {
			synchronized (this) {
				if (_ff == null) {
					Class<?> c1 = this.getClass();
					_ff = _fields.get(c1);
					if (_ff == null) {
						_ff = new ConcurrentHashMap<String, _F>();

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

									_F f2 = _ff.get(name);
									if (f2 == null) {
										f2 = _F.create(f);
										f2.password = f1.password();
										_ff.put(name, f2);
									} else {
										f2.link(f);
									}

									String name1 = f.getName().toLowerCase();
									if (!X.isSame(name, name1)) {

										f2 = _ff.get(name1);
										if (f2 == null) {
											f2 = _F.create(f);
											f2.password = f1.password();
											_ff.put(name1, f2);
										} else {
											f2.link(f);
										}
									}
								} else if ((f.getModifiers()
										& (Modifier.FINAL | Modifier.STATIC | Modifier.TRANSIENT)) == 0) {
									f.setAccessible(true);
									String name = f.getName().toLowerCase();

									_F f2 = _ff.get(name);
									if (f2 == null) {
										f2 = _F.create(f);
										if (f1 != null) {
											f2.password = f1.password();
										}
										_ff.put(name, f2);
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

						_fields.put(this.getClass(), _ff);
					}
				}
			}
		}

		return _ff;
	}

	private final static Map<Class<? extends Bean>, Map<String, _F>> _fields = new ConcurrentHashMap<Class<? extends Bean>, Map<String, _F>>();

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

		// 大小写不敏感
		String lowername = name.toString().toLowerCase();
		_F f = _getField(lowername);
		if (f != null) {
			try {
				return f.get(this);
			} catch (Exception e) {
				log.error(name, e);
			}
		}

		if (_data == null) {
			return null;
		}

		return _data.get(lowername);

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

		Set<String> names = new HashSet<>();

		if (_data != null) {
			names.addAll(_data.keySet());
		}

		Map<String, _F> m2 = _getFields();
		if (m2 != null) {
			names.addAll(m2.keySet());
		}

		return names.size();
	}

	/**
	 * test is empty bean
	 * 
	 * @return the boolean, true if empty
	 */
	public final boolean isEmpty() {
		return this.size() == 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map.containsKey(java.lang.Object)
	 */
	public boolean containsKey(Object key) {
		if (key == null)
			return false;
		String s = key.toString().toLowerCase();

		if (_data != null && _data.size() > 0) {
			if (_data.containsKey(s)) {
				return true;
			}
		}

		Map<String, _F> m2 = _getFields();
		if (m2 != null) {
			return m2.containsKey(s);
		}

		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map.containsValue(java.lang.Object)
	 */
	public final boolean containsValue(Object value) {

		if (_data != null && _data.size() > 0) {
			if (_data.containsValue(value)) {
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
	 * 对data进行修改，需要加锁 <br>
	 * remove all data from the bean, <br>
	 * set the fields to null that annotation by @Column.
	 */
	public final synchronized void clear() {

		if (_readonly)
			return;

		/**
		 * clear data in data
		 */
		if (_data != null) {
			_data.clear();
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
	 * 获取data的所有key，需要加锁<br>
	 * get the names from the bean, <br>
	 * the names in the "data" map, and the field annotation by @column.
	 *
	 * @return the sets of the names
	 */
	public final synchronized Set<String> keySet() {
		Set<String> l1 = new TreeSet<String>();
		if (_data != null && !_data.isEmpty()) {
			l1.addAll(_data.keySet());
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
	 * 获取所有data数据，需要加锁<br>
	 * get all data, include the field annotation by @Column
	 * 
	 * @return Map of data
	 */
	public synchronized Map<String, Object> getAll() {

		Map<String, Object> map_obj = new HashMap<String, Object>();
		if (_data != null && _data.size() > 0) {
			map_obj.putAll(_data);
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
			if (X.isIn(name, X.CREATED, "updated")) {
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
	 * 修改data值，需要加锁 <br>
	 * remove value by names.
	 *
	 * @param names the names
	 */
	public final synchronized void remove(String... names) {

		if (_readonly)
			return;

		if (_data != null && names != null) {
			for (String name : names) {
				if (name.indexOf("*") > -1) {

					if (_data != null) {
						String[] ss = _data.keySet().toArray(new String[_data.size()]);
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

	/**
	 * 对data进行修改，需要加锁
	 * 
	 * @param name
	 */
	private synchronized void _remove(String name) {
		try {
			_F f1 = _getField(name);
			if (f1 != null) {
				f1.set(this, null);
			} else if (_data != null) {
				_data.remove(name);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	// 按需生成扩展字段
	@Column(no = true)
	private Map<String, Object> _data = null;

//	private transient JSON json_obj;

	/**
	 * create the data as json.<br>
	 * 
	 * @return JSON
	 */
	@Comment(text = "转换为json")
	public JSON json() {
		return json(_selected);
	}

	@Comment(text = "转换为json")
	public JSON json2() {

		JSON json_obj = JSON.create();
		if (_selected != null && _selected.length > 0) {
			for (String s : _selected) {
				// name:name1->%tY-%<tm-%<td %<tH:%<tM:%<tS //格式化为时间格式
				// name:name1->.2 //保留2位小数点

				String name = s, name1 = s, format = null;

				StringFinder sf = StringFinder.create(s);
				int i = sf.find(":", "->");
				if (i > -1) {
					name = s.substring(0, i).trim();
					if (sf.charOf(0) == ':') {
						name1 = s.substring(i + 1).trim();
					} else {
						name1 = s;
					}
					i = name1.indexOf("->");
					if (i > 0) {
						format = name1.substring(i + 2).trim();
						name1 = name1.substring(0, i).trim();
					}
				}

				Object o = this.get(name);
				if (o instanceof Serializable) {
					if (format != null && format.length() > 0) {
						char c1 = format.charAt(0);
						if (c1 == '%') {
							o = String.format(format, o);
						} else if (c1 == '#') {
							o = new DecimalFormat("#,##0").format(o);
						} else if (c1 == '.') {
							// .2
							o = BigDecimal.valueOf(X.toDouble(o))
									.setScale(X.toInt(format.substring(1)), RoundingMode.HALF_UP).doubleValue();
						}
					}

					if (_meta == null) {
						json_obj.put(name1, o);
					} else {
						JSON c = _meta.get(name);
						if (c == null) {
							json_obj.put(name1, o);
						} else {
							json_obj.put(c.getString("display"), o);
						}
					}
				}
			}
		} else {
			for (String s : this.keySet()) {
				Object o = this.get(s);
				if (o instanceof Serializable) {
					if (_meta == null) {
						json_obj.put(s, o);
					} else {
						JSON c = _meta.get(s);
						if (c == null) {
							json_obj.put(s, o);
						} else {
							json_obj.put(c.getString("display"), o);
						}
					}
				}
			}
		}

		return json_obj;
	}

	@Comment(text = "转换为json", demo = ".json('a', 'b', 'c', 'd->.1f')")
	public JSON json(String... names) {

		JSON json_obj = JSON.create();
		if (X.isEmpty(names)) {
			for (String s : this.keySet()) {
				Object o = this.get(s);
				if (o instanceof Serializable) {
					json_obj.put(s, o);
				}
			}
			return json_obj;
		}

		for (String s : names) {
			// name:name1->%tY-%<tm-%<td %<tH:%<tM:%<tS //格式化为时间格式
			// name:name1->.2 //保留2位小数点

			String name = s, name1 = s, format = null;

			StringFinder sf = StringFinder.create(s);
			int i = sf.find(":", "->");
			if (i > -1) {
				name = s.substring(0, i).trim();
				if (sf.charOf(0) == ':') {
					name1 = s.substring(i + 1).trim();
				} else {
					name1 = s;
				}
				i = name1.indexOf("->");
				if (i > 0) {
					format = name1.substring(i + 2).trim();
					name1 = name1.substring(0, i).trim();
				}
			}

			Object o = this.get(name);
			if (o instanceof Serializable) {
				if (format != null && format.length() > 0) {
					char c1 = format.charAt(0);
					if (c1 == '%') {
						o = String.format(format, o);
					} else if (c1 == '#') {
						o = new DecimalFormat("#,##0").format(o);
					} else if (c1 == '.') {
						// .2
						o = BigDecimal.valueOf(X.toDouble(o))
								.setScale(X.toInt(format.substring(1)), RoundingMode.HALF_UP).doubleValue();
					}
				}

				json_obj.put(name1, o);
			}
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

//	public void load(org.sbson.BSONObject d) {
//		for (String name : d.keySet()) {
//			Object o = d.get(name);
//			this.set(name, o);
//		}
//	}

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
			Bean b = (Bean) super.clone();
			if (_data != null)
				b._data = new HashMap<>(_data);
			if (_meta != null)
				b._meta = new HashMap<>(_meta);
			if (_selected != null)
				b._selected = _selected.clone();
			return b;
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
		j1.remove("_.*", X.CREATED, "updated");
		String s1 = j1.toString();

		if (log.isDebugEnabled())
			log.debug("s1=" + s1);

		return MD5.md5(s1);
	}

	private static class _F {

		Field f;
		boolean password;
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

	public void setMeta(List<JSON> meta) {

		if (meta == null || meta.isEmpty()) {
			return;
		}

		this._meta = new HashMap<String, JSON>();
		for (JSON e : meta) {
			this._meta.put(e.getString(X.NAME), e);
		}
	}

	@Comment(text = "选择列", demo = ".select('a','b')")
	public Bean select(String... names) {
		_selected = names;
		return this;
	}

	@Comment(text = "输出为CSV文件", demo = ".csv('a','b')")
	public String csv(String... names) throws IOException {
		_selected = names;
		return csv();
	}

	@Comment(text = "输出为CSV文件", demo = ".csv()")
	public String csv() throws IOException {

		Temp t = Temp.create("a.csv");
		Exporter<Bean> ex = Exporter.create(t.getFile(), Exporter.FORMAT.csv);

		// print head
		String[] cc = null;
		if (_selected != null && _selected.length != 0) {
			cc = _selected;
		} else {
			if (!this.isEmpty()) {
				Set<String> names = this.keySet();
				cc = names.toArray(new String[names.size()]);
			}
		}
		if (cc != null) {
			if (_meta != null) {
				String[] ss = new String[cc.length];
				for (int i = 0; i < cc.length; i++) {
					String s = cc[i];
					JSON c = _meta.get(s);
					if (c == null) {
						ss[i] = s;
					} else {
						ss[i] = c.getString("display");
					}
				}
				ex.print(ss);
			} else {
				ex.print(cc);
			}

			String[] cc1 = cc;
			ex.createSheet(e -> {
				Object[] v = new Object[cc1.length];
				for (int i = 0; i < cc1.length; i++) {
					v[i] = e.get(cc1[i]);
				}
				return v;
			});

			ex.print(this);
		}

		ex.close();
		return X.IO.read(t.getFile(), X.UTF8);
	}

	private String _decode(String password) {
		if (X.isEmpty(password)) {
			return password;
		}
		if (password.startsWith("$$") && password.length() > 3) {
			char v = password.charAt(2);
			if (v == '3') {
				try {
					return SM4.decode(password.substring(3), Key.get("password", 20));
				} catch (Exception err) {
					log.error(err.getMessage(), err);
				}
			}
		}
		return password;
	}

//	@SuppressWarnings({ "rawtypes", "deprecation" })
//	public static void main(String[] args) {
//
//		Config.init(new File("/Users/joe/d/giiwa/conf/giiwa.properties"));
//		Helper.init2(Config.getConf());
//
//		String s = "aaaaa";
//		BeanDAO a = User.dao;
//
//		s = a.encode(s).toString();
//		System.out.println("encode=" + s);
//
//		Bean b = new Bean();
//		System.out.println("decode=" + b._decode(s));
//
//	}

}
