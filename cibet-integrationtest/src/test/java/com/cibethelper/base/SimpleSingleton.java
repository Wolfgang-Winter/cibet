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

import org.apache.log4j.Logger;

import com.cibethelper.entities.ITComplexEntity;
import com.logitags.cibet.sensor.pojo.CibetIntercept;
import com.logitags.cibet.sensor.pojo.PojoInvoker;

/**
 *
 */
@CibetIntercept(factoryClass = PojoInvoker.class)
public class SimpleSingleton implements ITComplexEntity, Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = -6639686148109088884L;

   private static Logger log = Logger.getLogger(SimpleSingleton.class);

   private static SimpleSingleton instance;

   private static int compValue = 10;

   protected static String param = "garnix";

   public static synchronized ITComplexEntity instance() {
      if (instance == null) {
         instance = new SimpleSingleton();
      }
      return instance;
   }

   public static synchronized ITComplexEntity instance(String p) {
      if (instance == null) {
         instance = new SimpleSingleton(p);
      }
      return instance;
   }

   protected SimpleSingleton() {
   }

   protected SimpleSingleton(String p) {
      param = p;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.util.ITComplexEntity#setCompValue(int)
    */
   @Override
   public void setCompValue(int compValue) {
      SimpleSingleton.compValue = compValue;

   }

   /**
    * @return the compValue
    */
   public int getCompValue() {
      log.info("actual compValue: " + compValue);
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
      SimpleSingleton.param = param;
   }

}
