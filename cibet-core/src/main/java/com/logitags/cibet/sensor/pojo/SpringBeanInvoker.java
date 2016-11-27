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
/**
 * 
 */
package com.logitags.cibet.sensor.pojo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.logitags.cibet.sensor.common.Invoker;
import com.logitags.cibet.sensor.common.MethodInvoker;

/**
 *
 */
@Component
public class SpringBeanInvoker extends MethodInvoker implements Invoker,
      ApplicationContextAware {

   private Log log = LogFactory.getLog(SpringBeanInvoker.class);

   private static ApplicationContext context;

   public static Invoker createInstance() {
      if (context != null) {
         return context.getBean(SpringBeanInvoker.class);
      } else {
         return null;
      }
   }

   /**
    * beanId parameter has following format: context[, beanId]<br>
    * context is the Spring context configuration where the controlled bean is
    * defined. It could be one of<br>
    * xml file in the classpath (ClassPathXmlApplicationContext)<br>
    * xml file in the file system (FileSystemXmlApplicationContext)<br>
    * The beanId is optional. If omitted the configuration may only contain one
    * definition for the controlled bean class.
    * 
    */
   @Override
   protected <T> T createObject(String beanId, Class<T> clazz) {
      if (beanId != null && beanId.length() > 0) {
         return context.getBean(beanId, clazz);
      } else {
         String[] ids = context.getBeanNamesForType(clazz);
         if (ids.length != 1) {
            String msg = ids.length
                  + " bean definitions found for class "
                  + clazz.getName()
                  + ". If more than one definition is found the bean id "
                  + "must be set explicitly in the @CibetIntercept param=\"beanId\"";
            log.error(msg);
            throw new RuntimeException(msg);
         }
         return (T) context.getBean(ids[0]);
      }
   }

   @SuppressWarnings("unchecked")
   public <T> T findBean(Class<T> clazz) {
      String[] ids = context.getBeanNamesForType(clazz);
      if (ids.length == 1) {
         return (T) context.getBean(ids[0]);

      } else if (ids.length > 1) {
         String msg = ids.length
               + " bean definitions found for class "
               + clazz.getName()
               + ". If more than one definition is found the bean id "
               + "must be set explicitly in the @CibetIntercept param=\"beanId\"";
         log.error(msg);
         throw new RuntimeException(msg);
      } else {
         return null;
      }
   }

   public synchronized void setApplicationContext(ApplicationContext ctx) {
      context = ctx;
   }
}
