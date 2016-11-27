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

import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * Controls throughput. Supports alarm, valve and shed mode.
 * 
 * @author Wolfgang
 *
 */
public class ThroughputMonitor extends AbstractMonitor {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(ThroughputMonitor.class);

   private static final int MAX_DEQUE_SIZE = 10000;

   private Map<String, AtomicInteger> dequeSize = new HashMap<>();
   private Map<String, Deque<Long>> timestamps = new HashMap<>();

   /**
    * window width in ms. Default is 1000 ms.
    */
   private int windowWidth = 1000;

   private int shedThreshold = -1;
   private int valveThreshold = -1;
   private int alarmThreshold = -1;

   private long throttleMaxTime = 1000;
   private long throttleInterval = 200;
   private Map<String, AtomicInteger> throttleCount = new HashMap<String, AtomicInteger>();
   private Map<String, AtomicBoolean> alarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> alarmThresholdCount = new HashMap<>();

   private LoadControlActuator myLoadControlActuator;

   public ThroughputMonitor(LoadControlActuator lca) {
      myLoadControlActuator = lca;
   }

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON)
         return previousResult;

      MonitorResult result = MonitorResult.PASSED;
      if (shedThreshold > -1 || valveThreshold > -1 || alarmThreshold > -1) {
         int currentThroughput = getThroughput(currentSetpointId);

         result = tryShed(currentThroughput, callback, metadata, currentSetpointId);
         if (result == MonitorResult.SHED)
            return result;

         result = tryValve(currentThroughput, callback, metadata, currentSetpointId);
         if (result == MonitorResult.PASSED) {
            tryAlarm(currentThroughput, callback, metadata, currentSetpointId);
         }
      }

      return result;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
      if (status != MonitorStatus.ON || metadata.getExecutionStatus() == ExecutionStatus.SHED)
         return;

      long now = System.currentTimeMillis();
      timestamps.get(currentSetpointId).add(now);

      if (dequeSize.get(currentSetpointId).get() > MAX_DEQUE_SIZE) {
         timestamps.get(currentSetpointId).poll();
      } else {
         dequeSize.get(currentSetpointId).incrementAndGet();
      }
   }

   @Override
   public void reset(String setpointId) {
      log.debug("reset ContinuousThroughputMonitor");
      dequeSize.put(setpointId, new AtomicInteger(0));
      timestamps.put(setpointId, new ConcurrentLinkedDeque<Long>());
      throttleCount.put(setpointId, new AtomicInteger(0));
      alarm.put(setpointId, new AtomicBoolean(false));
      alarmThresholdCount.put(setpointId, new AtomicLong(0));
   }

   private MonitorResult tryShed(int currentThroughput, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (shedThreshold > -1 && currentThroughput >= shedThreshold) {
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setMonitoredValue(getName());
            lcdata.setThreshold(String.valueOf(shedThreshold));
            lcdata.setValue(String.valueOf(currentThroughput));
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;
      } else {
         return MonitorResult.PASSED;
      }
   }

   /**
    * 
    * @param currentThroughput
    * @param shedMap
    * @param callback
    * @param metadata
    * @param setpointId
    * @return 0: passed, 1: throttled, 2: shed
    */
   private MonitorResult tryValve(int currentThroughput, LoadControlCallback callback, EventMetadata metadata,
         String setpointId) {
      MonitorResult result = MonitorResult.PASSED;
      if (valveThreshold > -1) {
         long startTime = System.currentTimeMillis();
         long maxTime = startTime + throttleMaxTime;
         while (maxTime - System.currentTimeMillis() > 0) {
            if (currentThroughput >= valveThreshold) {
               if (result == MonitorResult.PASSED) {
                  throttleCount.get(setpointId).incrementAndGet();
               }
               result = MonitorResult.THROTTLED;
               try {
                  Thread.sleep(throttleInterval);
               } catch (InterruptedException e) {
               }
               currentThroughput = getThroughput(setpointId);

            } else {
               if (result == MonitorResult.THROTTLED) {
                  throttleCount.get(setpointId).decrementAndGet();
                  if (callback != null) {
                     LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(),
                           metadata.getControlEvent(), getName());
                     lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
                     lcdata.setMonitoredValue(getName());
                     lcdata.setThreshold(String.valueOf(valveThreshold));
                     lcdata.setValue(String.valueOf(currentThroughput));
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
                  getName());
            lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
            lcdata.setMonitoredValue(getName());
            lcdata.setThreshold(String.valueOf(valveThreshold));
            lcdata.setValue(String.valueOf(currentThroughput));
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;

      } else {
         return result;
      }
   }

   private void tryAlarm(int currentThroughput, LoadControlCallback callback, EventMetadata metadata,
         String setpointId) {
      if (alarmThreshold > -1 && callback != null) {
         if (currentThroughput >= alarmThreshold) {
            long counter;
            if (alarm.get(setpointId).compareAndSet(false, true)) {
               counter = alarmThresholdCount.get(setpointId).incrementAndGet();
            } else {
               counter = alarmThresholdCount.get(setpointId).get();
            }
            LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(), metadata.getControlEvent(),
                  getName());
            lcdata.setAlarmCount(counter);
            lcdata.setMonitoredValue(getName());
            lcdata.setThreshold(String.valueOf(alarmThreshold));
            lcdata.setValue(String.valueOf(currentThroughput));
            callback.onAlarm(lcdata);

         } else {
            alarm.get(setpointId).set(false);
         }
      }
   }

   public int getThroughput(String setpoint) {
      long now = System.currentTimeMillis();
      long nowMin1 = now - windowWidth;
      int counter = 0;
      Iterator<Long> iter = timestamps.get(setpoint).descendingIterator();
      while (iter.hasNext()) {
         if (nowMin1 > iter.next()) {
            break;
         } else {
            counter++;
         }
      }

      // log.debug("Continuous TM duration: " + (System.currentTimeMillis() - now));
      if (counter > MAX_DEQUE_SIZE) {
         log.warn("Throughput is higher than allowed maximum [" + MAX_DEQUE_SIZE + " / " + windowWidth
               + " ms]. Reduce the window width!");
         return -1;
      } else {
         return counter;
      }
   }

   public double getTotalThroughput(String setpointId) {
      if (myLoadControlActuator == null)
         return -1;
      AtomicLong date = myLoadControlActuator.getFirstHitTime(setpointId);
      if (date != null) {
         long duration = System.currentTimeMillis() - date.get();
         // long duration = System.currentTimeMillis() -
         // setpointStartTime.get(setpointId);
         // return 1000d * acceptedResponseTime.get(setpointId).get() /
         // duration;
         return 1000d * myLoadControlActuator.getAccepted(setpointId).get() / duration;
      } else {
         return -1;
      }
   }

   /**
    * @return the window
    */
   public int getWindowWidth() {
      return windowWidth;
   }

   /**
    * @param width
    *           the window to set
    */
   public void setWindowWidth(int width) {
      windowWidth = width;
   }

   /**
    * @return the shedThreshold
    */
   public int getShedThreshold() {
      return shedThreshold;
   }

   /**
    * @param shedThreshold
    *           the shedThreshold to set
    */
   public void setShedThreshold(int shedThreshold) {
      this.shedThreshold = shedThreshold;
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
    *           setpoint id
    * @return counter
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
