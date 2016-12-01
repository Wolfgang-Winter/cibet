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
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * Monitors thread contention. Supports alarm and shed.
 * 
 */
public class ThreadContentionMonitor extends AbstractMonitor {

   private static Log log = LogFactory.getLog(ThreadContentionMonitor.class);

   /**
   * 
   */
   private static final long serialVersionUID = 1L;
   private static final String BLOCKEDTIME_START = "__BLOCKEDTIME_START";

   private Map<String, AtomicLong> totalBlockedTime = new HashMap<String, AtomicLong>();
   private volatile Map<String, Long> currentBlockedTime = new HashMap<String, Long>();
   private Map<String, AtomicLong> acceptedCount = new HashMap<String, AtomicLong>();

   private volatile Map<String, long[]> thresholdExceeded = new HashMap<String, long[]>();

   /**
    * time span in which requests are shed after the threshold is exceeded. The value can be given absolute or relative.
    * If absolute it is given in ms. If relative, it is in % of the average response time and the value must end with %.
    * Default is 1000ms
    */
   private String shedTime = "1000";
   private double shedTimePercent = -1;
   private double shedTimeAbs = 1000;

   /**
    * Allowed exceed of the average blocked time. The value can be given absolute or relative. If absolute it is given
    * in ms. If relative, it is in % of the average blocked time and the value must end with %.
    */
   private String shedThreshold = "-1";
   private double shedThresholdPercent = -1;
   private double shedThresholdAbs = -1;

   private String alarmThreshold = "-1";
   private double alarmThresholdPercent = -1;
   private double alarmThresholdAbs = -1;
   private Map<String, AtomicBoolean> alarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> alarmThresholdCount = new HashMap<>();

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON)
         return previousResult;

      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      ThreadInfo threadInfo = threadBean.getThreadInfo(Thread.currentThread().getId());
      metadata.getProperties().put(BLOCKEDTIME_START, threadInfo.getBlockedTime());

      if (shedTimeAbs > 0) {
         long startTime = System.currentTimeMillis();
         if (thresholdExceeded.get(currentSetpointId)[0] + shedTimeAbs > startTime) {
            shed(callback, metadata, currentSetpointId);
            return MonitorResult.SHED;
         }

      } else if (shedTimePercent > 0) {
         long startTime = System.currentTimeMillis();
         long timespan = Math.round(shedTimePercent * getAverageBlockedTime(currentSetpointId));
         if (thresholdExceeded.get(currentSetpointId)[0] + timespan > startTime) {
            shed(callback, metadata, currentSetpointId);
            return MonitorResult.SHED;
         }
      }
      return MonitorResult.PASSED;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String setpointId) {
      if (status != MonitorStatus.ON)
         return;

      Object obj = metadata.getProperties().get(BLOCKEDTIME_START);
      if (metadata.getExecutionStatus() == ExecutionStatus.SHED || obj == null) {
         currentBlockedTime.put(setpointId, 0L);
         return;
      }
      ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
      ThreadInfo threadInfo = threadBean.getThreadInfo(Thread.currentThread().getId());
      long blockedTime = threadInfo.getBlockedTime() - (long) obj;

      currentBlockedTime.put(setpointId, blockedTime);

      long actualTotalBlockedTime = totalBlockedTime.get(setpointId).addAndGet(blockedTime);
      long currentAccepted = acceptedCount.get(setpointId).incrementAndGet();

      if (currentAccepted < LEARNING_PHASE) {
         return;
      }

      int averageBlockTime = -1;
      if (shedThresholdPercent > 0) {
         averageBlockTime = (int) (actualTotalBlockedTime / currentAccepted);
         if (blockedTime > shedThresholdPercent * averageBlockTime) {
            thresholdExceeded.put(setpointId, new long[] { System.currentTimeMillis(), blockedTime });
         }

      } else if (shedThresholdAbs > 0) {
         if (blockedTime > shedThresholdAbs) {
            thresholdExceeded.put(setpointId, new long[] { System.currentTimeMillis(), blockedTime });
         }
      }

      if (callback == null)
         return;

      if (alarmThresholdPercent > 0) {
         if (averageBlockTime == -1) {
            averageBlockTime = (int) (actualTotalBlockedTime / currentAccepted);
         }
         if (blockedTime > alarmThresholdPercent * averageBlockTime) {
            long counter;
            if (alarm.get(setpointId).compareAndSet(false, true)) {
               counter = alarmThresholdCount.get(setpointId).incrementAndGet();
            } else {
               counter = alarmThresholdCount.get(setpointId).get();
            }
            alarm(blockedTime, counter, callback, metadata, setpointId);

         } else {
            alarm.get(setpointId).set(false);
         }

      } else if (alarmThresholdAbs > 0) {
         if (blockedTime > alarmThresholdAbs) {
            long counter;
            if (alarm.get(setpointId).compareAndSet(false, true)) {
               counter = alarmThresholdCount.get(setpointId).incrementAndGet();
            } else {
               counter = alarmThresholdCount.get(setpointId).get();
            }
            alarm(blockedTime, counter, callback, metadata, setpointId);

         } else {
            alarm.get(setpointId).set(false);
         }
      }
   }

   @Override
   public void reset(String setpointId) {
      enableThreadContentionInfo();
      totalBlockedTime.put(setpointId, new AtomicLong(0));
      acceptedCount.put(setpointId, new AtomicLong(0));
      thresholdExceeded.put(setpointId, new long[] { -1L, 0 });
      currentBlockedTime.put(setpointId, -1L);
      alarm.put(setpointId, new AtomicBoolean(false));
      alarmThresholdCount.put(setpointId, new AtomicLong(0));
   }

   @Override
   public void setStatus(MonitorStatus mode) {
      super.setStatus(mode);
      if (status == MonitorStatus.OFF) {
         disableThreadContentionInfo();
      } else {
         enableThreadContentionInfo();
      }
   }

   private void alarm(long duration, long counter, LoadControlCallback callback, EventMetadata metadata,
         String setpointId) {
      LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(), metadata.getControlEvent(),
            getName(), MonitorResult.ALARM);
      lcdata.setAlarmCount(counter);
      lcdata.setMonitoredValue(getName());
      lcdata.setThreshold(alarmThreshold);
      lcdata.setValue(String.valueOf(duration));
      callback.onAlarm(lcdata);
   }

   private void shed(LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
      if (callback != null) {
         LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
               metadata.getControlEvent(), getName(), MonitorResult.SHED);
         lcdata.setMonitoredValue(getName());
         lcdata.setThreshold(shedThreshold);
         lcdata.setValue(String.valueOf(thresholdExceeded.get(currentSetpointId)[1]));
         callback.onShed(lcdata);
      }
   }

   private void enableThreadContentionInfo() {
      if (status != MonitorStatus.OFF && status != MonitorStatus.NOT_SUPPORTED) {
         ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
         if (threadBean.isThreadContentionMonitoringSupported()) {
            threadBean.setThreadContentionMonitoringEnabled(true);
         } else {
            status = MonitorStatus.NOT_SUPPORTED;
         }
      }
   }

   private void disableThreadContentionInfo() {
      if (status != MonitorStatus.NOT_SUPPORTED) {
         ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
         threadBean.setThreadContentionMonitoringEnabled(false);
      }
   }

   /**
    * @return the shedThreshold
    */
   public String getShedThreshold() {
      return shedThreshold;
   }

   /**
    * @param thresh
    *           the shedThreshold to set
    */
   public void setShedThreshold(String thresh) {
      resolveValue(this, "shedThreshold", thresh);
   }

   /**
    * @return the shedTime
    */
   public String getShedTime() {
      return shedTime;
   }

   /**
    * @param time
    *           the shedTime to set
    */
   public void setShedTime(String time) {
      resolveValue(this, "shedTime", time);
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the currentBlockedTime for the given setpointId
    */
   public long getCurrentBlockedTime(String setpointId) {
      return currentBlockedTime.get(setpointId);
   }

   public long getAverageBlockedTime(String setpointId) {
      return acceptedCount.get(setpointId).get() == 0 ? 0
            : (long) (totalBlockedTime.get(setpointId).get() / acceptedCount.get(setpointId).get());
   }

   /**
    * @return the alarmThreshold
    */
   public String getAlarmThreshold() {
      return alarmThreshold;
   }

   /**
    * @param thresh
    *           the alarmThreshold to set
    */
   public void setAlarmThreshold(String thresh) {
      resolveValue(this, "alarmThreshold", thresh);
   }

}
