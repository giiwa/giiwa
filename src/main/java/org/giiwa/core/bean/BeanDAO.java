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

import java.sql.SQLException;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.framework.bean.User;

public class BeanDAO<T extends Bean> {

	/** The log utility */
	protected static Log log = LogFactory.getLog(BeanDAO.class);

	Class<T> t;

	private BeanDAO(Class<T> t) {
		this.t = t;
		// try {
		// t = (Class<T>) ((ParameterizedType)
		// getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		// } catch (Throwable e) {
		// log.error(e.getMessage(), e);
		// e.printStackTrace();
		// }
	}

	public T load(W q) {
		return Helper.load(q, t);
	}

	public T load(String[] fields, W q) {
		return Helper.load(fields, q, t);
	}

	public T load(Object id) {
		return Helper.load(id, t);
	}

	public Beans<T> load(String[] fields, W q, int s, int n) {
		Beans<T> bs = Helper.load(fields, q, s, n, t);
		if (bs != null) {
			bs.q = q;
			bs.dao = this;
		}
		return bs;
	}

	public Beans<T> load(W q, int s, int n) {
		Beans<T> bs = Helper.load(q, s, n, t);
		if (bs != null) {
			bs.q = q;
			bs.dao = this;
		}
		return bs;
	}

	public boolean exists(W q) throws SQLException {
		return Helper.exists(q, t);
	}

	public boolean exists(Object id) throws SQLException {
		return Helper.exists(id, t);
	}

	public int update(W q, V v) {
		return Helper.update(q, v, t);
	}

	public int update(Object id, V v) {
		return Helper.update(id, v, t);
	}

	public int insert(V v) {
		return Helper.insert(v, t);
	}

	public int delete(Object id) {
		return Helper.delete(id, t);
	}

	public int delete(W q) {
		return Helper.delete(q, t);
	}

	public long count(W q) {
		return Helper.count(q, t);
	}

	public <E> List<E> distinct(String name, W q, Class<E> t1) {
		return Helper.distinct(name, q, t, t1);
	}

	public int inc(W q, String name, int n, V v) {
		return Helper.inc(q, name, n, v, t);
	}

	public static void main(String[] args) {
		System.out.println(User.dao.t);
	}

	public static <E extends Bean> BeanDAO<E> create(Class<E> t) {
		return new BeanDAO<E>(t);
	}
}
