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

import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;

public class SetpointTest {

   private static Logger log = Logger.getLogger(SetpointTest.class);

   @Test
   public void constructorOk() {
      Setpoint sp1 = new Setpoint("1");
      Setpoint sp2 = new Setpoint("2", sp1);
      Assert.assertEquals("1", sp2.getExtendsId());
      Assert.assertEquals(sp1, sp2.getExtends());
      Assert.assertEquals("2", sp2.getId());
   }

   @Test(expected = IllegalArgumentException.class)
   public void constructorNullId() {
      Setpoint sp1 = new Setpoint("1");
      new Setpoint(null, sp1);
   }

   @Test
   public void constructorNullParent() {
      Setpoint sp2 = new Setpoint("2");
      Assert.assertEquals(null, sp2.getExtendsId());
      Assert.assertEquals(null, sp2.getExtends());
      Assert.assertEquals("2", sp2.getId());
   }

   @Test
   public void addEvent() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addEventIncludes(ControlEvent.DELETE);
      Assert.assertEquals(1, sp2.getControls().get(EventControl.NAME).getIncludes().size());
      sp2.addEventIncludes(ControlEvent.DELETE);
      Assert.assertEquals(1, sp2.getControls().get(EventControl.NAME).getIncludes().size());
      sp2.addEventIncludes(ControlEvent.INSERT);
      Assert.assertEquals(2, sp2.getControls().get(EventControl.NAME).getIncludes().size());
   }

   @Test
   public void addInvoker() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addInvokerExcludes("Hansi");
      Assert.assertEquals(1, sp2.getControls().get(InvokerControl.NAME).getExcludes().size());
      sp2.addInvokerIncludes("Hansi");
      Assert.assertEquals(1, sp2.getControls().get(InvokerControl.NAME).getIncludes().size());
   }

   @Test
   public void addStateChange() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addStateChangeExcludes("Hansi");
      Assert.assertEquals(1, sp2.getControls().get(StateChangeControl.NAME).getExcludes().size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void addActuatorNull() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addActuator((Actuator) null);
   }

   @Test
   public void addTarget() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addTargetIncludes("Hansi");

      Set<String> l = sp2.getControls().get(TargetControl.NAME).getIncludes();

      Assert.assertEquals(1, l.size());
      sp2.addTargetIncludes("Hansi");
      l = sp2.getControls().get(TargetControl.NAME).getIncludes();
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void addMethod() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addMethodExcludes("make");
      Set<String> l = sp2.getControls().get(MethodControl.NAME).getExcludes();

      Assert.assertEquals(1, l.size());
      sp2.addMethodExcludes("make");
      l = sp2.getControls().get(MethodControl.NAME).getExcludes();
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void addTenant() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addTenantIncludes("Hansi");
      Set<String> l = sp2.getControls().get(TenantControl.NAME).getIncludes();

      Assert.assertEquals(1, l.size());
      sp2.addTenantIncludes("Hansi");
      l = sp2.getControls().get(TenantControl.NAME).getIncludes();
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void resolveEvent() throws Exception {
      Setpoint sp2 = new Setpoint("2");
      sp2.addEventIncludes(ControlEvent.DELETE);
      Assert.assertEquals(1, sp2.getControls().get(EventControl.NAME).getIncludes().size());
      sp2.addEventIncludes(ControlEvent.DELETE);
      Assert.assertEquals(1, sp2.getControls().get(EventControl.NAME).getIncludes().size());
   }

   @Test
   public void resolveConditionNull() throws Exception {
      Assert.assertNull(Controller.resolve(null));
   }

   @Test
   public void resolveCondition() throws Exception {
      try {
         Controller.resolve("file:not/existing\\file");
         Assert.fail();
      } catch (IllegalArgumentException e) {
      }
   }

   @Test
   public void setCondition2() {
      Setpoint sp = new Setpoint("Test-L");

      sp.addConditionIncludes("com.logitags.cibet.TEntity");
      Assert.assertEquals(1, sp.getControls().get(ConditionControl.NAME).getIncludes().size());
      Assert.assertEquals("com.logitags.cibet.TEntity",
            sp.getControls().get(ConditionControl.NAME).getIncludes().iterator().next());
   }

   @Test
   public void setCustomControl() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addCustomControlIncludes(TenantControl.NAME, "Hansi");
      Set<String> l = sp2.getControls().get(TenantControl.NAME).getIncludes();

      Assert.assertEquals(1, l.size());
      sp2.addCustomControlIncludes(TenantControl.NAME, "Hansi");
      l = sp2.getControls().get(TenantControl.NAME).getIncludes();
      Assert.assertEquals(1, l.size());
   }

}
