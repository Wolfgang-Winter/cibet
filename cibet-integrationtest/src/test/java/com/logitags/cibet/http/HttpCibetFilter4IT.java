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
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.it.AbstractArquillian;

@RunWith(Arquillian.class)
public class HttpCibetFilter4IT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpCibetFilter4IT.class);

   @Deployment
   public static WebArchive createDeployment() {
      String warName = HttpCibetFilter4IT.class.getSimpleName() + ".war";
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
      archive.addAsWebInfResource("it/config_web2.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("spring-context_1.xml", "classes/spring-context.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Before
   public void beforeHttpCibetFilterIT() {
      log.debug("execute before()");
      InitializationService.instance().startContext();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      log.debug("end execute before()");
   }

   @After
   public void afterHttpCibetFilterIT() {
      InitializationService.instance().endContext();
      new ConfigurationService().reinitSetpoints();
   }

   @Ignore
   @Test
   public void testEjbSecurity() throws Exception {
      log.info("start testEjbSecurity()");
      HttpOptions getMethod = new HttpOptions(getBaseURL() + "/test/context?expVoter=1&parGutmann="
            + URLEncoder.encode("Ösal12345", "UTF-8") + "&role=" + URLEncoder.encode("admin", "UTF-8"));
      getMethod.addHeader("HTTP_X_FORWARDED_FOR", "192.68.2.1");

      HttpResponse response = client.execute(getMethod);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      getMethod.abort();
   }

}
