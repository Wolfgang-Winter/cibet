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
package com.logitags.cibet.context;

import java.util.HashMap;

public class ThreadLocalMap extends
      InheritableThreadLocal<HashMap<String, Object>> {

   public final HashMap<String, Object> childValue(
         HashMap<String, Object> parentValue) {
      if (parentValue != null) {
         return (HashMap<String, Object>) parentValue.clone();
      } else {
         return null;
      }
   }

}
