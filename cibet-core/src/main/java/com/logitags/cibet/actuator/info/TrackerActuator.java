/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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
package com.logitags.cibet.actuator.info;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.RequestScopeContext;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;

/**
 * This actuator stores the current EventResult tree into the database.
 * 
 * @author Wolfgang
 * 
 */
public class TrackerActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -3522844045467776133L;

   private static Log log = LogFactory.getLog(TrackerActuator.class);

   public static final String DEFAULTNAME = "TRACKER";

   public TrackerActuator() {
      setName(DEFAULTNAME);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#afterEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      log.debug("execute TrackerActuator afterEvent");
      if (Context.requestScope().isPlaying()) {
         return;
      }

      EventResult parent = (EventResult) Context.internalRequestScope().getProperty(RequestScopeContext.EVENTRESULT);
      log.debug(parent);
      if (parent != null) {
         if (parent.getEventResultId() == null) {
            Context.internalRequestScope().getOrCreateEntityManager(true).persist(parent);
         } else {
            EventResult lastResult = parent.getLastExecutingEventResult();
            Context.internalRequestScope().getOrCreateEntityManager(true).persist(lastResult);
         }
      }
   }

}
