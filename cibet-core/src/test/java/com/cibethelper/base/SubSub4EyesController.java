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

/**
 *
 */
public class SubSub4EyesController extends Sub4EyesController {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private String strParam;

   private Boolean boolParam;

   private Short shortParamB;

   private short shortParam;

   private Long longParam;

   public SubSub4EyesController() {
   }

   public SubSub4EyesController(String name) {
      setName(name);
   }

   /**
    * @return the strParam
    */
   public String getStrParam() {
      return strParam;
   }

   /**
    * @param strParam
    *           the strParam to set
    */
   public void setStrParam(String strParam) {
      this.strParam = strParam;
   }

   /**
    * @return the boolParam
    */
   public Boolean getBoolParam() {
      return boolParam;
   }

   /**
    * @param boolParam
    *           the boolParam to set
    */
   public void setBoolParam(Boolean boolParam) {
      this.boolParam = boolParam;
   }

   /**
    * @return the shortParamB
    */
   public Short getShortParamB() {
      return shortParamB;
   }

   /**
    * @param shortParamB
    *           the shortParamB to set
    */
   public void setShortParamB(Short shortParamB) {
      this.shortParamB = shortParamB;
   }

   /**
    * @return the shortParam
    */
   public short getShortParam() {
      return shortParam;
   }

   /**
    * @param shortParam
    *           the shortParam to set
    */
   public void setShortParam(short shortParam) {
      this.shortParam = shortParam;
   }

   /**
    * @return the longParam
    */
   public Long getLongParam() {
      return longParam;
   }

   /**
    * @param longParam
    *           the longParam to set
    */
   public void setLongParam(Long longParam) {
      this.longParam = longParam;
   }
}
