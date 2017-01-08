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
import java.util.ArrayList;
import java.util.List;

public class MiddleBean extends BasicMigrateBean implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private String offermann;

   private List<DependentBean> deplist = new ArrayList<>();

   private transient int transiInt;

   /**
    * @return the offermann
    */
   public String getOffermann() {
      return offermann;
   }

   /**
    * @param offermann
    *           the offermann to set
    */
   public void setOffermann(String offermann) {
      this.offermann = offermann;
   }

   /**
    * @return the deplist
    */
   public List<DependentBean> getDeplist() {
      return deplist;
   }

   /**
    * @param deplist
    *           the deplist to set
    */
   public void setDeplist(List<DependentBean> deplist) {
      this.deplist = deplist;
   }

   /**
    * @return the transiInt
    */
   public int getTransiInt() {
      return transiInt;
   }

   /**
    * @param transiInt
    *           the transiInt to set
    */
   public void setTransiInt(int transiInt) {
      this.transiInt = transiInt;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(super.toString());
      b.append(" || MiddleBean");
      b.append(", offermann:");
      b.append(offermann);
      b.append(", transiInt:");
      b.append(transiInt);
      b.append(", depList.size:");
      b.append(deplist == null ? "NULL" : deplist.size());
      return b.toString();
   }

}
