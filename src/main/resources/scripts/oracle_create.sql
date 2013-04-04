-- global semaphore tables
drop sequence OBJECT_LOCK_ID_SEQ;
create sequence OBJECT_LOCK_ID_SEQ start with 10000 increment by 1;

drop table object_lock;
create table object_lock (
	lock_id	number primary key,
	lock_key	varchar2(128),
	lock_count	integer,
	create_date	timestamp, 
	last_modified_date	timestamp
 );
 
create unique index ol_key_NDX on object_lock (lock_key);

-- flow session tables

drop sequence FLOW_ID_SEQ;
create sequence FLOW_ID_SEQ start with 10000 increment by 1;

drop table flow_session;
create table flow_session (
	flow_id		number primary key, 
	flow_key	varchar2(512),
	creation_date	timestamp,
	end_date	timestamp, 
	flow_status	varchar2(512),
	target		varchar2(512),
	flow_type	varchar2(256), 
	parent_id	number references flow_session(flow_id),
	current_action	varchar2(128), 
	next_action	varchar2(128), 
	flow_state	varchar2(64), 
	flow_result	varchar2(64), 
	last_modified	timestamp, 
	run_by		varchar2(256),
	requester	varchar2(512)
);

create index SESCOP_PRNT_IDX on FLOW_SESSION (PARENT_ID);
create index FS_ACTIONSTATUS on FLOW_SESSION (FLOW_STATE);
create index FS_CD_IDX on FLOW_SESSION (CREATION_DATE);
create index FS_EI on FLOW_SESSION (END_DATE, FLOW_ID);
create index FS_ENDDATE on FLOW_SESSION (END_DATE);
create index FS_KEY on FLOW_SESSION (TARGET);
create index FS_TYPE_IDX on FLOW_SESSION (FLOW_TYPE);
create index FS_FKEY_IDX on FLOW_SESSION (FLOW_KEY);


drop sequence FLOW_META_ID_SEQ;
create sequence FLOW_META_ID_SEQ start with 10000 increment by 1;

drop sequence flow_session_meta;
create table flow_session_meta (
	flow_meta_id	number primary key,
	flow_id		number references flow_session(flow_id),
	name		varchar2(512),
	str_value	varchar2(2000),
	blob_value	blob
);

CREATE INDEX FSM_SSNID_IDX ON FLOW_SESSION_META (FLOW_ID);

drop sequence FLOW_STEP_ID_SEQ;
create sequence FLOW_STEP_ID_SEQ start with 10000 increment by 1;

drop table flow_step_log
create table flow_step_log (
	flow_step_id	number primary key,
	flow_id		number references flow_session(flow_id),
	step_name	varchar2(512),
	creation_time	timestamp,
	result		varchar2(512),
	details		varchar2(2000)
);

