grammar Sql;

fragment A : [aA]; fragment B : [bB]; fragment C : [cC]; fragment D : [dD];
fragment E : [eE]; fragment F : [fF]; fragment G : [gG]; fragment H : [hH];
fragment I : [iI]; fragment J : [jJ]; fragment K : [kK]; fragment L : [lL];
fragment M : [mM]; fragment N : [nN]; fragment O : [oO]; fragment P : [pP];
fragment Q : [qQ]; fragment R : [rR]; fragment S : [sS]; fragment T : [tT];
fragment U : [uU]; fragment V : [vV]; fragment W : [wW]; fragment X : [xX];
fragment Y : [yY]; fragment Z : [zZ];

// 关键字
AND         : A N D;
OR          : O R;
NOT         : N O T;
IN          : I N;
BETWEEN     : B E T W E E N;
LIKE        : L I K E;
SELECT      : S E L E C T;
FROM        : F R O M;
WHERE       : W H E R E;
GROUP       : G R O U P;
BY          : B Y;
ORDER       : O R D E R;
ASC         : A S C;
DESC        : D E S C;
OFFSET      : O F F S E T;
LIMIT       : L I M I T;
TODATE      : T O D A T E '(';
TODAY       : T O D A Y '(';
NOW         : N O W '(';
TOSTRING    : T O S T R I N G '(';
TODOUBLE    : T O D O U B L E '(';
TOFLOAT     : T O F L O A T '(';
TOLONG      : T O L O N G '(';
UUID        : U U I D '(';
OBJECTID    : O B J E C T I D '(';
FORMAT      : F O R M A T '(';
COUNT       : C O U N T '(';
HELP        : H E L P '(';
NULL        : N U L L;

NAME
    : ID
    | BACKTICK_NAME
;

ID      : [_a-zA-Z][a-zA-Z_0-9.]*;
BACKTICK_NAME : '`' ~[`]+ '`';

LONG    : [0-9]+;
FLOAT   : [0-9]+ '.' [0-9]+;

STRING
    : '\'' .*? '\''
    | '"' .*? '"'
;

TIME    : [0-9]+[a-zA-Z];
WS      : [ \t\r\n]+ -> skip;

select
    : SELECT?
      columns?
      FROM? tablename?
      WHERE? expr?
      (GROUP BY group)?
      (ORDER BY order (',' order)*)?
      ( (OFFSET offset (LIMIT limit)?) | (LIMIT limit (OFFSET offset)?) )?
;

columns
    : '*'
    | columnItem (',' columnItem)*
;

columnItem
    : NAME
    | COUNT '*' ')'
    | COUNT columnItem ')'
    | HELP ')'
    | val
;

tablename
    : NAME                  // 单名称 NAME / `name`
    | NAME '.' NAME         // 库.表：NAME.NAME 支持`db`.`tbl`
    | STRING
    | STRING '.' STRING
;

expr
    : '(' expr ')'                                      # exprParen
    | NOT expr                                          # exprNot
    | NAME BETWEEN val AND val                          # exprBetween
    | NAME IN inValueList                               # exprIn
    | NAME op=('>=' | '>' | '<=' | '<' | '!=' | '<>' | '==' | '=' | LIKE) valOrList  # exprCompare
    | expr AND expr                                     # exprAnd
    | expr OR expr                                      # exprOr
;

valOrList
    : val (',' val)*
    | val ('|' val)*            //兼容老版本
;

inValueList
    :'(' val (',' val)* ')'
    |'[' val (',' val)* ']'     //扩展习惯
;

val
    : STRING
    | FLOAT
    | LONG
    | NULL
    | todate
    | time
    | tostring
    | todouble
    | tofloat
    | tolong
    | uuid
    | objectid
    | format
    | val op=('*'|'/') val
    | val op=('+'|'-') val
    | fg=('+'|'-') val
;

todate
    : TODATE time (',' STRING)? ')'
;

format
    : FORMAT val ',' STRING ')'
;

time
    : today
    | now
    | todate                    //用于时间格式化
    | '(' time ')'
    | time op=('+'|'-') time
    | time op=('+'|'-') TIME
    | LONG                      //用于时间格式化
    | STRING                    //用于时间格式化
;

today
    : TODAY (STRING)? ')'
;

now
    : NOW (STRING)? ')'
;

tostring
    : TOSTRING val ')'
;

todouble
    : TODOUBLE val (',' STRING)? ')'
;

tofloat
    : TOFLOAT val (',' STRING)? ')'
;

tolong
    : TOLONG val (',' STRING)? ')'
;

uuid
    : UUID (STRING)? ')'
;

objectid
    : OBJECTID (STRING)? ')'
;

group   : NAME (',' NAME)*;
order   : NAME (ASC | DESC)?;
offset  : LONG;
limit   : LONG;
