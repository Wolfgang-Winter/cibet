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
import org.hibernate.envers.event.spi.EnversPreCollectionRemoveEventListenerImpl;
import org.hibernate.event.spi.PreCollectionRemoveEvent;

import com.logitags.cibet.context.Context;

public class CibetPreCollectionRemoveEventListener extends EnversPreCollectionRemoveEventListenerImpl {

   /**
    *  
    */
   private static final long serialVersionUID = 1L;

   private transient Log log = LogFactory.getLog(CibetPreCollectionRemoveEventListener.class);

   protected CibetPreCollectionRemoveEventListener(AuditConfiguration enversConfiguration) {
      super(enversConfiguration);
   }

   /*
    * (non-Javadoc)
    * 
    * @see org.hibernate.envers.event.EnversPreCollectionRemoveEventListenerImpl#
    * onPreRemoveCollection(org.hibernate.event.spi.PreCollectionRemoveEvent)
    */
   @Override
   public void onPreRemoveCollection(PreCollectionRemoveEvent event) {
      if (!Context.internalRequestScope().isAuditedByEnvers()) {
         String entityName = event.getAffectedOwnerEntityName();
         log.debug(entityName + " not audited by Cibet configuration");
         return;
      }
      super.onPreRemoveCollection(event);
   }

}
