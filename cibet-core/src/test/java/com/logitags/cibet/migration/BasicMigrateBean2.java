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
package com.logitags.cibet.migration;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@MappedSuperclass
public abstract class BasicMigrateBean2 implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long theId;

   private int intParam;

   @Temporal(TemporalType.DATE)
   private Date date;

   @OneToOne
   private DependentBean dependendBean;

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
   public Date getDate() {
      return date;
   }

   /**
    * @param date
    *           the date to set
    */
   public void setDate(Date date) {
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

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("BasicMigrateBean2, date:");
      b.append(date);
      b.append(", intParam:");
      b.append(intParam);
      b.append(", theId:");
      b.append(theId);
      b.append(", dependendBean:");
      b.append(dependendBean);
      return b.toString();
   }

}
