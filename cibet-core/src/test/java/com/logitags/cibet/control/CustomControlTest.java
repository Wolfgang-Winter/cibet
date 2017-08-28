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
import java.util.Set;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.base.TrueCustomControl;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.bindings.ControlDefBinding;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class CustomControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(CustomControlTest.class);

   @Test
   public void start() throws Exception {
      log.info("start start()");
      initConfiguration("config_customControl.xml");

      TrueCustomControl ct = (TrueCustomControl) Configuration.instance().getControl("TRUE");
      Assert.assertNotNull(ct);
      Assert.assertEquals("45", ((TrueCustomControl) ct).getGaga());

      Setpoint s1 = Configuration.instance().getSetpoint("custControl/K2");
      Set<String> l = s1.getControls().get("TRUE").getIncludes();
      Assert.assertEquals(0, l.size());
      Set<String> l2 = s1.getControls().get("invoker").getIncludes();
      Assert.assertEquals(0, l2.size());

      Setpoint s2 = Configuration.instance().getSetpoint("custControl/K1");
      l = s2.getControls().get("TRUE").getIncludes();
      Assert.assertEquals(1, l.size());
      Assert.assertEquals("dsfg", l.iterator().next());
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

   @Test
   public void testCC1() throws Exception {
      log.info("start testCC1()");
      initConfiguration("cibet-config-exclude.xml");

      TEntity ent = createTEntity(7, "Stingel");
      MethodResource res = new MethodResource(ent, null, null);
      EventMetadata md = new EventMetadata(null, ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints(),
            Configuration.instance().getControl("CC1"));

      Assert.assertEquals(1, list.size());
      Assert.assertEquals("AA2", list.get(0).getId());
   }

}
