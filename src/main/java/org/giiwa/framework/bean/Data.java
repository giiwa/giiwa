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
package org.giiwa.framework.bean;

import org.giiwa.core.bean.Bean;
import org.giiwa.core.bean.Beans;
import org.giiwa.core.bean.Helper;
import org.giiwa.core.bean.Helper.V;
import org.giiwa.core.bean.Helper.W;
import org.giiwa.core.json.JSON;

/**
 * Load "freedom" data storage, not specify. <br>
 * 
 * @author wujun
 *
 */
public class Data extends Bean {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Load data from any table
	 *
	 * @param table
	 *            the table
	 * @param q
	 *            the query and order
	 * @param s
	 *            the start of number
	 * @param n
	 *            the number of items
	 * @return the beans
	 */
	public static Beans<Data> load(String table, W q, int s, int n) {
		return Helper.load(table, q, s, n, Data.class);
	}

	/**
	 * load the data from the table
	 * 
	 * @param table
	 *            the table name
	 * @param q
	 *            the query and order
	 * @return the Data object
	 */
	public static Data load(String table, W q) {
		return Helper.load(table, q, Data.class);
	}

	/**
	 * update the data
	 * 
	 * @param table
	 *            the tablename
	 * @param q
	 *            the query condition
	 * @param data
	 *            the json data
	 * @return the number of updated
	 */
	public static int update(String table, W q, JSON data) {
		return Helper.update(table, q, V.create().copy(data));
	}

	/**
	 * insert the json data to database
	 * 
	 * @param table
	 *            the string of tablename
	 * @param data
	 *            the json data
	 * @return the number of inserted
	 */
	public static int insert(String table, JSON data) {
		return Helper.insert(table, V.create().copy(data));
	}

	/**
	 * delete data by q
	 * 
	 * @param table
	 * @param q
	 * @return
	 */
	public static int remove(String table, W q) {
		return Helper.delete(q, table, Helper.DEFAULT);
	}

}
