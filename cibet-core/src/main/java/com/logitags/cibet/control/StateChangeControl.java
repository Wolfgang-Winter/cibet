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
package com.logitags.cibet.control;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.PersistenceUtil;

/**
 * evaluates state of object attributes against configured setpoints. This evaluation is executed only for UPDATE
 * control events. If one of the attributes listed in excludeStateChange tag is modified, this setpoint is skipped. If
 * one of the attributes listed in includeStateChange tag is modified, this setpoint is executed.
 * <p>
 * Attributes in excludeStateChange and includeStateChange tags are comma separated. Attributes in associated objects
 * can be given with the point notation. If an object has an attribute address which in turn has an attribute city this
 * is written with address.city.
 */
public class StateChangeControl implements Serializable, Control {

   /**
    * 
    */
   private static final long serialVersionUID = 2188075469227544287L;

   private static Log log = LogFactory.getLog(StateChangeControl.class);

   public static final String NAME = "stateChange";

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public Boolean evaluate(Set<String> values, EventMetadata metadata) {
      if (metadata == null) {
         String msg = "failed to execute stateChange evaluation: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (ControlEvent.UPDATE != metadata.getControlEvent()) {
         log.debug("skip StateChange evaluation: only for UPDATE control events");
         return null;
      }

      if (values == null || values.isEmpty()) return null;

      if (metadata.getResource().getUnencodedTargetObject() == null) {
         String msg = "failed to execute StateChange evaluation: Object is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      List<Difference> diffs = PersistenceUtil.getDirtyUpdates(metadata);

      for (Difference diff : diffs) {
         if (values.contains(diff.getCanonicalPath())) {
            return true;
         }
      }
      return false;
   }

}
