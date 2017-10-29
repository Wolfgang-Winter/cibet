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

import java.util.Set;

import com.logitags.cibet.core.EventMetadata;

/**
 * An evaluation for a setpoint.
 */
public interface Control {

   /**
    * returns the unique name of the control
    * 
    * @return
    */
   String getName();

   /**
    * evaluates the include or exclude control values against the metadata.
    * 
    * @param values
    *           the values of the control as configured
    * @param metadata
    * @return true if matches, false if not matches, null if not applicable
    */
   Boolean evaluate(Set<String> values, EventMetadata metadata);
}
