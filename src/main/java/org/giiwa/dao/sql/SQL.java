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
package org.giiwa.dao.sql;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bson.types.ObjectId;
import org.giiwa.bean.GLog;
import org.giiwa.bean.Stat;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.sql.SqlParser.ColumnsContext;
import org.giiwa.dao.sql.SqlParser.DescContext;
import org.giiwa.dao.sql.SqlParser.ExprContext;
import org.giiwa.dao.sql.SqlParser.GroupContext;
import org.giiwa.dao.sql.SqlParser.LimitContext;
import org.giiwa.dao.sql.SqlParser.NowContext;
import org.giiwa.dao.sql.SqlParser.NullContext;
import org.giiwa.dao.sql.SqlParser.ObjectidContext;
import org.giiwa.dao.sql.SqlParser.OffsetContext;
import org.giiwa.dao.sql.SqlParser.OrderContext;
import org.giiwa.dao.sql.SqlParser.SelectContext;
import org.giiwa.dao.sql.SqlParser.SetContext;
import org.giiwa.dao.sql.SqlParser.SetvalueContext;
import org.giiwa.dao.sql.SqlParser.ShowContext;
import org.giiwa.dao.sql.SqlParser.ShowoptionsContext;
import org.giiwa.dao.sql.SqlParser.StatContext;
import org.giiwa.dao.sql.SqlParser.TablenameContext;
import org.giiwa.dao.sql.SqlParser.TimeContext;
import org.giiwa.dao.sql.SqlParser.TodateContext;
import org.giiwa.dao.sql.SqlParser.TodayContext;
import org.giiwa.dao.sql.SqlParser.TolongContext;
import org.giiwa.dao.sql.SqlParser.TostringContext;
import org.giiwa.dao.sql.SqlParser.UuidContext;
import org.giiwa.dao.sql.SqlParser.ValContext;
import org.giiwa.json.JSON;
import org.giiwa.web.Language;

public class SQL {

	private static final Log log = LogFactory.getLog(SQL.class);

	private static Language lang = Language.getLanguage("zh_cn");

	public static W parse(String sql) throws SQLException {

		SqlLexer lexer = new SqlLexer(CharStreams.fromString(sql));
		SqlParser parser = new SqlParser(new CommonTokenStream(lexer));

		SQLException[] ex = new SQLException[] { null };
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				ex[0] = new SQLException(msg + "\n" + sql);
			}

		});
		StatContext s1 = parser.stat();
		if (ex[0] != null) {
			throw ex[0];
		}

		SQLVisitor sv = new SQLVisitor();
		return (W) sv.visit(s1);

	}

	static class SQLVisitor extends SqlBaseVisitor<Object> {

		private W q = W.create();
		private ValVisitor vv = new ValVisitor();

		@Override
		public W visitStat(StatContext ctx) {
			super.visitStat(ctx);
			return q;
		}

		@Override
		public Object visitDesc(DescContext ctx) {
			q.command = "desc";
			q.params = Arrays.asList(ctx.tablename().getText());
			return q;
		}

		@Override
		public W visitShow(ShowContext ctx) {
			q.command = "show";
			ShowoptionsContext ssc = ctx.showoptions();
			if (ssc != null) {
				TablenameContext tc = ssc.tablename();
				if (tc != null) {
					q.query(tc.getText());
				}
			}
			q.params = new ArrayList<Object>();
			for (int i = 0; i < ssc.getChildCount(); i++) {
				q.params.add(ssc.getChild(i).getText());
			}

			return q;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object visitSet(SetContext ctx) {

			q.command = "set";

			SetvalueContext val = ctx.setvalue();
			if (val != null) {
				q.params = (List<Object>) this.visitSetvalue(val);
			}
			return q;
		}

		@SuppressWarnings("unchecked")
		@Override
		public Object visitSetvalue(SetvalueContext ctx) {
			// NAME (NAME|val) (',' setvalue)*

			Object value = null;
			List<TerminalNode> ns = ctx.NAME();
			String name = ns.get(0).getText();

			if (ns.size() > 1) {
				value = ns.get(1).getText();
			} else {
				ValContext val = ctx.val();
				if (val != null) {
					value = vv.visitVal(val);
				}
			}

			List<JSON> l1 = JSON.createList();
			l1.add(JSON.create().append(name, value));
			List<SetvalueContext> l2 = ctx.setvalue();
			if (l2 != null) {
				for (SetvalueContext s : l2) {
					Object v = this.visitSetvalue(s);
					if (v != null) {
						l1.addAll((List<JSON>) v);
					}
				}
			}

			return l1;
		}

		@Override
		public W visitSelect(SelectContext ctx) {
			q.command = "select";

			ValContext val = ctx.val();
			if (val != null) {
				q.params = Arrays.asList(vv.visitVal(val));
				return q;
			} else {
				return (W) super.visitSelect(ctx);
			}
		}

		@Override
		public W visitGroup(GroupContext ctx) {
			q.groupby(ctx.getText());
			return q;
		}

		@Override
		public W visitColumns(ColumnsContext ctx) {
			q.fields(ctx.getText());
			return q;
		}

		@Override
		public W visitTablename(TablenameContext ctx) {
			String table = ctx.NAME().getText();
			if (!X.isIn(table, dump)) {
				q.query(table);
			}
			return q;
		}

		@Override
		public W visitExpr(ExprContext ctx) {

			if (q == null) {
				q = W.create();
			}

			List<ExprContext> l1 = ctx.expr();
			if (l1.size() == 2) {
				// expr and|or expr
				W q0 = this.q;
				this.q = null;
				W q1 = (W) l1.get(0).accept(this);
				this.q = null;
				W q2 = (W) l1.get(1).accept(this);
				String cond = ctx.cond == null ? "AND" : ctx.cond.getText();
				if (X.isIn(cond, "and")) {
					q1.command = q0.command;
					q1.params = q0.params;
					q1.table = q0.table;
					q1.fields(q0.fields());
					q0 = q1;
					q0.and(q2);
				} else {
					q1.command = q0.command;
					q1.params = q0.params;
					q1.table = q0.table;
					q1.fields(q0.fields());
					q0 = q1;
					q0.or(q2);
				}
				this.q = q0;

			} else if (l1.size() == 1) {

				Token not = ctx.not;
				if (not != null) {
					// not expr
					W q0 = this.q;
					this.q = null;
					W q1 = (W) l1.get(0).accept(this);
					if (!q1.isEmpty()) {
						q0.and(q1, W.NOT);
					}
					this.q = q0;
				} else {
					// (expr)
					W q0 = this.q;
					this.q = null;
					W q1 = (W) l1.get(0).accept(this);
					if (q0 == null || q0.isEmpty()) {
						q1.command = q0.command;
						q1.params = q0.params;
						q1.table = q0.table;
						q1.fields(q0.fields());
						q0 = q1;
					} else if (!q1.isEmpty()) {
						q0.and(q1);
					}
					this.q = q0;
				}
			} else {

				String cond = ctx.cond == null ? "AND" : ctx.cond.getText();
				String op = ctx.op == null ? null : ctx.op.getText();

				try {
					if (op != null) {

						ValContext val = ctx.val();
						Object v = val.accept(vv);

						W.OP op1 = null;
						int op2 = W.AND;

						if (X.isIn(op, ">")) {
							op1 = W.OP.gt;
						} else if (X.isIn(op, ">=")) {
							op1 = W.OP.gte;
						} else if (X.isIn(op, "<")) {
							op1 = W.OP.lt;
						} else if (X.isIn(op, "<=")) {
							op1 = W.OP.lte;
						} else if (X.isIn(op, "like")) {
							op1 = W.OP.like;
							if (X.isEmpty(v)) {
								// skip
								return q;
							}
						} else if (X.isIn(op, "!like", "not like")) {
							op1 = W.OP.like;
							op2 = W.NOT;
							if (X.isEmpty(v)) {
								// skip
								return q;
							}
						} else if (X.isIn(op, "!=")) {
							op1 = W.OP.neq;
						} else {
							// =, ==
							op1 = W.OP.eq;
						}

						String name = ctx.NAME().getText();
						if (name.startsWith("$") || name.startsWith("\\")) {
							name = name.substring(1);
						}

						if (X.isIn(cond, "AND")) {
							if (op2 == W.NOT) {
								q.and(W.create().and(name, v, op1), W.NOT);
							} else {
								q.and(name, v, op1);
							}
						} else {
							if (op2 == W.NOT) {
								q.or(W.create().and(name, v, op1), W.NOT);
							} else {
								q.or(name, v, op1);
							}
						}
					}

				} catch (Exception e) {
//					e.printStackTrace();
					log.error(e.getMessage());
					GLog.applog.error("sql", "parse", e.getMessage(), e);

				}
			}

			return q;
		}

		@Override
		public W visitOrder(OrderContext ctx) {

			String name = ctx.NAME().getText();
			String by = ctx.by == null ? "asc" : ctx.by.getText();
			if (X.isIn(by, "asc")) {
				q.sort(name);
			} else {
				q.sort(name, -1);
			}
			return q;

		}

		@Override
		public W visitOffset(OffsetContext ctx) {
			q.offset(X.toInt(ctx.LONG().getText()));
			return q;
		}

		@Override
		public W visitLimit(LimitContext ctx) {
			q.limit(X.toInt(ctx.LONG().getText()));
			return q;
		}

	}

	static class ValVisitor extends SqlBaseVisitor<Object> {

		@Override
		public Object visitVal(ValContext ctx) {

			// val: STRING ('|' FLOAT)*
			// | FLOAT ('|' FLOAT)*
			// | LONG ('|' LONG)*
			// | null
			// | time
			// | todate
			// | today
			// | now
			// | uuid
			// | objectid
			// | val op=('+'|'-'|'*'|'/') val
			// ;

			{
				List<TerminalNode> l1 = ctx.STRING();
				if (l1 != null && l1.size() > 0) {
					List<String> l2 = new ArrayList<String>();
					for (TerminalNode t : l1) {
						String s = t.getText();
						s = s.substring(1, s.length() - 1);
						l2.add(s);
					}
					return l2.toArray();
				}
			}

			{
				List<TerminalNode> l1 = ctx.LONG();
				if (l1 != null && l1.size() > 0) {
					if (l1.size() > 1) {
						return X.asList(l1, t1 -> X.toLong(((TerminalNode) t1).getText()));
					} else {
						return X.toLong(l1.get(0).getText());
					}
				}
			}

			{
				List<TerminalNode> l1 = ctx.FLOAT();
				if (l1 != null && l1.size() > 0) {
					if (l1.size() > 1) {
						return X.asList(l1, t1 -> X.toDouble(((TerminalNode) t1).getText()));
					} else {
						return X.toDouble(l1.get(0).getText());
					}
				}
			}

			{
				NullContext n1 = ctx.null_();
				if (n1 != null) {
					return n1.accept(this);
				}
			}

			{
				TimeContext tc = ctx.time();
				if (tc != null) {
					return tc.accept(this);
				}
			}

			{
				TodateContext tc = ctx.todate();
				if (tc != null) {
					return tc.accept(this);
				}
			}

			{
				TostringContext tc = ctx.tostring();
				if (tc != null) {
					return tc.accept(this);
				}
			}

			{
				TolongContext tc = ctx.tolong();
				if (tc != null) {
					return tc.accept(this);
				}
			}

			{
				UuidContext uc = ctx.uuid();
				if (uc != null) {
					return uc.accept(this);
				}
			}

			{
				ObjectidContext oc = ctx.objectid();
				if (oc != null) {
					return oc.accept(this);
				}
			}

			{
				TodayContext t1 = ctx.today();
				if (t1 != null) {
					return t1.accept(this);
				}
			}

			{
				NowContext t1 = ctx.now();
				if (t1 != null) {
					return t1.accept(this);
				}
			}

			// val op=('+'|'-'|'*'|'/') val
			List<ValContext> l2 = ctx.val();
			if (l2 != null && l2.size() > 1) {
				Token op = ctx.op;
				if (op == null) {
					throw new RuntimeException("operation (*/+-) missed!");
				}
				String o = op.getText();
				Object v1 = ctx.val(0).accept(this);
				Object v2 = ctx.val(1).accept(this);
				if (X.isSame(o, "+")) {
					// 1 + 1
					if (v1 instanceof Integer && v2 instanceof Number) {
						return X.toInt(v1) + X.toInt(v2);
					} else if (v1 instanceof Long && v2 instanceof Number) {
						return X.toLong(v1) + X.toLong(v2);
					}
				} else if (X.isSame(o, "-")) {
					// 1 - 1
					if (v1 instanceof Integer && v2 instanceof Number) {
						return X.toInt(v1) - X.toInt(v2);
					} else if (v1 instanceof Long && v2 instanceof Number) {
						return X.toLong(v1) - X.toLong(v2);
					}
				} else if (X.isSame(o, "*")) {
					// 1 * 1
					if (v1 instanceof Integer && v2 instanceof Number) {
						return X.toInt(v1) - X.toInt(v2);
					} else if (v1 instanceof Long && v2 instanceof Number) {
						return X.toLong(v1) - X.toLong(v2);
					}
				} else if (X.isSame(o, "/")) {
					// 1/1
					if (v1 instanceof Integer && v2 instanceof Number) {
						return X.toInt(v1) / X.toInt(v2);
					} else if (v1 instanceof Long && v2 instanceof Number) {
						return X.toLong(v1) / X.toLong(v2);
					}
				}
				throw new RuntimeException("bad operation(" + o + ")!");
			}

			{
				Token fg = ctx.fg;
				if (fg != null) {
					ValContext val = ctx.val(0);
					String s = fg.getText();
					if (X.isSame(s, "+")) {
						return val.accept(this);
					} else if (X.isSame(s, "-")) {
						Object o = val.accept(this);
						if (o instanceof Long) {
							return -X.toLong(o);
						} else if (o instanceof Double) {
							return -X.toDouble(o);
						} else {
							throw new RuntimeException("bad flag [" + s + "] on " + o);
						}
					}
				}
			}
			return null;
		}

		@Override
		public Object visitUuid(UuidContext ctx) {
			TerminalNode str = ctx.STRING();
			if (str == null) {
				return UUID.randomUUID();
			}

			String s = str.getText();
			return UUID.fromString(s.substring(1, s.length() - 1));
		}

		@Override
		public Object visitObjectid(ObjectidContext ctx) {
			String s = ctx.STRING().getText();
			return new ObjectId(s.substring(1, s.length() - 1));
		}

		@Override
		public Object visitTodate(TodateContext ctx) {

			TerminalNode t = ctx.STRING();
			if (t != null) {
				String format = t.getText();
				format = format.substring(1, format.length() - 1);

				TimeContext t2 = ctx.time();
				String time = t2.accept(this).toString();
				if (time.startsWith("'") || time.startsWith("\"")) {
					// '2019-01-01'
					time = time.substring(1, time.length() - 1);
					return new Date(lang.parse(time, format));
				} else {
					// 20190101
					return new Date(lang.parse(time, format));
				}
			} else {
				TimeContext t2 = ctx.time();
				Object time = t2.accept(this);
				return new Date(X.toLong(time));
			}

		}

		@Override
		public Object visitTostring(TostringContext ctx) {
			// tostring: 'tostring(' val (',' val)* ')';
			List<ValContext> l1 = ctx.val();
			StringBuilder sb = new StringBuilder();
			if (l1 != null) {
				for (ValContext e : l1) {
					Object o = e.accept(this);
					if (o instanceof List) {
						X.asList(o, s -> sb.append(s));
					} else {
						sb.append(o);
					}
				}
			}
			return sb.toString();
		}

		@Override
		public Object visitTolong(TolongContext ctx) {
			ValContext v = ctx.val();
			if (v != null) {
				Object o = v.accept(this);
				if (o instanceof Date) {
					return ((Date) o).getTime();
				}
				return X.toLong(o);
			}
			return 0;
		}

		@Override
		public Object visitNull(NullContext ctx) {
			return null;
		}

		@Override
		public Object visitToday(TodayContext ctx) {

			TodayContext t1 = ctx.today();
			if (t1 != null) {
				return t1.accept(this).toString();
			} else {

				TerminalNode format = ctx.STRING();
				if (format == null) {
					return Stat.today();
				} else {
					// today('yyyyMMdd')
					String fmt = format.getText();
					fmt = fmt.substring(1, fmt.length() - 1);
					String time = lang.format(Stat.today(), fmt);
					if (X.isNumber(time)) {
						return X.toLong(time);
					}
					return time;
				}
			}
		}

		@Override
		public Object visitNow(NowContext ctx) {

			NowContext t1 = ctx.now();
			if (t1 != null) {
				return t1.accept(this).toString();
			} else {

				TerminalNode format = ctx.STRING();
				if (format == null) {
					return System.currentTimeMillis();
				} else {
					// now('yyyyMMdd')
					String fmt = format.getText();
					fmt = fmt.substring(1, fmt.length() - 1);
					String time = lang.format(System.currentTimeMillis(), fmt);
					if (X.isNumber(time)) {
						return X.toLong(time);
					}
					return time;
				}
			}
		}

		@Override
		public Object visitTime(TimeContext ctx) {

			/**
			 * today(...) <br>
			 * 'string' <br>
			 * long <br>
			 * (time) <br>
			 * () * () <br>
			 * () / () <br>
			 * () + () <br>
			 * () - () <br>
			 * () - TIME <br>
			 * TIME=7d
			 */

			Token op = ctx.op;
			if (op == null) {
				TodayContext today = ctx.today();
				if (today != null) {
					return today.accept(this);
				}

				// sting or long
				TerminalNode t = ctx.STRING();
				if (t != null) {
					String s = t.getText();
					return s.substring(1, s.length() - 1);
				}

				t = ctx.LONG();
				if (t != null) {
					return X.toLong(t.getText());
				}

				return ctx.time(0).accept(this);
			}

			Object v1 = ctx.time(0).accept(this);
			String o = op.getText();
			Object v2 = null;

			TerminalNode t1 = ctx.TIME();
			if (t1 != null) {
				String s1 = t1.getText();
				long v = X.toLong(s1);
				char d = s1.charAt(s1.length() - 1);
				if (d == 'd' || d == 'D') {
					v2 = v * X.ADAY;
				} else if (d == 'h' || d == 'H') {
					v2 = v * X.AHOUR;
				} else if (d == 'm' || d == 'M') {
					v2 = v * X.AMINUTE;
				} else if (d == 's' || d == 'S') {
					v2 = v * 1000;
				}

			} else {
				v2 = ctx.time(1).accept(this);
			}

			if (X.isSame(o, "+")) {
				return X.toLong(v1) + X.toLong(v2);
			} else if (X.isSame(o, "-")) {
				return X.toLong(v1) - X.toLong(v2);
			}
			throw new RuntimeException("bad operation (" + o + ")!");
		}

	}

	private static final String dump = "t___";

	/**
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public static W where(String sql) throws SQLException {
		if (X.isEmpty(sql)) {
			return W.create();
		}
		try {
			if (sql.toLowerCase().startsWith("order")) {
				return parse("select * from " + dump + " " + sql);
			}
			return parse("select * from " + dump + " where " + sql);
		} catch (SQLException e) {
			throw new SQLException(sql, e);
		}
	}

	/**
	 * @deprecated
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	public static W where2W(String sql) throws SQLException {
		return where(sql);
	}

}
