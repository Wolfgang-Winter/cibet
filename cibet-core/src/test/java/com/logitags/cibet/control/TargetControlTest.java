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
import com.cibethelper.base.Sub4EyesController;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.ejb.EjbResource;
import com.logitags.cibet.sensor.jpa.JpaResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class TargetControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(TargetControlTest.class);

   @Test
   public void evaluate() throws Exception {
      log.info("start evaluate()");
      String result = initConfiguration("config_tenant_event_target.xml");

      List<Setpoint> spB = Configuration.instance().getSetpoints();

      EjbResource res = new EjbResource("StringClass", (Method) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.ALL, res);
      List<Setpoint> list = evaluate(md, spB, new TargetControl());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("B1", list.get(0).getId());
      Assert.assertEquals("D", list.get(1).getId());

      res = new EjbResource("String", (Method) null, null);
      md = new EventMetadata(ControlEvent.ALL, res);
      list = evaluate(md, spB, new TargetControl());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("B1", list.get(0).getId());
      Assert.assertEquals("D", list.get(1).getId());

      JpaResource res2 = new JpaResource(new TEntity());
      md = new EventMetadata(ControlEvent.ALL, res2);
      list = evaluate(md, spB, new TargetControl());
      Assert.assertEquals(5, list.size());
      Assert.assertEquals("B1", list.get(0).getId());
      Assert.assertEquals("B2", list.get(1).getId());
      Assert.assertEquals("B3", list.get(2).getId());
      Assert.assertEquals("C", list.get(3).getId());
      Assert.assertEquals("D", list.get(4).getId());

      res = new EjbResource(new Sub4EyesController(), (Method) null, null);
      md = new EventMetadata(ControlEvent.ALL, res);
      list = evaluate(md, spB, new TargetControl());
      Assert.assertEquals(4, list.size());
      Assert.assertEquals("B1", list.get(0).getId());
      Assert.assertEquals("B2", list.get(1).getId());
      Assert.assertEquals("B3", list.get(2).getId());
      Assert.assertEquals("D", list.get(3).getId());
   }

   @Test
   public void evaluateExcludeTarget() throws Exception {
      log.info("start evaluateExcludeTarget()");
      initConfiguration("cibet-config-exclude.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      MethodResource res = new MethodResource("classname", m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(), new TargetControl());
      Assert.assertEquals(1, list.size());
   }
}
