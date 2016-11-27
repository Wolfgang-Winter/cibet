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
package com.logitags.cibet.core;

public class CibetException extends RuntimeException {

   /**
	 * 
	 */
   private static final long serialVersionUID = -8526215111320678983L;

   public CibetException() {
      super();
   }

   public CibetException(String message) {
      super(message);
   }

   public CibetException(String message, Throwable e) {
      super(message, e);
   }

}
