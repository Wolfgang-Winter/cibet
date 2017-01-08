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

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class DependentBean implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   @GeneratedValue(strategy = GenerationType.AUTO)
   private long theId;

   private String depString;

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
    * @return the depString
    */
   public String getDepString() {
      return depString;
   }

   /**
    * @param depString
    *           the depString to set
    */
   public void setDepString(String depString) {
      this.depString = depString;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(" || ");
      b.append(this.getClass().getName());
      b.append("depString:");
      b.append(depString);
      b.append(", theId:");
      b.append(theId);
      return b.toString();
   }

}
