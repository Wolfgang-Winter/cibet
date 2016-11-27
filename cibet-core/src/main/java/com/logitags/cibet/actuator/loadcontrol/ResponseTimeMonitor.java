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
 * Monitors response time. Supports alarm and shed.
 * 
 */
public class ResponseTimeMonitor extends AbstractMonitor {

   private static Log log = LogFactory.getLog(ResponseTimeMonitor.class);

   /**
   * 
   */
   private static final long serialVersionUID = 1L;
   private static final String RESPONSETIME_START = "__RESPONSETIME_START";

   /**
    * Timestamp in ms when the response time exceeded the threshold.
    */
   private volatile Map<String, long[]> thresholdExceeded = new HashMap<String, long[]>();

   /**
    * time span in which requests are shed after the threshold is exceeded. The
    * value can be given absolute or relative. If absolute it is given in ms. If
    * relative, it is in % of the average response time and the value must end
    * with %. Default is 1000ms
    */
   private String shedTime = "1000";
   private double shedTimePercent = -1;

   /**
    * shed time in ms
    */
   private double shedTimeAbs = 1000;

   /**
    * Allowed exceed of the average response time. The value can be given
    * absolute or relative. If absolute it is given in ms. If relative, it is in
    * % of the average response time and the value must end with %.
    */
   private String shedThreshold = "-1";
   private double shedThresholdPercent = -1;
   private double shedThresholdAbs = -1;

   private volatile Map<String, Long> currentResponseTime = new HashMap<String, Long>();

   private String alarmThreshold = "-1";
   private double alarmThresholdPercent = -1;
   private double alarmThresholdAbs = -1;
   private Map<String, AtomicBoolean> alarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> alarmThresholdCount = new HashMap<>();

   /**
    * Current minimum of the response time.
    */
   private Map<String, AtomicInteger> minimumResponseTime = new HashMap<String, AtomicInteger>();

   /**
    * Current maximum of the response time.
    */
   private Map<String, AtomicInteger> maximumResponseTime = new HashMap<String, AtomicInteger>();

   /**
    * Total of the response time.
    */
   private Map<String, AtomicLong> totalResponseTime = new HashMap<String, AtomicLong>();

   /**
    * Number of samples for response time monitor.
    */
   private Map<String, AtomicLong> acceptedCount = new HashMap<String, AtomicLong>();

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON)
         return previousResult;

      long startTime = System.currentTimeMillis();
      metadata.getProperties().put(RESPONSETIME_START, startTime);

      if (shedTimeAbs > 0) {
         if (thresholdExceeded.get(currentSetpointId)[0] + shedTimeAbs > startTime) {
            shed(callback, metadata, currentSetpointId);
            return MonitorResult.SHED;
         }

      } else if (shedTimePercent > 0) {
         double timespan = shedTimePercent * getAverageResponseTime(currentSetpointId);
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

      Object responseTimeStart = metadata.getProperties().get(RESPONSETIME_START);
      if (metadata.getExecutionStatus() == ExecutionStatus.SHED || responseTimeStart == null) {
         currentResponseTime.put(setpointId, 0L);
         return;
      }

      long startTime = (long) responseTimeStart;
      long endTime = System.currentTimeMillis();
      long duration = endTime - startTime;

      currentResponseTime.put(setpointId, duration);

      if (maximumResponseTime.get(setpointId).get() < duration) {
         maximumResponseTime.get(setpointId).set((int) duration);
      }
      if (minimumResponseTime.get(setpointId).get() > duration || minimumResponseTime.get(setpointId).get() == -1) {
         minimumResponseTime.get(setpointId).set((int) duration);
      }

      long actualTotalResponseTime = totalResponseTime.get(setpointId).addAndGet(duration);
      long currentAccepted = acceptedCount.get(setpointId).incrementAndGet();

      if (currentAccepted < LEARNING_PHASE) {
         return;
      }

      int averageResponseTime = -1;
      if (shedThresholdPercent > 0) {
         averageResponseTime = (int) (actualTotalResponseTime / currentAccepted);
         if (duration > shedThresholdPercent * averageResponseTime) {
            thresholdExceeded.put(setpointId, new long[] { endTime, duration });
         }

      } else if (shedThresholdAbs > 0) {
         if (duration > shedThresholdAbs) {
            thresholdExceeded.put(setpointId, new long[] { endTime, duration });
         }
      }

      if (callback == null)
         return;

      if (alarmThresholdPercent > 0) {
         if (averageResponseTime == -1) {
            averageResponseTime = (int) (actualTotalResponseTime / currentAccepted);
         }
         if (duration > alarmThresholdPercent * averageResponseTime) {
            long counter;
            if (alarm.get(setpointId).compareAndSet(false, true)) {
               counter = alarmThresholdCount.get(setpointId).incrementAndGet();
            } else {
               counter = alarmThresholdCount.get(setpointId).get();
            }

            alarm(duration, counter, callback, metadata, setpointId);

         } else {
            alarm.get(setpointId).set(false);
         }

      } else if (alarmThresholdAbs > 0) {
         if (duration > alarmThresholdAbs) {
            long counter;
            if (alarm.get(setpointId).compareAndSet(false, true)) {
               counter = alarmThresholdCount.get(setpointId).incrementAndGet();
            } else {
               counter = alarmThresholdCount.get(setpointId).get();
            }
            alarm(duration, counter, callback, metadata, setpointId);

         } else {
            alarm.get(setpointId).set(false);
         }
      }
   }

   @Override
   public void reset(String setpointId) {
      maximumResponseTime.put(setpointId, new AtomicInteger(-1));
      minimumResponseTime.put(setpointId, new AtomicInteger(-1));
      totalResponseTime.put(setpointId, new AtomicLong(0));
      acceptedCount.put(setpointId, new AtomicLong(0));
      currentResponseTime.put(setpointId, -1L);
      thresholdExceeded.put(setpointId, new long[] { -1L, 0 });
      alarm.put(setpointId, new AtomicBoolean(false));
      alarmThresholdCount.put(setpointId, new AtomicLong(0));
   }

   private void shed(LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
      if (callback != null) {
         LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
               metadata.getControlEvent(), getName());
         lcdata.setMonitoredValue(getName());
         lcdata.setThreshold(shedThreshold);
         lcdata.setValue(String.valueOf(thresholdExceeded.get(currentSetpointId)[1]));
         callback.onShed(lcdata);
      }
   }

   private void alarm(long duration, long counter, LoadControlCallback callback, EventMetadata metadata,
         String setpointId) {
      LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(), metadata.getControlEvent(),
            getName());
      lcdata.setAlarmCount(counter);
      lcdata.setMonitoredValue(getName());
      lcdata.setThreshold(alarmThreshold);
      lcdata.setValue(String.valueOf(duration));
      callback.onAlarm(lcdata);
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the currentResponseTime for the given setpointId
    */
   public long getCurrentResponseTime(String setpointId) {
      Long l = currentResponseTime.get(setpointId);
      return l == null ? -1 : l;
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
    * @return the minimumResponseTime for the given setpointId
    */
   public AtomicInteger getMinimumResponseTime(String setpointId) {
      return minimumResponseTime.get(setpointId);
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the maximumResponseTime for the given setpointId
    */
   public AtomicInteger getMaximumResponseTime(String setpointId) {
      return maximumResponseTime.get(setpointId);
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the averageResponseTime for the given setpointId
    */
   public int getAverageResponseTime(String setpointId) {
      return acceptedCount.get(setpointId).get() == 0 ? 0
            : (int) (totalResponseTime.get(setpointId).get() / acceptedCount.get(setpointId).get());
   }

}
