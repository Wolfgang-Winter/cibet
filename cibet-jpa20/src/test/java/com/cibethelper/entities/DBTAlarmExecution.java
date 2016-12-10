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
package com.cibethelper.entities;

import org.apache.log4j.Logger;

import com.logitags.cibet.actuator.loadcontrol.DB_ThroughputMonitorTest;
import com.logitags.cibet.actuator.loadcontrol.LoadControlCallback;
import com.logitags.cibet.actuator.loadcontrol.LoadControlData;
import com.logitags.cibet.actuator.loadcontrol.ThroughputMonitor;

public class DBTAlarmExecution implements LoadControlCallback {

   private static Logger log = Logger.getLogger(DBTAlarmExecution.class);

   @Override
   public void onShed(LoadControlData data) {
      if (ThroughputMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThroughputMonitor on Shed called: " + data);
         DB_ThroughputMonitorTest.shedCounter.incrementAndGet();
      }
   }

   @Override
   public void onAlarm(LoadControlData data) {
      if (ThroughputMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThroughputMonitor on Alarm called: " + data);
         DB_ThroughputMonitorTest.alarmCounter.incrementAndGet();
      }
   }

   @Override
   public void onThrottled(LoadControlData data) {
      if (ThroughputMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThroughputMonitor on Valve called: " + data);
         DB_ThroughputMonitorTest.throttleCounter.incrementAndGet();
      }
   }

   @Override
   public void onAccepted(LoadControlData loadControlData) {
      // TODO Auto-generated method stub

   }

}
