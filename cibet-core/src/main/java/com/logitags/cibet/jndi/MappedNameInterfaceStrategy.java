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
package com.logitags.cibet.jndi;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * $mappedName#$InterfaceName and $mappedName#ejb.$SimpleInterfaceName
 * 
 * @author Wolfgang
 * 
 */
public class MappedNameInterfaceStrategy implements JndiNameStrategy {

   private static Log log = LogFactory.getLog(MappedNameInterfaceStrategy.class);

   @Override
   public List<String> getJNDINames(Class<?> clazz) {
      List<String> names = new ArrayList<String>();
      String mappedName = findEJBMappedName(clazz);
      if (mappedName != null && mappedName.length() > 0) {
         Class<?>[] supers = clazz.getInterfaces();
         for (Class<?> c : supers) {
            names.add(mappedName + "#" + c.getName());
            names.add(mappedName + "#ejb." + c.getSimpleName());
         }
      }
      return names;
   }

   protected String findEJBMappedName(Class<?> clazz) {
      try {
         Class<Annotation> cl = (Class<Annotation>) Class.forName("javax.ejb.Stateless");
         Annotation less = clazz.getAnnotation(cl);
         if (less != null) {
            return (String) cl.getMethod("mappedName").invoke(less);
         }

         cl = (Class<Annotation>) Class.forName("javax.ejb.Stateful");
         Annotation ful = clazz.getAnnotation(cl);
         if (ful != null) {
            return (String) cl.getMethod("mappedName").invoke(ful);
         }

         cl = (Class<Annotation>) Class.forName("javax.ejb.Singleton");
         Annotation sin = clazz.getAnnotation(cl);
         if (sin != null) {
            return (String) cl.getMethod("mappedName").invoke(sin);
         }

         String msg = "Failed to lookup EJB instance of class " + clazz.getName()
               + " No annotation @Stateful, @Stateless or @Singleton found";
         log.error(msg);
         throw new RuntimeException(msg);
      } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
            | NoSuchMethodException | SecurityException e) {
         log.warn(e.getMessage());
         return null;
      }
   }

}
