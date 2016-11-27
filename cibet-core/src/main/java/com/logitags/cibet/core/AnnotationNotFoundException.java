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

/**
 * thrown if an annotation could not be found.
 */
public class AnnotationNotFoundException extends RuntimeException {
   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public AnnotationNotFoundException() {
      super();
   }

   public AnnotationNotFoundException(String message) {
      super(message);
   }

}
