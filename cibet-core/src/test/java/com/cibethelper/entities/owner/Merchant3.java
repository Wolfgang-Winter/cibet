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
package com.cibethelper.entities.owner;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.logitags.cibet.actuator.owner.Owner;

@Entity
@Table(name = "TEST_MERCHANT3")
public class Merchant3 implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   private long id;

   @ManyToOne
   @Owner(1)
   private Merchant3 parent;

   @Owner(2)
   private String tenant;

   @Owner(0)
   private String country;

   public Merchant3(long id, String tenant, String country) {
      super();
      this.id = id;
      this.tenant = tenant;
      this.country = country;
   }

   public Merchant3() {
   }

   /**
    * @return the id
    */
   public long getId() {
      return id;
   }

   /**
    * @param id
    *           the id to set
    */
   public void setId(long id) {
      this.id = id;
   }

   /**
    * @return the parent
    */
   public Merchant3 getParent() {
      return parent;
   }

   /**
    * @param parent
    *           the parent to set
    */
   public void setParent(Merchant3 parent) {
      this.parent = parent;
   }

   /**
    * @return the tenant
    */
   public String getTenant() {
      return tenant;
   }

   /**
    * @param tenant
    *           the tenant to set
    */
   public void setTenant(String tenant) {
      this.tenant = tenant;
   }

   /**
    * @return the country
    */
   public String getCountry() {
      return country;
   }

   /**
    * @param country
    *           the country to set
    */
   public void setCountry(String country) {
      this.country = country;
   }

}
