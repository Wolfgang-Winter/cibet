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
package com.logitags.cibet.actuator.lock;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.sensor.jpa.JpaResource;

public class LockerImplIntegrationDeleteInsertTest extends DBHelper {

   private static Logger log = Logger.getLogger(LockerImplIntegrationDeleteInsertTest.class);

   @After
   public void subDoAfter() {
      log.info("subDoAfter");
      new ConfigurationService().initialise();
   }

   @Test
   public void lockObject() throws AlreadyLockedException {
      log.info("start lockObject()");

      TEntity te = persistTEntity();

      Controllable lo = Locker.lock(te, ControlEvent.DELETE, "testremark");
      Assert.assertNotNull(lo);

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
      Assert.assertEquals("testremark", l2.get(0).getCreateRemark());
      Assert.assertEquals(USER, l2.get(0).getCreateUser());
      Assert.assertEquals(TEntity.class.getName(), ((JpaResource) l2.get(0).getResource()).getTarget());
   }

   @Test
   public void lockObjectLockedSameUser() throws AlreadyLockedException {
      log.info("start lockObjectLockedSameUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE);

      TEntity te = persistTEntity();
      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());

      Controllable lo = Locker.lock(te, ControlEvent.DELETE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      applEman.remove(te);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      log.debug("ARCHIVE: " + list.get(0));
      Assert.assertEquals(ControlEvent.DELETE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertNull(list.get(0).getRemark());
      Assert.assertNotNull(list.get(0).getResource().getTargetObject());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      res = q.getResultList();
      Assert.assertEquals(0, res.size());
   }

   @Test
   public void lockObjectLockedOtherUser() throws AlreadyLockedException {
      log.info("start lockObjectLockedOtherUser()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(te, ControlEvent.DELETE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      applEman.remove(te);
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());
   }

   @Test
   public void lockClassLockedOtherUser() throws AlreadyLockedException {
      log.info("start lockClassLockedOtherUser()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(TEntity.class, ControlEvent.DELETE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      applEman.remove(te);
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());
   }

   @Test
   public void lockObjectInsert() throws AlreadyLockedException {
      log.info("start lockObjectInsert()");

      TEntity te = persistTEntity();

      Controllable lo = Locker.lock(te, ControlEvent.INSERT, "testremark");
      Assert.assertNotNull(lo);

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
      Assert.assertEquals("testremark", l2.get(0).getCreateRemark());
      Assert.assertEquals(USER, l2.get(0).getCreateUser());
      Assert.assertEquals(TEntity.class.getName(), ((JpaResource) l2.get(0).getResource()).getTarget());
   }

   @Test
   public void lockObjectLockedSameUserInsert() throws AlreadyLockedException {
      log.info("start lockObjectLockedSameUserInsert()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity te = persistTEntity();
      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());

      Controllable lo = Locker.lock(te, ControlEvent.INSERT, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      TEntity te2 = persistTEntity();
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(2, list.size());
      log.debug("ARCHIVE: " + list.get(0));
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertNull(list.get(0).getRemark());
      Assert.assertNotNull(list.get(0).getResource().getTargetObject());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      res = q.getResultList();
      Assert.assertEquals(2, res.size());
   }

   @Test
   public void lockObjectLockedOtherUserInsert() throws AlreadyLockedException {
      log.info("start lockObjectLockedOtherUserInsert()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(te, ControlEvent.INSERT, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      TEntity te2 = persistTEntity();
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(2, res.size());
   }

   @Test
   public void lockClassLockedOtherUserInsert() throws AlreadyLockedException {
      log.info("start lockClassLockedOtherUserInsert()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(TEntity.class, ControlEvent.INSERT, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      TEntity te2 = persistTEntity();
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());
   }

   @Test
   public void lockClassDCLockedOtherUserInsert() throws Exception {
      log.info("start lockClassDCLockedOtherUserInsert()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST, ControlEvent.RELEASE);

      Context.sessionScope().setUser("user1");
      Controllable lo = Locker.lock(TEntity.class, ControlEvent.RELEASE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);
      Context.sessionScope().setUser(USER);

      TEntity te = persistTEntity();
      List<Controllable> unr = DcLoader.findUnreleased();
      Assert.assertEquals(1, unr.size());

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user2");
      unr.get(0).release(applEman, "first try");
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = DcLoader.findUnreleased();
      Assert.assertEquals(1, unr.size());

      res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      unr.get(0).release(applEman, "first try");
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = DcLoader.findUnreleased();
      Assert.assertEquals(0, unr.size());

      res = q.getResultList();
      Assert.assertEquals(1, res.size());
   }

   @Test
   public void lockObjectDCLockedOtherUserInsert() throws Exception {
      log.info("start lockObjectDCLockedOtherUserInsert()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE_INSERT);

      TEntity te = persistTEntity();
      List<Controllable> unr = DcLoader.findUnreleased();
      Assert.assertEquals(1, unr.size());

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      Controllable lo = Locker.lock(te, ControlEvent.RELEASE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      log.debug("user2");
      Context.sessionScope().setUser("user2");
      unr.get(0).release(applEman, "first try");
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = DcLoader.findUnreleased();
      Assert.assertEquals(1, unr.size());

      res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      unr.get(0).release(applEman, "first try");
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = DcLoader.findUnreleased();
      Assert.assertEquals(0, unr.size());

      res = q.getResultList();
      Assert.assertEquals(1, res.size());
   }

   @Test
   public void lockObjectSelectLockedOtherUser() throws AlreadyLockedException {
      log.info("start lockObjectSelectLockedOtherUser()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(te, ControlEvent.SELECT, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");

      TEntity te2 = applEman.find(TEntity.class, te.getId());
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertNull(te2);

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());
   }

}
