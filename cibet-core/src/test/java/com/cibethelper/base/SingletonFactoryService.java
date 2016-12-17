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
@CibetIntercept(factoryClass = FactoryInvoker.class, param = "com.cibethelper.base.SingletonFactory")
public class SingletonFactoryService implements ITComplexEntity {

   private static SingletonFactoryService instance;

   private static int compValue = 10;

   protected static String param = "garnix";

   public static synchronized ITComplexEntity instance() {
      if (instance == null) {
         instance = new SingletonFactoryService();
      }
      return instance;
   }

   public static synchronized ITComplexEntity instance(String p) {
      if (instance == null) {
         instance = new SingletonFactoryService(p);
      }
      return instance;
   }

   protected SingletonFactoryService() {
   }

   protected SingletonFactoryService(String p) {
      param = p;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.util.ITComplexEntity#setCompValue(int)
    */
   @Override
   public void setCompValue(int compValue) {
      SingletonFactoryService.compValue = compValue;

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
      SingletonFactoryService.param = param;
   }
}
