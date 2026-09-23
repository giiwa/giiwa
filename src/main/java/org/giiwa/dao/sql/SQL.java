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

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bson.types.ObjectId;
import org.giiwa.bean.Stat;
import org.giiwa.conf.Global;
import org.giiwa.dao.X;
import org.giiwa.dao.Helper.W;
import org.giiwa.dao.sql.SqlParser.*;
import org.giiwa.web.Language;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class SQL {

//	private static final Log log = LogFactory.getLog(SQL.class);
	private static final Language lang = Language.getLanguage("zh_cn");
//	private static final String DUMMY_TABLE = "t___";

	// 线程安全日期格式化器，替换非线程安全SimpleDateFormat
//	private static DateTimeFormatter getFormatter(String pattern) {
//		return DateTimeFormatter.ofPattern(pattern);
//	}

	/**
	 * 对外入口：解析SQL生成W查询条件
	 */
	public static W parse(String sql) throws SQLException {

		if (X.isEmpty(sql)) {
			return W.create();
		}

		SqlLexer lexer = new SqlLexer(CharStreams.fromString(sql));
		CommonTokenStream tokenStream = new CommonTokenStream(lexer);
		SqlParser parser = new SqlParser(tokenStream);

		// 使用原子引用存储异常，替代数组写法
		AtomicReference<SQLException> errorRef = new AtomicReference<>();
		parser.removeErrorListeners();
		parser.addErrorListener(new BaseErrorListener() {
			@Override
			public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
					int charPositionInLine, String msg, RecognitionException e) {
				String errMsg = String.format("Line %d, position %d: %s \nSQL: %s", line, charPositionInLine, msg, sql);
				errorRef.set(new SQLException(errMsg, e));
			}
		});

		SelectContext statCtx = parser.select();
		if (errorRef.get() != null) {
			throw errorRef.get();
		}

		SQLVisitor visitor = new SQLVisitor();
		return visitor.visit(statCtx);
	}

	/**
	 * 仅解析WHERE条件片段，拼接成完整SELECT语句解析
	 */
	@Deprecated
	public static W where(String sql) throws SQLException {
		if (X.isEmpty(sql)) {
			return W.create();
		}
//		String lowerSql = sql.toLowerCase().trim();
//		String fullSql;
//		if (lowerSql.startsWith("order ")) {
//			fullSql = "SELECT * FROM " + DUMMY_TABLE + " " + sql;
//		} else {
//			fullSql = "SELECT * FROM " + DUMMY_TABLE + " WHERE " + sql;
//		}
		try {
			return parse(sql);
		} catch (SQLException e) {
			throw new SQLException("Parse where clause failed: " + sql, e);
		}
	}

	/**
	 * 废弃兼容方法
	 * 
	 * @deprecated use {@link #where(String)}
	 */
	@Deprecated
	public static W where2W(String sql) throws SQLException {
		return where(sql);
	}

	/**
	 * 去除单/双引号包裹 'xxx' / "xxx" -> xxx
	 */
	private static String unwrapString(String text) {
		if ((text.startsWith("'") && text.endsWith("'")) || (text.startsWith("\"") && text.endsWith("\""))) {
			return text.substring(1, text.length() - 1);
		}
		return text;
	}

	/**
	 * 去除反引号包裹 `xxx` -> xxx
	 */
	private static String unwrapName(String text) {
		if (text.startsWith("`") && text.endsWith("`")) {
			return text.substring(1, text.length() - 1);
		}
		return text;
	}

	// ===================== SQL顶层访问器：处理SELECT、条件、分组、排序等 =====================
	static class SQLVisitor extends SqlBaseVisitor<W> {

		@Override
		public W visitSelect(SelectContext ctx) {
			W query = W.create();
//			query.command = "select";

			// 1. 解析表名
			TablenameContext tableCtx = ctx.tablename();
			if (tableCtx != null) {
				query.table = resolveTableName(tableCtx);
			}

			// 2. 解析查询字段
			ColumnsContext columnsCtx = ctx.columns();
			if (columnsCtx != null) {
				String fieldStr = columnsCtx.accept(new ColumnVisitor());
				query.fields(fieldStr);
				if (X.isEmpty(query.table)) {
					query.params = X.asList(X.split(fieldStr, ","), s -> s.toString());
				}

			}

			// 3. 解析WHERE条件表达式
			ExprContext exprCtx = ctx.expr();
			if (exprCtx != null) {
				W exprObj = exprCtx.accept(new ExprVisitor());
				if (!exprObj.isEmpty()) {
					query.and(exprObj);
				}
			}

			// 4. 解析GROUP BY
			GroupContext groupCtx = ctx.group();
			if (groupCtx != null) {
				String groupStr = groupCtx.accept(new ColumnVisitor());
				query.groupby(groupStr);
			}

			// 5. 解析ORDER BY（修复原循环遍历BUG）
			List<OrderContext> orderList = ctx.order();
			if (orderList != null && !orderList.isEmpty()) {
				for (OrderContext orderCtx : orderList) {
					String field = unwrapName(orderCtx.NAME().getText());
					if (orderCtx.DESC() == null) {
						query.sort(field);
					} else {
						query.sort(field, -1);
					}
				}
			}

			// 6. OFFSET 分页偏移
			if (ctx.offset() != null) {
				long offsetVal = X.toLong(ctx.offset().LONG().getText());
				query.offset((int) offsetVal);
			}

			// 7. LIMIT 条数限制
			if (ctx.limit() != null) {
				long limitVal = X.toLong(ctx.limit().LONG().getText());
				query.limit((int) limitVal);
			}

			return query;
		}

		/**
		 * 解析表名，兼容 NAME / STRING / STRING.STRING 三级格式
		 */
		private String resolveTableName(TablenameContext ctx) {

			List<TerminalNode> nameNodes = ctx.NAME();
			if (nameNodes.size() == 1) {
				return unwrapName(nameNodes.get(0).getText());
			} else if (nameNodes.size() == 2) {
				// 处理 `aaa`.`y1_data` 两个NAME用点连接
				String db = unwrapName(nameNodes.get(0).getText());
				String tbl = unwrapName(nameNodes.get(1).getText());
				return db + "." + tbl;
			}

			List<TerminalNode> strNodes = ctx.STRING();
			if (strNodes.size() == 1) {
				return unwrapString(strNodes.get(0).getText());
			} else if (strNodes.size() == 2) {
				return unwrapString(strNodes.get(0).getText()) + "." + unwrapString(strNodes.get(1).getText());
			}
			return "";
		}

	}

	// ===================== 字段列访问器：解析 select a,b,c / count(*) =====================
	static class ColumnVisitor extends SqlBaseVisitor<String> {

		@Override
		public String visitColumns(ColumnsContext ctx) {
			List<String> colList = new ArrayList<>();
			for (ColumnItemContext item : ctx.columnItem()) {
				if (item.COUNT() != null) {
					if (item.columnItem() != null) {
						colList.add("count(" + unwrapName(item.columnItem().getText()) + ")");
					} else {
						colList.add("count(*)");
					}
				} else if (item.NAME() != null) {
					colList.add(unwrapName(item.NAME().getText()));
				} else if (item.HELP() != null) {
					colList.add(
							"""
									类SQL语句，支持简单SQL，不支持统计函数、表链接和嵌套；
									select * from tablename where a=1 and (b between 1 and 2) and (c=1 or c>2) and d in (1,2,3) and e in [1,2,3] and  (not f like 'a') group by `g` order by `c` offset 1 limit 10;
									函数：
										count(*) - 条数；
										todate(time, [format]) - 转换为时间格式；
											todate(now()) - 时间对象；
											todate('2026-01-01 10:11', 'yyyy-MM-dd HH:mm') - 时间对象；
										today() - 今天凌晨0点时间, 整数；
											today() - 今天凌晨0点毫秒整数；
											today(‘yyyyMMdd’) - 20260606；
										now() - 当前时间, 整数；
											now() - 当前时间毫秒整数；
											now('yyyyMMddHHmmss') - 20260606100101；
										tostring() - 对象/浮点/整数转换为字符串；
											tostring(now())
										todouble() - 对象/浮点/字符串/整数转换为双精度；
											todouble('11.11') - 11.11；
											todouble('11.11', '.1') - 11.1, 保留一位小数；
										tofloat() - 对象/浮点/字符串/整数转换为浮点；
											tofloat('11.11') - 11.11；
											tofloat('11.11', '.1') - 11.1, 保留一位小数；
										tolong() - 对象/浮点/字符串/整数转换为整数；
											tolong('11.111') - 11；
											tolong('一贰') - 12；
											tolong(now(), 'yyyyMMdd') - 20260606；
										uuid() - 生成新的uuid，或字符串转换为uuid对象；
											uuid() - 生成新的uuid；
											uuid('ae87e44c-9f07-4fd9-a164-cb8111bcf0a9') - 转换字符串为uuid对象；
										objectid() - 生成新的objectid对象，或转换字符串为objectid对象；
											objectid() - 生成新的objectid；
											objectid('6a8b9194ec71df4b5c0250b2') - 转换字符串为objectid对象；
										format() - 格式化时间对象为字符串；
											format(now(), 'yyyy-MM-dd') - '20260606'；
										""");
				} else if (item.val() != null) {
					Object valObj = item.val().accept(new ValVisitor());
					colList.add(String.valueOf(valObj));
				}
			}
			if (colList.isEmpty()) {
				return "*";
			}
			return String.join(",", colList);
		}

		@Override
		public String visitGroup(GroupContext ctx) {
			List<String> groupList = new ArrayList<>();
			for (TerminalNode nameNode : ctx.NAME()) {
				groupList.add(unwrapName(nameNode.getText()));
			}
			return String.join(",", groupList);
		}
	}

	// ===================== 条件表达式访问器：解析 AND/OR/BETWEEN/IN/LIKE/NOT
	static class ExprVisitor extends SqlBaseVisitor<W> {

		private final ValVisitor valVisitor = new ValVisitor();

		@Override
		public W visitExprParen(ExprParenContext ctx) {
			Object q = ctx.expr().accept(this);
			if (q instanceof W) {
				return (W) q;
			}
			return W.create();
		}

		@Override
		public W visitExprNot(ExprNotContext ctx) {
			Object q = ctx.expr().accept(this);
			if (q instanceof W) {
				return ((W) q).not();
			}
			return W.create();
		}

		@Override
		public W visitExprBetween(ExprBetweenContext ctx) {
			W q = W.create();
			String field = unwrapName(ctx.NAME().getText());
			Object v1 = ctx.val(0).accept(valVisitor);
			Object v2 = ctx.val(1).accept(valVisitor);

			if (X.compareTo(v1, v2) > 1) {
				Object temp = v1;
				v1 = v2;
				v2 = temp;
			}

			q.and(field, v1, W.OP.gte);
			q.and(field, v2, W.OP.lte);
			return q;
		}

		@Override
		public W visitExprIn(ExprInContext ctx) {
			W q = W.create();
			String field = unwrapName(ctx.NAME().getText());
			InValueListContext valNodes = ctx.inValueList();
			if (valNodes != null) {
				var l1 = valNodes.val();
				for (var val : l1) {
					var v = val.accept(valVisitor);
					q.or(field, v);
				}
			}
			return q;
		}

		@Override
		public W visitExprCompare(ExprCompareContext ctx) {

			String opRaw = ctx.op.getText().toUpperCase();
			String field = unwrapName(ctx.NAME().getText());

			// 获取 valOrList 下所有 val 节点
			List<ValContext> valCtxList = ctx.valOrList().val();
			// 先解析所有值
			List<Object> values = new ArrayList<>();
			for (ValContext vCtx : valCtxList) {
				Object v = vCtx.accept(valVisitor);
				values.add(v);
			}

			// 场景1：单个值，走原有比较逻辑
			if (values.size() == 1) {
				W.Entity entity = new W.Entity();
				entity.name = field;
				entity.value = values.get(0);
				switch (opRaw) {
				case "LIKE":
					entity.op = W.OP.like;
					break;
				case ">":
					entity.op = W.OP.gt;
					break;
				case ">=":
					entity.op = W.OP.gte;
					break;
				case "<":
					entity.op = W.OP.lt;
					break;
				case "<=":
					entity.op = W.OP.lte;
					break;
				case "!=":
				case "<>":
					entity.op = W.OP.neq;
					break;
				case "=":
				case "==":
				default:
					entity.op = W.OP.eq;
					break;
				}
				return entity;
			}

			// 场景2：多个值 a=1|2|3 等价于 a IN (1,2,3)，仅对 = / == 生效
			if ("=".equals(opRaw) || "==".equals(opRaw)) {
				W q = W.create();
				for (Object val : values) {
					q.or(field, val);
				}
				return q;
			}

			// 场景3：多个值 a like '1'|'2'|'4'
			if ("LIKE".equals(opRaw)) {
				W q = W.create();
				for (Object val : values) {
					q.or(field, val, W.OP.like);
				}
				return q;
			}

			// 其他运算符（> < >= <= != LIKE）不支持多值|列表，抛异常
			throw new RuntimeException("Operator " + opRaw + " does not support | multi-value list, only =/== allowed");

		}

		@Override
		public W visitExprAnd(ExprAndContext ctx) {
			W q = W.create();
			for (ExprContext child : ctx.expr()) {
				Object childObj = child.accept(this);
				if (childObj instanceof W) {
					q.and((W) childObj);
				}
			}
			return q;
		}

		@Override
		public W visitExprOr(ExprOrContext ctx) {
			W q = W.create();
			for (ExprContext child : ctx.expr()) {
				Object childObj = child.accept(this);
				if (childObj instanceof W) {
					q.or((W) childObj);
				}
			}
			return q;
		}
		
	}

	// ===================== 值计算访问器：四则运算、函数、时间转换、类型解析 =====================
	static class ValVisitor extends SqlBaseVisitor<Object> {

		@Override
		public Object visitVal(ValContext ctx) {
			// 1. 字符串 'a'|'b'
			TerminalNode str = ctx.STRING();
			if (str != null) {
				return unwrapString(str.getText());
			}

			// 2. LONG 1
			TerminalNode lon = ctx.LONG();
			if (lon != null) {
				return X.toLong(lon.getText());
			}

			// 3. FLOAT 1.1
			TerminalNode flo = ctx.FLOAT();
			if (flo != null) {
				return X.toFloat(flo.getText());
			}

			// 4. NULL
			if (ctx.NULL() != null) {
				return null;
			}

			// 5. 内置函数优先级
			if (ctx.format() != null)
				return visitFormat(ctx.format());
			if (ctx.todate() != null)
				return visitTodate(ctx.todate());
			if (ctx.tostring() != null)
				return visitTostring(ctx.tostring());
			if (ctx.tolong() != null)
				return visitTolong(ctx.tolong());
			if (ctx.uuid() != null)
				return visitUuid(ctx.uuid());
			if (ctx.objectid() != null)
				return visitObjectid(ctx.objectid());
			if (ctx.time() != null)
				return visitTime(ctx.time());
			if (ctx.todouble() != null)
				return visitTodouble(ctx.todouble());
			if (ctx.tofloat() != null)
				return visitTofloat(ctx.tofloat());

			// 6. 四则运算 val op val
			List<ValContext> valChildren = ctx.val();
			if (valChildren != null && valChildren.size() == 2) {
				Token opToken = ctx.op;
				if (opToken == null) {
					throw new RuntimeException("Missing operator for calculate");
				}
				String op = opToken.getText();
				Object v1 = valChildren.get(0).accept(this);
				Object v2 = valChildren.get(1).accept(this);

				return calculateNumber(v1, v2, op);
			}

			// 7. 正负号 +val / -val
			Token fgToken = ctx.fg;
			if (fgToken != null) {
				Object v1 = ctx.val(0).accept(this);
				String fg = fgToken.getText();
				if ("-".equals(fg)) {
					if (v1 instanceof Long)
						return -((Long) v1);
					if (v1 instanceof Double)
						return -((Double) v1);
					if (v1 instanceof Float)
						return -((Float) v1);

					throw new RuntimeException(
							"Negative sign unsupported type: " + v1.getClass().getName() + ", val=" + v1);
				}
				return v1;
			}

			return null;
		}

		/**
		 * 统一数字四则运算（修复原*写成-的致命BUG）
		 */
		private Object calculateNumber(Object o1, Object o2, String op) {
			long l1 = X.toLong(o1);
			long l2 = X.toLong(o2);
			double d1 = X.toDouble(o1);
			double d2 = X.toDouble(o2);

			boolean isLong = (o1 instanceof Long || o1 instanceof Integer)
					&& (o2 instanceof Long || o2 instanceof Integer);

			switch (op) {
			case "+":
				return isLong ? l1 + l2 : d1 + d2;
			case "-":
				return isLong ? l1 - l2 : d1 - d2;
			case "*":
				return isLong ? l1 * l2 : d1 * d2;
			case "/":
				if (isLong) {
					if (l2 == 0)
						throw new ArithmeticException("Divide by zero");
					return l1 / l2;
				} else {
					if (Math.abs(d2) < 1e-9)
						throw new ArithmeticException("Divide by zero");
					return d1 / d2;
				}
			default:
				throw new RuntimeException("Unsupported operator: " + op);
			}
		}

		/**
		 * FORMAT函数 数值保留小数 / 时间格式化
		 */
		@Override
		public Object visitFormat(FormatContext ctx) {
			Object valObj = ctx.val().accept(this);
			String fmtRaw = unwrapString(ctx.STRING().getText());

			// 数字格式化 format(123.456, ".2")
			if (valObj instanceof Double || valObj instanceof Float) {
				if (fmtRaw.startsWith(".")) {
					int scale = X.toInt(fmtRaw.substring(1));
					return BigDecimal.valueOf(X.toDouble(valObj)).setScale(scale, RoundingMode.HALF_UP).doubleValue();
				}
			}

			// 时间戳格式化
			if (valObj instanceof Long) {
				return lang.format(valObj, fmtRaw);
			} else if (valObj instanceof Date) {
				return lang.format(((Date) valObj).getTime(), fmtRaw);
			}
			return valObj;
		}

		/**
		 * TODATE 字符串转日期时间戳
		 */
		@Override
		public Date visitTodate(TodateContext ctx) {

			TimeContext timeCtx = ctx.time();
			Object timeObj = timeCtx.accept(this);
			TerminalNode strNode = ctx.STRING();

			// TODATE(time, 'yyyyMMdd')
			if (strNode != null) {
				String fmt = unwrapString(strNode.getText());
				String timeStr = String.valueOf(timeObj);
				timeStr = unwrapString(timeStr);
				long ts = lang.parse(timeStr, fmt);
				return new Date(ts);
			} else {
				// TODATE(timestamp)
				return new Date(X.toLong(timeObj));
			}
		}

		@Override
		public String visitTostring(TostringContext ctx) {
			StringBuilder sb = new StringBuilder();
			ValContext v = ctx.val();
			if (v != null) {
				Object obj = v.accept(this);
				sb.append(obj);
			}
			return sb.toString();
		}

		@Override
		public Long visitTolong(TolongContext ctx) {
			Object obj = ctx.val().accept(this);
			if (obj instanceof Date) {
				long n = ((Date) obj).getTime();
				if (ctx.STRING() != null) {
					// 时间格式转换为长整型
					// tolong(todate(now()), 'yyyyMMdd')
					return X.toLong(lang.format(n, ctx.STRING().getText()));
				}
				return n;
			}

			long n = X.toLong(obj);
			if (ctx.STRING() != null) {
				// 长整数转换为长整型
				// tolong(now(), 'yyyyMMdd')
				return X.toLong(lang.format(n, ctx.STRING().getText()));
			}
			return n;
		}

		@Override
		public Float visitTofloat(TofloatContext ctx) {
			Object obj = ctx.val().accept(this);
			float d = X.toFloat(obj);
			if (ctx.STRING() != null) {
				// ".2"
				String s = ctx.STRING().getText();
				int i = s.indexOf(".");
				if (i > -1) {
					s = s.substring(i + 1).trim();
				}
				d = BigDecimal.valueOf(d).setScale(X.toInt(s), RoundingMode.HALF_UP).floatValue();
			}
			return d;
		}

		@Override
		public Double visitTodouble(TodoubleContext ctx) {
			Object obj = ctx.val().accept(this);
			double d = X.toDouble(obj);
			if (ctx.STRING() != null) {
				// ".2"
				String s = ctx.STRING().getText();
				int i = s.indexOf(".");
				if (i > -1) {
					s = s.substring(i + 1).trim();
				}
				d = BigDecimal.valueOf(d).setScale(X.toInt(s), RoundingMode.HALF_UP).doubleValue();
			}
			return d;
		}

		@Override
		public UUID visitUuid(UuidContext ctx) {
			TerminalNode strNode = ctx.STRING();
			if (strNode == null) {
				return UUID.randomUUID();
			}
			String uuidStr = unwrapString(strNode.getText());
			return UUID.fromString(uuidStr);
		}

		@Override
		public Object visitObjectid(ObjectidContext ctx) {
			if (ctx.STRING() == null) {
				return new ObjectId();
			}
			String oidStr = unwrapString(ctx.STRING().getText());
			return new ObjectId(oidStr);
		}

		@Override
		public Object visitToday(TodayContext ctx) {

			TerminalNode fmtNode = ctx.STRING();
			long todayTs = Stat.today();
			if (fmtNode == null) {
				return todayTs;
			}
			String fmt = unwrapString(fmtNode.getText());
			return lang.format(todayTs, fmt);

		}

		/**
		 * NOW() 修复递归死循环
		 */
		@Override
		public Object visitNow(NowContext ctx) {
			// 递归嵌套直接返回原始时间戳，终止递归
			long nowTs = Global.now();

			TerminalNode fmtNode = ctx.STRING();
			if (fmtNode == null) {
				// 返回长整数
				return nowTs;
			}
			String fmt = unwrapString(fmtNode.getText());
			return lang.format(nowTs, fmt);
		}

		/**
		 * TIME 时间运算 支持 7d/2h 偏移
		 */
		@Override
		public Object visitTime(TimeContext ctx) {

			Token opToken = ctx.op;
			// 无运算符，直接取基础值
			if (opToken == null) {
				if (ctx.today() != null)
					return visitToday(ctx.today());
				if (ctx.now() != null)
					return visitNow(ctx.now());
				if (ctx.STRING() != null) {
					return unwrapString(ctx.STRING().getText());
				}
				if (ctx.LONG() != null) {
					return X.toLong(ctx.LONG().getText());
				}
				if (ctx.todate() != null) {
					return visitTodate(ctx.todate()).getTime();
				}
				return ctx.time(0).accept(this);
			}

			// 带运算符 time +/- time / time +/- TIME(7d)
			String op = opToken.getText();
			Object left = ctx.time(0).accept(this);
			Object right;

			TerminalNode timeUnitNode = ctx.TIME();
			if (timeUnitNode != null) {
				String unitText = timeUnitNode.getText();
				long num = X.toLong(unitText.substring(0, unitText.length() - 1));
				char unit = unitText.charAt(unitText.length() - 1);
				switch (unit) {
				case 'w':
				case 'W':
					right = num * X.AWEEK;
					break;
				case 'd':
				case 'D':
					right = num * X.ADAY;
					break;
				case 'h':
				case 'H':
					right = num * X.AHOUR;
					break;
				case 'm':
				case 'M':
					right = num * X.AMINUTE;
					break;
				case 's':
				case 'S':
					right = num * 1000L;
					break;
				default:
					throw new RuntimeException("Unsupported time unit: " + unit + ", only support [wWdWhHmMsS]");
				}
			} else {
				right = ctx.time(1).accept(this);
			}

			long lLeft = X.toLong(left);
			long lRight = X.toLong(right);
			if ("+".equals(op)) {
				return lLeft + lRight;
			} else if ("-".equals(op)) {
				return lLeft - lRight;
			}
			throw new RuntimeException("Time expression only support + / -");
		}
	}

}