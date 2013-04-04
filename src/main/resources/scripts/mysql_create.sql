-- lock tables

create table if not exists object_lock (
	lock_id	bigint auto_increment primary key,
	lock_key	varchar(128),
	lock_count	integer(32),
	create_date	timestamp, 
	last_modified_date	timestamp
 );
 
create unique index ol_key_NDX on object_lock (lock_key);

-- flow session tables

create table if not exists flow_session (
	flow_id		bigint auto_increment primary key, 
	flow_key	varchar(512),
	creation_date	timestamp,
	end_date	timestamp, 
	flow_status	varchar(512),
	target		varchar(512),
	flow_type	varchar(256), 
	parent_id	bigint references flow_session(flow_id),
	current_action	varchar(128), 
	next_action	varchar(128), 
	flow_state	varchar(64), 
	flow_result	varchar(64), 
	last_modified	timestamp, 
	run_by		varchar(256),
	requester	varchar(512)
);

create index SESCOP_PRNT_IDX on FLOW_SESSION (PARENT_ID);
create index FS_ACTIONSTATUS on FLOW_SESSION (FLOW_STATE);
create index FS_CD_IDX on FLOW_SESSION (CREATION_DATE);
create index FS_EI on FLOW_SESSION (END_DATE, FLOW_ID);
create index FS_ENDDATE on FLOW_SESSION (END_DATE);
create index FS_KEY on FLOW_SESSION (TARGET);
create index FS_TYPE_IDX on FLOW_SESSION (FLOW_TYPE);
create index FS_FKEY_IDX on FLOW_SESSION (FLOW_KEY);


create table if not exists flow_session_meta (
	flow_meta_id	bigint auto_increment primary key,
	flow_id		bigint references flow_session(flow_id),
	name		varchar(512),
	str_value	varchar(2000),
	blob_value	blob
);

CREATE INDEX FSM_SSNID_IDX ON FLOW_SESSION_META (FLOW_ID);

create table if not exists flow_step_log (
	flow_step_id	bigint auto_increment primary key,
	flow_id		bigint references flow_session(flow_id),
	step_name	varchar(512),
	creation_time	timestamp,
	result		varchar(512),
	details		varchar(2000)
);


