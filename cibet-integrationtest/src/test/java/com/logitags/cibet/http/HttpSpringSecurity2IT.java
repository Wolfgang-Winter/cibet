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
package com.logitags.cibet.http;

import java.io.File;
import java.net.URLEncoder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
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
import com.cibethelper.base.NoControlActuator;
import com.cibethelper.ejb.RemoteEJB;
import com.cibethelper.ejb.RemoteEJBImpl;
import com.cibethelper.ejb.SimpleEjb;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.SpringTestServlet;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.sensor.http.Headers;

@RunWith(Arquillian.class)
public class HttpSpringSecurity2IT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpSpringSecurity2IT.class);

   protected String URL_TS = getBaseURL() + "/test/ts";

   protected HttpGet getMethod = new HttpGet(URL_TS);
   protected String URL_CONFIG = getBaseURL() + "/test/config";

   @Deployment(testable = true)
   public static WebArchive createDeployment() {
      String warName = HttpSpringSecurity2IT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-spring2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class, RemoteEJB.class,
            RemoteEJBImpl.class, SimpleEjb.class, SpringTestServlet.class, SpringTestAuthenticationManager.class,
            NoControlActuator.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);
      File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-springsecurity")
            .withTransitivity().asFile();
      archive.addAsLibraries(spring);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource("spring-context_2.xml", "classes/spring-context.xml");
      archive.addAsWebInfResource("it/config_web2man.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
      archive.addAsWebInfResource("jndi_.properties", "classes/jndi_.properties");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @After
   public void afterHttpSpringSecurity2IT() throws Exception {
      log.info("execute sub-afterAbstractArquillian(");
      HttpGet method = new HttpGet(getBaseURL() + "/test/spring/logoffSpring");
      client.execute(method);
      method.abort();
      Context.end();
   }

   @Before
   public void beforeHttpSpringSecurity2IT() {
      log.debug("execute before()");
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @Test
   public void testDeny2Man() throws Exception {
      log.info("start testDeny2Man()");

      HttpGet method = new HttpGet(
            getBaseURL() + "/test/spring/loginSpring?USER=Fred&ROLE=adminXXX" + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();
      Thread.sleep(20);

      log.debug("now the test");
      HttpOptions g = new HttpOptions(URL_TS + "?role=" + URLEncoder.encode("adminXXXX", "UTF-8"));
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testDeny2Man2() throws Exception {
      log.info("start testDeny2Man2()");
      HttpGet method = new HttpGet(
            getBaseURL() + "/test/spring/loginSpring?USER=Fred&secondUser=NULL&ROLE=admin" + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      Thread.sleep(20);
      log.debug("now the test");
      HttpOptions g = new HttpOptions(URL_TS + "?role=" + URLEncoder.encode("admin", "UTF-8") + "&secondUser="
            + URLEncoder.encode("NULL", "UTF-8"));
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testDeny2Man3() throws Exception {
      log.info("start testDeny2Man3()");
      HttpGet method = new HttpGet(getBaseURL()
            + "/test/spring/loginSpring?secondUser=hall&USER=Fred&secondRole=NULL&ROLE=admin" + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();
      Thread.sleep(20);

      log.debug("now the test");
      HttpOptions g = new HttpOptions(URL_TS + "?role=" + URLEncoder.encode("admin", "UTF-8") + "&secondUser="
            + URLEncoder.encode("hall", "UTF-8") + "&secondRole=" + URLEncoder.encode("NULL", "UTF-8"));
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      String erString = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      EventResult er = CibetUtil.decodeEventResult(erString);
      log.debug("#### EventResult: " + er);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(ExecutionStatus.DENIED, er.getChildResults().get(0).getExecutionStatus());
      g.abort();
   }

   @Test
   public void testDeny2Man4() throws Exception {
      log.info("start testDeny2Man4()");
      HttpGet method = new HttpGet(getBaseURL()
            + "/test/spring/loginSpring?secondUser=hil&secondRole=sssss&USER=Fred&ROLE=admin" + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();
      Thread.sleep(20);

      log.debug("now the test");
      HttpOptions g = new HttpOptions(URL_TS + "?role=" + URLEncoder.encode("admin", "UTF-8") + "&secondUser="
            + URLEncoder.encode("höl", "UTF-8") + "&secondRole=" + URLEncoder.encode("ssssssss", "UTF-8"));
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      String erString = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      EventResult er = CibetUtil.decodeEventResult(erString);
      Assert.assertEquals(ExecutionStatus.POSTPONED, er.getExecutionStatus());
      Assert.assertEquals(ExecutionStatus.DENIED, er.getChildResults().get(0).getExecutionStatus());

      g.abort();
   }

   @Test
   public void testGrant2Man() throws Exception {
      log.info("start testGrant2Man()");
      HttpGet method = new HttpGet(getBaseURL()
            + "/test/spring/loginSpring?USER=Fred&secondUser=hil&secondRole=second&ROLE=admin" + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();
      Thread.sleep(20);

      log.debug("now the test");
      HttpOptions g = new HttpOptions(URL_TS + "?role=" + URLEncoder.encode("admin", "UTF-8") + "&secondUser="
            + URLEncoder.encode("höl", "UTF-8") + "&secondRole=" + URLEncoder.encode("second", "UTF-8"));
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      String erString = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      EventResult er = CibetUtil.decodeEventResult(erString);
      log.debug(er);
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getExecutionStatus());
      Assert.assertEquals(ExecutionStatus.EXECUTED, er.getChildResults().get(0).getExecutionStatus());

      g.abort();
   }

}
