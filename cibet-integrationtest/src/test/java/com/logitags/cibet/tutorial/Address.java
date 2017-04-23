/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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
package com.logitags.cibet.tutorial;

import java.io.Serializable;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "CIB_ADDRESS")
public class Address implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   private String addressId;

   public String getAddressId() {
      return addressId;
   }

   public void setAddressId(String addressId) {
      this.addressId = addressId;
   }

   private String city;

   @Version
   private int version;

   public Address() {
   }

   public Address(String city) {
      this.city = city;
   }

   @PrePersist
   public void prePersist() {
      addressId = UUID.randomUUID().toString();
   }

   public String getCity() {
      return city;
   }

   public void setCity(String city) {
      this.city = city;
   }

   public int getVersion() {
      return version;
   }

   public void setVersion(int version) {
      this.version = version;
   }

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("Address id=");
      b.append(addressId);
      b.append("; city=");
      b.append(city);

      return b.toString();
   }

}
