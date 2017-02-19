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
package com.logitags.cibet.sensor.jdbc.bridge;


/**
 * Generator for unique IDs.
 * 
 */
public interface IdGenerator {

   /**
    * generates the next unique ID for the given sequence.
    * 
    * @param sequence
    *           sequence name
    * @return
    */
   long nextId(String sequence);
}
