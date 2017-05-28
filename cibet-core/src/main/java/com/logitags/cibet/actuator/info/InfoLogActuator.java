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
package com.logitags.cibet.actuator.info;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.core.EventMetadata;

/**
 * implementation of Actuator that does no control but prints out an info message on each method. Usable for testing and
 * debugging. The object (for persistence actions) or the method and parameters (method invocation) are logged.
 * 
 */
public class InfoLogActuator extends AbstractActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 1239511539140427644L;

   private transient Log log = LogFactory.getLog(InfoLogActuator.class);

   public static final String DEFAULTNAME = "INFOLOG";

   public InfoLogActuator() {
      setName(DEFAULTNAME);
   }

   public InfoLogActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#beforeEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      log.info("\n*************InfoLogActuator.beforeEvent of :\n" + ctx + "\n*************");
      super.beforeEvent(ctx);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.AbstractActuator#afterEvent(com.logitags.cibet .core.EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      log.info("\n*************InfoLogActuator.afterEvent of :\n" + ctx + "\n*************");
      super.afterEvent(ctx);
   }

}
