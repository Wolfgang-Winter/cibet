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

import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.SingletonFactory;
import com.cibethelper.base.SingletonFactoryService;
import com.cibethelper.base.SingletonFactoryService2;
import com.cibethelper.base.StaticFactory;
import com.cibethelper.base.StaticFactoryService;
import com.cibethelper.entities.TCompareEntity;
import com.cibethelper.entities.TComplexEntity;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.sensor.common.Invoker;

public class FactoryInvokerTest {

   Invoker fac = FactoryInvoker.createInstance();

   @Test
   public void createObjectConstructor() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      TCompareEntity o = (TCompareEntity) m.invoke(fac, TComplexEntity.class.getName(), TCompareEntity.class);
      Assert.assertNotNull(o);
   }

   @Test(expected = InvocationTargetException.class)
   public void createObjectSingletonMethodNotExisting() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      m.invoke(fac, SingletonFactory.class.getName() + ".nix()", TEntity.class);
   }

   @Test
   public void createObjectSingletonInvocation() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      try {
         m.invoke(fac, SingletonFactory.class.getName() + ".withException()", String.class);
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertEquals(RuntimeException.class, e.getCause().getClass());
         Assert.assertEquals(InvocationTargetException.class, e.getCause().getCause().getClass());
      }
   }

   @Test
   public void createObjectConstructorFacWithMethod() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      TCompareEntity o = (TCompareEntity) m.invoke(fac, TComplexEntity.class.getName() + ".getCompareEntity()",
            TCompareEntity.class);
      Assert.assertNotNull(o);
   }

   @Test
   public void createObjectSingletonSimpleConstructor() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      TEntity o = (TEntity) m.invoke(fac, SingletonFactory.class.getName(), TEntity.class);
      Assert.assertNotNull(o);

      SingletonFactoryService o2 = (SingletonFactoryService) m.invoke(fac,
            SingletonFactory.class.getName() + ".create()", SingletonFactoryService.class);
      Assert.assertNotNull(o);

      SingletonFactoryService2 o3 = (SingletonFactoryService2) m.invoke(fac,
            SingletonFactory.class.getName() + ".create2()", SingletonFactoryService2.class);
      Assert.assertNotNull(o3);
   }

   @Test
   public void createObjectSimpleConstructor() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      StaticFactoryService o = (StaticFactoryService) m.invoke(fac, StaticFactory.class.getName(),
            StaticFactoryService.class);
      Assert.assertNotNull(o);

      TEntity o2 = (TEntity) m.invoke(fac, StaticFactory.class.getName(), TEntity.class);
      Assert.assertNotNull(o2);
   }

   @Test
   public void createObjectNoFactoryClass() throws Exception {
      Invoker fac2 = FactoryInvoker.createInstance();
      Method m = fac2.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      try {
         TComplexEntity o = (TComplexEntity) m.invoke(fac2, null, TComplexEntity.class);
         Assert.fail();
      } catch (InvocationTargetException e1) {
         Throwable e = e1.getCause();
         Assert.assertTrue(e.getMessage().endsWith("param in @Intercept is null"));
      }
   }

   @Test(expected = IllegalArgumentException.class)
   public void setParameterNull() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("parseParameter", String.class);
      m.setAccessible(true);
      m.invoke(fac, null);
   }

   @Test
   public void setParameterClassOnly() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("parseParameter", String.class);
      m.setAccessible(true);
      String[] res = (String[]) m.invoke(fac, "  TestClass");
      Assert.assertEquals("TestClass", res[0]);
      Assert.assertNull(res[1]);
   }

   @Test
   public void setParameterClassAndMethod() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("parseParameter", String.class);
      m.setAccessible(true);
      String[] res = (String[]) m.invoke(fac, "  TestClass.callSomething()");
      Assert.assertEquals("TestClass", res[0]);
      Assert.assertEquals("callSomething", res[1]);
   }

   @Test
   public void setParameterClassAndMethodMissingPoint() throws Exception {
      Method m = fac.getClass().getDeclaredMethod("parseParameter", String.class);
      m.setAccessible(true);
      try {
         m.invoke(fac, "  TestClass_callSomething()");
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertEquals(IllegalArgumentException.class, e.getTargetException().getClass());
      }
   }

}
