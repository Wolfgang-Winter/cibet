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
package com.logitags.cibet.actuator.scheduler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.diff.Difference;

/**
 * Thrown when a business case shall be scheduled but another event on the same resource is already scheduled.
 * 
 */
public class ScheduledException extends CibetException {

   /**
    * 
    */
   private static final long serialVersionUID = 1613800779279337837L;

   private Map<Controllable, List<Difference>> scheduledResources = new HashMap<Controllable, List<Difference>>();

   public ScheduledException(Map<Controllable, List<Difference>> map) {
      scheduledResources = map;
   }

   /**
    * in case the Controllable represents an already scheduled UPDATE event, the returned list contains the differences
    * that are scheduled to be updated. For other control events null is returned.
    * 
    * @param co
    *           Controllable
    * @return the differences
    */
   public List<Difference> getDifferences(Controllable co) {
      return scheduledResources.get(co);
   }

   /**
    * returns the list of already scheduled business cases.
    * 
    * @return list of Controllable
    */
   public Set<Controllable> getScheduledControllables() {
      return scheduledResources.keySet();
   }

}
