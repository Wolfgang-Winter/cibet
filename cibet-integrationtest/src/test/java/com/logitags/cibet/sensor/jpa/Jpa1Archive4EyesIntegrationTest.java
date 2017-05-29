/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.sensor.jpa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.PersistenceException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.base.IdComparator;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.UnapprovedResourceException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * tests CibetEntityManager with Archive and FourEyes- actuators.
 * 
 * @author test
 * 
 */
public class Jpa1Archive4EyesIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(Jpa1Archive4EyesIntegrationTest.class);

   private Setpoint sp = null;

   @After
   public void afterJpa1Archive2ManRuleIntegrationTest() {
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   private void updateRemoveWith4Eyes(List<String> schemes) throws InterruptedException {
      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Thread.sleep(60);

      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
      selEnt.setCounter(12);
      applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();

      try {
         applEman.remove(entity);
         Assert.fail("UnapprovedEntityException not thrown");
      } catch (UnapprovedResourceException e) {
      }

      selEnt = applEman.find(TEntity.class, entity.getId());
      String msg = "entity with id " + entity.getId() + " not found";
      log.info("selEnt = " + selEnt);
      Assert.assertNotNull(msg, selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", "testTenant");
      List<Archive> list = q.getResultList();
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ControlEvent.UPDATE, list.get(0).getControlEvent());

      Assert.assertTrue(list.get(1).getRemark().startsWith("An unreleased Dual Control business case with ID "));
      Assert.assertEquals(ExecutionStatus.ERROR, list.get(1).getExecutionStatus());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.UPDATE, list1.get(0).getControlEvent());
   }

   private void checkEntity(TComplexEntity entity) {
      List<TEntity> eagerArlist = new ArrayList<TEntity>(entity.getEagerList());
      Collections.sort(eagerArlist, new IdComparator());
      List<TEntity> lazyArlist = new ArrayList<TEntity>(entity.getLazyList());
      Collections.sort(lazyArlist, new IdComparator());

      Assert.assertEquals(6, eagerArlist.get(0).getCounter());
      Assert.assertEquals(7, eagerArlist.get(1).getCounter());
      Assert.assertEquals(4, lazyArlist.get(0).getCounter());
      Assert.assertEquals(5, lazyArlist.get(1).getCounter());
      Assert.assertEquals(6, lazyArlist.get(2).getCounter());
      Assert.assertEquals(3, entity.getTen().getCounter());
   }

   @Test
   public void persistNoControl() {
      log.info("start persistNoControl()");
      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
   }

   @Test
   public void persistWithArchive() {
      log.info("start persistWithArchive()");
      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(TEntity.class.getName(), res.getTargetType());
      TEntity en = (TEntity) res.getObject();
      Assert.assertTrue("expected: " + entity.getId() + ", actual: " + en.getId(), en.getId() == entity.getId());
   }

   @Test
   public void persistWithArchivePersistEvent() throws InterruptedException {
      log.info("start persistWithArchivePersistEvent()");
      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.PERSIST);

      TEntity entity = persistTEntity();
      Thread.sleep(20);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(TEntity.class.getName(), res.getTargetType());
      TEntity en = (TEntity) res.getObject();
      Assert.assertTrue("expected: " + entity.getId() + ", actual: " + en.getId(), en.getId() == entity.getId());
   }

   @Test
   public void persistWithArchiveAllEvent() throws InterruptedException {
      log.info("start persistWithArchiveAllEvent()");
      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.ALL);

      TEntity entity = persistTEntity();
      Thread.sleep(2);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(TEntity.class.getName(), res.getTargetType());
      TEntity en = (TEntity) res.getObject();
      Assert.assertTrue("expected: " + entity.getId() + ", actual: " + en.getId(), en.getId() == entity.getId());
   }

   @Test
   public void persistWith4Eyes() {
      log.info("start persistWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertEquals("0", res.getPrimaryKeyId());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());
   }

   @Test
   public void removeNoControl() {
      log.info("start removeNoControl()");

      TEntity entity = persistTEntity();
      Assert.assertTrue(!applEman.contains(entity));
      log.info("in context: " + applEman.contains(entity) + ", " + entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      log.info("in context: " + applEman.contains(selEnt) + ", " + selEnt);
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertTrue(applEman.contains(selEnt));
      Assert.assertNotSame(selEnt, entity);
      Assert.assertEquals(5, selEnt.getCounter());

      applEman.remove(entity);
      selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);
   }

   @Test
   public void removeWithArchive() throws InterruptedException {
      log.info("start removeWithArchive()");
      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(TEntity.class.getName(), res.getTargetType());
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());

      Thread.sleep(60);
      applEman.remove(entity);

      list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(), String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.DELETE, list.get(1).getControlEvent());
   }

   @Test
   public void removeWith4Eyes() {
      log.info("start removeWith4Eyes()");

      TEntity entity = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      applEman.remove(entity);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
      List<Archive> list = ArchiveLoader.loadArchives();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.DELETE, list.get(0).getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.DELETE, list1.get(0).getControlEvent());
   }

   @Test
   public void updateNoControl() {
      log.info("start updateNoControl()");

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      selEnt.setCounter(12);
      applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();

      TEntity selEnt2 = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt2);
      Assert.assertNotSame(selEnt, selEnt2);
      Assert.assertEquals(12, selEnt2.getCounter());
   }

   @Test
   public void updateWithArchive() {
      log.info("start updateWithArchive()");

      TEntity entity = persistTEntity();
      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
      selEnt.setCounter(12);
      applEman.merge(selEnt);

      TEntity selEnt2 = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt2);
      Assert.assertEquals(12, selEnt2.getCounter());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(TEntity.class.getName(), ar.getResource().getTargetType());
      Assert.assertEquals(ControlEvent.UPDATE, ar.getControlEvent());
   }

   @Test
   public void updateWith4Eyes() {
      log.info("start updateWith4Eyes()");

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
      selEnt.setCounter(12);
      applEman.merge(selEnt);

      selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ArchiveLoader.loadArchives();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ControlEvent.UPDATE, list.get(0).getControlEvent());
      JpaResource res = (JpaResource) list.get(0).getResource();
      TEntity tent = (TEntity) res.getObject();
      Assert.assertEquals(12, tent.getCounter());

      List<Controllable> list1 = DcLoader.findUnreleased(TEntity.class.getName());
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.UPDATE, list1.get(0).getControlEvent());
   }

   @Test
   public void persistWithArchiveRollback() {
      log.info("start persistWithArchiveRollback()");

      TEntity entity1 = new TEntity();
      entity1.setCounter(25);
      entity1.setNameValue("valuexxcc");
      entity1.setOwner("testTenant");
      applEman.persist(entity1);

      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TEntity entity = persistTEntity();

      TEntity selEnt1 = applEman.find(TEntity.class, entity1.getId());
      Assert.assertNotNull(selEnt1);
      Assert.assertEquals(25, selEnt1.getCounter());

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_BY_PRIMARYKEYID);
      q.setParameter("targetType", TEntity.class.getName());
      q.setParameter("primaryKeyId", String.valueOf(entity.getId()));
      List<Archive> list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(TEntity.class.getName(), ar.getResource().getTargetType());
   }

   @Test
   public void persistWithQueryInBetween() {
      log.info("start persistWithQueryInBetween()");

      TEntity entity = persistTEntity();

      TEntity entity1 = new TEntity();
      entity1.setCounter(25);
      entity1.setNameValue("valuexxcc");
      entity1.setOwner("testTenant");
      applEman.persist(entity1);

      TEntity selEnt1 = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt1);
      Assert.assertEquals(5, selEnt1.getCounter());

      TEntity selEnt2 = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt2);
      Assert.assertEquals(5, selEnt2.getCounter());

      applEman.flush();

      selEnt1 = applEman.find(TEntity.class, entity1.getId());
      Assert.assertNotNull(selEnt1);
      Assert.assertEquals(25, selEnt1.getCounter());

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
   }

   @Test
   public void updateRemoveWith4EyesNormalSequence() throws InterruptedException {
      log.info("start updateRemoveWith4EyesNormalSequence()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      updateRemoveWith4Eyes(schemes);
   }

   @Test
   public void updateRemoveWith4EyesReversedActuatorSequence() throws InterruptedException {
      log.info("start updateRemoveWith4EyesReversedActuatorSequence()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      updateRemoveWith4Eyes(schemes);
   }

   @Test
   public void testRefresh() {
      log.info("start testRefresh()");
      TEntity entity = persistTEntity();

      applEman.refresh(entity);

      Assert.assertEquals(5, entity.getCounter());

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
      selEnt.setCounter(12);
      applEman.merge(selEnt);
      applEman.flush();

      entity = applEman.merge(entity);
      applEman.refresh(entity);
      // nativeEntityManager.flush(); added in CibetEntityManager.merge because of OpenJPA
      // OpenJPA:
      // Assert.assertEquals(12, entity.getCounter());
      // Hibernate:
      // Assert.assertEquals(5, entity.getCounter());

      log.info("dc delegate = " + applEman.getDelegate());
      if (applEman.getDelegate() != null) {
         log.info(applEman.getDelegate().getClass().getName());
      } else {
         log.info("dc delegate is null");
      }
   }

   /**
    * update after persist on same object in no control mode leads to direct update in database.
    */
   @Test
   public void persistUpdateNoControl() {
      log.info("start persistUpdateNoControl()");
      TEntity entity = persistTEntity();
      entity.setCounter(13);
      applEman.merge(entity);
      applEman.flush();
      applEman.clear();

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(13, selEnt.getCounter());
   }

   /**
    * update after persist on same object in archive mode leads to direct update in database and create of 2 archive
    * records.
    * 
    * @throws InterruptedException
    */
   @Test
   public void persistUpdateArchive() throws InterruptedException {
      log.info("start persistUpdateArchive()");
      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TEntity entity = persistTEntity();
      Thread.sleep(20);
      entity.setCounter(13);
      applEman.merge(entity);
      applEman.flush();
      applEman.clear();

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(13, selEnt.getCounter());

      Context.end();
      Context.start();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_BY_PRIMARYKEYID);
      q.setParameter("targetType", TEntity.class.getName());
      q.setParameter("primaryKeyId", String.valueOf(entity.getId()));
      List<Archive> list = q.getResultList();
      Assert.assertEquals(2, list.size());
      JpaResource res = (JpaResource) list.get(0).getResource();
      JpaResource res1 = (JpaResource) list.get(1).getResource();

      Assert.assertEquals(TEntity.class.getName(), res.getTargetType());
      Assert.assertEquals(5, ((TEntity) res.getObject()).getCounter());
      Assert.assertEquals(13, ((TEntity) res1.getObject()).getCounter());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.UPDATE, list.get(1).getControlEvent());
   }

   /**
    * update after persist on same object in 4-eyes mode leads to UnapprovedEntityException because object is not yet
    * persistent.
    * 
    * @throws InterruptedException
    */
   @Test
   public void persistUpdateWith4Eyes() throws InterruptedException {
      log.info("start persistUpWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      TEntity entity = persistTEntity();
      Thread.sleep(10);
      entity.setCounter(13);
      try {
         applEman.merge(entity);
         Assert.fail();
      } catch (UnapprovedResourceException e) {
      }

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      JpaResource res = (JpaResource) list.get(0).getResource();
      Assert.assertEquals("0", res.getPrimaryKeyId());

      Assert.assertTrue(list.get(1).getRemark().startsWith("An unreleased Dual Control business case with ID "));
      Assert.assertEquals(ExecutionStatus.ERROR, list.get(1).getExecutionStatus());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(0).getControlEvent());
   }

   /**
    * 2 consecutive calls to persist on the same object leads to: only one instance in database, second call is
    * disregarded
    */
   @Test
   public void persistPersistNoControl() {
      log.info("start persistPersistNoControl()");
      TEntity entity = persistTEntityWithoutClear();

      applEman.persist(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> list = q.getResultList();
      Assert.assertEquals(1, list.size());
   }

   /**
    * 2 consecutive calls to persist on the same object leads to: only one instance in database, second call is
    * disregarded
    */
   @Test
   public void persistPersistArchive() {
      log.info("start persistPersistArchive()");

      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);
      TEntity entity = persistTEntityWithoutClear();

      applEman.persist(entity);

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> list = q.getResultList();
      Assert.assertEquals(1, list.size());

      List<Archive> list1 = ArchiveLoader.loadArchives();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
   }

   /**
    * 2 consecutive calls to persist on the same object leads to: with 4-eyes: two instances in Controllable and ARCHIVE
    */
   @Test
   public void persistPersistWith4Eyes() {
      log.info("start persistPersistWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      TEntity entity = persistTEntityWithoutClear();

      entity.setCounter(13);
      applEman.persist(entity);

      List<Archive> list = ArchiveLoader.loadArchives();
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertEquals("0", res.getPrimaryKeyId());
      ar = list.get(1);
      res = (JpaResource) ar.getResource();
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertEquals("0", res.getPrimaryKeyId());

      List<Controllable> list1 = DcLoader.findUnreleased(TEntity.class.getName());
      Assert.assertEquals(2, list1.size());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(1).getControlEvent());
   }

   /**
    * leads to direct removal of object in database.
    */
   @Test
   public void persistRemoveNoControl() {
      log.info("start persistRemoveNoControl()");

      TEntity entity = persistTEntity();
      applEman.remove(entity);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull("entity with id " + entity.getId() + " not deleted", selEnt);
   }

   /**
    * removal after persist on same object in archive mode leads to direct removal in database and create of archive
    * record.
    * 
    * @throws InterruptedException
    */
   @Test
   public void persistRemoveArchive() throws InterruptedException {
      log.info("start persistRemoveArchive()");

      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TEntity entity = persistTEntity();
      Thread.sleep(20);
      applEman.remove(entity);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull("entity with id " + entity.getId() + " not deleted", selEnt);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_BY_PRIMARYKEYID);
      q.setParameter("targetType", TEntity.class.getName());
      q.setParameter("primaryKeyId", String.valueOf(entity.getId()));
      List<Archive> list = q.getResultList();
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.DELETE, list.get(1).getControlEvent());
      Assert.assertEquals(TEntity.class.getName(), list.get(0).getResource().getTargetType());
   }

   /**
    * remove after persist on the same object in 4-eyes mode: throws UnapprovedEntityException because object is not yet
    * persistent.
    * 
    * @throws InterruptedException
    */
   @Test
   public void persistRemoveWith4Eyes() throws InterruptedException {
      log.info("start persistRemoveWith4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);
      TEntity entity = persistTEntity();

      Thread.sleep(10);
      entity.setCounter(13);
      try {
         applEman.remove(entity);
         Assert.fail();
      } catch (UnapprovedResourceException e) {
      }

      List<Archive> list = ArchiveLoader.loadArchives();
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertEquals("0", res.getPrimaryKeyId());

      Assert.assertTrue(list.get(1).getRemark().startsWith("An unreleased Dual Control business case with ID "));
      Assert.assertEquals(ExecutionStatus.ERROR, list.get(1).getExecutionStatus());

      List<Controllable> list1 = DcLoader.findUnreleased(TEntity.class.getName());
      Assert.assertEquals(1, list1.size());
      Assert.assertEquals(ControlEvent.INSERT, list1.get(0).getControlEvent());
   }

   @Test
   public void mergeFindCheckAssociations() {
      log.info("start mergeFindCheckAssociations()");

      sp = registerSetpoint(TComplexEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_BY_PRIMARYKEYID);
      q.setParameter("targetType", TComplexEntity.class.getName());
      q.setParameter("primaryKeyId", String.valueOf(ce.getId()));
      List<Archive> list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(TComplexEntity.class.getName(), res.getTargetType());
      TComplexEntity decObj = (TComplexEntity) res.getObject();
      Assert.assertEquals(2, decObj.getEagerList().size());
      Assert.assertEquals(3, decObj.getLazyList().size());
      Assert.assertNotNull(decObj.getTen());

      checkEntity(decObj);

      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      checkEntity(selEnt);

      selEnt.setCompValue(13);
      selEnt.getEagerList().clear();
      applEman.merge(selEnt);
      applEman.flush();
      // applEman.clear();

      Context.end();
      Context.start();

      q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_BY_PRIMARYKEYID);
      q.setParameter("targetType", TComplexEntity.class.getName());
      q.setParameter("primaryKeyId", String.valueOf(ce.getId()));
      list = q.getResultList();
      Assert.assertEquals(2, list.size());
      Archive ar1 = list.get(0);
      Archive ar2 = list.get(1);
      JpaResource res1 = (JpaResource) ar1.getResource();
      JpaResource res2 = (JpaResource) ar2.getResource();

      TComplexEntity decObj1 = (TComplexEntity) res1.getObject();
      TComplexEntity decObj2 = (TComplexEntity) res2.getObject();
      Assert.assertEquals(2, decObj1.getEagerList().size());
      Assert.assertEquals(3, decObj1.getLazyList().size());
      Assert.assertNotNull(decObj1.getTen());
      Assert.assertEquals(12, decObj1.getCompValue());
      Assert.assertEquals(0, decObj2.getEagerList().size());
      Assert.assertEquals(3, decObj2.getLazyList().size());
      Assert.assertNotNull(decObj2.getTen());
      Assert.assertEquals(13, decObj2.getCompValue());

      checkEntity(decObj1);
      // checkEntity(decObj2);

      selEnt = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertEquals(0, selEnt.getEagerList().size());
      Assert.assertEquals(13, selEnt.getCompValue());

      selEnt.setCompValue(122);
      applEman.merge(selEnt);

      list = ArchiveLoader.loadArchivesByPrimaryKeyId(ce.getClass().getName(), String.valueOf(ce.getId()));
      Assert.assertEquals(3, list.size());
      Archive ar3 = list.get(2);
      res = (JpaResource) ar3.getResource();

      TComplexEntity decObj3 = (TComplexEntity) res.getObject();
      Assert.assertEquals(122, decObj3.getCompValue());
      Assert.assertEquals(0, decObj3.getEagerList().size());
   }

   @Test
   public void mergeAndQuery() {
      log.info("start mergeAndQuery()");

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      Query q = applEman.createQuery("SELECT a FROM TEntity a");
      List<TEntity> list = q.getResultList();
      Assert.assertEquals(1, list.size());
      TEntity selEnt = list.get(0);

      selEnt.setCounter(12);
      applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();

      selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
   }

   @Test
   public void mergeAndFind() {
      log.info("start mergeAndFind()");
      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);

      selEnt.setCounter(12);
      applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();

      selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());
   }

   @Test
   public void mergeAndFindNoControl() {
      log.info("start mergeAndFindNoControl()");

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.flush();
      applEman.clear();

      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull("entity with id " + ce.getId() + " not found", selEnt);
      Assert.assertEquals(12, ce.getCompValue());

      selEnt.setCompValue(13);
      applEman.merge(selEnt);
      applEman.flush();
      applEman.clear();

      selEnt = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull("entity with id " + ce.getId() + " not found", selEnt);
      Assert.assertEquals(13, selEnt.getCompValue());
      Assert.assertEquals(2, selEnt.getEagerList().size());
      Assert.assertEquals(3, selEnt.getLazyList().size());
      Assert.assertNotNull(selEnt.getTen());
   }

   @Test
   public void mergeQueryCheckAssociations() {
      log.info("start assoc4Eyes()");

      TComplexEntity ce = createTComplexEntity();
      applEman.persist(ce);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      ((CibetEntityManager) applEman).setLoadEager(true);
      TComplexEntity selEnt = applEman.find(TComplexEntity.class, ce.getId());
      ((CibetEntityManager) applEman).setLoadEager(false);

      TEntity e5 = new TEntity("val8", 8, "newOwner");
      selEnt.getEagerList().add(e5);
      selEnt.setCompValue(14);
      applEman.merge(selEnt);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      Context.end();
      Context.start();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Controllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Controllable dcOb = (Controllable) q.getSingleResult();
      Assert.assertNotNull(dcOb);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
      JpaResource res1 = (JpaResource) dcOb.getResource();
      selEnt = (TComplexEntity) res1.getObject();
      Assert.assertEquals(3, selEnt.getEagerList().size());
      Assert.assertEquals(3, selEnt.getLazyList().size());
      Assert.assertNotNull(selEnt.getTen());
      Assert.assertEquals(14, selEnt.getCompValue());
   }

   @Test
   public void selectWithArchive() {
      log.info("start selectWithArchive()");
      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.SELECT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      TEntity entity = persistTEntity();

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.SELECT, ar.getControlEvent());
      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(TEntity.class.getName(), res.getTargetType());
      Assert.assertEquals(TEntity.class, res.getObject());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
      Assert.assertEquals(entity.getId(), res.getPrimaryKeyObject());
   }

   @Test
   public void persistWithArchiveAddProperty() throws InterruptedException {
      log.info("start persistWithArchiveAddProperty()");
      ArchiveActuator act = new ArchiveActuator("propadder");
      act.getStoredProperties().add("counter");
      Configuration.instance().registerActuator(act);

      sp = registerSetpoint(TEntity.class.getName(), "propadder", ControlEvent.ALL);

      TEntity entity = persistTEntity();
      Thread.sleep(10);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      Map<String, Object> params = new HashMap<>();
      params.put("counter", 5);

      // applEman.getTransaction().commit();
      // applEman.getTransaction().begin();

      List<Archive> listx = ArchiveLoader.loadArchivesByProperties(TEntity.class, params);
      Assert.assertEquals(2, listx.size());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(TEntity.class.getName(), res.getTargetType());
      TEntity en = (TEntity) res.getObject();
      Assert.assertTrue("expected: " + entity.getId() + ", actual: " + en.getId(), en.getId() == entity.getId());
   }

   @Test
   public void persistWithArchiveAddProperties() {
      log.info("start persistWithArchiveAddProperties()");
      ArchiveActuator act = new ArchiveActuator("propadd2");
      act.getStoredProperties().add("counter");
      act.getStoredProperties().add("nameValue");
      act.getStoredProperties().add("xtimestamp");
      act.setEncrypt(true);
      Configuration.instance().registerActuator(act);

      sp = registerSetpoint(TEntity.class.getName(), "propadd2", ControlEvent.ALL);

      Date now = new Date();

      TEntity entity1 = persistTEntity();
      TEntity entity2 = persistTEntity(12, now, null);
      log.debug("Date:: " + entity2.getXtimestamp());
      TEntity selEnt = applEman.find(TEntity.class, entity2.getId());
      Assert.assertNotNull(selEnt);
      log.debug("Date:: " + selEnt.getXtimestamp());
      Assert.assertEquals(12, selEnt.getCounter());

      Map<String, Object> params = new HashMap<>();
      params.put("counter", 12);
      params.put("xtimestamp", now);

      // Context.end();
      // Context.start();
      // Context.sessionScope().setTenant(TENANT);

      List<Archive> listx = ArchiveLoader.loadArchivesByProperties(TEntity.class, params);
      // persist and find
      if (JBOSS.equals(APPSERVER)) {
         // different xtimestamp format : 2017-03-25 19:01:24.666 and Sat Mar 25 19:01:24 CET 2017
         Assert.assertEquals(1, listx.size());
      } else {
         Assert.assertEquals(2, listx.size());
      }
   }

   @Test
   public void persistWith4EyesAddProperty() {
      log.info("start persistWith4EyesAddProperty()");
      FourEyesActuator act = new FourEyesActuator("propadder");
      act.getStoredProperties().add("counter");
      Configuration.instance().registerActuator(act);

      sp = registerSetpoint(TEntity.class.getName(), "propadder", ControlEvent.ALL);

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      Map<String, Object> params = new HashMap<>();
      params.put("counter", 5);

      // applEman.getTransaction().commit();
      // applEman.getTransaction().begin();

      List<Controllable> listx = DcLoader.loadByProperties(TEntity.class, params);
      Assert.assertEquals(1, listx.size());
   }

   @Test
   public void persistWith4EyesAddProperties() {
      log.info("start persistWith4EyesAddProperties()");
      FourEyesActuator act = new FourEyesActuator("propadd2");
      act.getStoredProperties().add("counter");
      act.getStoredProperties().add("nameValue");
      act.getStoredProperties().add("xtimestamp");
      act.setEncrypt(true);
      Configuration.instance().registerActuator(act);

      sp = registerSetpoint(TEntity.class.getName(), "propadd2", ControlEvent.ALL);

      Date now = new Date();

      persistTEntity();
      TEntity entity2 = persistTEntity(12, now, null);
      TEntity selEnt = applEman.find(TEntity.class, entity2.getId());
      Assert.assertNull(selEnt);

      Map<String, Object> params = new HashMap<>();
      params.put("counter", 12);
      params.put("xtimestamp", now);

      // applEman.getTransaction().commit();
      // applEman.getTransaction().begin();

      List<Controllable> listx = DcLoader.loadByProperties(TEntity.class, params);
      Assert.assertEquals(1, listx.size());

      listx = DcLoader.loadAllByProperties(TEntity.class, params);
      Assert.assertEquals(1, listx.size());
   }

   @Test
   public void persistWithException() {
      log.info("start persistWithException()");
      TEntity entity = persistTEntityWithoutClear();

      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      applEman.clear();
      try {
         applEman.persist(entity);
         Assert.fail();
      } catch (PersistenceException e) {
         log.info(e.getMessage());
      }

      List<Archive> list1 = ArchiveLoader.loadArchives();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
      Assert.assertEquals(ExecutionStatus.ERROR, ar.getExecutionStatus());
   }

   @Test
   public void mergeWithException() {
      log.info("start mergeWithException()");
      TComplexEntity entity = new TComplexEntity();
      entity.setOwner("oike");
      applEman.persist(entity);
      applEman.flush();

      sp = registerSetpoint(TComplexEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      applEman.clear();
      entity.setOwner("-5");
      entity.setVersion(5);
      try {
         applEman.merge(entity);
         Assert.fail();
      } catch (Exception e) {
         log.info(e.getMessage());
      }

      List<Archive> list1 = ArchiveLoader.loadArchives();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(ControlEvent.UPDATE, ar.getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
      Assert.assertEquals(ExecutionStatus.ERROR, ar.getExecutionStatus());
   }

   /**
    * EclipseLink dosnt check optimisticLock on remove like Hibernate: javax.persistence.OptimisticLockException: Row
    * was updated or deleted by another transaction (or unsaved-value mapping was incorrect):
    * [com.cibethelper.entities.TComplexEntity#165]
    * 
    */
   @Test
   public void removeWithException() {
      Assume.assumeFalse(GLASSFISH.equals(APPSERVER));
      log.info("start removeWithException()");
      TComplexEntity entity = new TComplexEntity();
      entity.setOwner("oike");
      applEman.persist(entity);
      applEman.flush();

      sp = registerSetpoint(TComplexEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT,
            ControlEvent.UPDATE, ControlEvent.DELETE);

      applEman.clear();
      entity.setVersion(5);
      try {
         applEman.remove(entity);
         Assert.fail();
      } catch (Exception e) {
         log.info(e.getMessage());
      }

      List<Archive> list1 = ArchiveLoader.loadArchives();
      Assert.assertEquals(1, list1.size());
      Archive ar = list1.get(0);
      JpaResource res = (JpaResource) ar.getResource();
      Assert.assertEquals(ControlEvent.DELETE, ar.getControlEvent());
      Assert.assertEquals(String.valueOf(entity.getId()), res.getPrimaryKeyId());
      Assert.assertEquals(ExecutionStatus.ERROR, ar.getExecutionStatus());
   }

   @Test
   public void selectWithException() {
      log.info("start selectWithException()");
      sp = registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.PERSIST);

      try {
         // int instead of long
         // OpenJPA makes no difference between int and long
         applEman.find(TEntity.class, -5);
         if (!TOMEE.equals(APPSERVER)) {
            Assert.fail();
         }
      } catch (IllegalArgumentException e) {
      }

      Context.internalRequestScope().getEntityManager().clear();
      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TEntity.class.getName(), "-5");
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.SELECT, ar.getControlEvent());
      if (!TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(ExecutionStatus.ERROR, ar.getExecutionStatus());
      } else {
         Assert.assertEquals(ExecutionStatus.EXECUTED, ar.getExecutionStatus());
      }

      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(TEntity.class.getName(), res.getTargetType());
      Assert.assertEquals(TEntity.class, res.getObject());
      log.debug("res.getPrimaryKeyObject():" + res.getPrimaryKeyObject().getClass().getName());
      Assert.assertEquals(-5L, res.getPrimaryKeyObject());
   }

}
