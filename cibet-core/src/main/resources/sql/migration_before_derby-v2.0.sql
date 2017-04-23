-- Migration Script for Cibet 2.0. Must be executed BEFORE software is updated. 
-- Before executing, the following variables must be replaced:
-- $ARCHIVECONSTRAINT : foreign key constraint name from CIB_RESOURCEPARAMETER.ARCHIVEID to CIB_ARCHIVE.ARCHIVEID
-- $DCCONTROLLABLECONSTRAINT : foreign key constraint name from CIB_RESOURCEPARAMETER.DCCONTROLLABLEID to CIB_DCCONTROLLABLE.DCCONTROLLABLEID
-- After that, you can remove the security character (to prevent executing the script without replacing the variables): 
-- Remove all occurrences of !


-- cib_archive
!alter table CIB_ARCHIVE add ARCHIVEID1 VARCHAR(255);
!update CIB_ARCHIVE a1 set a1.ARCHIVEID1 = (select cast(cast(a2.ARCHIVEID as CHAR(254)) as VARCHAR(255)) from CIB_ARCHIVE a2 where a2.ARCHIVEID = a1.ARCHIVEID); 
-- FK to cib_archive
!alter table CIB_RESOURCEPARAMETER drop constraint $ARCHIVECONSTRAINT;
!alter table CIB_ARCHIVE drop primary key;
!alter table CIB_ARCHIVE alter column ARCHIVEID default -1;
!alter table CIB_ARCHIVE alter column LASTARCHIVEID default -1;

-- cib_dccontrollable
!alter table CIB_DCCONTROLLABLE add DCCONTROLLABLEID1 VARCHAR(255);
!update CIB_DCCONTROLLABLE c1 set c1.DCCONTROLLABLEID1 = (select cast(cast(c2.DCCONTROLLABLEID as CHAR(254)) as VARCHAR(255)) from CIB_DCCONTROLLABLE c2 where c2.DCCONTROLLABLEID = c1.DCCONTROLLABLEID); 
-- fk to cib_dccontrollable
!alter table CIB_RESOURCEPARAMETER drop constraint $DCCONTROLLABLECONSTRAINT;
!alter table CIB_DCCONTROLLABLE drop primary key;
!alter table CIB_DCCONTROLLABLE alter column DCCONTROLLABLEID default -1;

-- cib_resourceparameter
!alter table CIB_RESOURCEPARAMETER add PARAMETERID1 VARCHAR(255);
!alter table CIB_RESOURCEPARAMETER add ARCHIVEID1 VARCHAR(255);
!alter table CIB_RESOURCEPARAMETER add DCCONTROLLABLEID1 VARCHAR(255);
!update CIB_RESOURCEPARAMETER r1 set r1.PARAMETERID1 = (select cast(cast(r2.PARAMETERID as CHAR(254)) as VARCHAR(255)) from CIB_RESOURCEPARAMETER r2 where r2.PARAMETERID = r1.PARAMETERID); 
!update CIB_RESOURCEPARAMETER r1 set r1.ARCHIVEID1 = (select cast(cast(r2.ARCHIVEID as CHAR(254)) as VARCHAR(255)) from CIB_RESOURCEPARAMETER r2 where r2.PARAMETERID = r1.PARAMETERID); 
!update CIB_RESOURCEPARAMETER r1 set r1.DCCONTROLLABLEID1 = (select cast(cast(r2.DCCONTROLLABLEID as CHAR(254)) as VARCHAR(255)) from CIB_RESOURCEPARAMETER r2 where r2.PARAMETERID = r1.PARAMETERID); 
-- PK constraint
!alter table CIB_RESOURCEPARAMETER drop primary key;
!alter table CIB_RESOURCEPARAMETER alter column PARAMETERID default -1;

-- cib:lockedobject
!alter table CIB_LOCKEDOBJECT add LOCKEDOBJECTID1 VARCHAR(255);
!update CIB_LOCKEDOBJECT l1 set l1.LOCKEDOBJECTID1 = (select cast(cast(l2.LOCKEDOBJECTID as CHAR(254)) as VARCHAR(255)) from CIB_LOCKEDOBJECT l2 where l2.LOCKEDOBJECTID = l1.LOCKEDOBJECTID); 
!alter table CIB_LOCKEDOBJECT drop primary key;
!alter table CIB_LOCKEDOBJECT alter column LOCKEDOBJECTID default -1;

-- cib_eventresult
!alter table CIB_EVENTRESULT add EVENTRESULTID1 VARCHAR(255);
!alter table CIB_EVENTRESULT add PARENTRESULT_ID1 VARCHAR(255);
!update CIB_EVENTRESULT e1 set e1.EVENTRESULTID1 = (select cast(cast(e2.EVENTRESULTID as CHAR(254)) as VARCHAR(255)) from CIB_EVENTRESULT e2 where e2.EVENTRESULTID = e1.EVENTRESULTID); 
!update CIB_EVENTRESULT e1 set e1.PARENTRESULT_ID1 = (select cast(cast(e2.PARENTRESULT_ID as CHAR(254)) as VARCHAR(255)) from CIB_EVENTRESULT e2 where e2.EVENTRESULTID = e1.EVENTRESULTID); 
-- FK parent eventresult
!alter table CIB_EVENTRESULT drop constraint FKEVENTRESULT_PARENT;
!alter table CIB_EVENTRESULT drop primary key;
!alter table CIB_EVENTRESULT alter column EVENTRESULTID default -1;


