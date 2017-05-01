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
package com.logitags.cibet.context;

import java.util.HashMap;

import javax.persistence.EntityManager;

import com.logitags.cibet.authentication.ChainedAuthenticationProvider;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.EventResult;

public interface InternalRequestScope extends RequestScope {

   String CONTROLEVENT = "__CONTROLEVENT";
   String CALLER_PRINCIPAL_NAME = "__CALLER_PRINCIPAL_NAME";
   String EVENTRESULT = "__EVENTRESULT";
   String CASEID = "__CASEID";
   String ENTITYMANAGER_TYPE = "__ENTITYMANAGER_TYPE";
   String REMARK = "__REMARK";
   String IS_POSTPONED = "__IS_POSTPONED";
   String AUDITED_BY_ENVERS = "__AUDITED_BY_ENVERS";
   String DCCONTROLLABLE = "__DCCONTROLLABLE";
   String AUTHENTICATIONPROVIDER = "__CIBET_AUTHENTICATIONPROVIDER";
   String IGNORE_SCHEDULEDEXCEPTION = "__IGNORE_SCHEDULEDEXCEPTION";
   String MANAGED = "__MANAGED";
   String HTTPRESPONSESTATUS = "__HTTPRESPONSESTATUS";
   String PLAYING_MODE = "__PLAYING_MODE";
   String SCHEDULED_DATE = "__SCHEDULED_DATE";
   String CONTEXTEJB_JNDINAME = "__CONTEXTEJB_JNDINAME";
   String GROUP_ID = "__GROUP_ID";

   /**
    * clears all properties
    */
   void clear();

   /**
    * registers the given new EventResult. It is either added as new root of the EventResult tree or added to the tail
    * of the childrens list of the last EventResult in status EXECUTING.
    * 
    * @param thisResult
    * @return
    */
   EventResult registerEventResult(EventResult thisResult);

   void setApplicationEntityManager2(EntityManager manager);

   /**
    * Return the applications INTERNAL EntityManager instance or null if not set.
    * 
    * @return
    */
   EntityManager getApplicationEntityManager2();

   /**
    * sets the applications EntityManager for JPA sensors for the application entities.
    * 
    * @param manager
    */
   void setApplicationEntityManager(EntityManager manager);

   /**
    * returns the applications EntityManager that is used to persist the applications entities (not Cibet entities)
    * 
    * @return
    * @throws CibetException
    *            if no EntityManager set in context
    */
   EntityManager getApplicationEntityManager();

   /**
    * Return the Cibet EntityManager instance. Could be null, if not set in context.
    * 
    * @return
    */
   EntityManager getNullableEntityManager();

   /**
    * returns the applications EntityManager that is used to persist the applications entities (not Cibet entities).
    * Returns null if no EntityManager set in context
    * 
    * @return
    */
   EntityManager getNullableApplicationEntityManager();

   void setAuditedByEnvers(boolean flag);

   boolean isAuditedByEnvers();

   /**
    * returns the AuthenticationProvider either from the Request context or resolved in the Configuration
    * initialisation.
    * 
    * @return
    */
   ChainedAuthenticationProvider getAuthenticationProvider();

   /**
    * returns true if the current thread is managed by Cibet. That means initialised and cleaned up. This is true when
    * one of the HTTP CibetFilters or the CibetContextInterceptor is used.
    * 
    * @return
    */
   boolean isManaged();

   /**
    * 
    * set true if the current thread is managed by Cibet. That means initialised and cleaned up. This is set to true by
    * the HTTP CibetFilters and the CibetContextInterceptor.
    * 
    * @param b
    */
   void setManaged(boolean b);

   /**
    * get all properties of this request context
    * 
    * @return
    */
   HashMap<String, Object> getProperties();

}
