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
package com.cibethelper.base;

import com.logitags.cibet.actuator.common.AbstractActuator;

/**
 * implementation of Actuator that does no control.
 */
public class NoControlActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -2978869706367131299L;

   public static final String DEFAULTNAME = "NO_CONTROL";

   public NoControlActuator() {
      setName(DEFAULTNAME);
   }

   public NoControlActuator(String name) {
      setName(name);
   }

   public String getNameForSpringTest() {
      return DEFAULTNAME;
   }

}
