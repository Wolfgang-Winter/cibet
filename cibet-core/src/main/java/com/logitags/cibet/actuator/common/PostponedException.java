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

import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.core.CibetException;

/**
 * Thrown when the event is not executed instantaneously, but postponed.
 * 
 */
public class PostponedException extends CibetException {

   /**
	 * 
	 */
   private static final long serialVersionUID = 1613800779279337837L;

   /**
    * The DcControllable object that has been created and stored in the
    * database.
    */
   private DcControllable dcControllable;

   /**
    * @return the dcControllable
    */
   public DcControllable getDcControllable() {
      return dcControllable;
   }

   /**
    * @param dcControllable
    *           the dcControllable to set
    */
   public void setDcControllable(DcControllable dcControllable) {
      this.dcControllable = dcControllable;
   }

}
