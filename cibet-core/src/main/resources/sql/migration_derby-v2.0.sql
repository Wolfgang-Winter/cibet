-- Migration Script for Cibet 2.0.  
-- Before executing, the following variables must be replaced:
-- $ARCHIVECONSTRAINT : foreign key constraint name from CIB_RESOURCEPARAMETER.ARCHIVEID to CIB_ARCHIVE.ARCHIVEID
-- $DCCONTROLLABLECONSTRAINT : foreign key constraint name from CIB_RESOURCEPARAMETER.DCCONTROLLABLEID to CIB_DCCONTROLLABLE.DCCONTROLLABLEID
-- After that, you can remove the security character (to prevent executing the script without replacing the variables): 
-- Remove all occurrences of !


-- cib_archive
!alter table CIB_ARCHIVE add ARCHIVEID1 VARCHAR(255) not null default '?';
!update CIB_ARCHIVE a1 set a1.ARCHIVEID1 = (select cast(cast(a2.ARCHIVEID as CHAR(254)) as VARCHAR(255)) from CIB_ARCHIVE a2 where a2.ARCHIVEID = a1.ARCHIVEID); 
-- FK to cib_archive
!alter table CIB_RESOURCEPARAMETER drop constraint $ARCHIVECONSTRAINT;
!alter table CIB_ARCHIVE drop ARCHIVEID;
!alter table CIB_ARCHIVE drop LASTARCHIVEID;
!rename column CIB_ARCHIVE.ARCHIVEID1 to ARCHIVEID; 
!alter table CIB_ARCHIVE alter column ARCHIVEID default null;
!alter table CIB_ARCHIVE add primary key(ARCHIVEID); 

-- cib_dccontrollable
!alter table CIB_DCCONTROLLABLE add CONTROLLABLEID VARCHAR(255) not null default '?';
!update CIB_DCCONTROLLABLE c1 set c1.CONTROLLABLEID = (select cast(cast(c2.DCCONTROLLABLEID as CHAR(254)) as VARCHAR(255)) from CIB_DCCONTROLLABLE c2 where c2.DCCONTROLLABLEID = c1.DCCONTROLLABLEID); 
-- fk to cib_dccontrollable
!alter table CIB_RESOURCEPARAMETER drop constraint $DCCONTROLLABLECONSTRAINT;
!alter table CIB_DCCONTROLLABLE drop DCCONTROLLABLEID;
!alter table CIB_DCCONTROLLABLE alter column CONTROLLABLEID default null;
!alter table CIB_DCCONTROLLABLE add primary key(CONTROLLABLEID);
!rename table CIB_DCCONTROLLABLE to CIB_CONTROLLABLE;

-- cib_resourceparameter
!alter table CIB_RESOURCEPARAMETER add PARAMETERID1 VARCHAR(255) not null default '?';
!alter table CIB_RESOURCEPARAMETER add ARCHIVEID1 VARCHAR(255);
!alter table CIB_RESOURCEPARAMETER add DCCONTROLLABLEID1 VARCHAR(255);
!update CIB_RESOURCEPARAMETER r1 set r1.PARAMETERID1 = (select cast(cast(r2.PARAMETERID as CHAR(254)) as VARCHAR(255)) from CIB_RESOURCEPARAMETER r2 where r2.PARAMETERID = r1.PARAMETERID); 
!update CIB_RESOURCEPARAMETER r1 set r1.ARCHIVEID1 = (select cast(cast(r2.ARCHIVEID as CHAR(254)) as VARCHAR(255)) from CIB_RESOURCEPARAMETER r2 where r2.PARAMETERID = r1.PARAMETERID); 
!update CIB_RESOURCEPARAMETER r1 set r1.DCCONTROLLABLEID1 = (select cast(cast(r2.DCCONTROLLABLEID as CHAR(254)) as VARCHAR(255)) from CIB_RESOURCEPARAMETER r2 where r2.PARAMETERID = r1.PARAMETERID); 
-- PK constraint
!alter table CIB_RESOURCEPARAMETER drop PARAMETERID;
!alter table CIB_RESOURCEPARAMETER drop ARCHIVEID;
!alter table CIB_RESOURCEPARAMETER drop DCCONTROLLABLEID;
!rename column CIB_RESOURCEPARAMETER.PARAMETERID1 to PARAMETERID;
!rename column CIB_RESOURCEPARAMETER.ARCHIVEID1 to ARCHIVEID;
!rename column CIB_RESOURCEPARAMETER.DCCONTROLLABLEID1 to DCCONTROLLABLEID;
!alter table CIB_RESOURCEPARAMETER alter column PARAMETERID default null;
!alter table CIB_RESOURCEPARAMETER add primary key(PARAMETERID);
!alter table CIB_RESOURCEPARAMETER add constraint ARCHIVE_FK foreign key (ARCHIVEID) REFERENCES CIB_ARCHIVE (ARCHIVEID);
!alter table CIB_RESOURCEPARAMETER add constraint DCCONTROLLABLE_FK foreign key (DCCONTROLLABLEID) REFERENCES CIB_DCCONTROLLABLE (DCCONTROLLABLEID);

-- cib_eventresult
!alter table CIB_EVENTRESULT add EVENTRESULTID1 VARCHAR(255) not null default '?';
!alter table CIB_EVENTRESULT add PARENTRESULT_ID1 VARCHAR(255);
!update CIB_EVENTRESULT e1 set e1.EVENTRESULTID1 = (select cast(cast(e2.EVENTRESULTID as CHAR(254)) as VARCHAR(255)) from CIB_EVENTRESULT e2 where e2.EVENTRESULTID = e1.EVENTRESULTID); 
!update CIB_EVENTRESULT e1 set e1.PARENTRESULT_ID1 = (select cast(cast(e2.PARENTRESULT_ID as CHAR(254)) as VARCHAR(255)) from CIB_EVENTRESULT e2 where e2.EVENTRESULTID = e1.EVENTRESULTID); 
-- FK parent eventresult
!alter table CIB_EVENTRESULT drop constraint FKEVENTRESULT_PARENT;
!alter table CIB_EVENTRESULT drop EVENTRESULTID;
!alter table CIB_EVENTRESULT drop PARENTRESULT_ID;
!rename column CIB_EVENTRESULT.EVENTRESULTID1 to EVENTRESULTID;
!rename column CIB_EVENTRESULT.PARENTRESULT_ID1 to PARENTRESULT_ID;
!alter table CIB_EVENTRESULT alter column EVENTRESULTID default null;
!alter table CIB_EVENTRESULT add primary key(EVENTRESULTID); 
!alter table CIB_EVENTRESULT add constraint EVENTRESULT_FK foreign key (PARENTRESULT_ID) REFERENCES CIB_EVENTRESULT (EVENTRESULTID);

