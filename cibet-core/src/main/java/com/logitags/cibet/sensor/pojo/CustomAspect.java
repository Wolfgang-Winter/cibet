/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.sensor.pojo;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import com.logitags.cibet.sensor.common.Invoker;
import com.logitags.cibet.sensor.ejb.EJBInvoker;

@Aspect
public abstract class CustomAspect extends AbstractAspect {

   private Log log = LogFactory.getLog(CustomAspect.class);

   private static Boolean springAvailable;

   @Pointcut
   abstract void cibetIntercept();

   @Around(value = "cibetIntercept()", argNames = "thisJoinPoint")
   public Object doIntercept(ProceedingJoinPoint thisJoinPoint) throws Throwable {
      ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

      Class<? extends Invoker> factoryClass = PojoInvoker.class;

      Object invokedObject = thisJoinPoint.getTarget();
      if (invokedObject != null) {

         // check if EJB
         final String stateless = "javax.ejb.Stateless";
         final String stateful = "javax.ejb.Stateful";
         try {
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> statelessClass = (Class<? extends Annotation>) classLoader.loadClass(stateless);
            @SuppressWarnings("unchecked")
            Class<? extends Annotation> statefulClass = (Class<? extends Annotation>) classLoader.loadClass(stateful);

            if (invokedObject.getClass().getAnnotation(statelessClass) != null
                  || invokedObject.getClass().getAnnotation(statefulClass) != null) {
               factoryClass = EJBInvoker.class;
            }
         } catch (ClassNotFoundException e) {
            log.info("Failed to instantiate " + stateless);
         }

         // check if Spring bean
         if (factoryClass == PojoInvoker.class && isSpringAvailable()) {
            try {
               final String springInvokerClassname = "com.logitags.cibet.sensor.pojo.SpringBeanInvoker";
               @SuppressWarnings("unchecked")
               Class<? extends Invoker> springInvokerClass = (Class<? extends Invoker>) classLoader
                     .loadClass(springInvokerClassname);
               Method createMethod = springInvokerClass.getMethod("createInstance", (Class<?>[]) null);
               Object springInvoker = createMethod.invoke(null, (Object[]) null);
               if (springInvoker != null) {
                  Method findBean = springInvokerClass.getMethod("findBean", Class.class);
                  Object bean = findBean.invoke(springInvoker, invokedObject.getClass());
                  if (bean != null) {
                     factoryClass = springInvokerClass;
                  }
               }

            } catch (ClassNotFoundException e) {
               log.info(e.getMessage());
            }
         }

         log.debug("CustomAspect uses " + factoryClass + " for class " + invokedObject.getClass().getName());
      }

      return doIntercept(thisJoinPoint, factoryClass, null);
   }

   private boolean isSpringAvailable() {
      if (springAvailable == null) {
         ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
         try {
            classLoader.loadClass("org.springframework.context.ApplicationContextAware");
            springAvailable = true;
         } catch (Throwable e) {
            springAvailable = false;
         }
      }
      return springAvailable;
   }

}
