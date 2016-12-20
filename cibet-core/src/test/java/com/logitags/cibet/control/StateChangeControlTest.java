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
package com.logitags.cibet.control;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;
import com.logitags.cibet.sensor.pojo.MethodResourceHandler;

@RunWith(MockitoJUnitRunner.class)
public class StateChangeControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(StateChangeControlTest.class);

   @Mock
   private EntityManager em;

   @Mock
   private EntityManager em2;

   private List<Setpoint> evaluate(EventMetadata md, List<Setpoint> spoints) {
      Control eval = new StateChangeControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spoints) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         spi.getEffectiveControlValues(controlValues);
         Object value = controlValues.get("stateChange");
         if (value == null) {
            list.add(spi);
         } else {
            boolean okay = eval.evaluate(value, md);
            if (okay) {
               list.add(spi);
            }
         }
      }
      return list;
   }

   @Test
   public void evaluateMetadataTargetNull() throws Exception {
      log.info("start evaluateMetadataTargetNull()");

      Resource res = new Resource(JpaResourceHandler.class, null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> l = new ArrayList<Setpoint>();
      Setpoint s = new Setpoint("x");
      s.setStateChange("fast");
      l.add(s);
      try {
         Control eval = new StateChangeControl();
         eval.evaluate(l, md);
         Assert.fail();
      } catch (IllegalArgumentException e) {
      }
   }

   @Test
   public void evaluateSameEntityManager() throws Exception {
      log.info("start evaluateSameEntityManager()");
      initConfiguration("config_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(null);

      TEntity te = createTEntity(12, "Hase");
      te.setId(1);
      Mockito.when(em.find(TEntity.class, 1l)).thenReturn(te);

      try {
         Resource res = new Resource(JpaResourceHandler.class, te);
         EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
         Control eval = new StateChangeControl();
         eval.evaluate(Configuration.instance().getSetpoints(), md);
         Assert.fail();
      } catch (Exception e) {
         log.debug(e.getMessage(), e);
         Assert.assertTrue(
               e.getMessage().startsWith("failed to load object from database: " + "Set the internal EntityManager"));
      }
   }

   @Test
   public void evaluateExclude1() throws Exception {
      log.info("start evaluateExclude1()");
      initConfiguration("config_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(em2);

      TEntity te = createTEntity(12, "Hase");
      te.setId(1);
      Mockito.when(em.find(TEntity.class, 1l)).thenReturn(te);
      TEntity te2 = createTEntity(5, "Hase");
      te2.setId(1);
      Mockito.when(em2.find(TEntity.class, 1l)).thenReturn(te2);

      Resource res = new Resource(JpaResourceHandler.class, te);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
   }

   @Test
   public void evaluateExclude2() throws Exception {
      log.info("start evaluateExclude2()");
      initConfiguration("config_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(em2);

      TEntity te = createTEntity(5, "Wein");
      te.setId(1);
      Mockito.when(em.find(TEntity.class, 1l)).thenReturn(te);
      TEntity te2 = createTEntity(5, "Hase");
      te2.setId(1);
      Mockito.when(em2.find(TEntity.class, 1l)).thenReturn(te2);

      Resource res = new Resource(JpaResourceHandler.class, te);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

   @Test
   public void evaluateExclude3() throws Exception {
      log.info("start evaluateExclude3()");
      initConfiguration("config_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(em2);

      TEntity te = createTEntity(12, "Wein");
      te.setId(1);
      Mockito.when(em.find(TEntity.class, 1l)).thenReturn(te);
      TEntity te2 = createTEntity(5, "Hase");
      te2.setId(1);
      Mockito.when(em2.find(TEntity.class, 1l)).thenReturn(te2);

      Resource res = new Resource(JpaResourceHandler.class, te);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

   @Test
   public void evaluateExclude4() throws Exception {
      log.info("start evaluateExclude4()");
      initConfiguration("config_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager(em);
      Context.internalRequestScope().setApplicationEntityManager2(em2);

      TEntity te = createTEntity(12, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Igel");
      cte.setId(1);
      Mockito.when(em.find(TComplexEntity.class, 1l)).thenReturn(cte);

      TEntity te2 = createTEntity(5, "Hase");
      TComplexEntity cte2 = new TComplexEntity();
      cte2.setTen(te2);
      cte2.setCompValue(23);
      cte2.setOwner("Igel");
      cte2.setId(1);
      Mockito.when(em2.find(TComplexEntity.class, 1l)).thenReturn(cte2);

      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
   }

   @Test
   public void evaluateExclude5() throws Exception {
      log.info("start evaluateExclude5()");
      initConfiguration("config_condition_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager2(em);

      TEntity te = createTEntity(5, "Hase");
      TEntity te2 = createTEntity(6, "Hase6");
      TEntity te3 = createTEntity(7, "Hase7");
      TEntity te4 = createTEntity(8, "Hase8");

      TComplexEntity cte = new TComplexEntity();
      cte.setId(1);
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Igel");
      cte.addLazyList(te3);
      cte.addLazyList(te4);

      TComplexEntity cte2 = new TComplexEntity();
      cte2.setId(1);
      cte2.setTen(te);
      cte2.setCompValue(23);
      cte2.setOwner("Igel");
      cte2.addLazyList(te2);
      cte2.addLazyList(te3);
      cte2.addLazyList(te4);
      Mockito.when(em.find(TComplexEntity.class, 1l)).thenReturn(cte2);

      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
   }

   @Test
   public void evaluateExclude6() throws Exception {
      log.info("start evaluateExclude6()");
      initConfiguration("config_condition_stateChange1_invoker.xml");
      Context.internalRequestScope().setApplicationEntityManager2(em);

      TEntity te = createTEntity(5, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setId(2);
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Hase");

      TComplexEntity cte2 = new TComplexEntity();
      cte2.setId(2);
      cte2.setTen(te);
      cte2.setCompValue(23);
      cte2.setOwner("Igel");
      Mockito.when(em.find(TComplexEntity.class, 2l)).thenReturn(cte2);

      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

   @Test
   public void evaluateInclude1() throws Exception {
      log.info("start evaluateInclude1()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(em);

      TEntity te = createTEntity(12, "Hase");
      te.setId(2);

      TEntity te2 = createTEntity(5, "Hase");
      te2.setId(2);
      Mockito.when(em.find(TEntity.class, 2l)).thenReturn(te2);

      Resource res = new Resource(JpaResourceHandler.class, te);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());

      te.setCounter(5);
      te.setNameValue("Wein");
      res = new Resource(JpaResourceHandler.class, te);
      md = new EventMetadata(ControlEvent.UPDATE, res);
      list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateInclude2() throws Exception {
      log.info("start evaluateInclude2()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(em);

      TEntity te = createTEntity(12, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setId(3);
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Igel");

      TEntity te2 = createTEntity(5, "Hase");
      TComplexEntity cte2 = new TComplexEntity();
      cte2.setId(3);
      cte2.setTen(te2);
      cte2.setCompValue(23);
      cte2.setOwner("Igel");
      Mockito.when(em.find(TComplexEntity.class, 3l)).thenReturn(cte2);

      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
   }

   @Test
   public void evaluateInclude3() throws Exception {
      log.info("start evaluateInclude3()");
      initConfiguration("config_stateChange2_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(em);

      TEntity te = createTEntity(5, "Hase");
      TEntity te2 = createTEntity(6, "Hase6");
      TEntity te3 = createTEntity(7, "Hase7");
      TEntity te4 = createTEntity(8, "Hase8");

      TComplexEntity cte = new TComplexEntity();
      cte.setId(1);
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Igel");
      cte.addLazyList(te3);
      cte.addLazyList(te4);

      TComplexEntity cte2 = new TComplexEntity();
      cte2.setId(1);
      cte2.setTen(te);
      cte2.setCompValue(23);
      cte2.setOwner("Igel");
      cte2.addLazyList(te2);
      cte2.addLazyList(te3);
      cte2.addLazyList(te4);
      Mockito.when(em.find(TComplexEntity.class, 1l)).thenReturn(cte2);

      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
   }

   @Test
   public void evaluateInclude4() throws Exception {
      log.info("start evaluateInclude4()");
      initConfiguration("config_stateChange2_method.xml");
      Context.internalRequestScope().setApplicationEntityManager2(em);

      TEntity te = createTEntity(5, "Hase");
      TComplexEntity cte = new TComplexEntity();
      cte.setId(2);
      cte.setTen(te);
      cte.setCompValue(23);
      cte.setOwner("Hase");

      TComplexEntity cte2 = new TComplexEntity();
      cte2.setId(2);
      cte2.setTen(te);
      cte2.setCompValue(23);
      cte2.setOwner("Igel");
      Mockito.when(em.find(TComplexEntity.class, 2l)).thenReturn(cte2);

      Resource res = new Resource(JpaResourceHandler.class, cte);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateParentNoPersistentObject() throws Exception {
      log.info("start evaluateParentNoPersistentObject()");
      Context.internalRequestScope().setApplicationEntityManager2(em);
      Mockito.when(em.find(TEntity.class, 0l)).thenReturn(null);

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setStateChange(true, "TEntity");
      spB.add(sp);

      Setpoint sp2 = new Setpoint("head", sp);
      spB.add(sp2);

      Method m = TEntity.class.getMethod("getCounter");
      Resource res = new Resource(MethodResourceHandler.class, new TEntity(), m, null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATE, res);

      Control eval = new StateChangeControl();
      try {
         eval.evaluate(spB, md);
         Assert.fail();
      } catch (IllegalStateException e) {
         log.debug(e.getMessage(), e);
         Assert.assertTrue(e.getMessage().startsWith("failed to load object from database"));
      }
   }

}
