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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.TrueCustomControl;
import com.logitags.cibet.bindings.ControlDefBinding;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;

public class CustomControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(CustomControlTest.class);

   @Test
   public void start() throws Exception {
      log.info("start start()");
      initConfiguration("config_customControl.xml");

      TrueCustomControl ct = (TrueCustomControl) Configuration.instance().getControl("TRUE");
      Assert.assertNotNull(ct);
      Assert.assertEquals("45", ((TrueCustomControl) ct).getGaga());

      Setpoint s1 = Configuration.instance().getSetpoint("K2");
      List<String> l = (List<String>) s1.getControlValues().get("TRUE");
      Assert.assertEquals(1, l.size());
      Assert.assertEquals("", l.get(0));
      BooleanAttributedControlValue l2 = (BooleanAttributedControlValue) s1.getControlValues().get("invoker");
      Assert.assertEquals(false, l2.isBooleanValue());
      Assert.assertEquals("", l2.getValues().get(0));

      Setpoint s2 = Configuration.instance().getSetpoint("K3");
      l = (List<String>) s2.getControlValues().get("TRUE");
      Assert.assertEquals(1, l.size());
      Assert.assertEquals("", l.get(0));
      l2 = (BooleanAttributedControlValue) s2.getControlValues().get("invoker");
      Assert.assertEquals(false, l2.isBooleanValue());
      Assert.assertEquals("", l2.getValues().get(0));
   }

   @Test
   public void registerControlWrongName() throws Exception {
      ControlDefBinding cdef = new ControlDefBinding();
      cdef.setClazz(TrueCustomControl.class.getName());
      cdef.setName("hehe");

      Method m = Configuration.class.getDeclaredMethod("registerControlDefFromBinding", ControlDefBinding.class);
      m.setAccessible(true);
      try {
         m.invoke(Configuration.instance(), cdef);
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertTrue(e.getCause().getMessage().startsWith("name attribute '"));
      }
   }

   @Test
   public void registerControlClassNotFound() throws Exception {
      ControlDefBinding cdef = new ControlDefBinding();
      cdef.setClazz("classNotFound");
      cdef.setName("hehe");

      Method m = Configuration.class.getDeclaredMethod("registerControlDefFromBinding", ControlDefBinding.class);
      m.setAccessible(true);
      try {
         m.invoke(Configuration.instance(), cdef);
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertEquals("java.lang.ClassNotFoundException: classNotFound", e.getCause().getMessage());
      }
   }

   @Test
   public void unregisterControl() throws Exception {
      Configuration.instance().unregisterControl("TRUE");

      ControlDefBinding cdef = new ControlDefBinding();
      cdef.setClazz(TrueCustomControl.class.getName());
      cdef.setName("TRUE");

      Configuration cman = Configuration.instance();
      Method m = Configuration.class.getDeclaredMethod("registerControlDefFromBinding", ControlDefBinding.class);
      m.setAccessible(true);
      m.invoke(cman, cdef);

      Control l = cman.getControl("TRUE");
      Assert.assertNotNull(l);

      cman.unregisterControl("TRUE");
      l = cman.getControl("TRUE");
      Assert.assertNull(l);
   }

}
