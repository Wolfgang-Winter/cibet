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
import java.util.List;

import javax.persistence.Query;

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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.DBHelper;
import com.cibethelper.ejb.EjbService;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.cibethelper.servlet.PersistServlet;
import com.cibethelper.servlet.ShiroServlet;
import com.logitags.cibet.it.AbstractArquillian;

@RunWith(Arquillian.class)
public class HttpCibetContextFilterIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpCibetContextFilterIT.class);

   @Deployment(testable = false)
   public static WebArchive createDeployment() {
      String warName = HttpCibetContextFilterIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web-persist.xml");

      archive.addClasses(AbstractTEntity.class, TEntity.class, TComplexEntity.class, TComplexEntity2.class,
            ITComplexEntity.class, TCompareEntity.class, PersistServlet.class, ShiroServlet.class, EjbService.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa20").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it-derby.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   @BeforeClass
   public static void beforeClassHttpCibetContextFilterIT() throws Exception {
      DBHelper.beforeClass();
   }

   @Override
   @After
   public void afterAbstractArquillian() throws Exception {
      log.info("execute sub-afterAbstractArquillian(");
      HttpGet method = new HttpGet(getBaseURL() + "/logout");
      client.execute(method);
      method.abort();

      new DBHelper().doAfter();
   }

   @Test
   public void login() throws Exception {
      log.debug("start login()");

      HttpGet g = new HttpGet(getBaseURL() + "/login");
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String answer = readResponseBody(response);
      Assert.assertEquals("Login done", answer);
      g.abort();

      log.debug("now after login");
      g = new HttpGet(getBaseURL() + "/afterLogin");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("after Login: Fred2Freds tenantfreds name4", answer);
      g.abort();

      log.debug("now after logout");
      g = new HttpGet(getBaseURL() + "/logout");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("Logout done", answer);
      g.abort();

      log.debug("now after login 2");
      g = new HttpGet(getBaseURL() + "/afterLogin");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      // Following assert may fail due to thread inheritance of CibetContext
      Assert.assertTrue(
            "after Login: null__DEFAULT--".equals(answer) || "after Login: USERtestTenant--".equals(answer));
   }

   @Test
   public void withEntityManager() throws Exception {
      log.debug("start withEntityManager()");

      HttpGet g = new HttpGet(getBaseURL() + "/login");
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String answer = readResponseBody(response);
      Assert.assertEquals("Login done", answer);
      g.abort();

      log.debug("now persist");
      g = new HttpGet(getBaseURL() + "/persist");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("Persist done", answer);
      g.abort();
      Thread.sleep(200);

      Query q = DBHelper.applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", "hansis owner");
      List<TEntity> l = q.getResultList();
      Assert.assertEquals(1, l.size());

      log.debug("now persist 2");
      g = new HttpGet(getBaseURL() + "/persist");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("Persist done", answer);
      g.abort();

      q = DBHelper.applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", "hansis owner");
      l = q.getResultList();
      Assert.assertEquals(2, l.size());
   }

   @Test
   public void withEJBEntityManager() throws Exception {
      log.debug("start withEJBEntityManager()");

      HttpGet g = new HttpGet(getBaseURL() + "/login");
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      String answer = readResponseBody(response);
      Assert.assertEquals("Login done", answer);
      g.abort();

      log.debug("now persist2");
      g = new HttpGet(getBaseURL() + "/persist2");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("Persist2 done", answer);
      g.abort();

      Query q = DBHelper.applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", "Ganz");
      List<TEntity> l = q.getResultList();
      Assert.assertEquals(1, l.size());

      log.debug("now persist another");
      g = new HttpGet(getBaseURL() + "/persist2");
      response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      answer = readResponseBody(response);
      Assert.assertEquals("Persist2 done", answer);
      g.abort();

      q = DBHelper.applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", "Ganz");
      l = q.getResultList();
      Assert.assertEquals(2, l.size());
   }

}
