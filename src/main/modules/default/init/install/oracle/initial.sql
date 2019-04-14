#drop table if exists gi_config;
create table gi_config
(
	id varchar2(128) not null,
	linkid varchar2(50),
	s varchar2(4000),
	i NUMBER(10,0) default 0,
	l NUMBER(20,0) default 0,
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create unique index gi_config_index_id on gi_config(id);

#drop table if exists gi_user;
create table gi_user
(
	id NUMBER(20,0),
	name varchar2(50),
	nickname varchar2(255),
	title varchar2(255),
	password varchar2(255),
	md4passwd varchar2(128),
	locked NUMBER(10,0) default 0,
	lockexpired NUMBER(20,0) default 0,
	logintimes NUMBER(10,0) default 0,
	lastlogintime NUMBER(20,0),
	lastattemptime NUMBER(20,0),
	lastloginip varchar2(20),
	expired NUMBER(20,0),
	sid varchar2(50),
	ip varchar2(20),
	email varchar2(100),
	phone varchar2(50),
	photo varchar2(255),
	lastfailtime NUMBER(20,0) default 0,
	lastfailip varchar2(30),
	failtimes NUMBER(10,0) default 0,
	deleted NUMBER(10,0) default 0,
	updated NUMBER(20,0) default 0,
	created NUMBER(20,0) default 0,
	ajaxlogined NUMBER(20,0) default 0,
	weblogined NUMBER(20,0) default 0,
	_node varchar2(40),
	desktop varchar2(255)
);
create unique index gi_user_indexid on gi_user(id);
create unique index gi_user_index_name on gi_user(name);
create index gi_user_index_deleted on gi_user(deleted);
create index gi_user_index_locked on gi_user(locked);

#drop table if exists gi_userrole;
create table gi_userrole
(
	"uid" NUMBER(20,0),
	rid NUMBER(20,0),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create index gi_userrole_index_uid on gi_userrole("uid");
create unique index gi_userrole_index_uid_rid on gi_userrole("uid", rid);

#drop table if exists gi_role;
create table gi_role
(
	id NUMBER(20,0) not null,
	name varchar2(100),
	memo varchar2(255),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create unique index gi_role_indexid on gi_role(id);
create index gi_role_index_name on gi_role(name);

#drop table if exists gi_code;
create table gi_code
(
	s1 varchar2(1024),
	s2 varchar2(1024),
	val varchar2(4000),
	expired NUMBER(20,0),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create unique index gi_code_index_s1_s2 on gi_code(s1, s2);

#drop table if exists gi_access;
create table gi_access
(
	id varchar2(255),
	memo varchar2(4096),
	_node varchar2(40),
	updated NUMBER(20,0) default 0,
	created NUMBER(20,0) default 0
);
create unique index gi_access_indexid on gi_access(id);

#drop table if exists gi_roleaccess;
create table gi_roleaccess
(
	id varchar2(20),
	rid NUMBER(20,0),
	name varchar2(255),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create index gi_roleaccess_indexid on gi_roleaccess(id);
create index gi_roleaccess_index_rid on gi_roleaccess(rid);

#drop table if exists gi_repo;
create table gi_repo
(
	"uid" NUMBER(20,0),
	id varchar2(20),
	folder varchar2(255),
	name varchar2(255),
	total NUMBER(20,0),
	pos NUMBER(20,0),
	flag NUMBER(10,0),
	tag varchar2(20),
	expired NUMBER(20,0),
	memo varchar2(1024),
	ip varchar2(20),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create unique index gi_repo_indexid on gi_repo(id);
create index gi_repo_index_uid on gi_repo("uid");
create index gi_repo_index_name on gi_repo(name);
create index gi_repo_index_folder on gi_repo(folder);
create index gi_repo_index_tag on gi_repo(tag);
create index gi_repo_index_expired on gi_repo(expired);

#drop table if exists gi_menu;
create table gi_menu
(
	id NUMBER(20,0),
	node varchar2(50),
	parent NUMBER(20,0),
	name varchar2(50),
	url varchar2(255),
	classes varchar2(100),
	click varchar2(255),
	content varchar2(4000),
	tag varchar2(20),
	"access" varchar2(1024),
	childs NUMBER(10,0) default 0,
	seq NUMBER(10,0) default 1000,
	tip varchar2(255),
	style varchar2(255),
	load1 varchar2(255),
	show1 varchar2(255),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
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
	"uid" NUMBER(20,0),
	cost NUMBER(20,0),
	module varchar2(128),
	model varchar2(128),
	method varchar2(20),
	ip varchar2(20),
	client varchar2(1024),
	id varchar2(20),
	url varchar2(128),
	sid varchar2(50),
	header varchar2(2048),
	request varchar2(2048),
	response varchar2(2048),
	username varchar2(50),
	status NUMBER(10,0),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create index gi_accesslog_index_uid on gi_accesslog("uid");
create index gi_accesslog_index_method on gi_accesslog(method);

#drop table if exists gi_jar;
create table gi_jar
(
	id varchar2(20),
	module varchar2(50),
	name varchar2(50),
	reset NUMBER(10,0),
	node varchar2(50),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create index gi_jar_index_module on gi_jar(module);
create index gi_jar_index_name on gi_jar(name);
create index gi_jar_index_node on gi_jar(node);

#drop table if exists gi_authtoken;
create table gi_authtoken
(
	id varchar2(20),
	"uid" NUMBER(20,0),
	expired NUMBER(20,0),
	ip varchar2(128),
	sid varchar2(50),
	token varchar2(50),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create index gi_authtoken_index_id on gi_authtoken(id);
create index gi_authtoken_index_uid on gi_authtoken("uid");
create index gi_authtoken_index_sid on gi_authtoken(sid);
create index gi_authtoken_index_token on gi_authtoken(token);

#drop table if exists gi_oplog;
create table gi_oplog
(
	id varchar2(20),
	node varchar2(50),
	model varchar2(50),
	op varchar2(50),
	"uid" NUMBER(20,0),
	ip varchar2(128),
	type NUMBER(10,0),
	message varchar2(1024),
	trace varchar2(4000),
	_node varchar2(40),
	logger varchar(255),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create index gi_oplog_index_created on gi_oplog(created);
create index gi_oplog_index_type on gi_oplog(type);
create index gi_oplog_index_uid on gi_oplog("uid");
create index gi_oplog_index_node on gi_oplog(node);
create index gi_oplog_index_model on gi_oplog(model);
create index gi_oplog_index_op on gi_oplog(op);

#drop table if exists gi_footprint;
create table gi_footprint
(
	id varchar2(50),
	_table varchar2(100),
	dataid varchar2(100),
	field varchar2(100),
	"uid" NUMBER(20,0),
	data varchar2(200),
	updated NUMBER(20,0),
	created NUMBER(20,0)
);
create index gi_footprint_index_id on gi_footprint(id);

#drop table if exists gi_userlock;
create table gi_userlock
(
	id varcahr2(50),
	"uid" NUMBER(20,0),
	host varchar2(20),
	useragent varchar2(255),
	sid varchar2(50),
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);
create index gi_userlock_index_uid on gi_userlock("uid");

create table gi_httpcookie
(
	id varchar2(50),
	name varchar2(50),
	value varchar2(255),
	domain varchar2(255),
	path varchar2(255),
	expired NUMBER(20,0) default 0,
	_node varchar2(40),
	created NUMBER(20,0) default 0,
	updated NUMBER(20,0) default 0
);

create table gi_app
(
	id NUMBER(20,0) default 0,
	appid varchar2(50),
	memo varchar2(255),
	secret varchar2(128),
	ip varchar2(25),
	lastime NUMBER(20,0) default 0,
	expired NUMBER(20,0) default 0,
	role NUMBER(20,0) default 0,
	_node varchar2(40),
	updated NUMBER(20,0) default 0,
	created NUMBER(20,0) default 0
);
create index gi_app_index_appid on gi_app(appid);

create table gi_node
(
	id varchar2(50),
	ip varchar2(100),
	name varchar2(20),
	uptime number(20,0),
	cores number(10,0),
	os varchar2(100),
	label varchar2(100),
	mem number(20,0),
	usage number(10,0),
	globaltasks number(10,0),
	localthreads number(10,0),
	localpending number(10,0),
	localrunning number(10,0),
	giiwa varchar2(100),
	pid varchar2(10),
	url varchar2(255),
	_node varchar2(40),
	updated number(20,0),
	created number(20,0)
);
create index gi_node_index_id on gi_node(id);

create table gi_glog
(
	id varchar2(50),
	node varchar2(100),
	model varchar2(100),
	op varchar2(100),
	message varchar2(2048),
	trace varchar2(9186),
	uid bigint,
	ip varchar2(50),
	type1 number(10,0),
	level number(10,0),
	logger varchar2(255),
	_node varchar2(40),
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
	id varchar2(50),
	node varchar2(50),
	name varchar2(50),
	inet varchar2(50),
	inet6 varchar2(50),
	txbytes bigint,
	txpackets bigint,
	txdrop bigint,
	txerr bigint,
	rxbytes bigint,
	rxpackets bigint,
	rxdrop bigint,
	rxerr bigint,
	snapshot varchar2(1024),
	_type varchar2(10),
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_m_net_index_id on gi_m_net(id);
create index gi_m_net_index_deviceid on gi_m_net(deviceid);

create table gi_m_net_record
(
	id varchar2(50),
	node varchar2(50),
	name varchar2(50),
	inet varchar2(50),
	inet6 varchar2(50),
	txbytes bigint,
	txpackets bigint,
	txdrop bigint,
	txerr bigint,
	rxbytes bigint,
	rxpackets bigint,
	rxdrop bigint,
	rxerr bigint,
	snapshot varchar2(1024),
	_type varchar2(10),
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_m_net_record_index_id on gi_m_net_record(id);
create index gi_m_net_record_index_deviceid on gi_m_net_record(deviceid);

create table gi_m_mem
(
	id varchar2(50),
	node varchar2(50),
	total bigint,
	used bigint,
	usage int,
	free bigint,
	swaptotal bigint,
	swapfree bigint,
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_m_mem_index_id on gi_m_mem(id);
create index gi_m_mem_index_deviceid on gi_m_mem(deviceid);

create table gi_m_disk
(
	id varchar2(50),
	node varchar2(50),
	disk varchar2(128),
	path varchar2(128),
	total bigint,
	used bigint,
	free bigint,
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_m_disk_index_id on gi_m_disk(id);
create index gi_m_disk_index_deviceid on gi_m_disk(deviceid);

create table gi_m_cpu
(
	id varchar2(50),
	node varchar2(50),
	name varchar2(128),
	cores number(10,0),
	sys decimal(5,2),
	user1 decimal(5,2),
	usage decimal(5,2),
	wait decimal(5,2),
	nice decimal(5,2),
	idle decimal(5,2),
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_m_cpu_index_id on gi_m_cpu(id);
create index gi_m_cpu_index_deviceid on gi_m_cpu(deviceid);

create table gi_m_mem_record
(
	id varchar2(50),
	node varchar2(50),
	total bigint,
	used bigint,
	usage number(10,0),
	free bigint,
	swaptotal bigint,
	swapfree bigint,
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_m_mem_record_index_id on gi_m_mem_record(id);
create index gi_m_mem_record_index_deviceid on gi_m_mem_record(deviceid);

create table gi_m_disk_record
(
	id varchar2(50),
	node varchar2(50),
	disk varchar2(128),
	path varchar2(128),
	total bigint,
	used bigint,
	free bigint,
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_m_disk_record_index_id on gi_m_disk_record(id);
create index gi_m_disk_record_index_deviceid on gi_m_disk_record(deviceid);

create table gi_m_cpu_record
(
	id varchar2(50),
	node varchar2(50),
	name varchar2(128),
	sys decimal(5,2),
	user1 decimal(5,2),
	usage decimal(5,2),
	wait decimal(5,2),
	nice decimal(5,2),
	idle decimal(5,2),
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_m_cpu_record_index_id on gi_m_cpu_record(id);
create index gi_m_cpu_record_index_deviceid on gi_m_cpu_record(deviceid);

create table gi_license
(
	id varchar2(50),
	code varchar2(100),
	content varchar2(2048),
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_license_index_id on gi_license(id);

create table gi_stat
(
	id bigint,
	module varchar2(256),
	date varchar2(20),
	size varchar2(20),
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
	_node varchar2(40),
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
	tag varchar2(20),
	touid bigint,
	fromuid bigint,
	priority number(10,0),
	flag number(10,0),
	star number(10,0),
	title varchar2(255),
	content varchar2(4096),
	_node varchar2(40),
	updated bigint,
	created bigint
);
create index gi_message_index_id on gi_message(id);
create index gi_message_index_touid on gi_message(touid);
