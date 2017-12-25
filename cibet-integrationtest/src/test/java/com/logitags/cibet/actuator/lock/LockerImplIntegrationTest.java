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
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.sensor.jpa.JpaResource;

public class LockerImplIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(LockerImplIntegrationTest.class);

   @After
   public void subDoAfter() {
      log.info("subDoAfter");
      new ConfigurationService().initialise();
   }

   @Test
   public void beforeEventDenied() throws AlreadyLockedException {
      LockActuator act = new LockActuator("other");
      EventMetadata md = new EventMetadata(ControlEvent.DELETE, null);
      md.setExecutionStatus(ExecutionStatus.DENIED);
      act.beforeEvent(md);
   }

   @Test
   public void lockObject() throws AlreadyLockedException {
      log.info("start lockObject()");

      TEntity te = persistTEntity();

      Controllable lo = Locker.lock(te, ControlEvent.UPDATE, "testremark");
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
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE);

      TEntity te = persistTEntity();
      Controllable lo = Locker.lock(te, ControlEvent.UPDATE, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      te.setCounter(190);
      te = applEman.merge(te);

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("LOCKER, ARCHIVE", er.getActuators());

      Assert.assertEquals(190, te.getCounter());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      log.debug("ARCHIVE: " + list.get(0));
      Assert.assertEquals(ControlEvent.UPDATE, list.get(0).getControlEvent());
      Assert.assertEquals(USER, list.get(0).getCreateUser());
      Assert.assertNull(list.get(0).getRemark());
      Assert.assertNotNull(list.get(0).getResource().getTargetObject());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
   }

   @Test
   public void lockObjectLockedOtherUser() throws AlreadyLockedException {
      log.info("start lockObjectLockedOtherUser()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(te, ControlEvent.UPDATE, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      te.setCounter(190);
      te = applEman.merge(te);

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.DENIED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("LOCKER, ARCHIVE", er.getActuators());
      log.debug("EventResult=" + er);

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
   }

   @Test(expected = AlreadyLockedException.class)
   public void lockObjectLocked2Times() throws AlreadyLockedException {
      log.info("start lockObjectLocked2Times()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(te, ControlEvent.PERSIST, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      Locker.lock(te, ControlEvent.DELETE, "testremark2");
   }

   @Test
   public void lockObjectLocked2Times2() throws AlreadyLockedException {
      log.info("start lockObjectLocked2Times2()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(te, ControlEvent.DELETE, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      try {
         Context.sessionScope().setUser("otherUser");
         Locker.lock(te, ControlEvent.ALL, "testremark2");
         Assert.fail();
      } catch (AlreadyLockedException e) {
         Assert.assertNotNull(e.getLockedControl());
         Assert.assertEquals(USER, e.getLockedControl().getCreateUser());
      }
   }

   @Test(expected = AlreadyLockedException.class)
   public void lockClassLocked2Times() throws AlreadyLockedException {
      log.info("start lockClassLocked2Times()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(te, ControlEvent.PERSIST, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      Locker.lock(TEntity.class, ControlEvent.DELETE, "testremark2");
   }

   @Test(expected = AlreadyLockedException.class)
   public void lockClassLocked2Times2() throws AlreadyLockedException {
      log.info("start lockClassLocked2Times2()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Locker.lock(TEntity.class, ControlEvent.DELETE, "testremark2");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();

      Locker.lock(te, ControlEvent.PERSIST, "testremark");
   }

   @Test
   public void lockClassLockedOtherUser() throws AlreadyLockedException {
      log.info("start lockClassLockedOtherUser()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(TEntity.class, ControlEvent.UPDATE, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      te.setCounter(190);
      te = applEman.merge(te);
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug("EventResult=" + ev);
      Assert.assertEquals(ExecutionStatus.DENIED, ev.getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
   }

   @Test
   public void lockClassRemoveLock() throws AlreadyLockedException {
      log.info("start lockClassRemoveLock()");

      TEntity te = persistTEntity();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(TEntity.class, ControlEvent.UPDATE, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      te.setCounter(190);
      te = applEman.merge(te);
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug("EventResult=" + ev);
      Assert.assertEquals(ExecutionStatus.DENIED, ev.getExecutionStatus());

      Context.internalRequestScope().getOrCreateEntityManager(false).getTransaction().commit();
      Context.internalRequestScope().getOrCreateEntityManager(false).getTransaction().begin();

      // other user tries to remove strict lock
      try {
         Locker.unlockStrict(lo, null);
         Assert.fail();
      } catch (DeniedException e) {
      }
      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());

      // same user removes lock
      Context.sessionScope().setUser(USER);
      Locker.unlockStrict(lo, null);

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      l2 = Locker.loadLockedObjects();
      Assert.assertEquals(0, l2.size());

      te = applEman.merge(te);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void lockClassRemoveLock2() throws Exception {
      log.info("start lockClassRemoveLock2()");

      TEntity te = persistTEntity();

      LockActuator lact = (LockActuator) Configuration.instance().getActuator(LockActuator.DEFAULTNAME);
      lact.setThrowDeniedException(true);
      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(TEntity.class, ControlEvent.PERSIST, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);
      log.debug(lo);

      Context.sessionScope().setUser("otherUser");
      te.setCounter(190);
      try {
         te = applEman.merge(te);
         Assert.fail();
      } catch (DeniedException e) {
         EventResult ev = Context.requestScope().getExecutedEventResult();
         log.debug("EventResult=" + ev);
         Assert.assertEquals(ExecutionStatus.DENIED, ev.getExecutionStatus());
      }

      // other user tries to remove strict lock
      try {
         Locker.unlockStrict(lo, null);
         Assert.fail();
      } catch (DeniedException e) {
      }
      List<Controllable> l2 = Locker.loadLockedObjectsByUser(USER);
      Assert.assertEquals(1, l2.size());

      // same user removes lock
      Context.sessionScope().setUser(USER);
      Locker.unlockStrict(lo, null);

      l2 = Locker.loadLockedObjects();
      Assert.assertEquals(0, l2.size());

      te = applEman.merge(te);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
   }

   @Test
   public void lockClassAutomaticRemove() throws AlreadyLockedException {
      log.info("start lockClassAutomaticRemove()");

      TEntity te = persistTEntity();

      Configuration cman = Configuration.instance();
      LockActuator la = (LockActuator) cman.getActuator(LockActuator.DEFAULTNAME);
      la.setAutomaticUnlock(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(TEntity.class, ControlEvent.UPDATE, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());

      te.setCounter(190);
      te = applEman.merge(te);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());

      l2 = Locker.loadLockedObjects();
      Assert.assertEquals(0, l2.size());

      Context.sessionScope().setUser("other");
      te.setCounter(1290);
      te = applEman.merge(te);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
   }

   @Test
   public void lockClassAutomaticUnlock() throws AlreadyLockedException {
      log.info("start lockClassAutomaticUnlock()");

      TEntity te = persistTEntity();

      Configuration cman = Configuration.instance();
      LockActuator la = (LockActuator) cman.getActuator(LockActuator.DEFAULTNAME);
      la.setAutomaticUnlock(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      Controllable lo = Locker.lock(TEntity.class, ControlEvent.UPDATE, "testremark");
      Context.internalRequestScope().getOrCreateEntityManager(true).flush();
      Assert.assertNotNull(lo);

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      Assert.assertEquals(ExecutionStatus.LOCKED, l2.get(0).getExecutionStatus());

      te.setCounter(190);
      te = applEman.merge(te);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());

      Query q = Context.internalRequestScope().getOrCreateEntityManager(false)
            .createQuery("select a from Controllable a");
      l2 = q.getResultList();
      Assert.assertEquals(1, l2.size());
      Assert.assertEquals(ExecutionStatus.UNLOCKED, l2.get(0).getExecutionStatus());

      Context.sessionScope().setUser("other");
      te.setCounter(1290);
      te = applEman.merge(te);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
   }

}
