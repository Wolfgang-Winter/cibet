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

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.core.EventMetadata;

/**
 * evaluates Controls
 */
public class DefaultController implements Controller {

   private static Log log = LogFactory.getLog(DefaultController.class);

   /**
   *
   */
   public void evaluate(EventMetadata metadata) {
      if (log.isDebugEnabled()) {
         log.debug("evaluate EventMetadata:: " + metadata);
      }

      if (metadata == null) {
         String msg = "failed to evaluate setpoints: metadata is null";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }

      if (metadata.getControlEvent() == null) {
         log.debug("event is not controlled by Cibet");
         return;
      }

      Configuration cman = Configuration.instance();

      for (Setpoint sp : cman.getSetpoints()) {
         Map<String, Object> controlValues = new TreeMap<String, Object>(new ControlComparator());
         sp.getEffectiveControlValues(controlValues);
         boolean matches = true;

         for (Entry<String, Object> entry : controlValues.entrySet()) {
            Control co = cman.getControl(entry.getKey());
            if (co == null) {
               String msg = "Failed to evaluate setpoint " + sp.getId() + ": No Control registered with name "
                     + entry.getKey();
               log.error(msg);
               throw new RuntimeException(msg);
            }

            if (co.hasControlValue(entry.getValue())) {
               matches = co.evaluate(entry.getValue(), metadata);
               if (!matches) {
                  if (log.isDebugEnabled()) {
                     log.debug("Setpoint " + sp.getId() + " --> " + co.getName() + " FAILS!");
                  }
                  break;
               }
            }
            // log.debug(co.getName() + " MATCHES!");
         }

         if (matches) {
            metadata.addSetpoint(sp);
         }
      }

      if (log.isInfoEnabled()) {
         log.info(metadata);
      }
   }
}
