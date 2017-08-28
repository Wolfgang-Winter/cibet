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
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;

/**
 * evaluates the current event against configured setpoints. If the sensor/metadata contains IMPLICIT ControlEvent the
 * configuration in the setpoint is ignored.
 */
public class EventControl implements Serializable, Control {

   /**
    * 
    */
   private static final long serialVersionUID = 5677366782801007178L;
   private static Log log = LogFactory.getLog(EventControl.class);

   public static final String NAME = "event";

   @Override
   public String getName() {
      return NAME;
   }

   private Set<ControlEvent> parse(String configValue) {
      Set<ControlEvent> valueList = new HashSet<>();
      if (configValue == null || configValue.length() == 0) return valueList;
      StringTokenizer tok = new StringTokenizer(configValue, ",;");
      while (tok.hasMoreTokens()) {
         String t = tok.nextToken().trim();
         valueList.add(ControlEvent.valueOf(t));
      }
      return valueList;
   }

   @Override
   public Boolean evaluate(Set<String> values, EventMetadata metadata) {
      if (metadata == null || metadata.getControlEvent() == null) {
         String msg = "failed to execute event evaluation: metadata or event in metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (values == null || values.isEmpty()) return null;

      for (String cv : values) {
         Set<ControlEvent> controlEvents = parse(cv);
         for (ControlEvent controlEvent : controlEvents) {
            ControlEvent curEvent = metadata.getControlEvent();
            while (curEvent != null) {
               if (curEvent == controlEvent) {
                  return true;
               }
               curEvent = curEvent.getParent();
            }
         }
      }

      return false;
   }

}
