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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;

/**
 * evaluates the current event against configured setpoints. If the sensor/metadata contains IMPLICIT ControlEvent the
 * configuration in the setpoint is ignored.
 */
public class EventControl extends AbstractControl {

   /**
    * 
    */
   private static final long serialVersionUID = 5677366782801007178L;
   private static Log log = LogFactory.getLog(EventControl.class);

   public static final String NAME = "event";

   /**
    * returns true if metadata.getControlEvent() == ControlEvent.IMPLICIT
    */
   public boolean evaluate(Object controlValue, EventMetadata metadata) {
      if (metadata == null || metadata.getControlEvent() == null) {
         String msg = "failed to execute event evaluation: metadata or event in metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (metadata.getControlEvent() == ControlEvent.IMPLICIT) {
         return true;
      }

      List<String> eventList = (List<String>) controlValue;

      for (String cv : eventList) {
         ControlEvent curEvent = metadata.getControlEvent();
         while (curEvent != null) {
            if (cv.equals(curEvent.name())) {
               return true;
            }
            curEvent = curEvent.getParent();
         }
      }

      return false;
   }

   @Override
   public String getName() {
      return NAME;
   }

   @Override
   public Object resolve(String configValue) {
      log.debug("resolve " + getName() + " config value: " + configValue);
      List<String> valueList = new ArrayList<String>();
      if (configValue == null)
         return valueList;
      if (configValue.length() == 0) {
         valueList.add("");
      } else {
         StringTokenizer tok = new StringTokenizer(configValue, ",;");
         while (tok.hasMoreTokens()) {
            String t = tok.nextToken().trim();

            ControlEvent.valueOf(t);

            if (!valueList.contains(t)) {
               valueList.add(t);
            }
         }
      }
      return valueList;
   }

}
