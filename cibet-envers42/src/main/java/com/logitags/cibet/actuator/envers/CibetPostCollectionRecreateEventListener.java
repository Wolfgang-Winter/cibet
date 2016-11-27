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
import org.hibernate.envers.configuration.AuditConfiguration;
import org.hibernate.envers.event.EnversPostCollectionRecreateEventListenerImpl;
import org.hibernate.event.spi.PostCollectionRecreateEvent;

import com.logitags.cibet.context.Context;

public class CibetPostCollectionRecreateEventListener extends
      EnversPostCollectionRecreateEventListenerImpl {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private transient Log log = LogFactory
         .getLog(CibetPostCollectionRecreateEventListener.class);

   protected CibetPostCollectionRecreateEventListener(
         AuditConfiguration enversConfiguration) {

      super(enversConfiguration);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * org.hibernate.envers.event.EnversPostCollectionRecreateEventListenerImpl
    * #onPostRecreateCollection
    * (org.hibernate.event.spi.PostCollectionRecreateEvent)
    */
   @Override
   public void onPostRecreateCollection(PostCollectionRecreateEvent event) {
      String entityName = event.getAffectedOwnerEntityName();
      if (!Context.internalRequestScope().isAuditedByEnvers()) {
         log.debug(entityName
               + " owner entity NOT audited by Cibet configuration");
         return;
      } else {
         log.debug(entityName + " owner entity audited by Cibet configuration");
      }
      super.onPostRecreateCollection(event);
   }

}
