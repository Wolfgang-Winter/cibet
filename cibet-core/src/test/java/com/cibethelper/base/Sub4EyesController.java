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

import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

import com.logitags.cibet.actuator.dc.FourEyesActuator;

/**
 * 
 */
public class Sub4EyesController extends FourEyesActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @XmlValue
   private long dummy1 = 43;

   private boolean dummy2 = true;

   private int intParam1;

   private Integer intParamB;

   private double doubleParam;

   private Double doubleParamB;

   private float floatParam;

   private Float floatParamB;

   public Sub4EyesController() {
      setName("Sub4EyesController");
   }

   /**
    * @return the dummy1
    */
   @XmlAnyElement
   public long getDummy1() {
      return dummy1;
   }

   /**
    * @param dummy1
    *           the dummy1 to set
    */
   @XmlElement
   public void setDummy1(long dummy1) {
      this.dummy1 = dummy1;
   }

   /**
    * @return the dummy2
    */
   @XmlTransient
   public boolean isDummy2() {
      return dummy2;
   }

   /**
    * @param dummy2
    *           the dummy2 to set
    */
   public void setDummy2(boolean dummy2) {
      this.dummy2 = dummy2;
   }

   /**
    * @return the intParam1
    */
   public int getIntParam1() {
      return intParam1;
   }

   /**
    * @param intParam1
    *           the intParam1 to set
    */
   public void setIntParam1(int intParam1) {
      this.intParam1 = intParam1;
   }

   /**
    * @return the intParamB
    */
   public Integer getIntParamB() {
      return intParamB;
   }

   /**
    * @param intParamB
    *           the intParamB to set
    */
   public void setIntParamB(Integer intParamB) {
      this.intParamB = intParamB;
   }

   /**
    * @return the doubleParam
    */
   public double getDoubleParam() {
      return doubleParam;
   }

   /**
    * @param doubleParam
    *           the doubleParam to set
    */
   public void setDoubleParam(double doubleParam) {
      this.doubleParam = doubleParam;
   }

   /**
    * @return the doubleParamB
    */
   public Double getDoubleParamB() {
      return doubleParamB;
   }

   /**
    * @param doubleParamB
    *           the doubleParamB to set
    */
   public void setDoubleParamB(Double doubleParamB) {
      this.doubleParamB = doubleParamB;
   }

   /**
    * @return the floatParam
    */
   public float getFloatParam() {
      return floatParam;
   }

   /**
    * @param floatParam
    *           the floatParam to set
    */
   public void setFloatParam(float floatParam) {
      this.floatParam = floatParam;
   }

   /**
    * @return the floatParamB
    */
   public Float getFloatParamB() {
      return floatParamB;
   }

   /**
    * @param floatParamB
    *           the floatParamB to set
    */
   public void setFloatParamB(Float floatParamB) {
      this.floatParamB = floatParamB;
   }

}
