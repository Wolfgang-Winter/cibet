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

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;

@Entity
public class Te1 {

   @Id
   private String id;

   private long counter;

   @OneToOne(cascade = CascadeType.ALL)
   private Te2 te2;

   @OneToMany(fetch = FetchType.LAZY, cascade = { CascadeType.REMOVE, CascadeType.PERSIST, CascadeType.MERGE })
   @JoinTable(name = "Te2_LAZY", joinColumns = { @JoinColumn(name = "id") }, inverseJoinColumns = {
         @JoinColumn(name = "lazy_id") })
   private Set<Te2> lazyList = new LinkedHashSet<Te2>();

   public Set<Te2> getLazyList() {
      return lazyList;
   }

   public void setLazyList(Set<Te2> lazyList) {
      this.lazyList = lazyList;
   }

   public Te2 getTe2() {
      return te2;
   }

   public void setTe2(Te2 te2) {
      this.te2 = te2;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }

   public long getCounter() {
      return counter;
   }

   public void setCounter(long counter) {
      this.counter = counter;
   }

   @PrePersist
   public void pre() {
      id = UUID.randomUUID().toString();
   }

}
