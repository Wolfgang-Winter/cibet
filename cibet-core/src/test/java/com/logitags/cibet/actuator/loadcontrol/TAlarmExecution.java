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
package com.logitags.cibet.actuator.loadcontrol;

import org.apache.log4j.Logger;

public class TAlarmExecution implements LoadControlCallback {

   private static Logger log = Logger.getLogger(TAlarmExecution.class);

   @Override
   public void onShed(LoadControlData data) {
      if (CpuLoadMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("CpuLoadMonitorTest onShed called: " + data);
         CpuLoadMonitorTest.shedCounter.incrementAndGet();
      } else if (ResponseTimeMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ResponseTimeMonitor on Shed called: " + data);
         ResponseTimeMonitorTest.shedCounter.incrementAndGet();
      } else if (MemoryMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("MemoryMonitor on Shed called: " + data);
         MemoryMonitorTest.shedCounter.incrementAndGet();
         MemoryMonitorTest.data = data;
      } else if (ThreadContentionMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThreadContentionMonitor on Shed called: " + data);
         ThreadContentionMonitorTest.counter.incrementAndGet();
      } else if (ThreadCountMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThreadCountMonitor on Shed called: " + data);
         ThreadCountMonitorTest.shed.incrementAndGet();
      } else if (ThroughputMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThroughputMonitor on Shed called: " + data);
         ThroughputMonitorTest.shedCounter.incrementAndGet();
      }
   }

   @Override
   public void onAlarm(LoadControlData data) {

      if (CpuLoadMonitor.MONITORVALUE_PROCESSCPU.equals(data.getMonitoredValue())) {
         log.warn(CpuLoadMonitor.MONITORVALUE_PROCESSCPU + " onAlarm called");
         CpuLoadMonitorTest.processCounter.incrementAndGet();

      } else if (CpuLoadMonitor.MONITORVALUE_SYSTEMCPU.equals(data.getMonitoredValue())) {
         log.warn(CpuLoadMonitor.MONITORVALUE_SYSTEMCPU + " onAlarm called");
         CpuLoadMonitorTest.systemCounter.incrementAndGet();

      } else if (ResponseTimeMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ResponseTimeMonitor on Alarm called: " + data);
         ResponseTimeMonitorTest.counter.incrementAndGet();
         ResponseTimeMonitorTest.thresholdExceed.set((int) data.getAlarmCount());

      } else if (ThreadContentionMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThreadContentionMonitor on Alarm called: " + data);
         ThreadContentionMonitorTest.counter.incrementAndGet();
      } else if (MemoryMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("MemoryMonitor onAlarm called: " + data);
         MemoryMonitorTest.valveCounter.incrementAndGet();
      } else if (ThreadTimeMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThreadTimeMonitor onAlarm called: " + data);
         ThreadTimeMonitorTest.counter.incrementAndGet();
      } else if (ThreadCountMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThreadCountMonitor on Alarm called: " + data);
         ThreadCountMonitorTest.counter.incrementAndGet();
      } else if (ThroughputMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThroughputMonitor on Alarm called: " + data);
         ThroughputMonitorTest.alarmCounter.incrementAndGet();
      }
   }

   @Override
   public void onThrottled(LoadControlData data) {
      if (CpuLoadMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("CpuLoadMonitor onThrottled called: " + data);
         CpuLoadMonitorTest.processCounter.incrementAndGet();
      } else if (MemoryMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("MemoryMonitor onThrottled called: " + data);
         MemoryMonitorTest.valveCounter.incrementAndGet();
      } else if (ThreadCountMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThreadCountMonitor on Valve called: " + data);
         ThreadCountMonitorTest.counter.incrementAndGet();
      } else if (ThroughputMonitor.class.getSimpleName().equals(data.getMonitor())) {
         log.warn("ThroughputMonitor on Valve called: " + data);
         ThroughputMonitorTest.throttleCounter.incrementAndGet();
      }
   }

   @Override
   public void onAccepted(LoadControlData loadControlData) {
      // TODO Auto-generated method stub

   }

}
