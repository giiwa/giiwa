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
	 * Visit a parse tree produced by {@link SqlParser#stat}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitStat(SqlParser.StatContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#show}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShow(SqlParser.ShowContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#showoptions}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitShowoptions(SqlParser.ShowoptionsContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#desc}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitDesc(SqlParser.DescContext ctx);
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
	 * Visit a parse tree produced by {@link SqlParser#func}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunc(SqlParser.FuncContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#tablename}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTablename(SqlParser.TablenameContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(SqlParser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#val}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVal(SqlParser.ValContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#null}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitNull(SqlParser.NullContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#todate}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTodate(SqlParser.TodateContext ctx);
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
	 * Visit a parse tree produced by {@link SqlParser#sum}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSum(SqlParser.SumContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#avg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitAvg(SqlParser.AvgContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#count}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitCount(SqlParser.CountContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#max}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMax(SqlParser.MaxContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#min}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitMin(SqlParser.MinContext ctx);
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
	/**
	 * Visit a parse tree produced by {@link SqlParser#set}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSet(SqlParser.SetContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#insert}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInsert(SqlParser.InsertContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#value}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitValue(SqlParser.ValueContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#update}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUpdate(SqlParser.UpdateContext ctx);
	/**
	 * Visit a parse tree produced by {@link SqlParser#setvalue}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSetvalue(SqlParser.SetvalueContext ctx);
}