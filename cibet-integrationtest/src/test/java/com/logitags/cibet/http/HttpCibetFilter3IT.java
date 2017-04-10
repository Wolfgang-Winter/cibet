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
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.it.AbstractArquillian;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.http.HttpRequestInvoker;

@RunWith(Arquillian.class)
public class HttpCibetFilter3IT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpCibetFilter3IT.class);

   @Deployment
   public static WebArchive createDeployment() {
      String warName = HttpCibetFilter3IT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

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
      archive.addAsWebInfResource("it/config_web1.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("spring-context_1.xml", "classes/spring-context.xml");

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

   private DcControllable checkDc(String method) throws Exception {
      log.debug("now check");
      List<DcControllable> list = null;
      for (int i = 1; i < 6; i++) {
         list = DcLoader.findUnreleased();
         if (1 == list.size())
            break;

         log.debug("No result. Try query again: " + i);
         Thread.sleep(400);
      }

      Assert.assertEquals(1, list.size());
      DcControllable ar = list.get(0);
      Resource res = ar.getResource();
      Assert.assertEquals(ControlEvent.INVOKE, ar.getControlEvent());
      Assert.assertEquals(getBaseURL() + "/othereee", res.getTargetType());
      Assert.assertEquals(method, res.getMethod());
      Assert.assertEquals(HttpRequestInvoker.class.getName(), res.getInvokerClass());
      return ar;
   }

   @Ignore
   @Test
   public void testEjbFourEyes() throws Exception {
      log.info("start testEjbFourEyes()");

      HttpGet getMethod = new HttpGet(getBaseURL() + "/test/context?expVoter=0&parGutmann="
            + URLEncoder.encode("Üsal12345", "UTF-8") + "&role=" + URLEncoder.encode("ROLE_ADMINI", "UTF-8"));
      getMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(getMethod);
      Assert.assertEquals(HttpStatus.SC_ACCEPTED, response.getStatusLine().getStatusCode());
      getMethod.abort();
      DcControllable dc = checkDc("GET");
      Resource res = dc.getResource();
      // if ("EmbeddedTomcat7".equals(container.getName())) {
      // // Tomcat7 adds an ATTRIBUTE 'org.apache.catalina.ASYNC_SUPPORTED'
      // Assert.assertEquals(8, res.getParameters().size());
      // } else {
      Assert.assertEquals(7, res.getParameters().size());
      // }
   }

}
