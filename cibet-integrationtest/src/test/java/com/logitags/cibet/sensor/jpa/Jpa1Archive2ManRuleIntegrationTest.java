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
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.TwoManRuleActuator;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * tests CibetEntityManager with Archive and FourEyes- actuators.
 * 
 * @author test
 * 
 */
public class Jpa1Archive2ManRuleIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(Jpa1Archive2ManRuleIntegrationTest.class);

   private Setpoint sp = null;

   @After
   public void afterJpa1Archive2ManRuleIntegrationTest() {
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(null, sp.getId());
      }
   }

   @Test
   public void persistWith2ManRule() {
      log.info("start persistWith2ManRule()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity entity = persistTEntity();

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("TWO_MAN_RULE, ARCHIVE", er.getActuators());

      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(TEntity.class.getName(),
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
   public void persistWith2ManRuleDirectRelease() {
      log.info("start persistWith2ManRuleDirectRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE_INSERT);

      Context.sessionScope().setSecondUser("secondUser");
      TEntity entity = persistTEntity();
      Context.sessionScope().setSecondUser(null);

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
      // Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(1, er.getChildResults().size());
      Assert.assertEquals(ControlEvent.RELEASE_INSERT, er.getChildResults().get(0).getEvent());
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getChildResults().get(0).getExecutionStatus());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("ARCHIVE, TWO_MAN_RULE", er.getActuators());

      log.debug(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertTrue(!res.getPrimaryKeyId().equals("0"));

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void persistWith2ManRuleDirectReleaseWithApprovalUser() {
      log.info("start persistWith2ManRuleDirectReleaseWithApprovalUser()");
      EventResult er1 = Context.requestScope().getExecutedEventResult();
      log.debug(er1);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE_INSERT);

      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setApprovalUser("secondUser");
      TEntity entity = persistTEntity();
      Context.sessionScope().setSecondUser(null);

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
      // Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(1, er.getChildResults().size());
      Assert.assertEquals(ControlEvent.RELEASE_INSERT, er.getChildResults().get(0).getEvent());
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getChildResults().get(0).getExecutionStatus());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("ARCHIVE, TWO_MAN_RULE", er.getActuators());

      log.debug(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertTrue(!res.getPrimaryKeyId().equals("0"));

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
      Context.sessionScope().setApprovalUser(null);
   }

   @Test
   public void persistWith2ManRuleDirectReleaseWithInvalidApprovalUser() {
      log.info("start persistWith2ManRuleDirectReleaseWithInvalidApprovalUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE_INSERT);

      Context.sessionScope().setApprovalUser("Fizzi");
      Context.sessionScope().setSecondUser("secondUser");
      TEntity entity = persistTEntity();
      Context.sessionScope().setSecondUser(null);

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());

      log.debug(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      log.debug("***: " + list1.get(0));
      Assert.assertEquals("Fizzi", list1.get(0).getReleaseUser());
      Assert.assertEquals(ExecutionStatus.POSTPONED, list1.get(0).getExecutionStatus());

      Context.sessionScope().setApprovalUser(null);
   }

   @Test(expected = InvalidUserException.class)
   public void persistWith2ManRuleLaterReleaseInvalidUser() throws Exception {
      log.info("start persistWith2ManRuleLaterReleaseInvalidUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      log.info("now release");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());
      dcOb.release(applEman, "2man rule test");
   }

   @Test(expected = InvalidUserException.class)
   public void persistWith2ManRuleLaterReleaseSameUser() throws Exception {
      log.info("start persistWith2ManRuleLaterReleaseSameUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());
      try {
         dcOb.release(applEman, "2man rule test");
      } finally {
         Context.sessionScope().setSecondUser(null);
      }
   }

   @Test(expected = InvalidUserException.class)
   public void persistWith2ManRuleLaterReleaseInvalidFirstUser() throws Exception {
      log.info("start persistWith2ManRuleLaterReleaseInvalidFirstUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      log.info("now release");
      Context.sessionScope().setSecondUser("yyyy");
      Context.sessionScope().setUser("xxxx");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());
      try {
         dcOb.release(applEman, "2man rule test");
      } finally {
         Context.sessionScope().setSecondUser(null);
      }
   }

   @Test
   public void persistWith2ManRuleLaterRelease() throws Exception {
      log.info("start persistWith2ManRuleLaterRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());
      TEntity selEnt2 = (TEntity) dcOb.release(Context.internalRequestScope().getApplicationEntityManager(),
            "2man rule test");
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Context.sessionScope().setSecondUser(null);

      TEntity selEnt3 = applEman.find(TEntity.class, selEnt2.getId());
      Assert.assertNotNull(selEnt3);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(selEnt3.getClass().getName(),
            String.valueOf(selEnt3.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      JpaResource res = (JpaResource) ar.getResource();

      Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
      Assert.assertTrue(!res.getPrimaryKeyId().equals("0"));

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void deleteWith2ManRule() {
      log.info("start deleteWith2ManRule()");
      Context.sessionScope().setSecondUser(null);

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE);

      TEntity entity = persistTEntity();
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      applEman.remove(selEnt);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.DELETE, ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());
   }

   @Test
   public void deleteWith2ManRuleDirectRelease() {
      log.info("start deleteWith2ManRuleDirectRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE_DELETE);
      TEntity entity = persistTEntity();

      Context.sessionScope().setSecondUser("secondUser");
      applEman.remove(entity);
      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      // Assert.assertEquals(ExecutionStatus.POSTPONED,
      // Context.requestScope().getExecutedEventResult().getExecutionStatus());

      log.debug(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.DELETE == ar.getControlEvent() || ControlEvent.RELEASE_DELETE == ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test(expected = InvalidUserException.class)
   public void deleteWith2ManRuleLaterReleaseInvalidUser() throws Exception {
      log.info("start deleteWith2ManRuleLaterReleaseInvalidUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();

      applEman.remove(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      log.info("now release");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());
      dcOb.release(applEman, "2man rule test");
   }

   @Test(expected = InvalidUserException.class)
   public void deleteWith2ManRuleLaterReleaseSameUser() throws Exception {
      log.info("start deleteWith2ManRuleLaterReleaseSameUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();

      applEman.remove(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());
      try {
         dcOb.release(applEman, "2man rule test");
      } finally {
         Context.sessionScope().setSecondUser(null);
      }
   }

   @Test(expected = InvalidUserException.class)
   public void deleteWith2ManRuleLaterReleaseInvalidFirstUser() throws Exception {
      log.info("start deleteWith2ManRuleLaterReleaseInvalidFirstUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();

      applEman.remove(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      log.info("now release");
      Context.sessionScope().setSecondUser("yyyy");
      Context.sessionScope().setUser("xxxx");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());
      try {
         dcOb.release(applEman, "2man rule test");
      } finally {
         Context.sessionScope().setSecondUser(null);
      }
   }

   @Test
   public void deleteWith2ManRuleLaterRelease() throws Exception {
      log.info("start deleteWith2ManRuleLaterRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      applEman.remove(entity);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.DELETE, dcOb.getControlEvent());
      TEntity selEnt2 = (TEntity) dcOb.release(Context.internalRequestScope().getApplicationEntityManager(),
            "2man rule test");
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Context.sessionScope().setSecondUser(null);

      TEntity selEnt3 = applEman.find(TEntity.class, selEnt2.getId());
      Assert.assertNull(selEnt3);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(selEnt.getClass().getName(),
            String.valueOf(selEnt.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.DELETE, ar.getControlEvent());

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void updateWith2ManRule() {
      log.info("start updateWith2ManRule()");
      Context.sessionScope().setSecondUser(null);

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE);

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      entity.setNameValue("newname");

      applEman.merge(entity);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertEquals("valuexx", selEnt.getNameValue());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
   }

   @Test
   public void updateWith2ManRuleDirectRelease() {
      log.info("start updateWith2ManRuleDirectRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE_UPDATE);

      Context.sessionScope().setSecondUser("secondUser");
      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();

      entity.setNameValue("newname");
      applEman.merge(entity);
      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      // Assert.assertEquals(ExecutionStatus.POSTPONED,
      // Context.requestScope().getExecutedEventResult().getExecutionStatus());

      log.debug(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertEquals("newname", selEnt.getNameValue());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void updateWith2ManRuleDirectReleaseInvalidUser() {
      log.info("start updateWith2ManRuleDirectReleaseInvalidUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE_UPDATE);

      Context.sessionScope().setSecondUser(USER);
      TEntity entity = persistTEntity();

      entity.setNameValue("newname");
      try {
         applEman.merge(entity);
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertTrue(e.getCause() instanceof InvalidUserException);
      }
      Context.sessionScope().setSecondUser(null);
   }

   @Test(expected = InvalidUserException.class)
   public void updateWith2ManRuleLaterReleaseInvalidUser() throws Exception {
      log.info("start updateWith2ManRuleLaterReleaseInvalidUser()");
      // if (Context.internalRequestScope().getApplicationEntityManager2() == null) {
      // Context.internalRequestScope().setApplicationEntityManager2(createEntityManager());
      // }

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      entity.setNameValue("newname");
      applEman.merge(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertEquals("valuexx", selEnt.getNameValue());

      log.info("now release");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
      dcOb.release(applEman, "2man rule test");
   }

   @Test(expected = InvalidUserException.class)
   public void updateWith2ManRuleLaterReleaseSameUser() throws Exception {
      log.info("start updateWith2ManRuleLaterReleaseSameUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      entity.setNameValue("newname");
      applEman.merge(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertEquals("valuexx", selEnt.getNameValue());

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
      try {
         dcOb.release(applEman, "2man rule test");
      } finally {
         Context.sessionScope().setSecondUser(null);
      }
   }

   @Test(expected = InvalidUserException.class)
   public void updateWith2ManRuleLaterReleaseSameUserAsCreateUser() throws Exception {
      log.info("start updateWith2ManRuleLaterReleaseSameUserAsCreateUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      entity.setNameValue("newname");
      applEman.merge(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertEquals("valuexx", selEnt.getNameValue());

      log.info("now release");
      Context.sessionScope().setSecondUser(USER);
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
      try {
         dcOb.release(applEman, "2man rule test");
      } finally {
         Context.sessionScope().setSecondUser(null);
      }
   }

   @Test(expected = InvalidUserException.class)
   public void updateWith2ManRuleLaterReleaseInvalidFirstUser() throws Exception {
      log.info("start updateWith2ManRuleLaterReleaseInvalidFirstUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      entity.setNameValue("newname");
      applEman.merge(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertEquals("valuexx", selEnt.getNameValue());

      log.info("now release");
      Context.sessionScope().setSecondUser("yyyy");
      Context.sessionScope().setUser("xxxx");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
      try {
         dcOb.release(applEman, "2man rule test");
      } finally {
         Context.sessionScope().setSecondUser(null);
      }
   }

   @Test
   public void updateWith2ManRuleLaterRelease() throws Exception {
      log.info("start updateWith2ManRuleLaterRelease()");

      TwoManRuleActuator ssa = new TwoManRuleActuator();
      ssa.setName("2Man");
      ssa.setRemoveSecondUserAfterRelease(true);
      Configuration.instance().registerActuator(ssa);

      Assert.assertTrue(ssa.isRemoveSecondUserAfterRelease());

      List<String> schemes = new ArrayList<String>();
      schemes.add(ssa.getName());
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE);

      TEntity entity = persistTEntity();
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      entity.setNameValue("newname");
      applEman.merge(entity);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertEquals("valuexx", selEnt.getNameValue());

      log.info("now release");
      Context.sessionScope().setSecondUser("secondUser");
      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      Controllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, dcOb.getControlEvent());
      TEntity selEnt2 = (TEntity) dcOb.release(Context.internalRequestScope().getApplicationEntityManager(),
            "2man rule test");
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertNull(Context.sessionScope().getSecondUser());

      TEntity selEnt3 = applEman.find(TEntity.class, selEnt2.getId());
      Assert.assertNotNull(selEnt3);
      Assert.assertEquals("newname", selEnt3.getNameValue());

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(selEnt3.getClass().getName(),
            String.valueOf(selEnt3.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.UPDATE, ar.getControlEvent());

      list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test(expected = DeniedException.class)
   public void persistWith2ManRuleDirectReleaseDenied() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseDenied()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(50);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp2 = registerSetpoint(TEntity.class.getName(), schemes2, ControlEvent.RELEASE);

      try {
         authenticate("Heinz");

         Context.sessionScope().setSecondUser("secondUser");
         TEntity entity = persistTEntity();
         Context.sessionScope().setSecondUser(null);
         Assert.assertEquals(ExecutionStatus.EXECUTED,
               Context.requestScope().getExecutedEventResult().getExecutionStatus());

         log.debug(entity);
         TEntity selEnt = applEman.find(TEntity.class, entity.getId());
         Assert.assertNotNull(selEnt);

         List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
               String.valueOf(entity.getId()));
         Assert.assertEquals(2, list.size());
         Archive ar = list.get(0);
         JpaResource res = (JpaResource) ar.getResource();

         Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());
         Assert.assertTrue(!res.getPrimaryKeyId().equals("0"));

         List<Controllable> list1 = DcLoader.findUnreleased();
         Assert.assertEquals(0, list1.size());
      } finally {
         Configuration.instance().unregisterSetpoint(null, sp2.getId());
      }
   }

   @Test
   public void playPersistWith2ManRuleDirectRelease() {
      log.info("start playPersistWith2ManRuleDirectRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE_INSERT);

      Context.sessionScope().setSecondUser("secondUser");
      Context.requestScope().startPlay();
      TEntity entity = persistTEntity();
      Context.sessionScope().setSecondUser(null);

      EventResult er = Context.requestScope().stopPlay();

      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
      // Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(1, er.getChildResults().size());
      Assert.assertEquals(ControlEvent.RELEASE_INSERT, er.getChildResults().get(0).getEvent());
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getChildResults().get(0).getExecutionStatus());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("ARCHIVE, TWO_MAN_RULE", er.getActuators());

      log.debug(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNull(selEnt);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(0, list.size());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void selectWith2ManRuleDirectRelease() {
      log.info("start selectWith2ManRuleDirectRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.SELECT, ControlEvent.RELEASE_SELECT);

      Context.sessionScope().setSecondUser("secondUser");
      TEntity entity = persistTEntity();

      TEntity entity2 = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(entity2);
      Context.sessionScope().setSecondUser(null);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      // Assert.assertEquals(ExecutionStatus.POSTPONED,
      // Context.requestScope().getExecutedEventResult().getExecutionStatus());

      log.debug(entity2);

      List<Archive> list = ArchiveLoader.loadArchivesByPrimaryKeyId(entity.getClass().getName(),
            String.valueOf(entity.getId()));
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertTrue(
            ControlEvent.SELECT == ar.getControlEvent() || ControlEvent.RELEASE_SELECT == ar.getControlEvent());

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

}
