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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.CibetUtil;

/**
 * looks up an EJB in InitialContext applying different strategies for the JNDI name.
 * 
 * @author Wolfgang
 * 
 */
public class EjbLookup {

   private static Log log = LogFactory.getLog(EjbLookup.class);

   private static String JNDI_PROPERTIES_FILENAME = "jndi.properties";

   private static List<JndiNameStrategy> strategies = new ArrayList<JndiNameStrategy>();

   private static Map<Class<?>, String> jndiNameMap = Collections.synchronizedMap(new HashMap<Class<?>, String>());

   private static InitialContext ctx;

   static {
      strategies.add(new GlobalNamespaceStrategy());
      strategies.add(new ModuleNamespaceStrategy());
      strategies.add(new InterfaceStrategy());
      strategies.add(new BeanNameStrategy());
      strategies.add(new JBossSimpleNameStrategy());
      strategies.add(new JBossAnnotationNameStrategy());
      strategies.add(new MappedNameInterfaceStrategy());
      strategies.add(new BeanNameInterfaceTypeStrategy());
      strategies.add(new EjbNameInterfaceTypeStrategy());
      strategies.add(new MappedNameStrategy());
   }

   protected EjbLookup() {
   }

   public static <T> T lookupEjb(String jndiName, Class<T> clazz) {
      T ejb = null;
      if (jndiName != null) {
         try {
            ejb = (T) getInitialContext().lookup(jndiName);
         } catch (NamingException e) {
            log.warn("Failed to lookup " + clazz.getName() + " EJB. NamingException with jndi name '" + jndiName
                  + "': " + e.getMessage());
         }
      }

      if (ejb == null) {
         ejb = lookupEjb(clazz);
      }
      return ejb;
   }

   private static <T> T lookupEjb(Class<T> clazz) {
      if (jndiNameMap.containsKey(clazz)) {
         String jname = jndiNameMap.get(clazz);
         if (jname != null) {
            try {
               T ejb = (T) getInitialContext().lookup(jname);
               if (log.isDebugEnabled()) {
                  log.debug("EJB found from cache with jndi name '" + jname + "' for class " + clazz.getName());
               }
               return ejb;

            } catch (NamingException e) {
               log.warn("NamingException with jndi name '" + jname + "': " + e.getMessage());
            }
         } else {
            return null;
         }
      }

      for (JndiNameStrategy strat : strategies) {
         List<String> jndiNames = strat.getJNDINames(clazz);
         for (String name : jndiNames) {
            try {
               T ejb = (T) getInitialContext().lookup(name);
               T thisIsIt = null;
               Class<?>[] interfaces = clazz.getInterfaces();
               if (interfaces.length != 0) {
                  for (int i = 0; i < interfaces.length; i++) {
                     if (interfaces[i].isAssignableFrom(ejb.getClass())) {
                        thisIsIt = ejb;
                        break;
                     }
                  }
               } else {
                  thisIsIt = ejb;
               }

               if (thisIsIt != null) {
                  if (log.isDebugEnabled()) {
                     log.debug(strat.getClass().getName() + " found EJB with jndi name '" + name + "' for class "
                           + clazz.getName());
                  }
                  jndiNameMap.put(clazz, name);
                  return thisIsIt;
               }

            } catch (NamingException e) {
               log.info("try lookup with jndi name '" + name + "': " + e.getMessage());
            }
         }
      }

      // nothing found
      jndiNameMap.put(clazz, null);

      StringBuffer b = new StringBuffer("failed to lookup " + clazz.getName() + " EJB with following JNDI names:");
      for (JndiNameStrategy strat : strategies) {
         List<String> jndiNames = strat.getJNDINames(clazz);
         for (String name : jndiNames) {
            b.append("\n");
            b.append(name);
         }
      }
      b.append("\nSet an explicit JNDI name in web.xml if necessary!");
      log.warn(b.toString());
      return null;
   }

   public static void clearCache() {
      jndiNameMap.clear();
      ctx = null;
   }

   public static List<String> getJndiNames(Class<?> clazz) {
      List<String> list = new ArrayList<String>();

      for (JndiNameStrategy strat : strategies) {
         List<String> jndiNames = strat.getJNDINames(clazz);
         list.addAll(jndiNames);
      }
      return list;
   }

   public static void setJndiPropertiesFilename(String name) {
      JNDI_PROPERTIES_FILENAME = name;
   }

   /**
    * OpenEJB does not work in Jetty or Tomcat environment with default loading of jndi.properties.
    * 
    * @return
    * @throws NamingException
    */
   private static synchronized InitialContext getInitialContext() throws NamingException {
      if (ctx == null) {
         log.debug("init InitialContext with " + JNDI_PROPERTIES_FILENAME);
         Properties jndiProps = CibetUtil.loadProperties(JNDI_PROPERTIES_FILENAME);
         if (jndiProps == null) {
            log.info("failed to find " + JNDI_PROPERTIES_FILENAME
                  + " on the classpath. In an EE environment this file could be necessary to look up EJBs");
            jndiProps = new Properties();
         }
         ctx = new InitialContext(jndiProps);

         if (log.isDebugEnabled()) {
            try {
               doList(1, ctx, "");
            } catch (NamingException e) {
               log.warn(e.getMessage());
            }
         }
      }
      return ctx;
   }

   private static void doList(int level, InitialContext context, String name) throws NamingException {

      for (NamingEnumeration ne = context.list(name); ne.hasMore();) {
         NameClassPair ncp = (NameClassPair) ne.next();
         String objectName = ncp.getName();
         String className = ncp.getClassName();
         String classText = " :" + className;
         if (isContext(className)) {
            log.debug(getPad(level) + "+" + objectName + classText);
            doList(level + 1, context, name + "/" + objectName);
         } else {
            log.debug(getPad(level) + "-" + objectName + classText);
         }
      }
   }

   private static boolean isContext(String className) {
      try {
         Class objectClass = Thread.currentThread().getContextClassLoader().loadClass(className);
         return Context.class.isAssignableFrom(objectClass);
      } catch (ClassNotFoundException ex) {
         // object is probably not a context, report as non-context
         return false;
      }
   }

   private static String getPad(int level) {
      StringBuffer pad = new StringBuffer();
      for (int i = 0; i < level; i++) {
         pad.append(" ");
      }
      return pad.toString();
   }
}
