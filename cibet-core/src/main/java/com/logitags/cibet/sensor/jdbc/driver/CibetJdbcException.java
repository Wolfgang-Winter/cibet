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
package com.logitags.cibet.sensor.jdbc.driver;

import com.logitags.cibet.core.CibetException;

public class CibetJdbcException extends CibetException {

   /**
    * 
    */
   private static final long serialVersionUID = -1757745122247411967L;

   public CibetJdbcException() {
      super();
   }

   public CibetJdbcException(String message, Throwable e) {
      super(message, e);
   }

   public CibetJdbcException(String message) {
      super(message);
   }

}
