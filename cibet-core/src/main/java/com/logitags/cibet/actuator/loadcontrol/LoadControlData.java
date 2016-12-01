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

import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.resource.Resource;

/**
 * metadata for the methods of the LoadControlCallback interface.
 * 
 */
public class LoadControlData {

   public LoadControlData(String spid, Resource r, ControlEvent ce, String mon, MonitorResult monResult) {
      setpointId = spid;
      resource = r;
      controlEvent = ce;
      monitor = mon;
      monitorResult = monResult;
   }

   /**
    * the id of the Setpoint that was applied on this business case
    */
   private String setpointId;

   /**
    * the Resource (business case/request). The content, e.g. target type, of the Resource object depends on the
    * business case:<br>
    * JPA: entity class and primary key, query<br>
    * EJB, method call: class name, method name, method parameters<br>
    * HTTP request: HttpRequest, HttpResponse, URL, http parameters, http body <br>
    * JDBC: table name, SQL statement<br>
    */
   private Resource resource;

   /**
    * the requested event
    */
   private ControlEvent controlEvent;

   /**
    * the name of the monitor that led to the callback.
    */
   private String monitor;

   /**
    * The value name if a monitor monitors more than one value, otherwise same as monitor.
    */
   private String monitoredValue;

   /**
    * the configured threshold for the monitored value.
    */
   private String threshold;

   /**
    * the actual monitored value
    */
   private String value;

   /**
    * Total time in ms that a business case was throttled by a valve. Only greater 0 if a valve was configured and
    * applied.
    */
   private long throttleTime = 0;

   /**
    * the number of times that the throughput has reached or exceeded the threshold.
    */
   private long alarmCount;

   /**
    * Type of LoadControl
    */
   private MonitorResult monitorResult;

   public MonitorResult getMonitorResult() {
      return monitorResult;
   }

   public void setMonitorResult(MonitorResult monitorResult) {
      this.monitorResult = monitorResult;
   }

   /**
    * @return the setpointId
    */
   public String getSetpointId() {
      return setpointId;
   }

   /**
    * @return the resource
    */
   public Resource getResource() {
      return resource;
   }

   /**
    * @return the controlEvent
    */
   public ControlEvent getControlEvent() {
      return controlEvent;
   }

   /**
    * @return the monitor
    */
   public String getMonitor() {
      return monitor;
   }

   /**
    * @return the monitoredValue
    */
   public String getMonitoredValue() {
      return monitoredValue;
   }

   /**
    * @param monitoredValue
    *           the monitoredValue to set
    */
   public void setMonitoredValue(String monitoredValue) {
      this.monitoredValue = monitoredValue;
   }

   /**
    * @return the threshold
    */
   public String getThreshold() {
      return threshold;
   }

   /**
    * @param threshold
    *           the threshold to set
    */
   public void setThreshold(String threshold) {
      this.threshold = threshold;
   }

   /**
    * @return the value
    */
   public String getValue() {
      return value;
   }

   /**
    * @param value
    *           the value to set
    */
   public void setValue(String value) {
      this.value = value;
   }

   /**
    * @return the throttleTime
    */
   public long getThrottleTime() {
      return throttleTime;
   }

   /**
    * @param throttleTime
    *           the throttleTime to set
    */
   public void setThrottleTime(long throttleTime) {
      this.throttleTime = throttleTime;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("Monitor: ");
      b.append(monitor);
      b.append("/");
      b.append(monitoredValue);
      b.append(" Setpoint: ");
      b.append(setpointId);
      b.append(" [");
      b.append(monitorResult);
      b.append(" threshold: ");
      b.append(threshold);
      b.append(" /is: ");
      b.append(value);
      b.append(" /throttleTime: ");
      b.append(throttleTime);
      b.append(" /alarmCount: ");
      b.append(alarmCount);
      b.append("]");

      return b.toString();
   }

   /**
    * the number of times that the throughput has reached or exceeded the threshold.
    * 
    * @return the alarmCount
    */
   public long getAlarmCount() {
      return alarmCount;
   }

   /**
    * the number of times that the throughput has reached or exceeded the threshold.
    * 
    * @param alarmCount
    *           the alarmCount to set
    */
   public void setAlarmCount(long alarmCount) {
      this.alarmCount = alarmCount;
   }

}
