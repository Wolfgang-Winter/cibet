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
package com.logitags.cibet.sensor.ejb;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.ejb.CibetTest2MappedNameEJBImpl;
import com.cibethelper.ejb.CibetTestEJB;
import com.cibethelper.ejb.CibetTestStatefuMappedNamelEJBImpl;
import com.cibethelper.ejb.CibetTestStatefulEJB;
import com.cibethelper.entities.TEntity;
import com.logitags.cibet.jndi.EjbLookup;
import com.logitags.cibet.jndi.JBossAnnotationNameStrategy;
import com.logitags.cibet.jndi.MappedNameInterfaceStrategy;

public class EJBInvokerTest {

   private static Logger log = Logger.getLogger(EJBInvokerTest.class);

   @Test
   public void findEJBNameStateless() throws Exception {
      Method m = JBossAnnotationNameStrategy.class.getSuperclass().getDeclaredMethod("findEJBName", Class.class);
      m.setAccessible(true);
      JBossAnnotationNameStrategy inv = new JBossAnnotationNameStrategy();
      String result = (String) m.invoke(inv, CibetTestEJB.class);
      Assert.assertEquals("com.cibethelper.ejb.CibetTestEJB", result);
   }

   @Test
   public void findEJBNameStatefull() throws Exception {
      Method m = JBossAnnotationNameStrategy.class.getSuperclass().getDeclaredMethod("findEJBName", Class.class);
      m.setAccessible(true);
      JBossAnnotationNameStrategy inv = new JBossAnnotationNameStrategy();
      String result = (String) m.invoke(inv, CibetTestStatefulEJB.class);
      Assert.assertEquals("com.cibethelper.ejb.CibetTestStatefulEJB", result);
   }

   @Test
   public void findEJBNameNoEJB() throws Exception {
      Method m = JBossAnnotationNameStrategy.class.getSuperclass().getDeclaredMethod("findEJBName", Class.class);
      m.setAccessible(true);
      JBossAnnotationNameStrategy inv = new JBossAnnotationNameStrategy();
      try {
         m.invoke(inv, TEntity.class);
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertTrue(e.getCause() instanceof RuntimeException);
         Assert.assertTrue(e.getCause().getMessage().startsWith("Failed to lookup EJB instance of class"));
      }
   }

   @Test
   public void jndiNames() throws Exception {
      log.debug("start jndiNames()");

      List<String> l = EjbLookup.getJndiNames(CibetTestStatefulEJB.class);
      Assert.assertNotNull(l);
      for (String s : l) {
         log.debug(s);
      }

      int expectedSize = 16;
      try {
         Context ctx = new InitialContext();
         ctx.lookup("java:app/AppName");
         expectedSize = 18;
      } catch (NamingException e) {
      }
      Assert.assertEquals(expectedSize, l.size());
   }

   @Test
   public void jndiNamesWithMappedName() throws Exception {
      log.debug("start jndiNamesWithMappedName()");
      List<String> l = EjbLookup.getJndiNames(CibetTest2MappedNameEJBImpl.class);
      Assert.assertNotNull(l);
      for (String s : l) {
         log.debug(s);
      }

      int expectedSize = 19;
      try {
         Context ctx = new InitialContext();
         ctx.lookup("java:app/AppName");
         expectedSize = 21;
      } catch (NamingException e) {
      }

      Assert.assertEquals(expectedSize, l.size());
   }

   @Test
   public void findEJBMappedNameStatefull() throws Exception {
      Method m = MappedNameInterfaceStrategy.class.getSuperclass().getDeclaredMethod("findEJBMappedName", Class.class);
      m.setAccessible(true);
      MappedNameInterfaceStrategy inv = new MappedNameInterfaceStrategy();
      String result = (String) m.invoke(inv, CibetTestStatefuMappedNamelEJBImpl.class);
      Assert.assertEquals("CibetTestStatefulEJBImpl with mapped name", result);
   }

   @Test
   public void findEJBMappedNameStateless() throws Exception {
      Method m = MappedNameInterfaceStrategy.class.getSuperclass().getDeclaredMethod("findEJBMappedName", Class.class);
      m.setAccessible(true);
      MappedNameInterfaceStrategy inv = new MappedNameInterfaceStrategy();
      String result = (String) m.invoke(inv, CibetTest2MappedNameEJBImpl.class);
      Assert.assertEquals("CibetTest2EJBImpl_w_m", result);
   }

   @Test
   public void findEJBMappedNameNoEJB() throws Exception {
      Method m = MappedNameInterfaceStrategy.class.getSuperclass().getDeclaredMethod("findEJBMappedName", Class.class);
      m.setAccessible(true);
      MappedNameInterfaceStrategy inv = new MappedNameInterfaceStrategy();
      try {
         m.invoke(inv, TEntity.class);
         Assert.fail();
      } catch (InvocationTargetException e) {
         Assert.assertTrue(e.getCause() instanceof RuntimeException);
         Assert.assertTrue(e.getCause().getMessage().startsWith("Failed to lookup EJB instance of class"));
      }
   }

}
