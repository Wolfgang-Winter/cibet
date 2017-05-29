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
import java.util.Calendar;
import java.util.List;

import javax.persistence.EntityManager;
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
import com.logitags.cibet.actuator.scheduler.SchedulerActuator;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ConfigurationService;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.it.AbstractArquillian;

@RunWith(Arquillian.class)
public class HttpSchedulerIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(HttpSchedulerIT.class);

   protected String URL_TS = getBaseURL() + "/test/ts";

   @Deployment
   public static WebArchive createDeployment() {
      String warName = HttpSchedulerIT.class.getSimpleName() + ".war";
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
   public void beforeHttpSchedulerIT() {
      log.debug("execute before()");
      Context.start();
      Context.sessionScope().setUser(USER);
      Context.sessionScope().setTenant(TENANT);
      cman = Configuration.instance();
      log.debug("end execute before()");
   }

   @After
   public void afterHttpSchedulerIT() {
      Context.end();
      new ConfigurationService().reinitSetpoints();
   }

   @Test
   public void testScheduled() throws Exception {
      log.info("start testScheduled()");

      Calendar cal = Calendar.getInstance();
      cal.add(Calendar.SECOND, 6);
      SchedulerActuator sa = new SchedulerActuator("sa2");
      sa.setTimerStart(cal.getTime());
      sa.setPersistenceContextReference("persistence/Cibet2pp");
      cman.registerActuator(sa);

      List<String> schemes = new ArrayList<String>();
      schemes.add("sa2");
      registerSetpoint(URL_TS, schemes, ControlEvent.INVOKE);

      log.debug("now the test");
      Calendar cal2 = Calendar.getInstance();
      cal2.add(Calendar.SECOND, 3);

      HttpGet g = new HttpGet(URL_TS);
      g.addHeader("CIBET_SCHEDULEDDATE", new Long(cal2.getTimeInMillis()).toString());
      HttpResponse response = client.execute(g);
      Assert.assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());
      readResponseBody(response);

      EntityManager cem = Context.requestScope().getEntityManager();
      Query q = cem.createQuery("SELECT c FROM Controllable c");
      List<Controllable> list = q.getResultList();
      Assert.assertEquals(1, list.size());
      Controllable dc = list.get(0);
      Assert.assertEquals(ExecutionStatus.SCHEDULED, dc.getExecutionStatus());
      Assert.assertEquals("sa2", dc.getActuator());
      Assert.assertNull(dc.getExecutionDate());
      log.debug("-------------------- sleep");
      Thread.sleep(10000);
      log.debug("--------------- after TimerTask");

      cem.clear();
      // cem.getTransaction().commit();
      // cem.getTransaction().begin();
      q = cem.createQuery("SELECT c FROM Controllable c");
      list = q.getResultList();
      // cem.getTransaction().commit();
      Assert.assertEquals(1, list.size());
      dc = list.get(0);
      Assert.assertEquals(ExecutionStatus.EXECUTED, dc.getExecutionStatus());
      Assert.assertEquals("sa2", dc.getActuator());
      Assert.assertNotNull(dc.getExecutionDate());
   }

}
