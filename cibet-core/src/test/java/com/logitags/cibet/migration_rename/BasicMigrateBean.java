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
package com.logitags.cibet.migration_rename;

import java.io.Serializable;
import java.util.Calendar;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@MappedSuperclass
public abstract class BasicMigrateBean implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long theId;

   private int intParam;

   @Temporal(TemporalType.DATE)
   private Calendar date;

   @OneToOne
   private DependentBean dependendBean;

   private short newProp;

   /**
    * @return the theId
    */
   public long getTheId() {
      return theId;
   }

   /**
    * @param theId
    *           the theId to set
    */
   public void setTheId(long theId) {
      this.theId = theId;
   }

   /**
    * @return the intParam
    */
   public int getIntParam() {
      return intParam;
   }

   /**
    * @param intParam
    *           the intParam to set
    */
   public void setIntParam(int intParam) {
      this.intParam = intParam;
   }

   /**
    * @return the date
    */
   public Calendar getDate() {
      return date;
   }

   /**
    * @param date
    *           the date to set
    */
   public void setDate(Calendar date) {
      this.date = date;
   }

   /**
    * @return the dependendBean
    */
   public DependentBean getDependendBean() {
      return dependendBean;
   }

   /**
    * @param dependendBean
    *           the dependendBean to set
    */
   public void setDependendBean(DependentBean dependendBean) {
      this.dependendBean = dependendBean;
   }

   /**
    * @return the newProp
    */
   public short getNewProp() {
      return newProp;
   }

   /**
    * @param newProp
    *           the newProp to set
    */
   public void setNewProp(short newProp) {
      this.newProp = newProp;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("BasicMigrateBean, date:");
      b.append(date);
      b.append(", intParam:");
      b.append(intParam);
      b.append(", theId:");
      b.append(theId);
      b.append(", newProp:");
      b.append(newProp);
      b.append(", dependendBean:");
      b.append(dependendBean);
      return b.toString();
   }

}
