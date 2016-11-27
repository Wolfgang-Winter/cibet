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
 * Thrown when a control operation is done and an unapproved controlled object has been found.
 */
public class UnapprovedResourceException extends CibetException {
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private DcControllable unapprovedResource;

   public UnapprovedResourceException(String message, DcControllable dc) {
      super(message);
      unapprovedResource = dc;
   }

   public UnapprovedResourceException(DcControllable dc) {
      unapprovedResource = dc;
   }

   /**
    * @return the unapprovedResource
    */
   public DcControllable getUnapprovedResource() {
      return unapprovedResource;
   }

}
