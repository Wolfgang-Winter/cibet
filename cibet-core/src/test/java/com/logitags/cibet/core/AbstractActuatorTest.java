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
package com.logitags.cibet.core;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.logitags.cibet.actuator.dc.FourEyesActuator;

public class AbstractActuatorTest {

   /**
    * logger for tracing
    */
   private static Logger log = Logger.getLogger(AbstractActuatorTest.class);

   @Test
   public void testActuatorEquals() {
      log.info("start testActuatorEquals()");
      String x = "sdf";
      FourEyesActuator act = new FourEyesActuator();
      Assert.assertEquals(false, act.equals(x));
      FourEyesActuator act2 = new FourEyesActuator("other");
      Assert.assertEquals(false, act.equals(act2));
      FourEyesActuator act3 = new FourEyesActuator();
      Assert.assertEquals(true, act.equals(act3));
   }

}
