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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.udojava.jmx.wrapper.JMXBean;
import com.udojava.jmx.wrapper.JMXBeanAttribute;

@JMXBean(description = "JMX bean for managing common properties of LoadControlActuator", sorted = true)
public class CommonLoadControlJMXBean {

   private static Log log = LogFactory.getLog(CommonLoadControlJMXBean.class);

   private static final String NOT_AVAILABLE = "Unavailable";

   private LoadControlActuator actuator;

   public CommonLoadControlJMXBean(LoadControlActuator act) {
      actuator = act;
   }

   private String exceptionToString(Throwable e, String pretext) {
      String txt = pretext + e.getMessage();
      log.error(txt, e);
      StringWriter w = new StringWriter();
      PrintWriter printer = new PrintWriter(w);
      e.printStackTrace(printer);
      return txt + "\n" + w.toString();
   }

   /**
    * @return the actuatorName
    */
   @JMXBeanAttribute(name = ".Actuator Name", description = "Name of the actuator. Default is LOADCONTROL", sortValue = "a1")
   public String getActuatorName() {
      return actuator.getName();
   }

   /**
    * @return the startTime
    */
   @JMXBeanAttribute(name = ".Actuator Start Time", description = "Time when this actuator started to execute", sortValue = "a2")
   public Date getStartTime() {
      return actuator.getStartTime();
   }

   @JMXBeanAttribute(name = ".LoadControl Callback class", description = "Class name of a LoadControlCallback implementation class", sortValue = "a3")
   public String getLoadControlCallbackClass() {
      LoadControlCallback cb = actuator.getLoadControlCallback();
      if (cb == null) {
         return "";
      } else {
         return cb.getClass().getName();
      }
   }

   @JMXBeanAttribute(name = ".LoadControl Callback class", description = "Class name of a LoadControlCallback implementation class", sortValue = "a3")
   public void setLoadControlCallbackClass(String name) {
      if (name == null || "".equals(name.trim()) || "<null>".equalsIgnoreCase(name)) {
         actuator.setLoadControlCallback(null);
      } else {
         try {
            Class<LoadControlCallback> clazz = (Class<LoadControlCallback>) Class.forName(name.trim());
            actuator.setLoadControlCallback(clazz.newInstance());
         } catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            exceptionToString(e, "");
         }
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ProcessAlarmThreshold", description = "Threshold in % when an alarm is raised.", sortValue = "c01")
   public String getCpuProcessAlarmThreshold() {
      if (actuator.getCpuLoadMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getCpuLoadMonitor().getProcessAlarmThreshold();
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ProcessAlarmThreshold", description = "Threshold in % when an alarm is raised.", sortValue = "c01")
   public void setCpuProcessAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getCpuLoadMonitor().setProcessAlarmThreshold(threshold);
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ProcessShedThreshold", description = "Maximum allowed process CPU load (%)", sortValue = "c02")
   public String getCpuProcessShedThreshold() {
      if (actuator.getCpuLoadMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getCpuLoadMonitor().getProcessShedThreshold();
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ProcessShedThreshold", description = "Maximum allowed process CPU load (%)", sortValue = "c02")
   public void setCpuProcessShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getCpuLoadMonitor().setProcessShedThreshold(threshold);
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ProcessValveThreshold", description = "Threshold in % when a request is throttled by a valve.", sortValue = "c03")
   public String getCpuProcessValveThreshold() {
      if (actuator.getCpuLoadMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getCpuLoadMonitor().getProcessValveThreshold();
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ProcessValveThreshold", description = "Threshold in % when a request is throttled by a valve.", sortValue = "c03")
   public void setCpuProcessValveThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getCpuLoadMonitor().setProcessValveThreshold(threshold);
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.Status", description = "One of OFF, ON", sortValue = "c04")
   public String getCpuLoadStatus() {
      return actuator.getCpuLoadMonitor().getStatus().name();
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.Status", description = "One of OFF, ON", sortValue = "c04")
   public void setCpuLoadStatus(String mode) {
      actuator.getCpuLoadMonitor().setStatus(MonitorStatus.valueOf(mode.toUpperCase().trim()));
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.SystemAlarmThreshold", description = "Threshold in % when an alarm is raised.", sortValue = "c05")
   public String getCpuSystemAlarmThreshold() {
      if (actuator.getCpuLoadMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getCpuLoadMonitor().getSystemAlarmThreshold();
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.SystemAlarmThreshold", description = "Threshold in % when an alarm is raised.", sortValue = "c05")
   public void setCpuSystemAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getCpuLoadMonitor().setSystemAlarmThreshold(threshold);
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.SystemShedThreshold", description = "Maximum allowed system CPU load (%)", sortValue = "c06")
   public String getCpuSystemShedThreshold() {
      if (actuator.getCpuLoadMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getCpuLoadMonitor().getSystemShedThreshold();
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.SystemShedThreshold", description = "Maximum allowed system CPU load (%)", sortValue = "c06")
   public void setCpuSystemShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getCpuLoadMonitor().setSystemShedThreshold(threshold);
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.SystemValveThreshold", description = "Threshold in % when a request is throttled by a valve.", sortValue = "c07")
   public String getCpuSystemValveThreshold() {
      if (actuator.getCpuLoadMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getCpuLoadMonitor().getSystemValveThreshold();
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.SystemValveThreshold", description = "Threshold in % when a request is throttled by a valve.", sortValue = "c07")
   public void setCpuSystemValveThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getCpuLoadMonitor().setSystemValveThreshold(threshold);
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "c08")
   public String getCpuLoadThrottleInterval() {
      if (actuator.getCpuLoadMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getCpuLoadMonitor().getThrottleInterval());
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "c08")
   public void setCpuLoadThrottleInterval(String in) {
      if (NOT_AVAILABLE.equals(in)) return;
      try {
         long t = Long.parseLong(in);
         actuator.getCpuLoadMonitor().setThrottleInterval(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "c09")
   public String getCpuLoadThrottleMaxTime() {
      if (actuator.getCpuLoadMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getCpuLoadMonitor().getThrottleMaxTime());
      }
   }

   @JMXBeanAttribute(name = "CpuLoadMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "c09")
   public void setCpuLoadThrottleMaxTime(String time) {
      if (NOT_AVAILABLE.equals(time)) return;
      try {
         long t = Long.parseLong(time);
         actuator.getCpuLoadMonitor().setThrottleMaxTime(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.AlarmThreshold", description = "File descriptor threshold (absolute or in percent of the max available) for raising an alarm", sortValue = "c50")
   public String getFileDescriptorAlarmThreshold() {
      if (actuator.getFileDescriptorMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getFileDescriptorMonitor().getAlarmThreshold();
      }
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.AlarmThreshold", description = "File descriptor threshold (absolute or in percent of the max available) for raising an alarm", sortValue = "c50")
   public void setFileDescriptorAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getFileDescriptorMonitor().setAlarmThreshold(threshold);
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ShedThreshold", description = "Maximum allowed file descriptors (abs or in percent of the max available)", sortValue = "c51")
   public String getFileDescriptorShedThreshold() {
      if (actuator.getFileDescriptorMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getFileDescriptorMonitor().getShedThreshold();
      }
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ShedThreshold", description = "Maximum allowed file descriptors (abs or in percent of the max available)", sortValue = "c51")
   public void setFileDescriptorShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getFileDescriptorMonitor().setShedThreshold(threshold);
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.Status", description = "One of OFF, ON", sortValue = "c52")
   public String getFileDescriptorStatus() {
      return actuator.getFileDescriptorMonitor().getStatus().name();
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.Status", description = "One of OFF, ON", sortValue = "c52")
   public void setFileDescriptorStatus(String mode) {
      actuator.getFileDescriptorMonitor().setStatus(MonitorStatus.valueOf(mode.toUpperCase().trim()));
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "c53")
   public String getFileDescriptorThrottleInterval() {
      if (actuator.getFileDescriptorMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getFileDescriptorMonitor().getThrottleInterval());
      }
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "c53")
   public void setFileDescriptorThrottleInterval(String in) {
      if (NOT_AVAILABLE.equals(in)) return;
      try {
         long t = Long.parseLong(in);
         actuator.getFileDescriptorMonitor().setThrottleInterval(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "c54")
   public String getFileDescriptorThrottleMaxTime() {
      if (actuator.getFileDescriptorMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getFileDescriptorMonitor().getThrottleMaxTime());
      }
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "c54")
   public void setFileDescriptorThrottleMaxTime(String time) {
      if (NOT_AVAILABLE.equals(time)) return;
      try {
         long t = Long.parseLong(time);
         actuator.getFileDescriptorMonitor().setThrottleMaxTime(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ValveThreshold", description = "Maximum file descriptors (abs or %) before requests are throttled by a valve", sortValue = "c55")
   public String getFileDescriptorValveThreshold() {
      if (actuator.getFileDescriptorMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getFileDescriptorMonitor().getValveThreshold());
      }
   }

   @JMXBeanAttribute(name = "FileDescriptorMonitor.ValveThreshold", description = "Maximum file descriptors (abs or %) before requests are throttled by a valve", sortValue = "c55")
   public void setFileDescriptorValveThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getFileDescriptorMonitor().setValveThreshold(threshold);
   }

   @JMXBeanAttribute(name = "MemoryMonitor.CollectionUsageAlarmThreshold", description = "alarm threshold for the collection usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d01")
   public String getMemoryCollectionUsageAlarmThreshold() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getMemoryMonitor().getCollectionUsageAlarmThreshold();
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.CollectionUsageAlarmThreshold", description = "alarm threshold for the collection usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d01")
   public void setMemoryCollectionUsageAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getMemoryMonitor().setCollectionUsageAlarmThreshold(threshold);
   }

   @JMXBeanAttribute(name = "MemoryMonitor.CollectionUsageShedThreshold", description = "shed threshold for the collection usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d04")
   public String getMemoryCollectionUsageShedThreshold() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getMemoryMonitor().getCollectionUsageShedThreshold();
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.CollectionUsageShedThreshold", description = "shed threshold for the collection usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d04")
   public void setMemoryCollectionUsageShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getMemoryMonitor().setCollectionUsageShedThreshold(threshold);
   }

   @JMXBeanAttribute(name = "MemoryMonitor.CollectionUsageValveThreshold", description = "valve threshold for the collection usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d05")
   public String getMemoryCollectionUsageValveThreshold() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getMemoryMonitor().getCollectionUsageValveThreshold();
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.CollectionUsageValveThreshold", description = "valve threshold for the collection usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d05")
   public void setMemoryCollectionUsageValveThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getMemoryMonitor().setCollectionUsageValveThreshold(threshold);
   }

   @JMXBeanAttribute(name = "MemoryMonitor.GarbageCollectionWaitTime (ms)", description = "time in ms after which a garbage collection is activated when a threshold is exceeded", sortValue = "d06")
   public String getGarbageCollectionWaitTime() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getMemoryMonitor().getGarbageCollectionWaitTime());
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.GarbageCollectionWaitTime (ms)", description = "time in ms after which a garbage collection is activated when a threshold is exceeded", sortValue = "d06")
   public void setGarbageCollectionWaitTime(String time) {
      if (NOT_AVAILABLE.equals(time)) return;
      actuator.getMemoryMonitor().setGarbageCollectionWaitTime(Integer.parseInt(time));
   }

   @JMXBeanAttribute(name = "MemoryMonitor.Status", description = "One of OFF, ON", sortValue = "d07")
   public String getMemoryMonitorStatus() {
      return actuator.getMemoryMonitor().getStatus().name();
   }

   @JMXBeanAttribute(name = "MemoryMonitor.Status", description = "One of OFF, ON", sortValue = "d07")
   public void setMemoryMonitorStatus(String mode) {
      actuator.getMemoryMonitor().setStatus(MonitorStatus.valueOf(mode.toUpperCase().trim()));
   }

   @JMXBeanAttribute(name = "MemoryMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "d08")
   public String getMemoryThrottleInterval() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getMemoryMonitor().getThrottleInterval());
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "d08")
   public void setMemoryThrottleInterval(String in) {
      if (NOT_AVAILABLE.equals(in)) return;
      try {
         long t = Long.parseLong(in);
         actuator.getMemoryMonitor().setThrottleInterval(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "d09")
   public String getMemoryThrottleMaxTime() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getMemoryMonitor().getThrottleMaxTime());
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "d09")
   public void setMemoryThrottleMaxTime(String time) {
      if (NOT_AVAILABLE.equals(time)) return;
      try {
         long t = Long.parseLong(time);
         actuator.getMemoryMonitor().setThrottleMaxTime(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.UsageAlarmThreshold", description = "alarm threshold for the usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d10")
   public String getMemoryUsageAlarmThreshold() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getMemoryMonitor().getUsageAlarmThreshold();
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.UsageAlarmThreshold", description = "alarm threshold for the usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d10")
   public void setMemoryUsageAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getMemoryMonitor().setUsageAlarmThreshold(threshold);
   }

   @JMXBeanAttribute(name = "MemoryMonitor.UsageShedThreshold", description = "shed threshold for the usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d11")
   public String getMemoryUsageShedThreshold() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getMemoryMonitor().getUsageShedThreshold();
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.UsageShedThreshold", description = "shed threshold for the usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d11")
   public void setMemoryUsageShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getMemoryMonitor().setUsageShedThreshold(threshold);
   }

   @JMXBeanAttribute(name = "MemoryMonitor.UsageValveThreshold", description = "valve threshold for the usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d12")
   public String getMemoryUsageValveThreshold() {
      if (actuator.getMemoryMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getMemoryMonitor().getUsageValveThreshold();
      }
   }

   @JMXBeanAttribute(name = "MemoryMonitor.UsageValveThreshold", description = "valve threshold for the usage of the tenured gen memory pool (in bytes or percent of the max pool size)", sortValue = "d12")
   public void setMemoryUsageValveThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getMemoryMonitor().setUsageValveThreshold(threshold);
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.AlarmThreshold", description = "Response time threshold (in ms or in percent of the average response time) for raising an alarm", sortValue = "f01")
   public String getResponseTimeAlarmThreshold() {
      if (actuator.getResponseTimeMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getResponseTimeMonitor().getAlarmThreshold();
      }
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.AlarmThreshold", description = "Response time threshold (in ms or in percent of the average response time) for raising an alarm", sortValue = "f01")
   public void setResponseTimeAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getResponseTimeMonitor().setAlarmThreshold(threshold);
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.ShedThreshold", description = "Maximum allowed response time (in ms or in percent of the average response time)", sortValue = "f02")
   public String getResponseTimeShedThreshold() {
      if (actuator.getResponseTimeMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getResponseTimeMonitor().getShedThreshold();
      }
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.ShedThreshold", description = "Maximum allowed response time (in ms or in percent of the average response time)", sortValue = "f02")
   public void setResponseTimeShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getResponseTimeMonitor().setShedThreshold(threshold);
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.ShedTime", description = "if the ResponseTimeMonitor.ShedThreshold is exceeded the actuator will deny following requests for ResponseTimeMonitor.ShedTime milliseconds.", sortValue = "f03")
   public String getResponseTimeShedTime() {
      if (actuator.getResponseTimeMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getResponseTimeMonitor().getShedTime();
      }
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.ShedTime", description = "if the ResponseTimeMonitor.ShedThreshold is exceeded the actuator will deny following requests for ResponseTimeMonitor.ShedTime milliseconds.", sortValue = "f03")
   public void setResponseTimeShedTime(String time) {
      if (NOT_AVAILABLE.equals(time)) return;
      actuator.getResponseTimeMonitor().setShedTime(time);
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.Status", description = "One of OFF, ON", sortValue = "f04")
   public String getResponseTimeStatus() {
      return actuator.getResponseTimeMonitor().getStatus().name();
   }

   @JMXBeanAttribute(name = "ResponseTimeMonitor.Status", description = "One of OFF, ON", sortValue = "f04")
   public void setResponseTimeStatus(String mode) {
      actuator.getResponseTimeMonitor().setStatus(MonitorStatus.valueOf(mode.toUpperCase().trim()));
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.AlarmThreshold", description = "Thread contention time threshold (in ms or in percent of the average thread contention time) for raising an alarm", sortValue = "h01")
   public String getThreadContentionAlarmThreshold() {
      if (actuator.getThreadContentionMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getThreadContentionMonitor().getAlarmThreshold();
      }
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.AlarmThreshold", description = "Thread contention time threshold (in ms or in percent of the average thread contention time) for raising an alarm", sortValue = "h01")
   public void setThreadContentionAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThreadContentionMonitor().setAlarmThreshold(threshold);
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.ShedThreshold", description = "Maximum allowed thread contention time (in ms or in percent of the average thread contention time)", sortValue = "h02")
   public String getThreadContentionShedThreshold() {
      if (actuator.getThreadContentionMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getThreadContentionMonitor().getShedThreshold();
      }
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.ShedThreshold", description = "Maximum allowed thread contention time (in ms or in percent of the average thread contention time)", sortValue = "h02")
   public void setThreadContentionShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThreadContentionMonitor().setShedThreshold(threshold);
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.ShedTime", description = "if the ThreadContentionMonitor.ShedThreshold is exceeded the actuator will deny following requests for ThreadContentionMonitor.ShedTime milliseconds.", sortValue = "h03")
   public String getThreadContentionShedTime() {
      if (actuator.getThreadContentionMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return actuator.getThreadContentionMonitor().getShedTime();
      }
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.ShedTime", description = "if the ThreadContentionMonitor.ShedThreshold is exceeded the actuator will deny following requests for ThreadContentionMonitor.ShedTime milliseconds.", sortValue = "h03")
   public void setThreadContentionShedTime(String time) {
      if (NOT_AVAILABLE.equals(time)) return;
      actuator.getThreadContentionMonitor().setShedTime(time);
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.Status", description = "One of OFF, ON", sortValue = "h04")
   public String getThreadContentionStatus() {
      return actuator.getThreadContentionMonitor().getStatus().name();
   }

   @JMXBeanAttribute(name = "ThreadContentionMonitor.Status", description = "One of OFF, ON", sortValue = "h04")
   public void setThreadContentionStatus(String mode) {
      actuator.getThreadContentionMonitor().setStatus(MonitorStatus.valueOf(mode.toUpperCase().trim()));
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.AlarmThreshold", description = "Maximum accepted parallel threads before an alarm is raised", sortValue = "k01")
   public String getThreadCountAlarmThreshold() {
      if (actuator.getThreadCountMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThreadCountMonitor().getAlarmThreshold());
      }
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.AlarmThreshold", description = "Maximum accepted parallel threads before an alarm is raised", sortValue = "k01")
   public void setThreadCountAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThreadCountMonitor().setAlarmThreshold(Integer.parseInt(threshold));
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ShedThreshold", description = "Maximum accepted parallel threads", sortValue = "k02")
   public String getThreadCountShedThreshold() {
      if (actuator.getThreadCountMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThreadCountMonitor().getShedThreshold());
      }
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ShedThreshold", description = "Maximum accepted parallel threads", sortValue = "k02")
   public void setThreadCountShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThreadCountMonitor().setShedThreshold(Integer.parseInt(threshold));
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.Status", description = "One of OFF, ON", sortValue = "k03")
   public String getThreadCountStatus() {
      return actuator.getThreadCountMonitor().getStatus().name();
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.Status", description = "One of OFF, ON", sortValue = "k03")
   public void setThreadCountStatus(String mode) {
      actuator.getThreadCountMonitor().setStatus((MonitorStatus.valueOf(mode.toUpperCase().trim())));
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "k04")
   public String getThreadCountThrottleInterval() {
      if (actuator.getThreadCountMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThreadCountMonitor().getThrottleInterval());
      }
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "k04")
   public void setThreadCountThrottleInterval(String in) {
      if (NOT_AVAILABLE.equals(in)) return;
      try {
         long t = Long.parseLong(in);
         actuator.getThreadCountMonitor().setThrottleInterval(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "k05")
   public String getThreadCountThrottleMaxTime() {
      if (actuator.getThreadCountMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThreadCountMonitor().getThrottleMaxTime());
      }
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "k05")
   public void setThreadCountThrottleMaxTime(String time) {
      if (NOT_AVAILABLE.equals(time)) return;
      try {
         long t = Long.parseLong(time);
         actuator.getThreadCountMonitor().setThrottleMaxTime(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ValveThreshold", description = "Maximum accepted parallel threads before threads are throttled by a valve", sortValue = "k06")
   public String getThreadCountValveThreshold() {
      if (actuator.getThreadCountMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThreadCountMonitor().getValveThreshold());
      }
   }

   @JMXBeanAttribute(name = "ThreadCountMonitor.ValveThreshold", description = "Maximum accepted parallel threads before threads are throttled by a valve", sortValue = "k06")
   public void setThreadCountValveThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThreadCountMonitor().setValveThreshold(Integer.parseInt(threshold));
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.AlarmCpuTimeThreshold", description = "Threshold for thread CPU time when an alarm is raised", sortValue = "m01")
   public String getThreadTimeAlarmCpuThreshold() {
      if (actuator.getThreadTimeMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThreadTimeMonitor().getAlarmCpuTimeThreshold());
      }
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.AlarmCpuTimeThreshold", description = "Threshold for thread CPU time when an alarm is raised", sortValue = "m01")
   public void setThreadTimeAlarmCpuThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThreadTimeMonitor().setAlarmCpuTimeThreshold(threshold);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.AlarmUserTimeThreshold", description = "Threshold for thread User time when an alarm is raised", sortValue = "m02")
   public String getThreadTimeAlarmUserThreshold() {
      if (actuator.getThreadTimeMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThreadTimeMonitor().getAlarmUserTimeThreshold());
      }
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.AlarmUserTimeThreshold", description = "Threshold for thread User time when an alarm is raised", sortValue = "m02")
   public void setThreadTimeAlarmUserThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThreadTimeMonitor().setAlarmUserTimeThreshold(threshold);
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Status", description = "One of OFF, ON", sortValue = "m03")
   public String getThreadTimeStatus() {
      return actuator.getThreadTimeMonitor().getStatus().name();
   }

   @JMXBeanAttribute(name = "ThreadTimeMonitor.Status", description = "One of OFF, ON", sortValue = "m03")
   public void setThreadTimeStatus(String mode) {
      actuator.getThreadTimeMonitor().setStatus(MonitorStatus.valueOf(mode.toUpperCase().trim()));
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.AlarmThreshold", description = "Maximum throughput per window before an alarm is raised", sortValue = "n01")
   public String getThroughputAlarmThreshold() {
      if (actuator.getThroughputMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThroughputMonitor().getAlarmThreshold());
      }
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.AlarmThreshold", description = "Maximum throughput per window before an alarm is raised", sortValue = "n01")
   public void setThroughputAlarmThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThroughputMonitor().setAlarmThreshold(Integer.parseInt(threshold));
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ShedThreshold", description = "Maximum throughput per window", sortValue = "n02")
   public String getThroughputShedThreshold() {
      if (actuator.getThroughputMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThroughputMonitor().getShedThreshold());
      }
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ShedThreshold", description = "Maximum throughput per window", sortValue = "n02")
   public void setThroughputShedThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThroughputMonitor().setShedThreshold(Integer.parseInt(threshold));
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.Status", description = "One of OFF, ON", sortValue = "n03")
   public String getThroughputStatus() {
      return actuator.getThroughputMonitor().getStatus().name();
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.Status", description = "One of OFF, ON", sortValue = "n03")
   public void setThroughputStatus(String mode) {
      actuator.getThroughputMonitor().setStatus(MonitorStatus.valueOf(mode.toUpperCase().trim()));
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "n04")
   public String getThroughputThrottleInterval() {
      if (actuator.getThroughputMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThroughputMonitor().getThrottleInterval());
      }
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ThrottleInterval", description = "interval in ms for checking the load during a throttle period.", sortValue = "n04")
   public void setThroughputThrottleInterval(String in) {
      if (NOT_AVAILABLE.equals(in)) return;
      try {
         long t = Long.parseLong(in);
         actuator.getThroughputMonitor().setThrottleInterval(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "n05")
   public String getThroughputThrottleMaxTime() {
      if (actuator.getThroughputMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThroughputMonitor().getThrottleMaxTime());
      }
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ThrottleMaxTime", description = "max time in ms that a request is throttled before it is eventually shed.", sortValue = "n05")
   public void setThroughputThrottleMaxTime(String time) {
      if (NOT_AVAILABLE.equals(time)) return;
      try {
         long t = Long.parseLong(time);
         actuator.getThroughputMonitor().setThrottleMaxTime(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ValveThreshold", description = "Maximum throughput per window before requests are throttled by a valve", sortValue = "n07")
   public String getThroughputValveThreshold() {
      if (actuator.getThroughputMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThroughputMonitor().getValveThreshold());
      }
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.ValveThreshold", description = "Maximum throughput per window before requests are throttled by a valve", sortValue = "n07")
   public void setThroughputValveThreshold(String threshold) {
      if (NOT_AVAILABLE.equals(threshold)) return;
      actuator.getThroughputMonitor().setValveThreshold(Integer.parseInt(threshold));
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.WindowWidth", description = "Window width in ms. Default is throughput per 1000 ms", sortValue = "n08")
   public String getThroughputWindowWidth() {
      if (actuator.getThroughputMonitor().getStatus() != MonitorStatus.ON) {
         return NOT_AVAILABLE;
      } else {
         return String.valueOf(actuator.getThroughputMonitor().getWindowWidth());
      }
   }

   @JMXBeanAttribute(name = "ThroughputMonitor.WindowWidth", description = "Window width in ms. Default is throughput per 1000 ms", sortValue = "n08")
   public void setThroughputWindowWidth(String width) {
      if (NOT_AVAILABLE.equals(width)) return;
      try {
         int t = Integer.parseInt(width);
         actuator.getThroughputMonitor().setWindowWidth(t);
      } catch (NumberFormatException e) {
         exceptionToString(e, "");
      }
   }

}
