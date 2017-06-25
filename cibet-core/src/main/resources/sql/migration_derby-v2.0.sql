-- Migration Script for Cibet 2.0.  
-- Before executing, the following variables must be replaced:
-- $ARCHIVECONSTRAINT : foreign key constraint name from CIB_RESOURCEPARAMETER.ARCHIVEID to CIB_ARCHIVE.ARCHIVEID
-- $DCCONTROLLABLECONSTRAINT : foreign key constraint name from CIB_RESOURCEPARAMETER.DCCONTROLLABLEID to CIB_DCCONTROLLABLE.DCCONTROLLABLEID
-- After that, you can remove the security character (to prevent executing the script without replacing the variables): 
-- Remove all occurrences of !

-- Replace here:
alter table CIB_RESOURCEPARAMETER drop constraint $ARCHIVECONSTRAINT;
alter table CIB_RESOURCEPARAMETER drop constraint $DCCONTROLLABLECONSTRAINT;
-- end replacement

!CREATE TABLE CIB_RESOURCE ( 
   RESOURCEID VARCHAR(255) NOT NULL, 
   RESOURCETYPE VARCHAR(31) NOT NULL,
   RESULT BLOB(64 K), 
   TARGET VARCHAR(255), 
   TARGETOBJECT BLOB(64 K), 
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
!alter table CIB_ARCHIVE add ARCHIVEID1 VARCHAR(255) not null default '?';
!update CIB_ARCHIVE a1 set a1.ARCHIVEID1 = (select trim(cast(cast(a2.ARCHIVEID as CHAR(254)) as VARCHAR(255))) from CIB_ARCHIVE a2 where a2.ARCHIVEID = a1.ARCHIVEID); 
!alter table CIB_ARCHIVE drop ARCHIVEID;
!alter table CIB_ARCHIVE drop LASTARCHIVEID;
!rename column CIB_ARCHIVE.ARCHIVEID1 to ARCHIVEID; 
!alter table CIB_ARCHIVE alter column ARCHIVEID drop default;
!alter table CIB_ARCHIVE add primary key(ARCHIVEID);

!insert into CIB_RESOURCE (RESOURCEID, RESOURCETYPE, RESULT, TARGET, TARGETOBJECT, PRIMARYKEYID, METHOD, INVOKERCLASS, INVOKERPARAM, 
   ENCRYPTED, KEYREFERENCE, UNIQUEID, GROUPID) select 'A' || ARCHIVEID, 
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
 
!alter table CIB_ARCHIVE drop RESULT; 
!alter table CIB_ARCHIVE drop TARGETTYPE;
!alter table CIB_ARCHIVE drop TARGET;
!alter table CIB_ARCHIVE drop PRIMARYKEYID;
!alter table CIB_ARCHIVE drop METHOD;
!alter table CIB_ARCHIVE drop INVOKERCLASS;
!alter table CIB_ARCHIVE drop INVOKERPARAM;
!alter table CIB_ARCHIVE drop RESOURCEHANDLERCLASS;
!alter table CIB_ARCHIVE drop ENCRYPTED;
!alter table CIB_ARCHIVE drop KEYREFERENCE;
!alter table CIB_ARCHIVE drop UNIQUEID;
!alter table CIB_ARCHIVE drop GROUPID;

!alter table CIB_ARCHIVE add RESOURCEID VARCHAR(255) not null default '?';
!update CIB_ARCHIVE a1 set a1.RESOURCEID = (select 'A' || a2.ARCHIVEID from CIB_ARCHIVE a2 where a1.ARCHIVEID = a2.ARCHIVEID); 
!alter table CIB_ARCHIVE add constraint ARCH_RESOURCE_FK foreign key (RESOURCEID) REFERENCES CIB_RESOURCE (RESOURCEID);
!alter table CIB_ARCHIVE alter column RESOURCEID default null;

-- cib_dccontrollable
!alter table CIB_DCCONTROLLABLE add CONTROLLABLEID VARCHAR(255) not null default '?';
!update CIB_DCCONTROLLABLE c1 set c1.CONTROLLABLEID = (select trim(cast(cast(c2.DCCONTROLLABLEID as CHAR(254)) as VARCHAR(255))) from CIB_DCCONTROLLABLE c2 where c2.DCCONTROLLABLEID = c1.DCCONTROLLABLEID); 
!alter table CIB_DCCONTROLLABLE drop DCCONTROLLABLEID;
!alter table CIB_DCCONTROLLABLE alter column CONTROLLABLEID drop default;
!alter table CIB_DCCONTROLLABLE add primary key(CONTROLLABLEID);
!rename table CIB_DCCONTROLLABLE to CIB_CONTROLLABLE;

!insert into CIB_RESOURCE (RESOURCEID, RESOURCETYPE, RESULT, TARGET, TARGETOBJECT, PRIMARYKEYID, METHOD, INVOKERCLASS, INVOKERPARAM, 
   ENCRYPTED, KEYREFERENCE, UNIQUEID, GROUPID) select 'C' || CONTROLLABLEID, 
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

!alter table CIB_CONTROLLABLE drop RESULT; 
!alter table CIB_CONTROLLABLE drop TARGETTYPE;
!alter table CIB_CONTROLLABLE drop TARGET;
!alter table CIB_CONTROLLABLE drop PRIMARYKEYID;
!alter table CIB_CONTROLLABLE drop METHOD;
!alter table CIB_CONTROLLABLE drop INVOKERCLASS;
!alter table CIB_CONTROLLABLE drop INVOKERPARAM;
!alter table CIB_CONTROLLABLE drop RESOURCEHANDLERCLASS;
!alter table CIB_CONTROLLABLE drop ENCRYPTED;
!alter table CIB_CONTROLLABLE drop KEYREFERENCE;
!alter table CIB_CONTROLLABLE drop UNIQUEID;
!alter table CIB_CONTROLLABLE drop GROUPID;

!alter table CIB_CONTROLLABLE add RESOURCEID VARCHAR(255) not null default '?';
!update CIB_CONTROLLABLE a1 set a1.RESOURCEID = (select 'C' || a2.CONTROLLABLEID from CIB_CONTROLLABLE a2 where a1.CONTROLLABLEID = a2.CONTROLLABLEID); 
!alter table CIB_CONTROLLABLE add constraint CONT_RESOURCE_FK foreign key (RESOURCEID) REFERENCES CIB_RESOURCE (RESOURCEID);
!alter table CIB_CONTROLLABLE alter column RESOURCEID default null;

!rename column CIB_CONTROLLABLE.APPROVALUSER to RELEASEUSER; 
!rename column CIB_CONTROLLABLE.APPROVALADDRESS to RELEASEADDRESS; 
!rename column CIB_CONTROLLABLE.APPROVALREMARK to RELEASEREMARK; 
!rename column CIB_CONTROLLABLE.APPROVALDATE to RELEASEDATE; 


-- cib_resourceparameter
!alter table CIB_RESOURCEPARAMETER add PARAMETERID1 VARCHAR(255) not null default '?';
!alter table CIB_RESOURCEPARAMETER add RESOURCEID VARCHAR(255);
!update CIB_RESOURCEPARAMETER r1 set r1.PARAMETERID1 = (select trim(cast(cast(r2.PARAMETERID as CHAR(254)) as VARCHAR(255))) from CIB_RESOURCEPARAMETER r2 where r2.PARAMETERID = r1.PARAMETERID);
 
!update CIB_RESOURCEPARAMETER r1 set r1.RESOURCEID = 
(select 
 case 
   when r2.ARCHIVEID is not null then trim('A' || cast(cast(r2.ARCHIVEID as CHAR(254)) as VARCHAR(255)))
   when r2.DCCONTROLLABLEID is not null then trim('C' || cast(cast(r2.DCCONTROLLABLEID as CHAR(254)) as VARCHAR(255)))
 end   
from CIB_RESOURCEPARAMETER r2 where r2.PARAMETERID = r1.PARAMETERID);
 
!alter table CIB_RESOURCEPARAMETER drop PARAMETERID;
!alter table CIB_RESOURCEPARAMETER drop ARCHIVEID;
!alter table CIB_RESOURCEPARAMETER drop DCCONTROLLABLEID;
!rename column CIB_RESOURCEPARAMETER.PARAMETERID1 to PARAMETERID;
!alter table CIB_RESOURCEPARAMETER alter column PARAMETERID drop default;
!alter table CIB_RESOURCEPARAMETER add primary key(PARAMETERID);
!alter table CIB_RESOURCEPARAMETER add constraint RESOURCE_FK foreign key (RESOURCEID) REFERENCES CIB_RESOURCE (RESOURCEID);

-- cib_eventresult
!alter table CIB_EVENTRESULT add EVENTRESULTID1 VARCHAR(255) not null default '?';
!alter table CIB_EVENTRESULT add PARENTRESULT_ID1 VARCHAR(255);
!update CIB_EVENTRESULT e1 set e1.EVENTRESULTID1 = (select trim(cast(cast(e2.EVENTRESULTID as CHAR(254)) as VARCHAR(255))) from CIB_EVENTRESULT e2 where e2.EVENTRESULTID = e1.EVENTRESULTID); 
!update CIB_EVENTRESULT e1 set e1.PARENTRESULT_ID1 = (select trim(cast(cast(e2.PARENTRESULT_ID as CHAR(254)) as VARCHAR(255))) from CIB_EVENTRESULT e2 where e2.EVENTRESULTID = e1.EVENTRESULTID); 
-- FK parent eventresult
!alter table CIB_EVENTRESULT drop constraint FKEVENTRESULT_PARENT;
!alter table CIB_EVENTRESULT drop EVENTRESULTID;
!alter table CIB_EVENTRESULT drop PARENTRESULT_ID;
!rename column CIB_EVENTRESULT.EVENTRESULTID1 to EVENTRESULTID;
!rename column CIB_EVENTRESULT.PARENTRESULT_ID1 to PARENTRESULT_ID;
!alter table CIB_EVENTRESULT alter column EVENTRESULTID drop default;
!alter table CIB_EVENTRESULT add primary key(EVENTRESULTID); 
!alter table CIB_EVENTRESULT add constraint EVENTRESULT_FK foreign key (PARENTRESULT_ID) REFERENCES CIB_EVENTRESULT (EVENTRESULTID);

-- cib_lockedobject
!insert into CIB_RESOURCE (RESOURCEID, RESOURCETYPE, TARGET, TARGETOBJECT, PRIMARYKEYID, METHOD,  
   ENCRYPTED) select trim('L' || cast(cast(LOCKEDOBJECTID as CHAR(254)) as VARCHAR(255))), 
   case
     when LOCKEDEVENT = 'UPDATE' then 'JpaResource'
     when LOCKEDEVENT = 'PERSIST' then 'JpaResource'
     when LOCKEDEVENT = 'DELETE' then 'JpaResource'
     when LOCKEDEVENT = 'SELECT' then 'JpaResource'
     when LOCKEDEVENT = 'INVOKE' then 'MethodResource'
     else 'JpaResource'
   end,
   TARGETTYPE, OBJECT, OBJECTID, METHOD, 0 from CIB_LOCKEDOBJECT;

!insert into CIB_CONTROLLABLE (CONTROLEVENT, CREATEUSER, CREATEDATE, CREATEREMARK, TENANT, RELEASEUSER, RELEASEDATE,
   RELEASEREMARK, EXECUTIONSTATUS, VERSION, CONTROLLABLEID, RESOURCEID, ACTUATOR) 
   select LOCKEDEVENT, LOCKEDBY, LOCKDATE, LOCKREMARK, TENANT, UNLOCKEDBY, UNLOCKDATE, UNLOCKREMARK, LOCKSTATE, 0, 
   trim('L' || cast(cast(LOCKEDOBJECTID as CHAR(254)) as VARCHAR(255))),
   trim('L' || cast(cast(LOCKEDOBJECTID as CHAR(254)) as VARCHAR(255))),
   'LOCKER' from CIB_LOCKEDOBJECT;
   
!drop table CIB_LOCKEDOBJECT;
!DROP TABLE CIB_INTEGRITYCHECKRESULT;
!DROP TABLE CIB_INTEGRITYCHECK;
   