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
package com.cibethelper.loadcontrol;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.loadcontrol.AbstractMonitor;
import com.logitags.cibet.actuator.loadcontrol.LoadControlCallback;
import com.logitags.cibet.actuator.loadcontrol.MonitorResult;
import com.logitags.cibet.actuator.loadcontrol.MonitorStatus;
import com.logitags.cibet.actuator.loadcontrol.ThreadTimeMonitor;
import com.logitags.cibet.actuator.loadcontrol.ThreadTimeMonitorTest;
import com.logitags.cibet.core.EventMetadata;

/**
 * monitors thread cpu time and thread user time. Supports alarm.
 * 
 */
public class DummyMonitor extends AbstractMonitor {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static Log log = LogFactory.getLog(ThreadTimeMonitor.class);

   public static final String NAME = "DUMMY";

   public DummyMonitor() {
      log.debug("constructor DummyMonitor");
   }

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback alarm, EventMetadata metadata,
         String currentSetpointId) {
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON) {
         return previousResult;
      }

      log.info("beforeEvent DummyMonitor");
      ThreadTimeMonitorTest.customCounter.incrementAndGet();
      return MonitorResult.PASSED;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String spId) {
      if (status != MonitorStatus.ON)
         return;
      log.info("afterEvent DummyMonitor");
   }

   @Override
   public void reset(String setpointId) {
      log.info("reset DummyMonitor with SP " + setpointId);
   }

   @Override
   public void close() {
      log.info("close DummyMonitor");
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.loadcontrol.AbstractMonitor#getName()
    */
   @Override
   public String getName() {
      return NAME;
   }

}
