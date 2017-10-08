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
/**
 * 
 */
package com.logitags.cibet.control;

import java.util.List;

import javax.persistence.Query;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.DBHelper;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.jpa.JpaResource;

/**
 *
 */
public class StateChangeIntegrationTest extends DBHelper {

   /**
    * logger for tracing
    */
   private static Logger log = Logger.getLogger(StateChangeIntegrationTest.class);

   private Configuration cman = Configuration.instance();

   @Test
   public void evaluateSameEntityManager() throws Exception {
      log.info("start evaluateSameEntityManager()");
      initConfiguration("config_stateChange1_invoker.xml");

      TEntity te = createTEntity(5, "valuexx");
      applEman.persist(te);
      applEman.flush();

      te.setCounter(12);

      Query q = applEman.createNamedQuery(TEntity.SEL_BY_OWNER);
      q.setParameter("owner", TENANT);
      List<TEntity> l = q.getResultList();
      Context.internalRequestScope().setApplicationEntityManager2(null);

      JpaResource res = new JpaResource(te);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      try {
         evaluate(md, cman.getSetpoints(), new StateChangeControl());
         Assert.fail();
      } catch (Exception e) {
         Assert.assertTrue(
               e.getMessage().startsWith("failed to load object from database: " + "Set the internal EntityManager"));
      }
   }

   @Test
   public void evaluateExclude1u2() throws Exception {
      log.info("start evaluateExclude1u2()");
      initConfiguration("config_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TEntity te = createTEntity(5, "valuexx");
      applEman.persist(te);
      applEman.flush();
      applEman.getTransaction().commit();
      te.setCounter(12);

      JpaResource res = new JpaResource(te);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());

      te.setCounter(5);
      te.setNameValue("Wein");
      res = new JpaResource(te);
      md = new EventMetadata(ControlEvent.UPDATE, res);
      list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateExclude2a() throws Exception {
      log.info("start evaluateExclude2a()");
      initConfiguration("config_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TEntity te = createTEntity(5, "valuexx");
      applEman.persist(te);
      applEman.flush();
      applEman.getTransaction().commit();

      te.setCounter(12);
      te.setNameValue("Wein");

      JpaResource res = new JpaResource(te);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateExcludeSimpleAttribute() throws Exception {
      log.info("start evaluateExcludeSimpleAttribute()");
      initConfiguration("config_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity cte = createTComplexEntity();
      applEman.persist(cte);
      applEman.getTransaction().commit();

      cte.getTen().setCounter(12);

      JpaResource res = new JpaResource(cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateExcludeList() throws Exception {
      log.info("start evaluateExcludeList()");
      initConfiguration("config_condition_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity cte = createTComplexEntity();
      applEman.persist(cte);
      applEman.getTransaction().commit();
      applEman.clear();

      TComplexEntity cte2 = applEman.find(TComplexEntity.class, cte.getId());
      TEntity t = cte2.getLazyList().iterator().next();
      cte2.getLazyList().remove(t);

      JpaResource res = new JpaResource(cte2);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateExcludeSimpleAttributes() throws Exception {
      log.info("start evaluateExcludeSimpleAttributes()");
      initConfiguration("config_condition_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity cte = createTComplexEntity();
      applEman.persist(cte);
      applEman.getTransaction().commit();

      TComplexEntity cte2 = applEman.find(TComplexEntity.class, cte.getId());
      cte2.setOwner("Hase2");

      JpaResource res = new JpaResource(cte2);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateIncludeSimpleAttribute() throws Exception {
      log.info("start evaluateIncludeSimpleAttribute()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");

      TEntity te = createTEntity(5, "Hase");
      applEman.persist(te);
      applEman.getTransaction().commit();

      te.setCounter(12);

      Context.internalRequestScope().setApplicationEntityManager2(null);
      JpaResource res = new JpaResource(te);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      try {
         evaluate(md, cman.getSetpoints(), new StateChangeControl());
         Assert.fail();
      } catch (RuntimeException e) {
         Assert.assertTrue(
               e.getMessage().startsWith("failed to load object from database: " + "Set the internal EntityManager"));
      }

      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());

      te.setCounter(5);
      te.setNameValue("Wein");
      res = new JpaResource(te);
      md = new EventMetadata(ControlEvent.UPDATE, res);
      list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(0, list.size());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateIncludeSimpleAttribute2() throws Exception {
      log.info("start evaluateIncludeSimpleAttribute2()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity cte = createTComplexEntity();
      applEman.persist(cte);
      applEman.getTransaction().commit();

      cte.getTen().setCounter(12);

      // CibetContext.setApplicationEntityManager2(createEntityManager());
      JpaResource res = new JpaResource(cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateIncludeSimpleAttribute2NewLoaded() throws Exception {
      log.info("start evaluateIncludeSimpleAttribute2NewLoaded()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity cte = createTComplexEntity();
      applEman.persist(cte);
      applEman.getTransaction().commit();
      applEman.clear();

      TComplexEntity cte2 = applEman.find(TComplexEntity.class, cte.getId());
      cte2.getTen().setCounter(12);

      JpaResource res = new JpaResource(cte2);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateIncludeList() throws Exception {
      log.info("start evaluateIncludeList()");
      initConfiguration("config_stateChange2_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity cte = createTComplexEntity();
      applEman.persist(cte);
      applEman.getTransaction().commit();
      applEman.clear();

      TComplexEntity cte2 = applEman.find(TComplexEntity.class, cte.getId());
      TEntity t = cte2.getLazyList().iterator().next();
      cte2.getLazyList().remove(t);

      JpaResource res = new JpaResource(cte2);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateIncludeNothingIncluded() throws Exception {
      log.info("start evaluateIncludeNothingIncluded()");
      initConfiguration("config_stateChange2_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity cte = createTComplexEntity();
      applEman.persist(cte);
      applEman.getTransaction().commit();
      applEman.clear();

      TComplexEntity cte2 = applEman.find(TComplexEntity.class, cte.getId());
      cte2.setOwner("Hase");

      JpaResource res = new JpaResource(cte2);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(0, list.size());
      applEman.getTransaction().begin();
   }

   @Test
   public void evaluateNoChange() throws Exception {
      log.info("start evaluateNoChange()");
      initConfiguration("config_stateChange2_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(fac.createEntityManager());

      TComplexEntity cte = createTComplexEntity();
      applEman.persist(cte);
      applEman.getTransaction().commit();
      applEman.clear();

      // CibetContext.setApplicationEntityManager2(createEntityManager());
      JpaResource res = new JpaResource(cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, cman.getSetpoints(), new StateChangeControl());
      Assert.assertEquals(0, list.size());
      applEman.getTransaction().begin();
   }
}
