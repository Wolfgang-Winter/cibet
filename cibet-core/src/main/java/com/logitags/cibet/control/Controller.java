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
package com.logitags.cibet.control;

import com.logitags.cibet.core.EventMetadata;

/**
 *
 */
public interface Controller {

   /**
    * Evaluates the business case and detects setpoints and actuators to apply.
    * 
    * @param metadata
    */
   void evaluate(EventMetadata metadata);
}
