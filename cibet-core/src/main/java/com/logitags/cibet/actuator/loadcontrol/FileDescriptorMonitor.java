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
 * controls open file descriptors. Supports alarm, valve and shed mode.
 * 
 * @author Wolfgang
 *
 */
public class FileDescriptorMonitor extends AbstractMonitor {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static Log log = LogFactory.getLog(FileDescriptorMonitor.class);

   private static final String PERCENTVALUESTRING = "File Descriptors percent";
   private static final String ABSVALUESTRING = "File Descriptors count";

   private transient VMLoadControlJMXBean vmJmxBean = new VMLoadControlJMXBean();

   /**
    * Allowed exceed of the file descriptor count. The value can be given absolute or relative. If absolute it is given
    * in count. If relative, it is in % of the max available file descriptors and the value must end with %.
    */
   private String shedThreshold = "-1";
   private double shedThresholdPercent = -1;
   private double shedThresholdAbs = -1;

   private String alarmThreshold = "-1";
   private double alarmThresholdPercent = -1;
   private double alarmThresholdAbs = -1;
   private Map<String, AtomicBoolean> alarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> alarmThresholdCount = new HashMap<>();

   private String valveThreshold = "-1";
   private double valveThresholdPercent = -1;
   private double valveThresholdAbs = -1;

   private long throttleMaxTime = 1000;
   private long throttleInterval = 200;

   private Map<String, AtomicInteger> throttleCount = new HashMap<String, AtomicInteger>();

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON) {
         return previousResult;
      }

      long fdCount = vmJmxBean.getOpenFileDescriptors();
      double fdCountPercent = vmJmxBean.getOpenFileDescriptorsPercent();

      MonitorResult result = tryShed(fdCount, fdCountPercent, callback, metadata, currentSetpointId);
      if (result == MonitorResult.SHED) {
         return result;
      }

      result = tryValve(fdCount, fdCountPercent, callback, metadata, currentSetpointId);
      if (result != MonitorResult.PASSED) {
         return result;
      }

      result = tryAlarm(fdCount, fdCountPercent, callback, metadata, currentSetpointId);
      return result;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
   }

   @Override
   public void reset(String setpointId) {
      if (vmJmxBean.getOpenFileDescriptorsPercent() < 0) {
         status = MonitorStatus.NOT_SUPPORTED;
      }
      throttleCount.put(setpointId, new AtomicInteger(0));
      alarm.put(setpointId, new AtomicBoolean(false));
      alarmThresholdCount.put(setpointId, new AtomicLong(0));
   }

   private MonitorResult tryShed(long fdCount, double fdCountPercent, LoadControlCallback callback,
         EventMetadata metadata, String currentSetpointId) {
      if (shedThresholdPercent > -1 && fdCountPercent > shedThresholdPercent) {
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setMonitoredValue(PERCENTVALUESTRING);
            lcdata.setThreshold(String.valueOf(shedThresholdPercent));
            lcdata.setValue(String.valueOf(fdCountPercent));
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;

      } else if (shedThresholdAbs > -1 && fdCount > shedThresholdAbs) {
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setMonitoredValue(ABSVALUESTRING);
            lcdata.setThreshold(String.valueOf(shedThresholdAbs));
            lcdata.setValue(String.valueOf(fdCount));
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;
      }

      return MonitorResult.PASSED;
   }

   private MonitorResult tryValve(long fdCount, double fdCountPercent, LoadControlCallback callback,
         EventMetadata metadata, String currentSetpointId) {

      MonitorResult result = MonitorResult.PASSED;
      if (valveThresholdAbs > 0 || valveThresholdPercent > 0) {
         long startTime = System.currentTimeMillis();
         long maxTime = startTime + throttleMaxTime;

         while (maxTime - System.currentTimeMillis() > 0) {
            if ((fdCount > valveThresholdAbs && valveThresholdAbs > 0)
                  || (fdCountPercent > valveThresholdPercent && valveThresholdPercent > 0)) {
               if (result == MonitorResult.PASSED) {
                  throttleCount.get(currentSetpointId).incrementAndGet();
               }
               result = MonitorResult.THROTTLED;
               try {
                  Thread.sleep(throttleInterval);
               } catch (InterruptedException e) {
               }
               if (valveThresholdAbs > 0) {
                  fdCount = vmJmxBean.getOpenFileDescriptors();
               }
               if (valveThresholdPercent > 0) {
                  fdCountPercent = vmJmxBean.getOpenFileDescriptorsPercent();
               }
            } else {
               if (result == MonitorResult.THROTTLED) {
                  throttleCount.get(currentSetpointId).decrementAndGet();
                  if (callback != null) {
                     LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                           metadata.getControlEvent(), getName());
                     lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
                     if (valveThresholdAbs > 0) {
                        lcdata.setMonitoredValue(ABSVALUESTRING);
                        lcdata.setThreshold(String.valueOf(valveThresholdAbs));
                        lcdata.setValue(String.valueOf(fdCount));
                     } else if (valveThresholdPercent > 0) {
                        lcdata.setMonitoredValue(PERCENTVALUESTRING);
                        lcdata.setThreshold(String.valueOf(valveThresholdPercent));
                        lcdata.setValue(String.valueOf(fdCountPercent));
                     }
                     callback.onThrottled(lcdata);
                  }
               }
               return result;
            }
         }

         // shed after throttling
         throttleCount.get(currentSetpointId).decrementAndGet();
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
            if (fdCount > valveThresholdAbs && valveThresholdAbs > 0) {
               lcdata.setMonitoredValue(ABSVALUESTRING);
               lcdata.setThreshold(String.valueOf(valveThresholdAbs));
               lcdata.setValue(String.valueOf(fdCount));
            } else {
               lcdata.setMonitoredValue(PERCENTVALUESTRING);
               lcdata.setThreshold(String.valueOf(valveThresholdPercent));
               lcdata.setValue(String.valueOf(fdCountPercent));
            }
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;

      } else {
         return result;
      }
   }

   private MonitorResult tryAlarm(long fdCount, double fdCountPercent, LoadControlCallback callback,
         EventMetadata metadata, String currentSetpointId) {
      if (callback == null)
         return MonitorResult.PASSED;

      if (alarmThresholdAbs > 0) {
         if (fdCount > alarmThresholdAbs) {
            long counter;
            if (alarm.get(currentSetpointId).compareAndSet(false, true)) {
               counter = alarmThresholdCount.get(currentSetpointId).incrementAndGet();
            } else {
               counter = alarmThresholdCount.get(currentSetpointId).get();
            }

            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setAlarmCount(counter);
            lcdata.setMonitoredValue(ABSVALUESTRING);
            lcdata.setThreshold(String.valueOf(alarmThresholdAbs));
            lcdata.setValue(String.valueOf(fdCount));
            callback.onAlarm(lcdata);
            return MonitorResult.ALARM;

         } else {
            alarm.get(currentSetpointId).set(false);
         }
      } else if (alarmThresholdPercent > 0) {
         if (fdCountPercent > alarmThresholdPercent) {
            long counter;
            if (alarm.get(currentSetpointId).compareAndSet(false, true)) {
               counter = alarmThresholdCount.get(currentSetpointId).incrementAndGet();
            } else {
               counter = alarmThresholdCount.get(currentSetpointId).get();
            }

            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName());
            lcdata.setAlarmCount(counter);
            lcdata.setMonitoredValue(PERCENTVALUESTRING);
            lcdata.setThreshold(String.valueOf(alarmThresholdPercent));
            lcdata.setValue(String.valueOf(fdCountPercent));
            callback.onAlarm(lcdata);
            return MonitorResult.ALARM;

         } else {
            alarm.get(currentSetpointId).set(false);
         }
      }
      return MonitorResult.PASSED;
   }

   /**
    * @return the shedThreshold
    */
   public String getShedThreshold() {
      return shedThreshold;
   }

   /**
    * @param threshold
    *           the shedThreshold to set
    */
   public void setShedThreshold(String threshold) {
      resolveValue(this, "shedThreshold", threshold);
      if (shedThresholdPercent > -1)
         shedThresholdPercent = shedThresholdPercent * 100;
   }

   /**
    * @return the alarmThreshold
    */
   public String getAlarmThreshold() {
      return alarmThreshold;
   }

   /**
    * @param threshold
    *           the alarmThreshold to set
    */
   public void setAlarmThreshold(String threshold) {
      resolveValue(this, "alarmThreshold", threshold);
      if (alarmThresholdPercent > -1)
         alarmThresholdPercent = alarmThresholdPercent * 100;
   }

   /**
    * @return the valveThreshold
    */
   public String getValveThreshold() {
      return valveThreshold;
   }

   /**
    * @param threshold
    *           the valveThreshold to set
    */
   public void setValveThreshold(String threshold) {
      resolveValue(this, "valveThreshold", threshold);
      if (valveThresholdPercent > -1)
         valveThresholdPercent = valveThresholdPercent * 100;
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

   /**
    * returns the current number of throttled requests waiting to be executed.
    * 
    * @param setpoint
    *           setpoint
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
