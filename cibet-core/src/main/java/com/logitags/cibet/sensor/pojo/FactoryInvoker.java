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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.sensor.common.Invoker;

/**
 * This implementation creates an object instance with an object factory class.
 */
public class FactoryInvoker extends PojoInvoker {

   private transient Log log = LogFactory.getLog(FactoryInvoker.class);

   /**
    * 
    */
   private static final long serialVersionUID = 2018884101601988185L;

   private static Invoker instance = null;

   public static synchronized Invoker createInstance() {
      if (instance == null) {
         instance = new FactoryInvoker();
      }
      return instance;
   }

   protected FactoryInvoker() {
   }

   /**
    * Possible for factory instantiation: constructor, Singleton, static method. facClassMethod parameter contains the
    * factory class and optionally the factory method like:<br>
    * com.project.MyFactory<br>
    * or com.project.MyFactory.instantiate()
    * 
    */
   @Override
   protected <T> T createObject(String facClassMethod, Class<T> clazz) {
      if (facClassMethod == null) {
         throw new IllegalArgumentException("param in @Intercept is null");
      }
      String[] factoryClassMethod = parseParameter(facClassMethod);

      try {
         Class<?> facClass = Class.forName(factoryClassMethod[0]);
         // 1. check if static method, method given
         if (factoryClassMethod[1] != null) {
            Method method = facClass.getMethod(factoryClassMethod[1], (Class[]) null);
            if (method.getReturnType().isAssignableFrom(clazz) && method.getParameterTypes().length == 0
                  && Modifier.isStatic(method.getModifiers())) {
               return (T) method.invoke(null, (Object[]) null);
            }
         }

         // 2. check if static method, no method given
         Method[] methods = facClass.getMethods();
         for (Method method : methods) {
            if (method.getReturnType().isAssignableFrom(clazz) && method.getParameterTypes().length == 0
                  && Modifier.isStatic(method.getModifiers())) {
               return (T) method.invoke(null, (Object[]) null);
            }
         }

         Object factory = super.createObject(null, facClass);

         // 3. Singleton or constructor, method given
         if (factoryClassMethod[1] != null) {
            Method method = facClass.getMethod(factoryClassMethod[1], (Class[]) null);
            if (method.getReturnType().isAssignableFrom(clazz) && method.getParameterTypes().length == 0) {
               return (T) method.invoke(factory, (Object[]) null);
            }
         }

         // 3. Singleton or constructor, no method given
         for (Method method : methods) {
            if (method.getReturnType().isAssignableFrom(clazz) && method.getParameterTypes().length == 0) {
               T t = (T) method.invoke(factory, (Object[]) null);
               return t;
            }
         }

         throw new NoSuchMethodException(
               "No method found with return type assignable from " + clazz.getName() + " and null parameter");

      } catch (ClassNotFoundException e) {
         String msg = "Failed to instantiate object from class " + clazz.getName() + ": Factory class "
               + factoryClassMethod[0] + " could not be instantiated: " + e.getMessage();
         log.error(msg, e);
         throw new IllegalArgumentException(msg, e);
      } catch (NoSuchMethodException e) {
         String msg = "Failed to instantiate object from class " + clazz.getName() + ": Factory method "
               + factoryClassMethod[1] + " could not be found in class " + factoryClassMethod[0] + ": "
               + e.getMessage();
         log.error(msg, e);
         throw new RuntimeException(msg, e);
      } catch (InvocationTargetException e) {
         String msg = "Failed to instantiate object from class " + clazz.getName() + " with factory method "
               + factoryClassMethod[1] + " in factory class " + factoryClassMethod[0] + ": " + e.getMessage();
         log.error(msg, e);
         throw new RuntimeException(msg, e);
      } catch (IllegalAccessException e) {
         String msg = "Failed to instantiate object from class " + clazz.getName() + " with factory method "
               + factoryClassMethod[1] + " in factory class " + factoryClassMethod[0] + ": " + e.getMessage();
         log.error(msg, e);
         throw new RuntimeException(msg, e);
      }
   }

   /**
    * The factory class and optionally the factory method:<br>
    * com.project.MyFactory<br>
    * or com.project.MyFactory.instantiate()
    * 
    * @see com.logitags.cibet.sensor.common.Invoker#setParameter(java.lang.String)
    * 
    * @param parameter
    * @return 1. element is the factoryClass, 2. element the factoryMethod
    */
   private String[] parseParameter(String parameter) {
      if (parameter == null) {
         throw new IllegalArgumentException("parameter is null");
      }
      String[] result = new String[2];

      parameter = parameter.trim();
      if (parameter.endsWith("()")) {
         int index = parameter.lastIndexOf(".");
         if (index < 0) {
            throw new IllegalArgumentException("param attribute of @Intercept annotation must be of "
                  + "format 'classname' or 'classname.methodname()'");
         }
         result[0] = parameter.substring(0, index);
         result[1] = parameter.substring(index + 1, parameter.length() - 2);
      } else {
         result[0] = parameter;
         result[1] = null;
      }
      return result;
   }

}
