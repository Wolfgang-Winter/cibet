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
    * resolves the String input value for this Control from the cibet-config.xml
    * file into a format/type that is used in the evaluate method.
    * 
    * @param configValue
    * @return
    */
   Object resolve(String configValue);

   /**
    * Returns true if the controlValue has defined a condition for this Control.
    * Normally this is the case if controlValue is not null and has at least one
    * element if controlValue is a List and the first element is a String with
    * length greater 0, or if controlValue is a String with length greater 0.
    * 
    * @param controlValue
    * @return
    */
   boolean hasControlValue(Object controlValue);

   /**
    * evaluates the control value against the metadata.
    * 
    * @param controlValue
    * @param metadata
    * @return
    */
   boolean evaluate(Object controlValue, EventMetadata metadata);
}
