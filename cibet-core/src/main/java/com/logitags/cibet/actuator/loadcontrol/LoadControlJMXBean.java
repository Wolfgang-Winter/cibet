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

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;
import com.udojava.jmx.wrapper.JMXBeanOperation;

@JMXBean(description = "JMX bean for managing LoadControlActuator", sorted = true)
public class LoadControlJMXBean {

   private static Log log = LogFactory.getLog(LoadControlJMXBean.class);

   private LoadControlActuator actuator;

   private String setpointId;

   public LoadControlJMXBean(LoadControlActuator act, String spId) {
      actuator = act;
      setpointId = spId;
   }

   @JMXBeanOperation(name = "Reset", description = "resets all monitors and general counters")
   public void resetAll() {
      resetAcceptCounter();
      resetThroughputMonitor();
      resetResponseTime();
      resetThreadTime();
      resetThreadContentionTime();
      resetCpuLoadMonitor();
      resetMemoryMonitor();
      resetThreadCountMonitor();
      resetFileDescriptorMonitor();
      for (Monitor custom : actuator.getCustomMonitors()) {
         custom.reset(setpointId);
      }
   }

   @JMXBeanOperation(name = "Reset All General Counters", description = "resets counters of accepted and shed requests and timestamps")
   public void resetAcceptCounter() {
      actuator.resetAcceptCounter(setpointId);
   }

   @JMXBeanOperation(name = "Reset ThroughputMonitor", description = "resets ThroughputMonitor")
   public void resetThroughputMonitor() {
      actuator.getThroughputMonitor().reset(setpointId);
   }

   @JMXBeanOperation(name = "Reset CpuLoadMonitor", description = "resets CpuLoadMonitor")
   public void resetCpuLoadMonitor() {
      actuator.getCpuLoadMonitor().reset(setpointId);
   }

   @JMXBeanOperation(name = "Reset FileDescriptorMonitor", description = "resets counters")
   public void resetFileDescriptorMonitor() {
      actuator.getFileDescriptorMonitor().reset(setpointId);
   }

   @JMXBeanOperation(name = "Reset MemoryMonitor", description = "resets MemoryMonitor")
   public void resetMemoryMonitor() {
      actuator.getMemoryMonitor().reset(setpointId);
   }

   @JMXBeanOperation(name = "Reset ResponseTimeMonitor", description = "resets response time counters")
   public void resetResponseTime() {
      actuator.getResponseTimeMonitor().reset(setpointId);
   }

   @JMXBeanOperation(name = "Reset ThreadContentionMonitor", description = "resets ThreadContentionMonitor blocked time counters")
   public void resetThreadContentionTime() {
      actuator.getThreadContentionMonitor().reset(setpointId);
   }

   @JMXBeanOperation(name = "Reset ThreadCountMonitor", description = "resets ThreadCountMonitor")
   public void resetThreadCountMonitor() {
      actuator.getThreadCountMonitor().reset(setpointId);
   }

   @JMXBeanOperation(name = "Reset ThreadTimeMonitor", description = "resets thread cpu time and thread user time counters")
   public void resetThreadTime() {
      actuator.getThreadTimeMonitor().reset(setpointId);
   }

   /**
    * @return the actuatorName
    */
   @JMXBeanAttribute(name = "A1. Actuator Name", description = "Name of the actuator. Default is LOADCONTROL", sortValue = "a1")
   public String getActuatorName() {
      return actuator.getName();
   }

   /**
    * @return the setpointId
    */
   @JMXBeanAttribute(name = "A2. Setpoint Id", description = "Id of the Setpoint which this JMX bean is monitoring", sortValue = "a2")
   public String getSetpointId() {
      return setpointId;
   }

   @JMXBeanAttribute(name = "A3. First Hit", description = "Timestamp of the first hit on this Setpoint", sortValue = "a3")
   public Date getFirstHitTime() {
      AtomicLong date = actuator.getFirstHitTime(setpointId);
      if (date != null) {
         return new Date(date.get());
      } else {
         return null;
      }
   }

   @JMXBeanAttribute(name = "A4. Last Hit", description = "Timestamp of the last recent hit on this Setpoint", sortValue = "a4")
   public Date getLastHitTime() {
      AtomicLong date = actuator.getLastHitTime(setpointId);
      if (date.get() != 0) {
         return new Date(date.get());
      } else {
         return null;
      }
   }

   @JMXBeanAttribute(name = "A5. Effective Hit Time", description = "Duration in ms between first and last hit on this Setpoint", sortValue = "a5")
   public long getEffectiveActiveTime() {
      AtomicLong first = actuator.getFirstHitTime(setpointId);
      AtomicLong last = actuator.getLastHitTime(setpointId);
      if (first == null || last.get() == 0) {
         return -1;
      } else {
         return last.get() - first.get();
      }
   }

   /**
    * @return the accepted
    */
   @JMXBeanAttribute(name = "A6. Accepted", description = "Total number of accepted requests", sortValue = "a6")
   public long getAccepted() {
      return actuator.getAccepted(setpointId).get();
   }

   /**
    * @return the denied
    */
   @JMXBeanAttribute(name = "A7. Shed", description = "Total number of shed requests", sortValue = "a7")
   public long getShed() {
      return actuator.getShed(setpointId).get();
   }

   @JMXBeanAttribute(name = "A8. Shed Ratio", description = "Ratio of shed requests in %", sortValue = "a8")
   public double getDeniedRatio() {
      long denied = getShed();
      double percent = 100d * denied / (getAccepted() + denied);
      return percent;
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ThrottleCount", description = "Current number of throttled threads", sortValue = "c01")
   public int getCpuLoadThrottleCount() {
      return actuator.getCpuLoadMonitor().getThrottleCount(setpointId);
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ThrottleCount", description = "Current number of throttled threads", sortValue = "c50")
   public int getFileDescriptorThrottleCount() {
      return actuator.getFileDescriptorMonitor().getThrottleCount(setpointId);
   }

   @JMXBeanAttribute(name = "MemoryMonitor.ThrottleCount", description = "Current number of throttled threads", sortValue = "e01")
   public int getMemoryThrottleCount() {
      return actuator.getMemoryMonitor().getThrottleCount(setpointId);
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.Average Response Time", description = "the average response time in ms", sortValue = "g01")
   public int getAverageResponseTime() {
      return actuator.getResponseTimeMonitor().getAverageResponseTime(setpointId);
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.Current Response Time", description = "the current response time in ms", sortValue = "g02")
   public long getResponseTime() {
      return actuator.getResponseTimeMonitor().getCurrentResponseTime(setpointId);
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.Maximum Response Time", description = "the maximum response time in ms", sortValue = "g03")
   public int getMaximumResponseTime() {
      return actuator.getResponseTimeMonitor().getMaximumResponseTime(setpointId).get();
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.Minimum Response Time", description = "the minimum response time in ms", sortValue = "g04")
   public int getMinimumResponseTime() {
      return actuator.getResponseTimeMonitor().getMinimumResponseTime(setpointId).get();
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.Average Blocked Time", description = "the average time in ms a thread is blocked by another thread", sortValue = "k01")
   public long getAverageBlockedTime() {
      return actuator.getThreadContentionMonitor().getAverageBlockedTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.Average Blocked Time Ratio (%)", description = "the average percentage of the response time that a thread is blocked", sortValue = "k02")
   public double getThreadBlockedTimeRatio() {
      return 100d * actuator.getThreadContentionMonitor().getAverageBlockedTime(setpointId)
            / actuator.getResponseTimeMonitor().getAverageResponseTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.Current Blocked Time", description = "the current time in ms a thread is blocked by another thread", sortValue = "k03")
   public long getCurrentBlockedTime() {
      return actuator.getThreadContentionMonitor().getCurrentBlockedTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ThreadCount", description = "Current number of parallel threads", sortValue = "m01")
   public int getThreadCountThreadCount() {
      return actuator.getThreadCountMonitor().getThreadCount(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ThrottleCount", description = "Current number of throttled threads", sortValue = "m02")
   public int getThreadCountThrottleCount() {
      return actuator.getThreadCountMonitor().getThrottleCount(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Average Thread CPU Time", description = "the average thread cpu time in ms that a thread has needed for execution", sortValue = "o01")
   public int getAverageThreadCpuTime() {
      return actuator.getThreadTimeMonitor().getAverageThreadCpuTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Average Thread User Time", description = "the average thread user time in ms that a thread has needed for execution", sortValue = "o02")
   public int getAverageThreadUserTime() {
      return actuator.getThreadTimeMonitor().getAverageThreadUserTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Current Thread CPU Time", description = "the current thread cpu time in ms that a thread has needed for execution", sortValue = "o03")
   public long getCurrentThreadCpuTime() {
      return actuator.getThreadTimeMonitor().getCurrentThreadCpuTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Current Thread User Time", description = "the current thread user time in ms that a thread has needed for execution", sortValue = "o04")
   public long getCurrentThreadUserTime() {
      return actuator.getThreadTimeMonitor().getCurrentThreadUserTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Maximum Thread CPU Time", description = "the maximum thread cpu time in ms that a thread has needed for execution", sortValue = "o05")
   public int getMaximumThreadCpuTime() {
      return actuator.getThreadTimeMonitor().getMaximumThreadCpuTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Maximum Thread User Time", description = "the maximum thread user time in ms that a thread has needed for execution", sortValue = "o06")
   public int getMaximumThreadUserTime() {
      return actuator.getThreadTimeMonitor().getMaximumThreadUserTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Minimum Thread CPU Time", description = "the minimum thread cpu time in ms that a thread has needed for execution", sortValue = "o07")
   public int getMinimumThreadCpuTime() {
      return actuator.getThreadTimeMonitor().getMinimumThreadCpuTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Minimum Thread User Time", description = "the minimum thread user time in ms that a thread has needed for execution", sortValue = "o08")
   public int getMinimumThreadUserTime() {
      return actuator.getThreadTimeMonitor().getMinimumThreadUserTime(setpointId);
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.Throughput", description = "the throughput per time window in a sliding window", sortValue = "p01")
   public int getThroughput() {
      return actuator.getThroughputMonitor().getThroughput(setpointId);
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ThrottleCount", description = "Current number of throttled threads", sortValue = "p02")
   public int getContinuousThroughputThrottleCount() {
      return actuator.getThroughputMonitor().getThrottleCount(setpointId);
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.TotalThroughput", description = "Throughput as accepted requests per elapsed time in s", sortValue = "p03")
   public double getTotalThroughput() {
      return actuator.getThroughputMonitor().getTotalThroughput(setpointId);
   }

}
