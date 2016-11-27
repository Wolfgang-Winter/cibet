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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.EventMetadata;

/**
 * monitors thread cpu time and thread user time. Supports alarm.
 * 
 */
public class ThreadTimeMonitor2 extends AbstractMonitor {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static Log log = LogFactory.getLog(ThreadTimeMonitor.class);

   public ThreadTimeMonitor2() {
      log.debug("constructor ThreadTimeMonitor2");
   }

   @Override
   public MonitorResult beforeEvent(MonitorResult previousResult, LoadControlCallback alarm, EventMetadata metadata,
         String currentSetpointId) {
      log.info("beforeEvent ThreadTimeMonitor2");
      if (previousResult == MonitorResult.SHED || status != MonitorStatus.ON) return previousResult;
      return MonitorResult.PASSED;
   }

   @Override
   public void afterEvent(LoadControlCallback callback, EventMetadata metadata, String spId) {
      log.info("afterEvent ThreadTimeMonitor2");
   }

   @Override
   public void reset(String setpointId) {
      log.info("reset ThreadTimeMonitor2 with SP " + setpointId);
   }

   @Override
   public void close() {
      log.info("close ThreadTimeMonitor2");
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.loadcontrol.AbstractMonitor#getName()
    */
   @Override
   public String getName() {
      return "ThreadTimeMonitor2ForTesting";
   }

}
