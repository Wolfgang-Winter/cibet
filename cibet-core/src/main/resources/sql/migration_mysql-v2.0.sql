-- Migration Script for Cibet 2.0.  
-- Before executing, the following variables must be replaced:
-- $ARCHIVECONSTRAINT : foreign key constraint name from CIB_RESOURCEPARAMETER.ARCHIVEID to CIB_ARCHIVE.ARCHIVEID
-- $DCCONTROLLABLECONSTRAINT : foreign key constraint name from CIB_RESOURCEPARAMETER.DCCONTROLLABLEID to CIB_DCCONTROLLABLE.DCCONTROLLABLEID
-- After that, you can remove the security character (to prevent executing the script without replacing the variables): 
-- Remove all occurrences of !

-- Replace here:
-- show the foreign key names:
SHOW CREATE TABLE CIB_RESOURCEPARAMETER;
-- replace with the foreign key names:
alter table CIB_RESOURCEPARAMETER drop foreign key $DCCONTROLLABLECONSTRAINT_1;
alter table CIB_RESOURCEPARAMETER drop foreign key $ARCHIVECONSTRAINT_1;
-- replace with the index names: 
alter table CIB_RESOURCEPARAMETER drop index $DCCONTROLLABLECONSTRAINT_2;
alter table CIB_RESOURCEPARAMETER drop index $ARCHIVECONSTRAINT_2;
-- end replacement

CREATE TABLE CIB_RESOURCE ( 
   RESOURCEID VARCHAR(255) NOT NULL, 
   RESOURCETYPE VARCHAR(31) NOT NULL,
   RESULT BLOB, 
   TARGET VARCHAR(255), 
   TARGETOBJECT BLOB, 
   PRIMARYKEYID VARCHAR(50), 
   METHOD VARCHAR(255), 
   INVOKERCLASS VARCHAR(255), 
   INVOKERPARAM VARCHAR(255), 
   ENCRYPTED smallint NOT NULL DEFAULT 0,
   KEYREFERENCE VARCHAR(255),
   UNIQUEID VARCHAR(255),
   GROUPID VARCHAR(255),
   PRIMARY KEY (RESOURCEID)
);

-- cib_archive
!delete from CIB_ARCHIVE where ARCHIVEID = -1;
!alter table CIB_ARCHIVE add column ARCHIVEID1 VARCHAR(255) default '?' not null;
!update CIB_ARCHIVE set ARCHIVEID1 = trim(cast(ARCHIVEID as CHAR(254)));
!alter table CIB_ARCHIVE drop column ARCHIVEID;
!alter table CIB_ARCHIVE drop column LASTARCHIVEID;
!alter table CIB_ARCHIVE change column ARCHIVEID1 ARCHIVEID varchar(255) not null;
!alter table CIB_ARCHIVE add primary key(ARCHIVEID);

insert into CIB_RESOURCE (RESOURCEID, RESOURCETYPE, RESULT, TARGET, TARGETOBJECT, PRIMARYKEYID, METHOD, INVOKERCLASS, INVOKERPARAM, 
   ENCRYPTED, KEYREFERENCE, UNIQUEID, GROUPID) select concat('A', ARCHIVEID), 
   case
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.jpa.JpaResourceHandler' then 'JpaResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.ejb.EjbResourceHandler' then 'EjbResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.http.HttpRequestResourceHandler' then 'HttpRequestResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.jdbc.driver.JdbcResourceHandler' then 'JdbcResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.jpa.JpaQueryResourceHandler' then 'JpaQueryResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.jpa.JpaUpdateResourceHandler' then 'JpaResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.pojo.MethodResourceHandler' then 'MethodResource'
     else 'error'
   end,
   RESULT, TARGETTYPE, TARGET, PRIMARYKEYID, METHOD, INVOKERCLASS, INVOKERPARAM, 
   ENCRYPTED, KEYREFERENCE, UNIQUEID, GROUPID from CIB_ARCHIVE;

alter table CIB_ARCHIVE drop column RESULT; 
alter table CIB_ARCHIVE drop column TARGETTYPE;
alter table CIB_ARCHIVE drop column TARGET;
alter table CIB_ARCHIVE drop column PRIMARYKEYID;
alter table CIB_ARCHIVE drop column METHOD;
alter table CIB_ARCHIVE drop column INVOKERCLASS;
alter table CIB_ARCHIVE drop column INVOKERPARAM;
alter table CIB_ARCHIVE drop column RESOURCEHANDLERCLASS;
alter table CIB_ARCHIVE drop column ENCRYPTED;
alter table CIB_ARCHIVE drop column KEYREFERENCE;
alter table CIB_ARCHIVE drop column UNIQUEID;
alter table CIB_ARCHIVE drop column GROUPID;

alter table CIB_ARCHIVE add column RESOURCEID VARCHAR(255) default '?' not null;
update CIB_ARCHIVE set RESOURCEID = concat('A', ARCHIVEID); 
alter table CIB_ARCHIVE add constraint ARCH_RESOURCE_FK foreign key (RESOURCEID) REFERENCES CIB_RESOURCE (RESOURCEID);
alter table CIB_ARCHIVE alter column RESOURCEID drop default;

-- cib_dccontrollable
!alter table CIB_DCCONTROLLABLE add column CONTROLLABLEID VARCHAR(255) default '?' not null;
!update CIB_DCCONTROLLABLE set CONTROLLABLEID = trim(cast(DCCONTROLLABLEID as CHAR(254))); 
!alter table CIB_DCCONTROLLABLE drop column DCCONTROLLABLEID;
!alter table CIB_DCCONTROLLABLE alter column CONTROLLABLEID drop default;
!alter table CIB_DCCONTROLLABLE add primary key(CONTROLLABLEID);
!alter table CIB_DCCONTROLLABLE rename to CIB_CONTROLLABLE;

insert into CIB_RESOURCE (RESOURCEID, RESOURCETYPE, RESULT, TARGET, TARGETOBJECT, PRIMARYKEYID, METHOD, INVOKERCLASS, INVOKERPARAM, 
   ENCRYPTED, KEYREFERENCE, UNIQUEID, GROUPID) select concat('C', CONTROLLABLEID), 
   case
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.jpa.JpaResourceHandler' then 'JpaResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.ejb.EjbResourceHandler' then 'EjbResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.http.HttpRequestResourceHandler' then 'HttpRequestResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.jdbc.driver.JdbcResourceHandler' then 'JdbcResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.jpa.JpaQueryResourceHandler' then 'JpaQueryResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.jpa.JpaUpdateResourceHandler' then 'JpaResource'
     when RESOURCEHANDLERCLASS = 'com.logitags.cibet.sensor.pojo.MethodResourceHandler' then 'MethodResource'
     else 'error'
   end,
   RESULT, TARGETTYPE, TARGET, PRIMARYKEYID, METHOD, INVOKERCLASS, INVOKERPARAM, 
   ENCRYPTED, KEYREFERENCE, UNIQUEID, GROUPID from CIB_CONTROLLABLE;

alter table CIB_CONTROLLABLE drop column RESULT; 
alter table CIB_CONTROLLABLE drop column TARGETTYPE;
alter table CIB_CONTROLLABLE drop column TARGET;
alter table CIB_CONTROLLABLE drop column PRIMARYKEYID;
alter table CIB_CONTROLLABLE drop column METHOD;
alter table CIB_CONTROLLABLE drop column INVOKERCLASS;
alter table CIB_CONTROLLABLE drop column INVOKERPARAM;
alter table CIB_CONTROLLABLE drop column RESOURCEHANDLERCLASS;
alter table CIB_CONTROLLABLE drop column ENCRYPTED;
alter table CIB_CONTROLLABLE drop column KEYREFERENCE;
alter table CIB_CONTROLLABLE drop column UNIQUEID;
alter table CIB_CONTROLLABLE drop column GROUPID;

alter table CIB_CONTROLLABLE add column RESOURCEID VARCHAR(255) default '?' not null;
update CIB_CONTROLLABLE set RESOURCEID = concat('C', CONTROLLABLEID);  
alter table CIB_CONTROLLABLE add constraint CONT_RESOURCE_FK foreign key (RESOURCEID) REFERENCES CIB_RESOURCE (RESOURCEID);
alter table CIB_CONTROLLABLE alter column RESOURCEID drop default;

alter table CIB_CONTROLLABLE change column APPROVALUSER RELEASEUSER VARCHAR(50); 
alter table CIB_CONTROLLABLE change column APPROVALADDRESS RELEASEADDRESS VARCHAR(255); 
alter table CIB_CONTROLLABLE change column APPROVALREMARK RELEASEREMARK VARCHAR(255); 
alter table CIB_CONTROLLABLE change column APPROVALDATE RELEASEDATE DATETIME; 


-- cib_resourceparameter
alter table CIB_RESOURCEPARAMETER add column PARAMETERID1 VARCHAR(255) default '?' not null;
alter table CIB_RESOURCEPARAMETER add column RESOURCEID VARCHAR(255) default '?' not null;
update CIB_RESOURCEPARAMETER set PARAMETERID1 = trim(cast(PARAMETERID as CHAR(254))); 
 
update CIB_RESOURCEPARAMETER set RESOURCEID = 
 case 
   when ARCHIVEID is not null then trim(concat('A', cast(ARCHIVEID as CHAR(254))))
   when DCCONTROLLABLEID is not null then trim(concat('C', cast(DCCONTROLLABLEID as CHAR(254))))
 end;
 
alter table CIB_RESOURCEPARAMETER drop column PARAMETERID;
alter table CIB_RESOURCEPARAMETER drop column ARCHIVEID;
alter table CIB_RESOURCEPARAMETER drop column DCCONTROLLABLEID;
alter table CIB_RESOURCEPARAMETER change column PARAMETERID1 PARAMETERID VARCHAR(255);
alter table CIB_RESOURCEPARAMETER alter column PARAMETERID drop default;
alter table CIB_RESOURCEPARAMETER alter column RESOURCEID drop default;
alter table CIB_RESOURCEPARAMETER add primary key(PARAMETERID);
alter table CIB_RESOURCEPARAMETER add constraint RESOURCE_FK foreign key (RESOURCEID) REFERENCES CIB_RESOURCE (RESOURCEID);

-- cib_eventresult
!alter table CIB_EVENTRESULT add column EVENTRESULTID1 VARCHAR(255) default '?' not null;
!alter table CIB_EVENTRESULT add column PARENTRESULT_ID1 VARCHAR(255);
update CIB_EVENTRESULT set EVENTRESULTID1 = trim(cast(EVENTRESULTID as CHAR(254))); 
update CIB_EVENTRESULT set PARENTRESULT_ID1 = trim(cast(PARENTRESULT_ID as CHAR(254))); 
-- FK parent eventresult
alter table CIB_EVENTRESULT drop foreign key FKEVENTRESULT_PARENT;
alter table CIB_EVENTRESULT drop index FKEVENTRESULT_PARENT;
!alter table CIB_EVENTRESULT drop column EVENTRESULTID;
!alter table CIB_EVENTRESULT drop column PARENTRESULT_ID;
!alter table CIB_EVENTRESULT change column EVENTRESULTID1 EVENTRESULTID VARCHAR(255) NOT NULL;
!alter table CIB_EVENTRESULT change column PARENTRESULT_ID1 PARENTRESULT_ID VARCHAR(255);
!alter table CIB_EVENTRESULT alter column EVENTRESULTID drop default;
!alter table CIB_EVENTRESULT add primary key(EVENTRESULTID); 
!alter table CIB_EVENTRESULT add constraint EVENTRESULT_FK foreign key (PARENTRESULT_ID) REFERENCES CIB_EVENTRESULT (EVENTRESULTID);

-- cib_lockedobject
insert into CIB_RESOURCE (RESOURCEID, RESOURCETYPE, TARGET, TARGETOBJECT, PRIMARYKEYID, METHOD,  
   ENCRYPTED) select trim(concat('L', cast(LOCKEDOBJECTID as CHAR(254)))), 
   case
     when LOCKEDEVENT = 'UPDATE' then 'JpaResource'
     when LOCKEDEVENT = 'PERSIST' then 'JpaResource'
     when LOCKEDEVENT = 'DELETE' then 'JpaResource'
     when LOCKEDEVENT = 'SELECT' then 'JpaResource'
     when LOCKEDEVENT = 'INVOKE' then 'MethodResource'
     else 'JpaResource'
   end,
   TARGETTYPE, OBJECT, OBJECTID, METHOD, 0 from CIB_LOCKEDOBJECT;

insert into CIB_CONTROLLABLE (CONTROLEVENT, CREATEUSER, CREATEDATE, CREATEREMARK, TENANT, RELEASEUSER, RELEASEDATE,
   RELEASEREMARK, EXECUTIONSTATUS, VERSION, CONTROLLABLEID, RESOURCEID, ACTUATOR) 
   select LOCKEDEVENT, LOCKEDBY, LOCKDATE, LOCKREMARK, TENANT, UNLOCKEDBY, UNLOCKDATE, UNLOCKREMARK, LOCKSTATE, 0, 
   trim(concat('L', cast(LOCKEDOBJECTID as CHAR(254)))),
   trim(concat('L', cast(LOCKEDOBJECTID as CHAR(254)))),
   'LOCKER' from CIB_LOCKEDOBJECT;
   
drop table CIB_LOCKEDOBJECT;
DROP TABLE CIB_INTEGRITYCHECKRESULT;
DROP TABLE CIB_INTEGRITYCHECK;
   