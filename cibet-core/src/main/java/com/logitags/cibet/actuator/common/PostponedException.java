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
 * Thrown when the event is not executed instantaneously, but postponed.
 * 
 */
public class PostponedException extends CibetException {

   /**
    * 
    */
   private static final long serialVersionUID = 1613800779279337837L;

   /**
    * The Controllable object that has been created and stored in the database.
    */
   private Controllable controllable;

   /**
    * @return the controllable
    */
   public Controllable getControllable() {
      return controllable;
   }

   /**
    * @param controllable
    *           the Controllable to set
    */
   public void setControllable(Controllable controllable) {
      this.controllable = controllable;
   }

}
