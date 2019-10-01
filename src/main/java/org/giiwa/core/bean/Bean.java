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
package org.giiwa.core.bean;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.Document;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.json.JSON;

/**
 * The {@code Bean} Class is entity class that mapping to a table,<br>
 * work with {@code Helper}, you can load/update/delete data from DB(RDS/Mongo),
 * almost includes all methods that need for database <br>
 * 
 */
public class Bean implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 4L;

	/** The log utility */
	private static Log log = LogFactory.getLog(Bean.class);

	private long _expired = -1;

	/**
	 * the row number
	 */
	public long _rowid;

	/**
	 * set expired time before using in cache
	 * 
	 * @param expired the expired
	 */
	public void expired(long expired) {
		this._expired = expired;
	}

	/**
	 * Expired.
	 *
	 * @return true, if successful
	 */
	public boolean expired() {
		return _expired > 0 && System.currentTimeMillis() > _expired;
	}

	/**
	 * get the created timestamp of the data
	 * 
	 * @return long of the created
	 */
	public long getCreated() {
		return X.toLong((Object) get(X.CREATED));
	}

	/**
	 * get the updated timestamp of the data
	 * 
	 * @return long the of the updated
	 */
	public long getUpdated() {
		return X.toLong((Object) get(X.UPDATED));
	}

	/**
	 * refill the bean from json.
	 *
	 * @param jo the JSON object
	 * @return true if all successful
	 */
	public boolean fromJSON(JSON jo) {
		for (String name : jo.keySet()) {
			set(name, jo.get(name));
		}
		return true;
	}

	/**
	 * get the key-value in the bean to json.<br>
	 *
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
		return this.getClass().getSimpleName() + "@" + this.getJSON();
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
		if (value == V.ignore)
			return null;

		if (data == null) {
			data = new HashMap<String, Object>();
		}

		Object old = null;

		// change all to lower case avoid some database auto change to upper case
		name = name.toLowerCase();
		// looking for all the fields
		Field f1 = getField(name);

		if (f1 != null) {
			try {
				// log.debug("f1=" + f1 + ", value=" + value);
				f1.setAccessible(true);
				old = f1.get(this);

				Class<?> t1 = f1.getType();
				// log.debug("t1=" + t1 + ", f1.name=" + f1.getName());
				if (t1 == long.class) {
					f1.set(this, X.toLong(value));
				} else if (t1 == int.class) {
					f1.set(this, X.toInt(value));
				} else if (t1 == double.class) {
					f1.set(this, X.toDouble(value, 0));
				} else if (t1 == float.class) {
					f1.set(this, X.toFloat(value, 0));
				} else if (List.class.isAssignableFrom(t1) || t1.isArray()) {
					// change the value to list
					if (value != null) {
						List<Object> l1 = new ArrayList<Object>();
						if (value instanceof List) {
							l1.addAll((List<Object>) value);
						} else if (value.getClass().isArray()) {
							l1.add(Arrays.asList(value));
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
					} else {
						f1.set(this, value);
					}
				}
			} catch (Exception e) {
				log.error(name + "=" + value, e);
			}
		} else {
			old = data.get(name);
			if (value == null) {
				data.remove(name);
			} else {
				if (value instanceof Date) {
					data.put(name, ((Date) value).getTime());
				} else if (value instanceof Timestamp) {
					data.put(name, ((Timestamp) value).getTime());
				} else {
					data.put(name, value);
				}
			}
		}
		return old;
	}

	/**
	 * get the Field by the colname
	 * 
	 * @param columnname the colname
	 * @return the Field
	 */
	public Field getField(String columnname) {

		Map<String, Field> m = _getFields();
		return m == null ? null : m.get(columnname);

	}

	@SuppressWarnings("unchecked")
	private Map<String, Field> _getFields() {

		Class<? extends Bean> c1 = this.getClass();
		Map<String, Field> m = _fields.get(c1);
		if (m == null) {
			m = new HashMap<String, java.lang.reflect.Field>();

			int i = 0;
			for (; c1 != null;) {
				i++;
				if (log.isDebugEnabled())
					log.debug("c1=" + c1);

				Field[] ff = c1.getDeclaredFields();
				for (Field f : ff) {
					Column f1 = f.getAnnotation(Column.class);
					if (f1 != null) {
						String name = f1.name().toLowerCase();
						if (!m.containsKey(name)) {
							m.put(name, f);
						}
					}
				}
				if (i > 5) {
					log.error("c1=" + c1);
				}

				if (Bean.class.isAssignableFrom(c1.getSuperclass())) {
					if (c1.getSuperclass().isAssignableFrom(Bean.class)) {
						c1 = null;
					} else {
						c1 = (Class<? extends Bean>) c1.getSuperclass();
					}
				} else {
					c1 = null;
				}

			}

			_fields.put(this.getClass(), m);
		}
		return m;
	}

	private final static Map<Class<? extends Bean>, Map<String, Field>> _fields = new HashMap<Class<? extends Bean>, Map<String, Field>>();

	/**
	 * get the value by name from bean <br>
	 * 
	 * @param <T>  the Object
	 * @param name the name of the data or the column
	 * @return Object the value of the name, return null if the name not exists
	 */
	@SuppressWarnings("unchecked")
	public final <T> T get(Object name) {
		if (name == null) {
			return null;
		}

		String s = name.toString().toLowerCase();
		Field f = getField(s);
		if (f != null) {
			try {
				f.setAccessible(true);
				return (T) f.get(this);
			} catch (Exception e) {
				log.error(name, e);
			}
		}

		if (data == null) {
			return null;
		}

		if (data.containsKey(s)) {
			return (T) data.get(s);
		}

		return null;
	}

	/**
	 * get the size of the names.
	 *
	 * @return the int
	 */
	public final int size() {
		return getAll().size();
	}

	/**
	 * test is empty bean
	 * 
	 * @return the boolean, true if empty
	 */
	public final boolean isEmpty() {
		return getAll().isEmpty();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map.containsKey(java.lang.Object)
	 */
	public final boolean containsKey(Object key) {
		return getAll().containsKey(key);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.Map.containsValue(java.lang.Object)
	 */
	public final boolean containsValue(Object value) {
		return getAll().containsValue(value);
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
	 * remove the key from the bean.
	 *
	 * @param key the name
	 * @return the object of old value
	 */
	public final Object remove(Object key) {
		return this.set(key.toString(), null);
	}

	/**
	 * put all the data in the map to Bean.
	 *
	 * @param m the data map
	 */
	public final void putAll(Map<? extends String, ? extends Object> m) {
		for (String s : m.keySet()) {
			set(s, m.get(s));
		}
	}

	/**
	 * remove all data from the bean, <br>
	 * set the fields to null that annotation by @Column.
	 */
	public final void clear() {
		/**
		 * clear data in data
		 */
		if (data != null) {
			data.clear();
		}

		/**
		 * clear data Annotation by @Column
		 */
		Map<String, Field> m1 = this._getFields();
		if (m1 != null && m1.size() > 0) {
			for (Field f : m1.values()) {
				try {
					f.setAccessible(true);
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
		return getAll().keySet();
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

	private transient Map<String, Object> map_obj;

	/**
	 * get all data, include the field annotation by @Column
	 * 
	 * @return Map of data
	 */
	public Map<String, Object> getAll() {
		if (map_obj == null) {
			map_obj = new HashMap<String, Object>();
			if (data != null && data.size() > 0) {
				map_obj.putAll(data);
			}

			Map<String, Field> m2 = _getFields();
			if (m2 != null && m2.size() > 0) {
				for (String name : m2.keySet()) {
					if (!X.isSame("data", name)) {
						Field f = m2.get(name);
						try {
							f.setAccessible(true);
							map_obj.put(name, f.get(this));
						} catch (Exception e) {
							log.error(e.getMessage(), e);
						}
					}
				}
			}
		}
		return map_obj;
	}

	/**
	 * remove all value, same as clear.
	 */
	public final void removeAll() {
		clear();
	}

	/**
	 * remove value by names.
	 *
	 * @param names the names
	 */
	public final void remove(String... names) {
		if (data != null && names != null) {
			for (String name : names) {
				remove(name);
			}
		}
	}

	private Map<String, Object> data = null;

	private transient JSON json_obj;

	/**
	 * create the data as json.<br>
	 * 
	 * @return JSON
	 */
	public final JSON getJSON() {

		if (json_obj == null) {
			json_obj = new JSON();

			toJSON(json_obj);
		}
		return json_obj;
	}

	/**
	 * Load by default, get all columns to a map<br>
	 * it will invoked when load data from the MongoDB<br>
	 * by default, will load all data in Bean Map.
	 * 
	 * @param d      the Document
	 * @param fields the String[]
	 */
	public void load(Document d, String[] fields) {
		if (fields == null || fields.length == 0) {
			for (String name : d.keySet()) {
				Object o = d.get(name);
				this.set(name, o);
			}
		} else {
			for (String name : fields) {
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
	public void load(ResultSet r, String[] fields) throws SQLException {
		if (fields == null || fields.length == 0) {
			ResultSetMetaData m = r.getMetaData();
			int cols = m.getColumnCount();
			for (int i = 1; i <= cols; i++) {
				try {
					Object o = r.getObject(i);
					if (o instanceof Date) {
						o = ((Date) o).getTime();
					} else if (o instanceof oracle.sql.TIMESTAMP) {
						o = ((oracle.sql.TIMESTAMP) o).toString();
					}

					String name = m.getColumnName(i);

//					log.debug("name=" + name + ", o=" + (o == null ? null : o.getClass()));

					this.set(name, o);
				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		} else {
			for (String name : fields) {
				try {
					Object o = r.getObject(name);
					if (o instanceof Date) {
						o = ((Date) o).getTime();
					} else if (o instanceof oracle.sql.TIMESTAMP) {
						o = ((oracle.sql.TIMESTAMP) o).toString();
					}

					if (log.isDebugEnabled())
						log.debug("name=" + name + ", o=" + (o == null ? null : o.getClass()));

					this.set(name, o);

				} catch (Exception e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	public boolean contains(V v) {
		for (String name : v.names()) {
			Object v0 = get(name);
			Object v1 = v.value(name);
			if (!X.isSame(v0, v1)) {
				log.debug("name=" + name + ", v0=" + v0 + ", v1=" + v1);
				return false;
			}
		}

		return true;
	}

	/**
	 * cleanup, do nothing in default
	 */
	public void cleanup() {
		// TODO Auto-generated method stub

	}

	/**
	 * refine the bean and output as a json object
	 * 
	 * @param <T> the subclass of Bean
	 * @param e   the refine function
	 * @return the JSON
	 */
	@SuppressWarnings("unchecked")
	public <T extends Bean> JSON refine(Function<T, JSON> e) {
		return e.apply((T) this);
	}

}
