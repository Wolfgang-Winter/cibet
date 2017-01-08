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

import javax.persistence.Entity;

@Entity
public class Migrant extends MiddleBean {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private String longer;

   public String hang;

   /**
    * @return the longer
    */
   public String getLonger() {
      return longer;
   }

   /**
    * @param longer
    *           the longer to set
    */
   public void setLonger(String longer) {
      this.longer = longer;
   }

   /**
    * @return the hang
    */
   public String getHang() {
      return hang;
   }

   /**
    * @param hang
    *           the hang to set
    */
   public void setHang(String hang) {
      this.hang = hang;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(super.toString());
      b.append(" || ");
      b.append(this.getClass().getName());
      b.append(", non-static hang:");
      b.append(hang);
      b.append(", longer:");
      b.append(longer);
      return b.toString();
   }

}
