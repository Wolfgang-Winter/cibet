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
package com.logitags.cibet.it;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ejb.EJB;

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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.ejb.CibetTestEJB;
import com.cibethelper.entities.AbstractTEntity;
import com.cibethelper.entities.ITComplexEntity;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TComplexEntity2;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.Resource;

@RunWith(Arquillian.class)
public class MultiThreadIT extends AbstractArquillian {

   private static Logger log = Logger.getLogger(MultiThreadIT.class);

   @EJB
   private CibetTestEJB ejb;

   private static List<TResult> resultList = Collections.synchronizedList(new ArrayList<TResult>());

   @After
   public void afterMultiThreadIT() throws Exception {
      log.info("afterMultiThreadIT() ...");
      resultList.clear();
   }

   @Deployment
   public static WebArchive createDeployment() {
      String warName = MultiThreadIT.class.getSimpleName() + ".war";
      WebArchive archive = ShrinkWrap.create(WebArchive.class, warName);
      archive.setWebXML("it/web2.xml");

      archive.addClasses(AbstractArquillian.class, CoreTestBase.class, AbstractTEntity.class, TEntity.class,
            TComplexEntity.class, TComplexEntity2.class, ITComplexEntity.class, TCompareEntity.class,
            CibetTestEJB.class);

      File[] cibet = Maven.resolver().loadPomFromFile("pom.xml").resolve("com.logitags:cibet-jpa").withTransitivity()
            .asFile();
      archive.addAsLibraries(cibet);

      archive.addAsWebInfResource("META-INF/persistence-it-derby.xml", "classes/META-INF/persistence.xml");
      archive.addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");

      log.debug(archive.toString(true));
      archive.as(ZipExporter.class).exportTo(new File("target/" + warName), true);

      return archive;
   }

   private class TResult {
      public int lineNumber;
      public Object value;
      public String thread;

      public TResult(String t, int ln, Object v) {
         thread = t;
         lineNumber = ln;
         value = v;
      }
   }

   private class ThreadExecution extends Thread {

      private int counter;

      private String user;

      private int testNumber;

      public ThreadExecution(int testnbr, int count) {
         super("thread-" + count);
         this.user = "user-" + count;
         testNumber = testnbr;
         counter = count;
      }

      public void run() {
         InitializationService.instance().startContext();
         Context.sessionScope().setTenant(getName());
         Context.sessionScope().setUser(user);
         try {
            switch (testNumber) {
            case 1:
               run1();
               break;
            case 2:
               run2();
               break;
            }
         } catch (Exception e) {
            log.error(e.getMessage(), e);
            resultList.add(new TResult(getName(), 161, null));
            throw new RuntimeException(e);
         }
         InitializationService.instance().endContext();
      }

      private void run1() throws Exception {
         log.info("run thread 1 for owner " + Context.sessionScope().getTenant());
         TEntity entity = createTEntity(counter, Context.sessionScope().getTenant());
         entity = ejb.persist(entity);

         try {
            TEntity selEnt = ejb.findTEntity(entity.getId());
            Assert.assertNotNull(selEnt);
            Assert.assertEquals(counter, selEnt.getCounter());
            Assert.assertEquals(getName(), selEnt.getNameValue());

            List<Archive> list = ejb.queryArchive(getName(), TEntity.class.getName(), String.valueOf(entity.getId()));
            Assert.assertEquals(1, list.size());
            Assert.assertEquals(Context.sessionScope().getUser(), list.get(0).getCreateUser());
         } catch (AssertionError e) {
            resultList.add(new TResult(getName(), 161, null));
            log.error(e.getMessage(), e);
         }
      }

      private void run2() throws Exception {
         log.info("run thread 2 for owner " + Context.sessionScope().getTenant());
         TEntity entity = createTEntity(counter, Context.sessionScope().getTenant());
         entity = ejb.persist(entity);
         TEntity selEnt = ejb.findTEntity(entity.getId());
         Assert.assertNull(selEnt);

         List<Archive> list = ejb.findArchives();
         Assert.assertEquals(1, list.size());
         Resource res = list.get(0).getResource();
         Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
         Assert.assertEquals("0", res.getPrimaryKeyId());

         List<DcControllable> list1 = ejb.queryDcControllable();
         Assert.assertEquals(1, list1.size());
         Assert.assertEquals(ControlEvent.INSERT, list1.get(0).getControlEvent());

         // Release
         List<DcControllable> li = ejb.findUnreleased();
         DcControllable co = li.get(0);
         Context.sessionScope().setUser("test22");
         ejb.release(co);

         li = ejb.findUnreleased();
         Assert.assertEquals(0, li.size());

         list = ejb.findArchives();
         Assert.assertEquals(2, list.size());
         res = list.get(0).getResource();
         Resource res1 = list.get(1).getResource();

         Assert.assertEquals(res.getPrimaryKeyId(), res1.getPrimaryKeyId());
         TEntity te = ejb.findTEntity(Long.parseLong(res.getPrimaryKeyId()));
         Assert.assertNotNull(te);
      }
   }

   @Test
   public void startMultipleThreadsArchive() throws Exception {
      log.info("start startMultipleThreadsArchive()");
      int nbr = 10;

      registerSetpoint(TEntity.class.getName(), ArchiveActuator.DEFAULTNAME, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.DELETE);

      List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
      for (int i = 0; i < nbr; i++) {
         ThreadExecution t = new ThreadExecution(1, i);
         tlist.add(t);
      }

      int delay = 15;
      log.info("start threads");
      for (ThreadExecution te : tlist) {
         te.start();
         Thread.sleep(delay);
      }

      log.info("now join threads");
      for (ThreadExecution te : tlist) {
         te.join();
      }

      log.info("checking ...");
      List<Archive> list = ejb.queryArchives();
      Assert.assertEquals(nbr, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());

      for (TResult tr : resultList) {
         log.info("THREADRESULTS: " + tr.thread + ": " + tr.lineNumber + ": " + tr.value);
      }
      Assert.assertEquals(0, resultList.size());
   }

   @Test
   public void startMultipleThreads4Eyes() throws Exception {
      log.info("start startMultipleThreads4Eyes()");

      int nbr = 10;

      List<String> schemes = new ArrayList<String>();
      schemes.add(ArchiveActuator.DEFAULTNAME);
      schemes.add(FourEyesActuator.DEFAULTNAME);
      registerSetpoint(TEntity.class.getName(), schemes, ControlEvent.INSERT, ControlEvent.UPDATE,
            ControlEvent.RELEASE_INSERT);

      List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
      for (int i = 0; i < nbr; i++) {
         ThreadExecution t = new ThreadExecution(2, i);
         tlist.add(t);
      }

      int delay = 15;

      log.info("start threads");
      for (ThreadExecution te : tlist) {
         te.start();
         Thread.sleep(delay);
      }

      log.info("now join threads");
      for (ThreadExecution te : tlist) {
         te.join();
      }

      log.info("checking ...");
      List<Archive> list = ejb.queryArchives();
      Assert.assertEquals(nbr * 2, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());

      for (TResult tr : resultList) {
         log.info("THREADRESULTS: " + tr.thread + ": " + tr.lineNumber + ": " + tr.value);
      }
      Assert.assertEquals(0, resultList.size());
   }

}
