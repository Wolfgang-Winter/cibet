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

import java.util.List;

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
public class StateChangeControl extends AbstractControl {

   /**
    * 
    */
   private static final long serialVersionUID = 2188075469227544287L;

   private static Log log = LogFactory.getLog(StateChangeControl.class);

   public static final String NAME = "stateChange";

   private boolean isInConstrained(List<String> constraints, List<Difference> diffs) {
      for (Difference diff : diffs) {
         if (constraints.contains(diff.getCanonicalPath())) {
            return true;
         }
      }
      return false;
   }

   private boolean isExConstrained(List<String> constraints, final List<Difference> diffs) {
      if (diffs.size() == 0)
         return true;
      for (Difference diff : diffs) {
         // log.debug(diff);
         if (!constraints.contains(diff.getCanonicalPath())) {
            return true;
         }
      }
      return false;
   }

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public boolean hasControlValue(Object cv) {
      BooleanAttributedControlValue value = (BooleanAttributedControlValue) cv;
      if (value == null || value.getValues().isEmpty()
            || (value.getValues().size() == 1 && value.getValues().get(0).length() == 0)) {
         return false;
      } else {
         return true;
      }
   }

   @Override
   public boolean evaluate(Object controlValue, EventMetadata metadata) {
      if (ControlEvent.UPDATE != metadata.getControlEvent()) {
         log.debug("skip StateChange evaluation: only for UPDATE control events");
         return true;
      }

      if (metadata.getResource().getUnencodedTargetObject() == null) {
         String msg = "failed to execute StateChange evaluation: Object is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      List<Difference> diffs = PersistenceUtil.getDirtyUpdates(metadata);
      BooleanAttributedControlValue invokerValue = (BooleanAttributedControlValue) controlValue;
      if (invokerValue.isBooleanValue()) {
         // isExclude
         if (isExConstrained(invokerValue.getValues(), diffs)) {
            return true;
         }
      } else {
         if (isInConstrained(invokerValue.getValues(), diffs)) {
            return true;
         }
      }

      return false;
   }

}
