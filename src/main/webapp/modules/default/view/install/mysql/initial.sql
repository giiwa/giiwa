#drop table if exists tblconfig;
create table tblconfig
(
	name varchar(50) not null,
	description varchar(255),
	s varchar(8192),
	i int default 0,
	l bigint default 0,
	d decimal(16,6) default 0
);
create unique index tblconfig_index_name on tblconfig(name);
insert into tblconfig(name, s) values('db.version', '1.1');

#drop table if exists dual;
create table dual
(
	x varchar(1)
);
insert into dual values('x');

#drop table if exists tbluser;
create table tbluser
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
	created bigint,
	rank int default 0,
	workspace varchar(200),
	address varchar(255),
	company varchar(255),
	title varchar(255),
	phone varchar(50),
	photo varchar(255),
	total bigint default -1,
	free bigint default 0,
	spi varchar(50),
	description varchar(1024),
	special varchar(1024),
	department varchar(100),
	remote int default 0,
	certid varchar(20),
	lastfailtime bigint default 0,
	lastfailip varchar(30),
	failtimes int default 0,
	deleted int default 0,
	city varchar(255),
	district varchar(255),
	role varchar(255),
	updated bigint default 0
);
create unique index tbluser_index_id on tbluser(id);
create index tbluser_index_name on tbluser(name);
create index tbluser_index_certid on tbluser(certid);
create index tbluser_index_deleted on tbluser(deleted);
create index tbluser_index_locked on tbluser(locked);

insert into tbluser(id, name, certid, nickname, password, created) values(0, 'admin', '123456', 'admin', 'albqdj2cd1aun', extract(epoch from current_timestamp) * 1000::bigint);
alter table tbluser alter id type bigint;

create table tbluserrole
(
	uid bigint,
	rid int,
	created bigint
);
create index tbluserrole_index_uid on tbluserrole(uid);
create unique index tbluserrole_index_uid_rid on tbluserrole(uid, rid);
insert into tbluserrole(uid, rid) values(0, 0);
alter table tbluserrole alter uid type bigint;
alter table tbluserrole alter rid type int;


#drop table if exists tblrole;
create table tblrole
(
	id int,
	name varchar(100),
	memo varchar(255),
	updated bigint default 0
);
create unique index tblrole_index_id on tblrole(id);
insert into tblrole(id, name) values(0, 'admin');

#drop table if exists tbluserrole;
create table tbluserrole
(
	uid bigint,
	rid int,
	created bigint
);
create index tbluserrole_index_uid on tbluserrole(uid);
create unique index tbluserrole_index_uid_rid on tbluserrole(uid, rid);
insert into tbluserrole(uid, rid) values(0, 0);
alter table tbluserrole alter uid type bigint;

#drop table if exists tblaccess;
create table tblaccess
(
	id varchar(255),
	name varchar(255)
);
create unique index tblaccess_index_name on tblaccess(name);
create index tblaccess_index_id on tblaccess(id);
insert into tblaccess(name) values('access.admin');

#drop table if exists tblroleaccess;
create table tblroleaccess
(
	id varchar(20),
	rid int,
	name varchar(255)
);
create index tblroleaccess_index_id on tblroleaccess(id);
create index tblroleaccess_index_rid on tblroleaccess(rid);
insert into tblroleaccess(rid, name) values(0, 'access.admin');

#drop table if exists tblconn;
create table tblconn
(
  	clientid varchar(20),
  	phone varchar(20),
  	alias varchar(100),
  	password varchar(128),
  	photo varchar(100),
  	locked int default 0,
  	pubkey varchar(2048),
  	ip varchar(50),
  	capability int default 0,
  	created bigint,
  	login int,
  	logined bigint,
  	updated bigint,
  	address varchar(255),
  	uid bigint default 0,
  	sent bigint default 0,
  	received bigint default 0
);
create unique index tblconn_index_clientid on tblconn(clientid);
create index tblconn_index_phone on tblconn(phone);
create index tblconn_index_alias on tblconn(alias);
create index tblconn_index_uid on tblconn(uid);
create index tblconn_index_password on tblconn(password);
alter table tblconn alter uid type bigint;

#drop table if exists tblurlmapping;
create table tblurlmapping
(
	url varchar(100),
	dest varchar(100),
	seq int default 999
);
create unique index tblurlmapping_index_dest on tblurlmapping(dest);
create index tblurlmapping_index_seq on tblurlmapping(seq);

#drop table if exists tblrepo;
create table tblrepo
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
create unique index tblrepo_index_id on tblrepo(id);
create index tblrepo_index_uid on tblrepo(uid);
create index tblrepo_index_name on tblrepo(name);
create index tblrepo_index_folder on tblrepo(folder);
create index tblrepo_index_tag on tblrepo(tag);
create index tblrepo_index_expired on tblrepo(expired);
alter table tblrepo alter uid type bigint;

#drop table if exists tblpendindex;
create table tblpendindex
(
	clazz varchar(255),
	id varchar(255),
	created bigint
);
create index tblpendindex_index_clazz on tblpendindex(clazz);
create index tblpendindex_index_id on tblpendindex(id);
create index tblpendindex_index_created on tblpendindex(created);

#drop table if exists tblkeypair;
create table tblkeypair
(
	created bigint,
	memo varchar(255),
	length int,
	pubkey varchar(2048),
	prikey varchar(2048)
);
create unique index tblkeypair_index_created on tblkeypair(created);

create table tblstat
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
create index tblstat_index_id on tblstat(id);
create index tblstat_index_date on tblstat(date);
create index tblstat_index_module on tblstat(module);
create index tblstat_index_f0 on tblstat(f0);
create index tblstat_index_f1 on tblstat(f1);
create index tblstat_index_f2 on tblstat(f2);
create index tblstat_index_f3 on tblstat(f3);
create index tblstat_index_f4 on tblstat(f4);
create index tblstat_index_uid on tblstat(uid);
create index tblstat_index_updated on tblstat(updated);
alter table tblstat alter uid type bigint;


#drop table if exists tblmenu;
create table tblmenu
(
	id serial,
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
create index tblmenu_index_id on tblmenu(id);
create index tblmenu_index_parent on tblmenu(parent);
create index tblmenu_index_name on tblmenu(name);
create index tblmenu_index_tag on tblmenu(tag);
create index tblmenu_index_seq on tblmenu(seq);
create index tblmenu_index_node on tblmenu(node);
alter table tblmenu add style varchar(255);
alter table tblmenu add load varchar(255);

create table tblload
(
	name varchar(255),
	node varchar(255),
	count int default 0,
	updated bigint default 0
);
create unique index tblload_index_name_node on tblload(name, node);
create index tblload_index_name on tblload(name);
create index tblload_index_count on tblload(count);
create index tblload_index_updated on tblload(updated);
