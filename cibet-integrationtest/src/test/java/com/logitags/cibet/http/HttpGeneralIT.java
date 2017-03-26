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
package com.logitags.cibet.http;

import java.io.File;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.DBHelper;
import com.cibethelper.base.NoControlActuator;
import com.cibethelper.base.SubArchiveController;
import com.cibethelper.ejb.Ejb2Service;
import com.cibethelper.ejb.EjbService;
import com.cibethelper.ejb.OutService;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.GeneralServlet;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.sensor.pojo.SpringBeanInvoker;

@RunWith(Arquillian.class)
public class HttpGeneralIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpGeneralIT.class);

   protected static final String SEL_DCCONTROLLABLE = "SELECT c FROM DcControllable c WHERE c.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED";

   private DBHelper dbHelper = new DBHelper();

   private EntityManager localcibet;

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = HttpGeneralIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-general.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, TEntity.class, TComplexEntity.class,
            ITComplexEntity.class, AbstractTEntity.class, TCompareEntity.class, GeneralServlet.class, OutService.class,
            EjbService.class, Ejb2Service.class, SubArchiveController.class, NoControlActuator.class,
            TComplexEntity2.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);
      File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-springsecurity")
            .withTransitivity().asFile();
      archive.addAsLibraries(spring);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("spring-context_2.xml", "classes/spring-context.xml");
      archive.addAsWebInfResource("it/config_springsecurity.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("META-INF/aop-general.xml", "classes/META-INF/aop.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @BeforeClass
   public static void beforeClassHttpCibetContextFilterIT() throws Exception {
      DBHelper.beforeClass();
   }

   @Before
   public void beforeHttpGeneralIT() {
      InitializationService.instance().startContext();
      localcibet = Context.requestScope().getEntityManager();
   }

   @Override
   @After
   public void afterAbstractArquillian() throws Exception {
      log.info("execute sub-afterAbstractArquillian(");
      HttpGet method = new HttpGet(getBaseURL() + "/logout");
      client.execute(method);
      method.abort();

      InitializationService.instance().endContext();

      dbHelper.doAfter();
   }

   @Test
   public void testPojoInvoke() throws Exception {
      log.info("start testPojoInvoke()");
      String body = executeGET(getBaseURL() + "/logThis.cibet?MSG=Hallo!");
      Assert.assertEquals("Answer: null", body);

      log.info("************** now log in with Spring ...");
      body = executeGET(getBaseURL() + "/loginSpring.cibet?USER=Kim&ROLE=MANAGER");
      Assert.assertEquals("Login done for user Kim", body);

      log.info("************** now showSession ...");
      body = executeGET(getBaseURL() + "/showSession.cibet");
      Assert.assertEquals("after Login: User: Kim; Tenant: __DEFAULT;  - -", body);

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now showSession after logout ...");
      body = executeGET(getBaseURL() + "/showSession.cibet");
      Assert.assertEquals("after Login: User: null; Tenant: __DEFAULT;  - -", body);

      log.info("************** now log in ...");
      body = executeGET(getBaseURL() + "/login.cibet?USER=Fritz");
      Assert.assertEquals("Login done for user Fritz", body);

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now log in with Spring ...");
      body = executeGET(getBaseURL() + "/loginSpring.cibet?USER=Kim&ROLE=MANAGER");
      Assert.assertEquals("Login done for user Kim", body);

      log.info("************** now showSession ...");
      body = executeGET(getBaseURL() + "/showSession.cibet");
      Assert.assertEquals("after Login: User: Kim; Tenant: __DEFAULT;  - -", body);

      log.info("************** log message ...");
      body = executeGET(getBaseURL() + "/logThis.cibet?MSG=Hallo!");
      Assert.assertEquals("Answer: Hallo!", body);
   }

   @Test
   @CibetContext
   public void testScheduleUpdate() throws Exception {
      log.info("start testScheduleUpdate()");

      TEntity t1 = new TEntity("Stung1", 1, "owner1");
      TEntity t2 = new TEntity("Stung2", 2, "owner2");
      TEntity t3 = new TEntity("Stung3", 3, "owner3");

      TComplexEntity base = new TComplexEntity();
      base.setCompValue(45);
      base.setTen(t1);
      base.getLazyList().add(t2);
      base.getLazyList().add(t3);
      dbHelper.persist(base);

      log.info("************** schedule update ...");
      try {
         String body = executeGET(getBaseURL() + "/schedule.cibet?id=" + base.getId());
         Assert.assertEquals("Answer: Okay!", body);
      } finally {
         List<TComplexEntity> l = (List<TComplexEntity>) dbHelper.select("select a from TComplexEntity a");
         Assert.assertEquals(1, l.size());
         TComplexEntity te2 = l.get(0);

         log.debug(te2);
         log.debug("AAAA:: " + te2.getEagerList().size());

         Query q = localcibet.createQuery("select a from DcControllable a");
         DcControllable dc = (DcControllable) q.getSingleResult();
         log.debug(dc);
         log.debug("ExecutionDate:" + dc.getExecutionDate());
      }
   }

   @Test
   public void testPersistAndRelease() throws Exception {
      log.info("start testPersistAndRelease()");

      String body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now log in with Spring ...");
      body = executeGET(getBaseURL() + "/loginSpring.cibet?USER=Kim&ROLE=MANAGER");
      Assert.assertEquals("Login done for user Kim", body);

      log.info("************** now persist denied ...");
      log.debug("tenant: " + Context.internalSessionScope().getTenant());
      body = executeGET(getBaseURL() + "/persist.cibet", HttpStatus.SC_INTERNAL_SERVER_ERROR);
      Assert.assertTrue(body.indexOf("com.logitags.cibet.actuator.common.DeniedEjbException: Access is denied") > 0);

      log.debug("tenant: " + Context.internalSessionScope().getTenant());
      Query q = localcibet.createQuery("select a from Archive a");
      List<Archive> list = q.getResultList();

      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

      localcibet.remove(list.get(0));

      InitializationService.instance().endContext();
      InitializationService.instance().startContext();
      localcibet = Context.requestScope().getEntityManager();

      List<TEntity> tentList = (List<TEntity>) dbHelper.select("SELECT e FROM TEntity e");
      Assert.assertEquals(0, tentList.size());

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now log in ...");
      body = executeGET(getBaseURL() + "/loginSpring.cibet?USER=Kim&ROLE=MANAGER&Freds=true");
      Assert.assertEquals("Login done for user Kim", body);

      log.info("************** now persist accepted ...");
      body = executeGET(getBaseURL() + "/persist.cibet");
      Assert.assertEquals("TEntity persist with ID: 0", body);

      q = localcibet
            .createQuery("select a from Archive a where a.resource.targetType ='" + TEntity.class.getName() + "'");
      list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(ExecutionStatus.POSTPONED, list.get(0).getExecutionStatus());
      Assert.assertEquals("0", ((Archive) list.get(0)).getResource().getPrimaryKeyId());

      tentList = (List<TEntity>) dbHelper.select("SELECT e FROM TEntity e");
      Assert.assertEquals(0, tentList.size());

      q = localcibet.createQuery(SEL_DCCONTROLLABLE);
      List<DcControllable> dcList = q.getResultList();
      Assert.assertEquals(1, dcList.size());
      Assert.assertEquals(ExecutionStatus.POSTPONED, dcList.get(0).getExecutionStatus());

      log.info("************** now release denied ...");
      body = executeGET(getBaseURL() + "/releasePersist.cibet", HttpStatus.SC_INTERNAL_SERVER_ERROR);
      Assert.assertTrue(body.indexOf(
            "InvalidUserException: release failed: user id Kim of releasing user is equal to the user id of the initial user Kim") > 0);

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now log in ...");
      body = executeGET(getBaseURL() + "/loginSpring.cibet?USER=Henny&ROLE=SIMPLEUSER");
      Assert.assertEquals("Login done for user Henny", body);

      log.info("************** now release denied ...");
      body = executeGET(getBaseURL() + "/releasePersist.cibet");
      Assert.assertEquals("TEntity not released", body);

      q = localcibet.createQuery(SEL_DCCONTROLLABLE);
      dcList = q.getResultList();
      Assert.assertEquals(1, dcList.size());
      Assert.assertEquals(ExecutionStatus.POSTPONED, dcList.get(0).getExecutionStatus());

      tentList = (List<TEntity>) dbHelper.select("SELECT e FROM TEntity e");
      Assert.assertEquals(0, tentList.size());

      q = localcibet
            .createQuery("select a from Archive a where a.resource.targetType ='" + TEntity.class.getName() + "'");
      list = q.getResultList();
      Assert.assertEquals(2, list.size());
      Assert.assertEquals(ExecutionStatus.DENIED, list.get(1).getExecutionStatus());
      Assert.assertEquals("Henny", list.get(1).getCreateUser());

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now log in ...");
      body = executeGET(getBaseURL() + "/loginSpring.cibet?USER=Fluppi&ROLE=MANAGER");
      Assert.assertEquals("Login done for user Fluppi", body);

      log.info("************** now release accepted ...");
      body = executeGET(getBaseURL() + "/releasePersist.cibet");
      Assert.assertTrue(body.startsWith("TEntity released with ID:"));

      int index = body.indexOf(":");
      String id = body.substring(index + 2);
      log.info("search TEntity and DcControllable with ID " + id);

      tentList = (List<TEntity>) dbHelper.select("SELECT e FROM TEntity e WHERE e.id=" + Long.parseLong(id));
      Assert.assertEquals(1, tentList.size());

      localcibet.clear();
      q = localcibet.createQuery("SELECT e FROM DcControllable e WHERE e.resource.primaryKeyId = :id");
      q.setParameter("id", id);
      dcList = q.getResultList();
      Assert.assertEquals(1, dcList.size());
      log.debug(dcList.get(0));
      Assert.assertEquals(ExecutionStatus.EXECUTED, dcList.get(0).getExecutionStatus());
      Assert.assertEquals("good!", dcList.get(0).getApprovalRemark());
      Assert.assertEquals("Fluppi", dcList.get(0).getApprovalUser());

      q = localcibet.createQuery("select a from Archive a where a.resource.targetType ='" + TEntity.class.getName()
            + "' ORDER BY a.createDate");
      list = (List<Archive>) q.getResultList();
      Assert.assertEquals(3, list.size());

      Assert.assertEquals(ExecutionStatus.EXECUTED, list.get(2).getExecutionStatus());
      Assert.assertEquals("Fluppi", list.get(2).getCreateUser());
      Assert.assertEquals(list.get(0).getCaseId(), list.get(1).getCaseId());
      Assert.assertEquals(list.get(0).getCaseId(), list.get(2).getCaseId());
      Assert.assertEquals(id, ((Archive) list.get(0)).getResource().getPrimaryKeyId());
      Assert.assertEquals(id, ((Archive) list.get(1)).getResource().getPrimaryKeyId());
      Assert.assertEquals(id, ((Archive) list.get(2)).getResource().getPrimaryKeyId());
   }

   @Test
   public void testFilter() throws Exception {
      log.info("start testFilter()");

      String body = executeGET(getBaseURL() + "/login.cibet?USER=Jens&tenant=XYCompany");
      Assert.assertEquals("Login done for user Jens", body);

      log.info("************** now URL forbidden 1 ...");
      executeGET(getBaseURL() + "/logThis.url", HttpStatus.SC_FORBIDDEN);
      log.info("start loading archives");

      Query q = localcibet.createQuery("select a from Archive a where a.tenant ='XYCompany'");
      List<Archive> list = q.getResultList();
      Assert.assertEquals(1, list.size());

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now URL no setpoint ...");
      body = executeGET(getBaseURL() + "/logThis.url?color=green");
      Assert.assertEquals("Answer: EjbService.logThis called with: Hallo Cibet", body);
      q = localcibet.createQuery("SELECT a FROM Archive a");
      list = q.getResultList();
      Assert.assertEquals(1, list.size());

      body = executeGET(getBaseURL() + "/logThis.url?color=red");
      Thread.sleep(1000);
      Assert.assertEquals("Answer: EjbService.logThis called with: Hallo Cibet", body);
      list = q.getResultList();
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void testFilter2Man() throws Exception {
      log.info("start testFilter2Man()");

      log.info("************** now logout ...");
      String body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now log in ...");
      body = executeGET(getBaseURL() + "/login.cibet?USER=Fritz");
      Assert.assertEquals("Login done for user Fritz", body);

      log.info("************** now URL 2-man ...");
      executeGET(getBaseURL() + "/logThis.url?color=blue", HttpStatus.SC_ACCEPTED);
      Query q = localcibet.createQuery("SELECT a FROM Archive a");
      List<Archive> list = q.getResultList();
      Assert.assertEquals(1, list.size());

      q = localcibet.createQuery("SELECT d FROM DcControllable d");
      List<DcControllable> dcl = q.getResultList();
      Assert.assertEquals(1, dcl.size());
      Assert.assertEquals(ExecutionStatus.POSTPONED, dcl.get(0).getExecutionStatus());

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now log in ...");
      body = executeGET(getBaseURL() + "/login.cibet?USER=Holger");
      Assert.assertEquals("Login done for user Holger", body);

      log.info("************** now log in second...");
      body = executeGET(getBaseURL() + "/login.cibet?USER=Wil&second=true");
      Assert.assertEquals("second Login done for user Wil", body);

      log.info("************** now release failed...");
      body = executeGET(getBaseURL() + "/releaseHttp.cibet", HttpStatus.SC_INTERNAL_SERVER_ERROR);
      int index1 = body.indexOf("InvalidUserException: release failed: the actual authenticated user is not equal");
      Assert.assertTrue(index1 > 0);

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);

      log.info("************** now log in ...");
      body = executeGET(getBaseURL() + "/login.cibet?USER=Fritz");
      Assert.assertEquals("Login done for user Fritz", body);

      log.info("************** now log in second...");
      body = executeGET(getBaseURL() + "/login.cibet?USER=Wil&second=true");
      Assert.assertEquals("second Login done for user Wil", body);

      log.info("************** now release ...");
      body = executeGET(getBaseURL() + "/releaseHttp.cibet");
      Assert.assertEquals("Answer: EjbService.logThis called with: Hallo Cibet", body);

      localcibet.clear();
      q = localcibet.createQuery("SELECT d FROM DcControllable d");
      dcl = q.getResultList();
      Assert.assertEquals(1, dcl.size());
      Assert.assertEquals(ExecutionStatus.EXECUTED, dcl.get(0).getExecutionStatus());
   }

   @Test
   public void testAspect() throws Exception {
      log.info("start testAspect()");
      String body = executeGET(getBaseURL() + "/aspect.cibet");
      log.debug(body);

      Query q = localcibet.createQuery("SELECT e FROM EventResult e");
      List<EventResult> erlist = q.getResultList();
      Assert.assertEquals(1, erlist.size());
      log.debug(erlist.get(0));
   }

   @Test
   public void testEjbAspect() throws Exception {
      log.info("start testEjbAspect()");
      String body = executeGET(getBaseURL() + "/aspectEJB.cibet");
      log.debug(body);

      Query q = localcibet.createQuery("SELECT e FROM EventResult e WHERE e.parentResult IS NULL");
      List<EventResult> erlist = q.getResultList();
      Assert.assertEquals(1, erlist.size());
      log.debug(erlist.get(0));
      Assert.assertTrue(erlist.get(0).toString().indexOf("[EjbResource]") > 0);
   }

   @Test
   public void testSpringAspect() throws Exception {
      log.info("start testSpringAspect()");

      String body = executeGET(getBaseURL() + "/aspectSpring.cibet");
      log.debug(body);

      Query q = localcibet.createQuery("SELECT e FROM EventResult e WHERE e.parentResult IS NULL");
      List<EventResult> erlist = q.getResultList();
      Assert.assertEquals(1, erlist.size());
      log.debug(erlist.get(0));
      Assert.assertTrue(erlist.get(0).toString().indexOf(SpringBeanInvoker.class.getName()) > 0);
   }

   @Test
   public void testMergeLazyException() throws Exception {
      log.info("start testMergeLazyException()");

      String body = executeGET(getBaseURL() + "/login.cibet?USER=Jens&tenant=XYCompany");
      Assert.assertEquals("Login done for user Jens", body);

      log.info("************** merge ...");
      body = executeGET(getBaseURL() + "/merge.cibet");
      Assert.assertEquals("merge chain done", body);

      Query q = localcibet.createQuery("select a from Archive a where a.tenant ='XYCompany' order by a.createDate");
      List<Archive> list = q.getResultList();
      log.info(list.size() + " Archives loaded");
      // JBoss makes LazyInitializationException: only 1 archive
      // Glassfish can make loadLazyEntities in ArchiveActuator, even if EM is not same session: 2 Archives
      if (list.size() == 2) {
         // Resource res = list.get(1).getResource();
         // log.debug("Resource: " + res);
         // TComplexEntity tc = (TComplexEntity) res.getObject();
         // log.debug("lazy size: " + tc.getLazyList().size());
         // if (tc.getLazyList().size() > 0) {
         // log.debug("TEntity counter: " + tc.getLazyList().iterator().next().getCounter());
         // }
      }
      // Assert.assertEquals(1, list.size());

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);
   }

   @Test
   public void testMerge() throws Exception {
      log.info("start testMerge()");

      String body = executeGET(getBaseURL() + "/login.cibet?USER=Jens&tenant=XYCompany");
      Assert.assertEquals("Login done for user Jens", body);

      log.info("************** merge ...");
      body = executeGET(getBaseURL() + "/merge.cibet?lazy=loadLazy");
      Assert.assertEquals("merge chain done", body);

      Query q = localcibet.createQuery("select a from Archive a where a.tenant ='XYCompany' order by a.createDate");
      List<Archive> list = q.getResultList();
      Assert.assertEquals(2, list.size());
      // Resource res = list.get(1).getResource();
      // log.debug("lazy Resource: " + res);
      // TComplexEntity tc = (TComplexEntity) res.getObject();
      // log.debug("lazy size: " + tc.getLazyList().size());
      // if (tc.getLazyList().size() > 0) {
      // log.debug("TEntity counter: " + tc.getLazyList().iterator().next().getCounter());
      // }

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);
   }

   @Test
   public void testMergeLazyException4Eyes() throws Exception {
      log.info("start testMergeLazyException4Eyes()");

      String body = executeGET(getBaseURL() + "/login.cibet?USER=Jens&tenant=NEXT");
      Assert.assertEquals("Login done for user Jens", body);

      log.info("************** merge ...");
      body = executeGET(getBaseURL() + "/merge.cibet");
      Assert.assertEquals("merge chain done", body);

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");

      body = executeGET(getBaseURL() + "/login.cibet?USER=JensReleaser&tenant=NEXT");
      Assert.assertEquals("Login done for user JensReleaser", body);

      log.info("************** release ...");
      body = executeGET(getBaseURL() + "/releasePersist.cibet");
      Assert.assertEquals("TComplexEntity2 released", body);

      List<TComplexEntity2> cList = (List<TComplexEntity2>) dbHelper.select("SELECT e FROM TComplexEntity2 e");
      Assert.assertEquals(1, cList.size());
      log.debug(cList.get(0));

      log.info("************** now logout ...");
      body = executeGET(getBaseURL() + "/logout.cibet");
      Assert.assertEquals("Logout done", body);
   }

   @Test
   public void testPojoAspectInEJB() throws Exception {
      log.info("start testPojoAspectInEJB()");

      String body = executeGET(getBaseURL() + "/loginSpring.cibet?USER=Kim&ROLE=MANAGER");
      Assert.assertEquals("Login done for user Kim", body);
      log.info("************** now aspect ...");
      body = executeGET(getBaseURL() + "/aspectPojo.cibet");

      Query q = localcibet.createQuery("select a from Archive a");
      List<Archive> list = q.getResultList();

      Assert.assertEquals(1, list.size());
      log.debug(list.get(0));
      Assert.assertEquals(ExecutionStatus.EXECUTED, list.get(0).getExecutionStatus());
   }

   @Test
   public void testProxy() throws Exception {
      log.info("start testProxy()");

      String body = executeGET(getBaseURL() + "/loginSpring.cibet?USER=Kim&ROLE=MANAGER");
      Assert.assertEquals("Login done for user Kim", body);
      log.info("************** now proxy ...");
      body = executeGET(getBaseURL() + "/proxy.cibet");

      Query q = localcibet.createQuery("select a from Archive a");
      List<Archive> list = q.getResultList();

      Assert.assertEquals(1, list.size());
      log.debug(list.get(0));
      Assert.assertEquals(ExecutionStatus.EXECUTED, list.get(0).getExecutionStatus());
      Assert.assertEquals("Kim", list.get(0).getCreateUser());
   }

}
