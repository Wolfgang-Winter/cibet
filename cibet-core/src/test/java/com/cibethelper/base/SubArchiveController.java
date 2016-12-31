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
package com.cibethelper.base;

import com.logitags.cibet.actuator.archive.ArchiveActuator;

/**
 * 
 */
public class SubArchiveController extends ArchiveActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.archive.ArchiveController#getSchemeName()
    */
   @Override
   public String getName() {
      return "SubArchiveTestController";
   }

}
