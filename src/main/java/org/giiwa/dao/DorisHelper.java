package org.giiwa.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.giiwa.dao.Helper.V;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.driver.Doris;
import org.giiwa.json.JSON;

public class DorisHelper extends RDSHelper {

	private Doris d = new Doris();

	protected synchronized void _checkDriver(Connection c) {
		if (driver == null) {
			try {
				driver = d;
				log.warn("driverinfo=, driver=" + driver);

			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	@Override
	public void createTable(String tablename, String memo, List<JSON> cols, JSON properties) throws SQLException {

		// create table
		Connection con = null;
		Statement stat = null;

		JSON id = X.get(cols, "name", "id");
		if (id == null) {
			throw new SQLException("primary key missed, [id]");
		}

		StringBuilder sql = new StringBuilder();

		TimeStamp t0 = TimeStamp.create();

		try {
			con = this.getConnection();
			if (con == null) {
				throw new SQLException("get connection failed!");
			}

			stat = con.createStatement();

			List<JSON> l2 = null;
			try {

				l2 = driver.listColumns(con, dbname, schema, tablename);
				if (l2 != null && !l2.isEmpty()) {

					// check columns
					for (JSON j1 : cols) {
						String name = j1.getString("name");
						JSON j2 = X.get(l2, "name", name);

						// log.info("name=" + name + ", j2=" + j2);
						if (j2 == null) {
							this.addColumn(tablename, j1);
						} else {
							String type1 = j1.getString("type");
							String type2 = j2.getString("type");
							int size1 = j1.getInt("size");
							int size2 = j2.getInt("size");

							if (!X.isSame(type1, type2)) {
								// type different
								log.warn("alert table " + tablename + ", column " + name + ", type1=" + type1
										+ ", type2=" + type2);
								this.alterColumn(tablename, j1);
							} else {
								// check size
								if (X.isIn(type1, "text")) {
									if (size1 != 0 && size2 != 0) {
										if (size1 != size2 && (size1 < 1024 || size2 < 1024)) {

											log.warn("alert table " + tablename + ", column " + name + ", type=" + type1
													+ ", size1=" + size1 + ", size2=" + size2);

											this.alterColumn(tablename, j1);
										}
									}
								} else if (X.isIn(type1, "float", "double")) {
									if (size1 != size2) {
										log.warn("alert table " + tablename + ", column " + name + ", type=" + type1
												+ ", size1=" + size1 + ", size2=" + size2 + ", j1=" + j1 + ", j2="
												+ j2);
										this.alterColumn(tablename, j1);
									}
								}
							}
						}
					}

					// check and update the comments
					driver.comment(con, dbname, schema, tablename, memo, cols);

					return;
				}
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}

			log.info("creating table [" + tablename + "], cols=" + cols);

			sql.append("create table " + tablename + " ( ");

			// id first
			{
				String name = id.getString("name");
				String type = id.getString("type");
				int size = id.getInt("size");
				String display = id.getString("display").replaceAll("'", "\"");
				String s = driver.createColumn(name, type, size, 0, display);
				sql.append(s);
			}

			for (JSON col : cols) {
				String name = col.getString("name");
				if (X.isSame(name, "id")) {
					continue;
				}
				String type = col.getString("type");
				int size = col.getInt("size");
				String display = col.getString("display").replaceAll("'", "\"");
//				int key = col.getInt("key");

				String s = driver.createColumn(name, type, size, 0, display);
				sql.append(", " + s);

			}
			sql.append(" )");
//			if (!X.isEmpty(memo)) {
//				sql.append(" comment '" + memo.replaceAll("'", "\"") + "'");
//			}
			// create table table1(a varchar(10), b varchar(10)) unique key(a) distributed
			// by hash(a) properties("enable_unique_key_merge_on_write" = "false");
			sql.append(
					" unique key(id) distributed by hash(id) properties(\"enable_unique_key_merge_on_write\" = \"false\")");

			log.info(sql.toString());

			stat.execute(sql.toString());

//			driver.comment(con, dbname, schema, tablename, memo, cols);

			if (t0.pastms() > 30000) {
				log.warn("slow30, cost=" + t0.past());
			}

		} catch (SQLException e) {
			if (e.getMessage().contains("already exists")) {
				// ignore
				return;
			}
			log.error(sql.toString(), e);
			throw new SQLException(X.getMessage(e));
		} finally {
			close(stat, con);
		}

	}

	@Override
	public int delete(String tablename, W q) {

		if (q != null && !q.isEmpty()) {
			return super.delete(tablename, q);
		}

		Connection c = null;
		Statement p = null;
		ResultSet r = null;

		try {
			c = getConnection();

			if (c == null)
				throw new SQLException("get connection failed!");

			tablename = driver.fullname(dbname, schema, tablename);
			p = c.createStatement();

			r = p.executeQuery("select count(*) n from " + tablename);
			int n = 0;
			if (r.next()) {
				n = r.getInt("n");
			}

			p.execute("truncate table " + tablename);

			return n;
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			close(r, p, c);
		}

		return -1;
	}

	@Override
	public int insertTable(String table, List<V> values) throws SQLException {

		if (values == null || values.isEmpty())
			return 0;

		/**
		 * insert it in database
		 */
		PreparedStatement p = null;
		Connection c = null;
		StringBuilder sql = new StringBuilder();

		TimeStamp t = TimeStamp.create();

		// log.info("v=" + values);

		try {
			if (X.isEmpty(values))
				return 0;

			c = getConnection();

			table = driver.fullname(dbname, schema, table);

			/**
			 * create the sql statement
			 */
			sql.append("insert into ").append(table).append(" (");
			StringBuilder s = new StringBuilder();
			int total = 0;

			for (String name : values.get(0).names()) {
				if (s.length() > 0)
					s.append(",");
				s.append(name);
				total++;
			}
			sql.append(s).append(") values( ");

			for (int i = 0; i < total - 1; i++) {
				sql.append("?, ");
			}
			sql.append("?)");

			// log.info("sql=" + sql);

			p = c.prepareStatement(sql.toString());

			int order = 1;
			for (V v : values) {

				if (v == null || v.isEmpty())
					return 0;

				for (String name : v.names()) {
					Object v1 = v.value(name);
					_setParameter(p, order++, v1, c);
				}

				p.addBatch();
			}

			int n = X.sum(p.executeBatch());

			return n;

		} catch (SQLException e) {
			log.error(values.toString(), e);
			throw e;
		} finally {
			close(p, c);
			Helper.Stat.write(table, t.pastms() / values.size());
		}

	}

}
