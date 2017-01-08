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

package com.logitags.cibet.actuator.common;

import com.logitags.cibet.core.CibetException;

/**
 * Thrown if the current user is not allowed to execute an action or if no user is set in Cbeit context at all.
 */
public class InvalidUserException extends CibetException {
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public InvalidUserException(String msg) {
      super(msg);
   }

}
