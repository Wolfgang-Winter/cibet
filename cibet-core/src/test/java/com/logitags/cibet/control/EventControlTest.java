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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.jpa.JpaResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class EventControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(EventControlTest.class);

   @Test
   public void evaluate() throws Exception {
      log.info("start evaluate()");

      Control eval = new EventControl();
      JpaResource res = new JpaResource((Object) null);
      EventMetadata md = new EventMetadata(ControlEvent.DELETE, res);

      List<Setpoint> list = new ArrayList<Setpoint>();
      List<Setpoint> spB = Configuration.instance().getSetpoints();
      for (Setpoint sp : spB) {
         if (sp.getControls().get("event") == null) {
            continue;
         }
         boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
         if (okay) {
            list.add(sp);
            log.debug("add setpoint " + sp.getId());
         }
      }
      Assert.assertEquals(0, list.size());

      md = new EventMetadata(ControlEvent.INVOKE, null);
      list.clear();
      for (Setpoint sp : spB) {
         if (sp.getControls().get("event") == null) {
            continue;
         }
         boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
         if (okay) {
            list.add(sp);
         }
      }
      Assert.assertTrue(list.size() == 3);
      Assert.assertEquals("C", list.get(0).getId());
      Assert.assertEquals("D", list.get(1).getId());
      Assert.assertEquals("ConditionControlTest-3", list.get(2).getId());

      md = new EventMetadata(ControlEvent.UPDATE, null);
      list.clear();
      for (Setpoint sp : spB) {
         if (sp.getControls().get("event") == null) {
            continue;
         }
         boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
         if (okay) {
            list.add(sp);
         }
      }
      Assert.assertTrue(list.size() == 1);
      for (Setpoint sB : list) {
         Assert.assertTrue(sB.getId().startsWith("A"));
      }

      md = new EventMetadata(ControlEvent.UPDATEQUERY, null);
      list.clear();
      for (Setpoint sp : spB) {
         if (sp.getControls().get("event") == null) {
            continue;
         }
         boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
         if (okay) {
            list.add(sp);
         }
      }
      Assert.assertEquals(1, list.size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void evaluateNull() {
      log.info("start evaluateNull()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.addEventIncludes(ControlEvent.FIRST_RELEASE);
      spB.add(sp);

      Control eval = new EventControl();
      eval.evaluate(null, (EventMetadata) null);
   }

   @Test
   public void evaluateWithParent() {
      log.info("start evaluateWithParent()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.addEventIncludes(ControlEvent.FIRST_RELEASE);
      spB.add(sp);

      Setpoint sp2 = new Setpoint("head", sp);
      spB.add(sp2);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.FIRST_RELEASE, res);

      Control eval = new EventControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spB) {
         Set<ConcreteControl> sc = spi.getEffectiveControls();
         for (ConcreteControl cc : sc) {
            if ("event".equals(cc.getControl().getName())) {
               boolean okay = eval.evaluate(cc.getIncludes(), md);
               if (okay) {
                  list.add(spi);
               }
               break;
            }
         }
      }
      Assert.assertEquals(2, list.size());
   }

   @Test
   public void evaluateImplicitNok() {
      log.info("start evaluateImplicitNok()");

      Setpoint sp = new Setpoint("conditionParams");
      sp.addEventIncludes(ControlEvent.FIRST_RELEASE_INSERT);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATEQUERY, res);

      Control eval = new EventControl();
      boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
      Assert.assertFalse(okay);
   }

   @Test
   public void evaluateImplicitOk() {
      log.info("start evaluateImplicitOk()");

      Setpoint sp = new Setpoint("conditionParams");
      sp.addEventIncludes(ControlEvent.UPDATEQUERY);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATEQUERY, res);

      Control eval = new EventControl();
      boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
      Assert.assertTrue(okay);
   }

   @Test
   public void evaluateImplicitOk2() {
      log.info("start evaluateImplicitOk2()");

      Setpoint sp = new Setpoint("conditionParams");
      sp.addEventIncludes(ControlEvent.PERSIST);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.UPDATEQUERY, res);

      Control eval = new EventControl();
      boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
      Assert.assertTrue(okay);
   }

   @Test
   public void evaluateHierarchy() {
      log.info("start evaluateHierarchy()");

      Setpoint sp = new Setpoint("conditionParams");
      sp.addEventIncludes(ControlEvent.FIRST_RELEASE);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.FIRST_RELEASE_INSERT, res);

      Control eval = new EventControl();
      boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
      Assert.assertTrue(okay);
   }

   @Test
   public void evaluateHierarchyAll() {
      log.info("start evaluateHierarchyAll()");

      Setpoint sp = new Setpoint("conditionParams");
      sp.addEventIncludes(ControlEvent.ALL);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.FIRST_RELEASE_INSERT, res);

      Control eval = new EventControl();
      boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
      Assert.assertTrue(okay);
   }

   @Test
   public void evaluateHierarchyNot() {
      log.info("start evaluateHierarchyNot()");

      Setpoint sp = new Setpoint("conditionParams");
      sp.addEventIncludes(ControlEvent.RELEASE_INVOKE);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.FIRST_RELEASE, res);

      Control eval = new EventControl();
      boolean okay = eval.evaluate(sp.getControls().get("event").getIncludes(), md);
      Assert.assertFalse(okay);
   }

   @Test
   public void evaluateEventEx1() throws Exception {
      log.info("start evaluateEventEx1()");
      initConfiguration("cibet-config-exclude.xml");

      TEntity ent = createTEntity(7, "Stingel");

      MethodResource res = new MethodResource(ent, null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INSERT, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(), new EventControl());
      Assert.assertEquals(2, list.size());
      initConfiguration("cibet-config.xml");
   }

   @Test
   public void evaluateEventEx2() throws Exception {
      log.info("start evaluateEventEx2()");
      initConfiguration("cibet-config-exclude.xml");

      TEntity ent = createTEntity(7, "Stingel");

      MethodResource res = new MethodResource(ent, null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.UPDATE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(), new EventControl());
      Assert.assertEquals(1, list.size());
      initConfiguration("cibet-config.xml");
   }

}
