/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
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
package com.logitags.jmeter;

import org.apache.log4j.Logger;

import com.logitags.cibet.actuator.loadcontrol.LoadControlCallback;
import com.logitags.cibet.actuator.loadcontrol.LoadControlData;
import com.logitags.cibet.actuator.loadcontrol.MemoryMonitor;

public class LoggingCallback implements LoadControlCallback {

   private static Logger log = Logger.getLogger("LOADCONTROL_ALARM");

   @Override
   public void onShed(LoadControlData lc) {
      log.warn("SHED callback called: " + lc);
   }

   @Override
   public void onAlarm(LoadControlData lc) {
      if (MemoryMonitor.class.getSimpleName().equals(lc.getMonitor())) {
         log.warn("Memory threshold exceeded: ALARM callback called: " + lc);
      } else {
         log.warn("ALARM callback called: " + lc);
      }
   }

   @Override
   public void onThrottled(LoadControlData lc) {
      log.warn("THROTTLED callback called: " + lc);
   }

   @Override
   public void onAccepted(LoadControlData loadControlData) {
      // TODO Auto-generated method stub

   }

}
