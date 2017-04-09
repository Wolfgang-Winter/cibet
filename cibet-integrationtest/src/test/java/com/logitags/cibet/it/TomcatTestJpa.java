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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.DBHelper;
import com.cibethelper.ejb.OutService;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianSEServlet;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

@RunWith(Arquillian.class)
public class TomcatTestJpa extends DBHelper {

   private static Logger log = Logger.getLogger(TomcatTestJpa.class);

   protected CloseableHttpClient client = HttpClients.createDefault();

   protected static final String SEL_DCCONTROLLABLE = "SELECT c FROM DcControllable c WHERE c.executionStatus = com.logitags.cibet.core.ExecutionStatus.POSTPONED";

   @Deployment(name = "jpa", testable = false)
   public static WebArchive createDeployment() {
      String warName = TomcatTestJpa.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/webSE.xml");

      archive.addClasses(TEntity.class, ArquillianSEServlet.class, OutService.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, AbstractTEntity.class, ITComplexEntity.class,
            TCompareEntity.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      File[] shiro = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-shiro").withTransitivity()
            .asFile();
      archive.addAsLibraries(shiro);

      File[] shiro1 = Maven.resolver()
            .addDependencies(MavenDependencies.createDependency("org.apache.shiro:shiro-web:1.2.2", ScopeType.COMPILE,
                  false, MavenDependencies.createExclusion("org.slf4j:slf4j-api")))
            .resolve().withTransitivity().asFile();
      archive.addAsLibraries(shiro1);

      archive.delete("/WEB-INF/lib/slf4j-api-1.7.21.jar");

      archive.addAsManifestResource("it/context.xml", "context.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("META-INF/persistence-tomcat.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource("shiro.ini", "classes/shiro.ini");
      archive.addAsWebInfResource("it/config_shiro.xml", "classes/cibet-config.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeTomcatTestJdbc() {
      log.debug("execute before()");
      new ConfigurationService().initialise();
      InitializationService.instance().startContext();
      com.logitags.cibet.context.Context.sessionScope().setUser(USER);
      com.logitags.cibet.context.Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterTest() throws Exception {
      InitializationService.instance().endContext();
   }

   protected String getBaseURL() {
      return HTTPURL + this.getClass().getSimpleName();
   }

   protected String executeGET(String url) throws Exception {
      log.info("request: " + url);
      HttpGet get = new HttpGet(url);
      HttpResponse response = client.execute(get);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      return readResponseBody(response);
   }

   protected String readResponseBody(HttpResponse response) throws Exception {
      // Read the response body.
      HttpEntity entity = response.getEntity();
      InputStream instream = null;
      try {
         if (entity != null) {
            instream = entity.getContent();

            BufferedReader reader = new BufferedReader(new InputStreamReader(instream, "UTF-8"));
            String body = reader.readLine();
            log.info("body=" + body);
            return body;
         } else {
            return null;
         }
      } catch (IOException ex) {
         // In case of an IOException the connection will be released
         // back to the connection manager automatically
         throw ex;

      } catch (RuntimeException ex) {
         // In case of an unexpected exception you may want to abort
         // the HTTP request in order to shut down the underlying
         // connection and release it back to the connection manager.
         throw ex;

      } finally {
         // Closing the input stream will trigger connection release
         if (instream != null)
            instream.close();
         Thread.sleep(100);
      }
   }

   protected String executeGET(String url, int expected) throws Exception {
      HttpGet get = new HttpGet(url);
      HttpResponse response = client.execute(get);
      Assert.assertEquals(expected, response.getStatusLine().getStatusCode());
      return readResponseBody(response);
   }

   // @Override
   // @Test
   // public void testJdbc() throws Exception {
   // log.info("skip testJdbc()");
   // }

   @Test
   public void testPojoInvoke() throws Exception {
      log.info("start testPojoInvoke()");
      try {
         log.info("************** now logout ...");
         String body = executeGET(getBaseURL() + "/logout.cibet");
         Assert.assertEquals("Logout done", body);

         log.info("************** not allowed ...");
         body = executeGET(getBaseURL() + "/logThis.cibet?MSG=Hallo!");
         Assert.assertEquals("Answer: null", body);

         log.info("************** now log in with Shiro ...");
         body = executeGET(getBaseURL() + "/loginShiro.cibet?USER=Kim&ROLE=MANAGER");
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

         log.info("************** now log in with Shiro ...");
         body = executeGET(getBaseURL() + "/loginShiro.cibet?USER=Kim&ROLE=MANAGER");
         Assert.assertEquals("Login done for user Kim", body);

         log.info("************** now showSession ...");
         body = executeGET(getBaseURL() + "/showSession.cibet");
         Assert.assertEquals("after Login: User: Kim; Tenant: __DEFAULT;  - -", body);

         log.info("************** log message ...");
         body = executeGET(getBaseURL() + "/logThis.cibet?MSG=Hallo!");
         Assert.assertEquals("Answer: Hallo!", body);
      } finally {
         afterTest();
      }
   }

   @Test
   public void testPersistAndRelease() throws Exception {
      log.info("start testPersistAndRelease()");
      EntityManager cibetEman = Context.requestScope().getEntityManager();

      try {

         String body = executeGET(getBaseURL() + "/logout.cibet");
         Assert.assertEquals("Logout done", body);

         log.info("************** now log in with Shiro ...");
         body = executeGET(getBaseURL() + "/loginShiro.cibet?USER=Kim&ROLE=MANAGER");
         Assert.assertEquals("Login done for user Kim", body);

         log.info("************** now persist denied ...");
         body = executeGET(getBaseURL() + "/persist.cibet", HttpStatus.SC_INTERNAL_SERVER_ERROR);
         Assert.assertTrue(body.indexOf("com.logitags.cibet.actuator.common.DeniedException: Access denied") > 0);

         Query q = cibetEman.createQuery("select a from Archive a");
         List<Archive> list = q.getResultList();
         Assert.assertEquals(1, list.size());
         Assert.assertEquals(ExecutionStatus.DENIED, list.get(0).getExecutionStatus());

         Thread.sleep(500);

         q = applEman.createQuery("SELECT e FROM TEntity e");
         List<TEntity> tentList = q.getResultList();
         Assert.assertEquals(0, tentList.size());

         log.info("************** now logout ...");
         body = executeGET(getBaseURL() + "/logout.cibet");
         Assert.assertEquals("Logout done", body);

         log.info("************** now log in ...");
         body = executeGET(getBaseURL() + "/loginShiro.cibet?USER=Kim&ROLE=MANAGER&Freds=true");
         Assert.assertEquals("Login done for user Kim", body);

         log.info("************** now persist accepted ...");
         body = executeGET(getBaseURL() + "/persist.cibet");
         Assert.assertEquals("TEntity persist with ID: 0", body);

         Thread.sleep(500);

         cibetEman.getTransaction().commit();
         cibetEman.getTransaction().begin();

         q = cibetEman.createQuery("select a from Archive a where a.resource.targetType ='" + TEntity.class.getName()
               + "' order by a.createDate");
         list = q.getResultList();
         Assert.assertEquals(2, list.size());
         Assert.assertEquals(ExecutionStatus.POSTPONED, list.get(1).getExecutionStatus());
         Assert.assertEquals("0", ((Archive) list.get(1)).getResource().getPrimaryKeyId());

         q = applEman.createQuery("SELECT e FROM TEntity e");
         tentList = q.getResultList();
         Assert.assertEquals(0, tentList.size());

         q = cibetEman.createQuery(SEL_DCCONTROLLABLE);
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
         body = executeGET(getBaseURL() + "/loginShiro.cibet?USER=Henny&ROLE=SIMPLEUSER");
         Assert.assertEquals("Login done for user Henny", body);

         log.info("************** now release denied ...");
         body = executeGET(getBaseURL() + "/releasePersist.cibet");
         Assert.assertEquals("TEntity not released", body);

         q = cibetEman.createQuery(SEL_DCCONTROLLABLE);
         dcList = q.getResultList();
         Assert.assertEquals(1, dcList.size());
         Assert.assertEquals(ExecutionStatus.POSTPONED, dcList.get(0).getExecutionStatus());

         q = applEman.createQuery("SELECT e FROM TEntity e");
         tentList = q.getResultList();
         Assert.assertEquals(0, tentList.size());

         cibetEman.getTransaction().commit();
         cibetEman.getTransaction().begin();

         q = cibetEman.createQuery("select a from Archive a where a.resource.targetType ='" + TEntity.class.getName()
               + "' order by a.createDate");
         list = q.getResultList();
         Assert.assertEquals(3, list.size());
         Assert.assertEquals(ExecutionStatus.DENIED, list.get(2).getExecutionStatus());
         Assert.assertEquals("Henny", list.get(2).getCreateUser());

         log.info("************** now logout ...");
         body = executeGET(getBaseURL() + "/logout.cibet");
         Assert.assertEquals("Logout done", body);

         log.info("************** now log in ...");
         body = executeGET(getBaseURL() + "/loginShiro.cibet?USER=Fluppi&ROLE=MANAGER");
         Assert.assertEquals("Login done for user Fluppi", body);

         Thread.sleep(500);
         log.info("************** now release accepted ...");
         body = executeGET(getBaseURL() + "/releasePersist.cibet");
         Assert.assertTrue(body.startsWith("TEntity released with ID:"));

         int index = body.indexOf(":");
         String id = body.substring(index + 2);
         log.info("search TEntity and DcControllable with ID " + id);

         applEman.getTransaction().commit();
         applEman.getTransaction().begin();

         q = applEman.createQuery("SELECT e FROM TEntity e WHERE e.id=:id");
         q.setParameter("id", Long.parseLong(id));
         tentList = q.getResultList();
         Assert.assertEquals(1, tentList.size());

         cibetEman.getTransaction().commit();
         cibetEman.getTransaction().begin();

         q = cibetEman.createQuery("SELECT e FROM DcControllable e WHERE e.resource.primaryKeyId = :id");
         q.setParameter("id", id);
         dcList = q.getResultList();
         Assert.assertEquals(1, dcList.size());
         cibetEman.refresh(dcList.get(0));
         log.debug(dcList.get(0));
         Assert.assertEquals(ExecutionStatus.EXECUTED, dcList.get(0).getExecutionStatus());
         Assert.assertEquals("good!", dcList.get(0).getApprovalRemark());
         Assert.assertEquals("Fluppi", dcList.get(0).getApprovalUser());

         q = cibetEman.createQuery("select a from Archive a where a.resource.targetType ='" + TEntity.class.getName()
               + "' ORDER BY a.createDate");
         list = q.getResultList();
         Assert.assertEquals(4, list.size());
         cibetEman.refresh(list.get(0));
         cibetEman.refresh(list.get(1));
         cibetEman.refresh(list.get(2));
         cibetEman.refresh(list.get(3));
         for (Archive a : list) {
            log.debug(":::" + a.getExecutionStatus() + a.getControlEvent() + " " + a.getResource().getPrimaryKeyId());
         }

         Assert.assertEquals(ExecutionStatus.EXECUTED, list.get(3).getExecutionStatus());
         Assert.assertEquals("Fluppi", list.get(3).getCreateUser());
         Assert.assertEquals(list.get(1).getCaseId(), list.get(2).getCaseId());
         Assert.assertEquals(list.get(2).getCaseId(), list.get(3).getCaseId());
         Assert.assertEquals(id, ((Archive) list.get(1)).getResource().getPrimaryKeyId());
         Assert.assertEquals(id, ((Archive) list.get(2)).getResource().getPrimaryKeyId());
         Assert.assertEquals(id, ((Archive) list.get(3)).getResource().getPrimaryKeyId());
      } finally {
         afterTest();
      }
   }

   @Test
   public void testFilter() throws Exception {
      log.info("start testFilter()");
      EntityManager cibetEman = Context.requestScope().getEntityManager();

      try {
         String body = executeGET(getBaseURL() + "/login.cibet?USER=Jens&tenant=XYCompany");
         Assert.assertEquals("Login done for user Jens", body);

         log.info("************** now URL forbidden 1 ...");
         executeGET(getBaseURL() + "/logThis.url?MSG=gutte", HttpStatus.SC_FORBIDDEN);
         log.info("start loading archives");

         Query q = cibetEman.createQuery("select a from Archive a where a.tenant ='XYCompany'");
         List<Archive> list = q.getResultList();
         Assert.assertEquals(1, list.size());

         log.info("************** now logout ...");
         body = executeGET(getBaseURL() + "/logout.cibet");
         Assert.assertEquals("Logout done", body);

         log.info("************** now URL no setpoint ...");
         body = executeGET(getBaseURL() + "/logThis.url?color=green&MSG=gutte");
         Assert.assertEquals("Answer: gutte", body);
         q = cibetEman.createQuery("SELECT a FROM Archive a");
         list = q.getResultList();
         Assert.assertEquals(1, list.size());

         log.info("************** now URL allowed ...");
         body = executeGET(getBaseURL() + "/logThis.url?color=red&MSG=gutte");
         Thread.sleep(200);
         Assert.assertEquals("Answer: gutte", body);

         cibetEman.getTransaction().commit();
         cibetEman.getTransaction().begin();

         q = cibetEman.createQuery("SELECT a FROM Archive a");
         list = q.getResultList();
         Assert.assertEquals(2, list.size());
      } finally {
         afterTest();
      }
   }

   @Test
   public void testFilter2Man() throws Exception {
      log.info("start testFilter2Man()");
      EntityManager cibetEman = Context.requestScope().getEntityManager();
      try {
         log.info("************** now logout ...");
         String body = executeGET(getBaseURL() + "/logout.cibet");
         Assert.assertEquals("Logout done", body);

         log.info("************** now log in ...");
         body = executeGET(getBaseURL() + "/login.cibet?USER=Fritz");
         Assert.assertEquals("Login done for user Fritz", body);

         log.info("************** now URL 2-man ...");
         executeGET(getBaseURL() + "/logThis.url?color=blue&MSG=gutte", HttpStatus.SC_ACCEPTED);
         Query q = cibetEman.createQuery("SELECT a FROM Archive a");
         List<Archive> list = q.getResultList();
         Assert.assertEquals(1, list.size());

         q = cibetEman.createQuery("SELECT d FROM DcControllable d");
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
         Assert.assertEquals("Answer: gutte", body);

         Thread.sleep(500);

         cibetEman.getTransaction().commit();
         cibetEman.getTransaction().begin();

         q = cibetEman.createQuery("SELECT d FROM DcControllable d");
         dcl = q.getResultList();
         Assert.assertEquals(1, dcl.size());

         cibetEman.refresh(dcl.get(0));

         Assert.assertEquals(ExecutionStatus.EXECUTED, dcl.get(0).getExecutionStatus());

         q = cibetEman.createQuery("SELECT t FROM EventResult t");
         List<EventResult> er = q.getResultList();
         Assert.assertEquals(1, er.size());
      } finally {
         afterTest();
      }

   }

   @Test
   public void testScheduleUpdate() throws Exception {
      log.info("start testScheduleUpdate()");

      TEntity t1 = new TEntity("Stung1", 1, "owner1");
      applEman.persist(t1);
      applEman.getTransaction().commit();
      applEman.getTransaction().begin();
      applEman.clear();

      log.info("************** schedule update ...");
      String body = executeGET(getBaseURL() + "/schedule.cibet?id=" + t1.getId());
      Assert.assertEquals("Answer: Okay!", body);
   }

   /**
    * does not work with OpenJpa to insert entities with associations when they are not enhanced.
    * 
    * @throws Exception
    */
   @Ignore
   @Test
   public void testMerge() throws Exception {
      log.info("start testMerge()");
      EntityManager cibetEman = Context.requestScope().getEntityManager();

      log.info("************** now log in with Shiro ...");
      String body = executeGET(getBaseURL() + "/loginShiro.cibet?USER=Kim&ROLE=MANAGER");
      Assert.assertEquals("Login done for user Kim", body);

      log.info("************** merge ...");
      body = executeGET(getBaseURL() + "/merge.cibet?lazy=loadLazy");
      Assert.assertEquals("merge chain done", body);

      Query q = cibetEman.createQuery("select a from Archive a where a.tenant ='XYCompany' order by a.createDate");
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

   /**
    * does not work with OpenJpa to insert entities with associations when they are not enhanced.
    * 
    * @throws Exception
    */
   @Ignore
   @Test
   public void testMergeLazyException() throws Exception {
      log.info("start testMergeLazyException()");
      EntityManager cibetEman = Context.requestScope().getEntityManager();

      log.info("************** now log in with Shiro ...");
      String body = executeGET(getBaseURL() + "/loginShiro.cibet?USER=Kim&ROLE=MANAGER");
      Assert.assertEquals("Login done for user Kim", body);

      log.info("************** merge ...");
      body = executeGET(getBaseURL() + "/merge.cibet");
      Assert.assertEquals("merge chain done", body);

      cibetEman.clear();
      Query q = cibetEman.createQuery("select a from Archive a where a.tenant ='XYCompany' order by a.createDate");
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

}
