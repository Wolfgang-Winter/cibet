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
/**
 * 
 */
package com.logitags.cibet.actuator.common;

import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.core.EventMetadata;

/**
 * defines the controlling methods relating to persistence and invocation
 * actions for an actuator.
 */
public interface Actuator {

   /**
    * return the unique Actuator name.
    * 
    * @return actuator name
    */
   String getName();

   /**
    * sets the unique Actuator name.
    * 
    * @param name
    *           actuator name
    */
   void setName(String name);

   /**
    * executed by the framework before the event.
    * 
    * @param ctx
    *           EventMetadata
    */
   void beforeEvent(EventMetadata ctx);

   /**
    * executed by the framework after the event.
    * 
    * @param ctx
    *           EventMetadata
    */
   void afterEvent(EventMetadata ctx);

   /**
    * the init() method allows initialisation work to be done for the actuator.
    * This method is called by the framework when an actuator is registered.
    * 
    * @param config
    *           Configuration
    */
   void init(Configuration config);

   /**
    * the close() method allows uninitialisation work to be done for the
    * actuator. This method is called by the framework when the actuator is
    * unregistered.
    */
   void close();
}
