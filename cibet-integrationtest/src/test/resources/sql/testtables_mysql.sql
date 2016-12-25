--DROP TABLE CIB_TCOMPLEXENTITY_EAGER;
--DROP TABLE CIB_TCOMPLEXENTITY_LAZY;
--DROP TABLE CIB_COMPLEXTESTENTITY_CIB_TESTENTITY;
--DROP TABLE CIB_COMPLEXTESTENTITY;
--DROP TABLE cib_syntetic1entity;
--DROP TABLE cib_syntetic2entity;
--DROP TABLE TPSENTITY;
--DROP TABLE CIB_JMENTITY;
--DROP TABLE CIB_TCOMPLEXENTITY2_EAGER;
--DROP TABLE CIB_TCOMPLEXENTITY2_LAZY;
--DROP TABLE CIB_COMPLEXTESTENTITY2;
--DROP TABLE CIB_TESTENTITY; 

-- ----------------------------------------------
-- TABLES FOR TESTING
-- ----------------------------------------------
--#

CREATE TABLE cib_testentity (
   ID BIGINT NOT NULL AUTO_INCREMENT, 
   NAMEVALUE VARCHAR(255), 
   COUNTER INT NOT NULL, 
   USERID VARCHAR(255), 
   OWNER VARCHAR(255)
   XCALDATE DATE,
   XCALTIMESTAMP DATETIME,
   XDATE DATE,
   XTIME TIME,
   XTIMESTAMP DATETIME
   PRIMARY KEY (id)
);


CREATE TABLE cib_complextestentity (
        id bigint NOT NULL AUTO_INCREMENT,
        userid VARCHAR(255),
        owner VARCHAR(255),
        compValue INT NOT NULL,
        ten_id bigint,
        version INT NOT NULL,
        PRIMARY KEY (id),
        CONSTRAINT FK58B2DAA2A6650A7A FOREIGN KEY (ten_id) REFERENCES cib_testentity (id),
        INDEX FK58B2DAA2A6650A7A (ten_id)
);
    
CREATE TABLE CIB_JMENTITY (
	ID BIGINT NOT NULL AUTO_INCREMENT, 
	NAMEVALUE VARCHAR(255), 
	COUNTER INT NOT NULL, 
	USERID VARCHAR(255), 
	OWNER VARCHAR(255)
);

CREATE TABLE
    cib_syntetic1entity
    (
        id bigint NOT NULL AUTO_INCREMENT,
        intArray tinyblob,
        version DATETIME,
        PRIMARY KEY (id)
    );    

CREATE TABLE
    cib_syntetic2entity
    (
        id VARCHAR(255) NOT NULL,
        PRIMARY KEY (id)
    );    
    
CREATE TABLE
    cib_tcomplexentity_eager
    (
        id bigint NOT NULL,
        eager_id bigint NOT NULL,
        PRIMARY KEY (id, eager_id),
        CONSTRAINT FK4144B93B974B037F FOREIGN KEY (eager_id) REFERENCES cib_testentity (id) ,
        CONSTRAINT FK4144B93BC7A98674 FOREIGN KEY (id) REFERENCES cib_complextestentity (id),
        CONSTRAINT eager_id UNIQUE (eager_id),
        INDEX FK4144B93BC7A98674 (id),
        INDEX FK4144B93B974B037F (eager_id)
    );
    
CREATE TABLE
    cib_tcomplexentity_lazy
    (
        id bigint NOT NULL,
        lazy_id bigint NOT NULL,
        PRIMARY KEY (id, lazy_id),
        CONSTRAINT FK863F36F1D846E3C3 FOREIGN KEY (lazy_id) REFERENCES cib_testentity (id) ,
        CONSTRAINT FK863F36F1C7A98674 FOREIGN KEY (id) REFERENCES cib_complextestentity (id),
        CONSTRAINT lazy_id UNIQUE (lazy_id),
        INDEX FK863F36F1C7A98674 (id),
        INDEX FK863F36F1D846E3C3 (lazy_id)
    );
    
    
CREATE TABLE TPSENTITY (
	id						BIGINT NOT NULL,
	langstring			VARCHAR(255),
	bytes					BLOB,
	datevalue			DATE,
	timevalue			TIME,
	floatvalue			float,
	doublevalue			double,
	timestampvalue		timestamp,
	onebyte				smallint,
	bool					char(1),
	decimalValue		decimal(5,3),
	clobvalue			mediumtext,
	nclobvalue			mediumtext,
	primary key(id)	
);

CREATE TABLE cib_complextestentity2 (
        id bigint NOT NULL AUTO_INCREMENT,
        owner VARCHAR(255),
        compValue INT NOT NULL,
        ten_id bigint,
        version INT NOT NULL,
        SELFOWNER2 varchar(255),
        PRIMARY KEY (id),
        CONSTRAINT FK_CC1 FOREIGN KEY (ten_id) REFERENCES cib_testentity (id),
        INDEX FK_CC2 (ten_id)
);

CREATE TABLE cib_tcomplexentity2_eager (
        id bigint NOT NULL,
        eager_id bigint NOT NULL,
        PRIMARY KEY (id, eager_id),
        CONSTRAINT FK_CC3 FOREIGN KEY (eager_id) REFERENCES cib_testentity (id) ,
        CONSTRAINT FK_CC4 FOREIGN KEY (id) REFERENCES cib_complextestentity2 (id),
        CONSTRAINT UNIQUE_eager_id UNIQUE (eager_id),
        INDEX FK_CC5 (id),
        INDEX FK_CC6 (eager_id)
    );
    
CREATE TABLE cib_tcomplexentity2_lazy (
        id bigint NOT NULL,
        lazy_id bigint NOT NULL,
        PRIMARY KEY (id, lazy_id),
        CONSTRAINT FK_CC7 FOREIGN KEY (lazy_id) REFERENCES cib_testentity (id) ,
        CONSTRAINT FK_CC8 FOREIGN KEY (id) REFERENCES cib_complextestentity2 (id),
        CONSTRAINT UNIQUE_lazy_id UNIQUE (lazy_id),
        INDEX FK_CC9 (id),
        INDEX FK_CC10 (lazy_id)
    );
