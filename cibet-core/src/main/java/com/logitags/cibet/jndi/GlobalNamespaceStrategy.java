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
import java.util.List;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Use portable JNDI lookup in the java:global namespace to look up local enterprise beans within the same module.
 * JavaEE6
 * 
 * @author Wolfgang
 * 
 */
public class GlobalNamespaceStrategy extends AbstractLookupStrategy implements JndiNameStrategy {

   private static Log log = LogFactory.getLog(GlobalNamespaceStrategy.class);

   @Override
   public List<String> getJNDINames(Class<?> clazz) {
      List<String> names = new ArrayList<String>();

      try {
         Context ctx = new InitialContext();
         String module = resolveAppName(ctx);
         if (module != null && module.length() > 0) {
            String ejbName = findEJBName(clazz);
            if (ejbName != null && ejbName.length() > 0) {
               names.add("java:global/" + module + "/" + ejbName);
            }
            names.add("java:global/" + module + "/" + clazz.getSimpleName());
            Class<?>[] supers = clazz.getInterfaces();
            for (Class<?> c : supers) {
               names.add("java:global/" + module + "/" + clazz.getSimpleName() + "/" + c.getSimpleName());
            }
         }

      } catch (NamingException e) {
         log.warn(e.getMessage());
      }
      return names;
   }

   private String resolveAppName(Context ctx) {
      try {
         String module = (String) ctx.lookup("java:app/AppName");
         log.debug("appName=" + module);
         if (module != null && module.length() > 0) {
            return module;
         }

      } catch (NamingException e) {
         log.warn("NamingException: " + e.getMessage());
      }

      try {
         String module = (String) ctx.lookup("java:module/ModuleName");
         log.debug("appName=" + module);
         return module;

      } catch (NamingException e) {
         log.warn("NamingException: " + e.getMessage());
         return null;
      }
   }

}
