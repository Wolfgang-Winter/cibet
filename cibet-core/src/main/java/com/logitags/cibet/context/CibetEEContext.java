/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.context;

import com.logitags.cibet.core.EventMetadata;

public interface CibetEEContext {

   /**
    * executed before invocation of the method.
    * 
    * @param ctx
    */
   void beforeEvent(EventMetadata ctx);

   /**
    * executed after invocation of the method.
    * 
    * @param ctx
    */
   void afterEvent(EventMetadata ctx);

   /**
    * sets an EntityManager from an injected EntityManagerFactory.
    * 
    * @return true if EntityManager has been successfully set.
    */
   boolean setEntityManagerIntoContext();

   /**
    * sets the Caller Principal name from the SessionContext into CibetContext
    * 
    */
   void setCallerPrincipalNameIntoContext();

}
