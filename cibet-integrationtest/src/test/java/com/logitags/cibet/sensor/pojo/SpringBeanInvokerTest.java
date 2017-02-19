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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.cibethelper.SpringExampleBean;
import com.cibethelper.base.StaticFactoryService;
import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.sensor.common.Invoker;

/**
 * -javaagent:D:\Java\maven-repository\org\aspectj\aspectjweaver\1.8.8\aspectjweaver-1.8.8.jar
 * 
 * @author Wolfgang
 *
 */
public class SpringBeanInvokerTest {

   private static Logger log = Logger.getLogger(SpringBeanInvokerTest.class);

   @BeforeClass
   public static void initContext() {
      new ClassPathXmlApplicationContext(new String[] { "spring-context_3.xml" });
   }

   @Test
   public void createObjectConstructorFacWithParam() throws Exception {
      log.debug("start createObjectConstructorFacWithParam");

      Invoker fac = SpringBeanInvoker.createInstance();
      Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
      m.setAccessible(true);
      SpringExampleBean o = (SpringExampleBean) m.invoke(fac, "MySpringExampleBean", SpringExampleBean.class);
      Assert.assertNotNull(o);
   }

   @Test
   public void findBean() {
      SpringBeanInvoker fac = (SpringBeanInvoker) SpringBeanInvoker.createInstance();
      Assert.assertNotNull(fac.findBean(SpringExampleBean.class));
   }

   @Test
   public void findBeanNull() {
      SpringBeanInvoker fac = (SpringBeanInvoker) SpringBeanInvoker.createInstance();
      Assert.assertNull(fac.findBean(Archive.class));
   }

   @Test
   public void findBeanMoreThanOne() {
      SpringBeanInvoker fac = (SpringBeanInvoker) SpringBeanInvoker.createInstance();
      try {
         fac.findBean(StaticFactoryService.class);
         Assert.fail();
      } catch (Exception e) {
         Assert.assertTrue(e.getMessage().startsWith("2 bean definitions found for class"));
      }
   }

   /**
    * Start in own thread to test threadsafe beanId parameter in SpringBeanInvoker.
    * 
    * @throws Exception
    */
   @Test
   public void createObjectSimple() throws Exception {
      log.debug("start createObjectSimple");
      Thread p = new Thread() {

         public void run() {
            try {
               Invoker fac = SpringBeanInvoker.createInstance();
               Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
               m.setAccessible(true);
               SpringExampleBean o = (SpringExampleBean) m.invoke(fac, null, SpringExampleBean.class);
               Assert.assertNotNull(o);
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }

      };

      p.start();
   }

   /**
    * Start in own thread to test threadsafe beanId parameter in SpringBeanInvoker.
    * 
    * @throws Exception
    */
   @Test
   public void createObjectTwoBeans() throws Exception {
      log.debug("start createObjectTwoBeans");

      Thread p = new Thread() {

         public void run() {
            try {
               Invoker fac = SpringBeanInvoker.createInstance();
               Method m = fac.getClass().getDeclaredMethod("createObject", String.class, Class.class);
               m.setAccessible(true);
               try {
                  m.invoke(fac, null, StaticFactoryService.class);
                  Assert.fail();
               } catch (InvocationTargetException e) {
                  log.debug(e.getCause().getMessage(), e);
                  Assert.assertTrue(e.getCause().getMessage().startsWith("2 bean definitions found for class"));
               }
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         }

      };

      p.start();
   }

}
