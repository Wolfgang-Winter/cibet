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

import java.io.Serializable;

import com.logitags.cibet.sensor.pojo.CibetIntercept;
import com.logitags.cibet.sensor.pojo.PojoInvoker;

/**
 *
 */
@CibetIntercept(factoryClass = PojoInvoker.class, param = "Walter")
public class ParamSingleton implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static ParamSingleton instance;

   protected static String param = "garnix";

   private static int compValue = 10;

   public static synchronized ParamSingleton instance() {
      if (instance == null) {
         instance = new ParamSingleton();
      }
      return instance;
   }

   public static synchronized ParamSingleton instance(String p) {
      if (instance == null) {
         instance = new ParamSingleton(p);
      }
      return instance;
   }

   public ParamSingleton() {
   }

   public ParamSingleton(String p) {
      param = p;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.util.SimpleSingleton#getCompValue()
    */
   public int getCompValue() {
      return compValue;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.util.SimpleSingleton#setCompValue(int)
    */
   public void setCompValue(int compValue) {
      ParamSingleton.compValue = compValue;
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
      SimpleSingleton.param = param;
   }

   public void clear() {
      instance = null;
   }
}
