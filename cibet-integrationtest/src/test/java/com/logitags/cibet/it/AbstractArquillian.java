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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.UserTransaction;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.lock.LockedObject;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventResult;

public abstract class AbstractArquillian extends CoreTestBase {

   private static Logger log = Logger.getLogger(AbstractArquillian.class);

   @PersistenceContext(unitName = "APPL-UNIT")
   protected EntityManager applEman;

   @Resource
   protected UserTransaction ut;

   protected CloseableHttpClient client = HttpClients.createDefault();

   protected Configuration cman;

   protected Date today = new Date();

   @BeforeClass
   public static void beforeClassAbstractArquillian() throws IOException {
      log.debug("beforeClassAbstractArquillian()");
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      properties.load(url.openStream());
      HTTPURL = properties.getProperty("http.url");
      HTTPSURL = properties.getProperty("https.url");
      log.debug("HTTPURL: " + HTTPURL);
      if (properties.getProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY).contains("openejb")) {
         APPSERVER = TOMEE;
      } else if (properties.getProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY).contains("jboss")) {
         APPSERVER = JBOSS;
      } else if (properties.getProperty(javax.naming.Context.INITIAL_CONTEXT_FACTORY).contains("sun.enterprise")) {
         APPSERVER = GLASSFISH;
      }
   }

   @Before
   public void beforeAbstractArquillian() {
      log.info("call beforeAbstractArquillian(");
   }

   @After
   public void afterAbstractArquillian() throws Exception {
      Thread.sleep(500);

      // if (1 == 1) {
      // return;
      // }

      log.info("Arquillian.afterTest()");
      ut.begin();

      EntityManager cibetEman = Context.requestScope().getEntityManager();

      Query q = applEman.createQuery("SELECT e FROM TComplexEntity e");
      List<TComplexEntity> cList = q.getResultList();
      for (TComplexEntity te : cList) {
         applEman.remove(te);
      }
      log.info(cList.size() + " TComplexEntity removed");

      q = applEman.createQuery("SELECT e FROM TEntity e");
      List<TEntity> tentList = q.getResultList();
      for (TEntity te : tentList) {
         applEman.remove(te);
      }
      log.info(tentList.size() + " TEntitys removed");

      q = cibetEman.createQuery("SELECT e FROM DcControllable e");
      List<DcControllable> dcList = q.getResultList();
      for (DcControllable dc : dcList) {
         cibetEman.remove(dc);
      }
      log.info(dcList.size() + " DcControllables removed");

      q = cibetEman.createQuery("SELECT a FROM Archive a");
      List<Archive> list = q.getResultList();
      for (Archive ar : list) {
         cibetEman.remove(ar);
      }
      log.info(list.size() + " Archives removed");

      q = cibetEman.createQuery("SELECT e FROM EventResult e");
      List<EventResult> erlist = q.getResultList();
      for (EventResult er : erlist) {
         cibetEman.remove(er);
      }
      log.info(erlist.size() + " EventResults removed");

      q = cibetEman.createQuery("SELECT e FROM LockedObject e");
      List<LockedObject> llist = q.getResultList();
      for (LockedObject er : llist) {
         cibetEman.remove(er);
      }
      log.info(llist.size() + " LockedObjects removed");

      q = cibetEman.createQuery("SELECT a FROM Resource a");
      List<com.logitags.cibet.resource.Resource> rlist = q.getResultList();
      log.info(rlist.size() + " resource loaded");
      for (com.logitags.cibet.resource.Resource r : rlist) {
         cibetEman.remove(r);
         log.info("....");
      }
      log.info(rlist.size() + " Resources removed");

      q = applEman.createNativeQuery("DELETE FROM CIB_COMPLEXTESTENTITY_AUD");
      int count = q.executeUpdate();
      log.debug(count + " rows deleted in CIB_COMPLEXTESTENTITY_AUD");

      q = applEman.createNativeQuery("DELETE FROM CIB_TCOMPLEXENTITY_EAGER_AUD");
      count = q.executeUpdate();
      log.debug(count + " rows deleted in CIB_TCOMPLEXENTITY_EAGER_AUD");

      q = applEman.createNativeQuery("DELETE FROM CIB_TCOMPLEXENTITY_LAZY_AUD");
      count = q.executeUpdate();
      log.debug(count + " rows deleted in CIB_TCOMPLEXENTITY_LAZY_AUD");

      q = applEman.createNativeQuery("DELETE FROM CIB_TESTENTITY_AUD");
      count = q.executeUpdate();
      log.debug(count + " rows deleted in CIB_TESTENTITY_AUD");

      q = applEman.createNativeQuery("DELETE FROM REVINFO");
      count = q.executeUpdate();
      log.debug(count + " rows deleted in REVINFO");

      ut.commit();

      try {
         Class rtClass = Thread.currentThread().getContextClassLoader().getParent().loadClass("org.jacoco.agent.rt.RT");
         Object jacocoAgent = rtClass.getMethod("getAgent", null).invoke(null);
         Method dumpMethod = jacocoAgent.getClass().getMethod("dump", boolean.class);
         dumpMethod.invoke(jacocoAgent, false);
      } catch (ClassNotFoundException e) {
         log.warn("no jacoco agent attached to this jvm");
      } catch (Exception e) {
         log.error("while trying to dump jacoco data", e);
      }
   }

   private static void loadHTTPURL() {
      URL url = Thread.currentThread().getContextClassLoader().getResource("jndi_.properties");
      Properties properties = new Properties();
      try {
         properties.load(url.openStream());
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
      HTTPURL = properties.getProperty("http.url");
      HTTPSURL = properties.getProperty("https.url");
   }

   protected void cleanAUD() throws Exception {
      ut.begin();

      Query q = applEman.createNativeQuery("SELECT count(*) FROM CIB_COMPLEXTESTENTITY_AUD");
      try {
         Object rows = q.getSingleResult();
         log.debug(rows + " rows in CIB_COMPLEXTESTENTITY_AUD");
      } catch (NoResultException e) {
         log.debug("no rows in CIB_COMPLEXTESTENTITY_AUD");
      }
      q = applEman.createNativeQuery("DELETE FROM CIB_COMPLEXTESTENTITY_AUD");
      int count = q.executeUpdate();
      log.debug(count + " rows deleted in CIB_COMPLEXTESTENTITY_AUD");
      q = applEman.createNativeQuery("DELETE FROM CIB_TCOMPLEXENTITY_EAGER_AUD");
      count = q.executeUpdate();
      log.debug(count + " rows deleted in CIB_TCOMPLEXENTITY_EAGER_AUD");
      q = applEman.createNativeQuery("DELETE FROM CIB_TCOMPLEXENTITY_LAZY_AUD");
      count = q.executeUpdate();
      log.debug(count + " rows deleted in CIB_TCOMPLEXENTITY_LAZY_AUD");
      q = applEman.createNativeQuery("DELETE FROM CIB_TESTENTITY_AUD");
      count = q.executeUpdate();
      log.debug(count + " rows deleted in CIB_TESTENTITY_AUD");
      q = applEman.createNativeQuery("DELETE FROM REVINFO");
      count = q.executeUpdate();
      log.debug(count + " rows deleted in REVINFO");
      ut.commit();
   }

   protected void remove(Object ent) throws Exception {
      ut.begin();
      applEman.remove(ent);
      ut.commit();
      applEman.clear();
   }

   protected <T> T merge(T entity) throws Exception {
      ut.begin();
      try {
         entity = applEman.merge(entity);
         ut.commit();
         applEman.clear();
         return entity;
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         ut.rollback();
         throw e;
      }
   }

   public <T> void persist(T entity) throws Exception {
      // applEman.getTransaction().begin();
      ut.begin();
      applEman.persist(entity);
      ut.commit();
      // applEman.getTransaction().commit();
      applEman.clear();
   }

   protected String executeGET(String url, int expected) throws Exception {
      HttpGet get = new HttpGet(url);
      HttpResponse response = client.execute(get);
      Assert.assertEquals(expected, response.getStatusLine().getStatusCode());
      return readResponseBody(response);
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

   protected List<TEntity> loadTEntities() {
      applEman.clear();
      Query q = applEman.createQuery("select t from TEntity t");
      List<TEntity> list = q.getResultList();
      return list;
   }

   protected String getBaseURL() {
      if (HTTPURL == null) {
         loadHTTPURL();
      }
      return HTTPURL + this.getClass().getSimpleName();
   }

   protected String getBaseSSLURL() {
      if (HTTPSURL == null) {
         loadHTTPURL();
      }
      return HTTPSURL + this.getClass().getSimpleName();
   }

}
