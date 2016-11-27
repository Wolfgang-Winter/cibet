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
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;

/**
 * Monitors memory usage and collection memory usage. Supports alarm, valve and shed
 */
public class MemoryMonitor extends AbstractMonitor {

   private static Log log = LogFactory.getLog(MemoryMonitor.class);

   /**
   * 
   */
   private static final long serialVersionUID = 1L;

   public static final String MONITORVALUE_USAGE = "usage";
   public static final String MONITORVALUE_COLLECTIONUSAGE = "collectionUsage";

   /**
    * time in ms after which a garbage collection is executed if the threshold is exceeded.
    */
   private int garbageCollectionWaitTime = 3000;
   /**
    * timestamp when the threshold was exceeded the last time
    */
   private AtomicLong thresholdExceededTimestamp;

   /**
    * memory usage threshold of the old generation memory pool. Could be given as absolute or relative value. If
    * absolute, it is in bytes. If the value ends with % it is relative as percentage of the max available memory.
    */
   private String usageShedThreshold = "-1";

   /**
    * memory collection usage threshold of the old generation memory pool. Could be given as absolute or relative value.
    * If absolute, it is in bytes. If the value ends with % it is relative as percentage of the max available memory.
    */
   private String collectionUsageShedThreshold = "-1";

   private String usageValveThreshold = "-1";

   /**
    * threshold in bytes
    */
   private double usageValveThresholdAbs = -1;
   private double usageValveThresholdPercent = -1;

   private String collectionUsageValveThreshold = "-1";

   /**
    * threshold in bytes
    */
   private double collectionUsageValveThresholdAbs = -1;
   private double collectionUsageValveThresholdPercent = -1;

   private long throttleMaxTime = 1000;
   private long throttleInterval = 200;

   private Map<String, AtomicInteger> throttleCount = new HashMap<String, AtomicInteger>();

   private String usageAlarmThreshold = "-1";
   private double usageAlarmThresholdAbs = -1;
   private double usageAlarmThresholdPercent = -1;
   private Map<String, AtomicBoolean> usageAlarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> usageAlarmThresholdCount = new HashMap<>();

   private String collectionUsageAlarmThreshold = "-1";
   private double collectionUsageAlarmThresholdAbs = -1;
   private double collectionUsageAlarmThresholdPercent = -1;
   private Map<String, AtomicBoolean> collectionUsageAlarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> collectionUsageAlarmThresholdCount = new HashMap<>();

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON) {
         return previousResult;
      }

      MemoryPoolMXBean tenurePool = VMLoadControlJMXBean.getTenuredGenPool();
      if (tenurePool == null) {
         return previousResult;
      }

      MonitorResult result = tryShed(tenurePool, callback, metadata, currentSetpointId);
      if (result == MonitorResult.PASSED) {
         result = tryValve(tenurePool, callback, metadata, currentSetpointId);
      }
      if (result == MonitorResult.PASSED) {
         result = tryUsageAlarm(tenurePool, callback, metadata, currentSetpointId);
      }
      if (result == MonitorResult.PASSED) {
         result = tryCollectionUsageAlarm(tenurePool, callback, metadata, currentSetpointId);
      }

      if (result == MonitorResult.SHED) {
         long now = System.currentTimeMillis();
         if (garbageCollectionWaitTime > 0 && !thresholdExceededTimestamp.compareAndSet(0, now)) {
            long ts = thresholdExceededTimestamp.get();
            if (ts != 0 && now - ts > garbageCollectionWaitTime) {
               thresholdExceededTimestamp.set(0);
               MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
               memoryBean.gc();
            }
         }
      }
      return result;
   }

   private MonitorResult tryValve(MemoryPoolMXBean tenurePool, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      MonitorResult result = MonitorResult.PASSED;
      if (usageValveThresholdAbs > 0 || usageValveThresholdPercent > 0 || collectionUsageValveThresholdAbs > 0
            || collectionUsageValveThresholdPercent > 0) {
         long startTime = System.currentTimeMillis();
         long maxTime = startTime + throttleMaxTime;
         double uThreshold = usageValveThresholdAbs;
         if (usageValveThresholdPercent > 0) {
            uThreshold = tenurePool.getUsage().getMax() * usageValveThresholdPercent;
         }
         double cuThreshold = collectionUsageValveThresholdAbs;
         if (collectionUsageValveThresholdPercent > 0) {
            cuThreshold = tenurePool.getCollectionUsage().getMax() * collectionUsageValveThresholdPercent;
         }

         while (maxTime - System.currentTimeMillis() > 0) {
            if ((uThreshold > 0 && tenurePool.getUsage().getUsed() > uThreshold)
                  || (cuThreshold > 0 && tenurePool.getCollectionUsage().getUsed() > cuThreshold)) {
               if (result == MonitorResult.PASSED) {
                  throttleCount.get(currentSetpointId).incrementAndGet();
               }
               result = MonitorResult.THROTTLED;
               try {
                  Thread.sleep(throttleInterval);
               } catch (InterruptedException e) {
               }

            } else {
               if (result == MonitorResult.THROTTLED) {
                  throttleCount.get(currentSetpointId).decrementAndGet();
                  // call valve callback
                  if (callback != null) {
                     LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                           metadata.getControlEvent(), getName());
                     lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
                     if (uThreshold > 0) {
                        lcdata.setMonitoredValue(MONITORVALUE_USAGE);
                        lcdata.setThreshold(String.valueOf(uThreshold));
                        lcdata.setValue(String.valueOf(tenurePool.getUsage().getUsed()));
                     } else {
                        lcdata.setMonitoredValue(MONITORVALUE_COLLECTIONUSAGE);
                        lcdata.setThreshold(String.valueOf(cuThreshold));
                        lcdata.setValue(String.valueOf(tenurePool.getCollectionUsage().getUsed()));
                     }
                     callback.onThrottled(lcdata);
                  }
               }
               return result;
            }
         }

         throttleCount.get(currentSetpointId).decrementAndGet();
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
            if (uThreshold > 0 && tenurePool.getUsage().getUsed() > uThreshold) {
               lcdata.setMonitoredValue(MONITORVALUE_USAGE);
               lcdata.setThreshold(String.valueOf(uThreshold));
               lcdata.setValue(String.valueOf(tenurePool.getUsage().getUsed()));
            } else {
               lcdata.setMonitoredValue(MONITORVALUE_COLLECTIONUSAGE);
               lcdata.setThreshold(String.valueOf(cuThreshold));
               lcdata.setValue(String.valueOf(tenurePool.getCollectionUsage().getUsed()));
            }
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;

      } else {
         return result;
      }
   }

   private MonitorResult tryShed(MemoryPoolMXBean tenurePool, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (tenurePool.isCollectionUsageThresholdExceeded()) {
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setMonitoredValue(MONITORVALUE_COLLECTIONUSAGE);
            lcdata.setThreshold(collectionUsageShedThreshold);
            lcdata.setValue(String.valueOf(tenurePool.getCollectionUsage().getUsed()));
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;

      } else if (tenurePool.isUsageThresholdExceeded()) {
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setMonitoredValue(MONITORVALUE_USAGE);
            lcdata.setThreshold(usageShedThreshold);
            lcdata.setValue(String.valueOf(tenurePool.getUsage().getUsed()));
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;

      } else {
         if (garbageCollectionWaitTime > 0) {
            thresholdExceededTimestamp.set(0);
         }
         return MonitorResult.PASSED;
      }
   }

   private MonitorResult tryUsageAlarm(MemoryPoolMXBean tenurePool, LoadControlCallback callback,
         EventMetadata metadata, String setpointId) {
      if ((usageAlarmThresholdAbs > 0 || usageAlarmThresholdPercent > 0) && callback != null) {
         double uThreshold = usageAlarmThresholdAbs;
         if (usageAlarmThresholdPercent > 0) {
            uThreshold = tenurePool.getUsage().getMax() * usageAlarmThresholdPercent;
         }

         if (tenurePool.getUsage().getUsed() > uThreshold) {
            long counter;
            if (usageAlarm.get(setpointId).compareAndSet(false, true)) {
               counter = usageAlarmThresholdCount.get(setpointId).incrementAndGet();
            } else {
               counter = usageAlarmThresholdCount.get(setpointId).get();
            }
            LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(), metadata.getControlEvent(),
                  getName());
            lcdata.setAlarmCount(counter);
            lcdata.setMonitoredValue(MONITORVALUE_USAGE);
            lcdata.setThreshold(String.valueOf(uThreshold));
            lcdata.setValue(String.valueOf(tenurePool.getUsage().getUsed()));
            callback.onAlarm(lcdata);

            return MonitorResult.ALARM;

         } else {
            usageAlarm.get(setpointId).set(false);
         }
      }
      return MonitorResult.PASSED;
   }

   private MonitorResult tryCollectionUsageAlarm(MemoryPoolMXBean tenurePool, LoadControlCallback callback,
         EventMetadata metadata, String setpointId) {
      if ((collectionUsageAlarmThresholdAbs > 0 || collectionUsageAlarmThresholdPercent > 0) && callback != null) {
         double cuThreshold = collectionUsageAlarmThresholdAbs;
         if (collectionUsageAlarmThresholdPercent > 0) {
            cuThreshold = tenurePool.getCollectionUsage().getMax() * collectionUsageAlarmThresholdPercent;
         }

         if (tenurePool.getCollectionUsage().getUsed() > cuThreshold) {
            long counter;
            if (collectionUsageAlarm.get(setpointId).compareAndSet(false, true)) {
               counter = collectionUsageAlarmThresholdCount.get(setpointId).incrementAndGet();
            } else {
               counter = collectionUsageAlarmThresholdCount.get(setpointId).get();
            }
            LoadControlData lcdata = new LoadControlData(setpointId, metadata.getResource(), metadata.getControlEvent(),
                  getName());
            lcdata.setAlarmCount(counter);
            lcdata.setMonitoredValue(MONITORVALUE_COLLECTIONUSAGE);
            lcdata.setThreshold(String.valueOf(cuThreshold));
            lcdata.setValue(String.valueOf(tenurePool.getCollectionUsage().getUsed()));
            callback.onAlarm(lcdata);

            return MonitorResult.ALARM;

         } else {
            collectionUsageAlarm.get(setpointId).set(false);
         }
      }
      return MonitorResult.PASSED;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String setpointId) {
   }

   @Override
   public void reset(String setpointId) {
      thresholdExceededTimestamp = new AtomicLong(0);
      applyMemoryUsageThreshold();
      applyMemoryCollectionUsageThreshold();
      usageAlarm.put(setpointId, new AtomicBoolean(false));
      usageAlarmThresholdCount.put(setpointId, new AtomicLong(0));
      collectionUsageAlarm.put(setpointId, new AtomicBoolean(false));
      collectionUsageAlarmThresholdCount.put(setpointId, new AtomicLong(0));
      throttleCount.put(setpointId, new AtomicInteger(0));
   }

   @Override
   public void setStatus(MonitorStatus mode) {
      super.setStatus(mode);
      applyMemoryUsageThreshold();
      applyMemoryCollectionUsageThreshold();
   }

   private void applyMemoryUsageThreshold() {
      if (status == MonitorStatus.NOT_SUPPORTED)
         return;
      MemoryPoolMXBean tenurePool = VMLoadControlJMXBean.getTenuredGenPool();
      if (tenurePool != null) {
         if (status == MonitorStatus.ON && !"-1".equals(usageShedThreshold.trim())) {
            if (usageShedThreshold.trim().endsWith("%")) {
               double threshold = Double
                     .valueOf(usageShedThreshold.trim().substring(0, usageShedThreshold.trim().length() - 1));
               if (threshold < 0 || threshold > 100) {
                  throw new IllegalArgumentException("memory threshold must be between 0 and 100%");
               }

               long max = tenurePool.getUsage().getMax();
               if (max == -1) {
                  String txt = "Cannot set memory usage threshold: Max value of tenured Gen memory pool is unavailable.";
                  log.error(txt);
                  status = MonitorStatus.NOT_SUPPORTED;
                  return;
               }
               double thresh = max * threshold / 100;
               log.info("set memory usage threshold to " + thresh);
               tenurePool.setUsageThreshold(Math.round(thresh));
            } else {
               log.info("set memory usage threshold to " + usageShedThreshold);
               tenurePool.setUsageThreshold(Long.parseLong(usageShedThreshold.trim()));
            }

         } else {
            tenurePool.setUsageThreshold(0);
         }
      } else {
         status = MonitorStatus.NOT_SUPPORTED;
      }
   }

   private void applyMemoryCollectionUsageThreshold() {
      if (status == MonitorStatus.NOT_SUPPORTED)
         return;
      MemoryPoolMXBean tenuredPool = VMLoadControlJMXBean.getTenuredGenPool();
      if (tenuredPool != null) {
         if (status == MonitorStatus.ON && !"-1".equals(collectionUsageShedThreshold.trim())) {
            if (collectionUsageShedThreshold.trim().endsWith("%")) {
               double threshold = Double.valueOf(collectionUsageShedThreshold.trim().substring(0,
                     collectionUsageShedThreshold.trim().length() - 1));
               if (threshold < 0 || threshold > 100) {
                  throw new IllegalArgumentException("memory collection usage threshold must be between 0 and 100 (%)");
               }

               long max = tenuredPool.getCollectionUsage().getMax();
               if (max == -1) {
                  String txt = "Cannot set memory collection usage threshold: Max value of tenured gen memory pool is unavailable.";
                  log.error(txt);
                  status = MonitorStatus.NOT_SUPPORTED;
                  return;
               }
               double thresh = max * threshold / 100;
               log.info("set memory collection usage threshold to " + thresh);
               tenuredPool.setCollectionUsageThreshold(Math.round(thresh));

            } else {
               log.info("set memory collection usage threshold to " + collectionUsageShedThreshold);
               tenuredPool.setCollectionUsageThreshold(Long.parseLong(collectionUsageShedThreshold));
            }

         } else {
            tenuredPool.setCollectionUsageThreshold(0);
         }
      } else {
         status = MonitorStatus.NOT_SUPPORTED;
      }
   }

   /**
    * @return the garbageCollectionWaitTime
    */
   public int getGarbageCollectionWaitTime() {
      return garbageCollectionWaitTime;
   }

   /**
    * @param garbageCollectionWaitTime
    *           the garbageCollectionWaitTime to set
    */
   public void setGarbageCollectionWaitTime(int garbageCollectionWaitTime) {
      log.info("Set garbageCollectionWaitTime to " + garbageCollectionWaitTime);
      this.garbageCollectionWaitTime = garbageCollectionWaitTime;
   }

   /**
    * @return the usageShedThreshold
    */
   public String getUsageShedThreshold() {
      return usageShedThreshold;
   }

   /**
    * set usage shed threshold. When the value ends with %, it is the percentage of the maximum memory
    * 
    * @param threshold
    *           the usageShedThreshold to set
    */
   public void setUsageShedThreshold(String threshold) {
      if (status == MonitorStatus.NOT_SUPPORTED)
         return;
      this.usageShedThreshold = threshold.trim();
      applyMemoryUsageThreshold();
   }

   /**
    * @return the collectionUsageShedThreshold
    */
   public String getCollectionUsageShedThreshold() {
      return collectionUsageShedThreshold;
   }

   /**
    * @param threshold
    *           the collectionUsageShedThreshold to set
    */
   public void setCollectionUsageShedThreshold(String threshold) {
      if (status == MonitorStatus.NOT_SUPPORTED)
         return;
      this.collectionUsageShedThreshold = threshold.trim();
      applyMemoryCollectionUsageThreshold();
   }

   /**
    * @return the usageValveThreshold
    */
   public String getUsageValveThreshold() {
      return usageValveThreshold;
   }

   /**
    * @param threshold
    *           the usageValveThreshold to set
    */
   public void setUsageValveThreshold(String threshold) {
      resolveValue(this, "usageValveThreshold", threshold);
   }

   /**
    * @return the collectionUsageValveThreshold
    */
   public String getCollectionUsageValveThreshold() {
      return collectionUsageValveThreshold;
   }

   /**
    * @param threshold
    *           the collectionUsageValveThreshold to set
    */
   public void setCollectionUsageValveThreshold(String threshold) {
      resolveValue(this, "collectionUsageValveThreshold", threshold);
   }

   /**
    * @return the usageAlarmThreshold
    */
   public String getUsageAlarmThreshold() {
      return usageAlarmThreshold;
   }

   /**
    * @param threshold
    *           the usageAlarmThreshold to set
    */
   public void setUsageAlarmThreshold(String threshold) {
      resolveValue(this, "usageAlarmThreshold", threshold);
   }

   /**
    * @return the collectionUsageAlarmThreshold
    */
   public String getCollectionUsageAlarmThreshold() {
      return collectionUsageAlarmThreshold;
   }

   /**
    * @param threshold
    *           the collectionUsageAlarmThreshold to set
    */
   public void setCollectionUsageAlarmThreshold(String threshold) {
      resolveValue(this, "collectionUsageAlarmThreshold", threshold);
   }

   /**
    * returns the current number of throttled requests waiting to be executed.
    * 
    * @param setpoint
    *           setpoint id
    * @return counter for the given setpoint id
    */
   public int getThrottleCount(String setpoint) {
      AtomicInteger count = throttleCount.get(setpoint);
      if (count != null) {
         return count.get();
      } else {
         return 0;
      }
   }

   /**
    * @return the throttleMaxTime
    */
   public long getThrottleMaxTime() {
      return throttleMaxTime;
   }

   /**
    * @param maxThrottleTime
    *           the maxThrottleTime to set
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

}
