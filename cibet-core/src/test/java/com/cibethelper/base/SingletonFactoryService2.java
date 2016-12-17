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
@CibetIntercept(factoryClass = FactoryInvoker.class, param = "com.cibethelper.base.SingletonFactory.create2()")
public class SingletonFactoryService2 implements ITComplexEntity {

   private static SingletonFactoryService2 instance;

   private static int compValue = 10;

   protected static String param = "garnix";

   public static synchronized SingletonFactoryService2 instance() {
      if (instance == null) {
         instance = new SingletonFactoryService2();
      }
      return instance;
   }

   public static synchronized ITComplexEntity instance(String p) {
      if (instance == null) {
         instance = new SingletonFactoryService2(p);
      }
      return instance;
   }

   protected SingletonFactoryService2() {
   }

   protected SingletonFactoryService2(String p) {
      param = p;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.util.ITComplexEntity#setCompValue(int)
    */
   @Override
   public void setCompValue(int compValue) {
      SingletonFactoryService2.compValue = compValue;

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
      SingletonFactoryService2.param = param;
   }
}
