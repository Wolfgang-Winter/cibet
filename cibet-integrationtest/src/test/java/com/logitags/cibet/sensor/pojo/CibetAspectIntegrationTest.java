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
package com.logitags.cibet.sensor.pojo;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.NoResultException;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.cibethelper.SpringExampleBean;
import com.cibethelper.SpringExampleBean2;
import com.cibethelper.base.AspectInvokeTestClass;
import com.cibethelper.base.DBHelper;
import com.cibethelper.base.ParamSingleton;
import com.cibethelper.base.SimpleSingleton;
import com.cibethelper.base.SingletonFactory;
import com.cibethelper.base.SingletonFactoryService;
import com.cibethelper.base.SingletonFactoryService2;
import com.cibethelper.base.StaticFactory;
import com.cibethelper.base.StaticFactoryService;
import com.cibethelper.base.StaticFactoryService2;
import com.cibethelper.base.SubArchiveController;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.DeniedException;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.dc.TwoManRuleActuator;
import com.logitags.cibet.actuator.springsecurity.SpringSecurityActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * add -javaagent:${project_loc}\..\cibet-material\technics\aspectjweaver-1.6.9. jar to java command
 */
public class CibetAspectIntegrationTest extends DBHelper {

   private static Logger log = Logger.getLogger(CibetAspectIntegrationTest.class);

   private Setpoint sp = null;

   @After
   public void afterCibetAspectIntegrationTest() {
      if (sp != null) {
         Configuration.instance().unregisterSetpoint(sp.getId());
      }
   }

   private void release() throws ResourceApplyException {
      Context.internalRequestScope().getEntityManager().clear();
      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      MethodResource res = (MethodResource) l.get(0).getResource();
      log.debug("size: " + res.getParameters().size());

      Context.sessionScope().setUser("test2");
      l.get(0).release(applEman, null);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());
   }

   @Test
   public void invokeArchive() {
      log.info("start invokeArchive");
      sp = registerSetpoint(TComplexEntity.class, ArchiveActuator.DEFAULTNAME, "setCompValue", ControlEvent.INVOKE);

      TComplexEntity ent1 = new TComplexEntity();
      ent1.setCompValue(22);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());
   }

   @Test
   public void invoke4Eyes() throws Exception {
      log.info("start invoke4Eyes");
      TComplexEntity ent1 = new TComplexEntity();
      ent1.setStatValue(55);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class, schemes, "setStatValue", ControlEvent.INVOKE);

      ent1.setStatValue(3434);
      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setStatValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setStatValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());
      log.debug("size: " + res.getParameters().size());

      Assert.assertEquals(55, ent1.getStatValue());
      release();
      Assert.assertEquals(3434, ent1.getStatValue());
   }

   @Test
   public void invoke4EyesStatic() throws Exception {
      log.info("start invoke4EyesStatic");
      TComplexEntity.setStaticStatValue(33);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(TComplexEntity.class, schemes, "setStaticStatValue", ControlEvent.INVOKE);

      TComplexEntity.setStaticStatValue(44);
      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setStaticStatValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setStaticStatValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());

      Assert.assertEquals(33, TComplexEntity.getStaticStatValue());
      release();
      Assert.assertEquals(44, TComplexEntity.getStaticStatValue());
   }

   @Test
   public void invoke4EyesReleaseWithConstructorParam() throws Exception {
      log.info("start invoke4EyesReleaseWithConstructorParam");
      TCompareEntity ent1 = new TCompareEntity();
      ent1.setStatValue("garnix");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(TCompareEntity.class, schemes, "setStatValue", ControlEvent.INVOKE);

      ent1.setStatValue("GOGO-Girl");

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setStatValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());
      Assert.assertEquals("Hasenfuss", res.getInvokerParam());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setStatValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());

      Assert.assertEquals("garnix", ent1.getStatValue());
      release();
      Assert.assertEquals("GOGO-Girl", ent1.getStatValue());
      Assert.assertEquals("Hasenfuss", ent1.getConstrValue());
   }

   @Test
   public void invoke4EyesReleaseSingleton() throws Exception {
      log.info("start invoke4EyesReleaseSingleton");
      ITComplexEntity ent1 = SimpleSingleton.instance();
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(SimpleSingleton.class, schemes, "setCompValue", ControlEvent.INVOKE);

      ent1.setCompValue(3434);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());

      Assert.assertEquals(10, ent1.getCompValue());
      release();
      Assert.assertEquals(3434, ent1.getCompValue());
   }

   @Test
   public void invoke4EyesSingletonWithConstructorParam() throws Exception {
      log.info("start invoke4EyesSingletonWithConstructorParam");
      ParamSingleton ent1 = ParamSingleton.instance();
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(ParamSingleton.class, schemes, "setCompValue", ControlEvent.INVOKE);

      ent1.setCompValue(100);
      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());
      Assert.assertEquals("Walter", res.getInvokerParam());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());
      Assert.assertEquals("Walter", res.getInvokerParam());

      Assert.assertEquals(10, ent1.getCompValue());
      release();
      Assert.assertEquals(100, ent1.getCompValue());
      Assert.assertEquals("Walter", ParamSingleton.getParam());
   }

   @Test
   public void invoke4EyesReleaseStaticFactory() throws Exception {
      log.info("start invoke4EyesReleaseStaticFactory");
      ITComplexEntity ent1 = StaticFactory.create();
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(StaticFactoryService.class, schemes, "setCompValue", ControlEvent.INVOKE);

      ent1.setCompValue(3434);
      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(FactoryInvoker.class.getName(), res.getInvokerClass());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(FactoryInvoker.class.getName(), res.getInvokerClass());

      Assert.assertEquals(10, ent1.getCompValue());
      release();
      Assert.assertEquals(3434, ent1.getCompValue());
   }

   @Test
   public void invoke4EyesReleaseStaticFactoryWithMethod() throws Exception {
      log.info("start invoke4EyesReleaseStaticFactoryWithMethod");
      ITComplexEntity ent1 = StaticFactory.create2();
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(StaticFactoryService2.class, schemes, "setCompValue", ControlEvent.INVOKE);

      ent1.setCompValue(3434);
      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(FactoryInvoker.class.getName(), res.getInvokerClass());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(FactoryInvoker.class.getName(), res.getInvokerClass());

      Assert.assertEquals(10, ent1.getCompValue());
      release();
      Assert.assertEquals(3434, ent1.getCompValue());
   }

   @Test
   public void invoke4EyesReleaseSingletonFactory() throws Exception {
      log.info("start invoke4EyesReleaseSingletonFactory");
      ITComplexEntity ent1 = SingletonFactory.getInstance().create();
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(SingletonFactoryService.class, schemes, "setCompValue", ControlEvent.INVOKE);

      ent1.setCompValue(3434);
      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(FactoryInvoker.class.getName(), res.getInvokerClass());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(FactoryInvoker.class.getName(), res.getInvokerClass());
      Assert.assertEquals("com.cibethelper.base.SingletonFactory", res.getInvokerParam());

      Assert.assertEquals(10, ent1.getCompValue());
      release();
      Assert.assertEquals(3434, ent1.getCompValue());
   }

   @Test
   public void invoke4EyesReleaseSingletonFactoryWithMethod() throws Exception {
      log.info("start invoke4EyesReleaseSingletonFactoryWithMethod");
      ITComplexEntity ent1 = SingletonFactory.getInstance().create2();
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(SingletonFactoryService2.class, schemes, "setCompValue", ControlEvent.INVOKE);

      ent1.setCompValue(3434);
      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(FactoryInvoker.class.getName(), res.getInvokerClass());
      Assert.assertEquals("com.cibethelper.base.SingletonFactory.create2()", res.getInvokerParam());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(FactoryInvoker.class.getName(), res.getInvokerClass());

      Assert.assertEquals(10, ent1.getCompValue());
      release();
      Assert.assertEquals(3434, ent1.getCompValue());
   }

   @Test
   public void invoke4EyesSpringBean() throws Exception {
      log.info("start invoke4EyesSpringBean");
      ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "spring-context_3.xml" });

      ITComplexEntity ent1 = context.getBean("MySpringExampleBean", SpringExampleBean.class);
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(SpringExampleBean.class, schemes, "setCompValue", ControlEvent.INVOKE);

      // set ThrowPostponedException
      FourEyesActuator fea = (FourEyesActuator) Configuration.instance().getActuator(FourEyesActuator.DEFAULTNAME);
      fea.setThrowPostponedException(true);

      try {
         ent1.setCompValue(3434);
         Assert.fail();
      } catch (PostponedException e) {
      }

      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(SpringBeanInvoker.class.getName(), res.getInvokerClass());
      Assert.assertEquals("MySpringExampleBean", res.getInvokerParam());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(SpringBeanInvoker.class.getName(), res.getInvokerClass());

      Assert.assertEquals(10, ent1.getCompValue());
      release();
      Assert.assertEquals(3434, ent1.getCompValue());
      fea.setThrowPostponedException(false);
   }

   @Test
   public void invoke4EyesSpringNoBeanId() throws Exception {
      log.info("start invoke4EyesSpringNoBeanId");
      ApplicationContext context = new ClassPathXmlApplicationContext(new String[] { "spring-context_3.xml" });

      ITComplexEntity ent1 = context.getBean("MySpringExampleBean2", SpringExampleBean2.class);
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(SpringExampleBean2.class, schemes, "setCompValue", ControlEvent.INVOKE);

      FourEyesActuator fea = (FourEyesActuator) Configuration.instance().getActuator(FourEyesActuator.DEFAULTNAME);
      fea.setThrowPostponedException(true);

      try {
         ent1.setCompValue(3434);
         Assert.fail();
      } catch (PostponedException e) {
      }

      applEman.flush();

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(SpringBeanInvoker.class.getName(), res.getInvokerClass());
      Assert.assertTrue(res.getInvokerParam() == null || "".equals(res.getInvokerParam()));

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());
      Assert.assertEquals(SpringBeanInvoker.class.getName(), res.getInvokerClass());

      Assert.assertEquals(10, ent1.getCompValue());
      release();
      Assert.assertEquals(3434, ent1.getCompValue());
      fea = (FourEyesActuator) Configuration.instance().getActuator(FourEyesActuator.DEFAULTNAME);
      fea.setThrowPostponedException(false);
   }

   @Test
   public void invokeWith2ManRule() throws Exception {
      log.info("start invokeWith2ManRule");
      ITComplexEntity ent1 = SimpleSingleton.instance();
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      sp = registerSetpoint(SimpleSingleton.class, schemes, "setCompValue", ControlEvent.INVOKE);

      ent1.setCompValue(3434);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertEquals(10, ent1.getCompValue());

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("setCompValue", res.getMethod());

      List<DcControllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, list1.size());
      DcControllable dcOb = list1.get(0);
      Assert.assertEquals(ControlEvent.INVOKE, dcOb.getControlEvent());
   }

   @Test
   public void invokeWith2ManRuleDirectRelease() {
      log.info("start invokeWith2ManRuleDirectRelease()");
      ITComplexEntity ent1 = SimpleSingleton.instance();
      ent1.setCompValue(10);

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(SimpleSingleton.class, schemes, "setCompValue", ControlEvent.INVOKE,
            ControlEvent.RELEASE_INVOKE);

      Context.sessionScope().setSecondUser("secondUser");
      ent1.setCompValue(3434);
      Assert.assertEquals(ExecutionStatus.POSTPONED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertEquals(3434, ent1.getCompValue());
      Context.sessionScope().setSecondUser(null);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      List<Archive> list = q.getResultList();
      Assert.assertEquals(2, list.size());
      Archive ar = list.get(0);
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());

      List<DcControllable> list1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, list1.size());
   }

   @Test
   public void invokeWith2ManRuleDirectReleasePreDenied() throws Exception {
      log.info("start invokeWith2ManRuleDirectReleasePreDenied()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(SimpleSingleton.class, schemes, "getCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPreAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp2 = registerSetpoint(SimpleSingleton.class, schemes2, "getCompValue", ControlEvent.RELEASE_INVOKE);

      ITComplexEntity ent1 = SimpleSingleton.instance();
      ent1.setCompValue(20);

      authenticate("Heinz");
      authenticateSecond("HAAA");
      Context.sessionScope().setSecondUser("secondUser");

      try {
         ent1.getCompValue();
         Assert.fail();
      } catch (DeniedException e) {
         log.debug(e.getMessage());
         log.debug(e.getDeniedUser());
         Assert.assertEquals("Access is denied", e.getMessage());
         Assert.assertEquals("secondUser", e.getDeniedUser());
      }
      Context.sessionScope().setSecondUser(null);
      Context.sessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
      Configuration.instance().unregisterSetpoint(sp2.getId());
   }

   @Test
   public void invokeWith2ManRuleDirectReleasePostDenied() throws Exception {
      log.info("start invokeWith2ManRuleDirectReleasePostDenied()");
      new ClassPathXmlApplicationContext(new String[] { "spring-context_1.xml" });

      SpringSecurityActuator act = (SpringSecurityActuator) Configuration.instance()
            .getActuator(SpringSecurityActuator.DEFAULTNAME);
      act.setPreAuthorize("hasAnyRole('Heinz', 'WALTER')");
      act.setThrowDeniedException(true);

      List<String> schemes = new ArrayList<String>();
      schemes.add(SpringSecurityActuator.DEFAULTNAME);
      schemes.add(TwoManRuleActuator.DEFAULTNAME);
      sp = registerSetpoint(SimpleSingleton.class, schemes, "getCompValue", ControlEvent.INVOKE);

      SpringSecurityActuator releaseSec = new SpringSecurityActuator("RELEASESEC");
      releaseSec.setPostAuthorize("hasAnyRole('Wooo')");
      releaseSec.setThrowDeniedException(true);
      releaseSec.setSecondPrincipal(true);
      Configuration.instance().registerActuator(releaseSec);

      Thread.sleep(30);
      List<String> schemes2 = new ArrayList<String>();
      schemes2.add(releaseSec.getName());
      schemes2.add(TwoManRuleActuator.DEFAULTNAME);
      Setpoint sp2 = registerSetpoint(SimpleSingleton.class, schemes2, "getCompValue", ControlEvent.RELEASE_INVOKE);

      ITComplexEntity ent1 = SimpleSingleton.instance();
      ent1.setCompValue(20);

      authenticate("Heinz");
      authenticateSecond("HAAA");
      Context.sessionScope().setSecondUser("secondUser");

      try {
         ent1.getCompValue();
         Assert.fail();
      } catch (DeniedException e) {
         log.debug(e.getMessage());
         log.debug(e.getDeniedUser());
         Assert.assertEquals("Access is denied", e.getMessage());
         Assert.assertEquals("secondUser", e.getDeniedUser());
      }
      Context.sessionScope().setSecondUser(null);
      Context.sessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
      Configuration.instance().unregisterSetpoint(sp2.getId());
   }

   @Test(expected = NoResultException.class)
   public void invokeMethodNoInterceptor() {
      log.info("start invokeMethodNoInterceptor");
      sp = registerSetpoint(AspectInvokeTestClass.class, ArchiveActuator.DEFAULTNAME,
            "callWithAspect, callWithoutAspect", ControlEvent.INVOKE);

      AspectInvokeTestClass ent1 = new AspectInvokeTestClass();
      String answer = ent1.callWithoutAspect("Hello!");
      Assert.assertEquals("Hello!", answer);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      q.getSingleResult();
   }

   // @Ignore
   @Test
   public void invokeMethodInterceptor() {
      log.info("start invokeMethodInterceptor");
      sp = registerSetpoint(AspectInvokeTestClass.class, ArchiveActuator.DEFAULTNAME,
            "callWithAspect, callWithoutAspect", ControlEvent.INVOKE);

      AspectInvokeTestClass ent1 = new AspectInvokeTestClass();
      String answer = ent1.callWithAspect("Hello!");
      Assert.assertEquals("Hello!", answer);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("callWithAspect", res.getMethod());
      Assert.assertEquals(AspectInvokeTestClass.class.getName(), res.getTargetType());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());
   }

   @Test
   public void invokeCustomAspect() throws Exception {
      log.info("start invokeCustomAspect");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      sp = registerSetpoint(SubArchiveController.class, schemes, "getName", ControlEvent.INVOKE);

      SubArchiveController ctrl = new SubArchiveController();
      String name = ctrl.getName();
      log.debug("name=" + name);
      Assert.assertNull(name);

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      Assert.assertNotNull(ar);
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("getName", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());

      q = Context.requestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      DcControllable co = (DcControllable) q.getSingleResult();
      Assert.assertNotNull(co);
      res = (MethodResource) co.getResource();
      Assert.assertEquals("getName", res.getMethod());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());

      log.debug("now release");
      Context.sessionScope().setUser("test2");
      name = (String) co.release(applEman, null);

      Assert.assertEquals("SubArchiveTestController", name);
   }

   @Test
   public void invokeMethodInterceptorWithException() {
      log.info("start invokeMethodInterceptorWithException");
      sp = registerSetpoint(AspectInvokeTestClass.class, ArchiveActuator.DEFAULTNAME, "callWithException",
            ControlEvent.INVOKE);

      AspectInvokeTestClass ent1 = new AspectInvokeTestClass();
      try {
         ent1.callWithException();
         Assert.fail();
      } catch (Exception e) {
         log.debug(e.getMessage());
         Assert.assertEquals("Expected FAIL", e.getMessage());
      }

      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      q.setParameter("tenant", TENANT);
      Archive ar = (Archive) q.getSingleResult();
      MethodResource res = (MethodResource) ar.getResource();
      Assert.assertEquals("callWithException", res.getMethod());
      Assert.assertEquals(AspectInvokeTestClass.class.getName(), res.getTargetType());
      Assert.assertEquals(PojoInvoker.class.getName(), res.getInvokerClass());
      Assert.assertEquals(ExecutionStatus.ERROR, ar.getExecutionStatus());
   }

}
