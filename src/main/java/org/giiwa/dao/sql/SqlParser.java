// Generated from java-escape by ANTLR 4.11.1
package org.giiwa.dao.sql;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class SqlParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.11.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, T__14=15, T__15=16, T__16=17, 
		T__17=18, T__18=19, AND=20, OR=21, NOT=22, IN=23, BETWEEN=24, LIKE=25, 
		SELECT=26, FROM=27, WHERE=28, GROUP=29, BY=30, ORDER=31, ASC=32, DESC=33, 
		OFFSET=34, LIMIT=35, TODATE=36, TODAY=37, NOW=38, TOSTRING=39, TODOUBLE=40, 
		TOFLOAT=41, TOLONG=42, UUID=43, OBJECTID=44, FORMAT=45, COUNT=46, HELP=47, 
		NULL=48, NAME=49, ID=50, BACKTICK_NAME=51, LONG=52, FLOAT=53, STRING=54, 
		TIME=55, WS=56;
	public static final int
		RULE_select = 0, RULE_columns = 1, RULE_columnItem = 2, RULE_tablename = 3, 
		RULE_expr = 4, RULE_valOrList = 5, RULE_inValueList = 6, RULE_val = 7, 
		RULE_todate = 8, RULE_format = 9, RULE_time = 10, RULE_today = 11, RULE_now = 12, 
		RULE_tostring = 13, RULE_todouble = 14, RULE_tofloat = 15, RULE_tolong = 16, 
		RULE_uuid = 17, RULE_objectid = 18, RULE_group = 19, RULE_order = 20, 
		RULE_offset = 21, RULE_limit = 22;
	private static String[] makeRuleNames() {
		return new String[] {
			"select", "columns", "columnItem", "tablename", "expr", "valOrList", 
			"inValueList", "val", "todate", "format", "time", "today", "now", "tostring", 
			"todouble", "tofloat", "tolong", "uuid", "objectid", "group", "order", 
			"offset", "limit"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "','", "'*'", "')'", "'.'", "'('", "'>='", "'>'", "'<='", "'<'", 
			"'!='", "'<>'", "'=='", "'='", "'|'", "'['", "']'", "'/'", "'+'", "'-'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, "AND", "OR", "NOT", "IN", 
			"BETWEEN", "LIKE", "SELECT", "FROM", "WHERE", "GROUP", "BY", "ORDER", 
			"ASC", "DESC", "OFFSET", "LIMIT", "TODATE", "TODAY", "NOW", "TOSTRING", 
			"TODOUBLE", "TOFLOAT", "TOLONG", "UUID", "OBJECTID", "FORMAT", "COUNT", 
			"HELP", "NULL", "NAME", "ID", "BACKTICK_NAME", "LONG", "FLOAT", "STRING", 
			"TIME", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "java-escape"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public SqlParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SelectContext extends ParserRuleContext {
		public TerminalNode SELECT() { return getToken(SqlParser.SELECT, 0); }
		public ColumnsContext columns() {
			return getRuleContext(ColumnsContext.class,0);
		}
		public TerminalNode FROM() { return getToken(SqlParser.FROM, 0); }
		public TablenameContext tablename() {
			return getRuleContext(TablenameContext.class,0);
		}
		public TerminalNode WHERE() { return getToken(SqlParser.WHERE, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public TerminalNode GROUP() { return getToken(SqlParser.GROUP, 0); }
		public List<TerminalNode> BY() { return getTokens(SqlParser.BY); }
		public TerminalNode BY(int i) {
			return getToken(SqlParser.BY, i);
		}
		public GroupContext group() {
			return getRuleContext(GroupContext.class,0);
		}
		public TerminalNode ORDER() { return getToken(SqlParser.ORDER, 0); }
		public List<OrderContext> order() {
			return getRuleContexts(OrderContext.class);
		}
		public OrderContext order(int i) {
			return getRuleContext(OrderContext.class,i);
		}
		public TerminalNode OFFSET() { return getToken(SqlParser.OFFSET, 0); }
		public OffsetContext offset() {
			return getRuleContext(OffsetContext.class,0);
		}
		public TerminalNode LIMIT() { return getToken(SqlParser.LIMIT, 0); }
		public LimitContext limit() {
			return getRuleContext(LimitContext.class,0);
		}
		public SelectContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_select; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitSelect(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SelectContext select() throws RecognitionException {
		SelectContext _localctx = new SelectContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_select);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(47);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==SELECT) {
				{
				setState(46);
				match(SELECT);
				}
			}

			setState(50);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				{
				setState(49);
				columns();
				}
				break;
			}
			setState(53);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==FROM) {
				{
				setState(52);
				match(FROM);
				}
			}

			setState(56);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				{
				setState(55);
				tablename();
				}
				break;
			}
			setState(59);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WHERE) {
				{
				setState(58);
				match(WHERE);
				}
			}

			setState(62);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (((_la) & ~0x3f) == 0 && ((1L << _la) & 562949957615648L) != 0) {
				{
				setState(61);
				expr(0);
				}
			}

			setState(67);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==GROUP) {
				{
				setState(64);
				match(GROUP);
				setState(65);
				match(BY);
				setState(66);
				group();
				}
			}

			setState(79);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ORDER) {
				{
				setState(69);
				match(ORDER);
				setState(70);
				match(BY);
				setState(71);
				order();
				setState(76);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__0) {
					{
					{
					setState(72);
					match(T__0);
					setState(73);
					order();
					}
					}
					setState(78);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(93);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case OFFSET:
				{
				{
				setState(81);
				match(OFFSET);
				setState(82);
				offset();
				setState(85);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LIMIT) {
					{
					setState(83);
					match(LIMIT);
					setState(84);
					limit();
					}
				}

				}
				}
				break;
			case LIMIT:
				{
				{
				setState(87);
				match(LIMIT);
				setState(88);
				limit();
				setState(91);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==OFFSET) {
					{
					setState(89);
					match(OFFSET);
					setState(90);
					offset();
					}
				}

				}
				}
				break;
			case EOF:
				break;
			default:
				break;
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ColumnsContext extends ParserRuleContext {
		public List<ColumnItemContext> columnItem() {
			return getRuleContexts(ColumnItemContext.class);
		}
		public ColumnItemContext columnItem(int i) {
			return getRuleContext(ColumnItemContext.class,i);
		}
		public ColumnsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columns; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitColumns(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColumnsContext columns() throws RecognitionException {
		ColumnsContext _localctx = new ColumnsContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_columns);
		int _la;
		try {
			setState(104);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__1:
				enterOuterAlt(_localctx, 1);
				{
				setState(95);
				match(T__1);
				}
				break;
			case T__4:
			case T__17:
			case T__18:
			case TODATE:
			case TODAY:
			case NOW:
			case TOSTRING:
			case TODOUBLE:
			case TOFLOAT:
			case TOLONG:
			case UUID:
			case OBJECTID:
			case FORMAT:
			case COUNT:
			case HELP:
			case NULL:
			case NAME:
			case LONG:
			case FLOAT:
			case STRING:
				enterOuterAlt(_localctx, 2);
				{
				setState(96);
				columnItem();
				setState(101);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__0) {
					{
					{
					setState(97);
					match(T__0);
					setState(98);
					columnItem();
					}
					}
					setState(103);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ColumnItemContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public TerminalNode COUNT() { return getToken(SqlParser.COUNT, 0); }
		public ColumnItemContext columnItem() {
			return getRuleContext(ColumnItemContext.class,0);
		}
		public TerminalNode HELP() { return getToken(SqlParser.HELP, 0); }
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public ColumnItemContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_columnItem; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitColumnItem(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ColumnItemContext columnItem() throws RecognitionException {
		ColumnItemContext _localctx = new ColumnItemContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_columnItem);
		try {
			setState(117);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(106);
				match(NAME);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(107);
				match(COUNT);
				setState(108);
				match(T__1);
				setState(109);
				match(T__2);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(110);
				match(COUNT);
				setState(111);
				columnItem();
				setState(112);
				match(T__2);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(114);
				match(HELP);
				setState(115);
				match(T__2);
				}
				break;
			case 5:
				enterOuterAlt(_localctx, 5);
				{
				setState(116);
				val(0);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TablenameContext extends ParserRuleContext {
		public List<TerminalNode> NAME() { return getTokens(SqlParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(SqlParser.NAME, i);
		}
		public List<TerminalNode> STRING() { return getTokens(SqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(SqlParser.STRING, i);
		}
		public TablenameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tablename; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTablename(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TablenameContext tablename() throws RecognitionException {
		TablenameContext _localctx = new TablenameContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_tablename);
		try {
			setState(127);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(119);
				match(NAME);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(120);
				match(NAME);
				setState(121);
				match(T__3);
				setState(122);
				match(NAME);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(123);
				match(STRING);
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(124);
				match(STRING);
				setState(125);
				match(T__3);
				setState(126);
				match(STRING);
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExprParenContext extends ExprContext {
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ExprParenContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExprParen(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExprOrContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode OR() { return getToken(SqlParser.OR, 0); }
		public ExprOrContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExprOr(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExprInContext extends ExprContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public TerminalNode IN() { return getToken(SqlParser.IN, 0); }
		public InValueListContext inValueList() {
			return getRuleContext(InValueListContext.class,0);
		}
		public ExprInContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExprIn(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExprNotContext extends ExprContext {
		public TerminalNode NOT() { return getToken(SqlParser.NOT, 0); }
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ExprNotContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExprNot(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExprAndContext extends ExprContext {
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public TerminalNode AND() { return getToken(SqlParser.AND, 0); }
		public ExprAndContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExprAnd(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExprCompareContext extends ExprContext {
		public Token op;
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public ValOrListContext valOrList() {
			return getRuleContext(ValOrListContext.class,0);
		}
		public TerminalNode LIKE() { return getToken(SqlParser.LIKE, 0); }
		public ExprCompareContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExprCompare(this);
			else return visitor.visitChildren(this);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class ExprBetweenContext extends ExprContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public TerminalNode BETWEEN() { return getToken(SqlParser.BETWEEN, 0); }
		public List<ValContext> val() {
			return getRuleContexts(ValContext.class);
		}
		public ValContext val(int i) {
			return getRuleContext(ValContext.class,i);
		}
		public TerminalNode AND() { return getToken(SqlParser.AND, 0); }
		public ExprBetweenContext(ExprContext ctx) { copyFrom(ctx); }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExprBetween(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 8;
		enterRecursionRule(_localctx, 8, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(148);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
			case 1:
				{
				_localctx = new ExprParenContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(130);
				match(T__4);
				setState(131);
				expr(0);
				setState(132);
				match(T__2);
				}
				break;
			case 2:
				{
				_localctx = new ExprNotContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(134);
				match(NOT);
				setState(135);
				expr(6);
				}
				break;
			case 3:
				{
				_localctx = new ExprBetweenContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(136);
				match(NAME);
				setState(137);
				match(BETWEEN);
				setState(138);
				val(0);
				setState(139);
				match(AND);
				setState(140);
				val(0);
				}
				break;
			case 4:
				{
				_localctx = new ExprInContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(142);
				match(NAME);
				setState(143);
				match(IN);
				setState(144);
				inValueList();
				}
				break;
			case 5:
				{
				_localctx = new ExprCompareContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(145);
				match(NAME);
				setState(146);
				((ExprCompareContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 33570752L) != 0) ) {
					((ExprCompareContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(147);
				valOrList();
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(158);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(156);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
					case 1:
						{
						_localctx = new ExprAndContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(150);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(151);
						match(AND);
						setState(152);
						expr(3);
						}
						break;
					case 2:
						{
						_localctx = new ExprOrContext(new ExprContext(_parentctx, _parentState));
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(153);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(154);
						match(OR);
						setState(155);
						expr(2);
						}
						break;
					}
					} 
				}
				setState(160);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,18,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ValOrListContext extends ParserRuleContext {
		public List<ValContext> val() {
			return getRuleContexts(ValContext.class);
		}
		public ValContext val(int i) {
			return getRuleContext(ValContext.class,i);
		}
		public ValOrListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_valOrList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitValOrList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValOrListContext valOrList() throws RecognitionException {
		ValOrListContext _localctx = new ValOrListContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_valOrList);
		try {
			int _alt;
			setState(177);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(161);
				val(0);
				setState(166);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(162);
						match(T__0);
						setState(163);
						val(0);
						}
						} 
					}
					setState(168);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
				}
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(169);
				val(0);
				setState(174);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(170);
						match(T__13);
						setState(171);
						val(0);
						}
						} 
					}
					setState(176);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				}
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class InValueListContext extends ParserRuleContext {
		public List<ValContext> val() {
			return getRuleContexts(ValContext.class);
		}
		public ValContext val(int i) {
			return getRuleContext(ValContext.class,i);
		}
		public InValueListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_inValueList; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitInValueList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InValueListContext inValueList() throws RecognitionException {
		InValueListContext _localctx = new InValueListContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_inValueList);
		int _la;
		try {
			setState(201);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__4:
				enterOuterAlt(_localctx, 1);
				{
				setState(179);
				match(T__4);
				setState(180);
				val(0);
				setState(185);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__0) {
					{
					{
					setState(181);
					match(T__0);
					setState(182);
					val(0);
					}
					}
					setState(187);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(188);
				match(T__2);
				}
				break;
			case T__14:
				enterOuterAlt(_localctx, 2);
				{
				setState(190);
				match(T__14);
				setState(191);
				val(0);
				setState(196);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__0) {
					{
					{
					setState(192);
					match(T__0);
					setState(193);
					val(0);
					}
					}
					setState(198);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(199);
				match(T__15);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ValContext extends ParserRuleContext {
		public Token fg;
		public Token op;
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TerminalNode FLOAT() { return getToken(SqlParser.FLOAT, 0); }
		public TerminalNode LONG() { return getToken(SqlParser.LONG, 0); }
		public TerminalNode NULL() { return getToken(SqlParser.NULL, 0); }
		public TodateContext todate() {
			return getRuleContext(TodateContext.class,0);
		}
		public TimeContext time() {
			return getRuleContext(TimeContext.class,0);
		}
		public TostringContext tostring() {
			return getRuleContext(TostringContext.class,0);
		}
		public TodoubleContext todouble() {
			return getRuleContext(TodoubleContext.class,0);
		}
		public TofloatContext tofloat() {
			return getRuleContext(TofloatContext.class,0);
		}
		public TolongContext tolong() {
			return getRuleContext(TolongContext.class,0);
		}
		public UuidContext uuid() {
			return getRuleContext(UuidContext.class,0);
		}
		public ObjectidContext objectid() {
			return getRuleContext(ObjectidContext.class,0);
		}
		public FormatContext format() {
			return getRuleContext(FormatContext.class,0);
		}
		public List<ValContext> val() {
			return getRuleContexts(ValContext.class);
		}
		public ValContext val(int i) {
			return getRuleContext(ValContext.class,i);
		}
		public ValContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_val; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitVal(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValContext val() throws RecognitionException {
		return val(0);
	}

	private ValContext val(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ValContext _localctx = new ValContext(_ctx, _parentState);
		ValContext _prevctx = _localctx;
		int _startState = 14;
		enterRecursionRule(_localctx, 14, RULE_val, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(219);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				{
				setState(204);
				match(STRING);
				}
				break;
			case 2:
				{
				setState(205);
				match(FLOAT);
				}
				break;
			case 3:
				{
				setState(206);
				match(LONG);
				}
				break;
			case 4:
				{
				setState(207);
				match(NULL);
				}
				break;
			case 5:
				{
				setState(208);
				todate();
				}
				break;
			case 6:
				{
				setState(209);
				time(0);
				}
				break;
			case 7:
				{
				setState(210);
				tostring();
				}
				break;
			case 8:
				{
				setState(211);
				todouble();
				}
				break;
			case 9:
				{
				setState(212);
				tofloat();
				}
				break;
			case 10:
				{
				setState(213);
				tolong();
				}
				break;
			case 11:
				{
				setState(214);
				uuid();
				}
				break;
			case 12:
				{
				setState(215);
				objectid();
				}
				break;
			case 13:
				{
				setState(216);
				format();
				}
				break;
			case 14:
				{
				setState(217);
				((ValContext)_localctx).fg = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==T__17 || _la==T__18) ) {
					((ValContext)_localctx).fg = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(218);
				val(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(229);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(227);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
					case 1:
						{
						_localctx = new ValContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_val);
						setState(221);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(222);
						((ValContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__1 || _la==T__16) ) {
							((ValContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(223);
						val(4);
						}
						break;
					case 2:
						{
						_localctx = new ValContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_val);
						setState(224);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(225);
						((ValContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__17 || _la==T__18) ) {
							((ValContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(226);
						val(3);
						}
						break;
					}
					} 
				}
				setState(231);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,27,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TodateContext extends ParserRuleContext {
		public TerminalNode TODATE() { return getToken(SqlParser.TODATE, 0); }
		public TimeContext time() {
			return getRuleContext(TimeContext.class,0);
		}
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TodateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_todate; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTodate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TodateContext todate() throws RecognitionException {
		TodateContext _localctx = new TodateContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_todate);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(232);
			match(TODATE);
			setState(233);
			time(0);
			setState(236);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(234);
				match(T__0);
				setState(235);
				match(STRING);
				}
			}

			setState(238);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class FormatContext extends ParserRuleContext {
		public TerminalNode FORMAT() { return getToken(SqlParser.FORMAT, 0); }
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public FormatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_format; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitFormat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FormatContext format() throws RecognitionException {
		FormatContext _localctx = new FormatContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_format);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(240);
			match(FORMAT);
			setState(241);
			val(0);
			setState(242);
			match(T__0);
			setState(243);
			match(STRING);
			setState(244);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TimeContext extends ParserRuleContext {
		public Token op;
		public TodayContext today() {
			return getRuleContext(TodayContext.class,0);
		}
		public NowContext now() {
			return getRuleContext(NowContext.class,0);
		}
		public TodateContext todate() {
			return getRuleContext(TodateContext.class,0);
		}
		public List<TimeContext> time() {
			return getRuleContexts(TimeContext.class);
		}
		public TimeContext time(int i) {
			return getRuleContext(TimeContext.class,i);
		}
		public TerminalNode LONG() { return getToken(SqlParser.LONG, 0); }
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TerminalNode TIME() { return getToken(SqlParser.TIME, 0); }
		public TimeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_time; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTime(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TimeContext time() throws RecognitionException {
		return time(0);
	}

	private TimeContext time(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		TimeContext _localctx = new TimeContext(_ctx, _parentState);
		TimeContext _prevctx = _localctx;
		int _startState = 20;
		enterRecursionRule(_localctx, 20, RULE_time, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(256);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case TODAY:
				{
				setState(247);
				today();
				}
				break;
			case NOW:
				{
				setState(248);
				now();
				}
				break;
			case TODATE:
				{
				setState(249);
				todate();
				}
				break;
			case T__4:
				{
				setState(250);
				match(T__4);
				setState(251);
				time(0);
				setState(252);
				match(T__2);
				}
				break;
			case LONG:
				{
				setState(254);
				match(LONG);
				}
				break;
			case STRING:
				{
				setState(255);
				match(STRING);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(266);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(264);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,30,_ctx) ) {
					case 1:
						{
						_localctx = new TimeContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_time);
						setState(258);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(259);
						((TimeContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__17 || _la==T__18) ) {
							((TimeContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(260);
						time(5);
						}
						break;
					case 2:
						{
						_localctx = new TimeContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_time);
						setState(261);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(262);
						((TimeContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__17 || _la==T__18) ) {
							((TimeContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(263);
						match(TIME);
						}
						break;
					}
					} 
				}
				setState(268);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,31,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TodayContext extends ParserRuleContext {
		public TerminalNode TODAY() { return getToken(SqlParser.TODAY, 0); }
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TodayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_today; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitToday(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TodayContext today() throws RecognitionException {
		TodayContext _localctx = new TodayContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_today);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(269);
			match(TODAY);
			setState(271);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING) {
				{
				setState(270);
				match(STRING);
				}
			}

			setState(273);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class NowContext extends ParserRuleContext {
		public TerminalNode NOW() { return getToken(SqlParser.NOW, 0); }
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public NowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_now; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitNow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NowContext now() throws RecognitionException {
		NowContext _localctx = new NowContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_now);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(275);
			match(NOW);
			setState(277);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING) {
				{
				setState(276);
				match(STRING);
				}
			}

			setState(279);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TostringContext extends ParserRuleContext {
		public TerminalNode TOSTRING() { return getToken(SqlParser.TOSTRING, 0); }
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public TostringContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tostring; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTostring(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TostringContext tostring() throws RecognitionException {
		TostringContext _localctx = new TostringContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_tostring);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(281);
			match(TOSTRING);
			setState(282);
			val(0);
			setState(283);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TodoubleContext extends ParserRuleContext {
		public TerminalNode TODOUBLE() { return getToken(SqlParser.TODOUBLE, 0); }
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TodoubleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_todouble; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTodouble(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TodoubleContext todouble() throws RecognitionException {
		TodoubleContext _localctx = new TodoubleContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_todouble);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(285);
			match(TODOUBLE);
			setState(286);
			val(0);
			setState(289);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(287);
				match(T__0);
				setState(288);
				match(STRING);
				}
			}

			setState(291);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TofloatContext extends ParserRuleContext {
		public TerminalNode TOFLOAT() { return getToken(SqlParser.TOFLOAT, 0); }
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TofloatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tofloat; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTofloat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TofloatContext tofloat() throws RecognitionException {
		TofloatContext _localctx = new TofloatContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_tofloat);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(293);
			match(TOFLOAT);
			setState(294);
			val(0);
			setState(297);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(295);
				match(T__0);
				setState(296);
				match(STRING);
				}
			}

			setState(299);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class TolongContext extends ParserRuleContext {
		public TerminalNode TOLONG() { return getToken(SqlParser.TOLONG, 0); }
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TolongContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_tolong; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitTolong(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TolongContext tolong() throws RecognitionException {
		TolongContext _localctx = new TolongContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_tolong);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(301);
			match(TOLONG);
			setState(302);
			val(0);
			setState(305);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(303);
				match(T__0);
				setState(304);
				match(STRING);
				}
			}

			setState(307);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class UuidContext extends ParserRuleContext {
		public TerminalNode UUID() { return getToken(SqlParser.UUID, 0); }
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public UuidContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_uuid; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitUuid(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UuidContext uuid() throws RecognitionException {
		UuidContext _localctx = new UuidContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_uuid);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(309);
			match(UUID);
			setState(311);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING) {
				{
				setState(310);
				match(STRING);
				}
			}

			setState(313);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class ObjectidContext extends ParserRuleContext {
		public TerminalNode OBJECTID() { return getToken(SqlParser.OBJECTID, 0); }
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public ObjectidContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_objectid; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitObjectid(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ObjectidContext objectid() throws RecognitionException {
		ObjectidContext _localctx = new ObjectidContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_objectid);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(315);
			match(OBJECTID);
			setState(317);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==STRING) {
				{
				setState(316);
				match(STRING);
				}
			}

			setState(319);
			match(T__2);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class GroupContext extends ParserRuleContext {
		public List<TerminalNode> NAME() { return getTokens(SqlParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(SqlParser.NAME, i);
		}
		public GroupContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_group; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitGroup(this);
			else return visitor.visitChildren(this);
		}
	}

	public final GroupContext group() throws RecognitionException {
		GroupContext _localctx = new GroupContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_group);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(321);
			match(NAME);
			setState(326);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__0) {
				{
				{
				setState(322);
				match(T__0);
				setState(323);
				match(NAME);
				}
				}
				setState(328);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OrderContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public TerminalNode ASC() { return getToken(SqlParser.ASC, 0); }
		public TerminalNode DESC() { return getToken(SqlParser.DESC, 0); }
		public OrderContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_order; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitOrder(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OrderContext order() throws RecognitionException {
		OrderContext _localctx = new OrderContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_order);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(329);
			match(NAME);
			setState(331);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==ASC || _la==DESC) {
				{
				setState(330);
				_la = _input.LA(1);
				if ( !(_la==ASC || _la==DESC) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class OffsetContext extends ParserRuleContext {
		public TerminalNode LONG() { return getToken(SqlParser.LONG, 0); }
		public OffsetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_offset; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitOffset(this);
			else return visitor.visitChildren(this);
		}
	}

	public final OffsetContext offset() throws RecognitionException {
		OffsetContext _localctx = new OffsetContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_offset);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(333);
			match(LONG);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class LimitContext extends ParserRuleContext {
		public TerminalNode LONG() { return getToken(SqlParser.LONG, 0); }
		public LimitContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_limit; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitLimit(this);
			else return visitor.visitChildren(this);
		}
	}

	public final LimitContext limit() throws RecognitionException {
		LimitContext _localctx = new LimitContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_limit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(335);
			match(LONG);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 4:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 7:
			return val_sempred((ValContext)_localctx, predIndex);
		case 10:
			return time_sempred((TimeContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 2);
		case 1:
			return precpred(_ctx, 1);
		}
		return true;
	}
	private boolean val_sempred(ValContext _localctx, int predIndex) {
		switch (predIndex) {
		case 2:
			return precpred(_ctx, 3);
		case 3:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean time_sempred(TimeContext _localctx, int predIndex) {
		switch (predIndex) {
		case 4:
			return precpred(_ctx, 4);
		case 5:
			return precpred(_ctx, 3);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u00018\u0152\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0001\u0000\u0003\u00000\b\u0000\u0001\u0000"+
		"\u0003\u00003\b\u0000\u0001\u0000\u0003\u00006\b\u0000\u0001\u0000\u0003"+
		"\u00009\b\u0000\u0001\u0000\u0003\u0000<\b\u0000\u0001\u0000\u0003\u0000"+
		"?\b\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000D\b\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0001\u0000\u0005\u0000K\b"+
		"\u0000\n\u0000\f\u0000N\t\u0000\u0003\u0000P\b\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0003\u0000V\b\u0000\u0001\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0000\u0003\u0000\\\b\u0000\u0003\u0000^\b\u0000"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0005\u0001d\b\u0001"+
		"\n\u0001\f\u0001g\t\u0001\u0003\u0001i\b\u0001\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002v\b\u0002\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0003\u0003\u0080\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u0095\b\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0005\u0004\u009d\b\u0004\n\u0004\f\u0004\u00a0\t\u0004\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0005\u0005\u00a5\b\u0005\n\u0005\f\u0005\u00a8\t\u0005"+
		"\u0001\u0005\u0001\u0005\u0001\u0005\u0005\u0005\u00ad\b\u0005\n\u0005"+
		"\f\u0005\u00b0\t\u0005\u0003\u0005\u00b2\b\u0005\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0005\u0006\u00b8\b\u0006\n\u0006\f\u0006\u00bb"+
		"\t\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0005\u0006\u00c3\b\u0006\n\u0006\f\u0006\u00c6\t\u0006\u0001\u0006"+
		"\u0001\u0006\u0003\u0006\u00ca\b\u0006\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0003\u0007\u00dc\b\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0005\u0007\u00e4\b\u0007\n\u0007"+
		"\f\u0007\u00e7\t\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0003\b\u00ed\b"+
		"\b\u0001\b\u0001\b\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0003\n\u0101\b\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0005"+
		"\n\u0109\b\n\n\n\f\n\u010c\t\n\u0001\u000b\u0001\u000b\u0003\u000b\u0110"+
		"\b\u000b\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0003\f\u0116\b\f\u0001"+
		"\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001\r\u0001\u000e\u0001\u000e\u0001"+
		"\u000e\u0001\u000e\u0003\u000e\u0122\b\u000e\u0001\u000e\u0001\u000e\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u012a\b\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0003"+
		"\u0010\u0132\b\u0010\u0001\u0010\u0001\u0010\u0001\u0011\u0001\u0011\u0003"+
		"\u0011\u0138\b\u0011\u0001\u0011\u0001\u0011\u0001\u0012\u0001\u0012\u0003"+
		"\u0012\u013e\b\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0005\u0013\u0145\b\u0013\n\u0013\f\u0013\u0148\t\u0013\u0001\u0014"+
		"\u0001\u0014\u0003\u0014\u014c\b\u0014\u0001\u0015\u0001\u0015\u0001\u0016"+
		"\u0001\u0016\u0001\u0016\u0000\u0003\b\u000e\u0014\u0017\u0000\u0002\u0004"+
		"\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e \""+
		"$&(*,\u0000\u0004\u0002\u0000\u0006\r\u0019\u0019\u0001\u0000\u0012\u0013"+
		"\u0002\u0000\u0002\u0002\u0011\u0011\u0001\u0000 !\u017c\u0000/\u0001"+
		"\u0000\u0000\u0000\u0002h\u0001\u0000\u0000\u0000\u0004u\u0001\u0000\u0000"+
		"\u0000\u0006\u007f\u0001\u0000\u0000\u0000\b\u0094\u0001\u0000\u0000\u0000"+
		"\n\u00b1\u0001\u0000\u0000\u0000\f\u00c9\u0001\u0000\u0000\u0000\u000e"+
		"\u00db\u0001\u0000\u0000\u0000\u0010\u00e8\u0001\u0000\u0000\u0000\u0012"+
		"\u00f0\u0001\u0000\u0000\u0000\u0014\u0100\u0001\u0000\u0000\u0000\u0016"+
		"\u010d\u0001\u0000\u0000\u0000\u0018\u0113\u0001\u0000\u0000\u0000\u001a"+
		"\u0119\u0001\u0000\u0000\u0000\u001c\u011d\u0001\u0000\u0000\u0000\u001e"+
		"\u0125\u0001\u0000\u0000\u0000 \u012d\u0001\u0000\u0000\u0000\"\u0135"+
		"\u0001\u0000\u0000\u0000$\u013b\u0001\u0000\u0000\u0000&\u0141\u0001\u0000"+
		"\u0000\u0000(\u0149\u0001\u0000\u0000\u0000*\u014d\u0001\u0000\u0000\u0000"+
		",\u014f\u0001\u0000\u0000\u0000.0\u0005\u001a\u0000\u0000/.\u0001\u0000"+
		"\u0000\u0000/0\u0001\u0000\u0000\u000002\u0001\u0000\u0000\u000013\u0003"+
		"\u0002\u0001\u000021\u0001\u0000\u0000\u000023\u0001\u0000\u0000\u0000"+
		"35\u0001\u0000\u0000\u000046\u0005\u001b\u0000\u000054\u0001\u0000\u0000"+
		"\u000056\u0001\u0000\u0000\u000068\u0001\u0000\u0000\u000079\u0003\u0006"+
		"\u0003\u000087\u0001\u0000\u0000\u000089\u0001\u0000\u0000\u00009;\u0001"+
		"\u0000\u0000\u0000:<\u0005\u001c\u0000\u0000;:\u0001\u0000\u0000\u0000"+
		";<\u0001\u0000\u0000\u0000<>\u0001\u0000\u0000\u0000=?\u0003\b\u0004\u0000"+
		">=\u0001\u0000\u0000\u0000>?\u0001\u0000\u0000\u0000?C\u0001\u0000\u0000"+
		"\u0000@A\u0005\u001d\u0000\u0000AB\u0005\u001e\u0000\u0000BD\u0003&\u0013"+
		"\u0000C@\u0001\u0000\u0000\u0000CD\u0001\u0000\u0000\u0000DO\u0001\u0000"+
		"\u0000\u0000EF\u0005\u001f\u0000\u0000FG\u0005\u001e\u0000\u0000GL\u0003"+
		"(\u0014\u0000HI\u0005\u0001\u0000\u0000IK\u0003(\u0014\u0000JH\u0001\u0000"+
		"\u0000\u0000KN\u0001\u0000\u0000\u0000LJ\u0001\u0000\u0000\u0000LM\u0001"+
		"\u0000\u0000\u0000MP\u0001\u0000\u0000\u0000NL\u0001\u0000\u0000\u0000"+
		"OE\u0001\u0000\u0000\u0000OP\u0001\u0000\u0000\u0000P]\u0001\u0000\u0000"+
		"\u0000QR\u0005\"\u0000\u0000RU\u0003*\u0015\u0000ST\u0005#\u0000\u0000"+
		"TV\u0003,\u0016\u0000US\u0001\u0000\u0000\u0000UV\u0001\u0000\u0000\u0000"+
		"V^\u0001\u0000\u0000\u0000WX\u0005#\u0000\u0000X[\u0003,\u0016\u0000Y"+
		"Z\u0005\"\u0000\u0000Z\\\u0003*\u0015\u0000[Y\u0001\u0000\u0000\u0000"+
		"[\\\u0001\u0000\u0000\u0000\\^\u0001\u0000\u0000\u0000]Q\u0001\u0000\u0000"+
		"\u0000]W\u0001\u0000\u0000\u0000]^\u0001\u0000\u0000\u0000^\u0001\u0001"+
		"\u0000\u0000\u0000_i\u0005\u0002\u0000\u0000`e\u0003\u0004\u0002\u0000"+
		"ab\u0005\u0001\u0000\u0000bd\u0003\u0004\u0002\u0000ca\u0001\u0000\u0000"+
		"\u0000dg\u0001\u0000\u0000\u0000ec\u0001\u0000\u0000\u0000ef\u0001\u0000"+
		"\u0000\u0000fi\u0001\u0000\u0000\u0000ge\u0001\u0000\u0000\u0000h_\u0001"+
		"\u0000\u0000\u0000h`\u0001\u0000\u0000\u0000i\u0003\u0001\u0000\u0000"+
		"\u0000jv\u00051\u0000\u0000kl\u0005.\u0000\u0000lm\u0005\u0002\u0000\u0000"+
		"mv\u0005\u0003\u0000\u0000no\u0005.\u0000\u0000op\u0003\u0004\u0002\u0000"+
		"pq\u0005\u0003\u0000\u0000qv\u0001\u0000\u0000\u0000rs\u0005/\u0000\u0000"+
		"sv\u0005\u0003\u0000\u0000tv\u0003\u000e\u0007\u0000uj\u0001\u0000\u0000"+
		"\u0000uk\u0001\u0000\u0000\u0000un\u0001\u0000\u0000\u0000ur\u0001\u0000"+
		"\u0000\u0000ut\u0001\u0000\u0000\u0000v\u0005\u0001\u0000\u0000\u0000"+
		"w\u0080\u00051\u0000\u0000xy\u00051\u0000\u0000yz\u0005\u0004\u0000\u0000"+
		"z\u0080\u00051\u0000\u0000{\u0080\u00056\u0000\u0000|}\u00056\u0000\u0000"+
		"}~\u0005\u0004\u0000\u0000~\u0080\u00056\u0000\u0000\u007fw\u0001\u0000"+
		"\u0000\u0000\u007fx\u0001\u0000\u0000\u0000\u007f{\u0001\u0000\u0000\u0000"+
		"\u007f|\u0001\u0000\u0000\u0000\u0080\u0007\u0001\u0000\u0000\u0000\u0081"+
		"\u0082\u0006\u0004\uffff\uffff\u0000\u0082\u0083\u0005\u0005\u0000\u0000"+
		"\u0083\u0084\u0003\b\u0004\u0000\u0084\u0085\u0005\u0003\u0000\u0000\u0085"+
		"\u0095\u0001\u0000\u0000\u0000\u0086\u0087\u0005\u0016\u0000\u0000\u0087"+
		"\u0095\u0003\b\u0004\u0006\u0088\u0089\u00051\u0000\u0000\u0089\u008a"+
		"\u0005\u0018\u0000\u0000\u008a\u008b\u0003\u000e\u0007\u0000\u008b\u008c"+
		"\u0005\u0014\u0000\u0000\u008c\u008d\u0003\u000e\u0007\u0000\u008d\u0095"+
		"\u0001\u0000\u0000\u0000\u008e\u008f\u00051\u0000\u0000\u008f\u0090\u0005"+
		"\u0017\u0000\u0000\u0090\u0095\u0003\f\u0006\u0000\u0091\u0092\u00051"+
		"\u0000\u0000\u0092\u0093\u0007\u0000\u0000\u0000\u0093\u0095\u0003\n\u0005"+
		"\u0000\u0094\u0081\u0001\u0000\u0000\u0000\u0094\u0086\u0001\u0000\u0000"+
		"\u0000\u0094\u0088\u0001\u0000\u0000\u0000\u0094\u008e\u0001\u0000\u0000"+
		"\u0000\u0094\u0091\u0001\u0000\u0000\u0000\u0095\u009e\u0001\u0000\u0000"+
		"\u0000\u0096\u0097\n\u0002\u0000\u0000\u0097\u0098\u0005\u0014\u0000\u0000"+
		"\u0098\u009d\u0003\b\u0004\u0003\u0099\u009a\n\u0001\u0000\u0000\u009a"+
		"\u009b\u0005\u0015\u0000\u0000\u009b\u009d\u0003\b\u0004\u0002\u009c\u0096"+
		"\u0001\u0000\u0000\u0000\u009c\u0099\u0001\u0000\u0000\u0000\u009d\u00a0"+
		"\u0001\u0000\u0000\u0000\u009e\u009c\u0001\u0000\u0000\u0000\u009e\u009f"+
		"\u0001\u0000\u0000\u0000\u009f\t\u0001\u0000\u0000\u0000\u00a0\u009e\u0001"+
		"\u0000\u0000\u0000\u00a1\u00a6\u0003\u000e\u0007\u0000\u00a2\u00a3\u0005"+
		"\u0001\u0000\u0000\u00a3\u00a5\u0003\u000e\u0007\u0000\u00a4\u00a2\u0001"+
		"\u0000\u0000\u0000\u00a5\u00a8\u0001\u0000\u0000\u0000\u00a6\u00a4\u0001"+
		"\u0000\u0000\u0000\u00a6\u00a7\u0001\u0000\u0000\u0000\u00a7\u00b2\u0001"+
		"\u0000\u0000\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a9\u00ae\u0003"+
		"\u000e\u0007\u0000\u00aa\u00ab\u0005\u000e\u0000\u0000\u00ab\u00ad\u0003"+
		"\u000e\u0007\u0000\u00ac\u00aa\u0001\u0000\u0000\u0000\u00ad\u00b0\u0001"+
		"\u0000\u0000\u0000\u00ae\u00ac\u0001\u0000\u0000\u0000\u00ae\u00af\u0001"+
		"\u0000\u0000\u0000\u00af\u00b2\u0001\u0000\u0000\u0000\u00b0\u00ae\u0001"+
		"\u0000\u0000\u0000\u00b1\u00a1\u0001\u0000\u0000\u0000\u00b1\u00a9\u0001"+
		"\u0000\u0000\u0000\u00b2\u000b\u0001\u0000\u0000\u0000\u00b3\u00b4\u0005"+
		"\u0005\u0000\u0000\u00b4\u00b9\u0003\u000e\u0007\u0000\u00b5\u00b6\u0005"+
		"\u0001\u0000\u0000\u00b6\u00b8\u0003\u000e\u0007\u0000\u00b7\u00b5\u0001"+
		"\u0000\u0000\u0000\u00b8\u00bb\u0001\u0000\u0000\u0000\u00b9\u00b7\u0001"+
		"\u0000\u0000\u0000\u00b9\u00ba\u0001\u0000\u0000\u0000\u00ba\u00bc\u0001"+
		"\u0000\u0000\u0000\u00bb\u00b9\u0001\u0000\u0000\u0000\u00bc\u00bd\u0005"+
		"\u0003\u0000\u0000\u00bd\u00ca\u0001\u0000\u0000\u0000\u00be\u00bf\u0005"+
		"\u000f\u0000\u0000\u00bf\u00c4\u0003\u000e\u0007\u0000\u00c0\u00c1\u0005"+
		"\u0001\u0000\u0000\u00c1\u00c3\u0003\u000e\u0007\u0000\u00c2\u00c0\u0001"+
		"\u0000\u0000\u0000\u00c3\u00c6\u0001\u0000\u0000\u0000\u00c4\u00c2\u0001"+
		"\u0000\u0000\u0000\u00c4\u00c5\u0001\u0000\u0000\u0000\u00c5\u00c7\u0001"+
		"\u0000\u0000\u0000\u00c6\u00c4\u0001\u0000\u0000\u0000\u00c7\u00c8\u0005"+
		"\u0010\u0000\u0000\u00c8\u00ca\u0001\u0000\u0000\u0000\u00c9\u00b3\u0001"+
		"\u0000\u0000\u0000\u00c9\u00be\u0001\u0000\u0000\u0000\u00ca\r\u0001\u0000"+
		"\u0000\u0000\u00cb\u00cc\u0006\u0007\uffff\uffff\u0000\u00cc\u00dc\u0005"+
		"6\u0000\u0000\u00cd\u00dc\u00055\u0000\u0000\u00ce\u00dc\u00054\u0000"+
		"\u0000\u00cf\u00dc\u00050\u0000\u0000\u00d0\u00dc\u0003\u0010\b\u0000"+
		"\u00d1\u00dc\u0003\u0014\n\u0000\u00d2\u00dc\u0003\u001a\r\u0000\u00d3"+
		"\u00dc\u0003\u001c\u000e\u0000\u00d4\u00dc\u0003\u001e\u000f\u0000\u00d5"+
		"\u00dc\u0003 \u0010\u0000\u00d6\u00dc\u0003\"\u0011\u0000\u00d7\u00dc"+
		"\u0003$\u0012\u0000\u00d8\u00dc\u0003\u0012\t\u0000\u00d9\u00da\u0007"+
		"\u0001\u0000\u0000\u00da\u00dc\u0003\u000e\u0007\u0001\u00db\u00cb\u0001"+
		"\u0000\u0000\u0000\u00db\u00cd\u0001\u0000\u0000\u0000\u00db\u00ce\u0001"+
		"\u0000\u0000\u0000\u00db\u00cf\u0001\u0000\u0000\u0000\u00db\u00d0\u0001"+
		"\u0000\u0000\u0000\u00db\u00d1\u0001\u0000\u0000\u0000\u00db\u00d2\u0001"+
		"\u0000\u0000\u0000\u00db\u00d3\u0001\u0000\u0000\u0000\u00db\u00d4\u0001"+
		"\u0000\u0000\u0000\u00db\u00d5\u0001\u0000\u0000\u0000\u00db\u00d6\u0001"+
		"\u0000\u0000\u0000\u00db\u00d7\u0001\u0000\u0000\u0000\u00db\u00d8\u0001"+
		"\u0000\u0000\u0000\u00db\u00d9\u0001\u0000\u0000\u0000\u00dc\u00e5\u0001"+
		"\u0000\u0000\u0000\u00dd\u00de\n\u0003\u0000\u0000\u00de\u00df\u0007\u0002"+
		"\u0000\u0000\u00df\u00e4\u0003\u000e\u0007\u0004\u00e0\u00e1\n\u0002\u0000"+
		"\u0000\u00e1\u00e2\u0007\u0001\u0000\u0000\u00e2\u00e4\u0003\u000e\u0007"+
		"\u0003\u00e3\u00dd\u0001\u0000\u0000\u0000\u00e3\u00e0\u0001\u0000\u0000"+
		"\u0000\u00e4\u00e7\u0001\u0000\u0000\u0000\u00e5\u00e3\u0001\u0000\u0000"+
		"\u0000\u00e5\u00e6\u0001\u0000\u0000\u0000\u00e6\u000f\u0001\u0000\u0000"+
		"\u0000\u00e7\u00e5\u0001\u0000\u0000\u0000\u00e8\u00e9\u0005$\u0000\u0000"+
		"\u00e9\u00ec\u0003\u0014\n\u0000\u00ea\u00eb\u0005\u0001\u0000\u0000\u00eb"+
		"\u00ed\u00056\u0000\u0000\u00ec\u00ea\u0001\u0000\u0000\u0000\u00ec\u00ed"+
		"\u0001\u0000\u0000\u0000\u00ed\u00ee\u0001\u0000\u0000\u0000\u00ee\u00ef"+
		"\u0005\u0003\u0000\u0000\u00ef\u0011\u0001\u0000\u0000\u0000\u00f0\u00f1"+
		"\u0005-\u0000\u0000\u00f1\u00f2\u0003\u000e\u0007\u0000\u00f2\u00f3\u0005"+
		"\u0001\u0000\u0000\u00f3\u00f4\u00056\u0000\u0000\u00f4\u00f5\u0005\u0003"+
		"\u0000\u0000\u00f5\u0013\u0001\u0000\u0000\u0000\u00f6\u00f7\u0006\n\uffff"+
		"\uffff\u0000\u00f7\u0101\u0003\u0016\u000b\u0000\u00f8\u0101\u0003\u0018"+
		"\f\u0000\u00f9\u0101\u0003\u0010\b\u0000\u00fa\u00fb\u0005\u0005\u0000"+
		"\u0000\u00fb\u00fc\u0003\u0014\n\u0000\u00fc\u00fd\u0005\u0003\u0000\u0000"+
		"\u00fd\u0101\u0001\u0000\u0000\u0000\u00fe\u0101\u00054\u0000\u0000\u00ff"+
		"\u0101\u00056\u0000\u0000\u0100\u00f6\u0001\u0000\u0000\u0000\u0100\u00f8"+
		"\u0001\u0000\u0000\u0000\u0100\u00f9\u0001\u0000\u0000\u0000\u0100\u00fa"+
		"\u0001\u0000\u0000\u0000\u0100\u00fe\u0001\u0000\u0000\u0000\u0100\u00ff"+
		"\u0001\u0000\u0000\u0000\u0101\u010a\u0001\u0000\u0000\u0000\u0102\u0103"+
		"\n\u0004\u0000\u0000\u0103\u0104\u0007\u0001\u0000\u0000\u0104\u0109\u0003"+
		"\u0014\n\u0005\u0105\u0106\n\u0003\u0000\u0000\u0106\u0107\u0007\u0001"+
		"\u0000\u0000\u0107\u0109\u00057\u0000\u0000\u0108\u0102\u0001\u0000\u0000"+
		"\u0000\u0108\u0105\u0001\u0000\u0000\u0000\u0109\u010c\u0001\u0000\u0000"+
		"\u0000\u010a\u0108\u0001\u0000\u0000\u0000\u010a\u010b\u0001\u0000\u0000"+
		"\u0000\u010b\u0015\u0001\u0000\u0000\u0000\u010c\u010a\u0001\u0000\u0000"+
		"\u0000\u010d\u010f\u0005%\u0000\u0000\u010e\u0110\u00056\u0000\u0000\u010f"+
		"\u010e\u0001\u0000\u0000\u0000\u010f\u0110\u0001\u0000\u0000\u0000\u0110"+
		"\u0111\u0001\u0000\u0000\u0000\u0111\u0112\u0005\u0003\u0000\u0000\u0112"+
		"\u0017\u0001\u0000\u0000\u0000\u0113\u0115\u0005&\u0000\u0000\u0114\u0116"+
		"\u00056\u0000\u0000\u0115\u0114\u0001\u0000\u0000\u0000\u0115\u0116\u0001"+
		"\u0000\u0000\u0000\u0116\u0117\u0001\u0000\u0000\u0000\u0117\u0118\u0005"+
		"\u0003\u0000\u0000\u0118\u0019\u0001\u0000\u0000\u0000\u0119\u011a\u0005"+
		"\'\u0000\u0000\u011a\u011b\u0003\u000e\u0007\u0000\u011b\u011c\u0005\u0003"+
		"\u0000\u0000\u011c\u001b\u0001\u0000\u0000\u0000\u011d\u011e\u0005(\u0000"+
		"\u0000\u011e\u0121\u0003\u000e\u0007\u0000\u011f\u0120\u0005\u0001\u0000"+
		"\u0000\u0120\u0122\u00056\u0000\u0000\u0121\u011f\u0001\u0000\u0000\u0000"+
		"\u0121\u0122\u0001\u0000\u0000\u0000\u0122\u0123\u0001\u0000\u0000\u0000"+
		"\u0123\u0124\u0005\u0003\u0000\u0000\u0124\u001d\u0001\u0000\u0000\u0000"+
		"\u0125\u0126\u0005)\u0000\u0000\u0126\u0129\u0003\u000e\u0007\u0000\u0127"+
		"\u0128\u0005\u0001\u0000\u0000\u0128\u012a\u00056\u0000\u0000\u0129\u0127"+
		"\u0001\u0000\u0000\u0000\u0129\u012a\u0001\u0000\u0000\u0000\u012a\u012b"+
		"\u0001\u0000\u0000\u0000\u012b\u012c\u0005\u0003\u0000\u0000\u012c\u001f"+
		"\u0001\u0000\u0000\u0000\u012d\u012e\u0005*\u0000\u0000\u012e\u0131\u0003"+
		"\u000e\u0007\u0000\u012f\u0130\u0005\u0001\u0000\u0000\u0130\u0132\u0005"+
		"6\u0000\u0000\u0131\u012f\u0001\u0000\u0000\u0000\u0131\u0132\u0001\u0000"+
		"\u0000\u0000\u0132\u0133\u0001\u0000\u0000\u0000\u0133\u0134\u0005\u0003"+
		"\u0000\u0000\u0134!\u0001\u0000\u0000\u0000\u0135\u0137\u0005+\u0000\u0000"+
		"\u0136\u0138\u00056\u0000\u0000\u0137\u0136\u0001\u0000\u0000\u0000\u0137"+
		"\u0138\u0001\u0000\u0000\u0000\u0138\u0139\u0001\u0000\u0000\u0000\u0139"+
		"\u013a\u0005\u0003\u0000\u0000\u013a#\u0001\u0000\u0000\u0000\u013b\u013d"+
		"\u0005,\u0000\u0000\u013c\u013e\u00056\u0000\u0000\u013d\u013c\u0001\u0000"+
		"\u0000\u0000\u013d\u013e\u0001\u0000\u0000\u0000\u013e\u013f\u0001\u0000"+
		"\u0000\u0000\u013f\u0140\u0005\u0003\u0000\u0000\u0140%\u0001\u0000\u0000"+
		"\u0000\u0141\u0146\u00051\u0000\u0000\u0142\u0143\u0005\u0001\u0000\u0000"+
		"\u0143\u0145\u00051\u0000\u0000\u0144\u0142\u0001\u0000\u0000\u0000\u0145"+
		"\u0148\u0001\u0000\u0000\u0000\u0146\u0144\u0001\u0000\u0000\u0000\u0146"+
		"\u0147\u0001\u0000\u0000\u0000\u0147\'\u0001\u0000\u0000\u0000\u0148\u0146"+
		"\u0001\u0000\u0000\u0000\u0149\u014b\u00051\u0000\u0000\u014a\u014c\u0007"+
		"\u0003\u0000\u0000\u014b\u014a\u0001\u0000\u0000\u0000\u014b\u014c\u0001"+
		"\u0000\u0000\u0000\u014c)\u0001\u0000\u0000\u0000\u014d\u014e\u00054\u0000"+
		"\u0000\u014e+\u0001\u0000\u0000\u0000\u014f\u0150\u00054\u0000\u0000\u0150"+
		"-\u0001\u0000\u0000\u0000)/258;>CLOU[]ehu\u007f\u0094\u009c\u009e\u00a6"+
		"\u00ae\u00b1\u00b9\u00c4\u00c9\u00db\u00e3\u00e5\u00ec\u0100\u0108\u010a"+
		"\u010f\u0115\u0121\u0129\u0131\u0137\u013d\u0146\u014b";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}