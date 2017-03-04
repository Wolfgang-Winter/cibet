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
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.http.HttpRequestInvoker;

@RunWith(Arquillian.class)
public class HttpRequestInvokerIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpRequestInvokerIT.class);

   protected String URL_TESTINVOKE = getBaseURL() + "/test/testInvoke";

   @Deployment
   public static WebArchive createDeployment() {
      String warName = HttpRequestInvokerIT.class.getSimpleName() + ".war";
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

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeHttpRequestInvokerIT() {
      log.debug("execute before()");
      InitializationService.instance().startContext();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterHttpRequestInvokerIT() {
      InitializationService.instance().endContext();
      // new ConfigurationService().reinitSetpoints();
   }

   private ResourceParameter createParameter(String name, Object value) throws IOException {
      ResourceParameter ar1 = new ResourceParameter();
      ar1.setClassname(value.getClass().getName());
      ar1.setName(name);
      ar1.setParameterType(ParameterType.HTTP_PARAMETER);
      ar1.setUnencodedValue(value);
      return ar1;
   }

   private ResourceParameter createDoubleParameter(String name, String value1, String value2) throws IOException {
      ResourceParameter ar1 = new ResourceParameter();
      ar1.setClassname(String[].class.getName());
      ar1.setName(name);
      ar1.setParameterType(ParameterType.HTTP_PARAMETER);
      String[] str = new String[] { value1, value2 };
      ar1.setUnencodedValue(str);
      return ar1;
   }

   private ResourceParameter createHeader(String name, String value) throws IOException {
      ResourceParameter ar1 = new ResourceParameter();
      ar1.setClassname(String.class.getName());
      ar1.setName(name);
      ar1.setParameterType(ParameterType.HTTP_HEADER);
      ar1.setUnencodedValue(value);
      return ar1;
   }

   private ResourceParameter createDoubleHeader(String name, String value1, String value2) throws IOException {
      ResourceParameter ar1 = new ResourceParameter();
      ar1.setClassname(String[].class.getName());
      ar1.setName(name);
      ar1.setParameterType(ParameterType.HTTP_HEADER);
      String[] str = new String[] { value1, value2 };
      ar1.setUnencodedValue(str);
      return ar1;
   }

   private void executeRequest(String method) throws Exception {
      log.info("start test with method " + method);

      HttpRequestInvoker inv = new HttpRequestInvoker();
      List<ResourceParameter> params = new LinkedList<ResourceParameter>();
      params.add(createParameter("Ente1", "Erpel"));
      params.add(createParameter("PferdÖ1", "?Rüpel"));
      params.add(createHeader("Content-Type", "text/html; charset=utf-8"));

      Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.INVOKE);

      HttpResponse resp = (HttpResponse) inv.execute(null, URL_TESTINVOKE, method, params);
      Assert.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
      String body = readResponseBody(resp);
      log.debug("'" + body + "'");
      Assert.assertNotNull(body);
      Assert.assertTrue(body.startsWith("TestInvoke done: Ente1=Erpel ; PferdÖ1=?Rüpel ; "));
   }

   @Test
   public void testRequest() throws Exception {
      executeRequest("GET");
      executeRequest("options");
      executeRequest("delete");
      executeRequest("POST");
   }

   /**
    * On Tomcat no parameters are read from entity body on PUT request. On Jetty parameters are read.
    * 
    * @throws Exception
    */
   @Test
   public void testPUTRequest() throws Exception {
      log.info("start testPUTRequest()");

      HttpRequestInvoker inv = new HttpRequestInvoker();
      List<ResourceParameter> params = new LinkedList<ResourceParameter>();
      params.add(createParameter("Ente1", "Erpel"));
      params.add(createParameter("PferdÖ1", "?Rüpel"));
      params.add(createHeader("Content-Type", "text/html; charset=utf-8"));

      Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.INVOKE);

      HttpResponse resp = (HttpResponse) inv.execute(null, URL_TESTINVOKE, "PUT", params);
      Assert.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
      String body = readResponseBody(resp);
      log.debug("'" + body + "'");
      Assert.assertNotNull(body);
      Assert.assertTrue(body.toLowerCase().startsWith(
            "testinvoke done: ente1=erpel ; pferdö1=?rüpel ; headers: content-type = text/html; charset=utf-8 ; cibet_controlevent = invoke ; content-length = 0 "));
      // TestInvoke done: Ente1=Erpel ; PferdÖ1=?Rüpel ; HEADERS: content-type = text/html; charset=utf-8 ;
      // cibet_controlevent = INVOKE ; content-length = 0 ; host = localhost:8788 ; connection = Keep-Alive ; user-agent
      // = Apache-HttpClient/4.5.2 (Java/1.8.0_66) ; accept-encoding = gzip,deflate ;
   }

   @Test
   public void requestHEAD() throws Exception {
      log.info("start requestHEAD()");

      HttpRequestInvoker inv = new HttpRequestInvoker();
      List<ResourceParameter> params = new LinkedList<ResourceParameter>();
      params.add(createParameter("Ente1", "Erpel"));
      params.add(createParameter("PferdÖ1", "?Rüpel"));

      Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.INVOKE);

      HttpResponse resp = (HttpResponse) inv.execute(null, URL_TESTINVOKE, "HEAD", params);
      Assert.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
      String body = readResponseBody(resp);
      Assert.assertNull(body);
      for (Header h : resp.getAllHeaders()) {
         log.debug(h.getName() + " = " + h.getValue());
      }
   }

   @Test
   public void requestTRACE() throws Exception {
      log.info("start requestTRACE()");

      Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.INVOKE);

      HttpRequestInvoker inv = new HttpRequestInvoker();
      HttpResponse resp = (HttpResponse) inv.execute(null, URL_TESTINVOKE, "TRACE", null);
      for (Header h : resp.getAllHeaders()) {
         log.debug(h.getName() + " = " + h.getValue());
      }
      log.debug("reason: " + resp.getStatusLine().getReasonPhrase());
      String body = readResponseBody(resp);
      Assert.assertNull(body);
      Assert.assertTrue(HttpStatus.SC_FORBIDDEN == resp.getStatusLine().getStatusCode()
            || HttpStatus.SC_METHOD_NOT_ALLOWED == resp.getStatusLine().getStatusCode());
   }

   @Test
   public void requestPOSTDoubledParams() throws Exception {
      String tenant = "test requestPOSTDoubledParams";
      log.info("start " + tenant);

      HttpRequestInvoker inv = new HttpRequestInvoker();
      List<ResourceParameter> params = new LinkedList<ResourceParameter>();
      params.add(createParameter("Ente1", "Erpel"));
      params.add(createDoubleParameter("PferdÖ1", "?Rüpel", "Schnaps"));
      params.add(createHeader("Content-Type", "text/html; charset=utf-8"));

      Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.INVOKE);

      HttpResponse resp = (HttpResponse) inv.execute(null, URL_TESTINVOKE, "POst", params);
      Assert.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
      String body = readResponseBody(resp);
      Assert.assertNotNull(body);
      Assert.assertTrue(body.startsWith("TestInvoke done: Ente1=Erpel ; PferdÖ1=?Rüpel|Schnaps| ; "));
      for (Header h : resp.getAllHeaders()) {
         log.debug(h.getName() + " = " + h.getValue());
      }
   }

   @Test
   public void requestGETDoubledParams() throws Exception {
      String tenant = "test requestGETDoubledParams";
      log.info("start " + tenant);

      HttpRequestInvoker inv = new HttpRequestInvoker();
      List<ResourceParameter> params = new LinkedList<ResourceParameter>();
      params.add(createParameter("Ente1", "Erpel"));
      params.add(createDoubleParameter("PferdÖ1", "?Rüpel", "Schnaps"));
      params.add(createHeader("Warzenschwein", "Übelkeit"));
      params.add(createHeader("Content-Type", "text/html; charset=utf-8"));

      Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.INVOKE);

      HttpResponse resp = (HttpResponse) inv.execute(null, URL_TESTINVOKE, "GEt", params);
      Assert.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
      String body = readResponseBody(resp);
      Assert.assertNotNull(body);
      Assert.assertTrue(body.startsWith("TestInvoke done: Ente1=Erpel ; PferdÖ1=?Rüpel|Schnaps| ; "));
      for (Header h : resp.getAllHeaders()) {
         log.debug(h.getName() + " = " + h.getValue());
      }
   }

   /**
    * Tomcat changes header names to lower case, Jetty let as is.
    * 
    * @throws Exception
    */
   @Test
   public void requestPOSTWithHeaders() throws Exception {
      log.info("start requestPOSTWithHeaders()");

      HttpRequestInvoker inv = new HttpRequestInvoker();
      List<ResourceParameter> params = new LinkedList<ResourceParameter>();
      params.add(createParameter("Ente1", "Erpel"));
      params.add(createParameter("PferdÖ1", "?Rüpel"));
      params.add(createParameter("PferdÖ1", "Schnaps"));
      params.add(createHeader("Warzenschwein", "Übelkeit"));
      params.add(createHeader("Content-Type", "text/html; charset=utf-8"));

      Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.INVOKE);

      HttpResponse resp = (HttpResponse) inv.execute(null, URL_TESTINVOKE, "post", params);
      Assert.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
      String body = readResponseBody(resp);
      Assert.assertNotNull(body);
      Assert.assertTrue(body.startsWith("TestInvoke done: Ente1=Erpel ; PferdÖ1=?Rüpel|Schnaps| ; "));
      int index = -1;
      // if ("EmbeddedTomcat7".equals(container.getName())) {
      // index = body.indexOf("warzenschwein = �belkeit ; ");
      // } else {
      index = body.toLowerCase().indexOf("warzenschwein = übelkeit ; ");
      // }
      Assert.assertTrue(index > 0);
      for (Header h : resp.getAllHeaders()) {
         log.debug(h.getName() + " = " + h.getValue());
      }
   }

   @Test
   public void requestPOSTWithMultiHeaders() throws Exception {
      log.info("start requestPOSTWithMultiHeaders()");

      HttpRequestInvoker inv = new HttpRequestInvoker();
      List<ResourceParameter> params = new LinkedList<ResourceParameter>();
      params.add(createParameter("Ente1", "Erpel"));
      params.add(createParameter("PferdÖ1", "?Rüpel"));
      params.add(createParameter("PferdÖ1", "Schnaps"));
      params.add(createDoubleHeader("Warzenschwein", "Übelkeit", "Fahrenheit"));
      params.add(createHeader("Content-Type", "text/html; charset=utf-8"));

      Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, ControlEvent.INVOKE);

      HttpResponse resp = (HttpResponse) inv.execute(null, URL_TESTINVOKE, "post", params);
      Assert.assertEquals(HttpStatus.SC_OK, resp.getStatusLine().getStatusCode());
      String body = readResponseBody(resp);
      Assert.assertNotNull(body);
      Assert.assertTrue(body.startsWith("TestInvoke done: Ente1=Erpel ; PferdÖ1=?Rüpel|Schnaps| ; "));
      int index = -1;
      // if ("EmbeddedTomcat7".equals(container.getName())) {
      // index = body.indexOf("warzenschwein = Fahrenheit ; ");
      // } else {
      index = body.toLowerCase().indexOf("warzenschwein = fahrenheit ; ");
      // }
      Assert.assertTrue(index > 0);
      for (Header h : resp.getAllHeaders()) {
         log.debug(h.getName() + " = " + h.getValue());
      }
   }

}
