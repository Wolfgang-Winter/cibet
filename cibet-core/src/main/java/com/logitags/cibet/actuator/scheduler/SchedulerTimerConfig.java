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
package com.logitags.cibet.actuator.scheduler;

import java.io.Serializable;

public class SchedulerTimerConfig implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private String schedulerName;

   private String persistenceReference;

   public SchedulerTimerConfig() {
   }

   public SchedulerTimerConfig(String name, String ref) {
      schedulerName = name;
      persistenceReference = ref;
   }

   /**
    * @return the schedulerName
    */
   public String getSchedulerName() {
      return schedulerName;
   }

   /**
    * @param schedulerName
    *           the schedulerName to set
    */
   public void setSchedulerName(String schedulerName) {
      this.schedulerName = schedulerName;
   }

   /**
    * @return the persistenceReference
    */
   public String getPersistenceReference() {
      return persistenceReference;
   }

   /**
    * @param persistenceReference
    *           the persistenceReference to set
    */
   public void setPersistenceReference(String persistenceReference) {
      this.persistenceReference = persistenceReference;
   }

}
