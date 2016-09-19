#drop table if exists gi_config;
create table gi_config
(
	id varchar(128) not null,
	linkid varchar(50),
	s varchar(8192),
	i int default 0,
	l bigint default 0,
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_config_index_id on gi_config(id);

#drop table if exists dual;
create table dual
(
	x varchar(1)
);
insert into dual values('x');

#drop table if exists gi_user;
create table gi_user
(
	id bigint,
	name varchar(50),
	nickname varchar(255),
	title varchar(255),
	password varchar(255),
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
	failtimes int default 0,
	deleted int default 0,
	updated bigint default 0,
	created bigint default 0
);
create unique index gi_user_indexid on gi_user(id);
create index gi_user_index_name on gi_user(name);
create index gi_user_index_deleted on gi_user(deleted);
create index gi_user_index_locked on gi_user(locked);

#drop table if exists gi_userrole;
create table gi_userrole
(
	uid bigint,
	rid bigint,
	created bigint default 0,
	updated bigint default 0
);
create index gi_userrole_index_uid on gi_userrole(uid);
create unique index gi_userrole_index_uid_rid on gi_userrole(uid, rid);

#drop table if exists gi_role;
create table gi_role
(
	id bigint not null,
	name varchar(100),
	memo varchar(255),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_role_indexid on gi_role(id);
create index gi_role_index_name on gi_role(name);

#drop table if exists gi_code;
create table gi_code
(
	s1 varchar(50),
	s2 varchar(50),
	expired bigint,
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_code_index_s1_s2 on gi_code(s1, s2);

#drop table if exists gi_access;
create table gi_access
(
	id varchar(255),
	created bigint default 0,
	updated bigint default 0,
	created bigint default 0
);
create unique index gi_access_indexid on gi_access(id);
alter table gi_access add created bigint default 0;

#drop table if exists gi_roleaccess;
create table gi_roleaccess
(
	id varchar(20),
	rid bigint,
	name varchar(255),
	created bigint default 0,
	updated bigint default 0
);
create index gi_roleaccess_indexid on gi_roleaccess(id);
create index gi_roleaccess_index_rid on gi_roleaccess(rid);

#drop table if exists gi_repo;
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
	memo varchar(1024),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_repo_indexid on gi_repo(id);
create index gi_repo_index_uid on gi_repo(uid);
create index gi_repo_index_name on gi_repo(name);
create index gi_repo_index_folder on gi_repo(folder);
create index gi_repo_index_tag on gi_repo(tag);
create index gi_repo_index_expired on gi_repo(expired);

#drop table if exists gi_menu;
create table gi_menu
(
	id bigint,
	node varchar(50),
	parent bigint,
	name varchar(50),
	url varchar(255),
	classes varchar(100),
	click varchar(255),
	content varchar(4096),
	tag varchar(20),
	access varchar(1024),
	childs int default 0,
	seq int default 1000,
	tip varchar(255),
	style varchar(255),
	load varchar(255),
	created bigint default 0,
	updated bigint default 0
);
create index gi_menu_indexid on gi_menu(id);
create index gi_menu_index_parent on gi_menu(parent);
create index gi_menu_index_name on gi_menu(name);
create index gi_menu_index_tag on gi_menu(tag);
create index gi_menu_index_seq on gi_menu(seq);
create index gi_menu_index_node on gi_menu(node);

alter table gi_menu add updated bigint default 0;
alter table gi_menu add created bigint default 0;

#drop table if exists gi_accesslog;
create table gi_accesslog 
(
	uid bigint,
	cost bigint,
	module varchar(128),
	model varchar(128),
	method varchar(20),
	ip varchar(20),
	client varchar(128),
	id varchar(20),
	url varchar(128),
	sid varchar(50),
	username varchar(50),
	status int,
	created bigint default 0,
	updated bigint default 0
);
create index gi_accesslog_index_uid on gi_accesslog(uid);
create index gi_accesslog_index_method on gi_accesslog(method);

alter table gi_accesslog add updated bigint default 0;

##upgrade
alter table gi_accesslog add module varchar(128);
alter table gi_accesslog add model varchar(128);


#drop table if exists gi_jar;
create table gi_jar
(
	id varchar(20),
	module varchar(50),
	name varchar(50),
	reset int,
	node varchar(50),
	created bigint default 0,
	updated bigint default 0
);
create index gi_jar_index_module on gi_jar(module);
create index gi_jar_index_name on gi_jar(name);
create index gi_jar_index_node on gi_jar(node);

#drop table if exists gi_authtoken;
create table gi_authtoken
(
	id varchar(20),
	uid bigint,
	expired bigint,
	ip varchar(20),
	sid varchar(50),
	token varchar(50),
	created bigint default 0,
	updated bigint default 0
);
create index gi_authtoken_index_id on gi_authtoken(id);
create index gi_authtoken_index_uid on gi_authtoken(uid);
create index gi_authtoken_index_sid on gi_authtoken(sid);
create index gi_authtoken_index_token on gi_authtoken(token);

#drop table if exists gi_oplog;
create table gi_oplog
(
	id varchar(20),
	node varchar(50),
	model varchar(50),
	op varchar(50),
	uid bigint,
	ip varchar(20),
	type int,
	message varchar(1024),
	trace varchar(8192),
	created bigint default 0,
	updated bigint default 0
);
create index gi_oplog_index_created on gi_oplog(created);
create index gi_oplog_index_type on gi_oplog(type);
create index gi_oplog_index_uid on gi_oplog(uid);
create index gi_oplog_index_node on gi_oplog(node);
create index gi_oplog_index_model on gi_oplog(model);
create index gi_oplog_index_op on gi_oplog(op);

#drop table if exists gi_userlock;
create table gi_userlock
(
	uid bigint,
	host varchar(20),
	useragent varchar(255),
	sid varchar(50),
	created bigint default 0,
	updated bigint default 0
);
create index gi_userlock_index_uid on gi_userlock(uid);
