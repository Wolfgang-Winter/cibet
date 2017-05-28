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
package com.logitags.cibet.sensor.ejb;

import java.lang.reflect.Method;
import java.util.Set;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.pojo.MethodResource;

@Entity
@DiscriminatorValue(value = "EjbResource")
public class EjbResource extends MethodResource {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   public EjbResource() {
   }

   /**
    * constructor used for EJB and POJO resources
    * 
    * @param invokedObject
    * @param m
    * @param params
    */
   public EjbResource(Object invokedObject, Method m, Set<ResourceParameter> params) {
      super(invokedObject, m, params);
   }

}
