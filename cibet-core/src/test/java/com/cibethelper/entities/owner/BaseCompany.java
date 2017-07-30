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

import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import com.logitags.cibet.actuator.owner.Owner;

@MappedSuperclass
public class BaseCompany implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Id
   private String id;

   @Owner
   private String tenant;

   /**
    * @return the id
    */
   public String getId() {
      return id;
   }

   /**
    * @param id
    *           the id to set
    */
   public void setId(String id) {
      this.id = id;
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

}
