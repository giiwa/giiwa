create table gi_config
(
	id String,
	linkid Nullable(String),
	s Nullable(String),
	i int default 0,
	l bigint default 0,
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_user
(
	id bigint,
	name String,
	nickname Nullable(String),
	title Nullable(String),
	password Nullable(String),
	locked int default 0,
	lockexpired bigint default 0,
	logintimes int default 0,
	lastlogintime bigint,
	lastattemptime bigint,
	lastloginip Nullable(String),
	expired bigint,
	sid Nullable(String),
	ip Nullable(String),
	email Nullable(String),
	phone Nullable(String),
	photo Nullable(String),
	lastfailtime bigint default 0,
	lastfailip Nullable(String),
	failtimes int default 0,
	deleted int default 0,
	updated bigint default 0,
	created bigint default 0,
	ajaxlogined bigint default 0,
	weblogined bigint default 0,
	_node Nullable(String),
	desktop Nullable(String)
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_userrole
(
	"uid" bigint,
	rid bigint,
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_role
(
	id bigint not null,
	name Nullable(String),
	memo Nullable(String),
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_code
(
	s1 Nullable(String),
	s2 Nullable(String),
	val Nullable(String),
	expired bigint,
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;
alter table gi_code add index gi_code_index_s1_s2(s1, s2);

create table gi_access
(
	id String,
	memo Nullable(String),
	_node Nullable(String),
	updated bigint default 0,
	created bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_roleaccess
(
	id String,
	rid bigint,
	name Nullable(String),
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_repo
(
	"uid" bigint,
	id String,
	folder Nullable(String),
	name Nullable(String),
	total bigint,
	pos bigint,
	flag int,
	tag Nullable(String),
	expired bigint,
	memo Nullable(String),
	ip Nullable(String),
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_menu
(
	id bigint,
	node Nullable(String),
	parent bigint,
	name Nullable(String),
	url Nullable(String),
	classes Nullable(String),
	click Nullable(String),
	content Nullable(String),
	tag Nullable(String),
	"access" Nullable(String),
	childs int default 0,
	seq int default 1000,
	tip Nullable(String),
	style Nullable(String),
	load1 Nullable(String),
	show1 Nullable(String),
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_accesslog 
(
	"uid" bigint,
	cost bigint,
	module Nullable(String),
	model Nullable(String),
	method Nullable(String),
	ip Nullable(String),
	client Nullable(String),
	id Nullable(String),
	url Nullable(String),
	sid Nullable(String),
	header Nullable(String),
	request Nullable(String),
	response Nullable(String),
	username Nullable(String),
	status int,
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_jar
(
	id String,
	module Nullable(String),
	name Nullable(String),
	reset int,
	node Nullable(String),
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_authtoken
(
	id String,
	"uid" bigint,
	expired bigint,
	ip Nullable(String),
	sid Nullable(String),
	token Nullable(String),
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_oplog
(
	id String,
	node Nullable(String),
	model Nullable(String),
	op Nullable(String),
	"uid" bigint,
	ip Nullable(String),
	type int,
	message Nullable(String),
	trace Nullable(String),
	_node Nullable(String),
	logger Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_footprint
(
	id String,
	_table Nullable(String),
	dataid Nullable(String),
	field Nullable(String),
	"uid" bigint,
	data Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_userlock
(
	id String,
	"uid" bigint,
	host Nullable(String),
	useragent Nullable(String),
	sid Nullable(String),
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_httpcookie
(
	id String,
	name Nullable(String),
	value Nullable(String),
	domain Nullable(String),
	path Nullable(String),
	expired bigint default 0,
	_node Nullable(String),
	created bigint default 0,
	updated bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_app
(
	id bigint default 0,
	appid Nullable(String),
	memo Nullable(String),
	secret Nullable(String),
	ip Nullable(String),
	lastime bigint default 0,
	expired bigint default 0,
	role bigint default 0,
	_node Nullable(String),
	updated bigint default 0,
	created bigint default 0
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_node
(
	id String,
	ip Nullable(String),
	name Nullable(String),
	uptime bigint,
	cores int,
	os Nullable(String),
	label Nullable(String),
	mem bigint,
	_usage int,
	globaltasks int,
	localthreads int,
	localpending int,
	localrunning int,
	giiwa Nullable(String),
	pid Nullable(String),
	url Nullable(String),
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_glog
(
	id String,
	node Nullable(String),
	model Nullable(String),
	iid Nullable(String),
	op Nullable(String),
	message Nullable(String),
	trace Nullable(String),
	uid bigint,
	ip Nullable(String),
	type1 int,
	level int,
	logger Nullable(String),
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_m_net
(
	id String,
	node Nullable(String),
	name Nullable(String),
	inet Nullable(String),
	inet6 Nullable(String),
	txbytes bigint,
	txpackets bigint,
	txdrop bigint,
	txerr bigint,
	rxbytes bigint,
	rxpackets bigint,
	rxdrop bigint,
	rxerr bigint,
	snapshot Nullable(String),
	_type Nullable(String),
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_m_net_record
(
	id String,
	node Nullable(String),
	name Nullable(String),
	inet Nullable(String),
	inet6 Nullable(String),
	txbytes bigint,
	txpackets bigint,
	txdrop bigint,
	txerr bigint,
	rxbytes bigint,
	rxpackets bigint,
	rxdrop bigint,
	rxerr bigint,
	snapshot Nullable(String),
	_type Nullable(String),
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_m_mem
(
	id String,
	node Nullable(String),
	total bigint,
	used bigint,
	usage int,
	free bigint,
	swaptotal bigint,
	swapfree bigint,
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_m_disk
(
	id String,
	node Nullable(String),
	disk Nullable(String),
	path Nullable(String),
	total bigint,
	used bigint,
	free bigint,
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_m_cpu
(
	id String,
	node Nullable(String),
	name Nullable(String),
	cores int,
	sys Float32,
	user1 Float32,
	usage Float32,
	wait Float32,
	nice Float32,
	idle Float32,
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_m_mem_record
(
	id String,
	node Nullable(String),
	total bigint,
	used bigint,
	usage int,
	free bigint,
	swaptotal bigint,
	swapfree bigint,
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_m_disk_record
(
	id String,
	node Nullable(String),
	disk Nullable(String),
	path Nullable(String),
	total bigint,
	used bigint,
	free bigint,
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_m_cpu_record
(
	id String,
	node Nullable(String),
	name Nullable(String),
	sys Float32,
	user1 Float32,
	usage Float32,
	wait Float32,
	nice Float32,
	idle Float32,
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_license
(
	id String,
	code Nullable(String),
	content Nullable(String),
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_stat
(
	id bigint,
	module Nullable(String),
	date Nullable(String),
	size Nullable(String),
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
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;

create table gi_message
(
	id bigint,
	refer bigint,
	tag Nullable(String),
	touid bigint,
	fromuid bigint,
	priority int,
	flag int,
	star int,
	title Nullable(String),
	content Nullable(String),
	_node Nullable(String),
	updated bigint,
	created bigint
)engine = MergeTree() order by tuple() SETTINGS index_granularity = 8192;
