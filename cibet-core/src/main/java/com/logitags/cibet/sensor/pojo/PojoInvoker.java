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

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.sensor.common.Invoker;
import com.logitags.cibet.sensor.common.MethodInvoker;

/**
 * creates a new object of the given class. Sets optional constructor parameters. Creates also objects from static
 * Singleton methods.
 * 
 * @version $Id$
 */
public class PojoInvoker extends MethodInvoker implements Invoker, Serializable {

   private transient Log log = LogFactory.getLog(PojoInvoker.class);

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Invoker instance = null;

   public static synchronized Invoker createInstance() {
      if (instance == null) {
         instance = new PojoInvoker();
      }
      return instance;
   }

   protected PojoInvoker() {
   }

   /**
    * 
    * @param constructorParam
    * @param clazz
    * @return Object
    */
   protected <T> T createObject(String constructorParam, Class<T> clazz) {
      try {
         if (constructorParam != null && constructorParam.length() > 0) {
            try {
               Constructor<T> co = clazz.getConstructor(String.class);
               log.debug("construct object with String constructor and param " + constructorParam);
               return co.newInstance(constructorParam);
            } catch (NoSuchMethodException e) {
               // Singleton ?
               Method[] methods = clazz.getMethods();
               for (Method method : methods) {
                  if (method.getReturnType().isAssignableFrom(clazz) && method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0] == String.class && Modifier.isStatic(method.getModifiers())) {
                     return (T) method.invoke(null, new Object[] { constructorParam });
                  }
               }
               throw new InstantiationException(
                     "Failed to create an object of type " + clazz + ": No String parameter constructor or static "
                           + "singleton method with a String parameter " + "which returns type " + clazz + " found");
            }
         } else {
            try {
               return clazz.newInstance();
            } catch (Exception e) {
               // Singleton ?
               Method[] methods = clazz.getMethods();
               for (Method method : methods) {
                  if (method.getReturnType().isAssignableFrom(clazz) && method.getParameterTypes().length == 0
                        && Modifier.isStatic(method.getModifiers())) {
                     return (T) method.invoke(null, (Object[]) null);
                  }
               }
               throw new InstantiationException(
                     "Failed to create an object of type " + clazz + ": No nullary constructor or static singleton "
                           + "method which returns type " + clazz + " found");
            }
         }
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

}
