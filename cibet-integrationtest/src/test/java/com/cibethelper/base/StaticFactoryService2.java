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
package com.cibethelper.base;

import com.cibethelper.entities.ITComplexEntity;
import com.logitags.cibet.sensor.pojo.CibetIntercept;
import com.logitags.cibet.sensor.pojo.FactoryInvoker;

/**
 *
 */
@CibetIntercept(factoryClass = FactoryInvoker.class, param = "com.cibethelper.base.StaticFactory.create2()")
public class StaticFactoryService2 implements ITComplexEntity {

   private static StaticFactoryService2 instance;

   private static int compValue = 10;

   protected static String param = "garnix";

   public static synchronized StaticFactoryService2 instance() {
      if (instance == null) {
         instance = new StaticFactoryService2();
      }
      return instance;
   }

   public static synchronized ITComplexEntity instance(String p) {
      if (instance == null) {
         instance = new StaticFactoryService2(p);
      }
      return instance;
   }

   protected StaticFactoryService2() {
   }

   protected StaticFactoryService2(String p) {
      param = p;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.util.ITComplexEntity#setCompValue(int)
    */
   @Override
   public void setCompValue(int compValue) {
      StaticFactoryService2.compValue = compValue;

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
      StaticFactoryService2.param = param;
   }
}
