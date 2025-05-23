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
package org.giiwa.bean;

import java.sql.SQLException;
import java.util.concurrent.locks.Lock;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.conf.Global;
import org.giiwa.dao.Bean;
import org.giiwa.dao.Beans;
import org.giiwa.dao.Helper;
import org.giiwa.dao.Helper.W;
import org.giiwa.task.Consumer;

/**
 * Load "freedom" data storage, not specify. <br>
 * 
 * @author wujun
 *
 */
@Deprecated
public final class Data extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private static Log log = LogFactory.getLog(Data.class);

	/**
	 * Load data from the table
	 *
	 * @param table the table
	 * @param q     the query and order
	 * @param s     the start of number
	 * @param n     the number of items
	 * @return the beans
	 */
	public static Beans<Data> load(String table, W q, int s, int n) {
		try {
			return Helper.primary.load(table, q, s, n, Data.class);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
		return Beans.create();
	}

	/**
	 * atomic load data from the table and run the callback func,
	 * 
	 * @param table
	 * @param q
	 * @param s
	 * @param n
	 * @param func
	 * @return
	 */
	public static Beans<Data> load(String table, W q, int s, int n, Consumer<Beans<Data>> func) {
		Lock door = Global.getLock("data." + table);
		door.lock();
		try {

			Beans<Data> l1 = load(table, q, s, n);
			if (func != null) {
				func.accept(l1);
			}
			return l1;
		} finally {
			door.unlock();
		}
	}

	/**
	 * load the data from the table
	 * 
	 * @param table the table name
	 * @param q     the query and order
	 * @return the Data object
	 */
	public static Data load(String table, W q) {
		return Helper.primary.load(table, q, Data.class);
	}

	/**
	 * atomic load the data and call back the func
	 * 
	 * @param table
	 * @param q
	 * @param func
	 * @return
	 */
	public static Data load(String table, W q, Consumer<Data> func) {
		Lock door = Global.getLock("data." + table);
		door.lock();
		try {
			Data d = Helper.primary.load(table, q, Data.class);
			if (func != null) {
				func.accept(d);
			}
			return d;
		} finally {
			door.unlock();
		}
	}

	/**
	 * check the data exists or not
	 * 
	 * @param table the table name
	 * @param q     the query
	 * @return true if exists, else false
	 * @throws SQLException
	 */
	public static boolean exists(String table, W q) throws SQLException {
		return Helper.primary.exists(table, q);
	}

	/**
	 * update the data
	 * 
	 * @param table the tablename
	 * @param q     the query condition
	 * @param data  the json data
	 * @return the number of updated
	 * @throws SQLException
	 */
//	@Deprecated
//	public static int update(String table, W q, JSON data) throws SQLException {
//		return Data.update(table, q, V.create().copy(data));
//	}

//	@Deprecated
//	public static int update(String table, Object id, V data) throws SQLException {
//		return update(table, W.create().and(X.ID, id), data);
//	}

	/**
	 * update the data
	 * 
	 * @param table the table name
	 * @param q     the query
	 * @param data  the data
	 * @return the num which updated
	 * @throws SQLException
	 */
//	@Deprecated
//	public static int update(String table, W q, V data) throws SQLException {
//		return Helper.update(table, q, data);
//	}

	/**
	 * insert the json data to database
	 * 
	 * @param table the string of tablename
	 * @param data  the json data
	 * @return the number of inserted
	 * @throws SQLException
	 */
//	@Deprecated
//	public static int insert(String table, JSON data) throws SQLException {
//		return Data.insert(table, V.create().copy(data));
//	}

	/**
	 * insert a data
	 * 
	 * @param table the table name
	 * @param data  the data
	 * @return the num which inserted
	 * @throws SQLException
	 */
//	@Deprecated
//	public static int insert(String table, V data) throws SQLException {
//		return Helper.insert(table, data);
//	}

	/**
	 * delete data by q
	 * 
	 * @param table the table
	 * @param q     the query
	 * @return the number impacted
	 */
//	public static int delete(String table, W q) {
//		return Helper.delete(table, q);
//	}

	/**
	 * @Deprecated
	 * @param table
	 * @param q
	 * @return
	 */
//	public static int remove(String table, W q) {
//		return Helper.delete(table, q);
//	}

}
