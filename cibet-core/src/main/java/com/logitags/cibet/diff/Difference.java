/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2014 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.diff;

import java.io.Serializable;

/**
 * represents the differences between two versions of the same object comparing properties one by one. Considers also
 * transitive differences of associated objects.
 * 
 */
public class Difference implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private String propertyPath;

   private String canonicalPath;

   private String propertyName;

   private Class<?> propertyType;

   private DifferenceType differenceType;

   private Object oldValue;

   private Object newValue;

   /**
    * property path in the format of java-object-diff. This includes the transitive property names separated by / and in
    * Collections and Maps the element respective key.
    * 
    * @return the propertyPath
    */
   public String getPropertyPath() {
      return propertyPath;
   }

   /**
    * @param propertyPath
    *           the propertyPath to set
    */
   public void setPropertyPath(String propertyPath) {
      this.propertyPath = propertyPath;
   }

   /**
    * one of REMOVED, ADDED or MODIFIED
    * 
    * @return the diffType
    */
   public DifferenceType getDifferenceType() {
      return differenceType;
   }

   /**
    * @param diffType
    *           the diffType to set
    */
   public void setDifferenceType(DifferenceType diffType) {
      this.differenceType = diffType;
   }

   /**
    * old value of the property when it has been changed or removed
    * 
    * @return the oldValue
    */
   public Object getOldValue() {
      return oldValue;
   }

   /**
    * @param oldValue
    *           the oldValue to set
    */
   public void setOldValue(Object oldValue) {
      this.oldValue = oldValue;
   }

   /**
    * new value of the property when it has been changed or added
    * 
    * @return the newValue
    */
   public Object getNewValue() {
      return newValue;
   }

   /**
    * @param newValue
    *           the newValue to set
    */
   public void setNewValue(Object newValue) {
      this.newValue = newValue;
   }

   /**
    * simple name of the property without path
    * 
    * @return the propertyName
    */
   public String getPropertyName() {
      return propertyName;
   }

   /**
    * @param propertyName
    *           the propertyName to set
    */
   public void setPropertyName(String propertyName) {
      this.propertyName = propertyName;
   }

   /**
    * class name of the property
    * 
    * @return the propertyType
    */
   public Class<?> getPropertyType() {
      return propertyType;
   }

   /**
    * @param propertyType
    *           the propertyType to set
    */
   public void setPropertyType(Class<?> propertyType) {
      this.propertyType = propertyType;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("propertyName=");
      b.append(propertyName);
      b.append(" ; propertyPath=");
      b.append(propertyPath);
      b.append(" ; propertyType=");
      b.append(propertyType);
      b.append(" ; differenceType=");
      b.append(differenceType);
      b.append(" ; oldValue=");
      b.append(oldValue);
      b.append(" ; newValue=");
      b.append(newValue);
      b.append(" ; canonicalPath: ");
      b.append(canonicalPath);
      return b.toString();
   }

   /**
    * a standardized representation of the property name that shows a difference. A property in the main object is
    * simply represented by the property name. Transitive properties are represented by the names, separated by a dot
    * like in address.street where the parent object has a property address of type Address which has a String property
    * street.
    * 
    * @return the canonicalPath
    */
   public String getCanonicalPath() {
      return canonicalPath;
   }

   /**
    * @param canonicalPath
    *           the canonicalPath to set
    */
   public void setCanonicalPath(String canonicalPath) {
      this.canonicalPath = canonicalPath;
   }
}
