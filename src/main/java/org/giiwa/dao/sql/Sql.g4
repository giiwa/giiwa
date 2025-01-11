grammar Sql;

LONG : [0-9]+;
FLOAT : [0-9\\.]+;
NAME: [\\]*[_a-zA-Z][a-zA-Z_0-9\\.]*;
STRING: '\'' (.)*? '\''
	|'"' (.)*? '"'
	;
TIME: [0-9]+[dDhHmMsS];

WS: [ \t\r\n]+ -> skip;

stat: show
	| desc
	| select
	| set
	| insert
	| update
	;

/*
 * show databases
 * show dbs
 */
show: ('show'|'SHOW') showoptions;

showoptions: ('dbs' | 'DBS' | 'databases' | 'DATABASES' | 'ENGINES' | 'COLLATION' | 'CHARACTER SET' | 'groups' | 'GROUPS' | 'tables' | 'TABLES' |  'CREATE TABLE' tablename| ('table'|'TABLE') ('like'|'LIKE') tablename | 'DBS' | 'DATABASE' | 'GROUP' | ('columns'|'COLUMNS'|'full columns'|'FULL COLUMNS'|'full fields'|'FULL FIELDS'|'INDEX') ('from'|'FROM') tablename | 'FULL TABLES' (|'WHERE' expr))
	;

desc: ('desc'|'DESC') tablename
	;
	
/*
 * select * from table1 .... 
 * select * from () where () group () order by () offset ? limit ?
 * 
 */
select: (| 'select'|'SELECT') (| val | columns) (|('from'|'FROM') tablename) (|('where'|'WHERE') expr) (|((|'group' 'by'|'GROUP' 'BY') group)) (|(('order' 'by'|'ORDER' 'BY') order (',' order)*)) (|(('offset'|'OFFSET') offset)) (|(('limit'|'LIMIT') limit));

/*
 * *
 * a,b,c
 * a.b, a.c
 * a, count(*), sum(b), max(c)
 */
columns: '*'
	| NAME (',' NAME)* (',' func)*
	;
func: sum
	| avg
	| count
	| max
	| min
	;
tablename: NAME
	| STRING
	;

/*
 * a >= ... 
 * a > ...
 * a <= ...
 * a < ...
 * a != ...
 * a = ...
 * a like ...
 * a !like ...
 * ()
 * () and () or ()
 */
expr: NAME op=('>=' | '>' | '<=' | '<' | '!=' | '==' | '=' | 'like' | 'LIKE' | '!like' | '!LIKE' | 'not like' | 'NOT LIKE') val
    | '(' expr ')' 
	| expr cond=('and' | 'or' | 'AND' | 'OR') expr
	| not='not' expr
    ;

/*
 * 'string1|string2' => string1 or string2
 * 1.1|1.2 => 12.1 or 11.2
 * 1|2 => 1 or 2
 * null
 * todate(...)
 * today(...)
 * uuid(...)
 * () * ()
 * () / ()
 * () + ()
 * () - ()
 * 
 */
val: STRING ('|' STRING)*
	| FLOAT ('|' FLOAT)*
	| LONG ('|' LONG)*
	| null
	| todate
	| today
	| now
	| time
	| tostring
	| tolong
	| uuid
	| objectid
	| val op=('*'|'/') val
	| val op=('+'|'-') val
	| fg=('+'|'-') val
	;
	
/*
 * a = null
 * a = null()
 */	
null: 'null'
	| 'NULL'
	| 'null(' ')'
	| 'NULL(' ')'
	;

/*
 * todate('20220101', 'yyyyMMdd')
 * todate(20220101, 'yyyyMMdd')
 * todate(20220101 + 2, 'yyyyMMdd')
 * todate(today())
 * todate(today() - 1000*60*60*24)
 * todate(today('yyyyMMdd') - 7, 'yyyyMMdd')
 * ...
 */
todate: ('todate('|'TODATE(') time ',' STRING ')'
	| ('todate('|'TODATE(') time ')'
	;
	
/*
 * today(...)
 * 'string'
 * long
 * (time)
 * () * () 
 * () / ()
 * () + ()
 * () - ()
 */
time: today
	| now
	| STRING
	| LONG
	| '(' time ')'
	| time op=('+'|'-') time
	| time op=('+'|'-') TIME
	;

/*
 * today() => long
 * today('yyyy-MM-dd') => string
 * today('yyyyMMdd') => int
 * 'today(...)'
 * "today(...)"
 */	
today: ('today('|'TODAY(') (|STRING) ')'
	| '\'' today '\''
	| '"' today '"'
	;

/*
 * now() => long
 * now('yyyy-MM-dd') => string
 * now('yyyyMMdd') => int
 * 'now(...)'
 * "now(...)"
 */	
now: ('now('|'NOW') (|STRING) ')'
	| '\'' now '\''
	| '"' now '"'
	;
	
/*
 * tostring(a,b,c)
 */
tostring: ('tostring('|'TOSTRING(') val (',' val)* ')';

/*
 * tolong(val)
 */
tolong: ('tolong('|'TOLONG(') val ')';

/*
 * uuid('...')
 * touuid('...')
 */
uuid: ('uuid('|'UUID(') (|STRING) ')'
	|('touuid('|'TOUUID(') STRING ')'
	;

/*
 * objectid('...')
 */
objectid: ('objectid('|'OBJECTID(') STRING ')'
	;

/*
 * sum(name)
 */
sum: ('sum('|'SUM(') NAME ')';

/*
 * avg(name)
 */
avg: ('avg('|'AVG') NAME ')';

/*
 * count(*)
 * count(name)
 */
count: ('count('|'COUNT(') NAME ')'
	|('count('|'COUNT(') '*' ')'
	;

/*
 * max(name)
 */
max: ('max('|'MAX(') NAME ')';

/*
 * min(name)
 */
min: ('min('|'MIN(') NAME ')';

group: NAME (',' NAME)*;
order: NAME (|by=('asc' | 'desc'|'ASC'|'DESC'));
offset: LONG;
limit: LONG;

/*
 * set name value
 */
set: ('set'|'SET') setvalue
	;
	
/*
 * insert into table 
 */
insert: ('insert'|'INSERT') ('into'|'INTO') tablename (|columns) ('value'|'VALUE') '(' value ')' 
	;
value: val (',' val)*
	;

update: ('update'|'UPDATE') tablename ('set'|'SET') setvalue (|('where'|'WHERE') expr)
	;

setvalue: NAME (NAME|val) (',' setvalue)*
	;