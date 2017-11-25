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
import org.hibernate.envers.configuration.spi.AuditConfiguration;
import org.hibernate.envers.event.spi.EnversPostDeleteEventListenerImpl;
import org.hibernate.event.spi.PostDeleteEvent;

import com.logitags.cibet.context.Context;

public class CibetPostDeleteEventListener extends EnversPostDeleteEventListenerImpl {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private transient Log log = LogFactory.getLog(CibetPostDeleteEventListener.class);

   protected CibetPostDeleteEventListener(AuditConfiguration enversConfiguration) {
      super(enversConfiguration);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.hibernate.envers.event.EnversPostDeleteEventListenerImpl#onPostDelete
    * (org.hibernate.event.spi.PostDeleteEvent)
    */
   @Override
   public void onPostDelete(PostDeleteEvent event) {
      if (!Context.internalRequestScope().isAuditedByEnvers()) {
         String entityName = event.getPersister().getEntityName();
         log.debug(entityName + " not audited by Cibet configuration");
         return;
      }
      super.onPostDelete(event);
   }

}
