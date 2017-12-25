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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.DBHelper;
import com.cibethelper.ejb.OutService;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ArquillianJDBCServlet;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ExecutionStatus;

@RunWith(Arquillian.class)
public class TomcatTestJdbc extends DBHelper {

   private static Logger log = Logger.getLogger(TomcatTestJdbc.class);

   protected CloseableHttpClient client = HttpClients.createDefault();

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = TomcatTestJdbc.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/webJDBC.xml");

      archive.addClasses(TEntity.class, ArquillianJDBCServlet.class, OutService.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      File[] shiro = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-shiro").withTransitivity()
            .asFile();
      archive.addAsLibraries(shiro);

      File[] shiroweb = Maven.resolver().loadPomFromFile("pom.xml").resolve("org.apache.shiro:shiro-web")
            .withTransitivity().asFile();
      archive.addAsLibraries(shiroweb);

      archive.delete("/WEB-INF/lib/slf4j-api-1.7.21.jar");

      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsManifestResource("it/context.xml", "context.xml");
      archive.addAsWebInfResource("shiro.ini", "classes/shiro.ini");
      archive.addAsWebInfResource("it/config_shiro.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("META-INF/persistence-jdbc-it.xml", "classes/META-INF/persistence.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeTomcatTestJdbc() {
      log.debug("execute before()");
      new ConfigurationService().initialise();
      Context.start();
      com.logitags.cibet.context.Context.sessionScope().setUser(USER);
      com.logitags.cibet.context.Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterTest() throws Exception {
      Context.end();
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
         if (instream != null) instream.close();
         Thread.sleep(100);
      }
   }

   @Test
   public void testJdbc() throws Exception {
      log.info("start testJdbc()");
      EntityManager cibetEman = Context.internalRequestScope().getOrCreateEntityManager(false);

      log.info("************** now logout ...");
      String body = executeGET(getBaseURL() + "/logout.cibet");
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

      Query q = cibetEman.createQuery("select a from Archive a");
      List<Archive> list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(OutService.class.getName(), list.get(0).getResource().getTarget());
      Assert.assertEquals(ExecutionStatus.EXECUTED, list.get(0).getExecutionStatus());

      log.info("************** now persist ...");
      body = executeGET(getBaseURL() + "/persist.cibet");
      Assert.assertEquals("TEntity persist", body);

      q = applEman.createQuery("select t from TEntity t");
      List<TEntity> tlist = q.getResultList();
      Assert.assertEquals(1, tlist.size());

      cibetEman.clear();
      cibetEman.getTransaction().commit();
      cibetEman.getTransaction().begin();

      q = cibetEman
            .createQuery("select a from Archive a where a.resource.target='cib_testentity' order by a.createDate");
      list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Assert.assertEquals(5, list.get(0).getResource().getParameters().size());
      Assert.assertEquals("cib_testentity", list.get(0).getResource().getTarget());
      cibetEman.getTransaction().commit();
      cibetEman.getTransaction().begin();
   }

}
