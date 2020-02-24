package org.giiwa.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.giiwa.bean.GLog;
import org.giiwa.dao.Beans;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.DBHelper;
import org.giiwa.dao.Helper.W;
import org.giiwa.engine.JS;
import org.giiwa.json.JSON;
import org.giiwa.misc.StringFinder;

public class SQL {

	static Log log = LogFactory.getLog(SQL.class);

	/**
	 * 
	 * @param h   the db helper
	 * @param sql "select * from [table] where a=1 orderby b [desc] offset 1 limit
	 *            10";
	 * @return the list of Bean
	 */
	public static List<Bean> query(DBHelper h, String sql) {

		try {
			JSON q = _sql(StringFinder.create(sql));

			if (log.isDebugEnabled())
				log.debug("q=" + q);

			String cols = q.getString("cols");

			W q1 = where2W(StringFinder.create(q.getString("where")));
			if (q.containsKey("orderby")) {
				String order = q.getString("orderby");
				if (!X.isEmpty(order)) {
					String[] ss = X.split(order, ",");
					if (ss != null) {
						if (q1 == null)
							q1 = W.create();

						for (String s : ss) {
							String[] ss1 = X.split(s, " ");
							if (ss1.length > 1) {
								if (X.isSame(ss1[1], "desc")) {
									q1.sort(ss1[0], -1);
								} else {
									q1.sort(ss1[0], 1);
								}
							} else {
								q1.sort(ss1[0], 1);
							}
						}
					}
				}
			}

			Beans<Bean> bs = h.load(q.getString("tablename"), X.isSame("*", cols) ? null : X.split(cols, "[,]"), q1,
					q.getInt("offset", 0), q.getInt("limit", 10), Bean.class, X.EMPTY);

			return bs;

		} catch (Exception e) {
			log.error(sql, e);
			GLog.applog.error("sql", "query", sql, e, null, null);
		}
		return null;

	}

	private static JSON _sql(StringFinder sf) throws SQLException {

		if (log.isDebugEnabled())
			log.debug("select ...");
		sf.nextTo(" ");
		sf.trim();
		String cols = sf.nextTo("from");
		if (X.isEmpty(cols)) {
			throw new SQLException("unknow column [" + cols + "]");
		}

		JSON r = JSON.create();
		r.put("cols", cols);

		sf.trim();
		sf.nextTo(" ");
		sf.trim();
		String table = sf.nextTo(" ");
		if (X.isEmpty(table)) {
			throw new SQLException("unknow table [" + table + "]");
		}
		r.put("tablename", table);

		return _condition(sf, r);
	}

	private static JSON _condition(StringFinder sf, JSON r) throws SQLException {
		sf.trim();
		String s = sf.nextTo(" ");
		if (X.isEmpty(s)) {
			return r;
		}

		if (X.isSame(s, "where")) {
			sf.trim();
			String w = sf.nextTo("group|order|offset|limit");
			if (!X.isEmpty(w)) {
				r.put("where", w);
				return _condition(sf, r);
			} else {
				throw new SQLException("error where [" + w + "]");
			}
		} else if (X.isSame(s, "groupby")) {
			sf.trim();
			String g = sf.nextTo("order|offset|limit");
			if (!X.isEmpty(g)) {
				r.put("groupby", g);
				return _condition(sf, r);
			} else {
				throw new SQLException("error groupby [" + g + "]");
			}
		} else if (X.isSame(s, "order")) {
			sf.trim();
			String by = sf.nextTo(" ");
			if (X.isSame("by", by)) {
				String o = sf.nextTo("offset|limit");
				if (!X.isEmpty(o)) {
					r.put("orderby", o);
					return _condition(sf, r);
				} else {
					throw new SQLException("error order [" + o + "]");
				}
			} else {
				throw new SQLException("error order [" + by + "]");
			}
		} else if (X.isSame(s, "offset")) {
			sf.trim();
			String o = sf.nextTo("limit");
			if (!X.isEmpty(o)) {
				r.put("offset", X.toInt(o));
				return _condition(sf, r);
			} else {
				throw new SQLException("error offset [" + o + "]");
			}
		} else if (X.isSame(s, "limit")) {
			sf.trim();
			String o = sf.nextTo(" ");
			if (!X.isEmpty(o)) {
				r.put("limit", X.toInt(o));
				return _condition(sf, r);
			} else {
				throw new SQLException("error limit [" + o + "]");
			}
		} else {
			throw new SQLException("unknow key [" + s + "]");
		}
	}

	public static W where2W(StringFinder s) throws Exception {
		// “name=’2’ and age>10 or (name=‘1‘ and age<20) or name like ‘dd%’;
		if (s == null || !s.hasMore()) {
			return W.create();
		}

		Stack<String> conn = new Stack<String>();
		W q = W.create();

		while (s.hasMore()) {
			s.trim();
			char c = s.next();
			if (c == '(') {
				W q1 = where2W(s);
				if (q1 != null && !q1.isEmpty()) {
					String o = conn.isEmpty() ? "and" : conn.pop();
					if (X.isSame(o, "and")) {
						q.and(q1);
					} else {
						q.or(q1);
					}
				}
			} else if (c == ')') {
				return q;
			} else {
				s.skip(-1);

				StringFinder s1 = StringFinder.create(s.nextTo("(|)| and | or | not | AND | OR | NOT "));
				s.trim();

				// if (!s.hasMore())
				// throw new Exception("expect [op] more after [" + s1.toString() +
				// "]");

				String name = s1.nextTo(" |!|=|>|<|like|LIKE");

				s1.trim();
				c = s1.next();
				String op = null;
				if (c == '=') {
					op = "=";
				} else if (c == '>' || c == '<' || c == '!') {
					char c1 = s1.next();
					if (c1 == '=') {
						op = Character.toString(c) + c1;
					} else {
						s1.skip(-1);
						op = Character.toString(c);
					}
				} else {
					// like ?
					s1.skip(-1);
					op = s1.nextTo(" ");
				}

				s1.trim();
				c = s1.next();

				Object value = null;
				if (c == '\'') {
					String s2 = s1.pair('\'');
					if (s2.indexOf("|") > -1) {
						value = X.split(s2, "\\|");
					} else {
						value = s2;
					}
				} else {
					s1.skip(-1);

					String s2 = s1.remain();

					if (s2.indexOf("|") > -1) {
						value = X.asList(X.split(s2, "\\|"), e -> {
							try {
								return JS.calculate(e.toString());
							} catch (Exception e1) {
//								log.error(e1.getMessage(), e1);
								return e;
							}
						});
					} else {
						try {
							value = JS.calculate(s2);
						} catch (Exception e) {
//							log.error(e.getMessage(), e);
							value = s2;
						}
					}
				}

				String o = conn.isEmpty() ? "and" : conn.pop();
				if (X.isSame(o, "and")) {
					if (X.isSame(op, "=")) {
						q.and(name, value);
					} else if (X.isSame(op, "!=")) {
						q.and(name, value, W.OP.neq);
					} else if (X.isSame(op, ">")) {
						q.and(name, value, W.OP.gt);
					} else if (X.isSame(op, ">=")) {
						q.and(name, value, W.OP.gte);
					} else if (X.isSame(op, "<")) {
						q.and(name, value, W.OP.lt);
					} else if (X.isSame(op, "<=")) {
						q.and(name, value, W.OP.lte);
					} else if (X.isSame(op, "<")) {
						q.and(name, value, W.OP.lt);
					} else if (X.isSame(op, "like")) {
						q.and(name, value, W.OP.like);
					} else if (X.isSame(op, "like_")) {
						q.and(name, value, W.OP.like_);
					} else if (X.isSame(op, "like_$")) {
						q.and(name, value, W.OP.like_$);
					}
				} else {
					if (X.isSame(op, "=")) {
						q.or(name, value);
					} else if (X.isSame(op, "!=")) {
						q.or(name, value, W.OP.neq);
					} else if (X.isSame(op, ">")) {
						q.or(name, value, W.OP.gt);
					} else if (X.isSame(op, ">=")) {
						q.or(name, value, W.OP.gte);
					} else if (X.isSame(op, "<")) {
						q.or(name, value, W.OP.lt);
					} else if (X.isSame(op, "<=")) {
						q.or(name, value, W.OP.lte);
					} else if (X.isSame(op, "<")) {
						q.or(name, value, W.OP.lt);
					} else if (X.isSame(op, "like")) {
						q.or(name, value, W.OP.like);
					} else if (X.isSame(op, "like_")) {
						q.or(name, value, W.OP.like_);
					} else if (X.isSame(op, "like_$")) {
						q.or(name, value, W.OP.like_$);
					}
				}
			}

			s.trim();
			// get conn
			if (s.hasMore()) {
				c = s.next();
				if (c == ')') {
					return q;
				}

				s.skip(-1);
				String s1 = s.nextTo(" ");
				conn.push(s1);
			}

		}

		return q;
	}

	public static W where2W(String name, StringFinder s) throws Exception {
		// “’2’ and >10 or (‘1‘ and <20) ;
		if (s == null || !s.hasMore()) {
			return W.create();
		}

		Stack<String> conn = new Stack<String>();
		W q = W.create();

		while (s.hasMore()) {
			s.trim();
			char c = s.next();
			if (c == '(') {
				W q1 = where2W(name, s);
				if (q1 != null) {
					String o = conn.isEmpty() ? "and" : conn.pop();
					if (X.isSame(o, "and")) {
						q.and(q1);
					} else {
						q.or(q1);
					}
				}
			} else if (c == ')') {
				return q;
			} else {
				s.skip(-1);

				String s1 = s.nextTo("(|)|and|or");
				s.trim();

				s1.trim();
				c = s1.charAt(0);
				String op = null;
				if (c == '>' || c == '<') {
					char c1 = s1.charAt(1);
					if (c1 == '=') {
						op = Character.toString(c) + c1;
						s1 = s1.substring(2);
					} else {
						op = Character.toString(c);
						s1 = s1.substring(1);
					}
				} else if (c == '!') {
					op = "!=";
					s1 = s1.substring(1);
				} else {
					// like ?
					op = "=";
				}

				s1.trim();

				Object value = s1;

				String o = conn.isEmpty() ? "and" : conn.pop();
				if (X.isSame(o, "and")) {
					if (X.isSame(op, "=")) {
						q.and(name, value);
					} else if (X.isSame(op, "!=")) {
						q.and(name, value, W.OP.neq);
					} else if (X.isSame(op, ">")) {
						q.and(name, value, W.OP.gt);
					} else if (X.isSame(op, ">=")) {
						q.and(name, value, W.OP.gte);
					} else if (X.isSame(op, "<")) {
						q.and(name, value, W.OP.lt);
					} else if (X.isSame(op, "<=")) {
						q.and(name, value, W.OP.lte);
					} else if (X.isSame(op, "<")) {
						q.and(name, value, W.OP.lt);
					} else if (X.isSame(op, "like")) {
						q.and(name, value, W.OP.like);
					} else if (X.isSame(op, "like_")) {
						q.and(name, value, W.OP.like_);
					} else if (X.isSame(op, "like_$")) {
						q.and(name, value, W.OP.like_$);
					}
				} else {
					if (X.isSame(op, "=")) {
						q.or(name, value);
					} else if (X.isSame(op, "!=")) {
						q.or(name, value, W.OP.neq);
					} else if (X.isSame(op, ">")) {
						q.or(name, value, W.OP.gt);
					} else if (X.isSame(op, ">=")) {
						q.or(name, value, W.OP.gte);
					} else if (X.isSame(op, "<")) {
						q.or(name, value, W.OP.lt);
					} else if (X.isSame(op, "<=")) {
						q.or(name, value, W.OP.lte);
					} else if (X.isSame(op, "<")) {
						q.or(name, value, W.OP.lt);
					} else if (X.isSame(op, "like")) {
						q.or(name, value, W.OP.like);
					} else if (X.isSame(op, "like_")) {
						q.or(name, value, W.OP.like_);
					} else if (X.isSame(op, "like_$")) {
						q.or(name, value, W.OP.like_$);
					}
				}
			}

			s.trim();
			// get conn
			if (s.hasMore()) {
				c = s.next();
				if (c == ')') {
					return q;
				}

				s.skip(-1);
				String s1 = s.nextTo(" ");
				conn.push(s1);
			}

		}

		return q;
	}

	/**
	 * @param h   the db helper
	 * @param sql the sql
	 * @return the Bean
	 * @throws SQLException
	 */
	public static Bean get(DBHelper h, String sql) throws SQLException {

		try {

			JSON q = _sql(StringFinder.create(sql));
			W q1 = where2W(StringFinder.create(q.getString("where")));
			if (q.containsKey("orderby")) {
				String order = q.getString("orderby");
				if (!X.isEmpty(order)) {
					String[] ss = X.split(order, ",");
					if (ss != null) {
						if (q1 == null)
							q1 = W.create();

						for (String s : ss) {
							String[] ss1 = X.split(s, " ");
							if (ss1.length > 1) {
								if (X.isSame(ss1[1], "desc")) {
									q1.sort(ss1[0], -1);
								} else {
									q1.sort(ss1[0], 1);
								}
							} else {
								q1.sort(ss1[0], 1);
							}
						}
					}
				}
			}

			return h.load(q.getString("tablename"), null, q1, Bean.class, X.EMPTY);
		} catch (Exception e) {
			log.error(sql, e);
		}
		return null;

	}

	public static int execute(DBHelper h, String sql) throws SQLException {

		Statement stat = null;

		try {
			Connection con = null;
			if (h instanceof RDSHelper) {
				con = ((RDSHelper) h).getConnection();
			}
			if (con != null) {
				stat = con.createStatement();
				return stat.executeUpdate(sql);
			}
		} finally {
			RDSHelper.inst.close(stat);
		}
		return -1;
	}

	public static void main(String[] args) throws Exception {
		String s = "a>10/2*5 and b>11 and (c>1 or r>2)";
		W q = SQL.where2W(StringFinder.create(s));
		System.out.println(q);

		// SQL.query(null, "select *");
		JSON q1 = _sql(StringFinder.create("select a,b,c from gi_oplog order by a limit 10"));
		System.out.println(q1);

		// SQL.query(h, sql);
		s = "11 and (1 or 2) and !12";

		q = SQL.where2W("a", StringFinder.create(s));
		System.out.println(q);

		q = W.create();
		q.and("cate='J1|J3/7'");
		System.out.println(q);
		q.scan(e -> {
			if (X.isSame("cate", e.name)) {
				if (e.value.toString().indexOf("/") > -1) {
//					e.remove();
					String[] ss = X.range(e.value.toString(), "/");
					e.replace(ss);
				}
			}
		});
		System.out.println(q);

		s = "a='a(b)'";
		q = SQL.where2W(StringFinder.create(s));
		System.out.println(q);

	}

	// private static String[] KW = { "and", "or", "like", "=", "!=", ">", ">=",
	// "<", "<=" };

}
