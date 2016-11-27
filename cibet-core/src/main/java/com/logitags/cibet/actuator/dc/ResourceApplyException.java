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
package com.logitags.cibet.actuator.dc;

/**
 * Exception is thrown if during apply() of a resource an application exception
 * is thrown.
 */
public class ResourceApplyException extends Exception {

   /**
	 * 
	 */
   private static final long serialVersionUID = 7986891208484517177L;

   public ResourceApplyException(String msg) {
      super(msg);
   }

   public ResourceApplyException(String msg, Throwable e) {
      super(msg, e);
   }

   public ResourceApplyException(Throwable e) {
      super(e);
   }
}
