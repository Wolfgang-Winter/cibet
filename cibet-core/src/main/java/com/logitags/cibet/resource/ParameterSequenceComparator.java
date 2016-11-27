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
package com.logitags.cibet.resource;

import java.io.Serializable;
import java.util.Comparator;


public class ParameterSequenceComparator implements
      Comparator<ResourceParameter>, Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = -492150864778027307L;

   @Override
   public int compare(ResourceParameter o1, ResourceParameter o2) {
      if (o1 == null || o2 == null) return 0;
      return Integer.valueOf(o1.getSequence()).compareTo(
            Integer.valueOf(o2.getSequence()));
   }

}
