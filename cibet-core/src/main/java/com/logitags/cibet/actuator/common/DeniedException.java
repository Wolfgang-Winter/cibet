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


public class DeniedException extends CibetException {

   /**
	 * 
	 */
   private static final long serialVersionUID = -7557529218247165624L;

   private String deniedUser;

   public DeniedException(String user) {
      super();
      deniedUser = user;
   }

   public DeniedException(String message, Throwable e, String user) {
      super(message, e);
      deniedUser = user;
   }

   public DeniedException(String message, String user) {
      super(message);
      deniedUser = user;
   }

   /**
    * @return the deniedUser
    */
   public String getDeniedUser() {
      return deniedUser;
   }

}
