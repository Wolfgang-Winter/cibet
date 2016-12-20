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
package com.logitags.cibet.actuator.springsecurity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.cibethelper.SpringTestBase;
import com.cibethelper.entities.TComplexEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * add -javaagent:${project_loc}\..\cibet-material\technics\aspectjweaver-1.6.9. jar to java command
 */
public class DB_SpringSecurityTest extends SpringTestBase {

   private static Logger log = Logger.getLogger(DB_SpringSecurityTest.class);

   private static EntityManager cibetEman;

   @BeforeClass
   public static void beforeClass() throws Exception {
      log.debug("BEFORECLASS");
      EntityManagerFactory fac = Persistence.createEntityManagerFactory("localTest");
      cibetEman = fac.createEntityManager();
   }

   @Before
   public void before() {
      log.info("BEFORE TEST");
      super.before();
      cibetEman.getTransaction().begin();
   }

   @After
   public void after() {
      log.info("AFTER TEST");
      super.after();
      cibetEman.getTransaction().rollback();
   }

   @Test
   public void invoke4EyesDenyRelease() throws Exception {
      log.debug("start invoke4EyesDenyRelease()");
      TComplexEntity ent1 = new TComplexEntity();
      ent1.setStatValue(55);

      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class, schemes, "setStatValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      Thread.sleep(50);

      SpringSecurityActuator ssa = new SpringSecurityActuator();
      ssa.setName("SPRING2");
      ssa.setPreAuthorize("hasRole(\"ASD')");
      Configuration.instance().registerActuator(ssa);

      schemes = new ArrayList<String>();
      schemes.add("SPRING2");
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class, schemes, "setStatValue", ControlEvent.RELEASE);

      authenticate("WALTER");

      ent1.setStatValue(22);
      cibetEman.flush();

      EventResult er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("ARCHIVE, SPRING_SECURITY, FOUR_EYES", er.getActuators());

      Assert.assertEquals(55, ent1.getStatValue());

      // release
      Context.sessionScope().setUser("releaser");
      List<DcControllable> l = DcLoader.findUnreleased(TComplexEntity.class.getName());
      log.debug(l.get(0));
      l.get(0).release(Context.requestScope().getEntityManager(), null);

      // CibetContext.getEntityManager().clear();
      l = DcLoader.findUnreleased(TComplexEntity.class.getName());
      log.debug(l.get(0));

      er = Context.requestScope().getExecutedEventResult();
      Assert.assertNotNull(er);
      Assert.assertEquals(ExecutionStatus.DENIED, er.getExecutionStatus());
      Assert.assertEquals(0, er.getChildResults().size());
      Assert.assertNull(er.getParentResult());
      Assert.assertEquals("SPRING2, FOUR_EYES, ARCHIVE", er.getActuators());

      Assert.assertEquals(55, ent1.getStatValue());

      ssa.setThrowDeniedException(true);
      try {
         l.get(0).release(Context.requestScope().getEntityManager(), null);
         Assert.fail();
      } catch (DeniedException e) {
      }

      authenticate("ASD");
      log.debug(l.get(0));
      l = DcLoader.findUnreleased(TComplexEntity.class.getName());
      log.debug(l.get(0));
      l.get(0).release(Context.requestScope().getEntityManager(), "ok");

      // cibetEman.getTransaction().commit();
      log.debug("end");
   }

   @Test
   public void invoke4EyesPostAuth() throws ResourceApplyException {
      log.debug("start invoke4EyesPostAuth");
      TComplexEntity ent1 = new TComplexEntity();
      ent1.setStatValue(55);
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class, schemes, "setStatValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPostAuthorize("hasRole( 'WALTER')");

      authenticate("WALTER");

      ent1.setStatValue(22);

      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertEquals(55, ent1.getStatValue());

      // release
      Context.sessionScope().setUser("releaser");
      List<DcControllable> l = DcLoader.findUnreleased(TComplexEntity.class.getName());
      l.get(0).release(Context.requestScope().getEntityManager(), null);
      Assert.assertEquals(22, ent1.getStatValue());
   }

   @Test
   public void invoke4EyesPostAuthDenied() {
      log.debug("start invoke4EyesPostAuthDenied()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class, schemes, "setStatValue", ControlEvent.INVOKE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPostAuthorize("hasRole( 'WALTER')");

      authenticate("WALTER[RELEASE]");

      TComplexEntity ent1 = new TComplexEntity();
      TComplexEntity.setStaticStatValue(55);
      ent1.setStatValue(22);

      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertEquals(55, ent1.getStatValue());

      List<DcControllable> l = DcLoader.findUnreleased(TComplexEntity.class.getName());
      Assert.assertEquals(0, l.size());
   }

   @Test
   public void invoke4EyesNoRule() throws ResourceApplyException {
      log.debug("start invoke4EyesNoRule()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class, schemes, "setStatValue", ControlEvent.INVOKE);

      authenticate("WALTER[REDO]");

      TComplexEntity ent1 = new TComplexEntity();
      TComplexEntity.setStaticStatValue(22);
      ent1.setStatValue(33);

      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertEquals(22, ent1.getStatValue());

      // release
      Context.sessionScope().setUser("releaser");
      List<DcControllable> l = DcLoader.findUnreleased(TComplexEntity.class.getName());
      l.get(0).release(Context.requestScope().getEntityManager(), null);
      Assert.assertEquals(33, ent1.getStatValue());
   }

   @Test
   public void deleteDenied() {
      log.debug("start deleteDenied()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, ControlEvent.DELETE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      authenticate("WILLI");

      TComplexEntity ent1 = createTComplexEntity();
      cibetEman.persist(ent1);
      Assert.assertTrue(ent1.getId() != 0);
      cibetEman.flush();

      try {
         cibetEman.remove(ent1);
         Assert.fail();
      } catch (DeniedException e) {
      }
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      cibetEman.clear();
      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNotNull(ent2);
   }

   @Test
   public void deleteOk() {
      log.debug("start deleteOk()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, ControlEvent.DELETE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      authenticate("Heinz");

      TComplexEntity ent1 = createTComplexEntity();
      cibetEman.persist(ent1);
      cibetEman.flush();
      Assert.assertTrue(ent1.getId() != 0);

      cibetEman.remove(ent1);
      cibetEman.flush();
      cibetEman.clear();
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNull(ent2);
   }

   @Test
   public void updateDenied() {
      log.debug("start updateDenied()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RESTORE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      authenticate("WILLI");

      TComplexEntity ent1 = createTComplexEntity();
      cibetEman.persist(ent1);
      cibetEman.flush();
      Assert.assertTrue(ent1.getId() != 0);

      ent1.setCompValue(1045);
      cibetEman.merge(ent1);
      cibetEman.flush();
      cibetEman.clear();
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNotNull(ent2);
      Assert.assertEquals(12, ent2.getCompValue());
      Assert.assertNotSame(ent1, ent2);
   }

   @Test
   public void updateOkRestore() throws InterruptedException {
      log.debug("start updateOkRestore()");
      Context.sessionScope().setTenant(TENANT);
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RESTORE);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      authenticate("Heinz");

      TComplexEntity ent1 = createTComplexEntity();
      cibetEman.persist(ent1);
      cibetEman.flush();
      Assert.assertTrue(ent1.getId() != 0);

      ent1.setCompValue(1045);
      cibetEman.merge(ent1);
      cibetEman.flush();
      cibetEman.clear();
      // Context.requestScope().getEntityManager().getTransaction().commit();
      // Context.requestScope().getEntityManager().getTransaction().begin();

      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNotNull(ent2);
      Assert.assertEquals(1045, ent2.getCompValue());
      Assert.assertNotSame(ent2, ent1);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive arch = (Archive) q.getSingleResult();
      Assert.assertEquals(ControlEvent.UPDATE, arch.getControlEvent());

      Thread.sleep(20);
      // deny restore
      authenticate("Ganzig");

      TComplexEntity ce = (TComplexEntity) arch.restore(cibetEman, "HALLO");
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertNull(ce);
      cibetEman.flush();

      Thread.sleep(20);
      // allow restore
      authenticate("Heinz");

      ce = (TComplexEntity) arch.restore(cibetEman, "HALLO");
      Assert.assertNotNull(ce);
      Assert.assertEquals(1045, ce.getCompValue());

      List<Archive> list = q.getResultList();
      Assert.assertEquals(3, list.size());
      Assert.assertEquals(ControlEvent.UPDATE, list.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.RESTORE_UPDATE, list.get(1).getControlEvent());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(1).getExecutionStatus());
      Assert.assertEquals(ControlEvent.RESTORE_UPDATE, list.get(2).getControlEvent());
      Assert.assertEquals(ExecutionStatus.EXECUTED, list.get(2).getExecutionStatus());
   }

   @Test
   public void insertDenied() {
      log.debug("start insertDenied()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      authenticate("WILLI");

      TComplexEntity ent1 = createTComplexEntity();
      try {
         cibetEman.persist(ent1);
         Assert.fail();
      } catch (DeniedException e) {
      }
      cibetEman.flush();
      cibetEman.clear();
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNull(ent2);
   }

   @Test
   public void insertOk() {
      log.debug("start insertOk()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      authenticate("Heinz");

      TComplexEntity ent1 = createTComplexEntity();
      cibetEman.persist(ent1);
      cibetEman.flush();
      cibetEman.clear();

      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertTrue(ent1.getId() != 0);
      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNotNull(ent2);
   }

   @Test
   public void insertOkNoRules() {
      log.debug("start insertOkNoRules()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      authenticate("Heinz");

      TComplexEntity ent1 = createTComplexEntity();
      cibetEman.persist(ent1);
      cibetEman.flush();
      cibetEman.clear();
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertTrue(ent1.getId() != 0);
      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNotNull(ent2);
   }

   @Test
   public void insertOkUnknownRules() {
      log.debug("start insertOkUnknownRules()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("xxxAnyRole('Heinz', 'WALTER')");

      authenticate("Willi");

      TComplexEntity ent1 = createTComplexEntity();
      try {
         cibetEman.persist(ent1);
         Assert.fail();
      } catch (RuntimeException e) {
      }

      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNull(ent2);
   }

   @Test
   public void insertNotAuthenticated() {
      log.debug("start insertNotAuthenticated()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });
      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, ControlEvent.INSERT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");

      TComplexEntity ent1 = createTComplexEntity();
      cibetEman.persist(ent1);
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertTrue(ent1.getId() == 0);
      TComplexEntity ent2 = cibetEman.find(TComplexEntity.class, ent1.getId());
      Assert.assertNull(ent2);
   }

   @Test
   public void selectDenied() {
      log.debug("start selectDenied()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context.xml" });

      registerSetpoint(TComplexEntity.class, SpringSecurityActuator.DEFAULTNAME, ControlEvent.SELECT);

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      authenticate("WILLI");

      TComplexEntity ent1 = createTComplexEntity();
      cibetEman.persist(ent1);
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      try {
         cibetEman.find(TComplexEntity.class, ent1.getId());
         Assert.fail();
      } catch (DeniedException e) {
      }
      cibetEman.flush();
      cibetEman.clear();
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
   }

}
