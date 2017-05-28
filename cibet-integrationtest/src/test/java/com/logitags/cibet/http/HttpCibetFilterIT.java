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
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
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
import org.junit.Ignore;
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
import com.cibethelper.servlet.ContextSetFilter;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.archive.ArchiveLoader;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.HttpRequestResource;

@RunWith(Arquillian.class)
public class HttpCibetFilterIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpCibetFilterIT.class);

   protected String URL_EEE = getBaseURL() + "/eee";
   protected String URL_TS = getBaseURL() + "/test/ts";

   @Deployment
   public static WebArchive createDeployment() {
      String warName = HttpCibetFilterIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web3.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class, RemoteEJB.class,
            RemoteEJBImpl.class, SimpleEjb.class, ArquillianTestServlet1.class, ContextSetFilter.class,
            SpringTestAuthenticationManager.class);

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
   public void beforeHttpCibetFilterIT() {
      log.debug("execute before()");
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterHttpCibetFilterIT() {
      Context.end();
      new ConfigurationService().reinitSetpoints();
   }

   private Archive check(String method, String url) throws Exception {
      log.debug("check...");
      Thread.sleep(300);

      List<Archive> list = null;
      for (int i = 1; i < 6; i++) {
         list = ArchiveLoader.loadAllArchives();
         // Query q = cem.createQuery("SELECT a FROM Archive a WHERE tenant = '" + TENANT + "'");
         // list = q.getResultList();
         if (1 == list.size())
            break;

         log.debug("No result. Try query again: " + i);
         // cem.clear();
         // if (cem.getTransaction().isActive()) {
         // cem.getTransaction().commit();
         // cem.getTransaction().begin();
         // }
         //
         Thread.sleep(400);
      }

      Assert.assertEquals(1, list.size());
      Archive ar = list.get(0);
      HttpRequestResource res = (HttpRequestResource) ar.getResource();
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals(url, res.getTargetType());
      Assert.assertEquals(method, res.getMethod());
      return ar;
   }

   private void check0() throws Exception {
      Thread.sleep(350);
      List<Archive> list = ArchiveLoader.loadAllArchives();
      ;
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void testBlank() throws Exception {
      log.info("start testBlank()");
      HttpResponse response = client.execute(new HttpGet(URL_TS));
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
   }

   @Test
   public void testTarget() throws Exception {
      log.info("start testTarget()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS, schemes, ControlEvent.INVOKE);

      HttpGet g = new HttpGet(URL_TS);
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check("GET", URL_TS);
   }

   @Test
   public void testTargetNO() throws Exception {
      log.info("start testTargetNO()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(URL_TS + "xx", schemes, ControlEvent.INVOKE);

      HttpGet g = new HttpGet(URL_TS);
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check0();
   }

   @Test
   public void testMethod() throws Exception {
      log.info("start testMethod()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(null, "GET", schemes, ControlEvent.INVOKE);

      HttpResponse response = client.execute(new HttpGet(URL_TS));
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check("GET", URL_TS);
   }

   @Test
   public void testMethodNO() throws Exception {
      log.info("start testMethodNO()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(null, "PUT", schemes, ControlEvent.INVOKE);

      HttpResponse response = client.execute(new HttpGet(URL_TS));
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check0();
   }

   @Test
   public void testPost() throws Exception {
      log.info("start testPost()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(null, "POST", schemes, ControlEvent.INVOKE);

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check("POST", URL_TS);
   }

   @Test
   public void testTargetWildcard() throws Exception {
      log.info("start testTargetWildcard()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes, ControlEvent.INVOKE);

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check("POST", URL_TS);
   }

   @Test
   public void testInvoker() throws Exception {
      log.info("start testInvoker()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);
      sp.setInvoker("192.68.2.1");

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);
      postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check("POST", URL_TS);
   }

   @Test
   public void testInvokerWildcard() throws Exception {
      log.info("start testInvokerWildcard()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);
      sp.setInvoker("192.68.*");

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);
      postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check("POST", URL_TS);
   }

   @Test
   public void testInvokerNO() throws Exception {
      log.info("start testInvokerNO()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);
      sp.setInvoker("192.68.*");

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check0();
   }

   @Test
   public void testCondition() throws Exception {
      log.info("start testCondition()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);
      sp.setCondition("$HTTPPARAMETERS.get('act')=='aschenfels'");

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS);
      postMethod.setEntity(entity);
      postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check("POST", URL_TS);
   }

   @Test
   public void testCondition2() throws Exception {
      log.info("start testCondition2()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);
      sp.setCondition("$HTTPPARAMETERS.get('act')=='aschenfels' && $HTTPPARAMETERS.get('dubi2')!=null;");

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS + "?wrm=warmhalten");
      postMethod.setEntity(entity);
      postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      check("POST", URL_TS);
   }

   @Test
   public void testCondition3() throws Exception {
      log.info("start testCondition3()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);
      sp.setCondition(
            "js = new java.lang.String($HTTPHEADERS.get('http_x_forwarded_for')); $HTTPPARAMETERS.get('mixi')==\"hasenfiss\" && js.startsWith(\"192.68.2.1\")");

      StringEntity entity = new StringEntity("important message �sal", "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS + "?mixi=hasenfiss");
      postMethod.setEntity(entity);
      postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String expected = readResponseBody(response);
      Assert.assertEquals("OK message: ;mixi=hasenfiss", expected);
      Archive ar = check("POST", URL_TS);
      HttpRequestResource res = (HttpRequestResource) ar.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(8, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(11, res.getParameters().size());
      }
   }

   @Test
   public void testAttribute() throws Exception {
      log.info("start testAttribute()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);

      // The attribute is set in ContextSetFilter
      log.debug("now the test");
      StringEntity entity = new StringEntity("important message Ösal");
      HttpPost postMethod = new HttpPost(getBaseURL() + "/test/context" + "?attribute=Ösal12345");
      postMethod.setEntity(entity);
      postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      Archive ar = check("POST", getBaseURL() + "/test/context");
      HttpRequestResource res = (HttpRequestResource) ar.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(9, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(12, res.getParameters().size());
      }
      postMethod.abort();
   }

   @Test
   public void testMultiParameters() throws Exception {
      log.info("start testMultiParameters()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);
      sp.setCondition("$HTTPPARAMETERS.get('act')=='aschenfels' && $HTTPPARAMETERS.get('dubi2')!=null;");

      List<NameValuePair> formparams = new ArrayList<NameValuePair>();
      formparams.add(new BasicNameValuePair("act", "aschenfels"));
      formparams.add(new BasicNameValuePair("dubi2", "Klassenmann"));
      formparams.add(new BasicNameValuePair("dubi2", "Hosenfrau"));
      UrlEncodedFormEntity entity = new UrlEncodedFormEntity(formparams, "UTF-8");
      HttpPost postMethod = new HttpPost(URL_TS + "?wrm=warmhalten");
      postMethod.setEntity(entity);
      postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      Archive ar = check("POST", URL_TS);
      HttpRequestResource res = (HttpRequestResource) ar.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(10, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(13, res.getParameters().size());
      }
      for (ResourceParameter p : res.getParameters()) {
         if ("[Ljava.lang.String;".equals(p.getClassname())) {
            for (String s : (String[]) p.getUnencodedValue()) {
               log.debug(p.getName() + ": " + s + ",class: " + p.getClassname() + ", type: " + p.getParameterType());
            }
         } else {
            log.debug(p.getName() + ": " + p.getUnencodedValue() + ",class: " + p.getClassname() + ", type: "
                  + p.getParameterType());
         }
      }
   }

   @Test
   public void testMultiHeader() throws Exception {
      log.info("start testMultiHeader()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL().substring(0, getBaseURL().length() - 4) + "*", schemes,
            ControlEvent.INVOKE);

      StringEntity entity = new StringEntity("important message �sal");
      HttpPost postMethod = new HttpPost(URL_TS + "?attribute=osal12345");
      postMethod.setEntity(entity);
      postMethod.addHeader("Accept-Language", "de");
      postMethod.addHeader("Accept-Language", "fr");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      Archive ar = check("POST", URL_TS);
      HttpRequestResource res = (HttpRequestResource) ar.getResource();
      if (TOMEE.equals(APPSERVER)) {
         Assert.assertEquals(8, res.getParameters().size());
      } else if (JBOSS.equals(APPSERVER)) {
         Assert.assertEquals(11, res.getParameters().size());
      }
      postMethod.abort();

      boolean found = false;
      for (ResourceParameter p : res.getParameters()) {
         if (("accept-language").equals(p.getName())) {
            found = true;
            Assert.assertEquals("[Ljava.lang.String;", p.getClassname());
            String[] str = (String[]) p.getUnencodedValue();
            Assert.assertEquals(2, str.length);
            for (String s : (String[]) p.getUnencodedValue()) {
               Assert.assertTrue("de".equals(s) || "fr".equals(s));
            }
         }
      }
      Assert.assertTrue(found);
   }

   @Ignore
   @Test
   public void testEjb() throws Exception {
      log.info("start testEjb()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL() + "/test/context", schemes, ControlEvent.INVOKE);

      StringEntity entity = new StringEntity("important message �sal");
      HttpPost postMethod = new HttpPost(
            getBaseURL() + "/test/context" + "?attribute=" + URLEncoder.encode("Ösal12345", "UTF-8") + "&role="
                  + URLEncoder.encode("EJBUSER", "UTF-8") + "&expVoter=1");
      postMethod.setEntity(entity);
      postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(postMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      Archive ar = check("POST", URL_EEE);
      HttpRequestResource res = (HttpRequestResource) ar.getResource();
      // if ("EmbeddedTomcat7".equals(container.getName())) {
      // // Tomcat7 adds an ATTRIBUTE 'org.apache.catalina.ASYNC_SUPPORTED'
      // Assert.assertEquals(11, res.getParameters().size());
      // } else {
      Assert.assertEquals(10, res.getParameters().size());
      // }
   }

   // /**
   // * second time for checking if static InitialContext is okay in CibetFilter
   // *
   // * @throws Exception
   // */
   // // @Test
   // public void testEjb2() throws Exception {
   // log.info("start testEjb2()");
   // initJndiProperties("jndi-for-openejb.properties");
   // HttpClient client = new DefaultHttpClient();
   //
   // StringBuffer burl = new StringBuffer();
   // burl.append(URL_CONFIG);
   // burl.append("?id=");
   // burl.append(URLEncoder.encode("testEjb2", "UTF-8"));
   // burl.append("&event=");
   // burl.append(URLEncoder.encode(ControlEvent.INVOKE.name(), "UTF-8"));
   // burl.append("&actuator=");
   // burl.append(URLEncoder.encode(ArchiveActuator.DEFAULTNAME, "UTF-8"));
   // burl.append("&target=");
   // burl.append(URLEncoder.encode(container.getBaseURL() + "/e*", "UTF-8"));
   // burl.append("&invoker=");
   // burl.append(URLEncoder.encode("192.68.2.1", "UTF-8"));
   //
   // HttpGet configMethod = new HttpGet(burl.toString());
   // HttpResponse response = client.execute(configMethod);
   // Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
   // configMethod.abort();
   //
   // // // Execute the method. The attribute is set in ContextSetFilter
   // log.debug("now execute the test");
   // StringEntity entity = new StringEntity("important message Ösalsal");
   // HttpPost postMethod = new HttpPost(URL_EEE + "?attribute=" + URLEncoder.encode("Olas12345", "UTF-8"));
   // postMethod.setEntity(entity);
   // postMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");
   //
   // response = client.execute(postMethod);
   // Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
   // Archive ar = check("POST", URL_EEE);
   // Resource res = ar.getResource();
   // if ("EmbeddedTomcat7".equals(container.getName())) {
   // // Tomcat7 adds an ATTRIBUTE 'org.apache.catalina.ASYNC_SUPPORTED'
   // Assert.assertEquals(10, res.getParameters().size());
   // } else {
   // Assert.assertEquals(9, res.getParameters().size());
   // }
   // }

   @Test
   public void testWithException() throws Exception {
      log.info("start testWithException()");
      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      Setpoint sp = registerSetpoint(getBaseURL() + "/test/exc*", schemes, ControlEvent.INVOKE);

      HttpGet g = new HttpGet(getBaseURL() + "/test/exception");
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      Archive ar = check("GET", getBaseURL() + "/test/exception");
      Assert.assertEquals(ExecutionStatus.ERROR, ar.getExecutionStatus());
   }

}
