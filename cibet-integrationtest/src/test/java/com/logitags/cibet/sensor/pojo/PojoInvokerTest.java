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
package com.logitags.cibet.sensor.pojo;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.SpringExampleBean;
import com.cibethelper.base.ParamSingleton;
import com.cibethelper.base.SimpleSingleton;
import com.cibethelper.base.SingletonFactory;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.dc.FourEyesActuator;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.common.Invoker;
import com.logitags.cibet.sensor.common.MethodInvoker;

public class PojoInvokerTest {

   private static Logger log = Logger.getLogger(PojoInvokerTest.class);

   private Invoker fac = PojoInvoker.createInstance();

   @Test
   public void createObjectSingleton() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      SingletonFactory o = (SingletonFactory) m.invoke(fac, null, SingletonFactory.class);
      Assert.assertNotNull(o);
   }

   @Test
   public void createObjectSimpleSingleton() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      SimpleSingleton o = (SimpleSingleton) m.invoke(fac, null, SimpleSingleton.class);
      Assert.assertNotNull(o);
   }

   @Test
   public void createObjectParamSingleton() throws Exception {
      new ParamSingleton("garnix");
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      ParamSingleton o = (ParamSingleton) m.invoke(fac, null, ParamSingleton.class);
      Assert.assertNotNull(o);
      Assert.assertEquals("garnix", ParamSingleton.getParam());
      o.clear();
   }

   @Test
   public void createObjectParamSingletonWithParam() throws Exception {
      // Assert.assertEquals("garnix", ParamSingleton.getParam());
      Invoker fac2 = PojoInvoker.createInstance();
      Method m = fac2.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      ParamSingleton o = (ParamSingleton) m.invoke(fac2, "Heinz", ParamSingleton.class);
      Assert.assertNotNull(o);
      Assert.assertEquals("Heinz", ParamSingleton.getParam());
      o.clear();
   }

   @Test
   public void createObjectSimpleConstructor() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      TComplexEntity o = (TComplexEntity) m.invoke(fac, null, TComplexEntity.class);
      Assert.assertNotNull(o);
   }

   @Test
   public void createObjectParamSimpleConstructor() throws Exception {
      Invoker fac2 = PojoInvoker.createInstance();
      Method m = fac2.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      FourEyesActuator o = (FourEyesActuator) m.invoke(fac2, "Heinz", FourEyesActuator.class);
      Assert.assertNotNull(o);
      Assert.assertEquals("Heinz", o.getName());
   }

   @Test
   public void createObjectParamSimpleConstructorError() throws Exception {
      Invoker fac2 = PojoInvoker.createInstance();
      Method m = fac2.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      try {
         m.invoke(fac2, "Heinz", TComplexEntity.class);
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertEquals(RuntimeException.class, e.getCause().getClass());
         Assert.assertEquals(InstantiationException.class, e.getCause().getCause().getClass());
      }
   }

   @Test
   public void createObjectSingletonWithParamError() throws Exception {
      Invoker fac2 = PojoInvoker.createInstance();
      Method m = fac2.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      try {
         m.invoke(fac2, "Heinz", SingletonFactory.class);
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertEquals(RuntimeException.class, e.getCause().getClass());
         Assert.assertEquals(InstantiationException.class, e.getCause().getCause().getClass());
      }
   }

   @Test
   public void invokeStatic() throws Exception {
      Set<ResourceParameter> parameters = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      ResourceParameter p1 = new ResourceParameter("pa1", String.class.getName(), "Hans",
            ParameterType.METHOD_PARAMETER, 4);
      parameters.add(p1);
      ResourceParameter p2 = new ResourceParameter("pa2", String.class.getName(), " Wurst",
            ParameterType.METHOD_PARAMETER, 5);
      parameters.add(p2);

      Object res = fac.execute(null, SpringExampleBean.class.getName(), "concatenate", parameters);
      Assert.assertNotNull(res);
      Assert.assertEquals("Hans Wurst", res);
   }

   @Test
   public void invokeStaticArray() throws Exception {
      Set<ResourceParameter> parameters = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      String[] strings = new String[] { "Hans", " Wurst2" };
      ResourceParameter p1 = new ResourceParameter("pa1", strings.getClass().getName(), strings,
            ParameterType.METHOD_PARAMETER, 4);
      parameters.add(p1);

      Object res = fac.execute(null, SpringExampleBean.class.getName(), "concatenate2", parameters);
      Assert.assertNotNull(res);
      Assert.assertEquals("Hans Wurst2", res);
   }

   @Test
   public void invokeSimpleConstructor() throws Exception {
      Set<ResourceParameter> parameters = new TreeSet<ResourceParameter>(new ParameterSequenceComparator());
      List<String> strings = new ArrayList<String>();
      strings.add("Hans");
      strings.add(" Wurst3");
      ResourceParameter p1 = new ResourceParameter("pa1", List.class.getName(), strings, ParameterType.METHOD_PARAMETER,
            4);
      parameters.add(p1);

      List res = (List) fac.execute(null, TComplexEntity.class.getName(), "giveCollection4", parameters);
      Assert.assertNotNull(res);
      Assert.assertEquals("HansX", res.get(0));
      Assert.assertEquals(" Wurst3X", res.get(1));
   }

   @Test
   public void classForName() throws Exception {
      Method m = MethodInvoker.class.getDeclaredMethod("classForName", String.class);
      m.setAccessible(true);
      MethodInvoker inv = new PojoInvoker();

      byte[] t1 = new byte[] {};
      log.debug(t1.getClass().getCanonicalName());
      log.debug(t1.getClass().getName());
      byte[][] t2 = new byte[][] {};
      log.debug(t2.getClass().getCanonicalName());
      int t3 = 4;
      Object t4 = t3;
      log.debug(t4.getClass().getCanonicalName());
      log.debug(t4.getClass().getName());

      Class<?> clazz = (Class<?>) m.invoke(inv, (String) null);
      Assert.assertNull(clazz);

      clazz = (Class<?>) m.invoke(inv, "[B");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("[B".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "[[B");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("[[B".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "[L" + TEntity.class.getName() + ";");
      log.debug("canonical name: " + clazz.getCanonicalName());
      log.debug("name: '" + clazz.getName() + "'");
      Assert.assertTrue(("[L" + TEntity.class.getName() + ";").equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "short");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("short".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "byte");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("byte".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "boolean");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("boolean".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "char");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("char".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "double");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("double".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "float");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("float".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, "long");
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue("long".equals(clazz.getName()));

      clazz = (Class<?>) m.invoke(inv, TEntity.class.getName());
      log.debug("canonical name: " + clazz.getCanonicalName());
      Assert.assertTrue(TEntity.class.getName().equals(clazz.getName()));

      try {
         clazz = (Class<?>) m.invoke(inv, "CibetUtilTest.non_existing_class");
         Assert.fail();
      } catch (Exception e) {
         // okay
      }
   }

}
