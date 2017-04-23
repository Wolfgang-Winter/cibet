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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.PrePersist;
import javax.persistence.Table;
import javax.persistence.Version;

@Entity
@Table(name = "CIB_PERSON")
public class Person implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   private String personId;

   private String name;

   private String state = "GOOD";

   public String getState() {
      return state;
   }

   public void setState(String state) {
      this.state = state;
   }

   @OneToMany(cascade = CascadeType.ALL)
   private List<Address> addresses = new ArrayList<>();

   public Person() {
   }

   public Person(String name) {
      this.name = name;
   }

   @PrePersist
   public void prePersist() {
      personId = UUID.randomUUID().toString();
   }

   public String getPersonId() {
      return personId;
   }

   public void setPersonId(String personId) {
      this.personId = personId;
   }

   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public List<Address> getAddresses() {
      return addresses;
   }

   public void setAddresses(List<Address> addresses) {
      this.addresses = addresses;
   }

   public int getVersion() {
      return version;
   }

   public void setVersion(int version) {
      this.version = version;
   }

   @Version
   private int version;

   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("Person id=");
      b.append(personId);
      b.append("; name=");
      b.append(name);
      b.append("; Addresse(s):\n");
      for (Address addr : addresses) {
         b.append(addr);
         b.append("\n");
      }

      return b.toString();
   }
}
