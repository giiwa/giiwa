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
		T__17=18, T__18=19, T__19=20, T__20=21, T__21=22, T__22=23, T__23=24, 
		T__24=25, T__25=26, T__26=27, T__27=28, T__28=29, T__29=30, T__30=31, 
		T__31=32, T__32=33, T__33=34, T__34=35, T__35=36, T__36=37, T__37=38, 
		T__38=39, T__39=40, T__40=41, T__41=42, T__42=43, T__43=44, T__44=45, 
		T__45=46, T__46=47, T__47=48, T__48=49, T__49=50, T__50=51, T__51=52, 
		T__52=53, T__53=54, T__54=55, T__55=56, T__56=57, T__57=58, T__58=59, 
		T__59=60, T__60=61, T__61=62, T__62=63, T__63=64, T__64=65, T__65=66, 
		T__66=67, T__67=68, T__68=69, T__69=70, T__70=71, T__71=72, T__72=73, 
		T__73=74, T__74=75, T__75=76, T__76=77, T__77=78, T__78=79, T__79=80, 
		T__80=81, T__81=82, T__82=83, T__83=84, T__84=85, T__85=86, T__86=87, 
		T__87=88, T__88=89, T__89=90, T__90=91, T__91=92, T__92=93, T__93=94, 
		T__94=95, T__95=96, T__96=97, T__97=98, T__98=99, T__99=100, T__100=101, 
		T__101=102, T__102=103, T__103=104, T__104=105, T__105=106, T__106=107, 
		T__107=108, T__108=109, T__109=110, T__110=111, T__111=112, T__112=113, 
		LONG=114, FLOAT=115, NAME=116, STRING=117, TIME=118, WS=119;
	public static final int
		RULE_stat = 0, RULE_show = 1, RULE_showoptions = 2, RULE_desc = 3, RULE_select = 4, 
		RULE_columns = 5, RULE_func = 6, RULE_tablename = 7, RULE_expr = 8, RULE_val = 9, 
		RULE_null = 10, RULE_todate = 11, RULE_time = 12, RULE_today = 13, RULE_now = 14, 
		RULE_tostring = 15, RULE_tolong = 16, RULE_uuid = 17, RULE_objectid = 18, 
		RULE_sum = 19, RULE_avg = 20, RULE_count = 21, RULE_max = 22, RULE_min = 23, 
		RULE_group = 24, RULE_order = 25, RULE_offset = 26, RULE_limit = 27, RULE_set = 28, 
		RULE_insert = 29, RULE_value = 30, RULE_update = 31, RULE_setvalue = 32;
	private static String[] makeRuleNames() {
		return new String[] {
			"stat", "show", "showoptions", "desc", "select", "columns", "func", "tablename", 
			"expr", "val", "null", "todate", "time", "today", "now", "tostring", 
			"tolong", "uuid", "objectid", "sum", "avg", "count", "max", "min", "group", 
			"order", "offset", "limit", "set", "insert", "value", "update", "setvalue"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'show'", "'SHOW'", "'dbs'", "'DBS'", "'databases'", "'DATABASES'", 
			"'ENGINES'", "'COLLATION'", "'CHARACTER SET'", "'groups'", "'GROUPS'", 
			"'tables'", "'TABLES'", "'CREATE TABLE'", "'table'", "'TABLE'", "'like'", 
			"'LIKE'", "'DATABASE'", "'GROUP'", "'columns'", "'COLUMNS'", "'full columns'", 
			"'FULL COLUMNS'", "'full fields'", "'FULL FIELDS'", "'INDEX'", "'from'", 
			"'FROM'", "'FULL TABLES'", "'WHERE'", "'desc'", "'DESC'", "'select'", 
			"'SELECT'", "'where'", "'group'", "'by'", "'BY'", "'order'", "'ORDER'", 
			"','", "'offset'", "'OFFSET'", "'limit'", "'LIMIT'", "'*'", "'>='", "'>'", 
			"'<='", "'<'", "'!='", "'=='", "'='", "'!like'", "'!LIKE'", "'not like'", 
			"'NOT LIKE'", "'('", "')'", "'and'", "'or'", "'AND'", "'OR'", "'not'", 
			"'|'", "'/'", "'+'", "'-'", "'null'", "'NULL'", "'null('", "'NULL('", 
			"'todate('", "'TODATE('", "'today('", "'TODAY('", "'''", "'\"'", "'now('", 
			"'NOW'", "'tostring('", "'TOSTRING('", "'tolong('", "'TOLONG('", "'uuid('", 
			"'UUID('", "'touuid('", "'TOUUID('", "'objectid('", "'OBJECTID('", "'sum('", 
			"'SUM('", "'avg('", "'AVG'", "'count('", "'COUNT('", "'max('", "'MAX('", 
			"'min('", "'MIN('", "'asc'", "'ASC'", "'set'", "'SET'", "'insert'", "'INSERT'", 
			"'into'", "'INTO'", "'value'", "'VALUE'", "'update'", "'UPDATE'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, null, null, null, null, null, null, 
			null, null, null, null, null, null, "LONG", "FLOAT", "NAME", "STRING", 
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
	public static class StatContext extends ParserRuleContext {
		public ShowContext show() {
			return getRuleContext(ShowContext.class,0);
		}
		public DescContext desc() {
			return getRuleContext(DescContext.class,0);
		}
		public SelectContext select() {
			return getRuleContext(SelectContext.class,0);
		}
		public SetContext set() {
			return getRuleContext(SetContext.class,0);
		}
		public InsertContext insert() {
			return getRuleContext(InsertContext.class,0);
		}
		public UpdateContext update() {
			return getRuleContext(UpdateContext.class,0);
		}
		public StatContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_stat; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitStat(this);
			else return visitor.visitChildren(this);
		}
	}

	public final StatContext stat() throws RecognitionException {
		StatContext _localctx = new StatContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_stat);
		try {
			setState(72);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case T__1:
				enterOuterAlt(_localctx, 1);
				{
				setState(66);
				show();
				}
				break;
			case T__31:
			case T__32:
				enterOuterAlt(_localctx, 2);
				{
				setState(67);
				desc();
				}
				break;
			case EOF:
			case T__19:
			case T__27:
			case T__28:
			case T__30:
			case T__33:
			case T__34:
			case T__35:
			case T__36:
			case T__39:
			case T__40:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case T__46:
			case T__58:
			case T__67:
			case T__68:
			case T__69:
			case T__70:
			case T__71:
			case T__72:
			case T__73:
			case T__74:
			case T__75:
			case T__76:
			case T__77:
			case T__78:
			case T__79:
			case T__80:
			case T__81:
			case T__82:
			case T__83:
			case T__84:
			case T__85:
			case T__86:
			case T__87:
			case T__88:
			case T__89:
			case T__90:
			case LONG:
			case FLOAT:
			case NAME:
			case STRING:
				enterOuterAlt(_localctx, 3);
				{
				setState(68);
				select();
				}
				break;
			case T__103:
			case T__104:
				enterOuterAlt(_localctx, 4);
				{
				setState(69);
				set();
				}
				break;
			case T__105:
			case T__106:
				enterOuterAlt(_localctx, 5);
				{
				setState(70);
				insert();
				}
				break;
			case T__111:
			case T__112:
				enterOuterAlt(_localctx, 6);
				{
				setState(71);
				update();
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
	public static class ShowContext extends ParserRuleContext {
		public ShowoptionsContext showoptions() {
			return getRuleContext(ShowoptionsContext.class,0);
		}
		public ShowContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_show; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitShow(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ShowContext show() throws RecognitionException {
		ShowContext _localctx = new ShowContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_show);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(74);
			_la = _input.LA(1);
			if ( !(_la==T__0 || _la==T__1) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(75);
			showoptions();
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
	public static class ShowoptionsContext extends ParserRuleContext {
		public TablenameContext tablename() {
			return getRuleContext(TablenameContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ShowoptionsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_showoptions; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitShowoptions(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ShowoptionsContext showoptions() throws RecognitionException {
		ShowoptionsContext _localctx = new ShowoptionsContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_showoptions);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(105);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				{
				setState(77);
				match(T__2);
				}
				break;
			case 2:
				{
				setState(78);
				match(T__3);
				}
				break;
			case 3:
				{
				setState(79);
				match(T__4);
				}
				break;
			case 4:
				{
				setState(80);
				match(T__5);
				}
				break;
			case 5:
				{
				setState(81);
				match(T__6);
				}
				break;
			case 6:
				{
				setState(82);
				match(T__7);
				}
				break;
			case 7:
				{
				setState(83);
				match(T__8);
				}
				break;
			case 8:
				{
				setState(84);
				match(T__9);
				}
				break;
			case 9:
				{
				setState(85);
				match(T__10);
				}
				break;
			case 10:
				{
				setState(86);
				match(T__11);
				}
				break;
			case 11:
				{
				setState(87);
				match(T__12);
				}
				break;
			case 12:
				{
				setState(88);
				match(T__13);
				setState(89);
				tablename();
				}
				break;
			case 13:
				{
				setState(90);
				_la = _input.LA(1);
				if ( !(_la==T__14 || _la==T__15) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(91);
				_la = _input.LA(1);
				if ( !(_la==T__16 || _la==T__17) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(92);
				tablename();
				}
				break;
			case 14:
				{
				setState(93);
				match(T__3);
				}
				break;
			case 15:
				{
				setState(94);
				match(T__18);
				}
				break;
			case 16:
				{
				setState(95);
				match(T__19);
				}
				break;
			case 17:
				{
				setState(96);
				_la = _input.LA(1);
				if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 266338304L) != 0) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(97);
				_la = _input.LA(1);
				if ( !(_la==T__27 || _la==T__28) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(98);
				tablename();
				}
				break;
			case 18:
				{
				setState(99);
				match(T__29);
				setState(103);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case EOF:
					{
					}
					break;
				case T__30:
					{
					setState(101);
					match(T__30);
					setState(102);
					expr(0);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
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
	public static class DescContext extends ParserRuleContext {
		public TablenameContext tablename() {
			return getRuleContext(TablenameContext.class,0);
		}
		public DescContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_desc; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitDesc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final DescContext desc() throws RecognitionException {
		DescContext _localctx = new DescContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_desc);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(107);
			_la = _input.LA(1);
			if ( !(_la==T__31 || _la==T__32) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(108);
			tablename();
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
	public static class SelectContext extends ParserRuleContext {
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public ColumnsContext columns() {
			return getRuleContext(ColumnsContext.class,0);
		}
		public TablenameContext tablename() {
			return getRuleContext(TablenameContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public GroupContext group() {
			return getRuleContext(GroupContext.class,0);
		}
		public List<OrderContext> order() {
			return getRuleContexts(OrderContext.class);
		}
		public OrderContext order(int i) {
			return getRuleContext(OrderContext.class,i);
		}
		public OffsetContext offset() {
			return getRuleContext(OffsetContext.class,0);
		}
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
		enterRule(_localctx, 8, RULE_select);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(113);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case T__19:
			case T__27:
			case T__28:
			case T__30:
			case T__35:
			case T__36:
			case T__39:
			case T__40:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case T__46:
			case T__58:
			case T__67:
			case T__68:
			case T__69:
			case T__70:
			case T__71:
			case T__72:
			case T__73:
			case T__74:
			case T__75:
			case T__76:
			case T__77:
			case T__78:
			case T__79:
			case T__80:
			case T__81:
			case T__82:
			case T__83:
			case T__84:
			case T__85:
			case T__86:
			case T__87:
			case T__88:
			case T__89:
			case T__90:
			case LONG:
			case FLOAT:
			case NAME:
			case STRING:
				{
				}
				break;
			case T__33:
				{
				setState(111);
				match(T__33);
				}
				break;
			case T__34:
				{
				setState(112);
				match(T__34);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(118);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
			case 1:
				{
				}
				break;
			case 2:
				{
				setState(116);
				val(0);
				}
				break;
			case 3:
				{
				setState(117);
				columns();
				}
				break;
			}
			setState(123);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case T__19:
			case T__30:
			case T__35:
			case T__36:
			case T__39:
			case T__40:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case NAME:
				{
				}
				break;
			case T__27:
			case T__28:
				{
				setState(121);
				_la = _input.LA(1);
				if ( !(_la==T__27 || _la==T__28) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(122);
				tablename();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(128);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case T__19:
			case T__36:
			case T__39:
			case T__40:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
			case NAME:
				{
				}
				break;
			case T__30:
			case T__35:
				{
				setState(126);
				_la = _input.LA(1);
				if ( !(_la==T__30 || _la==T__35) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(127);
				expr(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(139);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case T__39:
			case T__40:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
				{
				}
				break;
			case T__19:
			case T__36:
			case NAME:
				{
				{
				setState(136);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case NAME:
					{
					}
					break;
				case T__36:
					{
					setState(132);
					match(T__36);
					setState(133);
					match(T__37);
					}
					break;
				case T__19:
					{
					setState(134);
					match(T__19);
					setState(135);
					match(T__38);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(138);
				group();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(156);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
				{
				}
				break;
			case T__39:
			case T__40:
				{
				{
				setState(146);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case T__39:
					{
					setState(142);
					match(T__39);
					setState(143);
					match(T__37);
					}
					break;
				case T__40:
					{
					setState(144);
					match(T__40);
					setState(145);
					match(T__38);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(148);
				order();
				setState(153);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__41) {
					{
					{
					setState(149);
					match(T__41);
					setState(150);
					order();
					}
					}
					setState(155);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(161);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case T__44:
			case T__45:
				{
				}
				break;
			case T__42:
			case T__43:
				{
				{
				setState(159);
				_la = _input.LA(1);
				if ( !(_la==T__42 || _la==T__43) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(160);
				offset();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(166);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
				{
				}
				break;
			case T__44:
			case T__45:
				{
				{
				setState(164);
				_la = _input.LA(1);
				if ( !(_la==T__44 || _la==T__45) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(165);
				limit();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
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
		public List<TerminalNode> NAME() { return getTokens(SqlParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(SqlParser.NAME, i);
		}
		public List<FuncContext> func() {
			return getRuleContexts(FuncContext.class);
		}
		public FuncContext func(int i) {
			return getRuleContext(FuncContext.class,i);
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
		enterRule(_localctx, 10, RULE_columns);
		int _la;
		try {
			int _alt;
			setState(184);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__46:
				enterOuterAlt(_localctx, 1);
				{
				setState(168);
				match(T__46);
				}
				break;
			case NAME:
				enterOuterAlt(_localctx, 2);
				{
				setState(169);
				match(NAME);
				setState(174);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(170);
						match(T__41);
						setState(171);
						match(NAME);
						}
						} 
					}
					setState(176);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,14,_ctx);
				}
				setState(181);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==T__41) {
					{
					{
					setState(177);
					match(T__41);
					setState(178);
					func();
					}
					}
					setState(183);
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
	public static class FuncContext extends ParserRuleContext {
		public SumContext sum() {
			return getRuleContext(SumContext.class,0);
		}
		public AvgContext avg() {
			return getRuleContext(AvgContext.class,0);
		}
		public CountContext count() {
			return getRuleContext(CountContext.class,0);
		}
		public MaxContext max() {
			return getRuleContext(MaxContext.class,0);
		}
		public MinContext min() {
			return getRuleContext(MinContext.class,0);
		}
		public FuncContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_func; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitFunc(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FuncContext func() throws RecognitionException {
		FuncContext _localctx = new FuncContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_func);
		try {
			setState(191);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__91:
			case T__92:
				enterOuterAlt(_localctx, 1);
				{
				setState(186);
				sum();
				}
				break;
			case T__93:
			case T__94:
				enterOuterAlt(_localctx, 2);
				{
				setState(187);
				avg();
				}
				break;
			case T__95:
			case T__96:
				enterOuterAlt(_localctx, 3);
				{
				setState(188);
				count();
				}
				break;
			case T__97:
			case T__98:
				enterOuterAlt(_localctx, 4);
				{
				setState(189);
				max();
				}
				break;
			case T__99:
			case T__100:
				enterOuterAlt(_localctx, 5);
				{
				setState(190);
				min();
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
	public static class TablenameContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
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
		enterRule(_localctx, 14, RULE_tablename);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(193);
			_la = _input.LA(1);
			if ( !(_la==NAME || _la==STRING) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
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
	public static class ExprContext extends ParserRuleContext {
		public Token op;
		public Token not;
		public Token cond;
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitExpr(this);
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
		int _startState = 16;
		enterRecursionRule(_localctx, 16, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(205);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NAME:
				{
				setState(196);
				match(NAME);
				setState(197);
				((ExprContext)_localctx).op = _input.LT(1);
				_la = _input.LA(1);
				if ( !(((_la) & ~0x3f) == 0 && ((1L << _la) & 576179277327106048L) != 0) ) {
					((ExprContext)_localctx).op = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(198);
				val(0);
				}
				break;
			case T__58:
				{
				setState(199);
				match(T__58);
				setState(200);
				expr(0);
				setState(201);
				match(T__59);
				}
				break;
			case T__64:
				{
				setState(203);
				((ExprContext)_localctx).not = match(T__64);
				setState(204);
				expr(1);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(212);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					{
					_localctx = new ExprContext(_parentctx, _parentState);
					pushNewRecursionContext(_localctx, _startState, RULE_expr);
					setState(207);
					if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
					setState(208);
					((ExprContext)_localctx).cond = _input.LT(1);
					_la = _input.LA(1);
					if ( !((((_la - 61)) & ~0x3f) == 0 && ((1L << (_la - 61)) & 15L) != 0) ) {
						((ExprContext)_localctx).cond = (Token)_errHandler.recoverInline(this);
					}
					else {
						if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
						_errHandler.reportMatch(this);
						consume();
					}
					setState(209);
					expr(3);
					}
					} 
				}
				setState(214);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,19,_ctx);
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
	public static class ValContext extends ParserRuleContext {
		public Token fg;
		public Token op;
		public List<TerminalNode> STRING() { return getTokens(SqlParser.STRING); }
		public TerminalNode STRING(int i) {
			return getToken(SqlParser.STRING, i);
		}
		public List<TerminalNode> FLOAT() { return getTokens(SqlParser.FLOAT); }
		public TerminalNode FLOAT(int i) {
			return getToken(SqlParser.FLOAT, i);
		}
		public List<TerminalNode> LONG() { return getTokens(SqlParser.LONG); }
		public TerminalNode LONG(int i) {
			return getToken(SqlParser.LONG, i);
		}
		public NullContext null_() {
			return getRuleContext(NullContext.class,0);
		}
		public TodateContext todate() {
			return getRuleContext(TodateContext.class,0);
		}
		public TodayContext today() {
			return getRuleContext(TodayContext.class,0);
		}
		public NowContext now() {
			return getRuleContext(NowContext.class,0);
		}
		public TimeContext time() {
			return getRuleContext(TimeContext.class,0);
		}
		public TostringContext tostring() {
			return getRuleContext(TostringContext.class,0);
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
		int _startState = 18;
		enterRecursionRule(_localctx, 18, RULE_val, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(251);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				{
				setState(216);
				match(STRING);
				setState(221);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(217);
						match(T__65);
						setState(218);
						match(STRING);
						}
						} 
					}
					setState(223);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,20,_ctx);
				}
				}
				break;
			case 2:
				{
				setState(224);
				match(FLOAT);
				setState(229);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(225);
						match(T__65);
						setState(226);
						match(FLOAT);
						}
						} 
					}
					setState(231);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,21,_ctx);
				}
				}
				break;
			case 3:
				{
				setState(232);
				match(LONG);
				setState(237);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
					if ( _alt==1 ) {
						{
						{
						setState(233);
						match(T__65);
						setState(234);
						match(LONG);
						}
						} 
					}
					setState(239);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,22,_ctx);
				}
				}
				break;
			case 4:
				{
				setState(240);
				null_();
				}
				break;
			case 5:
				{
				setState(241);
				todate();
				}
				break;
			case 6:
				{
				setState(242);
				today();
				}
				break;
			case 7:
				{
				setState(243);
				now();
				}
				break;
			case 8:
				{
				setState(244);
				time(0);
				}
				break;
			case 9:
				{
				setState(245);
				tostring();
				}
				break;
			case 10:
				{
				setState(246);
				tolong();
				}
				break;
			case 11:
				{
				setState(247);
				uuid();
				}
				break;
			case 12:
				{
				setState(248);
				objectid();
				}
				break;
			case 13:
				{
				setState(249);
				((ValContext)_localctx).fg = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==T__67 || _la==T__68) ) {
					((ValContext)_localctx).fg = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(250);
				val(1);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(261);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(259);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,24,_ctx) ) {
					case 1:
						{
						_localctx = new ValContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_val);
						setState(253);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(254);
						((ValContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__46 || _la==T__66) ) {
							((ValContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(255);
						val(4);
						}
						break;
					case 2:
						{
						_localctx = new ValContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_val);
						setState(256);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(257);
						((ValContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__67 || _la==T__68) ) {
							((ValContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(258);
						val(3);
						}
						break;
					}
					} 
				}
				setState(263);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,25,_ctx);
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
	public static class NullContext extends ParserRuleContext {
		public NullContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_null; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitNull(this);
			else return visitor.visitChildren(this);
		}
	}

	public final NullContext null_() throws RecognitionException {
		NullContext _localctx = new NullContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_null);
		try {
			setState(270);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__69:
				enterOuterAlt(_localctx, 1);
				{
				setState(264);
				match(T__69);
				}
				break;
			case T__70:
				enterOuterAlt(_localctx, 2);
				{
				setState(265);
				match(T__70);
				}
				break;
			case T__71:
				enterOuterAlt(_localctx, 3);
				{
				setState(266);
				match(T__71);
				setState(267);
				match(T__59);
				}
				break;
			case T__72:
				enterOuterAlt(_localctx, 4);
				{
				setState(268);
				match(T__72);
				setState(269);
				match(T__59);
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
	public static class TodateContext extends ParserRuleContext {
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
		enterRule(_localctx, 22, RULE_todate);
		int _la;
		try {
			setState(282);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,27,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(272);
				_la = _input.LA(1);
				if ( !(_la==T__73 || _la==T__74) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(273);
				time(0);
				setState(274);
				match(T__41);
				setState(275);
				match(STRING);
				setState(276);
				match(T__59);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(278);
				_la = _input.LA(1);
				if ( !(_la==T__73 || _la==T__74) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(279);
				time(0);
				setState(280);
				match(T__59);
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
	public static class TimeContext extends ParserRuleContext {
		public Token op;
		public TodayContext today() {
			return getRuleContext(TodayContext.class,0);
		}
		public NowContext now() {
			return getRuleContext(NowContext.class,0);
		}
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TerminalNode LONG() { return getToken(SqlParser.LONG, 0); }
		public List<TimeContext> time() {
			return getRuleContexts(TimeContext.class);
		}
		public TimeContext time(int i) {
			return getRuleContext(TimeContext.class,i);
		}
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
		int _startState = 24;
		enterRecursionRule(_localctx, 24, RULE_time, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(293);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,28,_ctx) ) {
			case 1:
				{
				setState(285);
				today();
				}
				break;
			case 2:
				{
				setState(286);
				now();
				}
				break;
			case 3:
				{
				setState(287);
				match(STRING);
				}
				break;
			case 4:
				{
				setState(288);
				match(LONG);
				}
				break;
			case 5:
				{
				setState(289);
				match(T__58);
				setState(290);
				time(0);
				setState(291);
				match(T__59);
				}
				break;
			}
			_ctx.stop = _input.LT(-1);
			setState(303);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(301);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,29,_ctx) ) {
					case 1:
						{
						_localctx = new TimeContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_time);
						setState(295);
						if (!(precpred(_ctx, 2))) throw new FailedPredicateException(this, "precpred(_ctx, 2)");
						setState(296);
						((TimeContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__67 || _la==T__68) ) {
							((TimeContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(297);
						time(3);
						}
						break;
					case 2:
						{
						_localctx = new TimeContext(_parentctx, _parentState);
						pushNewRecursionContext(_localctx, _startState, RULE_time);
						setState(298);
						if (!(precpred(_ctx, 1))) throw new FailedPredicateException(this, "precpred(_ctx, 1)");
						setState(299);
						((TimeContext)_localctx).op = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__67 || _la==T__68) ) {
							((TimeContext)_localctx).op = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(300);
						match(TIME);
						}
						break;
					}
					} 
				}
				setState(305);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,30,_ctx);
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
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public TodayContext today() {
			return getRuleContext(TodayContext.class,0);
		}
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
		enterRule(_localctx, 26, RULE_today);
		int _la;
		try {
			setState(320);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__75:
			case T__76:
				enterOuterAlt(_localctx, 1);
				{
				setState(306);
				_la = _input.LA(1);
				if ( !(_la==T__75 || _la==T__76) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(309);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case T__59:
					{
					}
					break;
				case STRING:
					{
					setState(308);
					match(STRING);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(311);
				match(T__59);
				}
				break;
			case T__77:
				enterOuterAlt(_localctx, 2);
				{
				setState(312);
				match(T__77);
				setState(313);
				today();
				setState(314);
				match(T__77);
				}
				break;
			case T__78:
				enterOuterAlt(_localctx, 3);
				{
				setState(316);
				match(T__78);
				setState(317);
				today();
				setState(318);
				match(T__78);
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
	public static class NowContext extends ParserRuleContext {
		public TerminalNode STRING() { return getToken(SqlParser.STRING, 0); }
		public NowContext now() {
			return getRuleContext(NowContext.class,0);
		}
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
		enterRule(_localctx, 28, RULE_now);
		int _la;
		try {
			setState(336);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__79:
			case T__80:
				enterOuterAlt(_localctx, 1);
				{
				setState(322);
				_la = _input.LA(1);
				if ( !(_la==T__79 || _la==T__80) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(325);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case T__59:
					{
					}
					break;
				case STRING:
					{
					setState(324);
					match(STRING);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(327);
				match(T__59);
				}
				break;
			case T__77:
				enterOuterAlt(_localctx, 2);
				{
				setState(328);
				match(T__77);
				setState(329);
				now();
				setState(330);
				match(T__77);
				}
				break;
			case T__78:
				enterOuterAlt(_localctx, 3);
				{
				setState(332);
				match(T__78);
				setState(333);
				now();
				setState(334);
				match(T__78);
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
	public static class TostringContext extends ParserRuleContext {
		public List<ValContext> val() {
			return getRuleContexts(ValContext.class);
		}
		public ValContext val(int i) {
			return getRuleContext(ValContext.class,i);
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
		enterRule(_localctx, 30, RULE_tostring);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(338);
			_la = _input.LA(1);
			if ( !(_la==T__81 || _la==T__82) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(339);
			val(0);
			setState(344);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__41) {
				{
				{
				setState(340);
				match(T__41);
				setState(341);
				val(0);
				}
				}
				setState(346);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(347);
			match(T__59);
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
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
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
			setState(349);
			_la = _input.LA(1);
			if ( !(_la==T__83 || _la==T__84) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(350);
			val(0);
			setState(351);
			match(T__59);
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
			setState(362);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__85:
			case T__86:
				enterOuterAlt(_localctx, 1);
				{
				setState(353);
				_la = _input.LA(1);
				if ( !(_la==T__85 || _la==T__86) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(356);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case T__59:
					{
					}
					break;
				case STRING:
					{
					setState(355);
					match(STRING);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(358);
				match(T__59);
				}
				break;
			case T__87:
			case T__88:
				enterOuterAlt(_localctx, 2);
				{
				setState(359);
				_la = _input.LA(1);
				if ( !(_la==T__87 || _la==T__88) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(360);
				match(STRING);
				setState(361);
				match(T__59);
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
	public static class ObjectidContext extends ParserRuleContext {
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
			setState(364);
			_la = _input.LA(1);
			if ( !(_la==T__89 || _la==T__90) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(365);
			match(STRING);
			setState(366);
			match(T__59);
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
	public static class SumContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public SumContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_sum; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitSum(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SumContext sum() throws RecognitionException {
		SumContext _localctx = new SumContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_sum);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(368);
			_la = _input.LA(1);
			if ( !(_la==T__91 || _la==T__92) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(369);
			match(NAME);
			setState(370);
			match(T__59);
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
	public static class AvgContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public AvgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_avg; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitAvg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final AvgContext avg() throws RecognitionException {
		AvgContext _localctx = new AvgContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_avg);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(372);
			_la = _input.LA(1);
			if ( !(_la==T__93 || _la==T__94) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(373);
			match(NAME);
			setState(374);
			match(T__59);
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
	public static class CountContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public CountContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_count; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitCount(this);
			else return visitor.visitChildren(this);
		}
	}

	public final CountContext count() throws RecognitionException {
		CountContext _localctx = new CountContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_count);
		int _la;
		try {
			setState(382);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,38,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(376);
				_la = _input.LA(1);
				if ( !(_la==T__95 || _la==T__96) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(377);
				match(NAME);
				setState(378);
				match(T__59);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(379);
				_la = _input.LA(1);
				if ( !(_la==T__95 || _la==T__96) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(380);
				match(T__46);
				setState(381);
				match(T__59);
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
	public static class MaxContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public MaxContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_max; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitMax(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MaxContext max() throws RecognitionException {
		MaxContext _localctx = new MaxContext(_ctx, getState());
		enterRule(_localctx, 44, RULE_max);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(384);
			_la = _input.LA(1);
			if ( !(_la==T__97 || _la==T__98) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(385);
			match(NAME);
			setState(386);
			match(T__59);
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
	public static class MinContext extends ParserRuleContext {
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
		public MinContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_min; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitMin(this);
			else return visitor.visitChildren(this);
		}
	}

	public final MinContext min() throws RecognitionException {
		MinContext _localctx = new MinContext(_ctx, getState());
		enterRule(_localctx, 46, RULE_min);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(388);
			_la = _input.LA(1);
			if ( !(_la==T__99 || _la==T__100) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(389);
			match(NAME);
			setState(390);
			match(T__59);
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
		enterRule(_localctx, 48, RULE_group);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(392);
			match(NAME);
			setState(397);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__41) {
				{
				{
				setState(393);
				match(T__41);
				setState(394);
				match(NAME);
				}
				}
				setState(399);
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
		public Token by;
		public TerminalNode NAME() { return getToken(SqlParser.NAME, 0); }
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
		enterRule(_localctx, 50, RULE_order);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(400);
			match(NAME);
			setState(403);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
			case T__41:
			case T__42:
			case T__43:
			case T__44:
			case T__45:
				{
				}
				break;
			case T__31:
			case T__32:
			case T__101:
			case T__102:
				{
				setState(402);
				((OrderContext)_localctx).by = _input.LT(1);
				_la = _input.LA(1);
				if ( !(_la==T__31 || _la==T__32 || _la==T__101 || _la==T__102) ) {
					((OrderContext)_localctx).by = (Token)_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				}
				break;
			default:
				throw new NoViableAltException(this);
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
		enterRule(_localctx, 52, RULE_offset);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(405);
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
		enterRule(_localctx, 54, RULE_limit);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(407);
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
	public static class SetContext extends ParserRuleContext {
		public SetvalueContext setvalue() {
			return getRuleContext(SetvalueContext.class,0);
		}
		public SetContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_set; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitSet(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetContext set() throws RecognitionException {
		SetContext _localctx = new SetContext(_ctx, getState());
		enterRule(_localctx, 56, RULE_set);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(409);
			_la = _input.LA(1);
			if ( !(_la==T__103 || _la==T__104) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(410);
			setvalue();
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
	public static class InsertContext extends ParserRuleContext {
		public TablenameContext tablename() {
			return getRuleContext(TablenameContext.class,0);
		}
		public ValueContext value() {
			return getRuleContext(ValueContext.class,0);
		}
		public ColumnsContext columns() {
			return getRuleContext(ColumnsContext.class,0);
		}
		public InsertContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_insert; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitInsert(this);
			else return visitor.visitChildren(this);
		}
	}

	public final InsertContext insert() throws RecognitionException {
		InsertContext _localctx = new InsertContext(_ctx, getState());
		enterRule(_localctx, 58, RULE_insert);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(412);
			_la = _input.LA(1);
			if ( !(_la==T__105 || _la==T__106) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(413);
			_la = _input.LA(1);
			if ( !(_la==T__107 || _la==T__108) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(414);
			tablename();
			setState(417);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__109:
			case T__110:
				{
				}
				break;
			case T__46:
			case NAME:
				{
				setState(416);
				columns();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(419);
			_la = _input.LA(1);
			if ( !(_la==T__109 || _la==T__110) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(420);
			match(T__58);
			setState(421);
			value();
			setState(422);
			match(T__59);
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
	public static class ValueContext extends ParserRuleContext {
		public List<ValContext> val() {
			return getRuleContexts(ValContext.class);
		}
		public ValContext val(int i) {
			return getRuleContext(ValContext.class,i);
		}
		public ValueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_value; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitValue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ValueContext value() throws RecognitionException {
		ValueContext _localctx = new ValueContext(_ctx, getState());
		enterRule(_localctx, 60, RULE_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(424);
			val(0);
			setState(429);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__41) {
				{
				{
				setState(425);
				match(T__41);
				setState(426);
				val(0);
				}
				}
				setState(431);
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
	public static class UpdateContext extends ParserRuleContext {
		public TablenameContext tablename() {
			return getRuleContext(TablenameContext.class,0);
		}
		public SetvalueContext setvalue() {
			return getRuleContext(SetvalueContext.class,0);
		}
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public UpdateContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_update; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitUpdate(this);
			else return visitor.visitChildren(this);
		}
	}

	public final UpdateContext update() throws RecognitionException {
		UpdateContext _localctx = new UpdateContext(_ctx, getState());
		enterRule(_localctx, 62, RULE_update);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(432);
			_la = _input.LA(1);
			if ( !(_la==T__111 || _la==T__112) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(433);
			tablename();
			setState(434);
			_la = _input.LA(1);
			if ( !(_la==T__103 || _la==T__104) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(435);
			setvalue();
			setState(439);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case EOF:
				{
				}
				break;
			case T__30:
			case T__35:
				{
				setState(437);
				_la = _input.LA(1);
				if ( !(_la==T__30 || _la==T__35) ) {
				_errHandler.recoverInline(this);
				}
				else {
					if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
					_errHandler.reportMatch(this);
					consume();
				}
				setState(438);
				expr(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
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
	public static class SetvalueContext extends ParserRuleContext {
		public List<TerminalNode> NAME() { return getTokens(SqlParser.NAME); }
		public TerminalNode NAME(int i) {
			return getToken(SqlParser.NAME, i);
		}
		public ValContext val() {
			return getRuleContext(ValContext.class,0);
		}
		public List<SetvalueContext> setvalue() {
			return getRuleContexts(SetvalueContext.class);
		}
		public SetvalueContext setvalue(int i) {
			return getRuleContext(SetvalueContext.class,i);
		}
		public SetvalueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_setvalue; }
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof SqlVisitor ) return ((SqlVisitor<? extends T>)visitor).visitSetvalue(this);
			else return visitor.visitChildren(this);
		}
	}

	public final SetvalueContext setvalue() throws RecognitionException {
		SetvalueContext _localctx = new SetvalueContext(_ctx, getState());
		enterRule(_localctx, 64, RULE_setvalue);
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(441);
			match(NAME);
			setState(444);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case NAME:
				{
				setState(442);
				match(NAME);
				}
				break;
			case T__58:
			case T__67:
			case T__68:
			case T__69:
			case T__70:
			case T__71:
			case T__72:
			case T__73:
			case T__74:
			case T__75:
			case T__76:
			case T__77:
			case T__78:
			case T__79:
			case T__80:
			case T__81:
			case T__82:
			case T__83:
			case T__84:
			case T__85:
			case T__86:
			case T__87:
			case T__88:
			case T__89:
			case T__90:
			case LONG:
			case FLOAT:
			case STRING:
				{
				setState(443);
				val(0);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			setState(450);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,45,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(446);
					match(T__41);
					setState(447);
					setvalue();
					}
					} 
				}
				setState(452);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,45,_ctx);
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

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 8:
			return expr_sempred((ExprContext)_localctx, predIndex);
		case 9:
			return val_sempred((ValContext)_localctx, predIndex);
		case 12:
			return time_sempred((TimeContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean val_sempred(ValContext _localctx, int predIndex) {
		switch (predIndex) {
		case 1:
			return precpred(_ctx, 3);
		case 2:
			return precpred(_ctx, 2);
		}
		return true;
	}
	private boolean time_sempred(TimeContext _localctx, int predIndex) {
		switch (predIndex) {
		case 3:
			return precpred(_ctx, 2);
		case 4:
			return precpred(_ctx, 1);
		}
		return true;
	}

	public static final String _serializedATN =
		"\u0004\u0001w\u01c6\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0002\u001a\u0007\u001a\u0002\u001b\u0007\u001b"+
		"\u0002\u001c\u0007\u001c\u0002\u001d\u0007\u001d\u0002\u001e\u0007\u001e"+
		"\u0002\u001f\u0007\u001f\u0002 \u0007 \u0001\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0000\u0001\u0000\u0001\u0000\u0003\u0000I\b\u0000\u0001\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u0002h\b\u0002"+
		"\u0003\u0002j\b\u0002\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0003\u0004r\b\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004w\b\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0003\u0004|\b\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003\u0004"+
		"\u0081\b\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004\u0089\b\u0004\u0001\u0004\u0003\u0004\u008c\b"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0003"+
		"\u0004\u0093\b\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0005\u0004\u0098"+
		"\b\u0004\n\u0004\f\u0004\u009b\t\u0004\u0003\u0004\u009d\b\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0003\u0004\u00a2\b\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0003\u0004\u00a7\b\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0005\u0005\u00ad\b\u0005\n\u0005\f\u0005\u00b0\t\u0005"+
		"\u0001\u0005\u0001\u0005\u0005\u0005\u00b4\b\u0005\n\u0005\f\u0005\u00b7"+
		"\t\u0005\u0003\u0005\u00b9\b\u0005\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006\u00c0\b\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0003\b\u00ce\b\b\u0001\b\u0001\b\u0001\b\u0005\b\u00d3\b\b"+
		"\n\b\f\b\u00d6\t\b\u0001\t\u0001\t\u0001\t\u0001\t\u0005\t\u00dc\b\t\n"+
		"\t\f\t\u00df\t\t\u0001\t\u0001\t\u0001\t\u0005\t\u00e4\b\t\n\t\f\t\u00e7"+
		"\t\t\u0001\t\u0001\t\u0001\t\u0005\t\u00ec\b\t\n\t\f\t\u00ef\t\t\u0001"+
		"\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0001\t\u0003\t\u00fc\b\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001\t\u0001"+
		"\t\u0005\t\u0104\b\t\n\t\f\t\u0107\t\t\u0001\n\u0001\n\u0001\n\u0001\n"+
		"\u0001\n\u0001\n\u0003\n\u010f\b\n\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0003\u000b\u011b\b\u000b\u0001\f\u0001\f\u0001\f\u0001\f"+
		"\u0001\f\u0001\f\u0001\f\u0001\f\u0001\f\u0003\f\u0126\b\f\u0001\f\u0001"+
		"\f\u0001\f\u0001\f\u0001\f\u0001\f\u0005\f\u012e\b\f\n\f\f\f\u0131\t\f"+
		"\u0001\r\u0001\r\u0001\r\u0003\r\u0136\b\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0003\r\u0141\b\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0003\u000e\u0146\b\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0003\u000e\u0151\b\u000e\u0001\u000f\u0001\u000f\u0001\u000f"+
		"\u0001\u000f\u0005\u000f\u0157\b\u000f\n\u000f\f\u000f\u015a\t\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0003\u0011\u0165\b\u0011\u0001\u0011\u0001"+
		"\u0011\u0001\u0011\u0001\u0011\u0003\u0011\u016b\b\u0011\u0001\u0012\u0001"+
		"\u0012\u0001\u0012\u0001\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001"+
		"\u0013\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0014\u0001\u0015\u0001"+
		"\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0001\u0015\u0003\u0015\u017f"+
		"\b\u0015\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0016\u0001\u0017\u0001"+
		"\u0017\u0001\u0017\u0001\u0017\u0001\u0018\u0001\u0018\u0001\u0018\u0005"+
		"\u0018\u018c\b\u0018\n\u0018\f\u0018\u018f\t\u0018\u0001\u0019\u0001\u0019"+
		"\u0001\u0019\u0003\u0019\u0194\b\u0019\u0001\u001a\u0001\u001a\u0001\u001b"+
		"\u0001\u001b\u0001\u001c\u0001\u001c\u0001\u001c\u0001\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0003\u001d\u01a2\b\u001d\u0001\u001d"+
		"\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001d\u0001\u001e\u0001\u001e"+
		"\u0001\u001e\u0005\u001e\u01ac\b\u001e\n\u001e\f\u001e\u01af\t\u001e\u0001"+
		"\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001\u001f\u0001"+
		"\u001f\u0003\u001f\u01b8\b\u001f\u0001 \u0001 \u0001 \u0003 \u01bd\b "+
		"\u0001 \u0001 \u0005 \u01c1\b \n \f \u01c4\t \u0001 \u0000\u0003\u0010"+
		"\u0012\u0018!\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016"+
		"\u0018\u001a\u001c\u001e \"$&(*,.02468:<>@\u0000!\u0001\u0000\u0001\u0002"+
		"\u0001\u0000\u000f\u0010\u0001\u0000\u0011\u0012\u0001\u0000\u0015\u001b"+
		"\u0001\u0000\u001c\u001d\u0001\u0000 !\u0002\u0000\u001f\u001f$$\u0001"+
		"\u0000+,\u0001\u0000-.\u0001\u0000tu\u0002\u0000\u0011\u00120:\u0001\u0000"+
		"=@\u0001\u0000DE\u0002\u0000//CC\u0001\u0000JK\u0001\u0000LM\u0001\u0000"+
		"PQ\u0001\u0000RS\u0001\u0000TU\u0001\u0000VW\u0001\u0000XY\u0001\u0000"+
		"Z[\u0001\u0000\\]\u0001\u0000^_\u0001\u0000`a\u0001\u0000bc\u0001\u0000"+
		"de\u0002\u0000 !fg\u0001\u0000hi\u0001\u0000jk\u0001\u0000lm\u0001\u0000"+
		"no\u0001\u0000pq\u01ff\u0000H\u0001\u0000\u0000\u0000\u0002J\u0001\u0000"+
		"\u0000\u0000\u0004i\u0001\u0000\u0000\u0000\u0006k\u0001\u0000\u0000\u0000"+
		"\bq\u0001\u0000\u0000\u0000\n\u00b8\u0001\u0000\u0000\u0000\f\u00bf\u0001"+
		"\u0000\u0000\u0000\u000e\u00c1\u0001\u0000\u0000\u0000\u0010\u00cd\u0001"+
		"\u0000\u0000\u0000\u0012\u00fb\u0001\u0000\u0000\u0000\u0014\u010e\u0001"+
		"\u0000\u0000\u0000\u0016\u011a\u0001\u0000\u0000\u0000\u0018\u0125\u0001"+
		"\u0000\u0000\u0000\u001a\u0140\u0001\u0000\u0000\u0000\u001c\u0150\u0001"+
		"\u0000\u0000\u0000\u001e\u0152\u0001\u0000\u0000\u0000 \u015d\u0001\u0000"+
		"\u0000\u0000\"\u016a\u0001\u0000\u0000\u0000$\u016c\u0001\u0000\u0000"+
		"\u0000&\u0170\u0001\u0000\u0000\u0000(\u0174\u0001\u0000\u0000\u0000*"+
		"\u017e\u0001\u0000\u0000\u0000,\u0180\u0001\u0000\u0000\u0000.\u0184\u0001"+
		"\u0000\u0000\u00000\u0188\u0001\u0000\u0000\u00002\u0190\u0001\u0000\u0000"+
		"\u00004\u0195\u0001\u0000\u0000\u00006\u0197\u0001\u0000\u0000\u00008"+
		"\u0199\u0001\u0000\u0000\u0000:\u019c\u0001\u0000\u0000\u0000<\u01a8\u0001"+
		"\u0000\u0000\u0000>\u01b0\u0001\u0000\u0000\u0000@\u01b9\u0001\u0000\u0000"+
		"\u0000BI\u0003\u0002\u0001\u0000CI\u0003\u0006\u0003\u0000DI\u0003\b\u0004"+
		"\u0000EI\u00038\u001c\u0000FI\u0003:\u001d\u0000GI\u0003>\u001f\u0000"+
		"HB\u0001\u0000\u0000\u0000HC\u0001\u0000\u0000\u0000HD\u0001\u0000\u0000"+
		"\u0000HE\u0001\u0000\u0000\u0000HF\u0001\u0000\u0000\u0000HG\u0001\u0000"+
		"\u0000\u0000I\u0001\u0001\u0000\u0000\u0000JK\u0007\u0000\u0000\u0000"+
		"KL\u0003\u0004\u0002\u0000L\u0003\u0001\u0000\u0000\u0000Mj\u0005\u0003"+
		"\u0000\u0000Nj\u0005\u0004\u0000\u0000Oj\u0005\u0005\u0000\u0000Pj\u0005"+
		"\u0006\u0000\u0000Qj\u0005\u0007\u0000\u0000Rj\u0005\b\u0000\u0000Sj\u0005"+
		"\t\u0000\u0000Tj\u0005\n\u0000\u0000Uj\u0005\u000b\u0000\u0000Vj\u0005"+
		"\f\u0000\u0000Wj\u0005\r\u0000\u0000XY\u0005\u000e\u0000\u0000Yj\u0003"+
		"\u000e\u0007\u0000Z[\u0007\u0001\u0000\u0000[\\\u0007\u0002\u0000\u0000"+
		"\\j\u0003\u000e\u0007\u0000]j\u0005\u0004\u0000\u0000^j\u0005\u0013\u0000"+
		"\u0000_j\u0005\u0014\u0000\u0000`a\u0007\u0003\u0000\u0000ab\u0007\u0004"+
		"\u0000\u0000bj\u0003\u000e\u0007\u0000cg\u0005\u001e\u0000\u0000dh\u0001"+
		"\u0000\u0000\u0000ef\u0005\u001f\u0000\u0000fh\u0003\u0010\b\u0000gd\u0001"+
		"\u0000\u0000\u0000ge\u0001\u0000\u0000\u0000hj\u0001\u0000\u0000\u0000"+
		"iM\u0001\u0000\u0000\u0000iN\u0001\u0000\u0000\u0000iO\u0001\u0000\u0000"+
		"\u0000iP\u0001\u0000\u0000\u0000iQ\u0001\u0000\u0000\u0000iR\u0001\u0000"+
		"\u0000\u0000iS\u0001\u0000\u0000\u0000iT\u0001\u0000\u0000\u0000iU\u0001"+
		"\u0000\u0000\u0000iV\u0001\u0000\u0000\u0000iW\u0001\u0000\u0000\u0000"+
		"iX\u0001\u0000\u0000\u0000iZ\u0001\u0000\u0000\u0000i]\u0001\u0000\u0000"+
		"\u0000i^\u0001\u0000\u0000\u0000i_\u0001\u0000\u0000\u0000i`\u0001\u0000"+
		"\u0000\u0000ic\u0001\u0000\u0000\u0000j\u0005\u0001\u0000\u0000\u0000"+
		"kl\u0007\u0005\u0000\u0000lm\u0003\u000e\u0007\u0000m\u0007\u0001\u0000"+
		"\u0000\u0000nr\u0001\u0000\u0000\u0000or\u0005\"\u0000\u0000pr\u0005#"+
		"\u0000\u0000qn\u0001\u0000\u0000\u0000qo\u0001\u0000\u0000\u0000qp\u0001"+
		"\u0000\u0000\u0000rv\u0001\u0000\u0000\u0000sw\u0001\u0000\u0000\u0000"+
		"tw\u0003\u0012\t\u0000uw\u0003\n\u0005\u0000vs\u0001\u0000\u0000\u0000"+
		"vt\u0001\u0000\u0000\u0000vu\u0001\u0000\u0000\u0000w{\u0001\u0000\u0000"+
		"\u0000x|\u0001\u0000\u0000\u0000yz\u0007\u0004\u0000\u0000z|\u0003\u000e"+
		"\u0007\u0000{x\u0001\u0000\u0000\u0000{y\u0001\u0000\u0000\u0000|\u0080"+
		"\u0001\u0000\u0000\u0000}\u0081\u0001\u0000\u0000\u0000~\u007f\u0007\u0006"+
		"\u0000\u0000\u007f\u0081\u0003\u0010\b\u0000\u0080}\u0001\u0000\u0000"+
		"\u0000\u0080~\u0001\u0000\u0000\u0000\u0081\u008b\u0001\u0000\u0000\u0000"+
		"\u0082\u008c\u0001\u0000\u0000\u0000\u0083\u0089\u0001\u0000\u0000\u0000"+
		"\u0084\u0085\u0005%\u0000\u0000\u0085\u0089\u0005&\u0000\u0000\u0086\u0087"+
		"\u0005\u0014\u0000\u0000\u0087\u0089\u0005\'\u0000\u0000\u0088\u0083\u0001"+
		"\u0000\u0000\u0000\u0088\u0084\u0001\u0000\u0000\u0000\u0088\u0086\u0001"+
		"\u0000\u0000\u0000\u0089\u008a\u0001\u0000\u0000\u0000\u008a\u008c\u0003"+
		"0\u0018\u0000\u008b\u0082\u0001\u0000\u0000\u0000\u008b\u0088\u0001\u0000"+
		"\u0000\u0000\u008c\u009c\u0001\u0000\u0000\u0000\u008d\u009d\u0001\u0000"+
		"\u0000\u0000\u008e\u008f\u0005(\u0000\u0000\u008f\u0093\u0005&\u0000\u0000"+
		"\u0090\u0091\u0005)\u0000\u0000\u0091\u0093\u0005\'\u0000\u0000\u0092"+
		"\u008e\u0001\u0000\u0000\u0000\u0092\u0090\u0001\u0000\u0000\u0000\u0093"+
		"\u0094\u0001\u0000\u0000\u0000\u0094\u0099\u00032\u0019\u0000\u0095\u0096"+
		"\u0005*\u0000\u0000\u0096\u0098\u00032\u0019\u0000\u0097\u0095\u0001\u0000"+
		"\u0000\u0000\u0098\u009b\u0001\u0000\u0000\u0000\u0099\u0097\u0001\u0000"+
		"\u0000\u0000\u0099\u009a\u0001\u0000\u0000\u0000\u009a\u009d\u0001\u0000"+
		"\u0000\u0000\u009b\u0099\u0001\u0000\u0000\u0000\u009c\u008d\u0001\u0000"+
		"\u0000\u0000\u009c\u0092\u0001\u0000\u0000\u0000\u009d\u00a1\u0001\u0000"+
		"\u0000\u0000\u009e\u00a2\u0001\u0000\u0000\u0000\u009f\u00a0\u0007\u0007"+
		"\u0000\u0000\u00a0\u00a2\u00034\u001a\u0000\u00a1\u009e\u0001\u0000\u0000"+
		"\u0000\u00a1\u009f\u0001\u0000\u0000\u0000\u00a2\u00a6\u0001\u0000\u0000"+
		"\u0000\u00a3\u00a7\u0001\u0000\u0000\u0000\u00a4\u00a5\u0007\b\u0000\u0000"+
		"\u00a5\u00a7\u00036\u001b\u0000\u00a6\u00a3\u0001\u0000\u0000\u0000\u00a6"+
		"\u00a4\u0001\u0000\u0000\u0000\u00a7\t\u0001\u0000\u0000\u0000\u00a8\u00b9"+
		"\u0005/\u0000\u0000\u00a9\u00ae\u0005t\u0000\u0000\u00aa\u00ab\u0005*"+
		"\u0000\u0000\u00ab\u00ad\u0005t\u0000\u0000\u00ac\u00aa\u0001\u0000\u0000"+
		"\u0000\u00ad\u00b0\u0001\u0000\u0000\u0000\u00ae\u00ac\u0001\u0000\u0000"+
		"\u0000\u00ae\u00af\u0001\u0000\u0000\u0000\u00af\u00b5\u0001\u0000\u0000"+
		"\u0000\u00b0\u00ae\u0001\u0000\u0000\u0000\u00b1\u00b2\u0005*\u0000\u0000"+
		"\u00b2\u00b4\u0003\f\u0006\u0000\u00b3\u00b1\u0001\u0000\u0000\u0000\u00b4"+
		"\u00b7\u0001\u0000\u0000\u0000\u00b5\u00b3\u0001\u0000\u0000\u0000\u00b5"+
		"\u00b6\u0001\u0000\u0000\u0000\u00b6\u00b9\u0001\u0000\u0000\u0000\u00b7"+
		"\u00b5\u0001\u0000\u0000\u0000\u00b8\u00a8\u0001\u0000\u0000\u0000\u00b8"+
		"\u00a9\u0001\u0000\u0000\u0000\u00b9\u000b\u0001\u0000\u0000\u0000\u00ba"+
		"\u00c0\u0003&\u0013\u0000\u00bb\u00c0\u0003(\u0014\u0000\u00bc\u00c0\u0003"+
		"*\u0015\u0000\u00bd\u00c0\u0003,\u0016\u0000\u00be\u00c0\u0003.\u0017"+
		"\u0000\u00bf\u00ba\u0001\u0000\u0000\u0000\u00bf\u00bb\u0001\u0000\u0000"+
		"\u0000\u00bf\u00bc\u0001\u0000\u0000\u0000\u00bf\u00bd\u0001\u0000\u0000"+
		"\u0000\u00bf\u00be\u0001\u0000\u0000\u0000\u00c0\r\u0001\u0000\u0000\u0000"+
		"\u00c1\u00c2\u0007\t\u0000\u0000\u00c2\u000f\u0001\u0000\u0000\u0000\u00c3"+
		"\u00c4\u0006\b\uffff\uffff\u0000\u00c4\u00c5\u0005t\u0000\u0000\u00c5"+
		"\u00c6\u0007\n\u0000\u0000\u00c6\u00ce\u0003\u0012\t\u0000\u00c7\u00c8"+
		"\u0005;\u0000\u0000\u00c8\u00c9\u0003\u0010\b\u0000\u00c9\u00ca\u0005"+
		"<\u0000\u0000\u00ca\u00ce\u0001\u0000\u0000\u0000\u00cb\u00cc\u0005A\u0000"+
		"\u0000\u00cc\u00ce\u0003\u0010\b\u0001\u00cd\u00c3\u0001\u0000\u0000\u0000"+
		"\u00cd\u00c7\u0001\u0000\u0000\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000"+
		"\u00ce\u00d4\u0001\u0000\u0000\u0000\u00cf\u00d0\n\u0002\u0000\u0000\u00d0"+
		"\u00d1\u0007\u000b\u0000\u0000\u00d1\u00d3\u0003\u0010\b\u0003\u00d2\u00cf"+
		"\u0001\u0000\u0000\u0000\u00d3\u00d6\u0001\u0000\u0000\u0000\u00d4\u00d2"+
		"\u0001\u0000\u0000\u0000\u00d4\u00d5\u0001\u0000\u0000\u0000\u00d5\u0011"+
		"\u0001\u0000\u0000\u0000\u00d6\u00d4\u0001\u0000\u0000\u0000\u00d7\u00d8"+
		"\u0006\t\uffff\uffff\u0000\u00d8\u00dd\u0005u\u0000\u0000\u00d9\u00da"+
		"\u0005B\u0000\u0000\u00da\u00dc\u0005u\u0000\u0000\u00db\u00d9\u0001\u0000"+
		"\u0000\u0000\u00dc\u00df\u0001\u0000\u0000\u0000\u00dd\u00db\u0001\u0000"+
		"\u0000\u0000\u00dd\u00de\u0001\u0000\u0000\u0000\u00de\u00fc\u0001\u0000"+
		"\u0000\u0000\u00df\u00dd\u0001\u0000\u0000\u0000\u00e0\u00e5\u0005s\u0000"+
		"\u0000\u00e1\u00e2\u0005B\u0000\u0000\u00e2\u00e4\u0005s\u0000\u0000\u00e3"+
		"\u00e1\u0001\u0000\u0000\u0000\u00e4\u00e7\u0001\u0000\u0000\u0000\u00e5"+
		"\u00e3\u0001\u0000\u0000\u0000\u00e5\u00e6\u0001\u0000\u0000\u0000\u00e6"+
		"\u00fc\u0001\u0000\u0000\u0000\u00e7\u00e5\u0001\u0000\u0000\u0000\u00e8"+
		"\u00ed\u0005r\u0000\u0000\u00e9\u00ea\u0005B\u0000\u0000\u00ea\u00ec\u0005"+
		"r\u0000\u0000\u00eb\u00e9\u0001\u0000\u0000\u0000\u00ec\u00ef\u0001\u0000"+
		"\u0000\u0000\u00ed\u00eb\u0001\u0000\u0000\u0000\u00ed\u00ee\u0001\u0000"+
		"\u0000\u0000\u00ee\u00fc\u0001\u0000\u0000\u0000\u00ef\u00ed\u0001\u0000"+
		"\u0000\u0000\u00f0\u00fc\u0003\u0014\n\u0000\u00f1\u00fc\u0003\u0016\u000b"+
		"\u0000\u00f2\u00fc\u0003\u001a\r\u0000\u00f3\u00fc\u0003\u001c\u000e\u0000"+
		"\u00f4\u00fc\u0003\u0018\f\u0000\u00f5\u00fc\u0003\u001e\u000f\u0000\u00f6"+
		"\u00fc\u0003 \u0010\u0000\u00f7\u00fc\u0003\"\u0011\u0000\u00f8\u00fc"+
		"\u0003$\u0012\u0000\u00f9\u00fa\u0007\f\u0000\u0000\u00fa\u00fc\u0003"+
		"\u0012\t\u0001\u00fb\u00d7\u0001\u0000\u0000\u0000\u00fb\u00e0\u0001\u0000"+
		"\u0000\u0000\u00fb\u00e8\u0001\u0000\u0000\u0000\u00fb\u00f0\u0001\u0000"+
		"\u0000\u0000\u00fb\u00f1\u0001\u0000\u0000\u0000\u00fb\u00f2\u0001\u0000"+
		"\u0000\u0000\u00fb\u00f3\u0001\u0000\u0000\u0000\u00fb\u00f4\u0001\u0000"+
		"\u0000\u0000\u00fb\u00f5\u0001\u0000\u0000\u0000\u00fb\u00f6\u0001\u0000"+
		"\u0000\u0000\u00fb\u00f7\u0001\u0000\u0000\u0000\u00fb\u00f8\u0001\u0000"+
		"\u0000\u0000\u00fb\u00f9\u0001\u0000\u0000\u0000\u00fc\u0105\u0001\u0000"+
		"\u0000\u0000\u00fd\u00fe\n\u0003\u0000\u0000\u00fe\u00ff\u0007\r\u0000"+
		"\u0000\u00ff\u0104\u0003\u0012\t\u0004\u0100\u0101\n\u0002\u0000\u0000"+
		"\u0101\u0102\u0007\f\u0000\u0000\u0102\u0104\u0003\u0012\t\u0003\u0103"+
		"\u00fd\u0001\u0000\u0000\u0000\u0103\u0100\u0001\u0000\u0000\u0000\u0104"+
		"\u0107\u0001\u0000\u0000\u0000\u0105\u0103\u0001\u0000\u0000\u0000\u0105"+
		"\u0106\u0001\u0000\u0000\u0000\u0106\u0013\u0001\u0000\u0000\u0000\u0107"+
		"\u0105\u0001\u0000\u0000\u0000\u0108\u010f\u0005F\u0000\u0000\u0109\u010f"+
		"\u0005G\u0000\u0000\u010a\u010b\u0005H\u0000\u0000\u010b\u010f\u0005<"+
		"\u0000\u0000\u010c\u010d\u0005I\u0000\u0000\u010d\u010f\u0005<\u0000\u0000"+
		"\u010e\u0108\u0001\u0000\u0000\u0000\u010e\u0109\u0001\u0000\u0000\u0000"+
		"\u010e\u010a\u0001\u0000\u0000\u0000\u010e\u010c\u0001\u0000\u0000\u0000"+
		"\u010f\u0015\u0001\u0000\u0000\u0000\u0110\u0111\u0007\u000e\u0000\u0000"+
		"\u0111\u0112\u0003\u0018\f\u0000\u0112\u0113\u0005*\u0000\u0000\u0113"+
		"\u0114\u0005u\u0000\u0000\u0114\u0115\u0005<\u0000\u0000\u0115\u011b\u0001"+
		"\u0000\u0000\u0000\u0116\u0117\u0007\u000e\u0000\u0000\u0117\u0118\u0003"+
		"\u0018\f\u0000\u0118\u0119\u0005<\u0000\u0000\u0119\u011b\u0001\u0000"+
		"\u0000\u0000\u011a\u0110\u0001\u0000\u0000\u0000\u011a\u0116\u0001\u0000"+
		"\u0000\u0000\u011b\u0017\u0001\u0000\u0000\u0000\u011c\u011d\u0006\f\uffff"+
		"\uffff\u0000\u011d\u0126\u0003\u001a\r\u0000\u011e\u0126\u0003\u001c\u000e"+
		"\u0000\u011f\u0126\u0005u\u0000\u0000\u0120\u0126\u0005r\u0000\u0000\u0121"+
		"\u0122\u0005;\u0000\u0000\u0122\u0123\u0003\u0018\f\u0000\u0123\u0124"+
		"\u0005<\u0000\u0000\u0124\u0126\u0001\u0000\u0000\u0000\u0125\u011c\u0001"+
		"\u0000\u0000\u0000\u0125\u011e\u0001\u0000\u0000\u0000\u0125\u011f\u0001"+
		"\u0000\u0000\u0000\u0125\u0120\u0001\u0000\u0000\u0000\u0125\u0121\u0001"+
		"\u0000\u0000\u0000\u0126\u012f\u0001\u0000\u0000\u0000\u0127\u0128\n\u0002"+
		"\u0000\u0000\u0128\u0129\u0007\f\u0000\u0000\u0129\u012e\u0003\u0018\f"+
		"\u0003\u012a\u012b\n\u0001\u0000\u0000\u012b\u012c\u0007\f\u0000\u0000"+
		"\u012c\u012e\u0005v\u0000\u0000\u012d\u0127\u0001\u0000\u0000\u0000\u012d"+
		"\u012a\u0001\u0000\u0000\u0000\u012e\u0131\u0001\u0000\u0000\u0000\u012f"+
		"\u012d\u0001\u0000\u0000\u0000\u012f\u0130\u0001\u0000\u0000\u0000\u0130"+
		"\u0019\u0001\u0000\u0000\u0000\u0131\u012f\u0001\u0000\u0000\u0000\u0132"+
		"\u0135\u0007\u000f\u0000\u0000\u0133\u0136\u0001\u0000\u0000\u0000\u0134"+
		"\u0136\u0005u\u0000\u0000\u0135\u0133\u0001\u0000\u0000\u0000\u0135\u0134"+
		"\u0001\u0000\u0000\u0000\u0136\u0137\u0001\u0000\u0000\u0000\u0137\u0141"+
		"\u0005<\u0000\u0000\u0138\u0139\u0005N\u0000\u0000\u0139\u013a\u0003\u001a"+
		"\r\u0000\u013a\u013b\u0005N\u0000\u0000\u013b\u0141\u0001\u0000\u0000"+
		"\u0000\u013c\u013d\u0005O\u0000\u0000\u013d\u013e\u0003\u001a\r\u0000"+
		"\u013e\u013f\u0005O\u0000\u0000\u013f\u0141\u0001\u0000\u0000\u0000\u0140"+
		"\u0132\u0001\u0000\u0000\u0000\u0140\u0138\u0001\u0000\u0000\u0000\u0140"+
		"\u013c\u0001\u0000\u0000\u0000\u0141\u001b\u0001\u0000\u0000\u0000\u0142"+
		"\u0145\u0007\u0010\u0000\u0000\u0143\u0146\u0001\u0000\u0000\u0000\u0144"+
		"\u0146\u0005u\u0000\u0000\u0145\u0143\u0001\u0000\u0000\u0000\u0145\u0144"+
		"\u0001\u0000\u0000\u0000\u0146\u0147\u0001\u0000\u0000\u0000\u0147\u0151"+
		"\u0005<\u0000\u0000\u0148\u0149\u0005N\u0000\u0000\u0149\u014a\u0003\u001c"+
		"\u000e\u0000\u014a\u014b\u0005N\u0000\u0000\u014b\u0151\u0001\u0000\u0000"+
		"\u0000\u014c\u014d\u0005O\u0000\u0000\u014d\u014e\u0003\u001c\u000e\u0000"+
		"\u014e\u014f\u0005O\u0000\u0000\u014f\u0151\u0001\u0000\u0000\u0000\u0150"+
		"\u0142\u0001\u0000\u0000\u0000\u0150\u0148\u0001\u0000\u0000\u0000\u0150"+
		"\u014c\u0001\u0000\u0000\u0000\u0151\u001d\u0001\u0000\u0000\u0000\u0152"+
		"\u0153\u0007\u0011\u0000\u0000\u0153\u0158\u0003\u0012\t\u0000\u0154\u0155"+
		"\u0005*\u0000\u0000\u0155\u0157\u0003\u0012\t\u0000\u0156\u0154\u0001"+
		"\u0000\u0000\u0000\u0157\u015a\u0001\u0000\u0000\u0000\u0158\u0156\u0001"+
		"\u0000\u0000\u0000\u0158\u0159\u0001\u0000\u0000\u0000\u0159\u015b\u0001"+
		"\u0000\u0000\u0000\u015a\u0158\u0001\u0000\u0000\u0000\u015b\u015c\u0005"+
		"<\u0000\u0000\u015c\u001f\u0001\u0000\u0000\u0000\u015d\u015e\u0007\u0012"+
		"\u0000\u0000\u015e\u015f\u0003\u0012\t\u0000\u015f\u0160\u0005<\u0000"+
		"\u0000\u0160!\u0001\u0000\u0000\u0000\u0161\u0164\u0007\u0013\u0000\u0000"+
		"\u0162\u0165\u0001\u0000\u0000\u0000\u0163\u0165\u0005u\u0000\u0000\u0164"+
		"\u0162\u0001\u0000\u0000\u0000\u0164\u0163\u0001\u0000\u0000\u0000\u0165"+
		"\u0166\u0001\u0000\u0000\u0000\u0166\u016b\u0005<\u0000\u0000\u0167\u0168"+
		"\u0007\u0014\u0000\u0000\u0168\u0169\u0005u\u0000\u0000\u0169\u016b\u0005"+
		"<\u0000\u0000\u016a\u0161\u0001\u0000\u0000\u0000\u016a\u0167\u0001\u0000"+
		"\u0000\u0000\u016b#\u0001\u0000\u0000\u0000\u016c\u016d\u0007\u0015\u0000"+
		"\u0000\u016d\u016e\u0005u\u0000\u0000\u016e\u016f\u0005<\u0000\u0000\u016f"+
		"%\u0001\u0000\u0000\u0000\u0170\u0171\u0007\u0016\u0000\u0000\u0171\u0172"+
		"\u0005t\u0000\u0000\u0172\u0173\u0005<\u0000\u0000\u0173\'\u0001\u0000"+
		"\u0000\u0000\u0174\u0175\u0007\u0017\u0000\u0000\u0175\u0176\u0005t\u0000"+
		"\u0000\u0176\u0177\u0005<\u0000\u0000\u0177)\u0001\u0000\u0000\u0000\u0178"+
		"\u0179\u0007\u0018\u0000\u0000\u0179\u017a\u0005t\u0000\u0000\u017a\u017f"+
		"\u0005<\u0000\u0000\u017b\u017c\u0007\u0018\u0000\u0000\u017c\u017d\u0005"+
		"/\u0000\u0000\u017d\u017f\u0005<\u0000\u0000\u017e\u0178\u0001\u0000\u0000"+
		"\u0000\u017e\u017b\u0001\u0000\u0000\u0000\u017f+\u0001\u0000\u0000\u0000"+
		"\u0180\u0181\u0007\u0019\u0000\u0000\u0181\u0182\u0005t\u0000\u0000\u0182"+
		"\u0183\u0005<\u0000\u0000\u0183-\u0001\u0000\u0000\u0000\u0184\u0185\u0007"+
		"\u001a\u0000\u0000\u0185\u0186\u0005t\u0000\u0000\u0186\u0187\u0005<\u0000"+
		"\u0000\u0187/\u0001\u0000\u0000\u0000\u0188\u018d\u0005t\u0000\u0000\u0189"+
		"\u018a\u0005*\u0000\u0000\u018a\u018c\u0005t\u0000\u0000\u018b\u0189\u0001"+
		"\u0000\u0000\u0000\u018c\u018f\u0001\u0000\u0000\u0000\u018d\u018b\u0001"+
		"\u0000\u0000\u0000\u018d\u018e\u0001\u0000\u0000\u0000\u018e1\u0001\u0000"+
		"\u0000\u0000\u018f\u018d\u0001\u0000\u0000\u0000\u0190\u0193\u0005t\u0000"+
		"\u0000\u0191\u0194\u0001\u0000\u0000\u0000\u0192\u0194\u0007\u001b\u0000"+
		"\u0000\u0193\u0191\u0001\u0000\u0000\u0000\u0193\u0192\u0001\u0000\u0000"+
		"\u0000\u01943\u0001\u0000\u0000\u0000\u0195\u0196\u0005r\u0000\u0000\u0196"+
		"5\u0001\u0000\u0000\u0000\u0197\u0198\u0005r\u0000\u0000\u01987\u0001"+
		"\u0000\u0000\u0000\u0199\u019a\u0007\u001c\u0000\u0000\u019a\u019b\u0003"+
		"@ \u0000\u019b9\u0001\u0000\u0000\u0000\u019c\u019d\u0007\u001d\u0000"+
		"\u0000\u019d\u019e\u0007\u001e\u0000\u0000\u019e\u01a1\u0003\u000e\u0007"+
		"\u0000\u019f\u01a2\u0001\u0000\u0000\u0000\u01a0\u01a2\u0003\n\u0005\u0000"+
		"\u01a1\u019f\u0001\u0000\u0000\u0000\u01a1\u01a0\u0001\u0000\u0000\u0000"+
		"\u01a2\u01a3\u0001\u0000\u0000\u0000\u01a3\u01a4\u0007\u001f\u0000\u0000"+
		"\u01a4\u01a5\u0005;\u0000\u0000\u01a5\u01a6\u0003<\u001e\u0000\u01a6\u01a7"+
		"\u0005<\u0000\u0000\u01a7;\u0001\u0000\u0000\u0000\u01a8\u01ad\u0003\u0012"+
		"\t\u0000\u01a9\u01aa\u0005*\u0000\u0000\u01aa\u01ac\u0003\u0012\t\u0000"+
		"\u01ab\u01a9\u0001\u0000\u0000\u0000\u01ac\u01af\u0001\u0000\u0000\u0000"+
		"\u01ad\u01ab\u0001\u0000\u0000\u0000\u01ad\u01ae\u0001\u0000\u0000\u0000"+
		"\u01ae=\u0001\u0000\u0000\u0000\u01af\u01ad\u0001\u0000\u0000\u0000\u01b0"+
		"\u01b1\u0007 \u0000\u0000\u01b1\u01b2\u0003\u000e\u0007\u0000\u01b2\u01b3"+
		"\u0007\u001c\u0000\u0000\u01b3\u01b7\u0003@ \u0000\u01b4\u01b8\u0001\u0000"+
		"\u0000\u0000\u01b5\u01b6\u0007\u0006\u0000\u0000\u01b6\u01b8\u0003\u0010"+
		"\b\u0000\u01b7\u01b4\u0001\u0000\u0000\u0000\u01b7\u01b5\u0001\u0000\u0000"+
		"\u0000\u01b8?\u0001\u0000\u0000\u0000\u01b9\u01bc\u0005t\u0000\u0000\u01ba"+
		"\u01bd\u0005t\u0000\u0000\u01bb\u01bd\u0003\u0012\t\u0000\u01bc\u01ba"+
		"\u0001\u0000\u0000\u0000\u01bc\u01bb\u0001\u0000\u0000\u0000\u01bd\u01c2"+
		"\u0001\u0000\u0000\u0000\u01be\u01bf\u0005*\u0000\u0000\u01bf\u01c1\u0003"+
		"@ \u0000\u01c0\u01be\u0001\u0000\u0000\u0000\u01c1\u01c4\u0001\u0000\u0000"+
		"\u0000\u01c2\u01c0\u0001\u0000\u0000\u0000\u01c2\u01c3\u0001\u0000\u0000"+
		"\u0000\u01c3A\u0001\u0000\u0000\u0000\u01c4\u01c2\u0001\u0000\u0000\u0000"+
		".Hgiqv{\u0080\u0088\u008b\u0092\u0099\u009c\u00a1\u00a6\u00ae\u00b5\u00b8"+
		"\u00bf\u00cd\u00d4\u00dd\u00e5\u00ed\u00fb\u0103\u0105\u010e\u011a\u0125"+
		"\u012d\u012f\u0135\u0140\u0145\u0150\u0158\u0164\u016a\u017e\u018d\u0193"+
		"\u01a1\u01ad\u01b7\u01bc\u01c2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}