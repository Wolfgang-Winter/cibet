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
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.http.HttpRequestResourceHandler;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

public class EventControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(EventControlTest.class);

   @Test
   public void evaluate() throws Exception {
      log.info("start evaluate()");

      Control eval = new EventControl();
      Resource res = new Resource(JpaResourceHandler.class, null);
      EventMetadata md = new EventMetadata(ControlEvent.DELETE, res);

      List<Setpoint> list = new ArrayList<Setpoint>();
      List<Setpoint> spB = Configuration.instance().getSetpoints();
      for (Setpoint sp : spB) {
         if (!eval.hasControlValue(sp.getControlValue("event"))) {
            continue;
         }
         boolean okay = eval.evaluate(sp.getControlValue("event"), md);
         if (okay) {
            list.add(sp);
         }
      }
      Assert.assertTrue(list.size() == 0);

      md = new EventMetadata(ControlEvent.INVOKE, null);
      list.clear();
      for (Setpoint sp : spB) {
         if (!eval.hasControlValue(sp.getControlValue("event"))) {
            continue;
         }
         boolean okay = eval.evaluate(sp.getControlValue("event"), md);
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
         if (!eval.hasControlValue(sp.getControlValue("event"))) {
            continue;
         }
         boolean okay = eval.evaluate(sp.getControlValue("event"), md);
         if (okay) {
            list.add(sp);
         }
      }
      Assert.assertTrue(list.size() == 1);
      for (Setpoint sB : list) {
         Assert.assertTrue(sB.getId().startsWith("A"));
      }
   }

   @Test(expected = IllegalArgumentException.class)
   public void evaluateNull() {
      log.info("start evaluateNull()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setEvent(ControlEvent.FIRST_RELEASE.name());
      spB.add(sp);

      Control eval = new EventControl();
      eval.evaluate(spB, (EventMetadata) null);
   }

   @Test
   public void evaluateWithParent() {
      log.info("start evaluateWithParent()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setEvent(ControlEvent.FIRST_RELEASE.name());
      spB.add(sp);

      Setpoint sp2 = new Setpoint("head", sp);
      spB.add(sp2);

      Resource res = new Resource(HttpRequestResourceHandler.class, "targ", "POST", null, null);
      EventMetadata md = new EventMetadata(ControlEvent.FIRST_RELEASE, res);

      Control eval = new EventControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spB) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         spi.getEffectiveControlValues(controlValues);
         boolean okay = eval.evaluate(controlValues.get("event"), md);
         if (okay) {
            list.add(spi);
         }
      }
      Assert.assertEquals(2, list.size());
   }

}
