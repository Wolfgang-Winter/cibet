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
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;

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
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.lock.LockActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.it.AbstractArquillian;

@RunWith(Arquillian.class)
public class HttpLockerIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpLockerIT.class);

   protected String URL_TS = getBaseURL() + "/test/url";

   private static boolean registered = false;

   @EJB
   private RemoteEJB remoteEjb;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = HttpLockerIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class, RemoteEJB.class,
            RemoteEJBImpl.class, SimpleEjb.class, ArquillianTestServlet1.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withoutTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);
      File[] spring = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-springsecurity")
            .withTransitivity().asFile();
      archive.addAsLibraries(spring);

      archive.addAsWebInfResource("META-INF/persistence-it.xml", "classes/META-INF/persistence.xml");
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
      register();
      log.debug("end execute before()");
   }

   @After
   public void afterHttpParallelDcIT() {
      Context.end();
   }

   private void register() {
      if (registered)
         return;
      log.debug("| register Setpoint");
      registered = true;
      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, schemes, ControlEvent.INVOKE);
   }

   @Test
   public void lockUrlNotLocked() throws Exception {
      log.info("start lockUrlNotLocked()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, schemes, ControlEvent.INVOKE);
      log.debug("call " + URL_TS);

      HttpGet g = new HttpGet(URL_TS);
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String body = readResponseBody(response);
      Assert.assertEquals("OK message: ", body);
   }

   @Test
   public void lockUrlLockedSameUser() throws Exception {
      log.info("start lockUrlLockedSameUser()");

      Controllable lock = remoteEjb.lock(URL_TS);
      Assert.assertNotNull(lock);
      log.debug(lock);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      HttpGet g = new HttpGet(URL_TS);
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String body = readResponseBody(response);
      Assert.assertEquals("OK message: ", body);
   }

   @Test
   public void lockUrlLockedOtherUser() throws Exception {
      log.info("start lockUrlLockedOtherUser()");

      Controllable lock = remoteEjb.lock(URL_TS);
      Assert.assertNotNull(lock);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=Willi&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      HttpGet g = new HttpGet(URL_TS + "?user=OtherUser");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      readResponseBody(response);
   }

   @Test
   public void lockSubUrlLockedOtherUser() throws Exception {
      log.info("start lockSubUrlLockedOtherUser()");

      Controllable lock = remoteEjb.lock(URL_TS);
      Assert.assertNotNull(lock);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=Willi&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      List<String> schemes = new ArrayList<String>();
      schemes.add(LockActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, schemes, ControlEvent.INVOKE);

      HttpGet g = new HttpGet(URL_TS + "/hose");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
   }

}
