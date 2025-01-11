create table gi_config
(
	id 			varchar not null,
	linkid 		varchar,
	s 			varchar,
	i 			int default 0,
	l 			bigint default 0,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create unique index gi_config_index_id on gi_config(id);
select create_distributed_table('gi_config', 'id', 'hash');

create table gi_user
(
	id 			bigint,
	name 		varchar,
	nickname 	varchar,
	title 		varchar,
	password 	varchar,
	md4passwd 	varchar,
	md5passwd 	varchar,
	locked 		int default 0,
	lockexpired bigint default 0,
	logintimes 	int default 0,
	lastlogintime bigint,
	lastattemptime bigint,
	lastloginip varchar,
	expired 	bigint,
	sid 		varchar,
	ip 			varchar,
	email 		varchar,
	phone 		varchar,
	photo 		varchar,
	lastfailtime bigint default 0,
	lastfailip 	varchar,
	createdip 	varchar,
	failtimes 	int default 0,
	deleted 	int default 0,
	updated 	bigint default 0,
	created 	bigint default 0,
	ajaxlogined bigint default 0,
	weblogined 	bigint default 0,
	_node 		varchar,
	desktop 	varchar
);
create unique index gi_user_indexid on gi_user(id);
create unique index gi_user_index_name on gi_user(name);
create index gi_user_index_deleted on gi_user(deleted);
create index gi_user_index_locked on gi_user(locked);
select create_distributed_table('gi_user', 'id', 'hash');

create table gi_userconfig
(
	created		bigint,
	data		text,
	id			varchar,
	name		varchar,
	sid			varchar,
	uid			bigint,
	updated		bigint	
);
create index gi_userconfig_indexid on gi_userconfig(id);
create index gi_userconfig_indexuid on gi_userconfig(uid);
create index gi_userconfig_indexuidname on gi_userconfig(uid,name);
select create_distributed_table('gi_userconfig', 'id', 'hash');

create table gi_userrole
(
	id 			varchar,
	uid 		bigint,
	rid 		bigint,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create index gi_userrole_index_uid on gi_userrole(uid);
create unique index gi_userrole_index_uid_rid on gi_userrole(uid, rid);
select create_distributed_table('gi_userrole', 'id', 'hash');

create table gi_role
(
	id 			bigint not null,
	name 		varchar,
	memo 		varchar,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create unique index gi_role_indexid on gi_role(id);
create index gi_role_index_name on gi_role(name);
select create_distributed_table('gi_role', 'id', 'hash');

create table gi_code
(
	id 			varchar,
	s1 			varchar,
	s2 			varchar,
	val 		varchar,
	expired 	bigint,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create unique index gi_code_index_s1_s2 on gi_code(s1, s2);
select create_distributed_table('gi_code', 'id', 'hash');

create table gi_access
(
	id 			varchar,
	memo 		varchar,
	_node 		varchar,
	updated 	bigint default 0,
	created 	bigint default 0
);
create unique index gi_access_indexid on gi_access(id);
select create_distributed_table('gi_access', 'id', 'hash');

create table gi_roleaccess
(
	id 			varchar,
	rid 		bigint,
	name 		varchar,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create index gi_roleaccess_indexid on gi_roleaccess(id);
create index gi_roleaccess_index_rid on gi_roleaccess(rid);
select create_distributed_table('gi_roleaccess', 'id', 'hash');

create table gi_menu
(
	id 			bigint,
	node 		varchar,
	parent 		bigint,
	name 		varchar,
	url 		varchar,
	classes 	varchar,
	click 		varchar,
	content 	varchar,
	tag 		varchar,
	access 		varchar,
	childs 		int default 0,
	seq 		int default 1000,
	tip 		varchar,
	style 		varchar,
	load1 		varchar,
	show1 		varchar,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create index gi_menu_indexid on gi_menu(id);
create index gi_menu_index_parent on gi_menu(parent);
create index gi_menu_index_name on gi_menu(name);
create index gi_menu_index_tag on gi_menu(tag);
create index gi_menu_index_seq on gi_menu(seq);
create index gi_menu_index_node on gi_menu(node);
select create_distributed_table('gi_menu', 'id', 'hash');

create table gi_jar
(
	id 			varchar,
	module 		varchar,
	name 		varchar,
	reset 		int,
	node 		varchar,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create index gi_jar_index_module on gi_jar(module);
create index gi_jar_index_name on gi_jar(name);
create index gi_jar_index_node on gi_jar(node);
select create_distributed_table('gi_jar', 'id', 'hash');

create table gi_authtoken
(
	id 			varchar,
	uid 		bigint,
	expired 	bigint,
	ip 			varchar,
	sid 		varchar,
	token 		varchar,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create index gi_authtoken_index_id on gi_authtoken(id);
create index gi_authtoken_index_uid on gi_authtoken(uid);
create index gi_authtoken_index_sid on gi_authtoken(sid);
create index gi_authtoken_index_token on gi_authtoken(token);
select create_distributed_table('gi_authtoken', 'id', 'hash');

create table gi_userlock
(
	id 			varchar,
	uid 		bigint,
	host 		varchar,
	useragent 	varchar,
	sid 		varchar,
	_node 		varchar,
	created 	bigint default 0,
	updated 	bigint default 0
);
create index gi_userlock_index_uid on gi_userlock(uid);
select create_distributed_table('gi_userlock', 'id', 'hash');

create table gi_app
(
	access		varchar[],
	accessed	bigint,
	allowip		varchar,
	appid		varchar,
	created		bigint,
	expired		bigint,
	id			bigint,
	ip			varchar,
	lastime		bigint,
	memo		varchar,
	name		varchar,
	secret		varchar,
	updated		bigint
);
create index gi_app_index_appid on gi_app(appid);
select create_distributed_table('gi_app', 'id', 'hash');

create table gi_node
(
	_usage		int,
	apps		varchar[],
	color		varchar,
	cores		int,
	created		bigint,
	dfileavgcost	bigint,
	dfilemaxcost	bigint,
	dfilemincost	bigint,
	dfiletimes	bigint,
	giiwa		varchar,
	id			varchar,
	ip			varchar,
	label		varchar,
	lastcheck	bigint,
	localdelay	int,
	localpending	int,
	localrunning	int,
	localthreads	int,
	mac			varchar[],
	mem			bigint,
	modules		varchar[],
	pid			varchar,
	tcp_closewait	int,
	tcp_established	int,
	timestamp	varchar,
	updated		bigint,
	uptime		bigint,
	requests	bigint,
	url			varchar
);
create index gi_node_index_id on gi_node(id);
select create_distributed_table('gi_node', 'id', 'hash');

create table gi_glog
(
	id 			varchar,
	node 		varchar,
	model 		varchar,
	op 			varchar,
	message 	text,
	trace 		text,
	uid 		bigint,
	ip 			varchar,
	type1 		int,
	level 		int,
	logger 		varchar,
	_node 		varchar,
	updated 	bigint,
	created 	bigint
);
create index gi_glog_index_id on gi_glog(id);
create index gi_glog_index_op on gi_glog(op);
create index gi_glog_index_uid on gi_glog(uid);
create index gi_glog_index_type on gi_glog(type1);
create index gi_glog_index_level on gi_glog(level);
select create_distributed_table('gi_glog', 'id', 'hash');

create table gi_message
(
	command		varchar,
	created		bigint,
	id			varchar,
	message		text,
	sid			varchar,
	updated		bigint
);
create index gi_message_index_id on gi_message(id);
select create_distributed_table('gi_message', 'id', 'hash');

create table gi_disk
(
	id 			bigint,
	node 		varchar,
	path 		varchar,
	priority 	int,
	lasttime 	bigint,
	bad 		int,
	total 		bigint,
	free 		bigint,
	enabled 	int,
	count 		bigint,
	checktime 	bigint,
	_node 		varchar,
	updated 	bigint,
	created 	bigint
);
create index gi_disk_index_id on gi_disk(id);
select create_distributed_table('gi_disk', 'id', 'hash');

create table gi_autobackup
(
	clean		int,
	command		varchar,
	created		bigint,
	days		varchar,
	enabled		int,
	id			bigint,
	keeps		int,
	name		varchar,
	nextime		bigint,
	nodes		varchar,
	state		int,
	_table		text,
	time		varchar,
	type		int,
	updated		bigint,
	url			varchar	
);
select create_distributed_table('gi_autobackup', 'id', 'hash');

create table gi_license
(
	code		varchar,
	content		text,
	created		bigint,
	id			varchar,
	updated		bigint
);
select create_distributed_table('gi_license', 'id', 'hash');

create table gi_blacklist
(
	created		bigint,
	id			bigint,
	ip			varchar,
	memo		varchar,
	times		bigint,
	updated		bigint,
	url			varchar
);
select create_distributed_table('gi_blacklist', 'id', 'hash');

create table gi_m_diskio
(
	_reads		bigint,
	created		bigint,
	id			varchar,
	node		varchar,
	path		varchar,
	queue		float,
	readbytes	bigint,
	updated		bigint,
	writebytes	bigint,
	writes		bigint	
);
select create_distributed_table('gi_m_diskio', 'id', 'hash');

create table gi_m_diskio_record
(
	_reads		bigint,
	created		bigint,
	id			varchar,
	node		varchar,
	path		varchar,
	queue		float,
	readbytes	bigint,
	updated		bigint,
	writebytes	bigint,
	writes		bigint
);
select create_distributed_table('gi_m_diskio_record', 'id', 'hash');

