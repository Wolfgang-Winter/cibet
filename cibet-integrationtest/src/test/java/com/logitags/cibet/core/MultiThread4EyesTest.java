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

package com.logitags.cibet.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.actuator.archive.ArchiveActuator;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.actuator.dc.ResourceApplyException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.resource.Resource;

/**
 * -javaagent:D:\Java\maven-repository\org\aspectj\aspectjweaver\1.8.8\aspectjweaver-1.8.8.jar
 */
public class MultiThread4EyesTest extends DBHelper {

   /**
    * logger for tracing
    */
   private static Logger log = Logger.getLogger(MultiThread4EyesTest.class);

   private static List<TResult> resultList = Collections.synchronizedList(new ArrayList<TResult>());

   @Override
   @Before
   public void doBefore() throws Exception {
      log.debug("MultiThread4EyesTest.doBefore");
   }

   @After
   public void afterMultiThread4EyesTest() throws Exception {
      log.info("afterMultiThread4EyesTest() ...");
      resultList.clear();
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

      private EntityManager appcem;

      private String user;

      public ThreadExecution(String name, String user) {
         super(name);
         this.user = user;
      }

      @CibetContext
      public void run() {
         appcem = fac.createEntityManager();
         run2();
      }

      private void run2() {
         log.info("run runid 2 for owner " + Context.sessionScope().getTenant());
         Context.sessionScope().setTenant(getName());
         Context.sessionScope().setUser(user);

         EntityManager cem = Context.requestScope().getEntityManager();

         TEntity entity = persistTEntity();

         cem.getTransaction().commit();
         cem.getTransaction().begin();

         TEntity selEnt = appcem.find(TEntity.class, entity.getId());
         if (selEnt != null) {
            resultList.add(new TResult(getName(), 246, null));
         }
         Assert.assertNull(selEnt);

         Query q = cem.createQuery("SELECT a FROM Archive a WHERE a.tenant = :tenant");
         q.setParameter("tenant", Context.sessionScope().getTenant());
         List<Archive> list = q.getResultList();

         if (list.size() != 1) {
            log.error("THREADERROR");
            resultList.add(new TResult(getName(), 256, list.size()));
         }
         Assert.assertEquals(1, list.size());
         Archive ar = list.get(0);
         Resource resource = ar.getResource();

         if (ControlEvent.INSERT != ar.getControlEvent()) {
            resultList.add(new TResult(getName(), 262, ar.getControlEvent()));
         }
         Assert.assertEquals(ControlEvent.INSERT, ar.getControlEvent());

         if (!"0".equals(resource.getPrimaryKeyId())) {
            resultList.add(new TResult(getName(), 268, resource.getPrimaryKeyId()));
         }
         Assert.assertEquals("0", resource.getPrimaryKeyId());

         q = cem.createQuery("SELECT a FROM DcControllable a WHERE a.tenant = :tenant");
         q.setParameter("tenant", Context.sessionScope().getTenant());
         List<DcControllable> list1 = q.getResultList();

         if (list1.size() != 1) {
            resultList.add(new TResult(getName(), 278, list1.size()));
         }
         Assert.assertEquals(1, list1.size());
         DcControllable dcOb = list1.get(0);

         if (ControlEvent.INSERT != dcOb.getControlEvent()) {
            resultList.add(new TResult(getName(), 284, dcOb.getControlEvent()));
         }
         Assert.assertEquals(ControlEvent.INSERT, dcOb.getControlEvent());

         appcem.clear();
         List<DcControllable> l = DcLoader.findUnreleased(TEntity.class.getName());

         if (l.size() != 1) {
            resultList.add(new TResult(getName(), 292, l.size()));
         }
         Assert.assertEquals(1, l.size());

         DcControllable co = l.get(0);

         Context.sessionScope().setUser("test22");
         TEntity afterReleaseEntity = null;
         try {
            appcem.getTransaction().begin();
            afterReleaseEntity = (TEntity) co.release(appcem, null);
            appcem.getTransaction().commit();

            cem.getTransaction().commit();
            cem.getTransaction().begin();

         } catch (ResourceApplyException e) {
            log.error(e.getMessage(), e);
            Assert.fail();
         }

         l = DcLoader.findUnreleased(TEntity.class.getName());

         if (l.size() != 0) {
            resultList.add(new TResult(getName(), 310, l.size()));
         }
         Assert.assertEquals(0, l.size());

         appcem.clear();
         TEntity te = appcem.find(TEntity.class, afterReleaseEntity.getId());

         if (te == null) {
            resultList.add(new TResult(getName(), 319, null));
         }
         Assert.assertNotNull(te);
      }

      protected TEntity persistTEntity() {
         EntityTransaction etx = appcem.getTransaction();
         etx.begin();
         TEntity entity = new TEntity();
         entity.setCounter(5);
         entity.setNameValue("valuexx");
         entity.setOwner(Context.sessionScope().getTenant());
         try {
            appcem.persist(entity);
         } catch (PostponedException e) {
         }
         etx.commit();
         return entity;
      }

   }

   private void startThreads(int nbr, List<String> schemes, int delay) throws Exception {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()), null);
      sp.setTarget(TEntity.class.getName());
      sp.setEvent(ControlEvent.INSERT.name(), ControlEvent.UPDATE.name(), ControlEvent.DELETE.name(),
            ControlEvent.RELEASE_INSERT.name());
      Configuration cman = Configuration.instance();
      for (String scheme : schemes) {
         sp.addActuator(cman.getActuator(scheme));
      }
      cman.registerSetpoint(sp);

      List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
      for (int i = 0; i < nbr; i++) {
         ThreadExecution t = new ThreadExecution("thread-" + i, "user-" + i);
         tlist.add(t);
      }

      log.info("start threads");
      for (ThreadExecution te : tlist) {
         te.start();
         Thread.sleep(delay);
      }
      Thread.sleep(500);
      log.info("join threads");
      for (ThreadExecution te : tlist) {
         te.join();
      }
      Thread.sleep(500);
      log.info("threads joined");
   }

   @Test
   public void multipleThreads4Eyes() throws Exception {
      log.info("start multipleThreads4Eyes()");
      // if (skip || database.getName().indexOf("MySql") > -1) {
      // // InnoDB has problems with Deadlock resolution:
      // // http://dba.stackexchange.com/questions/4494/is-oracle-db-immune-to-the-innodb-deadlocks-found-in-mysql
      // log.info("not possible for jdbc or MySql");
      // return;
      // }
      // InitializationService.instance();

      int nbr = 10;

      List<String> schemes = new ArrayList<String>();
      schemes.add(FourEyesActuator.DEFAULTNAME);
      schemes.add(ArchiveActuator.DEFAULTNAME);

      int delay = 100;

      startThreads(nbr, schemes, delay);

      for (TResult tr : resultList) {
         log.info("THREADRESULTS: " + tr.thread + ": " + tr.lineNumber + ": " + tr.value);
      }
      Assert.assertEquals(0, resultList.size());

      log.info("checking ...");
      Query q = Context.requestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL);
      List<Archive> list = q.getResultList();
      Assert.assertEquals(nbr * 2, list.size());
      Assert.assertEquals(ControlEvent.INSERT, list.get(0).getControlEvent());
   }

}
