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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.giiwa.dao.RDSHelper;
import org.giiwa.dao.TimeStamp;
import org.giiwa.dao.X;
import org.giiwa.json.JSON;

/**
 * 达梦数据库
 * 
 * @author joe
 *
 */
public class DM extends RDSHelper._AbstractDriver {

	@Override
	public boolean check(String driverinfo) {
		return driverinfo.contains("dmdriver");
	}

	@Override
	public String createColumn(String name, String type, int size, int key, String memo) {

		String s = super.createColumn(name, type, size, key, memo);
		if (key == 1) {
			s += " primary key";
		}
		return s.toUpperCase();

	}

	@Override
	public String fullname(String dbname, String schema, String name) {
//		if (X.isEmpty(schema)) {
		schema = dbname;
//		}
		if (name.toUpperCase().startsWith(schema.toUpperCase() + ".")) {
			return name.toUpperCase();
		}
		return (schema + "." + name).toUpperCase();
	}

	@Override
	public void comment(Connection c, String dbname, String schema, String tablename, String memo, List<JSON> cols)
			throws SQLException {
		Statement stat = null;
		tablename = fullname(dbname, schema, tablename);
		String sql = null;

		try {
			stat = c.createStatement();
			if (!X.isEmpty(memo)) {
				sql = "COMMENT ON TABLE " + tablename + " is '" + memo.replaceAll("'", "\"") + "'";
//				log.info("sql=" + sql);
				stat.execute(sql);
			}

			for (JSON col : cols) {
//				stat.execute("ALTER TABLE " + schema + "." + tablename + " MODIFY COLUMN "
//						+ col.getString("name").toUpperCase() + " COMMENT '" + col.getString("display") + "'");
				sql = "COMMENT ON COLUMN " + (tablename + "." + col.getString("name")).toUpperCase() + " is '"
						+ col.getString("display").replaceAll("'", "\"") + "'";
//				log.info("sql=" + sql);
				stat.execute(sql);
			}
		} catch (Exception e) {
			log.error(sql, e);
			throw e;
		} finally {
			RDSHelper.close(stat);
		}

	}

	@Override
	public String addColumn(String tablename, JSON col) {
		return super.addColumn(tablename, col).toUpperCase();
	}

	@Override
	public List<String> alertColumn(String dbname, String schema, String tablename, String name, String type,
			int size) {
//		if (X.isEmpty(schema)) {
		schema = dbname;
//		}
		if (!tablename.toUpperCase().startsWith(schema.toUpperCase() + ".")) {
			tablename = (schema + "." + tablename).toUpperCase();
		}
		return Arrays
				.asList("ALTER TABLE " + tablename + " MODIFY (" + name.toUpperCase() + " " + type(type, size) + ")");
	}

	@Override
	public List<JSON> listColumns(Connection con, String dbname, String schema, String table) throws SQLException {

		List<JSON> l1 = JSON.createList();
		Statement stat = null;
		ResultSet r = null;

//		if (X.isEmpty(schema)) {
		schema = dbname;
//		}
//		schema = schema.toUpperCase();
//		table = table.toUpperCase();
		String sql = null;

		try {
			// 获取字段信息
			{
				int i = table.lastIndexOf(".");
				if (i > 0) {
					table = table.substring(i + 1);
				}
			}
			stat = con.createStatement();
			sql = "SELECT table_name, column_name, data_type, data_length, data_precision, data_scale,nullable FROM ALL_TAB_COLUMNS where owner='"
					+ schema + "' and table_name='" + table + "'";

//			log.info(sql);
			r = stat.executeQuery(sql);

			while (r.next()) {
				JSON j1 = JSON.create();
				j1.append("name", r.getString("column_name"));
				j1.append("nullable", getdata(r, "nullable"));

				String type1 = (String) getdata(r, "data_type");
				if (!X.isEmpty(type1)) {
					type1 = type1.toUpperCase();
				}
				j1.append("type1", type1);
				int i = type1.lastIndexOf("(");
				if (i > 0) {
					type1 = type1.substring(0, i);
				}
				String type = type(type1);
				j1.append("type", type);

				// get size
				int size = r.getInt("data_length");
				if (X.isIn(j1.getString("type"), "double", "decimal", "real", "float")) {
					size = r.getInt("data_scale");
				}
				j1.append("size", size);

				String display = j1.getString("name");
				j1.put("display", display);

				l1.add(j1);
			}

			for (JSON j1 : l1) {
				String name = j1.getString("name");

				sql = "select 1 from all_constraints a, all_cons_columns b where a.owner='" + schema
						+ "' and a.table_name='" + table
						+ "' and a.constraint_type='P' and b.owner=a.owner and b.table_name=a.table_name and b.column_name='"
						+ name + "' ";
				r = stat.executeQuery(sql);
				if (r.next()) {
					j1.put("key", 1);
				}
				r.close();
				r = null;

				sql = "select comments from all_col_comments where owner='" + schema + "' and table_name='" + table
						+ "' and column_name='" + name + "'";
				r = stat.executeQuery(sql);
				if (r.next()) {
					String display = r.getString("comments");
					if (!X.isEmpty(display)) {
						j1.put("display", display);
					}
				}
				r.close();
				r = null;
			}
		} catch (Exception e) {
			log.error(sql, e);
			throw e;
		} finally {
			RDSHelper.close(r, stat);
		}
		return l1;
	}

	@Override
	public List<JSON> listTables(Connection c, String dbname, String schema, String username, String tablename, int n)
			throws SQLException {

		// 达梦数据库

		List<JSON> l1 = JSON.createList();
		Statement stat = null;
		ResultSet r = null;

		try {
			stat = c.createStatement();
//			if (X.isEmpty(schema)) {
			schema = dbname;
//			}
//			schema = schema.toUpperCase();
			String sql = "select owner, table_name from all_tables where owner='" + schema + "'";

			log.info(sql);

			r = stat.executeQuery(sql);
			while (r.next()) {
				String owner = r.getString("owner");
				String name = r.getString("table_name");
				String comments = name;
				if (X.isEmpty(tablename) || name.matches(tablename)) {
					l1.add(JSON.create().append("name", name).append("type", "TABLE").append("display", comments)
							.append("memo", comments).append("owner", owner));
				}
				if (n > 0 && l1.size() >= n) {
					break;
				}
			}
			r.close();
			r = null;

			if (n < 0 || l1.size() < n) {
				sql = "select owner, view_name from all_views where owner='" + schema + "'";
				r = stat.executeQuery(sql);
				while (r.next()) {
					String owner = r.getString("owner");
					String name = r.getString("view_name");
					String comments = null;

					if (!X.isEmpty(owner)) {
						name = owner + "." + name;
					}
					if (X.isEmpty(tablename) || name.matches(tablename)) {
						l1.add(JSON.create().append("name", name).append("type", "VIEW").append("display", comments)
								.append("memo", comments));
					}
					if (n > 0 && l1.size() >= n) {
						break;
					}
				}
				r.close();
				r = null;

			}

			for (JSON j1 : l1) {
				String name = j1.getString("name");
				String type = j1.getString("type");
				r = stat.executeQuery("select comments from all_tab_comments where owner='" + schema
						+ "' and table_type='" + type + "' and table_name='" + name + "'");
				if (r.next()) {
					String comments = r.getString("comments");
					if (!X.isEmpty(comments)) {
						j1.put("display", comments);
						j1.put("memo", comments);
					}
				}
				r.close();
				r = null;
			}

		} finally {
			RDSHelper.close(r, stat);
		}
		return l1;
	}

	@Override
	public String load(String table, String fields, String where, String orderby, int offset, int limit) {

		StringBuilder sql = new StringBuilder();
		sql.append("select ");
		if (X.isEmpty(fields)) {
			sql.append(" * ");
		} else {
			sql.append(fields + " ");
		}

		if (limit > 0) {
			// select * from config.d_alert order by created desc offset 60 limit 10
			sql.append("from " + table + " ");
			if (!X.isEmpty(where)) {
				sql.append(" where " + where);
			}
			if (!X.isEmpty(orderby)) {
				sql.append(" " + orderby);
			}
			sql.append(" offset " + offset + " limit " + limit);

			// select * from (select rownum rn,a.* from table_name a where rownum < limit)
			// where rn >= offset;
//			sql.append("from (select ");
//			if (X.isEmpty(orderby)) {
//				sql.append("rownum rn,");
//			} else {
//				sql.append("row_number() over (" + orderby + ") rn,");
//			}
//			sql.append("a.* from ").append(table).append(" a ");
//			if (!X.isEmpty(where)) {
//				sql.append(" where ");
//				sql.append("(").append(where).append(") ");
//			}
//
//			if (offset < 0) {
//				offset = 0;
//			}
//
//			// 注意， 这不能用 rownum, 因为外层也有自己的rownum
//			sql.append(") where rn>" + offset);
//			if (limit > 0) {
//				int n = limit + offset + 1;
//				if (n > 0) {
//					sql.append(" and rn<" + n);
//				}
//			}

		} else {

			sql.append("from ").append(table);
			if (!X.isEmpty(where)) {
				sql.append(" where ").append(where);
			}

			if (!X.isEmpty(orderby)) {
				sql.append(" ").append(orderby);
			}
		}

		return sql.toString();

	}

	@Override
	public String cursor(String table, String where, String orderby, long offset) {

		StringBuilder sql = new StringBuilder();
		// select * from (select rownum rn,a.* from table_name a where rownum < limit)
		// where rn >= offset;
		sql.append("select * from (select ");
		if (X.isEmpty(orderby)) {
			sql.append("rownum rn,");
		} else {
			sql.append("row_number() over (" + orderby + ") rn,");
		}
		sql.append("a.* from ").append(table).append(" a ");

		if (!X.isEmpty(where)) {
			sql.append(" where ");
			sql.append("(").append(where).append(") ");
		}

		sql.append(") where rn>").append(offset);

		return sql.toString();
	}

	@Override
	public List<Map<String, Object>> getIndexes(Connection c, String dbname, String schema, String tablename)
			throws SQLException {

		List<Map<String, Object>> l1 = new ArrayList<Map<String, Object>>();

//		if (X.isEmpty(schema)) {
		schema = dbname;
//		}
//		schema = schema.toUpperCase();
//		tablename = tablename.toUpperCase();

		Statement stat = null;
		ResultSet r = null;
		try {

			// SELECT b.object_id FROM all_indexes a, dba_objects b where a.owner='DL' and
			// a.table_name='DM_TEST2' and b.object_name=a.index_name
			// select index_name from all_indexes where owner='DL' and table_name='DM_TEST2'
			// select COLUMN_NAME,DESCEND from all_ind_columns where
			// index_name='GI_CONFIG_INDEX_CQJPHFRSQZVGF' order by COLUMN_POSITION
			// select COLUMN_NAME,DESCEND from all_ind_columns where
			// index_name='INDEX33555473' order by COLUMN_POSITION

			String sql = "SELECT index_name FROM all_indexes where owner='" + schema + "' and table_name='" + tablename
					+ "'";
			stat = c.createStatement();
			r = stat.executeQuery(sql);

			List<String> l2 = new ArrayList<String>();
			while (r.next()) {
				String name = r.getString("index_name");
				l2.add(name);
			}
			r.close();
			r = null;

			for (String name : l2) {

				sql = "select COLUMN_NAME,DESCEND from all_ind_columns where index_name='" + name
						+ "' order by COLUMN_POSITION";
				r = stat.executeQuery(sql);
				name = name.toLowerCase();

				Map<String, Object> m = new LinkedHashMap<String, Object>();
				while (r.next()) {
					m.put(r.getString("column_name").toLowerCase(), X.isSame(r.getString("DESCEND"), "ASC") ? 1 : -1);
				}
				r.close();
				r = null;

				if (!m.isEmpty()) {
					Map<String, Object> m1 = new LinkedHashMap<String, Object>();
					m1.put("name", name);
					m1.put("key", m);
					l1.add(m1);
				}
			}

		} finally {
			RDSHelper.close(r);
		}

		log.info("got index, table=" + tablename + ", result=" + l1);

		return l1;

	}

	@Override
	public long size(Connection c, String dbname, String schema, String table) {

//		if (X.isEmpty(schema)) {
		schema = dbname;
//		}
//		schema = schema.toUpperCase();
		Statement stat = null;
		ResultSet r = null;
		String sql = null;
		TimeStamp t0 = TimeStamp.create();

		try {

			// 可能性能问题， 不知道啥情况
			sql = "select bytes from dba_segments where owner='" + schema + "' and segment_name='" + table + "'";

			stat = c.createStatement();
			r = stat.executeQuery(sql);
			if (r.next()) {
				return r.getLong("bytes");
			}

		} catch (Exception e) {
			log.error(sql, e);
		} finally {
			RDSHelper.close(r, stat);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past() + ", sql=" + sql);
			}

		}
		return -1;
	}

	@Override
	public JSON stats(Connection c, String dbname, String schema, String table) throws SQLException {

//		if (X.isEmpty(schema)) {
		schema = dbname;
//		}
//		schema = schema.toUpperCase();

		Statement stat = null;
		ResultSet r = null;
		TimeStamp t0 = TimeStamp.create();
		String sql = null;

		try {
			JSON j1 = JSON.create();
			// 可能性能问题， 不知道啥情况
			j1.append("storageSize", size(c, dbname, schema, table));

			sql = "SELECT index_name FROM all_indexes where owner='" + schema + "' and table_name='" + table + "'";
			stat = c.createStatement();
			r = stat.executeQuery(sql);

			List<String> l2 = new ArrayList<String>();
			while (r.next()) {
				String name = r.getString("index_name");
				l2.add(name);
			}
			r.close();
			r = null;

			long size = 0;
			for (String name : l2) {
				size += this.size(c, dbname, schema, name);
			}
			j1.append("indexSize", size);
			j1.append("totalSize", size + j1.getLong("storageSize"));

			return j1;
		} finally {
			RDSHelper.close(r, stat);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past() + ", sql=" + sql);
			}

		}
	}

}
