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
import java.util.Arrays;
import java.util.List;

import org.giiwa.dao.RDSHelper;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;

public class PG extends RDSHelper._AbstractDriver {

	@Override
	public boolean check(String driverinfo) {
		return driverinfo.contains("postgresql");
	}

	@Override
	public void comment(Connection c, String dbname, String schema, String tablename, String memo, List<JSON> cols)
			throws SQLException {

		Statement stat = null;
		try {

			stat = c.createStatement();
			if (!X.isEmpty(memo)) {
				stat.execute("comment on table " + tablename + " is '" + memo + "'");
			}
			if (cols != null && !cols.isEmpty()) {
				for (JSON col : cols) {
					stat.execute("comment on column " + tablename + "." + col.getString("name") + " is '"
							+ col.getString("display") + "'");
				}
			}

		} finally {
			RDSHelper.close(stat);
		}
	}

//	@Override
//	public Object createArrayOf(Connection c, List<?> l1) throws SQLException {
//
//		String type = "text";
//
//		Object o = l1.get(0);
//		if (o instanceof Integer || o instanceof Long) {
//			type = "bigint";
//		}
//
//		return c.createArrayOf(type, l1.toArray());
//	}

	@Override
	public JSON stats(Connection c, String dbname, String schema, String table) throws SQLException {

		Statement stat = null;
		ResultSet r = null;

		try {
			String sql = "select pg_relation_size('" + table + "') as a, pg_indexes_size('" + table + "') as b";

			stat = c.createStatement();

			if (log.isDebugEnabled()) {
				log.debug(sql);
			}

			r = stat.executeQuery(sql);
			JSON j1 = JSON.create();
			if (r.next()) {
				j1.append("storageSize", r.getLong("a"));
				j1.append("indexSize", r.getLong("b"));
				j1.append("totalSize", r.getLong("a") + r.getLong("b"));
			}
			return j1;

		} finally {
			RDSHelper.close(r, stat);
		}

	}

	@Override
	public String distributed(String table, String key) {

		return "select create_distributed_table('" + table + "', '" + key + "')";

	}

	@Override
	public String addColumn(String tablename, JSON col) {

		String name = col.getString("name");
		String type = col.getString("type");
		int size = col.getInt("size");

		return "alter table " + tablename + " add " + name + " " + type(type, size);
	}

	@Override
	public String createColumn(String name, String type, int size, int key, String memo) {
		String s = super.createColumn(name, type, size, key, memo);
		if (key == 1) {
			s += " primary key not null";
		}
		return s;
	}

	@Override
	public List<String> alertColumn(String dbname, String schema, String tablename, String name, String type,
			int size) {
		String sql = "alter table " + tablename + " alter column " + name + " type " + type(type, size);
		if (X.isIn(type, "bigint", "long", "int")) {
			sql += " using " + name + "::long";
		} else if (X.isIn(type, "date", "time", "timestamp", "datetime")) {
			sql += " using " + name + "::timestamp";
		}
		return Arrays.asList("alter table " + tablename + " drop column " + name, sql);
	}

	@Override
	public String type(String type, int size) {

		if (X.isIn(type, "string", "varchar", "text", "char")) {
			if (size < 1024) {
				return "VARCHAR(" + size + ")";
			}
			return "text";
		} else if (X.isIn(type, "long", "int")) {
			return "bigint";
		} else if (X.isIn(type, "double", "float")) {
			return "decimal(15, " + size + ")";
		} else if (X.isIn(type, "date", "time", "datetime", "timestamp")) {
			return "timestamp";
		} else if (X.isIn(type, "file", "url", "image", "video", "audio")) {
			return "varchar(512)";
		}

		return type;

	}

	@Override
	public List<JSON> listTables(Connection c, String dbname, String schema, String username, String name, int n)
			throws SQLException {

		Statement stat = null;
		ResultSet r = null;

		List<JSON> l1 = JSON.createList();

		try {

			stat = c.createStatement();

			String sql = "SELECT table_schema, table_name, table_type FROM information_schema.tables where table_schema='"
					+ schema + "' or table_schema='public'";

			log.info(sql);

			r = stat.executeQuery(sql);
			while (r.next()) {
				String owner = r.getString("table_schema");
				String tablename = r.getString("table_name");
				if (!X.isEmpty(owner)) {
					tablename = owner + "." + tablename;
				}
				String type = r.getString("table_type");
				if (X.isEmpty(name) || tablename.matches(name)) {
					l1.add(JSON.create().append("type", type).append("name", tablename).append("display", tablename)
							.append("memo", tablename));
				}
				if (n > 0 && l1.size() >= 1000) {
					break;
				}
			}
		} finally {
			RDSHelper.close(r, stat);
		}

		return l1;
	}

}
