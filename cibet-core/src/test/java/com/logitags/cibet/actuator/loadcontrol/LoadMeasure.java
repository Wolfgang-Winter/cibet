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

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class LoadMeasure extends Thread {

   private static Log log = LogFactory.getLog(LoadMeasure.class);

   private static double cpuUsage;

   private boolean active = true;

   public void run() {
      do {

         OperatingSystemMXBean operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory
               .getOperatingSystemMXBean();
         // cpuUsage = operatingSystemMXBean.getSystemLoadAverage();
         log.info(cpuUsage);

         RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
         int availableProcessors = operatingSystemMXBean.getAvailableProcessors();
         long prevUpTime = runtimeMXBean.getUptime();
         // long prevProcessCpuTime = operatingSystemMXBean.getProcessCpuTime();
         double cpuUsage;
         try {
            Thread.sleep(500);
         } catch (Exception ignored) {
         }

         operatingSystemMXBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
         long upTime = runtimeMXBean.getUptime();
         // long processCpuTime = operatingSystemMXBean.getProcessCpuTime();
         // long elapsedCpu = processCpuTime - prevProcessCpuTime;
         long elapsedTime = upTime - prevUpTime;

         // cpuUsage = Math.min(99F, elapsedCpu / (elapsedTime * 10000F * availableProcessors));

      } while (active == true);
   }

   /**
    * @return the active
    */
   public boolean isActive() {
      return active;
   }

   /**
    * @param active
    *           the active to set
    */
   public void setActive(boolean active) {
      this.active = active;
   }

}
