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

import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;
import org.giiwa.task.Function;
import org.giiwa.task.Task;

/**
 * The {@code Beans} Class used to contains the Bean in query. <br>
 * it's includes the total count for the query
 * 
 */
@Comment(text = "数据列表")
public final class Beans<E extends Bean> extends ArrayList<E> implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 2L;

	/** The log. */
	protected static Log log = LogFactory.getLog(Beans.class);

	/** The total. */
	public long total = -1; // unknown

	public JSON stats;

	public long created = System.currentTimeMillis();

	public long cost = -1;

	public List<String> columns = null;

	transient W q;
	transient String table;
	transient BeanDAO<?, ?> dao;

	private String id = UID.random(20);

	public static <E extends Bean> Beans<E> create() {
		return new Beans<E>();
	}

	public String getId() {
		return id;
	}

	@Comment(text = "统计计数")
	public long count() {
		try {
			if (dao != null && total < 0) {
				total = dao.count(q);

				if (log.isDebugEnabled())
					log.debug("count, total=" + total + ", q=" + q);

			} else if (total < 0 && q != null) {
				if (!X.isEmpty(q.table())) {
					try {
						total = q.count();
					} catch (Exception e) {
						log.error(e.getMessage(), e);
					}
				} else {
					total = Helper.count(table, q);
				}

				if (log.isDebugEnabled())
					log.debug("count, total=" + total + ", q=" + q + ", table=" + table);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return total;
	}

	@Comment(text = "总数")
	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}

	public JSON getStats() {
		return stats;
	}

	public void setStats(JSON stats) {
		this.stats = stats;
	}

	public long getCost() {
		return cost;
	}

	public void setCost(long cost) {
		this.cost = cost;
	}

	public List<String> getColumns() {
		return columns;
	}

	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

	public List<JSON> toJSON(Function<E, JSON> cb) {
		List<JSON> l1 = JSON.createList();
		for (E e : this) {
			JSON j = null;
			if (cb != null) {
				j = cb.apply(e);
			} else {
				j = e.json();

			}
			if (j != null) {
				l1.add(j);
			}
		}
		return l1;
	}

	@Comment(text = "转换为列表")
	public <K> List<K> asList(Function<E, K> cb) {
		List<K> l1 = new ArrayList<K>();
		for (E e : this) {
			K k = cb.apply(e);
			if (k != null) {
				l1.add(k);
			}
		}
		return l1;
	}

	@Comment(text = "转换为json列表", demo = ".asList('a', 'b', 'c')")
	public List<JSON> asList(String... names) {
		List<JSON> l1 = new ArrayList<JSON>();
		for (E e : this) {
			JSON j = e.json();
			l1.add(j.copy(names));
		}
		return l1;
	}

	@Comment(text = "转换为json列表", demo = ".jsons('a', 'b', 'c')")
	public List<JSON> jsons(String... names) {
		List<JSON> l1 = new ArrayList<JSON>();
		for (E e : this) {
			JSON j = e.json();
			l1.add(j.copy(names));
		}
		return l1;
	}

	@Comment(text = "转换为json列表", demo = ".jsons()")
	public List<JSON> jsons() {
		List<JSON> l1 = new ArrayList<JSON>();
		for (E e : this) {
			JSON j = e.json();
			l1.add(j);
		}
		return l1;
	}

	@Comment(text = "删除列表中字段", demo = ".remove('_id', '_node', 'created')")
	public Beans<E> remove(String... names) {
		for (E e : this) {
			e.remove(names);
		}
		return this;
	}

	/**
	 * 转为矩阵
	 * 
	 * @param heads
	 * @return
	 */
	public List<Object[]> mat(String... heads) {

		List<Object[]> l1 = new ArrayList<Object[]>();
		l1.add(heads);

		if (!this.isEmpty()) {
			if (this.size() > 100) {
				Task.forEach(this, e -> {
					Object[] ss = new Object[heads.length];
					for (int i = 0; i < heads.length; i++) {
						ss[i] = e.get(heads[i]);
					}
					l1.add(ss);
				});
			} else {
				for (Bean e : this) {
					Object[] ss = new Object[heads.length];
					for (int i = 0; i < heads.length; i++) {
						ss[i] = e.get(heads[i]);
					}
					l1.add(ss);
				}
			}
		}
		return l1;

	}

	/**
	 * 数字化
	 * 
	 * @param name
	 * @return
	 */
	public Beans<E> digitizing(String name) {

		if (this.isEmpty()) {
			return this;
		}

		Map<String, Integer> m = new HashMap<String, Integer>();

		if (this.size() > 100) {
			Task.forEach(this, e -> {
				Object s = e.get(name);
				if (X.isEmpty(s)) {
					s = X.EMPTY;
				}

				Integer i = m.get(s.toString());
				if (i == null) {
					i = m.size() + 1;
					m.put(s.toString(), i);
				}
				e.set(name, i);
			});
		} else {

			for (Bean e : this) {
				Object s = e.get(name);
				if (X.isEmpty(s)) {
					s = X.EMPTY;
				}

				Integer i = m.get(s.toString());
				if (i == null) {
					i = m.size() + 1;
					m.put(s.toString(), i);
				}
				e.set(name, i);
			}
		}
		return this;

	}

	/**
	 * 替换
	 * 
	 * @param name
	 * @param mapping
	 * @return
	 */
	public Beans<E> replace(String name, Object mapping) {

		if (this.isEmpty()) {
			return this;
		}

		JSON m = JSON.fromObject(mapping);
		if (m == null || m.isEmpty()) {
			return this;
		}

		if (this.size() > 100) {
			Task.forEach(this, e -> {
				Object s = e.get(name);
				if (X.isEmpty(s)) {
					e.set(name, m.get(X.EMPTY));
				} else {
					e.set(name, m.get(s.toString()));
				}
			});
		} else {
			for (Bean e : this) {
				Object s = e.get(name);
				if (X.isEmpty(s)) {
					e.set(name, m.get(X.EMPTY));
				} else {
					e.set(name, m.get(s.toString()));
				}
			}
		}
		return this;

	}

}
