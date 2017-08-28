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
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class TenantControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(TenantControlTest.class);

   @Test
   public void evaluate() throws Exception {
      log.info("start evaluate()");
      initConfiguration("config_tenant_event_target.xml");

      List<Setpoint> spB = Configuration.instance().getSetpoints();
      log.debug("setpoints size: " + spB.size());

      EventMetadata md = new EventMetadata(ControlEvent.ALL, null);
      List<Setpoint> list = evaluate(md, spB, new TenantControl());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());

      Context.sessionScope().setTenant("not|present");
      list = evaluate(md, spB, new TenantControl());
      Assert.assertTrue(list.size() == 1);
      Assert.assertEquals("C", list.get(0).getId());

      Context.sessionScope().setTenant("ten1");
      list = evaluate(md, spB, new TenantControl());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());

      Context.sessionScope().setTenant("ten1|x");
      list = evaluate(md, spB, new TenantControl());
      Assert.assertEquals(5, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("B1", list.get(1).getId());
      Assert.assertEquals("B2", list.get(2).getId());
      Assert.assertEquals("B3", list.get(3).getId());
      Assert.assertEquals("C", list.get(4).getId());

      // for (Setpoint sB : list) {
      // Assert.assertTrue(sB.getId().startsWith("B"));
      // }

      Context.sessionScope().setTenant("ten1|y");
      list = evaluate(md, spB, new TenantControl());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());

      Context.sessionScope().setTenant("ten1|y|z");
      list = evaluate(md, spB, new TenantControl());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

   @Test
   public void evaluateExcludeTenant() throws Exception {
      log.info("start evaluateExcludeTenant()");
      initConfiguration("cibet-config-exclude.xml");
      Context.sessionScope().setTenant("Werner");

      Method m = String.class.getDeclaredMethod("getBytes");
      MethodResource res = new MethodResource("classname", m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(), new TenantControl());
      Assert.assertEquals(1, list.size());
   }

}
