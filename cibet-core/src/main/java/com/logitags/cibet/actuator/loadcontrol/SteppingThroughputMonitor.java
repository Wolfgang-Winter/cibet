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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;

public class SteppingThroughputMonitor extends AbstractMonitor {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static Log log = LogFactory.getLog(SteppingThroughputMonitor.class);

   private Map<String, ThroughputCounter> secCurrentCounter = new ConcurrentHashMap<String, ThroughputCounter>();
   private Map<String, ThroughputCounter> secLastCounter = new ConcurrentHashMap<String, ThroughputCounter>();

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback callback, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON) return previousResult;

      long now = System.currentTimeMillis();
      if (now - 2000 > secCurrentCounter.get(currentSetpointId).getTimestamp()) {
         // no last window
         secLastCounter.put(currentSetpointId, new ThroughputCounter(now - 1000));
         secCurrentCounter.put(currentSetpointId, new ThroughputCounter(now, 1));
         // log.debug("no last window: new window");

      } else if (now - 1000 > secCurrentCounter.get(currentSetpointId).getTimestamp()) {
         // new window
         long timestamp = secCurrentCounter.get(currentSetpointId).getTimestamp();
         secLastCounter.put(currentSetpointId, secCurrentCounter.get(currentSetpointId));
         secCurrentCounter.put(currentSetpointId, new ThroughputCounter(timestamp + 1000, 1));
         // log.debug("new window");

      } else {
         // in current window
         secCurrentCounter.get(currentSetpointId).incrementAndGet();
         // log.debug("current throughput count=" + currentCount);
      }
      return MonitorResult.PASSED;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String currentSetpointId) {
   }

   @Override
   public void reset(String setpointId) {
      long now = System.currentTimeMillis();
      log.debug("reset ThroughputMonitor " + now);
      secCurrentCounter.put(setpointId, new ThroughputCounter(now));
      secLastCounter.put(setpointId, new ThroughputCounter(now - 1000));

   }

   public int getSecThroughput(String setpoint) {
      long now = System.currentTimeMillis();
      int result;
      if (now - 2000 > secCurrentCounter.get(setpoint).getTimestamp()) {
         // no last window
         // log.debug("GET: no last window: 0");
         result = 0;
      } else if (now - 1000 > secCurrentCounter.get(setpoint).getTimestamp()) {
         // no current window
         // log.debug("GET: one after current window: " + secCurrentCounter.get(setpoint).getCounter());
         result = secCurrentCounter.get(setpoint).getCounter();
      } else {
         // log.debug("GET: in current window: " + secLastCounter.get(setpoint).getCounter());
         result = secLastCounter.get(setpoint).getCounter();
      }
      return result;
   }
}
