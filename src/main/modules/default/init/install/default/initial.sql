create table gi_config
(
	id varchar(128) not null,
	linkid varchar(50),
	s varchar,
	i int default 0,
	l bigint default 0,
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_config_index_id on gi_config(id);

create table gi_user
(
	id bigint,
	name varchar(50),
	nickname varchar(255),
	title varchar(255),
	password varchar(255),
	md4passwd varchar(128),
	md5passwd varchar(128),
	locked int default 0,
	lockexpired bigint default 0,
	logintimes int default 0,
	lastlogintime bigint,
	lastattemptime bigint,
	lastloginip varchar(20),
	expired bigint,
	sid varchar(50),
	ip varchar(20),
	email varchar(100),
	phone varchar(50),
	photo varchar(255),
	lastfailtime bigint default 0,
	lastfailip varchar(30),
	createdip varchar(50),
	failtimes int default 0,
	deleted int default 0,
	updated bigint default 0,
	created bigint default 0,
	ajaxlogined bigint default 0,
	weblogined bigint default 0,
	_node varchar(40),
	desktop varchar(255)
);
create unique index gi_user_indexid on gi_user(id);
create unique index gi_user_index_name on gi_user(name);
create index gi_user_index_deleted on gi_user(deleted);
create index gi_user_index_locked on gi_user(locked);

create table gi_userrole
(
	uid bigint,
	rid bigint,
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create index gi_userrole_index_uid on gi_userrole(uid);
create unique index gi_userrole_index_uid_rid on gi_userrole(uid, rid);

create table gi_role
(
	id bigint not null,
	name varchar(100),
	memo varchar(255),
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_role_indexid on gi_role(id);
create index gi_role_index_name on gi_role(name);

create table gi_code
(
	s1 varchar(1024),
	s2 varchar(1024),
	val varchar,
	expired bigint,
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_code_index_s1_s2 on gi_code(s1, s2);

create table gi_access
(
	id varchar(255),
	memo varchar,
	_node varchar(40),
	updated bigint default 0,
	created bigint default 0
);
create unique index gi_access_indexid on gi_access(id);

create table gi_roleaccess
(
	id varchar(20),
	rid bigint,
	name varchar(255),
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create index gi_roleaccess_indexid on gi_roleaccess(id);
create index gi_roleaccess_index_rid on gi_roleaccess(rid);

create table gi_repo
(
	uid bigint,
	id varchar(20),
	folder varchar(255),
	name varchar(255),
	total bigint,
	pos bigint,
	flag int,
	tag varchar(20),
	expired bigint,
	memo varchar,
	ip varchar(20),
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_repo_indexid on gi_repo(id);
create index gi_repo_index_uid on gi_repo(uid);
create index gi_repo_index_name on gi_repo(name);
create index gi_repo_index_folder on gi_repo(folder);
create index gi_repo_index_tag on gi_repo(tag);
create index gi_repo_index_expired on gi_repo(expired);

create table gi_menu
(
	id bigint,
	node varchar(50),
	parent bigint,
	name varchar(50),
	url varchar(255),
	classes varchar(100),
	click varchar(255),
	content varchar,
	tag varchar(20),
	access varchar,
	childs int default 0,
	seq int default 1000,
	tip varchar(255),
	style varchar(255),
	load1 varchar(255),
	show1 varchar(255),
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create index gi_menu_indexid on gi_menu(id);
create index gi_menu_index_parent on gi_menu(parent);
create index gi_menu_index_name on gi_menu(name);
create index gi_menu_index_tag on gi_menu(tag);
create index gi_menu_index_seq on gi_menu(seq);
create index gi_menu_index_node on gi_menu(node);

create table gi_jar
(
	id varchar(20),
	module varchar(50),
	name varchar(50),
	reset int,
	node varchar(50),
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create index gi_jar_index_module on gi_jar(module);
create index gi_jar_index_name on gi_jar(name);
create index gi_jar_index_node on gi_jar(node);

create table gi_authtoken
(
	id varchar(20),
	uid bigint,
	expired bigint,
	ip varchar(128),
	sid varchar(50),
	token varchar(50),
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create index gi_authtoken_index_id on gi_authtoken(id);
create index gi_authtoken_index_uid on gi_authtoken(uid);
create index gi_authtoken_index_sid on gi_authtoken(sid);
create index gi_authtoken_index_token on gi_authtoken(token);

create table gi_userlock
(
	id varchar(50),
	uid bigint,
	host varchar(20),
	useragent varchar(255),
	sid varchar(50),
	_node varchar(40),
	created bigint default 0,
	updated bigint default 0
);
create index gi_userlock_index_uid on gi_userlock(uid);

create table gi_app
(
	id bigint,
	appid varchar(50),
	memo varchar(255),
	secret varchar(128),
	ip varchar(25),
	lastime bigint,
	expired bigint,
	role bigint,
	_node varchar(40),
	updated bigint,
	created bigint
);
create index gi_app_index_appid on gi_app(appid);

create table gi_node
(
	id varchar(50),
	ip varchar(100),
	name varchar(20),
	uptime bigint,
	cores int,
	os varchar(100),
	label varchar(100),
	mem bigint,
	_usage int,
	tasks int,
	globaltasks int,
	localthreads int,
	localpending int,
	localrunning int,
	giiwa varchar(100),
	pid varchar(10),
	url varchar(255),
	_node varchar(40),
	updated bigint,
	created bigint
);
create index gi_node_index_id on gi_node(id);

create table gi_glog
(
	id varchar(50),
	node varchar(100),
	model varchar(100),
	op varchar(100),
	message text,
	trace text,
	uid bigint,
	ip varchar(50),
	type1 int,
	level int,
	logger varchar(255),
	_node varchar(40),
	updated bigint,
	created bigint
);
create index gi_glog_index_id on gi_glog(id);
create index gi_glog_index_op on gi_glog(op);
create index gi_glog_index_uid on gi_glog(uid);
create index gi_glog_index_type on gi_glog(type1);
create index gi_glog_index_level on gi_glog(level);

create table gi_message
(
	id bigint,
	refer bigint,
	tag varchar(20),
	touid bigint,
	fromuid bigint,
	priority int,
	flag int,
	star int,
	title varchar(255),
	content varchar,
	_node varchar(40),
	updated bigint,
	created bigint
);
create index gi_message_index_id on gi_message(id);
create index gi_message_index_touid on gi_message(touid);

create table gi_disk
(
	id bigint,
	node varchar(40),
	path varchar(128),
	priority int,
	lasttime bigint,
	bad int,
	total bigint,
	free bigint,
	enabled int,
	count bigint,
	checktime bigint,
	_node varchar(40),
	updated bigint,
	created bigint
);
create index gi_disk_index_id on gi_disk(id);
