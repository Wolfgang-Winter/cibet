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

import org.apache.http.HttpResponse;
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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.SpringTestAuthenticationManager;
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
import com.cibethelper.servlet.ArquillianTestServlet1;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.Headers;
import com.logitags.cibet.sensor.http.HttpRequestInvoker;

/**
 * 
 * @author Wolfgang
 * 
 */
@RunWith(Arquillian.class)
public class HttpParallelDcIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpParallelDcIT.class);

   @Deployment(testable = true)
   public static WebArchive createDeployment() {
      String warName = HttpParallelDcIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-spring.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class, RemoteEJB.class,
            RemoteEJBImpl.class, SimpleEjb.class, ArquillianTestServlet1.class, SpringTestAuthenticationManager.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);
      File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-springsecurity")
            .withTransitivity().asFile();
      archive.addAsLibraries(spring);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource("spring-context_1.xml", "classes/spring-context.xml");
      archive.addAsWebInfResource("config_parallel.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("jndi_.properties", "classes/jndi_.properties");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeHttpParallelDcIT() {
      log.debug("execute before()");
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterHttpParallelDcIT() {
      Context.end();
   }

   private DcControllable checkDc(String target, String method, int count) throws Exception {
      log.debug("now check");

      List<DcControllable> list = null;
      for (int i = 1; i < 6; i++) {
         list = DcLoader.findUnreleased();
         if (1 == list.size())
            break;

         log.debug("No result. Try query again: " + i);
         Thread.sleep(400);
      }

      Assert.assertEquals(count, list.size());
      DcControllable ar = list.get(count - 1);
      Resource res = ar.getResource();
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals(target, res.getTargetType());
      Assert.assertEquals(method, res.getMethod());
      Assert.assertEquals(HttpRequestInvoker.class.getName(), res.getInvokerClass());
      return ar;
   }

   @Test
   public void postponeParallel() throws Exception {
      log.info("start postponeParallel()");

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=Willi&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      String url = getBaseURL() + "/test/parallel";
      HttpGet getMethod = new HttpGet(url + "?counter=890");
      response = client.execute(getMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String body = readResponseBody(response);
      Assert.assertEquals("NO Persist, counter: 891", body);
      DcControllable co = checkDc(url, "GET", 1);
      Resource res = co.getResource();
      Assert.assertTrue(res.getParameters().size() > 4);

      String evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      EventResult result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());
   }

   @Test
   public void postponeReleaseLessExecutions() throws Exception {
      log.info("start postponeReleaseLessExecutions()");
      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=Willi&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      String url = getBaseURL() + "/test/parallel2";
      HttpGet g = new HttpGet(url + "?counter=890");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String body = readResponseBody(response);
      Assert.assertEquals("NO Persist, counter: 891", body);
      DcControllable co = checkDc(url, "GET", 1);

      String evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      EventResult result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());

      ut.begin();
      Context.sessionScope().setUser("releaseUser");
      try {
         co.release(applEman, null);
         Assert.fail();
      } catch (ResourceApplyException e) {
         Assert.assertTrue(e.getMessage().endsWith("executed only 1 times"));
      }
      ut.rollback();
   }

   @Test
   public void postponeReleaseInvalidUser() throws Exception {
      log.info("start postponeReleaseInvalidUser()");
      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=Willi&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      String url = getBaseURL() + "/test/parallel2";
      HttpGet g = new HttpGet(url + "?counter=890");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String body = readResponseBody(response);
      Assert.assertEquals("NO Persist, counter: 891", body);
      DcControllable co = checkDc(url, "GET", 1);

      String evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      EventResult result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());

      log.debug("now the test 2");
      Thread.sleep(1001);
      g = new HttpGet(url + "?counter=895");
      g.setHeader(Headers.CIBET_CASEID.name(), result.getCaseId());
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      body = readResponseBody(response);
      Assert.assertEquals("NO Persist, counter: 896", body);
      co = checkDc(url, "GET", 2);

      log.debug("now the test 3");
      Thread.sleep(1001);
      g = new HttpGet(url + "?counter=900");
      g.setHeader(Headers.CIBET_CASEID.name(), result.getCaseId());
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      body = readResponseBody(response);
      Assert.assertEquals("NO Persist, counter: 901", body);
      co = checkDc(url, "GET", 3);

      evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());

      Context.sessionScope().setUser("Willi");
      ut.begin();
      try {
         co.release(applEman, null);
         Assert.fail();
      } catch (InvalidUserException e) {
      }
      ut.rollback();
   }

   @Test
   public void postponeRelease() throws Exception {
      log.info("start postponeRelease()");
      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=Willi&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      String url = getBaseURL() + "/test/parallel2";
      HttpGet g = new HttpGet(url + "?counter=890");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String body = readResponseBody(response);
      Assert.assertEquals("NO Persist, counter: 891", body);
      DcControllable co = checkDc(url, "GET", 1);

      String evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      EventResult result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());

      log.debug("now the test 2");
      Thread.sleep(1001);
      g = new HttpGet(url + "?counter=895");
      g.setHeader(Headers.CIBET_CASEID.name(), result.getCaseId());
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      body = readResponseBody(response);
      Assert.assertEquals("NO Persist, counter: 896", body);
      co = checkDc(url, "GET", 2);

      log.debug("now the test 3");
      Thread.sleep(1001);
      g = new HttpGet(url + "?counter=900");
      g.setHeader(Headers.CIBET_CASEID.name(), result.getCaseId());
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      body = readResponseBody(response);
      Assert.assertEquals("NO Persist, counter: 901", body);
      co = checkDc(url, "GET", 3);

      String expected = null;
      for (ResourceParameter par : co.getResource().getParameters()) {
         if (par.getName().equals("counter")) {
            int counter = Integer.parseInt((String) par.getUnencodedValue()) + 1;
            expected = "Persist done, counter: " + counter;
         }
      }
      log.debug("******expected: " + expected);

      evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());

      Context.sessionScope().setUser("releaseUser");
      ut.begin();
      HttpResponse body2 = (HttpResponse) co.release(applEman, "rem");
      ut.commit();
      log.debug(body2.toString());
      String b = readResponseBody(body2);
      Assert.assertEquals(expected, b);
   }

}
