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
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.TwoManRuleActuator;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * tests CibetEntityManager with Archive and FourEyes- actuators.
 * 
 * @author test
 * 
 */
public class Jpa1SpringSecurity2ManRuleIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(Jpa1SpringSecurity2ManRuleIntegrationTest.class);

   private Setpoint sp = null;

   @After
   public void afterJpa1SpringSecurity2ManRuleIntegrationTest() {
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   @Test
   public void persistWith2ManRuleDirectReleaseDenied1() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseDenied1()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes2, ControlEvent.RELEASE);

      authenticate("WILLI");

      try {
         persistTEntity();
         Assert.fail();
      } catch (DeniedException e) {
         log.debug(e.getMessage());
         log.debug(e.getDeniedUser());
         Assert.assertEquals("Access is denied", e.getMessage());
         Assert.assertEquals("USER", e.getDeniedUser());
      }
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug("EventResult=" + ev);
   }

   @Test
   public void persistWith2ManRuleDirectReleaseDenied2() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseDenied2()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes2, ControlEvent.RELEASE);

      authenticate("Heinz");

      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
      try {
         persistTEntity();
         Assert.fail();
      } catch (DeniedException e) {
         log.debug(e.getMessage());
         log.debug(e.getDeniedUser());
         Assert.assertEquals("No Authentication object found in CibetContext.getSecondPrincipal()", e.getMessage());
         Assert.assertEquals("secondUser", e.getDeniedUser());
      }
      Context.sessionScope().setSecondUser(null);
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug("EventResult=" + ev);
   }

   @Test
   public void persistWith2ManRuleDirectReleaseDenied2WrongSecondAuthType() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseDenied2WrongSecondAuthType()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes2, ControlEvent.RELEASE);

      authenticate("Heinz");

      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, schemes2);
      try {
         persistTEntity();
         Assert.fail();
      } catch (DeniedException e) {
         log.debug(e.getMessage());
         log.debug(e.getDeniedUser());
         Assert.assertEquals(
               "CibetContext.getSecondPrincipal() is expected to be of type org.springframework.security.core.Authentication but is of type java.util.ArrayList",
               e.getMessage());
         Assert.assertEquals("secondUser", e.getDeniedUser());
      }
      Context.sessionScope().setSecondUser(null);
      Context.sessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug("EventResult=" + ev);
   }

   @Test
   public void persistWith2ManRuleDirectReleaseDenied3() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseDenied3()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes2, ControlEvent.RELEASE);

      authenticate("Heinz");
      authenticateSecond("Haaa");
      Context.sessionScope().setSecondUser("secondUser");
      try {
         persistTEntity();
         Assert.fail();
      } catch (DeniedException e) {
         log.debug(e.getMessage());
         log.debug(e.getDeniedUser());
         Assert.assertEquals("Access is denied", e.getMessage());
         Assert.assertEquals("secondUser", e.getDeniedUser());
      }
      Context.sessionScope().setSecondUser(null);
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug("EventResult=" + ev);
   }

   @Test
   public void persistWith2ManRuleDirectReleaseGranted() throws Exception {
      log.info("start persistWith2ManRuleDirectReleaseGranted()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes2, ControlEvent.RELEASE);

      authenticate("Heinz");
      authenticateSecond("Wooo");
      Context.sessionScope().setSecondUser("secondUser");

      TEntity entity = persistTEntity();

      Context.sessionScope().setSecondUser(null);
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug("EventResult=" + ev);
      Assert.assertEquals(ExecutionStatus.POSTPONED, ev.getExecutionStatus());
      Assert.assertEquals(1, ev.getChildResults().size());
      Assert.assertEquals(ControlEvent.RELEASE_INSERT, ev.getChildResults().get(0).getEvent());
      Assert.assertEquals(ExecutionStatus.EXECUTED, ev.getChildResults().get(0).getExecutionStatus());

      log.debug(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull(selEnt);

      List<Controllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void playPersistWith2ManRuleDirectReleaseDenied2() throws Exception {
      log.info("start playPersistWith2ManRuleDirectReleaseDenied2()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes2, ControlEvent.RELEASE);

      authenticate("Heinz");

      Context.sessionScope().setSecondUser("secondUser");
      Context.sessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
      try {
         Context.requestScope().startPlay();
         persistTEntity();
         Assert.fail();
      } catch (DeniedException e) {
         log.debug(e.getMessage());
         log.debug(e.getDeniedUser());
         Assert.assertEquals("No Authentication object found in CibetContext.getSecondPrincipal()", e.getMessage());
         Assert.assertEquals("secondUser", e.getDeniedUser());
      }
      Context.sessionScope().setSecondUser(null);
      EventResult ev = Context.requestScope().stopPlay();
      ;
      log.debug("EventResult=" + ev);
      Assert.assertEquals(ExecutionStatus.POSTPONED, ev.getExecutionStatus());
      Assert.assertEquals(1, ev.getChildResults().size());
      Assert.assertEquals(ControlEvent.RELEASE_INSERT, ev.getChildResults().get(0).getEvent());
      Assert.assertEquals(ExecutionStatus.DENIED, ev.getChildResults().get(0).getExecutionStatus());

   }

}
