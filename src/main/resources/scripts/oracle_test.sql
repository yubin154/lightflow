-- tables and sequences for unit tests

drop sequence seq_test_dbframework;
drop table test_dbframework;

create sequence seq_test_dbframework start with 1 nocache;
create table test_dbframework (
	ID			NUMBER PRIMARY KEY,
	Col_VC		VARCHAR(256),
	Col_Long	NUMBER,
	Col_Float	FLOAT,
	Col_Date	DATE
);

create table test_relation (
	ID			NUMBER PRIMARY KEY,
	ParentID	NUMBER REFERENCES test_dbframework (ID),
	ChildID		NUMBER REFERENCES test_dbframework (ID),
	LastModified	DATE
);
	

