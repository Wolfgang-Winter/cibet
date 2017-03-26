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
package com.cibethelper.entities;

import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.PrePersist;

@Entity
public class Te2 {

   @Id
   private String id2;

   private long counter;

   public String getId2() {
      return id2;
   }

   public void setId2(String id) {
      this.id2 = id;
   }

   public long getCounter() {
      return counter;
   }

   public void setCounter(long counter) {
      this.counter = counter;
   }

   @PrePersist
   public void pre() {
      id2 = UUID.randomUUID().toString();
   }

}
