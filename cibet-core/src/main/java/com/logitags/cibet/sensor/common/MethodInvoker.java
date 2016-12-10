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
package com.logitags.cibet.sensor.common;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ResourceParameter;

/**
 * For invoking methods.
 * 
 * @author test
 * 
 */
public abstract class MethodInvoker implements Invoker {

   private static Log log = LogFactory.getLog(MethodInvoker.class);

   protected static final ParameterSequenceComparator comparator = new ParameterSequenceComparator();

   public Object execute(String parameter, String targetType, String methodName, List<ResourceParameter> parameters)
         throws Exception {
      try {
         Collections.sort(parameters, comparator);

         Class<?> objClass = Class.forName(targetType);
         Class<?>[] params = getParamTypes(parameters);
         Method method = objClass.getMethod(methodName, params);
         if (Modifier.isStatic(method.getModifiers())) {
            return method.invoke(null, getParamValues(parameters));

         } else {
            Object invokedObject = createObject(parameter, objClass);
            // in case object is a proxy
            method = invokedObject.getClass().getMethod(methodName, params);
            return method.invoke(invokedObject, getParamValues(parameters));
         }
      } catch (Exception e) {
         throw e;
      }
   }

   protected Class<?>[] getParamTypes(List<ResourceParameter> params) {
      Class<?>[] types = new Class[params.size()];
      for (int i = 0; i < params.size(); i++) {
         ResourceParameter param = params.get(i);
         types[i] = classForName(param.getClassname());
      }
      return types;
   }

   protected Object[] getParamValues(List<ResourceParameter> params) {
      Object[] objects = new Object[params.size()];
      for (int i = 0; i < params.size(); i++) {
         ResourceParameter param = params.get(i);
         objects[i] = param.getUnencodedValue();
      }
      return objects;
   }

   /**
    * creates a Class object from the given class name. Creates also Class objects from primitive types and arrays.
    * 
    * @param classname
    * @return
    */
   private Class<?> classForName(String classname) {
      try {
         if (classname == null) {
            return null;
         } else if (classname.equals("byte")) {
            return byte.class;
         } else if (classname.equals("boolean")) {
            return boolean.class;
         } else if (classname.equals("char")) {
            return char.class;
         } else if (classname.equals("double")) {
            return double.class;
         } else if (classname.equals("float")) {
            return float.class;
         } else if (classname.equals("int")) {
            return int.class;
         } else if (classname.equals("long")) {
            return long.class;
         } else if (classname.equals("short")) {
            return short.class;
         }

         Matcher m = CibetUtil.classNamePattern.matcher(classname);
         if (m.matches()) {
            log.debug("matches array class");
            Class<?> componentClass = CibetUtil.arrayClassForName(classname);

            int[] dimensions = new int[m.group(1).length()];
            for (int i = 0; i < dimensions.length; i++) {
               dimensions[i] = 1;
               log.debug("set dimension");
            }

            Object array = Array.newInstance(componentClass, dimensions);
            Class<?> clazz = array.getClass();
            log.debug("find class " + clazz);
            return clazz;
         }

         return Class.forName(classname);

      } catch (ClassNotFoundException e) {
         String msg = "Failed to create Class from type " + classname + ": " + e.getMessage();
         log.error(msg, e);
         throw new RuntimeException(msg, e);
      }
   }

   /**
    * creates an object of type T
    * 
    * @param parameter
    *           parameter
    * @param clazz
    *           type T
    * @return T
    */
   protected abstract <T> T createObject(String parameter, Class<T> clazz);

}
