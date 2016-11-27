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

import com.logitags.cibet.actuator.dc.DcControllable;
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

   private Map<DcControllable, List<Difference>> scheduledResources = new HashMap<DcControllable, List<Difference>>();

   public ScheduledException(Map<DcControllable, List<Difference>> map) {
      scheduledResources = map;
   }

   /**
    * in case the DcControllable represents an already scheduled UPDATE event, the returned list contains the
    * differences that are scheduled to be updated. For other control events null is returned.
    * 
    * @param co
    *           DcControllable
    * @return the differences
    */
   public List<Difference> getDifferences(DcControllable co) {
      return scheduledResources.get(co);
   }

   /**
    * returns the list of already scheduled business cases.
    * 
    * @return list of DcControllable
    */
   public Set<DcControllable> getScheduledDcControllables() {
      return scheduledResources.keySet();
   }

}
