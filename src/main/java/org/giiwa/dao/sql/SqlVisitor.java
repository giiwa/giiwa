// Generated from java-escape by ANTLR 4.11.1
package org.giiwa.dao.sql;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link SqlParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface SqlVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link SqlParser#select}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSelect(SqlParser.SelectContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#columns}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumns(SqlParser.ColumnsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#columnItem}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitColumnItem(SqlParser.ColumnItemContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#tablename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablename(SqlParser.TablenameContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprParen}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprParen(SqlParser.ExprParenContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprOr}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprOr(SqlParser.ExprOrContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprIn}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprIn(SqlParser.ExprInContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprNot}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprNot(SqlParser.ExprNotContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprAnd}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprAnd(SqlParser.ExprAndContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprCompare}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprCompare(SqlParser.ExprCompareContext ctx);
	/**
	 * Visit a parse tree produced by the {@code exprBetween}
	 * labeled alternative in {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExprBetween(SqlParser.ExprBetweenContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#valOrList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValOrList(SqlParser.ValOrListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#inValueList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInValueList(SqlParser.InValueListContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#val}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVal(SqlParser.ValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#todate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTodate(SqlParser.TodateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#format}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFormat(SqlParser.FormatContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#time}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTime(SqlParser.TimeContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#today}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitToday(SqlParser.TodayContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#now}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNow(SqlParser.NowContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#tostring}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTostring(SqlParser.TostringContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#todouble}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTodouble(SqlParser.TodoubleContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#tofloat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTofloat(SqlParser.TofloatContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#tolong}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTolong(SqlParser.TolongContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#uuid}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUuid(SqlParser.UuidContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#objectid}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitObjectid(SqlParser.ObjectidContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#group}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitGroup(SqlParser.GroupContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#order}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOrder(SqlParser.OrderContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#offset}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitOffset(SqlParser.OffsetContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#limit}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitLimit(SqlParser.LimitContext ctx);
}