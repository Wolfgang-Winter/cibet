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
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * monitors thread cpu time and thread user time. Supports alarm.
 * 
 */
public class ThreadTimeMonitor extends AbstractMonitor {

   private static Log log = LogFactory.getLog(ThreadTimeMonitor.class);

   /**
   * 
   */
   private static final long serialVersionUID = 1L;
   private static final String THREAD_USERTIME_START = "__THREAD_USERTIME_START";
   private static final String THREAD_CPUTIME_START = "__THREAD_CPUTIME_START";

   /**
    * Number of samples for thread time monitor.
    */
   private Map<String, AtomicLong> acceptedThreadTimeCount = new HashMap<String, AtomicLong>();

   /**
    * Total time of the thread cpu time.
    */
   private Map<String, AtomicLong> totalThreadCpuTime = new HashMap<String, AtomicLong>();

   /**
    * Total time of the thread user time.
    */
   private Map<String, AtomicLong> totalThreadUserTime = new HashMap<String, AtomicLong>();

   private volatile Map<String, Long> currentThreadCpuTime = new HashMap<String, Long>();

   private volatile Map<String, Long> currentThreadUserTime = new HashMap<String, Long>();

   /**
    * Current minimum of the thread cpu time.
    */
   private Map<String, AtomicInteger> minimumThreadCpuTime = new HashMap<String, AtomicInteger>();

   /**
    * Current maximum of the thread cpu time.
    */
   private Map<String, AtomicInteger> maximumThreadCpuTime = new HashMap<String, AtomicInteger>();

   /**
    * Current maximum of the thread user time.
    */
   private Map<String, AtomicInteger> maximumThreadUserTime = new HashMap<String, AtomicInteger>();

   /**
    * Current minimum of the thread user time.
    */
   private Map<String, AtomicInteger> minimumThreadUserTime = new HashMap<String, AtomicInteger>();

   private String alarmCpuTimeThreshold = "-1";
   private double alarmCpuTimeThresholdPercent = -1;
   private double alarmCpuTimeThresholdAbs = -1;
   private Map<String, AtomicBoolean> alarmCpuTime = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> alarmCpuTimeCount = new HashMap<>();

   private String alarmUserTimeThreshold = "-1";
   private double alarmUserTimeThresholdPercent = -1;
   private double alarmUserTimeThresholdAbs = -1;
   private Map<String, AtomicBoolean> alarmUserTime = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> alarmUserTimeCount = new HashMap<>();

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback alarm, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status == MonitorStatus.OFF
            || status == MonitorStatus.NOT_SUPPORTED) {
         return previousResult;
      }

      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      metadata.getProperties().put(THREAD_USERTIME_START, threadBean.getCurrentThreadUserTime());
      metadata.getProperties().put(THREAD_CPUTIME_START, threadBean.getCurrentThreadCpuTime());
      return MonitorResult.PASSED;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String spId) {
      if (metadata.getExecutionStatus() == ExecutionStatus.SHED) {
         return;
      }

      Object stUserTime = metadata.getProperties().get(THREAD_USERTIME_START);
      if (stUserTime == null) {
         return;
      }
      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      long userDuration = (threadBean.getCurrentThreadUserTime() - (long) stUserTime) / 1000000;
      long cpuDuration = (threadBean.getCurrentThreadCpuTime()
            - (long) metadata.getProperties().get(THREAD_CPUTIME_START)) / 1000000;

      currentThreadUserTime.put(spId, userDuration);
      currentThreadCpuTime.put(spId, cpuDuration);

      long currentAccepted = acceptedThreadTimeCount.get(spId).incrementAndGet();

      if (maximumThreadCpuTime.get(spId).get() < cpuDuration) {
         maximumThreadCpuTime.get(spId).set((int) cpuDuration);
      }
      if (minimumThreadCpuTime.get(spId).get() > cpuDuration || minimumThreadCpuTime.get(spId).get() == -1) {
         minimumThreadCpuTime.get(spId).set((int) cpuDuration);
      }

      totalThreadCpuTime.get(spId).addAndGet(cpuDuration);

      if (maximumThreadUserTime.get(spId).get() < userDuration) {
         maximumThreadUserTime.get(spId).set((int) userDuration);
      }
      if (minimumThreadUserTime.get(spId).get() > userDuration || minimumThreadUserTime.get(spId).get() == -1) {
         minimumThreadUserTime.get(spId).set((int) userDuration);
      }

      totalThreadUserTime.get(spId).addAndGet(userDuration);

      if (callback == null)
         return;
      if (currentAccepted < LEARNING_PHASE) {
         return;
      }

      if (alarmCpuTimeThresholdPercent > 0) {
         if (cpuDuration > alarmCpuTimeThresholdPercent * getAverageThreadCpuTime(spId)) {
            long counter;
            if (alarmCpuTime.get(spId).compareAndSet(false, true)) {
               counter = alarmCpuTimeCount.get(spId).incrementAndGet();
            } else {
               counter = alarmCpuTimeCount.get(spId).get();
            }
            alarm(cpuDuration, counter, callback, metadata, spId, alarmCpuTimeThreshold);

         } else {
            alarmCpuTime.get(spId).set(false);
         }

      } else if (alarmCpuTimeThresholdAbs > 0) {
         if (cpuDuration > alarmCpuTimeThresholdAbs) {
            long counter;
            if (alarmCpuTime.get(spId).compareAndSet(false, true)) {
               counter = alarmCpuTimeCount.get(spId).incrementAndGet();
            } else {
               counter = alarmCpuTimeCount.get(spId).get();
            }
            alarm(cpuDuration, counter, callback, metadata, spId, alarmCpuTimeThreshold);

         } else {
            alarmCpuTime.get(spId).set(false);
         }
      }

      if (alarmUserTimeThresholdPercent > 0) {
         if (userDuration > alarmUserTimeThresholdPercent * getAverageThreadUserTime(spId)) {
            long counter;
            if (alarmUserTime.get(spId).compareAndSet(false, true)) {
               counter = alarmUserTimeCount.get(spId).incrementAndGet();
            } else {
               counter = alarmUserTimeCount.get(spId).get();
            }
            alarm(userDuration, counter, callback, metadata, spId, alarmUserTimeThreshold);

         } else {
            alarmUserTime.get(spId).set(false);
         }

      } else if (alarmUserTimeThresholdAbs > 0) {
         if (userDuration > alarmUserTimeThresholdAbs) {
            long counter;
            if (alarmUserTime.get(spId).compareAndSet(false, true)) {
               counter = alarmUserTimeCount.get(spId).incrementAndGet();
            } else {
               counter = alarmUserTimeCount.get(spId).get();
            }
            alarm(userDuration, counter, callback, metadata, spId, alarmUserTimeThreshold);

         } else {
            alarmUserTime.get(spId).set(false);
         }
      }
   }

   private void alarm(long duration, long counter, LoadControlCallback callback, EventMetadata metadata,
         String setpointId, String threshold) {
      LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(), metadata.getControlEvent(),
            getName(), MonitorResult.ALARM);
      lcdata.setAlarmCount(counter);
      lcdata.setMonitoredValue(getName());
      lcdata.setThreshold(threshold);
      lcdata.setValue(String.valueOf(duration));
      callback.onAlarm(lcdata);
   }

   @Override
   public void reset(String setpointId) {
      enableThreadTimeInfo();
      totalThreadCpuTime.put(setpointId, new AtomicLong(0));
      totalThreadUserTime.put(setpointId, new AtomicLong(0));
      acceptedThreadTimeCount.put(setpointId, new AtomicLong(0));
      minimumThreadCpuTime.put(setpointId, new AtomicInteger(-1));
      minimumThreadUserTime.put(setpointId, new AtomicInteger(-1));
      maximumThreadCpuTime.put(setpointId, new AtomicInteger(-1));
      maximumThreadUserTime.put(setpointId, new AtomicInteger(-1));
      currentThreadCpuTime.put(setpointId, -1L);
      currentThreadUserTime.put(setpointId, -1L);
      alarmCpuTime.put(setpointId, new AtomicBoolean(false));
      alarmCpuTimeCount.put(setpointId, new AtomicLong(0));
      alarmUserTime.put(setpointId, new AtomicBoolean(false));
      alarmUserTimeCount.put(setpointId, new AtomicLong(0));
   }

   @Override
   public void close() {
      disableThreadTimeInfo();
   }

   private void enableThreadTimeInfo() {
      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      if (!threadBean.isCurrentThreadCpuTimeSupported()) {
         status = MonitorStatus.NOT_SUPPORTED;
      }

      if (status == MonitorStatus.ON) {
         threadBean.setThreadCpuTimeEnabled(true);
      }
   }

   private void disableThreadTimeInfo() {
      if (status != MonitorStatus.NOT_SUPPORTED) {
         ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
         threadBean.setThreadCpuTimeEnabled(false);
      }
   }

   @Override
   public void setStatus(MonitorStatus mode) {
      super.setStatus(mode);
      if (status == MonitorStatus.OFF) {
         disableThreadTimeInfo();
      } else {
         enableThreadTimeInfo();
      }
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the minimumThreadCpuTime for the given setpointId
    */
   public int getMinimumThreadCpuTime(String setpointId) {
      AtomicInteger at = minimumThreadCpuTime.get(setpointId);
      return at == null ? -1 : at.get();
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the maximumThreadCpuTime for the given setpointId
    */
   public int getMaximumThreadCpuTime(String setpointId) {
      AtomicInteger at = maximumThreadCpuTime.get(setpointId);
      return at == null ? -1 : at.get();
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the averageThreadCpuTime for the given setpointId
    */
   public int getAverageThreadCpuTime(String setpointId) {
      AtomicLong at = acceptedThreadTimeCount.get(setpointId);
      if (at == null) {
         return -1;
      } else {
         return at.get() == 0 ? 0 : (int) (totalThreadCpuTime.get(setpointId).get() / at.get());
      }
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the minimumThreadUserTime for the given setpointId
    */
   public int getMinimumThreadUserTime(String setpointId) {
      AtomicInteger at = minimumThreadUserTime.get(setpointId);
      return at == null ? -1 : at.get();
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the maximumThreadUserTime for the given setpointId
    */
   public int getMaximumThreadUserTime(String setpointId) {
      AtomicInteger at = maximumThreadUserTime.get(setpointId);
      return at == null ? -1 : at.get();
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the averageThreadUserTime for the given setpointId
    */
   public int getAverageThreadUserTime(String setpointId) {
      AtomicLong at = acceptedThreadTimeCount.get(setpointId);
      if (at == null) {
         return -1;
      } else {
         return at.get() == 0 ? 0 : (int) (totalThreadUserTime.get(setpointId).get() / at.get());
      }
   }

   /**
    * @return the alarmCpuTimeThreshold
    */
   public String getAlarmCpuTimeThreshold() {
      return alarmCpuTimeThreshold;
   }

   /**
    * @param thresh
    *           the alarmCpuTimeThreshold to set
    */
   public void setAlarmCpuTimeThreshold(String thresh) {
      resolveValue(this, "alarmCpuTimeThreshold", thresh);
   }

   /**
    * @return the alarmUserTimeThreshold
    */
   public String getAlarmUserTimeThreshold() {
      return alarmUserTimeThreshold;
   }

   /**
    * @param thresh
    *           the alarmUserTimeThreshold to set
    */
   public void setAlarmUserTimeThreshold(String thresh) {
      resolveValue(this, "alarmUserTimeThreshold", thresh);
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the currentThreadCpuTime for the given setpointId
    */
   public long getCurrentThreadCpuTime(String setpointId) {
      Long l = currentThreadCpuTime.get(setpointId);
      return l == null ? -1 : l;
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the currentThreadUserTime for the given setpointId
    */
   public long getCurrentThreadUserTime(String setpointId) {
      Long l = currentThreadUserTime.get(setpointId);
      return l == null ? -1 : l;
   }

}
