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

/**
 * Monitors thread count. Supports alarm, valve and shed.
 * 
 */
public class ThreadCountMonitor extends AbstractMonitor {

   /**
   * 
   */
   private static final long serialVersionUID = 1L;
   private static Log log = LogFactory.getLog(ThreadCountMonitor.class);

   /**
    * thread counter
    */
   private Map<String, AtomicInteger> threadCount = new HashMap<String, AtomicInteger>();

   /**
    * thread count monitor. Maximum number of threads
    */
   private int shedThreshold = -1;

   private int alarmThreshold = -1;
   private Map<String, AtomicBoolean> alarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> alarmThresholdCount = new HashMap<>();

   private int valveThreshold = -1;

   private long throttleMaxTime = 1000;
   private long throttleInterval = 200;

   private Map<String, AtomicInteger> throttleCount = new HashMap<String, AtomicInteger>();

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback callback, EventMetadata metadata,
         String setpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON)
         return previousResult;

      int counter = threadCount.get(setpointId).incrementAndGet();
      if (log.isDebugEnabled()) {
         log.debug(setpointId + " ThreadCount set before to " + counter);
      }

      if (shedThreshold > -1) {
         // healing
         if (counter > shedThreshold + 1) {
            boolean ok = threadCount.get(setpointId).compareAndSet(counter, counter - 1);
            if (ok) {
               log.warn("counter fixed to " + (counter - 1));
            } else {
               log.warn("FAILED to fix counter to " + (counter - 1));
            }
         }

         if (counter > shedThreshold) {
            shed(counter, callback, metadata, setpointId);
            return MonitorResult.SHED;
         }
      }

      MonitorResult result = tryValve(counter, callback, metadata, setpointId);
      if (result == MonitorResult.PASSED) {
         tryAlarm(counter, callback, metadata, setpointId);
      }
      return result;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String setpointId) {
      if (status != MonitorStatus.ON)
         return;

      int counter = threadCount.get(setpointId).decrementAndGet();
      if (log.isDebugEnabled()) {
         log.debug(setpointId + " ThreadCount set after to " + counter);
      }

      // healing
      if (counter < 0) {
         boolean ok = threadCount.get(setpointId).compareAndSet(counter, 0);
         if (ok) {
            log.warn("counter fixed to 0");
         } else {
            log.warn("FAILED to fix counter to 0");
         }
      }
   }

   private void shed(int counter, LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
      if (callback != null) {
         LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
               metadata.getControlEvent(), getName(), MonitorResult.SHED);
         lcdata.setMonitoredValue(getName());
         lcdata.setThreshold(String.valueOf(shedThreshold));
         lcdata.setValue(String.valueOf(counter));
         callback.onShed(lcdata);
      }
   }

   private MonitorResult tryValve(int counter, LoadControlCallback callback, EventMetadata metadata,
         String setpointId) {
      MonitorResult result = MonitorResult.PASSED;
      if (valveThreshold > -1) {
         long startTime = System.currentTimeMillis();
         long maxTime = startTime + throttleMaxTime;
         while (maxTime - System.currentTimeMillis() > 0) {
            if (counter > valveThreshold) {
               if (result == MonitorResult.PASSED) {
                  throttleCount.get(setpointId).incrementAndGet();
               }
               result = MonitorResult.THROTTLED;
               try {
                  Thread.sleep(throttleInterval);
               } catch (InterruptedException e) {
               }
               counter = threadCount.get(setpointId).get();

            } else {
               if (result == MonitorResult.THROTTLED) {
                  throttleCount.get(setpointId).decrementAndGet();
                  if (callback != null) {
                     LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(),
                           metadata.getControlEvent(), getName(), MonitorResult.THROTTLED);
                     lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
                     lcdata.setMonitoredValue(getName());
                     lcdata.setThreshold(String.valueOf(valveThreshold));
                     lcdata.setValue(String.valueOf(counter));
                     callback.onThrottled(lcdata);
                  }
               }
               return result;
            }
         }

         // shed after throttling
         throttleCount.get(setpointId).decrementAndGet();
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(), metadata.getControlEvent(),
                  getName(), MonitorResult.SHED);
            lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
            lcdata.setMonitoredValue(getName());
            lcdata.setThreshold(String.valueOf(valveThreshold));
            lcdata.setValue(String.valueOf(counter));
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;

      } else {
         return result;
      }
   }

   private void tryAlarm(int counter, LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
      if (alarmThreshold > -1 && callback != null) {
         if (counter > alarmThreshold) {
            long alarmcounter;
            if (alarm.get(currentSetpointId).compareAndSet(false, true)) {
               alarmcounter = alarmThresholdCount.get(currentSetpointId).incrementAndGet();
            } else {
               alarmcounter = alarmThresholdCount.get(currentSetpointId).get();
            }

            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName(), MonitorResult.ALARM);
            lcdata.setAlarmCount(alarmcounter);
            lcdata.setMonitoredValue(getName());
            lcdata.setThreshold(String.valueOf(alarmThreshold));
            lcdata.setValue(String.valueOf(counter));
            callback.onAlarm(lcdata);

         } else {
            alarm.get(currentSetpointId).set(false);
         }
      }
   }

   @Override
   public void reset(String setpointId) {
      threadCount.put(setpointId, new AtomicInteger(0));
      throttleCount.put(setpointId, new AtomicInteger(0));
      alarm.put(setpointId, new AtomicBoolean(false));
      alarmThresholdCount.put(setpointId, new AtomicLong(0));
   }

   /**
    * @param setpointId
    *           setpointId
    * @return the threads counter for the given setpointId
    */
   public int getThreadCount(String setpointId) {
      return threadCount.get(setpointId).get();
   }

   /**
    * @return the shedThreshold
    */
   public int getShedThreshold() {
      return shedThreshold;
   }

   /**
    * @param threadCountThreshold
    *           the shedThreshold to set
    */
   public void setShedThreshold(int threadCountThreshold) {
      log.info("Set shedThreshold to " + threadCountThreshold);
      this.shedThreshold = threadCountThreshold;
   }

   /**
    * @return the alarmThreshold
    */
   public int getAlarmThreshold() {
      return alarmThreshold;
   }

   /**
    * @param alarmThreshold
    *           the alarmThreshold to set
    */
   public void setAlarmThreshold(int alarmThreshold) {
      this.alarmThreshold = alarmThreshold;
   }

   /**
    * @return the valveThreshold
    */
   public int getValveThreshold() {
      return valveThreshold;
   }

   /**
    * @param valveThreshold
    *           the valveThreshold to set
    */
   public void setValveThreshold(int valveThreshold) {
      this.valveThreshold = valveThreshold;
   }

   /**
    * @return the throttleMaxTime
    */
   public long getThrottleMaxTime() {
      return throttleMaxTime;
   }

   /**
    * @param maxThrottleTime
    *           the throttleMaxTime to set
    */
   public void setThrottleMaxTime(long maxThrottleTime) {
      this.throttleMaxTime = maxThrottleTime;
   }

   /**
    * @return the throttleInterval
    */
   public long getThrottleInterval() {
      return throttleInterval;
   }

   /**
    * @param throttleInterval
    *           the throttleInterval to set
    */
   public void setThrottleInterval(long throttleInterval) {
      this.throttleInterval = throttleInterval;
   }

   /**
    * returns the current number of throttled requests waiting to be executed.
    * 
    * @param setpoint
    *           setpointId
    * @return throttle counter for the given setpointId
    */
   public int getThrottleCount(String setpoint) {
      AtomicInteger count = throttleCount.get(setpoint);
      if (count != null) {
         return count.get();
      } else {
         return 0;
      }
   }

}
