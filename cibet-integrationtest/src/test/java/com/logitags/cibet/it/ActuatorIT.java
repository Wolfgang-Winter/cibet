/*
 *******************************************************************************
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
import java.util.Iterator;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.persistence.Query;
import javax.persistence.TransactionRequiredException;
import javax.transaction.SystemException;

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
import com.cibethelper.ejb.SecuredRemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianTestServlet1;
import com.dumbster.smtp.SimpleSmtpServer;
import com.dumbster.smtp.SmtpMessage;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ParallelDcActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.actuator.dc.SixEyesActuator;
import com.logitags.cibet.actuator.dc.TwoManRuleActuator;
import com.logitags.cibet.actuator.lock.LockActuator;
import com.logitags.cibet.actuator.lock.Locker;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.diff.DifferenceType;
import com.logitags.cibet.sensor.jpa.JpaResource;

/**
 * DcManagerImplIntegrationTest, ParallelDcEjbContainerTest, EnversActuatorIntegrationTest,
 * LockerImplIntegrationDeleteInsertTest
 * 
 * @author Wolfgang
 * 
 */
@RunWith(Arquillian.class)
public class ActuatorIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(ActuatorIT.class);

   @EJB(beanName = "RemoteEJBImpl")
   private RemoteEJB remoteEjb;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = ActuatorIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, TEntity.class, TComplexEntity.class,
            ITComplexEntity.class, AbstractTEntity.class, TCompareEntity.class, TComplexEntity2.class,
            ArquillianTestServlet1.class, RemoteEJB.class, RemoteEJBImpl.class, SecuredRemoteEJBImpl.class,
            SimpleEjb.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      // File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-envers")
      // .withTransitivity().asFile();
      // archive.addAsLibraries(spring);

      File[] dumbster = Maven.resolver().loadPomFromFile("pom.xml").resolve("dumbster:dumbster").withTransitivity()
            .asFile();
      archive.addAsLibraries(dumbster);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("config_2.xml", "classes/cibet-config.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeActuatorIT() throws SystemException {
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      cman = Configuration.instance();
   }

   @After
   public void afterActuatorIT() {
      Context.end();
      new ConfigurationService().initialise();
   }

   protected Object release(Controllable co) throws Exception {
      Context.sessionScope().setUser("tester2");
      ut.begin();
      Object result = co.release(applEman, "blabla");
      ut.commit();
      return result;
   }

   @Test
   public void findUnreleased() throws Exception {
      log.info("start findUnreleased()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity te = createTEntity(12, "Hirsch");
      persist(te);

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      l = DcLoader.findUnreleased(TEntity.class.getName());
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void releasepersist() throws Exception {
      log.info("start releasepersist()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");

      TEntity ent = createTEntity(12, "Hirsch");

      persist(ent);
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased(ut);
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());

      Object res = release(co);
      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() != 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
      Context.requestScope().setRemark(null);
   }

   @Test
   public void releasepersistNoTransaction() throws Exception {
      log.info("start releasepersistNoTransaction()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);
      Context.requestScope().setRemark("created");

      TEntity ent = createTEntity(12, "Hirsch");
      persist(ent);
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());

      Context.sessionScope().setUser("tester2");
      try {
         co.release(applEman, "blabla");
         Assert.fail();
      } catch (TransactionRequiredException e) {
      }
   }

   @Test
   public void releasePersistWithApproveUser() throws Exception {
      log.info("start releasePersistWithApproveUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE);

      Context.sessionScope().setApprovalUser("fizz");
      TEntity ent = createTEntity(12, "Hirsch");
      persist(ent);
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("fizz", co.getReleaseUser());

      Context.sessionScope().setUser("test2");
      ut.begin();
      try {
         co.release(applEman, null);
         Assert.fail();
      } catch (InvalidUserException e) {
      }
      ut.commit();

      Context.sessionScope().setUser("fizz");
      ut.begin();
      Object res = co.release(applEman, null);
      ut.commit();
      Context.sessionScope().setApprovalUser(null);
      Assert.assertNotNull(res);
      Assert.assertTrue(res instanceof TEntity);
      Assert.assertTrue(((TEntity) res).getId() != 0);

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser(USER);
      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
   }

   @Test(expected = InvalidUserException.class)
   public void releasePersistInvalidUser() throws Exception {
      log.info("start releasePersistInvalidUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      TEntity ent = createTEntity(12, "Hirsch");
      persist(ent);

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      ut.begin();
      try {
         co.release(applEman, null);
      } finally {
         ut.commit();
      }
   }

   @Test
   public void releaseUpdate() throws Exception {
      log.info("start releaseUpdate()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.UPDATE, ControlEvent.RELEASE_UPDATE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());
      TEntity t1 = tce.getLazyList().iterator().next();
      tce.getLazyList().remove(t1);
      tce.setCompValue(122);
      applEman.merge(tce);
      ut.commit();

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);

      release(co);

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      applEman.clear();
      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());
      TComplexEntity tce4 = (TComplexEntity) query.getSingleResult();
      Assert.assertEquals(2, tce4.getLazyList().size());
      Assert.assertEquals(122, tce4.getCompValue());
   }

   @Test
   public void releaseRemove6Eyes() throws Exception {
      log.info("start releaseRemove6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE_DELETE,
            ControlEvent.FIRST_RELEASE_DELETE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());
      ut.commit();
      Context.requestScope().setRemark("Heinz");

      remove(tce);

      // first release
      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      Context.requestScope().setRemark("Manni");
      release(co);
      applEman.clear();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("tester2", co.getFirstApprovalUser());
      Assert.assertEquals(ControlEvent.DELETE, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());
      Assert.assertEquals("Heinz", co.getCreateRemark());
      Assert.assertEquals("blabla", co.getFirstApprovalRemark());

      // still not removed
      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());

      // 2. release
      try {
         // invalid user
         ut.begin();
         co.release(applEman, "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
      }
      ut.rollback();

      try {
         // invalid user
         Context.sessionScope().setUser("tester2");
         ut.begin();
         co.release(applEman, "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
         ut.rollback();
      }

      Context.sessionScope().setUser("tester3");
      ut.begin();
      co.release(applEman, "blabla2");
      ut.commit();

      // now it is removed
      applEman.clear();
      l = query.getResultList();
      Assert.assertEquals(0, l.size());
   }

   @Test
   public void releaseRemove6EyesWithApprovalUser() throws Exception {
      log.info("start releaseRemove6EyesWithApprovalUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.RELEASE_DELETE,
            ControlEvent.FIRST_RELEASE_DELETE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      ut.begin();
      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      Assert.assertNotNull(tce);
      Assert.assertEquals(3, tce.getLazyList().size());
      ut.commit();

      Context.sessionScope().setApprovalUser("Wizzi");
      remove(tce);

      // first release
      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      Context.sessionScope().setUser("tester2");
      try {
         ut.begin();
         co.release(applEman, "blabla1");
         Assert.fail();
      } catch (InvalidUserException e1) {
      }
      ut.rollback();

      Context.sessionScope().setUser("Wizzi");
      Context.sessionScope().setApprovalUser("M�ggel");
      ut.begin();
      co.release(applEman, "blabla1");
      ut.commit();
      applEman.clear();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("Wizzi", co.getFirstApprovalUser());
      Assert.assertNotNull(co.getFirstApprovalDate());
      Assert.assertEquals(ControlEvent.DELETE, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());

      // still not removed
      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());

      // 2. release
      Context.sessionScope().setUser("Nixxi");
      Context.sessionScope().setApprovalUser("Lobo");
      try {
         // invalid user
         ut.begin();
         co.release(applEman, "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
         ut.rollback();
      }

      Context.sessionScope().setUser("M�ggel");
      ut.begin();
      co.release(applEman, "blabla2");
      ut.commit();

      // now it is removed
      applEman.clear();
      l = query.getResultList();
      Assert.assertEquals(0, l.size());
      Context.sessionScope().setApprovalUser(null);
   }

   @Test
   public void rejectRemove4Eyes() throws Exception {
      log.info("start rejectRemove4Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE, ControlEvent.REJECT);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      remove(tce);

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      ut.begin();
      co.reject(applEman, "blabla1");
      ut.commit();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void rejectPersist6Eyes() throws Exception {
      log.info("start rejectPersist6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.REJECT_INSERT,
            ControlEvent.FIRST_RELEASE_INSERT);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(0, l.size());

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      release(co);

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("tester2", co.getFirstApprovalUser());
      Assert.assertEquals(ControlEvent.INSERT, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());

      l = query.getResultList();
      Assert.assertEquals(0, l.size());

      Context.sessionScope().setUser(USER);
      ut.begin();
      co.reject(applEman, "blabla2");
      ut.commit();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      l = query.getResultList();
      Assert.assertEquals(0, l.size());
   }

   @Test
   public void rejectPersist6EyesWithApprovalUser() throws Exception {
      log.info("start rejectPersist6EyesWithApprovalUser()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.REJECT_INSERT,
            ControlEvent.FIRST_RELEASE_INSERT);
      SimpleSmtpServer server = SimpleSmtpServer.start(8854);

      try {
         Context.sessionScope().setApprovalUser("fizz");
         TComplexEntity ce = createTComplexEntity();
         persist(ce);

         Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
         query.setParameter("owner", TENANT);
         List<TComplexEntity> l = query.getResultList();
         Assert.assertEquals(0, l.size());
         server.stop();
         Assert.assertEquals(0, server.getReceivedEmailSize());

         server = SimpleSmtpServer.start(8854);
         List<Controllable> l1 = DcLoader.findUnreleased();
         Assert.assertEquals(1, l1.size());
         Controllable co = l1.get(0);
         Context.sessionScope().setUser("fizz");
         Context.sessionScope().setApprovalUser("Muzzi");
         ut.begin();
         co.release(applEman, "blabla1");
         ut.commit();
         server.stop();
         Assert.assertEquals(0, server.getReceivedEmailSize());

         l1 = DcLoader.findUnreleased();
         Assert.assertEquals(1, l1.size());
         co = l1.get(0);
         Assert.assertEquals("fizz", co.getFirstApprovalUser());
         Assert.assertEquals(ControlEvent.INSERT, co.getControlEvent());
         Assert.assertEquals(USER, co.getCreateUser());

         l = query.getResultList();
         Assert.assertEquals(0, l.size());

         Context.sessionScope().setUser("Wolli");
         try {
            ut.begin();
            co.reject(applEman, "blabla2");
            Assert.fail();
         } catch (Exception e) {
            ut.rollback();
         }

         server = SimpleSmtpServer.start(8854);
         Context.sessionScope().setUser(USER);
         ut.begin();
         co.reject(applEman, "blabla2");
         ut.commit();
         server.stop();
         Assert.assertEquals(0, server.getReceivedEmailSize());

         l1 = DcLoader.findUnreleased();
         Assert.assertEquals(0, l1.size());

         l = query.getResultList();
         Assert.assertEquals(0, l.size());
         Context.sessionScope().setApprovalUser(null);
      } finally {
         server.stop();
      }
   }

   @Test
   public void rejectPersist6EyesWithApprovalUserWithEmail() throws Exception {
      log.info("start rejectPersist6EyesWithApprovalUserWithEmail()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.REJECT_INSERT,
            ControlEvent.FIRST_RELEASE_INSERT);
      SimpleSmtpServer server = SimpleSmtpServer.start(8854);

      try {
         Context.sessionScope().setUserAddress("USER@email.de");
         Context.sessionScope().setApprovalUser("fizz");
         Context.sessionScope().setApprovalAddress("fizz@email.de");
         TComplexEntity ce = createTComplexEntity();
         persist(ce);

         Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
         query.setParameter("owner", TENANT);
         List<TComplexEntity> l = query.getResultList();
         Assert.assertEquals(0, l.size());
         server.stop();
         Assert.assertEquals(1, server.getReceivedEmailSize());
         Iterator<SmtpMessage> emailIter = server.getReceivedEmail();
         SmtpMessage email = emailIter.next();
         Assert.assertEquals("Cibet Notification: FIRST_POSTPONED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@ttest.de", email.getHeaderValue("From"));
         Assert.assertEquals("fizz@email.de", email.getHeaderValue("To"));

         server = SimpleSmtpServer.start(8854);
         List<Controllable> l1 = DcLoader.findUnreleased();
         Assert.assertEquals(1, l1.size());
         Controllable co = l1.get(0);
         Context.sessionScope().setUser("fizz");
         Context.sessionScope().setApprovalUser("Muzzi");
         Context.sessionScope().setApprovalAddress("Muzzi@email.de");
         ut.begin();
         co.release(applEman, "blabla1");
         ut.commit();
         server.stop();
         Assert.assertEquals(2, server.getReceivedEmailSize());
         emailIter = server.getReceivedEmail();
         while (emailIter.hasNext()) {
            email = emailIter.next();
            Assert.assertEquals("from@ttest.de", email.getHeaderValue("From"));
            if ("Cibet Notification: FIRST_RELEASED".equals(email.getHeaderValue("Subject"))) {
               Assert.assertEquals("USER@email.de", email.getHeaderValue("To"));

            } else if ("Cibet Notification: POSTPONED".equals(email.getHeaderValue("Subject"))) {
               Assert.assertEquals("Muzzi@email.de", email.getHeaderValue("To"));

            } else {
               Assert.fail();
            }
         }

         l1 = DcLoader.findUnreleased();
         Assert.assertEquals(1, l1.size());
         co = l1.get(0);
         Assert.assertEquals("fizz", co.getFirstApprovalUser());
         Assert.assertEquals(ControlEvent.INSERT, co.getControlEvent());
         Assert.assertEquals(USER, co.getCreateUser());

         l = query.getResultList();
         Assert.assertEquals(0, l.size());

         Context.sessionScope().setUser("Wolli");
         try {
            ut.begin();
            co.reject(applEman, "blabla2");
            Assert.fail();
         } catch (Exception e) {
            ut.rollback();
         }

         server = SimpleSmtpServer.start(8854);
         Context.sessionScope().setUser(USER);
         ut.begin();
         co.reject(applEman, "blabla2");
         ut.commit();
         server.stop();
         Assert.assertEquals(1, server.getReceivedEmailSize());
         emailIter = server.getReceivedEmail();
         email = emailIter.next();
         Assert.assertEquals("Cibet Notification: REJECTED", email.getHeaderValue("Subject"));
         Assert.assertEquals("from@ttest.de", email.getHeaderValue("From"));
         Assert.assertEquals("USER@email.de", email.getHeaderValue("To"));

         l1 = DcLoader.findUnreleased();
         Assert.assertEquals(0, l1.size());

         l = query.getResultList();
         Assert.assertEquals(0, l.size());
         Context.sessionScope().setApprovalUser(null);
      } finally {
         server.stop();
         Context.sessionScope().setUserAddress(null);
         Context.sessionScope().setApprovalAddress(null);
         Context.sessionScope().setApprovalUser(null);
      }
   }

   @Test
   public void rejectRemove4EyesNoActuators() throws Exception {
      log.info("start rejectRemove4EyesNoActuators()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.DELETE);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);
      remove(ce);

      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      ut.begin();
      co.reject(applEman, "blabla1");
      ut.commit();

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(0, l1.size());

      Query query = applEman.createNamedQuery(TComplexEntity.SEL_BY_OWNER);
      query.setParameter("owner", TENANT);
      List<TComplexEntity> l = query.getResultList();
      Assert.assertEquals(1, l.size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void rejectWithNoUser() throws ResourceApplyException {
      Context.sessionScope().setUser(null);
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.reject(applEman, "dfdf");
   }

   @Test(expected = InvalidUserException.class)
   public void reject6EyesWithNoUser() throws ResourceApplyException {
      Context.sessionScope().setUser(null);
      Configuration.instance().reinitAuthenticationProvider(null);
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      sd.reject(applEman, "dfdf");
   }

   @Test(expected = InvalidUserException.class)
   public void reject6EyesInvalidUser() throws ResourceApplyException {
      Context.sessionScope().setUser("II");
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      sd.setFirstApprovalUser("nn");
      sd.reject(applEman, "dfdf");
   }

   @Test(expected = InvalidUserException.class)
   public void releaseWithNoUser() throws ResourceApplyException, Exception {
      Context.sessionScope().setUser(null);
      Configuration.instance().reinitAuthenticationProvider(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(FourEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.POSTPONED);
      ut.begin();
      try {
         sd.release(applEman, "dfdf");
      } catch (Exception e) {
         ut.rollback();
         throw e;
      }
   }

   @Test(expected = InvalidUserException.class)
   public void release6EyesWithNoUser() throws ResourceApplyException, Exception {
      Context.sessionScope().setUser(null);
      Configuration.instance().reinitAuthenticationProvider(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      ut.begin();
      try {
         sd.release(applEman, "dfdf");
      } catch (Exception e) {
         ut.rollback();
         throw e;
      }
   }

   @Test(expected = InvalidUserException.class)
   public void release6EyesSameUser() throws ResourceApplyException, Exception {
      Context.sessionScope().setUser("US");
      Configuration.instance().registerAuthenticationProvider(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      sd.setCreateUser("US");
      ut.begin();
      try {
         sd.release(applEman, "dfdf");
      } catch (Exception e) {
         ut.rollback();
         throw e;
      }
   }

   @Test(expected = ResourceApplyException.class)
   public void release6EyesNotPostponed() throws Exception {
      Context.sessionScope().setUser("xx");
      Configuration.instance().registerAuthenticationProvider(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(SixEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.REJECTED);
      ut.begin();
      try {
         sd.release(applEman, "dfdf");
      } catch (Exception e) {
         ut.rollback();
         throw e;
      }
   }

   @Test(expected = ResourceApplyException.class)
   public void releaseWithWrongStatus() throws Exception {
      Context.sessionScope().setUser(null);
      Configuration.instance().registerAuthenticationProvider(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(FourEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.EXECUTED);
      ut.begin();
      try {
         sd.release(applEman, "dfdf");
      } catch (Exception e) {
         ut.rollback();
         throw e;
      }
   }

   @Test(expected = ResourceApplyException.class)
   public void releaseWithWrongStatus2ManRule() throws ResourceApplyException, Exception {
      log.debug("start releaseWithWrongStatus2ManRule");
      Context.sessionScope().setUser(null);
      Configuration.instance().registerAuthenticationProvider(null);
      log.debug("CibetContext.getUser(): " + Context.sessionScope().getUser());
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      sd.setActuator(TwoManRuleActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.EXECUTED);
      ut.begin();
      try {
         sd.release(applEman, "dfdf");
      } catch (Exception e) {
         log.info(e.getMessage(), e);
         ut.rollback();
         throw e;
      }
   }

   @Test
   public void afterEventWithWrongEvent2ManRule() throws ResourceApplyException {
      EventMetadata md = new EventMetadata(ControlEvent.ALL, null);
      TwoManRuleActuator act = new TwoManRuleActuator();
      try {
         act.afterEvent(md);
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertTrue(e.getMessage().endsWith("is an abstract ControlEvent"));
      }
   }

   @Test(expected = IllegalArgumentException.class)
   public void releaseInvalidEvent() throws ResourceApplyException, Exception {
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.REDO);
      sd.setActuator(FourEyesActuator.DEFAULTNAME);
      sd.setExecutionStatus(ExecutionStatus.POSTPONED);
      ut.begin();
      try {
         sd.release(applEman, "dfdf");
      } catch (Exception e) {
         ut.rollback();
         throw e;
      }
   }

   @Test
   public void compare() throws Exception {
      log.info("start compare()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.UPDATE);

      TEntity entity = createTEntity(5, "Schneider");
      persist(entity);
      TEntity selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      selEnt.setCounter(12);
      selEnt.setNameValue("newYY");
      merge(selEnt);

      selEnt = applEman.find(TEntity.class, entity.getId());
      Assert.assertNotNull("entity with id " + entity.getId() + " not found", selEnt);
      Assert.assertEquals(5, selEnt.getCounter());

      List<Controllable> unreleased = DcLoader.findUnreleased();
      Assert.assertTrue(unreleased.size() == 1);

      Controllable coObj = unreleased.get(0);
      List<Difference> list = DcLoader.differences(coObj);
      // DcLoader.compare(applEman, coObj.getResource());
      Assert.assertTrue(list.size() == 2);
      Difference cou = list.get(0);
      Difference nv = list.get(1);
      if (list.get(0).getPropertyName().equals("counter")) {
         // okay
      } else if (list.get(0).getPropertyName().equals("nameValue")) {
         cou = list.get(1);
         nv = list.get(0);
      } else {
         Assert.fail();
      }
      Assert.assertEquals("counter", cou.getPropertyName());
      Assert.assertEquals(DifferenceType.MODIFIED, cou.getDifferenceType());
      Assert.assertEquals(new Integer(5), cou.getOldValue());
      Assert.assertEquals(new Integer(12), cou.getNewValue());

      Assert.assertEquals("nameValue", nv.getPropertyName());
      Assert.assertEquals(DifferenceType.MODIFIED, nv.getDifferenceType());
      Assert.assertEquals("Schneider", nv.getOldValue());
      Assert.assertEquals("newYY", nv.getNewValue());
   }

   @Test(expected = IllegalArgumentException.class)
   public void compareNotUpdatedObjects() {
      Controllable sd = new Controllable();
      sd.setControlEvent(ControlEvent.DELETE);
      DcLoader.differences(sd);
   }

   @Test
   public void releaseSelect6Eyes() throws Exception {
      log.info("start releaseSelect6Eyes()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(SixEyesActuator.DEFAULTNAME);
      registerSetpoint(TComplexEntity.class.getName(), schemes, ControlEvent.SELECT, ControlEvent.RELEASE_SELECT,
            ControlEvent.FIRST_RELEASE_SELECT);

      TComplexEntity ce = createTComplexEntity();
      persist(ce);

      Context.requestScope().setRemark("Heinz");
      ut.begin();
      TComplexEntity tce = applEman.find(TComplexEntity.class, ce.getId());
      ut.commit();
      Assert.assertNull(tce);

      // first release
      List<Controllable> l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      Controllable co = l1.get(0);
      Context.requestScope().setRemark("Manni");
      TComplexEntity tce2 = (TComplexEntity) release(co);
      Assert.assertNull(tce2);

      l1 = DcLoader.findUnreleased();
      Assert.assertEquals(1, l1.size());
      co = l1.get(0);
      Assert.assertEquals("tester2", co.getFirstApprovalUser());
      Assert.assertEquals(ControlEvent.SELECT, co.getControlEvent());
      Assert.assertEquals(USER, co.getCreateUser());
      Assert.assertEquals("Heinz", co.getCreateRemark());
      Assert.assertEquals("blabla", co.getFirstApprovalRemark());

      // 2. release
      try {
         // invalid user
         ut.begin();
         co.release(applEman, "blabla2");
         Assert.fail();
      } catch (InvalidUserException e) {
         ut.rollback();
      }

      Context.sessionScope().setUser("tester3");
      ut.begin();
      TComplexEntity tce3 = (TComplexEntity) co.release(applEman, "blabla2");
      ut.commit();
      Assert.assertEquals(ce.getId(), tce3.getId());
      Assert.assertEquals(ce.getEagerList().size(), tce3.getEagerList().size());
   }

   @Test
   public void passBackpersist() throws Exception {
      log.info("start passBackpersist()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE,
            ControlEvent.PASSBACK, ControlEvent.SUBMIT);
      Context.requestScope().setRemark("created");

      TEntity ent = createTEntity(5, "valuexx");
      persist(ent);
      Assert.assertEquals(0, ent.getId());

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());
      Controllable co = l.get(0);
      Assert.assertEquals("created", co.getCreateRemark());

      Context.sessionScope().setUser("test2");
      ut.begin();
      co.passBack(applEman, null);
      ut.commit();

      l = DcLoader.findUnreleased();
      Assert.assertEquals(0, l.size());

      l = DcLoader.loadByUser(USER);
      Assert.assertEquals(1, l.size());
      Assert.assertEquals(ExecutionStatus.PASSEDBACK, l.get(0).getExecutionStatus());
      Assert.assertEquals("test2", l.get(0).getReleaseUser());

      Context.sessionScope().setUser("test3");
      try {
         ut.begin();
         l.get(0).submit(applEman, "2.submit");
         Assert.fail();
      } catch (InvalidUserException e) {
         ut.rollback();
      }

      Context.sessionScope().setUser(USER);
      ut.begin();
      l.get(0).submit(applEman, "2.submit");
      ut.commit();
      l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.sessionScope().setUser("fizz");
      ut.begin();
      Object res = l.get(0).release(applEman, "rel");
      ut.commit();
      TEntity te = applEman.find(TEntity.class, ((TEntity) res).getId());
      Assert.assertNotNull(te);
      Context.requestScope().setRemark(null);
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

      List<Controllable> l = DcLoader.findUnreleased();
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

      List<Controllable> l = DcLoader.findUnreleased();
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

      List<Controllable> l = DcLoader.findUnreleased();
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

      List<Controllable> l = DcLoader.findUnreleased();
      Assert.assertEquals(1, l.size());

      Context.requestScope().setCaseId(er.getCaseId());
      Context.sessionScope().setUser("otherUser");
      te = remoteEjb.storeTEntityParallel(te);
      Assert.assertEquals(80, te.getCounter());

      l = DcLoader.findUnreleased();
      Assert.assertEquals(2, l.size());

      Context.sessionScope().setUser("releaseUser");
      TEntity result = (TEntity) release(l.get(1));
      Assert.assertEquals(81, result.getCounter());

      List<TEntity> list = loadTEntities();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(80 + 1, list.get(0).getCounter());
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

      List<Controllable> l = DcLoader.findUnreleased();
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

      TEntity result = (TEntity) release(l.get(0));
      Assert.assertEquals(counter + 1, result.getCounter());

      Thread.sleep(100);

      List<TEntity> list = loadTEntities();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(counter + 1, list.get(0).getCounter());
   }

   @Test
   public void lockObject() throws Exception {
      log.info("start lockObject()");

      TEntity te = createTEntity(5, "Klaus");
      persist(te);

      ut.begin();
      Controllable lo = Locker.lock(te, ControlEvent.DELETE, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
      Assert.assertEquals("testremark", l2.get(0).getCreateRemark());
      Assert.assertEquals(USER, l2.get(0).getCreateUser());
      Assert.assertEquals(TEntity.class.getName(), ((JpaResource) l2.get(0).getResource()).getTarget());
   }

   @Test
   public void lockObjectLockedSameUser() throws Exception {
      log.info("start lockObjectLockedSameUser()");
      Context.sessionScope().setTenant("LOCKTENANT");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.DELETE);

      TEntity te = createTEntity(5, "Klaus");
      persist(te);
      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());

      ut.begin();
      Controllable lo = Locker.lock(te, ControlEvent.DELETE, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      remove(te);
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
   public void lockObjectLockedOtherUser() throws Exception {
      log.info("start lockObjectLockedOtherUser()");
      Context.sessionScope().setTenant("LOCKTENANT");

      TEntity te = createTEntity(5, "Klaus");
      persist(te);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      ut.begin();
      Controllable lo = Locker.lock(te, ControlEvent.DELETE, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      remove(te);
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
   public void lockClassLockedOtherUser() throws Exception {
      log.info("start lockClassLockedOtherUser()");
      Context.sessionScope().setTenant("LOCKTENANT");

      TEntity te = createTEntity(5, "Klaus");
      persist(te);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      ut.begin();
      Controllable lo = Locker.lock(TEntity.class, ControlEvent.DELETE, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      remove(te);
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
   public void lockObjectInsert() throws Exception {
      log.info("start lockObjectInsert()");
      Context.sessionScope().setTenant("LOCKTENANT");

      TEntity te = createTEntity(5, "Klaus");
      persist(te);

      ut.begin();
      Controllable lo = Locker.lock(te, ControlEvent.INSERT, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      List<Controllable> l2 = Locker.loadLockedObjects();
      Assert.assertEquals(1, l2.size());
      log.debug("LOCKEDOBJECT: " + l2.get(0));
      Assert.assertEquals("testremark", l2.get(0).getCreateRemark());
      Assert.assertEquals(USER, l2.get(0).getCreateUser());
      Assert.assertEquals(TEntity.class.getName(), ((JpaResource) l2.get(0).getResource()).getTarget());
   }

   @Test
   public void lockObjectLockedSameUserInsert() throws Exception {
      log.info("start lockObjectLockedSameUserInsert()");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT);

      Context.sessionScope().setTenant("LOCKTENANT");

      TEntity te = createTEntity(5, "Klaus");
      persist(te);

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(1, res.size());

      ut.begin();
      Controllable lo = Locker.lock(te, ControlEvent.INSERT, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      TEntity te2 = createTEntity(6, "Klaus2");
      persist(te2);
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
   public void lockObjectLockedOtherUserInsert() throws Exception {
      log.info("start lockObjectLockedOtherUserInsert()");

      Context.sessionScope().setTenant("LOCKTENANT");

      TEntity te = createTEntity(5, "Klaus");
      persist(te);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      ut.begin();
      Controllable lo = Locker.lock(te, ControlEvent.INSERT, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");

      TEntity te2 = createTEntity(7, "Klaus3");
      persist(te2);
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
   public void lockClassLockedOtherUserInsert() throws Exception {
      log.info("start lockClassLockedOtherUserInsert()");

      Context.sessionScope().setTenant("LOCKTENANT");

      TEntity te = createTEntity(5, "Klaus");
      persist(te);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      ut.begin();
      Controllable lo = Locker.lock(TEntity.class, ControlEvent.INSERT, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");
      Context.sessionScope().setTenant("LOCKTENANT");

      TEntity te2 = createTEntity(5, "Klaus");
      persist(te2);
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

      Context.sessionScope().setTenant("LOCKTENANT");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST, ControlEvent.RELEASE);

      Context.sessionScope().setUser("user1");
      ut.begin();
      Controllable lo = Locker.lock(TEntity.class, ControlEvent.RELEASE, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);
      Context.sessionScope().setUser(USER);

      TEntity te = createTEntity(5, "Klaus");
      persist(te);
      List<Controllable> unr = DcLoader.findUnreleased();
      Assert.assertEquals(1, unr.size());

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(0, res.size());

      release(unr.get(0));
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = DcLoader.findUnreleased();
      Assert.assertEquals(1, unr.size());

      res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      ut.begin();
      unr.get(0).release(applEman, "first try");
      ut.commit();
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
      Context.sessionScope().setTenant("LOCKTENANT");

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.RELEASE_INSERT);

      TEntity te = createTEntity(5, "Klaus");
      persist(te);
      List<Controllable> unr = DcLoader.findUnreleased();
      Assert.assertEquals(1, unr.size());

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      ut.begin();
      Controllable lo = Locker.lock(te, ControlEvent.RELEASE, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      log.debug("user2");
      release(unr.get(0));
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = DcLoader.findUnreleased();
      Assert.assertEquals(1, unr.size());

      res = q.getResultList();
      Assert.assertEquals(0, res.size());

      Context.sessionScope().setUser("user1");
      ut.begin();
      unr.get(0).release(applEman, "first try");
      ut.commit();
      Assert.assertEquals(ExecutionStatus.EXECUTED,
            Context.requestScope().getExecutedEventResult().getExecutionStatus());

      unr = DcLoader.findUnreleased();
      Assert.assertEquals(0, unr.size());

      res = q.getResultList();
      Assert.assertEquals(1, res.size());
   }

   @Test
   public void lockObjectSelectLockedOtherUser() throws Exception {
      log.info("start lockObjectSelectLockedOtherUser()");
      Context.sessionScope().setTenant("LOCKTENANT");

      TEntity te = createTEntity(5, "Klaus");
      persist(te);

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.PERSIST);

      ut.begin();
      Controllable lo = Locker.lock(te, ControlEvent.SELECT, "testremark");
      ut.commit();
      Assert.assertNotNull(lo);

      Context.sessionScope().setUser("otherUser");

      ut.begin();
      TEntity te2 = applEman.find(TEntity.class, te.getId());
      ut.commit();
      Assert.assertEquals(ExecutionStatus.DENIED, Context.requestScope().getExecutedEventResult().getExecutionStatus());
      Assert.assertNull(te2);

      List<Archive> list = ArchiveLoader.loadArchives(TEntity.class.getName());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());
   }

}
