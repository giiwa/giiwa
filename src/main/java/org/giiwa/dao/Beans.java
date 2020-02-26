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
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.dao.Helper.W;
import org.giiwa.json.JSON;

/**
 * The {@code Beans} Class used to contains the Bean in query. <br>
 * it's includes the total count for the query
 * 
 */
public final class Beans<E extends Bean> extends ArrayList<E> implements Serializable {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 2L;

	/** The log. */
	protected static Log log = LogFactory.getLog(Beans.class);

	/** The total. */
	public long total = -1; // unknown

	public JSON stats;

	public long cost = -1;

	public List<String> columns = null;

	transient W q;
	transient String table;
	transient String db;
	transient BeanDAO<?, ?> dao;

	private String id = UID.random(20);

	public static <E extends Bean> Beans<E> create() {
		return new Beans<E>();
	}

	public String getId() {
		return id;
	}

	public long count() {
		if (dao != null && total < 0) {
			total = dao.count(q);
			log.debug("count, total=" + total + ", q=" + q);
		} else if (total < 0) {
			total = Helper.count(q, table, db);
			log.debug("count, total=" + total + ", q=" + q + ", table=" + table);
		}
		return total;
	}

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

}
