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

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class InvokerControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(InvokerControlTest.class);

   private List<Setpoint> evaluate(EventMetadata md, List<Setpoint> spoints) {
      Control eval = new InvokerControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spoints) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         spi.getEffectiveControlValues(controlValues);
         Object value = controlValues.get("invoker");
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
   public void evaluateExclude1() throws Exception {
      log.info("start evaluateExclude1()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");

      Method m = null;
      MethodResource res = new MethodResource("ClassName", m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
   }

   @Test
   public void evaluateExclude2() throws Exception {
      log.info("start evaluateExclude2()");
      initConfiguration("config_stateChange1_invoker.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      MethodResource res = new MethodResource("ClassName", m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
   }

   @Test
   public void evaluateInclude1() throws Exception {
      log.info("start evaluateInclude1()");
      initConfiguration("config_condition_stateChange1_invoker.xml");

      Method m = null;
      MethodResource res = new MethodResource("ClassName", m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());

      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

   @Test
   public void evaluateHttpMetadata() {
      log.info("start evaluateHttpMetadata()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setInvoker("168.52.3.4");
      spB.add(sp);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      res.setInvoker("168.52.3.4");

      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(1, list.size());
   }

   @Test
   public void evaluateHttpMetadataWildcard() {
      log.info("start evaluateHttpMetadataWildcard()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setInvoker("168.52*");
      spB.add(sp);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      res.setInvoker("168.52.3.4");

      List<Setpoint> list = evaluate(md, spB);
      Assert.assertEquals(1, list.size());
   }

   @Test
   public void evaluateHttpMetadataParent() {
      log.info("start evaluateHttpMetadataParent()");

      List<Setpoint> spB = new ArrayList<Setpoint>();
      Setpoint sp = new Setpoint("conditionParams");
      sp.setInvoker(true, "168.52*");
      spB.add(sp);

      Setpoint sp2 = new Setpoint("head", sp);
      spB.add(sp2);

      HttpRequestResource res = new HttpRequestResource("targ", "POST", (HttpServletRequest) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      res.setInvoker("167.52.3.4");
      List<Setpoint> list = evaluate(md, spB);

      Assert.assertEquals(2, list.size());
   }

}
