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
@CibetIntercept(factoryClass = SpringBeanInvoker.class, param = "MySpringExampleBean")
public class SpringExampleBean implements ITComplexEntity, SpringTestInterface, Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 7854878811357947690L;

   private static int compValue = 10;

   protected static String param = "garnix";

   public SpringExampleBean() {
   }

   public SpringExampleBean(String p) {
      param = p;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.util.ITComplexEntity#setCompValue(int)
    */
   @Override
   public void setCompValue(int compValue) {
      SpringExampleBean.compValue = compValue;

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
      SpringExampleBean.param = param;
   }

   public String giveFive() {
      return "Five";
   }

   public static String concatenate(String s1, String s2) {
      return s1 + s2;
   }

   public static String concatenate2(String... strings) {
      StringBuffer b = new StringBuffer();
      for (String s : strings) {
         b.append(s);
      }
      return b.toString();
   }
}
