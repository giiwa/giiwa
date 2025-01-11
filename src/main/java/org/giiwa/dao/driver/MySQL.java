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
package org.giiwa.dao.driver;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.giiwa.dao.RDSHelper;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;

public class MySQL extends RDSHelper._AbstractDriver {

	@Override
	public boolean check(String driverinfo) {
		return driverinfo.contains("mysql connector");
	}

	@Override
	public long size(Connection c, String dbname, String schema, String table) {

		if (X.isEmpty(schema)) {
			schema = dbname;
		}

		Statement stat = null;
		ResultSet r = null;
		String sql = null;
		try {
			sql = "SELECT TABLE_NAME,(DATA_LENGTH + INDEX_LENGTH) AS size FROM information_schema.TABLES where table_schema='"
					+ schema + "' and TABLE_NAME='" + table + "'";

			stat = c.createStatement();
			r = stat.executeQuery(sql);
			if (r.next()) {
				return r.getLong("size");
			}
		} catch (Exception e) {
			log.error(sql, e);
		} finally {
			RDSHelper.close(r, stat);
		}
		return -1;
	}

	@Override
	public JSON stats(Connection c, String dbname, String schema, String table) throws SQLException {

		Statement stat = null;
		ResultSet r = null;

		try {
			String sql = "select data_length as a,index_length as b, (data_length + index_length) as c from information_schema.tables where table_name = '"
					+ table + "'";

			stat = c.createStatement();
			r = stat.executeQuery(sql);
			JSON j1 = JSON.create();
			if (r.next()) {
				j1.append("storageSize", r.getLong("a"));
				j1.append("indexSize", r.getLong("b"));
				j1.append("totalSize", r.getLong("c"));
			}
			return j1;
		} finally {
			RDSHelper.close(r, stat);
		}

	}

	@Override
	public String type(String type, int size) {
		String s = super.type(type, size);
		if (X.isIn(s, "text")) {
			s = "longtext";
		}
		return s;
	}

	@Override
	public void comment(Connection c, String dbname, String schema, String tablename, String memo, List<JSON> cols)
			throws SQLException {
		Statement stat = null;

		try {

			stat = c.createStatement();
			stat.execute("alter table " + tablename + " comment '" + memo + "'");
//			for (JSON j1 : cols) {
//				String name = j1.getString("name");
//				String display = j1.getString("display");
//
//				try {
//					stat.execute(
//							"alter table " + tablename + " modifiy column " + name + " comment '" + display + "'");
//				} catch (Exception e) {
//					log.error(e.getMessage(), e);
//				}
//			}

		} finally {
			RDSHelper.close(stat);
		}
	}

	@Override
	public String addColumn(String tablename, JSON col) {

		String sql = super.addColumn(tablename, col);

		String memo = col.getString("memo");
		if (!X.isEmpty(memo)) {
			sql += " comment '" + memo + "'";
		}
		return sql;
	}

	@Override
	public String createColumn(String name, String type, int size, int key, String memo) {

		String s = super.createColumn(name, type, size, key, memo);
		if (key == 1) {
			s += " primary key";
		}
		if (!X.isEmpty(memo)) {
			s += " comment '" + memo + "'";
		}
		return s;

	}

	@Override
	public String alterTable(String dbname, String schema, String tablename, int partitions) {
		return "alter table " + dbname + "." + tablename + " partitions " + partitions;
	}

	@Override
	public List<JSON> listTables(Connection c, String dbname, String schema, String username, String tablename, int n)
			throws SQLException {

		List<JSON> l1 = JSON.createList();
		Statement stat = null;
		ResultSet r = null;

		try {

			stat = c.createStatement();
			String sql = "select * from information_schema.tables where table_schema='" + dbname + "'";

			log.info(sql);

			r = stat.executeQuery(sql);
			while (r.next()) {

				// 修复 麒麟软件 "t-solution" 的bug, schema包含“-”导致获取元数据和SQL查询数据错误
				// String schema = null;// r.getString("TABLE_SCHEMA");
				// end of bug

				String name = r.getString("table_name");
				name = name.toLowerCase();
				if (X.isEmpty(tablename) || name.matches(tablename)) {
					l1.add(JSON.create().append("name", name).append("type", r.getString("table_type"))
							.append("display", r.getString("TABLE_COMMENT"))
							.append("memo", r.getString("TABLE_COMMENT")));
				}
				if (n > 0 && l1.size() >= n) {
					break;
				}
			}
		} finally {
			RDSHelper.close(r, stat);
		}
		return l1;

	}

}
