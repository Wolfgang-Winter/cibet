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

//import javax.ejb.ApplicationException;


//@ApplicationException
public class XDeniedEjbException extends DeniedException {

   /**
    * 
    */
   private static final long serialVersionUID = 4629067956898863304L;

   public XDeniedEjbException(String user) {
      super(user);
   }

   public XDeniedEjbException(String message, Throwable e, String user) {
      super(message, e, user);
   }

   public XDeniedEjbException(String message, String user) {
      super(message, user);
   }

}
