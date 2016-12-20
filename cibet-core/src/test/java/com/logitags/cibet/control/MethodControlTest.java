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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CoreTestBase;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.pojo.MethodResourceHandler;

public class MethodControlTest extends CoreTestBase {

   private static Logger log = Logger.getLogger(MethodControlTest.class);

   private List<Setpoint> evaluate(EventMetadata md, List<Setpoint> spoints) {
      Control eval = new MethodControl();
      List<Setpoint> list = new ArrayList<Setpoint>();
      for (Setpoint spi : spoints) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         spi.getEffectiveControlValues(controlValues);
         Object value = controlValues.get("method");
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
   public void evaluateNoMethod() throws Exception {
      log.info("start evaluateNoMethod()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");

      Resource res = new Resource(MethodResourceHandler.class, "classname", (Method) null, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(4, list.size());
   }

   @Test
   public void evaluate() throws Exception {
      log.info("start evaluate()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      Resource res = new Resource(MethodResourceHandler.class, "classname", m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
   }

   @Test
   public void evaluateWithParam() throws Exception {
      log.info("start evaluateWithParam()");
      initConfiguration("config_condition_stateChange2_invoker_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      List<ResourceParameter> paramList = new LinkedList<ResourceParameter>();
      paramList.add(new ResourceParameter("PARAM0", int.class.getName(), 4, ParameterType.METHOD_PARAMETER, 1));
      Resource res = new Resource(MethodResourceHandler.class, "classname", m, paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(2, list.size());
      Assert.assertEquals("A", list.get(0).getId());
      Assert.assertEquals("C", list.get(1).getId());
   }

   @Test
   public void evaluateSimpleMethod() throws Exception {
      log.info("start evaluateSimpleMethod()");
      initConfiguration("config_stateChange2_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      Resource res = new Resource(MethodResourceHandler.class, "Classname", m, null);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("A", list.get(0).getId());
   }

   @Test
   public void evaluateWithParam2() throws Exception {
      log.info("start evaluateWithParam2()");
      initConfiguration("config_stateChange2_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      List<ResourceParameter> paramList = new LinkedList<ResourceParameter>();
      paramList.add(new ResourceParameter("PARAM0", int.class.getName(), 4, ParameterType.METHOD_PARAMETER, 1));
      paramList.add(new ResourceParameter("PARAM1", String.class.getName(), "Hase", ParameterType.METHOD_PARAMETER, 2));

      Resource res = new Resource(MethodResourceHandler.class, "Classname", m, paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateWithParam3() throws Exception {
      log.info("start evaluateWithParam3()");
      initConfiguration("config_stateChange2_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      List<ResourceParameter> paramList = new LinkedList<ResourceParameter>();
      paramList.add(new ResourceParameter("PARAM0", int.class.getName(), 4, ParameterType.METHOD_PARAMETER, 1));
      paramList.add(new ResourceParameter("PARAM1", byte[].class.getName(), new byte[] { 'x' },
            ParameterType.METHOD_PARAMETER, 2));
      Resource res = new Resource(MethodResourceHandler.class, "Classname", m, paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void evaluateWithParam4() throws Exception {
      log.info("start evaluateWithParam4()");
      initConfiguration("config_stateChange2_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      List<ResourceParameter> paramList = new LinkedList<ResourceParameter>();
      paramList.add(new ResourceParameter("PARAM0", int.class.getName(), 4, ParameterType.METHOD_PARAMETER, 1));
      paramList.add(new ResourceParameter("PARAM1", String.class.getName(), "Hase", ParameterType.METHOD_PARAMETER, 2));
      paramList.add(
            new ResourceParameter("PARAM2", TEntity.class.getName(), new TEntity(), ParameterType.METHOD_PARAMETER, 3));
      Resource res = new Resource(MethodResourceHandler.class, "Classname", m, paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
   }

   @Test
   public void evaluateWithParamInteger() throws Exception {
      log.info("start evaluateWithParamInteger()");
      initConfiguration("config_stateChange2_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      List<ResourceParameter> paramList = new LinkedList<ResourceParameter>();
      paramList.add(new ResourceParameter("PARAM0", Integer.class.getName(), new Integer(5),
            ParameterType.METHOD_PARAMETER, 1));
      paramList.add(new ResourceParameter("PARAM1", String.class.getName(), "Hase", ParameterType.METHOD_PARAMETER, 2));
      paramList.add(
            new ResourceParameter("PARAM2", TEntity.class.getName(), new TEntity(), ParameterType.METHOD_PARAMETER, 3));
      Resource res = new Resource(MethodResourceHandler.class, "Classname", m, paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
   }

   public void evaluateWithParamArray() throws Exception {
      log.info("start evaluateWithParamArray()");
      initConfiguration("config_stateChange2_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      List<ResourceParameter> paramList = new LinkedList<ResourceParameter>();
      paramList.add(
            new ResourceParameter("PARAM0", int[].class.getName(), new int[] { 5 }, ParameterType.METHOD_PARAMETER, 1));
      paramList.add(new ResourceParameter("PARAM1", String[][].class.getName(), new String[][] {},
            ParameterType.METHOD_PARAMETER, 2));
      Resource res = new Resource(MethodResourceHandler.class, "Classname", m, paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(1, list.size());
      Assert.assertEquals("C", list.get(0).getId());
   }

   public void evaluateWithParamIntegerArray() throws Exception {
      log.info("start evaluateWithParamIntegerArray()");
      initConfiguration("config_stateChange2_method.xml");

      Method m = String.class.getDeclaredMethod("getBytes");
      List<ResourceParameter> paramList = new LinkedList<ResourceParameter>();
      paramList.add(new ResourceParameter("PARAM0", Integer[].class.getName(), new Integer[] {},
            ParameterType.METHOD_PARAMETER, 1));
      paramList.add(new ResourceParameter("PARAM1", String[][].class.getName(), new String[][] {},
            ParameterType.METHOD_PARAMETER, 2));
      Resource res = new Resource(MethodResourceHandler.class, "Classname", m, paramList);
      EventMetadata md = new EventMetadata(ControlEvent.INVOKE, res);
      List<Setpoint> list = evaluate(md, Configuration.instance().getSetpoints());
      Assert.assertEquals(0, list.size());
   }

   @Test
   public void internalClassNameForName() throws Exception {
      Method m = MethodControl.class.getDeclaredMethod("internalClassNameForName", String.class);
      m.setAccessible(true);
      MethodControl ctrl = new MethodControl();

      String clazz = (String) m.invoke(ctrl, "[B");
      Assert.assertTrue("java.lang.Byte[]".equals(clazz));

      clazz = (String) m.invoke(ctrl, (String) null);
      Assert.assertNull(clazz);

      clazz = (String) m.invoke(ctrl, "[[B");
      Assert.assertTrue("java.lang.Byte[][]".equals(clazz));

      clazz = (String) m.invoke(ctrl, "[L" + TEntity.class.getName() + ";");
      log.debug("name: '" + clazz + "'");
      Assert.assertTrue((TEntity.class.getName() + "[]").equals(clazz));

      clazz = (String) m.invoke(ctrl, "short");
      Assert.assertTrue("java.lang.Short".equals(clazz));

      clazz = (String) m.invoke(ctrl, "byte");
      Assert.assertTrue("java.lang.Byte".equals(clazz));

      clazz = (String) m.invoke(ctrl, "boolean");
      Assert.assertTrue("java.lang.Boolean".equals(clazz));

      clazz = (String) m.invoke(ctrl, "char");
      Assert.assertTrue("java.lang.Character".equals(clazz));

      clazz = (String) m.invoke(ctrl, "double");
      Assert.assertTrue("java.lang.Double".equals(clazz));

      clazz = (String) m.invoke(ctrl, "float");
      Assert.assertTrue("java.lang.Float".equals(clazz));

      clazz = (String) m.invoke(ctrl, "long");
      Assert.assertTrue("java.lang.Long".equals(clazz));

      clazz = (String) m.invoke(ctrl, "com.logitags.cibet.util.TEntity");
      Assert.assertTrue("com.logitags.cibet.util.TEntity".equals(clazz));
   }

}
