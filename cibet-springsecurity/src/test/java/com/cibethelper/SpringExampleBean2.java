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
package com.cibethelper;

import java.io.Serializable;

import com.cibethelper.entities.ITComplexEntity;
import com.logitags.cibet.sensor.pojo.CibetIntercept;
import com.logitags.cibet.sensor.pojo.SpringBeanInvoker;

/**
 *
 */
@CibetIntercept(factoryClass = SpringBeanInvoker.class)
public class SpringExampleBean2 implements ITComplexEntity, Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 5438736467874380047L;

   private static int compValue = 10;

   protected static String param = "garnix";

   public SpringExampleBean2() {
   }

   public SpringExampleBean2(String p) {
      param = p;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.util.ITComplexEntity#setCompValue(int)
    */
   @Override
   public void setCompValue(int compValue) {
      SpringExampleBean2.compValue = compValue;

   }

   /**
    * @return the compValue
    */
   public int getCompValue() {
      return compValue;
   }

   /**
    * @return the param
    */
   public static String getParam() {
      return param;
   }

   /**
    * @param param
    *           the param to set
    */
   public static void setParam(String param) {
      SpringExampleBean2.param = param;
   }
}
