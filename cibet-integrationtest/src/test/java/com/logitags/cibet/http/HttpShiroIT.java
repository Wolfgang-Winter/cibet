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
import org.apache.log4j.Logger;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.jboss.shrinkwrap.resolver.api.maven.ScopeType;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenDependencies;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.ShiroServlet;
import com.logitags.cibet.it.AbstractArquillian;

@RunWith(Arquillian.class)
public class HttpShiroIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpShiroIT.class);

   protected String URL_CONFIG = "/config";

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = HttpShiroIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-shiro.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class,
            ShiroServlet.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-shiro").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      File[] shiro1 = Maven.resolver()
            .addDependencies(MavenDependencies.createDependency("org.apache.shiro:shiro-web:1.2.2", ScopeType.COMPILE,
                  false, MavenDependencies.createExclusion("org.slf4j:slf4j-api")))
            .resolve().withTransitivity().asFile();
      archive.addAsLibraries(shiro1);

      archive.addAsWebInfResource("it/config_webshiro.xml", "classes/cibet-config.xml");
      archive.addAsWebInfResource("shiro.ini", "classes/shiro.ini");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @Override
   @After
   public void afterAbstractArquillian() throws Exception {
      log.info("execute sub-afterAbstractArquillian(");
      HttpGet method = new HttpGet(getBaseURL() + "/logout");
      client.execute(method);
      method.abort();
   }

   @Test
   public void testGrantShiro() throws Exception {
      log.info("start testGrantShiro()");

      String user = URLEncoder.encode("lonestarr:vespa", "UTF-8");
      HttpGet method = new HttpGet(getBaseURL() + "/loginShiro?shiroUser=" + user);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro1");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testDenyShiro() throws Exception {
      log.info("start testDenyShiro()");
      String user = URLEncoder.encode("lonestarr:vespa", "UTF-8");
      HttpGet method = new HttpGet(getBaseURL() + "/loginShiro?shiroUser=" + user);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro2");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testGrantGuestShiro() throws Exception {
      log.info("start testGrantGuestShiro()");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro2");
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testDenyShiro2() throws Exception {
      log.info("start testDenyShiro2()");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro1");
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testDenyShiroRoles() throws Exception {
      log.info("start testDenyShiroRoles()");
      String user = URLEncoder.encode("lonestarr:vespa", "UTF-8");
      HttpGet method = new HttpGet(getBaseURL() + "/loginShiro?shiroUser=" + user);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro3");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testGrantShiroRoles() throws Exception {
      log.info("start testGrantShiroRoles()");
      String user = URLEncoder.encode("lonestarr:vespa", "UTF-8");
      HttpGet method = new HttpGet(getBaseURL() + "/loginShiro?shiroUser=" + user);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro4");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testGrantShiroPerm() throws Exception {
      log.info("start testGrantShiroPerm()");
      String user = URLEncoder.encode("lonestarr:vespa", "UTF-8");
      HttpGet method = new HttpGet(getBaseURL() + "/loginShiro?shiroUser=" + user);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro5");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testdenyShiroPerm() throws Exception {
      log.info("start testdenyShiroPerm()");
      String user = URLEncoder.encode("lonestarr:vespa", "UTF-8");
      HttpGet method = new HttpGet(getBaseURL() + "/loginShiro?shiroUser=" + user);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro6");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testGrantShiroUser() throws Exception {
      log.info("start testGrantShiroUser()");
      String user = URLEncoder.encode("lonestarr:vespa", "UTF-8");
      HttpGet method = new HttpGet(getBaseURL() + "/loginShiro?shiroUser=" + user);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiro7");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

   @Test
   public void testGrantShiroSecond() throws Exception {
      log.info("start testGrantShiroSecond()");
      String user = URLEncoder.encode("lonestarr:vespa", "UTF-8");
      HttpGet method = new HttpGet(getBaseURL() + "/loginShiro?shiroUser=" + user);
      HttpResponse response = client.execute(method);
      method.abort();

      log.debug("now the test");
      HttpGet g = new HttpGet(getBaseURL() + "/ts/shiroSecond");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();

      log.debug("now the test next");
      g = new HttpGet(getBaseURL() + "/ts/shiro1");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String result = readResponseBody(response);
      g.abort();
      Assert.assertEquals("SHIRO: true lonestarr | darkhelmet darkhelmet", result);

      Thread.sleep(50);
      log.debug("now the grant test ");
      g = new HttpGet(getBaseURL() + "/ts/shiro9");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();

      Thread.sleep(50);
      log.debug("now the deny test ");
      g = new HttpGet(getBaseURL() + "/ts/shiro8");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatusLine().getStatusCode());
      readResponseBody(response);
      g.abort();
   }

}
