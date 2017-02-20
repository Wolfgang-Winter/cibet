/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.it;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.EJB;
import javax.ejb.EJBException;

import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.ParallelDcActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.authentication.AnonymousAuthenticationProvider;
import com.logitags.cibet.authentication.InvocationContextAuthenticationProvider;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * DcManagerImplIntegrationTest, ParallelDcEjbContainerTest, EnversActuatorIntegrationTest,
 * LockerImplIntegrationDeleteInsertTest
 * 
 * @author Wolfgang
 * 
 */
@RunWith(Arquillian.class)
public class ParallelDcIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(ParallelDcIT.class);

   @EJB(beanName = "RemoteEJBImpl")
   private RemoteEJB remoteEjb;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = ParallelDcIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class, RemoteEJB.class,
            RemoteEJBImpl.class, SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it-derby.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource("config_2.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeParallelDcIT() {
      log.debug("execute before()");
      if (cman == null) {
         cman = Configuration.instance();
      } else {
         cman.initialise();
      }
      Map<String, Object> map = new HashMap<String, Object>();
      map.put("USER_NAME", USER);
      map.put("TENANT_NAME", TENANT);
      InitializationService.instance().startContext(null, new InvocationContextAuthenticationProvider(map),
            new AnonymousAuthenticationProvider());
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterParallelDcIT() {
      InitializationService.instance().endContext();
   }

   @Test
   public void interceptParallel() throws Exception {
      log.info("start interceptParallel()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ParallelDcActuator.DEFAULTNAME);
      registerSetpoint(RemoteEJBImpl.class.getName(), "storeTEntityParallel", schemes, ControlEvent.INVOKE);
      ((ParallelDcActuator) cman.getActuator(ParallelDcActuator.DEFAULTNAME)).setTimelag(1);

      TEntity entity = createTEntity(78, "Katrin");
      TEntity te = remoteEjb.storeTEntityParallel(entity);
      Assert.assertEquals(79, te.getCounter());
      EventResult er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(ParallelDcActuator.DEFAULTNAME, er.getActuators());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      List<TEntity> list = loadTEntities();
      Assert.assertEquals(0, list.size());

      // timelag
      log.debug("CaseId: " + er.getCaseId());
      Context.requestScope().setCaseId(er.getCaseId());
      try {
         te = remoteEjb.storeTEntityParallel(te);
         Assert.fail();
      } catch (EJBException e) {
         Assert.assertTrue(e.getCause() instanceof CibetException);
      }

      // invalid user
      Thread.sleep(1100);
      Context.requestScope().setCaseId(er.getCaseId());
      try {
         te = remoteEjb.storeTEntityParallel(te);
         Assert.fail();
      } catch (EJBException e) {
         Assert.assertTrue(e.getCause() instanceof InvalidUserException);
      }

      Context.sessionScope().setUser("otherUser");
      te = remoteEjb.storeTEntityParallel(te);
      Assert.assertEquals(80, te.getCounter());

      l = DcLoader.findUnreleased();
      Assert.assertEquals(2, l.size());

      list = loadTEntities();
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void parallelReleaseLessExecutions() throws Exception {
      log.info("start parallelReleaseLessExecutions()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ParallelDcActuator.DEFAULTNAME);
      registerSetpoint(RemoteEJBImpl.class.getName(), "storeTEntityParallel", schemes, ControlEvent.INVOKE);
      ((ParallelDcActuator) cman.getActuator(ParallelDcActuator.DEFAULTNAME)).setExecutions(2);

      TEntity entity = createTEntity(78, "Katrin");
      TEntity te = remoteEjb.storeTEntityParallel(entity);
      Assert.assertEquals(79, te.getCounter());
      EventResult er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      try {
         ut.begin();
         l.get(0).release("released by ParallelDc");
         Assert.fail();
      } catch (ResourceApplyException e) {
         Assert.assertTrue(e.getMessage().endsWith("but has been executed only 1 times"));
      }
      ut.rollback();
   }

   @Test
   public void parallelReleaseInvalidUser() throws Exception {
      log.info("start parallelReleaseInvalidUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ParallelDcActuator.DEFAULTNAME);
      registerSetpoint(RemoteEJBImpl.class.getName(), "storeTEntityParallel", schemes, ControlEvent.INVOKE);
      ((ParallelDcActuator) cman.getActuator(ParallelDcActuator.DEFAULTNAME)).setExecutions(2);

      TEntity entity = createTEntity(78, "Katrin");
      TEntity te = remoteEjb.storeTEntityParallel(entity);
      Assert.assertEquals(79, te.getCounter());
      EventResult er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.requestScope().setCaseId(er.getCaseId());
      Context.sessionScope().setUser("otherUser");
      te = remoteEjb.storeTEntityParallel(te);
      Assert.assertEquals(80, te.getCounter());

      try {
         ut.begin();
         l.get(0).release("released by ParallelDc");
         Assert.fail();
      } catch (InvalidUserException e) {
      }
      ut.commit();
   }

   @Test
   public void parallelRelease() throws Exception {
      log.info("start parallelRelease()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ParallelDcActuator.DEFAULTNAME);
      registerSetpoint(RemoteEJBImpl.class.getName(), "storeTEntityParallel", schemes, ControlEvent.INVOKE);
      ((ParallelDcActuator) cman.getActuator(ParallelDcActuator.DEFAULTNAME)).setExecutions(2);

      TEntity entity = createTEntity(78, "Katrin");
      TEntity te = remoteEjb.storeTEntityParallel(entity);
      Assert.assertEquals(79, te.getCounter());
      EventResult er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.requestScope().setCaseId(er.getCaseId());
      Context.sessionScope().setUser("otherUser");
      te = remoteEjb.storeTEntityParallel(te);
      Assert.assertEquals(80, te.getCounter());

      l = DcLoader.findUnreleased();
      Assert.assertEquals(2, l.size());

      Context.sessionScope().setUser("releaseUser");
      ut.begin();
      TEntity result = (TEntity) l.get(1).release(applEman, "blabla");
      ut.commit();
      Assert.assertEquals(80, result.getCounter());

      List<TEntity> list = loadTEntities();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(80, list.get(0).getCounter());
   }

   @Test
   public void parallelRelease2() throws Exception {
      log.info("start parallelRelease2()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(ParallelDcActuator.DEFAULTNAME);
      registerSetpoint(RemoteEJBImpl.class.getName(), "storeTEntityParallel", schemes, ControlEvent.INVOKE);
      ((ParallelDcActuator) cman.getActuator(ParallelDcActuator.DEFAULTNAME)).setExecutions(3);

      // first
      TEntity entity = createTEntity(78, "Katrin");
      TEntity te = remoteEjb.storeTEntityParallel(entity);
      Assert.assertEquals(79, te.getCounter());
      EventResult er = Context.requestScope().getExecutedEventResult();
      log.debug(er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());

      List<DcControllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      // second
      Context.requestScope().setCaseId(er.getCaseId());
      Context.sessionScope().setUser("otherUser");
      te = remoteEjb.storeTEntityParallel(te);
      Assert.assertEquals(80, te.getCounter());

      l = DcLoader.findUnreleased();
      Assert.assertEquals(2, l.size());

      // third
      Context.requestScope().setCaseId(er.getCaseId());
      Context.sessionScope().setUser("thirdUser");
      te = remoteEjb.storeTEntityParallel(te);
      Assert.assertEquals(81, te.getCounter());

      l = DcLoader.findUnreleased();
      Assert.assertEquals(3, l.size());

      int counter = ((TEntity) l.get(0).getResource().getResultObject()).getCounter();
      // reject 2 +3
      ut.begin();
      l.get(1).reject("rejected 1");
      ut.commit();
      l.get(1).setExecutionStatus(ExecutionStatus.REJECTED);
      ut.begin();
      l.get(2).reject("rejected 2");
      ut.commit();

      // release
      Context.sessionScope().setUser("releaseUser");

      try {
         ut.begin();
         l.get(1).release("released by ParallelDc");
         Assert.fail();
      } catch (ResourceApplyException e) {
         Assert.assertTrue(e.getMessage().endsWith("but is in status REJECTED"));
      }
      ut.rollback();

      Context.sessionScope().setUser("tester2");
      ut.begin();
      TEntity result = (TEntity) l.get(0).release(applEman, "blabla");
      ut.commit();
      Assert.assertEquals(counter, result.getCounter());

      Thread.sleep(100);

      List<TEntity> list = loadTEntities();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(counter, list.get(0).getCounter());
   }

}
