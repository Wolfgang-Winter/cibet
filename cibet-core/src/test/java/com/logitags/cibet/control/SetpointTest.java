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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.apache.log4j.Logger;
import org.junit.Test;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.bindings.ControlsBinding;
import com.logitags.cibet.bindings.SetpointBinding;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;

import junit.framework.Assert;

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
      Setpoint sp2 = new Setpoint("2", null);
      Assert.assertEquals(null, sp2.getExtendsId());
      Assert.assertEquals(null, sp2.getExtends());
      Assert.assertEquals("2", sp2.getId());
   }

   @Test(expected = IllegalArgumentException.class)
   public void addEventNull() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setEvent((String) null);
   }

   @Test
   public void addEvent() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setEvent(ControlEvent.DELETE.name());
      Assert.assertEquals(1, ((List<String>) sp2.getControlValue(EventControl.NAME)).size());
      sp2.setEvent(ControlEvent.DELETE.name());
      Assert.assertEquals(1, ((List<String>) sp2.getControlValue(EventControl.NAME)).size());
      sp2.setEvent(ControlEvent.INSERT.name());
      Assert.assertEquals(1, ((List<String>) sp2.getControlValue(EventControl.NAME)).size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void addInvokerNull() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setInvoker((String) null);
   }

   @Test
   public void addInvoker() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setInvoker(true, "Hansi");
      BooleanAttributedControlValue v = (BooleanAttributedControlValue) sp2.getControlValue("invoker");
      Assert.assertEquals(true, v.isBooleanValue());
      Assert.assertEquals(1, v.getValues().size());
      sp2.setInvoker("Hansi");
      Assert.assertEquals(1, v.getValues().size());
      sp2.setInvoker("Fred;Klaus,");
      v = (BooleanAttributedControlValue) sp2.getControlValue("invoker");
      Assert.assertEquals(2, v.getValues().size());
      Assert.assertEquals("Fred", v.getValues().get(0));
      sp2.setInvoker("Lobo", "Willi");
      v = (BooleanAttributedControlValue) sp2.getControlValue("invoker");
      Assert.assertEquals(2, v.getValues().size());
      sp2.setInvoker("");
      v = (BooleanAttributedControlValue) sp2.getControlValue("invoker");
      Assert.assertEquals(1, v.getValues().size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void addStateChangeNull() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setStateChange((String) null);
   }

   @Test
   public void addStateChange() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setStateChange(true, "Hansi");
      BooleanAttributedControlValue v = (BooleanAttributedControlValue) sp2.getControlValue("stateChange");

      Assert.assertEquals(true, v.isBooleanValue());
      Assert.assertEquals(1, v.getValues().size());

      sp2.setStateChange("Hansi");
      v = (BooleanAttributedControlValue) sp2.getControlValue("stateChange");
      Assert.assertEquals(1, v.getValues().size());

      sp2.setStateChange("Fred;Klaus,");
      v = (BooleanAttributedControlValue) sp2.getControlValue("stateChange");
      Assert.assertEquals(2, v.getValues().size());
      Assert.assertEquals("Klaus", v.getValues().get(1));

      sp2.setStateChange("Lobo", "Willi");
      v = (BooleanAttributedControlValue) sp2.getControlValue("stateChange");
      Assert.assertEquals(2, v.getValues().size());

      sp2.setStateChange("");
      v = (BooleanAttributedControlValue) sp2.getControlValue("stateChange");
      Assert.assertEquals(1, v.getValues().size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void addActuatorNull() {
      Setpoint sp2 = new Setpoint("2");
      sp2.addActuator((Actuator) null);
   }

   @Test(expected = IllegalArgumentException.class)
   public void addTargetNull() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setTarget((String) null);
   }

   @Test
   public void addTarget() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setTarget("Hansi");
      BooleanAttributedControlValue v = (BooleanAttributedControlValue) sp2.getControlValue("stateChange");
      Assert.assertNull(v);

      List<String> l = (List<String>) sp2.getControlValue("target");

      Assert.assertEquals(1, l.size());
      sp2.setTarget("Hansi");
      l = (List<String>) sp2.getControlValue("target");
      Assert.assertEquals(1, l.size());
      sp2.setTarget("Fred;Klaus,");
      l = (List<String>) sp2.getControlValue("target");
      Assert.assertEquals(2, l.size());
      sp2.setTarget("Lobo", "Willi");
      l = (List<String>) sp2.getControlValue("target");
      Assert.assertEquals(2, l.size());
      sp2.setTarget("");
      l = (List<String>) sp2.getControlValue("target");
      Assert.assertEquals(1, l.size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void addMethodNull() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setMethod((String) null);
   }

   @Test
   public void addMethod() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setMethod("make");
      List<String> l = (List<String>) sp2.getControlValue("method");

      Assert.assertEquals(1, l.size());
      sp2.setMethod("make");
      l = (List<String>) sp2.getControlValue("method");
      Assert.assertEquals(1, l.size());
      sp2.setMethod("make2(String, int, long);make4(long,int),");
      l = (List<String>) sp2.getControlValue("method");
      Assert.assertEquals(2, l.size());
      sp2.setMethod("doit()", "doit2(String,String,TEntity)");
      l = (List<String>) sp2.getControlValue("method");
      Assert.assertEquals(2, l.size());
      sp2.setMethod("");
      l = (List<String>) sp2.getControlValue("method");
      Assert.assertEquals(1, l.size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void addTenantNull() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setTenant((String) null);
   }

   @Test
   public void addTenant() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setTenant("Hansi");
      List<String> l = (List<String>) sp2.getControlValue("tenant");

      Assert.assertEquals(1, l.size());
      sp2.setTenant("Hansi");
      l = (List<String>) sp2.getControlValue("tenant");
      Assert.assertEquals(1, l.size());
      sp2.setTenant("Fred;Klaus,");
      l = (List<String>) sp2.getControlValue("tenant");
      Assert.assertEquals(2, l.size());
      sp2.setTenant("Lobo", "Willi");
      l = (List<String>) sp2.getControlValue("tenant");
      Assert.assertEquals(2, l.size());
      sp2.setTenant("");
      l = (List<String>) sp2.getControlValue("tenant");
      Assert.assertEquals(1, l.size());
   }

   @Test
   public void resolveEvent() throws Exception {
      Setpoint sp2 = new Setpoint("2");
      sp2.setEvent("DELETE");
      Assert.assertEquals(1, ((List<String>) sp2.getControlValue(EventControl.NAME)).size());
      sp2.setEvent("DELETE");
      Assert.assertEquals(1, ((List<String>) sp2.getControlValue(EventControl.NAME)).size());
      sp2.setEvent("UPDATE, RELEASE_UPDATE");
      Assert.assertEquals(2, ((List<String>) sp2.getControlValue(EventControl.NAME)).size());
      sp2.setEvent("");
      Assert.assertEquals(1, ((List<String>) sp2.getControlValue(EventControl.NAME)).size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void resolveEventError() throws Exception {
      Setpoint sp2 = new Setpoint("22");
      sp2.setEvent("sdsdsd");
   }

   @Test
   public void resolveConditionNull() throws Exception {
      ConditionControl control = new ConditionControl();
      Method m = ConditionControl.class.getDeclaredMethod("resolve", String.class);
      m.setAccessible(true);
      Object res = m.invoke(control, (String) null);
      Assert.assertNull(res);
   }

   @Test
   public void resolveCondition() throws Exception {
      ConditionControl control = new ConditionControl();
      Method m = ConditionControl.class.getDeclaredMethod("resolve", String.class);
      m.setAccessible(true);
      Object res = m.invoke(control, "delete");
      Assert.assertEquals("delete", res);

      try {
         m.invoke(control, "file:not/existing\\file");
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertTrue(e.getCause() instanceof IllegalArgumentException);
      }
   }

   @Test
   public void setCondition() {
      Setpoint sp = new Setpoint("Test-L");

      sp.setCondition(null);
      Assert.assertNull(sp.getControlValue("condition"));
      sp.setCondition("com.logitags.cibet.TEntity");
      Assert.assertNotNull(sp.getControlValue("condition"));
      Assert.assertEquals("com.logitags.cibet.TEntity", sp.getControlValue("condition"));
   }

   @Test
   public void resolveStringNull() throws Exception {
      log.debug("start resolveStringNull()");
      Setpoint sp = new Setpoint("sp1");
      SetpointBinding spb = new SetpointBinding();
      spb.setId("id1");
      ControlsBinding cb = new ControlsBinding();
      spb.setControls(cb);

      QName qname = new QName("event");
      JAXBElement<String> elem = new JAXBElement<String>(qname, String.class, null);
      spb.getControls().getTenantOrEventOrTarget().add(elem);

      Method m = Configuration.class.getDeclaredMethod("resolveSetpoint", Setpoint.class, SetpointBinding.class);
      m.setAccessible(true);
      m.invoke(Configuration.instance(), sp, spb);

   }

   @Test
   public void setCustomControl() {
      Setpoint sp2 = new Setpoint("2");
      sp2.setCustomControl(TenantControl.NAME, "Hansi");
      List<String> l = (List<String>) sp2.getControlValue("tenant");

      Assert.assertEquals(1, l.size());
      sp2.setCustomControl(TenantControl.NAME, "Hansi");
      l = (List<String>) sp2.getControlValue("tenant");
      Assert.assertEquals(1, l.size());
      sp2.setCustomControl(TenantControl.NAME, "Fred;Klaus,");
      l = (List<String>) sp2.getControlValue("tenant");
      Assert.assertEquals(2, l.size());
      sp2.setCustomControl(TenantControl.NAME, "");
      l = (List<String>) sp2.getControlValue("tenant");
      Assert.assertEquals(1, l.size());
   }

   @Test(expected = IllegalArgumentException.class)
   public void setStateChangeNull() throws Exception {
      log.debug("start setStateChangeNull()");
      Setpoint sp2 = new Setpoint("2");
      sp2.setStateChange(true, (String[]) null);
   }

}
