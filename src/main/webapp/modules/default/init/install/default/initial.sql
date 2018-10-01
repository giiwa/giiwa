#drop table if exists gi_config;
create table gi_config
(
	id varchar(128) not null,
	linkid varchar(50),
	s varchar(8192),
	i int default 0,
	l bigint default 0,
	_node varchar(20),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_config_index_id on gi_config(id);

#drop table if exists gi_user;
create table gi_user
(
	id bigint,
	name varchar(50),
	nickname varchar(255),
	title varchar(255),
	password varchar(255),
	md4passwd varchar(128),
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
	created bigint default 0,
	ajaxlogined bigint default 0,
	weblogined bigint default 0,
	_node varchar(20),
	desktop varchar(255)
);
create unique index gi_user_indexid on gi_user(id);
create unique index gi_user_index_name on gi_user(name);
create index gi_user_index_deleted on gi_user(deleted);
create index gi_user_index_locked on gi_user(locked);

#drop table if exists gi_userrole;
create table gi_userrole
(
	uid bigint,
	rid bigint,
	_node varchar(20),
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
	_node varchar(20),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_role_indexid on gi_role(id);
create index gi_role_index_name on gi_role(name);

#drop table if exists gi_code;
create table gi_code
(
	s1 varchar(1024),
	s2 varchar(1024),
	val varchar(4096),
	expired bigint,
	_node varchar(20),
	created bigint default 0,
	updated bigint default 0
);
create unique index gi_code_index_s1_s2 on gi_code(s1, s2);

#drop table if exists gi_access;
create table gi_access
(
	id varchar(255),
	memo varchar(4096),
	_node varchar(20),
	updated bigint default 0,
	created bigint default 0
);
create unique index gi_access_indexid on gi_access(id);

#drop table if exists gi_roleaccess;
create table gi_roleaccess
(
	id varchar(20),
	rid bigint,
	name varchar(255),
	_node varchar(20),
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
	_node varchar(20),
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
	load1 varchar(255),
	show1 varchar(255),
	_node varchar(20),
	created bigint default 0,
	updated bigint default 0
);
create index gi_menu_indexid on gi_menu(id);
create index gi_menu_index_parent on gi_menu(parent);
create index gi_menu_index_name on gi_menu(name);
create index gi_menu_index_tag on gi_menu(tag);
create index gi_menu_index_seq on gi_menu(seq);
create index gi_menu_index_node on gi_menu(node);

#drop table if exists gi_accesslog;
create table gi_accesslog 
(
	uid bigint,
	cost bigint,
	module varchar(128),
	model varchar(128),
	method varchar(20),
	ip varchar(20),
	client varchar(1024),
	id varchar(20),
	url varchar(128),
	sid varchar(50),
	username varchar(50),
	header varchar(2048),
	request varchar(2048),
	response varchar(2048),
	status int,
	_node varchar(20),
	created bigint default 0,
	updated bigint default 0
);
create index gi_accesslog_index_uid on gi_accesslog(uid);
create index gi_accesslog_index_method on gi_accesslog(method);

#drop table if exists gi_jar;
create table gi_jar
(
	id varchar(20),
	module varchar(50),
	name varchar(50),
	reset int,
	node varchar(50),
	_node varchar(20),
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
	ip varchar(128),
	sid varchar(50),
	token varchar(50),
	_node varchar(20),
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
	ip varchar(128),
	type int,
	message varchar(1024),
	trace varchar(8192),
	_node varchar(20),
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
	_node varchar(20),
	created bigint default 0,
	updated bigint default 0
);
create index gi_userlock_index_uid on gi_userlock(uid);

create table gi_httpcookie
(
	id varchar(50),
	name varchar(50),
	value varchar(255),
	domain varchar(255),
	path varchar(255),
	_node varchar(20),
	expired bigint default 0,
	created bigint default 0,
	updated bigint default 0
);

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
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_app_index_appid on gi_app(appid);

create table gi_node
(
	id varchar(50),
	ip varchar(100),
	uptime bigint,
	cores int,
	os varchar(100),
	mem bigint,
	_node varchar(20),
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
	message varchar(2048),
	trace varchar(4098),
	uid bigint,
	ip varchar(50),
	type1 int,
	level int,
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_glog_index_id on gi_glog(id);
create index gi_glog_index_op on gi_glog(op);
create index gi_glog_index_uid on gi_glog(uid);
create index gi_glog_index_type on gi_glog(type1);
create index gi_glog_index_level on gi_glog(level);

create table gi_m_net
(
	id varchar(50),
	node varchar(50),
	name varchar(50),
	inet varchar(50),
	inet6 varchar(50),
	txbytes bigint,
	txpackets bigint,
	txdrop bigint,
	txerr bigint,
	rxbytes bigint,
	rxpackets bigint,
	rxdrop bigint,
	rxerr bigint,
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_m_net_index_id on gi_m_net(id);
create index gi_m_net_index_deviceid on gi_m_net(deviceid);

create table gi_m_net_record
(
	id varchar(50),
	node varchar(50),
	name varchar(50),
	inet varchar(50),
	inet6 varchar(50),
	txbytes bigint,
	txpackets bigint,
	txdrop bigint,
	txerr bigint,
	rxbytes bigint,
	rxpackets bigint,
	rxdrop bigint,
	rxerr bigint,
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_m_net_record_index_id on gi_m_net_record(id);
create index gi_m_net_record_index_deviceid on gi_m_net_record(deviceid);

create table gi_m_mem
(
	id varchar(50),
	node varchar(50),
	total bigint,
	used bigint,
	usage int,
	free bigint,
	swaptotal bigint,
	swapfree bigint,
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_m_mem_index_id on gi_m_mem(id);
create index gi_m_mem_index_deviceid on gi_m_mem(deviceid);

create table gi_m_disk
(
	id varchar(50),
	node varchar(50),
	disk varchar(128),
	path varchar(128),
	total bigint,
	used bigint,
	free bigint,
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_m_disk_index_id on gi_m_disk(id);
create index gi_m_disk_index_deviceid on gi_m_disk(deviceid);

create table gi_m_cpu
(
	id varchar(50),
	node varchar(50),
	name varchar(128),
	sys decimal(5,2),
	user1 decimal(5,2),
	usage decimal(5,2),
	wait decimal(5,2),
	nice decimal(5,2),
	idle decimal(5,2),
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_m_cpu_index_id on gi_m_cpu(id);
create index gi_m_cpu_index_deviceid on gi_m_cpu(deviceid);

create table gi_m_mem_record
(
	id varchar(50),
	node varchar(50),
	total bigint,
	used bigint,
	usage int,
	free bigint,
	swaptotal bigint,
	swapfree bigint,
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_m_mem_record_index_id on gi_m_mem_record(id);
create index gi_m_mem_record_index_deviceid on gi_m_mem_record(deviceid);

create table gi_m_disk_record
(
	id varchar(50),
	node varchar(50),
	disk varchar(128),
	path varchar(128),
	total bigint,
	used bigint,
	free bigint,
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_m_disk_record_index_id on gi_m_disk_record(id);
create index gi_m_disk_record_index_deviceid on gi_m_disk_record(deviceid);

create table gi_m_cpu_record
(
	id varchar(50),
	node varchar(50),
	name varchar(128),
	sys decimal(5,2),
	user1 decimal(5,2),
	usage decimal(5,2),
	wait decimal(5,2),
	nice decimal(5,2),
	idle decimal(5,2),
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_m_cpu_record_index_id on gi_m_cpu_record(id);
create index gi_m_cpu_record_index_deviceid on gi_m_cpu_record(deviceid);

create table gi_license
(
	id varchar(50),
	code varchar(100),
	content varchar(2048),
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_license_index_id on gi_license(id);

create table gi_stat
(
	id bigint,
	module varchar(256),
	date varchar(20),
	size varchar(20),
	n0 bigint,
	n1 bigint,
	n2 bigint,
	n3 bigint,
	n4 bigint,
	n5 bigint,
	n6 bigint,
	n7 bigint,
	n8 bigint,
	n9 bigint,
	n10 bigint,
	n11 bigint,
	n12 bigint,
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_stat_index_id on gi_stat(id);
create index gi_stat_index_module on gi_stat(module);
create index gi_stat_index_date on gi_stat(date);

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
	content varchar(4096),
	_node varchar(20),
	updated bigint,
	created bigint
);
create index gi_message_index_id on gi_message(id);
create index gi_message_index_touid on gi_message(touid);
