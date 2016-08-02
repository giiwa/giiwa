#drop table if exists gi_config;
create table gi_config
(
	id varchar(50) not null,
	name varchar(50) not null,
	description varchar(255),
	s varchar(8192),
	i int default 0,
	l bigint default 0,
	d decimal(16,6) default 0
);
create unique index gi_config_index_name on gi_config(name);

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
	email varchar(100),
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
	address varchar(255),
	company varchar(255),
	title varchar(255),
	phone varchar(50),
	photo varchar(255),
	description varchar(1024),
	department varchar(100),
	lastfailtime bigint default 0,
	lastfailip varchar(30),
	failtimes int default 0,
	deleted int default 0,
	role varchar(255),
	updated bigint default 0,
	created bigint
);
create unique index gi_user_indexid on gi_user(id);
create index gi_user_index_name on gi_user(name);
create index gi_user_index_certid on gi_user(certid);
create index gi_user_index_deleted on gi_user(deleted);
create index gi_user_index_locked on gi_user(locked);

alter table gi_user alter id type bigint;

create table gi_userrole
(
	id bigint not null,
	uid bigint,
	rid int,
	created bigint
);
create index gi_userrole_index_uid on gi_userrole(uid);
create unique index gi_userrole_index_uid_rid on gi_userrole(uid, rid);
insert into gi_userrole(uid, rid) values(0, 0);
alter table gi_userrole alter uid type bigint;
alter table gi_userrole alter rid type int;


#drop table if exists gi_role;
create table gi_role
(
	id bigint not null,
	id int,
	name varchar(100),
	memo varchar(255),
	updated bigint default 0
);
create unique index gi_role_indexid on gi_role(id);
insert into gi_role(id, name) values(0, 'admin');

#drop table if exists gi_userrole;
create table gi_userrole
(
	id bigint not null,
	uid bigint,
	rid int,
	created bigint
);
create index gi_userrole_index_uid on gi_userrole(uid);
create unique index gi_userrole_index_uid_rid on gi_userrole(uid, rid);
insert into gi_userrole(uid, rid) values(0, 0);
alter table gi_userrole alter uid type bigint;

#drop table if exists gi_access;
create table gi_access
(
	id varchar(255),
	name varchar(255)
);
create unique index gi_access_index_name on gi_access(name);
create index gi_access_indexid on gi_access(id);
insert into gi_access(name) values('access.admin');

#drop table if exists gi_roleaccess;
create table gi_roleaccess
(
	id varchar(20),
	rid int,
	name varchar(255)
);
create index gi_roleaccess_indexid on gi_roleaccess(id);
create index gi_roleaccess_index_rid on gi_roleaccess(rid);
insert into gi_roleaccess(rid, name) values(0, 'access.admin');

#drop table if exists gi_repo;
create table gi_repo
(
	uid bigint,
	id varchar(20),
	folder varchar(255),
	name varchar(255),
	total bigint,
	pos bigint,
	created bigint,
	flag int,
	tag varchar(20),
	expired bigint,
	memo varchar(1024)
);
create unique index gi_repo_indexid on gi_repo(id);
create index gi_repo_index_uid on gi_repo(uid);
create index gi_repo_index_name on gi_repo(name);
create index gi_repo_index_folder on gi_repo(folder);
create index gi_repo_index_tag on gi_repo(tag);
create index gi_repo_index_expired on gi_repo(expired);
alter table gi_repo alter uid type bigint;

#drop table if exists gi_keypair;
create table gi_keypair
(
	id bigint not null,
	created bigint,
	memo varchar(255),
	length int,
	pubkey varchar(2048),
	prikey varchar(2048)
);
create unique index gi_keypair_index_created on gi_keypair(created);

create table gi_stat
(
	id varchar(20),
	date bigint,
	module varchar(255),
	f0 varchar(255),
	f1 varchar(255),
	f2 varchar(255),
	f3 varchar(255),
	f4 varchar(255),
	uid bigint,
	count decimal(20, 2),
	updated bigint
);
create index gi_stat_indexid on gi_stat(id);
create index gi_stat_index_date on gi_stat(date);
create index gi_stat_index_module on gi_stat(module);
create index gi_stat_index_f0 on gi_stat(f0);
create index gi_stat_index_f1 on gi_stat(f1);
create index gi_stat_index_f2 on gi_stat(f2);
create index gi_stat_index_f3 on gi_stat(f3);
create index gi_stat_index_f4 on gi_stat(f4);
create index gi_stat_index_uid on gi_stat(uid);
create index gi_stat_index_updated on gi_stat(updated);
alter table gi_stat alter uid type bigint;


#drop table if exists gi_menu;
create table gi_menu
(
	id int,
	node varchar(50),
	parent int,
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
	load varchar(255)
);
create index gi_menu_indexid on gi_menu(id);
create index gi_menu_index_parent on gi_menu(parent);
create index gi_menu_index_name on gi_menu(name);
create index gi_menu_index_tag on gi_menu(tag);
create index gi_menu_index_seq on gi_menu(seq);
create index gi_menu_index_node on gi_menu(node);
alter table gi_menu add style varchar(255);
alter table gi_menu add load varchar(255);
