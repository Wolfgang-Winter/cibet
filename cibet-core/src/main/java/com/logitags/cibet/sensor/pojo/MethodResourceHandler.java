/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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
package com.logitags.cibet.sensor.pojo;

import java.io.Serializable;

import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceHandler;
import com.logitags.cibet.sensor.ejb.EjbResourceHandler;

/**
 * This resource represents a method invocation. Target type is the class name.
 * 
 * @author Wolfgang
 * 
 */
public class MethodResourceHandler extends EjbResourceHandler implements ResourceHandler, Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = -5142727867428012361L;

   public MethodResourceHandler(Resource res) {
      super(res);
   }

   @Override
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append("[MethodResource] targetType: ");
      b.append(resource.getTargetType());
      b.append(" ; method: ");
      b.append(resource.getMethod());
      b.append(" ; invoker class: ");
      b.append(resource.getInvokerClass());
      return b.toString();
   }

}
