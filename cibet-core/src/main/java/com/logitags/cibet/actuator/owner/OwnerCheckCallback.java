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
package com.logitags.cibet.actuator.owner;

/**
 * callback interface for OwnerCheckActuator. This interface is called for a persistence event where the object/entity
 * owner is not the same as is set as tenant in the session scope.
 * 
 * @author Wolfgang
 *
 */
public interface OwnerCheckCallback {

   /**
    * Called by OwnerCheckActuator for a persistence event where the object owner is not the same as is set as tenant in
    * the session scope
    * 
    * @param tenant
    *           the tenant in session scope
    * @param object
    *           the offending object
    * @param ownerString
    *           the owner string that is created from the entity and compared to the tenant in session scope
    */
   void onOwnerCheckFailed(String tenant, Object object, String ownerString);
}
