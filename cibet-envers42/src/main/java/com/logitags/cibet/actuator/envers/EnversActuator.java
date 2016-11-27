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
package com.logitags.cibet.actuator.envers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.EventMetadata;

/**
 * controls auditing of entity persistence with Hibernate Envers
 * 
 * @author Wolfgang
 * 
 */
public class EnversActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private transient Log log = LogFactory.getLog(EnversActuator.class);

   public static final String DEFAULTNAME = "ENVERS";

   public EnversActuator() {
      setName(DEFAULTNAME);
   }

   public EnversActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#beforeEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      log.debug("EnversActuator beforeEvent");
      Context.internalRequestScope().setAuditedByEnvers(true);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#afterEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      log.debug("EnversActuator afterEvent");
      Context.internalRequestScope().getApplicationEntityManager().flush();
      log.debug("EnversActuator after flush");
      Context.internalRequestScope().setAuditedByEnvers(false);
   }

}
