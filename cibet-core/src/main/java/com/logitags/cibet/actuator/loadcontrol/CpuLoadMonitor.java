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
 * Monitors CPU load of the system and of the current Java process. Supports alarm, valve and shed.
 * 
 */
public class CpuLoadMonitor extends AbstractMonitor {

   /**
   * 
   */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(CpuLoadMonitor.class);

   public static final String MONITORVALUE_SYSTEMCPU = "systemCpuLoad";
   public static final String MONITORVALUE_PROCESSCPU = "processCpuLoad";

   private transient VMLoadControlJMXBean vmJmxBean = new VMLoadControlJMXBean();

   /**
    * true if one of the alarm, valve or shed thresholds is &gt; 0
    */
   private boolean isSystemCpuLoadActive = false;
   private boolean isProcessCpuLoadActive = false;

   /**
    * threshold in %
    */
   private String processShedThreshold = "-1";
   private double _processShedThreshold = -1;
   /**
    * threshold in %
    */
   private String systemShedThreshold = "-1";
   private double _systemShedThreshold = -1;

   /**
    * alarm threshold in %
    */
   private String systemAlarmThreshold = "-1";
   private double _systemAlarmThreshold = -1;
   private Map<String, AtomicBoolean> systemAlarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> systemAlarmThresholdCount = new HashMap<>();

   /**
    * alarm threshold in %
    */
   private String processAlarmThreshold = "-1";
   private double _processAlarmThreshold = -1;
   private Map<String, AtomicBoolean> processAlarm = new HashMap<String, AtomicBoolean>();
   private Map<String, AtomicLong> processAlarmThresholdCount = new HashMap<>();

   /**
    * valve threshold in %
    */
   private String systemValveThreshold = "-1";
   private double _systemValveThreshold = -1;

   private String processValveThreshold = "-1";
   private double _processValveThreshold = -1;

   private long throttleMaxTime = 1000;
   private long throttleInterval = 200;

   private Map<String, AtomicInteger> throttleCount = new HashMap<String, AtomicInteger>();

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON
            || (!isSystemCpuLoadActive && !isProcessCpuLoadActive)) {
         return previousResult;
      }

      double systemCpuLoad = -1;
      double processCpuLoad = -1;

      if (isSystemCpuLoadActive) {
         systemCpuLoad = vmJmxBean.getSystemCpuLoad();
      }
      if (isProcessCpuLoadActive) {
         processCpuLoad = vmJmxBean.getProcessCpuLoad();
      }

      MonitorResult result = tryShed(MONITORVALUE_SYSTEMCPU, systemCpuLoad, _systemShedThreshold, callback, metadata,
            currentSetpointId);
      if (result == MonitorResult.SHED) {
         return result;
      }

      result = tryShed(MONITORVALUE_PROCESSCPU, processCpuLoad, _processShedThreshold, callback, metadata,
            currentSetpointId);
      if (result == MonitorResult.SHED) {
         return result;
      }

      result = tryValve(systemCpuLoad, processCpuLoad, callback, metadata, currentSetpointId);
      if (result != MonitorResult.PASSED) {
         return result;
      }

      result = trySystemCpuAlarm(systemCpuLoad, callback, metadata, currentSetpointId);
      if (result == MonitorResult.ALARM) {
         return result;
      }
      return tryProcessCpuAlarm(processCpuLoad, callback, metadata, currentSetpointId);
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
   }

   @Override
   public void reset(String setpointId) {
      throttleCount.put(setpointId, new AtomicInteger(0));
      if (vmJmxBean.getSystemCpuLoad() < 0 && vmJmxBean.getProcessCpuLoad() < 0) {
         status = MonitorStatus.NOT_SUPPORTED;
      }
      systemAlarm.put(setpointId, new AtomicBoolean(false));
      systemAlarmThresholdCount.put(setpointId, new AtomicLong(0));
      processAlarm.put(setpointId, new AtomicBoolean(false));
      processAlarmThresholdCount.put(setpointId, new AtomicLong(0));
   }

   private MonitorResult tryValve(double systemCpuLoad, double processCpuLoad, LoadControlCallback callback,
         EventMetadata metadata, String currentSetpointId) {
      MonitorResult result = MonitorResult.PASSED;
      if (_systemValveThreshold > 0 || _processValveThreshold > 0) {
         long startTime = System.currentTimeMillis();
         long maxTime = startTime + throttleMaxTime;

         while (maxTime - System.currentTimeMillis() > 0) {
            if ((systemCpuLoad > _systemValveThreshold && _systemValveThreshold > 0)
                  || (processCpuLoad > _processValveThreshold && _processValveThreshold > 0)) {
               if (result == MonitorResult.PASSED) {
                  throttleCount.get(currentSetpointId).incrementAndGet();
               }
               result = MonitorResult.THROTTLED;
               try {
                  Thread.sleep(throttleInterval);
               } catch (InterruptedException e) {
               }
               if (_systemValveThreshold > 0) {
                  systemCpuLoad = vmJmxBean.getSystemCpuLoad();
               }
               if (_processValveThreshold > 0) {
                  processCpuLoad = vmJmxBean.getProcessCpuLoad();
               }
            } else {
               if (result == MonitorResult.THROTTLED) {
                  throttleCount.get(currentSetpointId).decrementAndGet();
                  if (callback != null) {
                     LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                           metadata.getControlEvent(), getName(), MonitorResult.THROTTLED);
                     lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
                     if (_systemValveThreshold <= 0) {
                        lcdata.setMonitoredValue(MONITORVALUE_PROCESSCPU);
                        lcdata.setThreshold(String.valueOf(_processValveThreshold));
                        lcdata.setValue(String.valueOf(processCpuLoad));
                     } else if (_processValveThreshold <= 0) {
                        lcdata.setMonitoredValue(MONITORVALUE_SYSTEMCPU);
                        lcdata.setThreshold(String.valueOf(_systemValveThreshold));
                        lcdata.setValue(String.valueOf(systemCpuLoad));
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
                  metadata.getControlEvent(), getName(), MonitorResult.SHED);
            lcdata.setThrottleTime(System.currentTimeMillis() - startTime);
            if (systemCpuLoad > _systemValveThreshold && _systemValveThreshold > 0) {
               lcdata.setMonitoredValue(MONITORVALUE_SYSTEMCPU);
               lcdata.setThreshold(String.valueOf(_systemValveThreshold));
               lcdata.setValue(String.valueOf(systemCpuLoad));
            } else {
               lcdata.setMonitoredValue(MONITORVALUE_PROCESSCPU);
               lcdata.setThreshold(String.valueOf(_processValveThreshold));
               lcdata.setValue(String.valueOf(processCpuLoad));
            }
            callback.onShed(lcdata);
         }
         return MonitorResult.SHED;

      } else {
         return result;
      }
   }

   private MonitorResult tryShed(String monitorValueName, double cpuLoad, double threshold,
         LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
      if (threshold > -1 && cpuLoad > threshold) {
         if (callback != null) {
            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName(), MonitorResult.SHED);
            lcdata.setMonitoredValue(monitorValueName);
            lcdata.setThreshold(String.valueOf(threshold));
            lcdata.setValue(String.valueOf(cpuLoad));
            callback.onShed(lcdata);
         }

         return MonitorResult.SHED;
      } else {
         return MonitorResult.PASSED;
      }
   }

   private MonitorResult trySystemCpuAlarm(double cpuLoad, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (_systemAlarmThreshold > 0 && callback != null) {
         if (cpuLoad > _systemAlarmThreshold) {
            long counter;
            if (systemAlarm.get(currentSetpointId).compareAndSet(false, true)) {
               counter = systemAlarmThresholdCount.get(currentSetpointId).incrementAndGet();
            } else {
               counter = systemAlarmThresholdCount.get(currentSetpointId).get();
            }

            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName(), MonitorResult.ALARM);
            lcdata.setAlarmCount(counter);
            lcdata.setMonitoredValue(MONITORVALUE_SYSTEMCPU);
            lcdata.setThreshold(String.valueOf(_systemAlarmThreshold));
            lcdata.setValue(String.valueOf(cpuLoad));
            callback.onAlarm(lcdata);
            return MonitorResult.ALARM;

         } else {
            systemAlarm.get(currentSetpointId).set(false);
         }
      }
      return MonitorResult.PASSED;
   }

   private MonitorResult tryProcessCpuAlarm(double cpuLoad, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (_processAlarmThreshold >= 0 && callback != null) {
         if (cpuLoad > _processAlarmThreshold) {
            long counter;
            if (processAlarm.get(currentSetpointId).compareAndSet(false, true)) {
               counter = processAlarmThresholdCount.get(currentSetpointId).incrementAndGet();
            } else {
               counter = processAlarmThresholdCount.get(currentSetpointId).get();
            }

            LoadControlData lcdata = new LoadControlData(currentSetpointId, metadata.getResource(),
                  metadata.getControlEvent(), getName(), MonitorResult.ALARM);
            lcdata.setAlarmCount(counter);
            lcdata.setMonitoredValue(MONITORVALUE_PROCESSCPU);
            lcdata.setThreshold(String.valueOf(_processAlarmThreshold));
            lcdata.setValue(String.valueOf(cpuLoad));
            callback.onAlarm(lcdata);
            return MonitorResult.ALARM;

         } else {
            processAlarm.get(currentSetpointId).set(false);
         }
      }
      return MonitorResult.PASSED;
   }

   /**
    * @return the processShedThreshold
    */
   public String getProcessShedThreshold() {
      return processShedThreshold;
   }

   /**
    * @param threshold
    *           the processShedThreshold to set
    */
   public void setProcessShedThreshold(String threshold) {
      _processShedThreshold = convert(threshold);
      log.info("Set processShedThreshold to " + threshold);
      this.processShedThreshold = threshold;
      resolveProcessActiveState();
   }

   /**
    * @return the systemShedThreshold
    */
   public String getSystemShedThreshold() {
      return systemShedThreshold;
   }

   /**
    * @param threshold
    *           the systemShedThreshold to set
    */
   public void setSystemShedThreshold(String threshold) {
      _systemShedThreshold = convert(threshold);
      log.info("Set systemShedThreshold to " + threshold);
      this.systemShedThreshold = threshold;
      resolveSystemActiveState();
   }

   /**
    * @return the systemAlarmThreshold
    */
   public String getSystemAlarmThreshold() {
      return systemAlarmThreshold;
   }

   /**
    * @param threshold
    *           the systemAlarmThreshold to set
    */
   public void setSystemAlarmThreshold(String threshold) {
      _systemAlarmThreshold = convert(threshold);
      log.info("Set systemAlarmThreshold to " + threshold);
      this.systemAlarmThreshold = threshold;
      resolveSystemActiveState();
   }

   /**
    * @return the processAlarmThreshold
    */
   public String getProcessAlarmThreshold() {
      return processAlarmThreshold;
   }

   /**
    * @param threshold
    *           the processAlarmThreshold to set
    */
   public void setProcessAlarmThreshold(String threshold) {
      _processAlarmThreshold = convert(threshold);
      log.info("Set processAlarmThreshold to " + threshold);
      this.processAlarmThreshold = threshold;
      resolveProcessActiveState();
   }

   /**
    * @return the systemValveThreshold
    */
   public String getSystemValveThreshold() {
      return systemValveThreshold;
   }

   /**
    * @param threshold
    *           the systemValveThreshold to set
    */
   public void setSystemValveThreshold(String threshold) {
      _systemValveThreshold = convert(threshold);
      log.info("Set systemValveThreshold to " + threshold);
      this.systemValveThreshold = threshold;
      resolveSystemActiveState();
   }

   /**
    * @return the processValveThreshold
    */
   public String getProcessValveThreshold() {
      return processValveThreshold;
   }

   /**
    * @param threshold
    *           the processValveThreshold to set
    */
   public void setProcessValveThreshold(String threshold) {
      _processValveThreshold = convert(threshold);
      log.info("Set processValveThreshold to " + threshold);
      this.processValveThreshold = threshold;
      resolveProcessActiveState();
   }

   private double convert(String threshold) {
      if (status == MonitorStatus.NOT_SUPPORTED)
         return -1;
      double dblThreshold;
      int index = threshold.indexOf("%");
      if (index > 0) {
         dblThreshold = Double.valueOf(threshold.substring(0, index).trim());
      } else {
         dblThreshold = Double.valueOf(threshold.trim());
      }
      return dblThreshold;
   }

   private void resolveProcessActiveState() {
      if (_processAlarmThreshold > 0 || _processShedThreshold > 0 || _processValveThreshold > 0) {
         isProcessCpuLoadActive = true;
      } else {
         isProcessCpuLoadActive = false;
      }
   }

   private void resolveSystemActiveState() {
      if (_systemAlarmThreshold > 0 || _systemShedThreshold > 0 || _systemValveThreshold > 0) {
         isSystemCpuLoadActive = true;
      } else {
         isSystemCpuLoadActive = false;
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
