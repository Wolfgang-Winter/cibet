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

package com.logitags.cibet.actuator.dc;

import com.logitags.cibet.core.CibetException;

/**
 *
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
