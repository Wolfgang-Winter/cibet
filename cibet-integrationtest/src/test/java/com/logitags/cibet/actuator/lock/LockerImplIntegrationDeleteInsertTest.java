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

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveService;
import com.logitags.cibet.actuator.archive.DefaultArchiveService;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcService;
import com.logitags.cibet.actuator.dc.DefaultDcService;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.helper.TEntity;
import com.logitags.cibet.helper.env.AbstractTestIntegration;
import com.logitags.cibet.helper.env.Database;

public class LockerImplIntegrationDeleteInsertTest extends AbstractTestIntegration {

   private static Logger log = Logger.getLogger(LockerImplIntegrationDeleteInsertTest.class);

   public LockerImplIntegrationDeleteInsertTest(Database db) {
      super(db);
   }

   @Test
   public void lockObject() throws AlreadyLockedException {
      log.info("start lockObject()");

      TEntity te = persistTEntity();

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(te, ControlEvent.DELETE, "testremark");
      Assert.assertNotNull(lo);

      List<LockedObject> l2 = locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
      Assert.assertEquals("testremark", l2.get(0).getLockRemark());
      Assert.assertEquals(USER, l2.get(0).getLockedBy());
      Assert.assertEquals(TEntity.class.getName(), l2.get(0).getTargetType());
   }

   @Test
   public void lockObjectLockedSameUser() throws AlreadyLockedException {
      log.info("start lockObjectLockedSameUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE);

      TEntity te = persistTEntity();
      Query q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(te, ControlEvent.DELETE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      cibetEman.remove(te);
      Assert.assertEquals(ExecutionStatus.EXECUTED, Context.requestScope().getExecutedEventResult()
            .getExecutionStatus());

      ArchiveService archiveManager = new DefaultArchiveService();
      List<Archive> list = archiveManager.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      log.debug("ARCHIVE: " + list.get(0));
      Assert.assertEquals(ControlEvent.DELETE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertNull(list.get(0).getRemark());
      Assert.assertNotNull(list.get(0).getResource().getTarget());

      List<LockedObject> l2 = locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
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

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(te, ControlEvent.DELETE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      cibetEman.remove(te);
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ArchiveService archiveManager = new DefaultArchiveService();
      List<Archive> list = archiveManager.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<LockedObject> l2 = locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Query q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
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

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(TEntity.class, ControlEvent.DELETE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      cibetEman.remove(te);
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ArchiveService archiveManager = new DefaultArchiveService();
      List<Archive> list = archiveManager.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<LockedObject> l2 = locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Query q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());
   }

   @Test
   public void lockObjectInsert() throws AlreadyLockedException {
      log.info("start lockObjectInsert()");

      TEntity te = persistTEntity();

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(te, ControlEvent.INSERT, "testremark");
      Assert.assertNotNull(lo);

      List<LockedObject> l2 = locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
      Assert.assertEquals("testremark", l2.get(0).getLockRemark());
      Assert.assertEquals(USER, l2.get(0).getLockedBy());
      Assert.assertEquals(TEntity.class.getName(), l2.get(0).getTargetType());
   }

   @Test
   public void lockObjectLockedSameUserInsert() throws AlreadyLockedException {
      log.info("start lockObjectLockedSameUserInsert()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity te = persistTEntity();
      Query q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(te, ControlEvent.INSERT, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      TEntity te2 = persistTEntity();
      Assert.assertEquals(ExecutionStatus.EXECUTED, Context.requestScope().getExecutedEventResult()
            .getExecutionStatus());

      ArchiveService archiveManager = new DefaultArchiveService();
      List<Archive> list = archiveManager.loadArchives(TEntity.class.getName());
      Assert.assertEquals(2, list.size());
      log.debug("ARCHIVE: " + list.get(0));
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertNull(list.get(0).getRemark());
      Assert.assertNotNull(list.get(0).getResource().getTarget());

      List<LockedObject> l2 = locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
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

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(te, ControlEvent.INSERT, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      TEntity te2 = persistTEntity();
      Assert.assertEquals(ExecutionStatus.EXECUTED, Context.requestScope().getExecutedEventResult()
            .getExecutionStatus());

      ArchiveService archiveManager = new DefaultArchiveService();
      List<Archive> list = archiveManager.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());

      List<LockedObject> l2 = locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Query q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
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

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(TEntity.class, ControlEvent.INSERT, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      TEntity te2 = persistTEntity();
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      ArchiveService archiveManager = new DefaultArchiveService();
      List<Archive> list = archiveManager.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<LockedObject> l2 = locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));

      Query q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
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
      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(TEntity.class, ControlEvent.RELEASE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);
      Context.sessionScope().setUser(USER);

      TEntity te = persistTEntity();
      DcService dcman = new DefaultDcService();
      List<DcControllable> unr = dcman.findUnreleased();
      Assert.assertEquals(1, unr.size());

      Query q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user2");
      dcman.release(cibetEman, unr.get(0), "first try");
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = dcman.findUnreleased();
      Assert.assertEquals(1, unr.size());

      res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      dcman.release(cibetEman, unr.get(0), "first try");
      Assert.assertEquals(ExecutionStatus.EXECUTED, Context.requestScope().getExecutedEventResult()
            .getExecutionStatus());

      unr = dcman.findUnreleased();
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
      DcService dcman = new DefaultDcService();
      List<DcControllable> unr = dcman.findUnreleased();
      Assert.assertEquals(1, unr.size());

      Query q = cibetEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(te, ControlEvent.RELEASE, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      log.debug("user2");
      Context.sessionScope().setUser("user2");
      dcman.release(cibetEman, unr.get(0), "first try");
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = dcman.findUnreleased();
      Assert.assertEquals(1, unr.size());

      res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      dcman.release(cibetEman, unr.get(0), "first try");
      Assert.assertEquals(ExecutionStatus.EXECUTED, Context.requestScope().getExecutedEventResult()
            .getExecutionStatus());

      unr = dcman.findUnreleased();
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

      Locker locker = new DefaultLocker();
      LockedObject lo = locker.lock(te, ControlEvent.SELECT, "testremark");
      Context.requestScope().getEntityManager().flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");

      TEntity te2 = cibetEman.find(TEntity.class, te.getId());
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertNull(te2);

      ArchiveService archiveManager = new DefaultArchiveService();
      List<Archive> list = archiveManager.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());
   }

}
