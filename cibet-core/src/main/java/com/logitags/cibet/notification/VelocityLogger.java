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
package com.logitags.cibet.notification;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.velocity.runtime.RuntimeServices;
import org.apache.velocity.runtime.log.LogChute;

public class VelocityLogger implements LogChute {

   private static Log log = LogFactory.getLog(EmailNotificationProvider.class);

   @Override
   public void init(RuntimeServices arg0) throws Exception {
   }

   @Override
   public boolean isLevelEnabled(int arg0) {
      switch (arg0) {
      case LogChute.DEBUG_ID:
         return log.isDebugEnabled();
      case LogChute.TRACE_ID:
         return log.isTraceEnabled();
      case LogChute.INFO_ID:
         return log.isInfoEnabled();
      case LogChute.WARN_ID:
         return log.isWarnEnabled();
      case LogChute.ERROR_ID:
         return log.isErrorEnabled();
      }
      return true;
   }

   @Override
   public void log(int arg0, String arg1) {
      switch (arg0) {
      case LogChute.DEBUG_ID:
         log.debug(arg1);
         break;
      case LogChute.TRACE_ID:
         log.trace(arg1);
         break;
      case LogChute.INFO_ID:
         log.info(arg1);
         break;
      case LogChute.WARN_ID:
         log.warn(arg1);
         break;
      case LogChute.ERROR_ID:
         log.error(arg1);
         break;
      default:
         log.fatal(arg1);
      }
   }

   @Override
   public void log(int arg0, String arg1, Throwable e) {
      switch (arg0) {
      case LogChute.DEBUG_ID:
         log.debug(arg1, e);
         break;
      case LogChute.TRACE_ID:
         log.trace(arg1, e);
         break;
      case LogChute.INFO_ID:
         log.info(arg1, e);
         break;
      case LogChute.WARN_ID:
         log.warn(arg1, e);
         break;
      case LogChute.ERROR_ID:
         log.error(arg1, e);
         break;
      default:
         log.fatal(arg1, e);
      }
   }

}
