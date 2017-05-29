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
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.UserTransaction;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicNameValuePair;
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
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.Headers;
import com.logitags.cibet.sensor.http.HttpRequestResource;

@RunWith(Arquillian.class)
public class HttpCibetFilter2IT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpCibetFilter2IT.class);

   protected String URL_TS = getBaseURL() + "/test/url";

   @javax.annotation.Resource
   protected UserTransaction ut;

   @Deployment
   public static WebArchive createDeployment() {
      String warName = HttpCibetFilter2IT.class.getSimpleName() + ".war";
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
   public void beforeHttpCibetFilter2IT() {
      log.debug("execute before()");
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterHttpCibetFilter2IT() {
      Context.end();
      new ConfigurationService().reinitSetpoints();
   }

   private Controllable checkDc(String method, String target) throws Exception {
      Thread.sleep(300);

      log.debug("now check");
      List<Controllable> list = DcLoader.findUnreleased();

      Assert.assertEquals(1, list.size());
      Controllable ar = list.get(0);
      HttpRequestResource resource = (HttpRequestResource) ar.getResource();
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals(target, resource.getTargetType());
      Assert.assertEquals(method, resource.getMethod());
      return ar;
   }

   private void checkDc0() throws Exception {
      Thread.sleep(350);
      List<Controllable> list = DcLoader.findUnreleased();
      Assert.assertEquals(0, list.size());
   }

   private List<Archive> loadArchives(int expected, String url) throws Exception {

      List<Archive> list = null;
      EntityManager cem = Context.requestScope().getEntityManager();
      for (int i = 1; i < 6; i++) {
         Query q = cem.createQuery("SELECT a FROM Archive a ORDER BY a.createDate");
         list = q.getResultList();
         if (expected == list.size())
            break;

         log.debug("No result. Try query again: " + i);
         cem.clear();
      }
      Assert.assertEquals(expected, list.size());
      for (Archive ar : list) {
         HttpRequestResource res = (HttpRequestResource) ar.getResource();
         Assert.assertEquals(url, res.getTargetType());
      }
      return list;
   }

   @Test
   public void test4Eyes() throws Exception {
      log.info("start test4Eyes()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, schemes, ControlEvent.INVOKE);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      HttpGet g = new HttpGet(URL_TS);
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      Controllable co = checkDc("GET", URL_TS);
      log.debug("user: " + co.getCreateUser());
      log.debug("tenant: " + co.getTenant());
      Assert.assertEquals("Hausaddresse", co.getCreateAddress());
      HttpRequestResource res = (HttpRequestResource) co.getResource();
      // JBoss: 8
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(5, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(8, res.getParameters().size());
      }

      String evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      EventResult result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());
   }

   @Test
   public void testPostRelease() throws Exception {
      log.info("start testPostRelease");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, "POST", schemes, ControlEvent.INVOKE);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);

      response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      Controllable co = checkDc("POST", URL_TS);
      HttpRequestResource res = (HttpRequestResource) co.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(9, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(12, res.getParameters().size());
      }

      Context.sessionScope().setUser("releaseUser");
      HttpResponse resp = (HttpResponse) co.release(applEman, null);
      Assert.assertNotNull(resp);
      String body = readResponseBody(resp);
      int length = "OK message: ;act=aschenfels;dubi2=Klassenmann".length();
      Assert.assertEquals(length, body.length());

      String evReHeader = resp.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      EventResult result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult2 ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.EXECUTED, result.getExecutionStatus());
   }

   @Test
   public void testPostRelease2Actuators() throws Exception {
      log.info("start testPostRelease2Actuators()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, "POST", schemes, ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);

      response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      Controllable co = checkDc("POST", URL_TS);
      HttpRequestResource res = (HttpRequestResource) co.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(9, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(12, res.getParameters().size());
      }

      Context.sessionScope().setUser("releaseUser");
      ut.begin();
      HttpResponse resp = (HttpResponse) co.release(applEman, "testrelease");
      ut.commit();
      Assert.assertNotNull(resp);
      String body = readResponseBody(resp);
      int length = "OK message: ;act=aschenfels;dubi2=Klassenmann".length();
      Assert.assertEquals(length, body.length());

      List<Archive> ars = loadArchives(2, URL_TS);
      Assert.assertEquals(ControlEvent.INVOKE, ars.get(0).getControlEvent());
      Assert.assertEquals(ControlEvent.RELEASE_INVOKE, ars.get(1).getControlEvent());
      Assert.assertEquals("testrelease", ars.get(1).getRemark());
   }

   @Test
   public void testPostReject2Actuators() throws Exception {
      log.info("start testPostReject2Actuators()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, "POST", schemes, ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE, ControlEvent.REJECT);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);

      response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      Controllable co = checkDc("POST", URL_TS);
      HttpRequestResource res = (HttpRequestResource) co.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(9, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(12, res.getParameters().size());
      }

      log.debug("start reject");
      Context.sessionScope().setUser("rejectUser");
      ut.begin();
      co.reject("testreject");
      ut.commit();
      log.debug("finished reject");

      List<Archive> ars = loadArchives(2, URL_TS);
      Assert.assertEquals(ControlEvent.INVOKE, ars.get(0).getControlEvent());
      Archive ar = ars.get(0);
      HttpRequestResource resi = (HttpRequestResource) ar.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(9, resi.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(12, resi.getParameters().size());
      }

      Assert.assertEquals(ControlEvent.REJECT_INVOKE, ars.get(1).getControlEvent());
      Assert.assertEquals("testreject", ars.get(1).getRemark());
      checkDc0();
   }

   @Test
   public void body1() throws Exception {
      log.info("start body1()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, "POST", schemes, ControlEvent.INVOKE);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      String content = "important message Ösal";
      ByteArrayEntity entity = new ByteArrayEntity(content.getBytes("UTF-8"));
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);

      response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      List<Archive> ars = loadArchives(1, URL_TS);
      Archive ar = ars.get(0);
      HttpRequestResource res = (HttpRequestResource) ar.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(6, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(9, res.getParameters().size());
      }
   }

   @Test
   public void bodyWithBody() throws Exception {
      log.info("start bodyWithBody()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, "POST", schemes, ControlEvent.INVOKE);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      String content = "important message Ösal";
      ByteArrayEntity entity = new ByteArrayEntity(content.getBytes("UTF-8"));
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);

      response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      List<Archive> ars = loadArchives(1, URL_TS);
      Archive ar = ars.get(0);
      HttpRequestResource res = (HttpRequestResource) ar.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(7, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(10, res.getParameters().size());
      }

      boolean found = false;
      for (ResourceParameter par : res.getParameters()) {
         if (par.getName().equals("__HTTP_BODY")) {
            found = true;
            byte[] body = (byte[]) par.getUnencodedValue();
            String s = new String(body, "UTF-8");
            log.debug("body=" + s);
            log.debug("length=" + body.length);
            Assert.assertEquals("important message Ösal", s);
            break;
         }
      }
      Assert.assertTrue("__HTTP_BODY not found", found);
   }

   @Test
   public void playTestPostReject2Actuators() throws Exception {
      log.info("start playTestPostReject2Actuators()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, "POST", schemes, ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE, ControlEvent.REJECT);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.addHeader(Headers.CIBET_PLAYINGMODE.name(), "true");
      postMethod.setEntity(entity);

      response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      Thread.sleep(100);
      log.debug("now check");
      checkDc0();

      String evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      EventResult result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());
      Assert.assertEquals(ControlEvent.INVOKE, result.getEvent());

      log.debug("now without playing");
      formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);
      response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      log.debug("Tenant: " + Context.sessionScope().getTenant());

      Controllable co = checkDc("POST", URL_TS);
      HttpRequestResource res = (HttpRequestResource) co.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(9, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(12, res.getParameters().size());
      }

      log.debug("start reject with playing");

      Context.sessionScope().setUser("rejectUser");
      Context.requestScope().startPlay();
      ut.begin();
      co.reject("testreject");
      ut.commit();
      EventResult er = Context.requestScope().stopPlay();
      Assert.assertEquals(ExecutionStatus.REJECTED, er.getExecutionStatus());
      Assert.assertEquals(ControlEvent.REJECT_INVOKE, er.getEvent());

      List<Archive> ars = loadArchives(1, URL_TS);
      Assert.assertEquals(ControlEvent.INVOKE, ars.get(0).getControlEvent());
      Archive ar = ars.get(0);
      HttpRequestResource resi = (HttpRequestResource) ar.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(9, resi.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(12, resi.getParameters().size());
      }

      checkDc("POST", URL_TS);
   }

   @Test
   public void includePattern() throws Exception {
      log.info("start includePattern()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(URL_TS + "*", schemes, ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE, ControlEvent.REJECT);

      String url = getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT;
      log.debug("execute URL: " + url);
      HttpGet method = new HttpGet(url);
      HttpResponse response = client.execute(method);
      method.abort();

      HttpGet g = new HttpGet(URL_TS + "/hhh");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      String evReHeader = response.getFirstHeader(Headers.CIBET_EVENTRESULT.name()).getValue();
      Assert.assertNotNull(evReHeader);
      EventResult result = CibetUtil.decodeEventResult(evReHeader);
      log.debug("EventResult ####: \n" + result);
      Assert.assertEquals(ExecutionStatus.POSTPONED, result.getExecutionStatus());
   }

   @Test
   public void excludes() throws Exception {
      log.info("start excludes()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(URL_TS + "*", schemes, ControlEvent.INVOKE, ControlEvent.RELEASE_INVOKE, ControlEvent.REJECT);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=" + USER + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      HttpGet g = new HttpGet(URL_TS + "/excl");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
   }

   @Test
   public void testConfiguredBasicAuth() throws Exception {
      log.info("start testConfiguredBasicAuth()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, schemes, ControlEvent.INVOKE);

      HttpGet g = new HttpGet(URL_TS + "?user=NO_USER");
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      List<Archive> archs = loadArchives(1, URL_TS);
      log.debug("user: " + archs.get(0).getCreateUser());
      Assert.assertTrue(Pattern.matches("127.0.0.1:\\d\\d\\d\\d\\d", archs.get(0).getCreateUser()));
   }

   @Test
   public void testConfiguredNoAnonym() throws Exception {
      log.info("start testConfiguredNoAnonym()");
      String url = getBaseURL() + "/basicAuth";
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(url, schemes, ControlEvent.INVOKE);

      HttpGet g = new HttpGet(url + "?user=NO_USER");
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      List<Archive> archs = loadArchives(1, url);
      log.debug("user: " + archs.get(0).getCreateUser());
      Assert.assertNull(archs.get(0).getCreateUser());
   }

   /**
    * user credentials are set in EmbeddedTomcat7. Login is done in ContextSetFilter
    * 
    * @throws Exception
    */
   @Test
   public void testBasicAuthLogin() throws Exception {
      log.info("start testBasicAuthLogin()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, schemes, ControlEvent.INVOKE);

      HttpGet method = new HttpGet(getBaseURL() + "/test/setuser?USER=Jesofi" + "&TENANT=" + TENANT);
      HttpResponse response = client.execute(method);
      method.abort();

      HttpGet g = new HttpGet(URL_TS + "?user=LOGIN");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      List<Archive> archs = loadArchives(1, URL_TS);
      log.debug("user: " + archs.get(0).getCreateUser());
      Assert.assertEquals("Jesofi", archs.get(0).getCreateUser());
   }

}
