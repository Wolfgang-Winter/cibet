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
package com.logitags.cibet.authentication;

import java.util.Map;

import com.logitags.cibet.sensor.http.Headers;

/**
 * This implementation retrieves the user name, address and tenant from the context data of an EJB interceptor
 * invocation. The data are retrieved from the context map as properties CIBET_USER, CIBET_USERADDRESS and CIBET_TENANT
 * if present.
 * 
 * 
 * @author Wolfgang
 * 
 */
public class InvocationContextAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = -1341868052782009213L;

   private Map<String, Object> contextData;

   public InvocationContextAuthenticationProvider(Map<String, Object> ctxData) {
      contextData = ctxData;
   }

   @Override
   public String getUsername() {
      String user = (String) contextData.get(Headers.CIBET_USER.name());
      return user;
   }

   /**
    * returns always DEFAULT_TENANT.
    */
   @Override
   public String getTenant() {
      String tenant = (String) contextData.get(Headers.CIBET_TENANT.name());
      if (tenant == null) {
         return DEFAULT_TENANT;
      } else {
         return tenant;
      }
   }

   /**
    * returns null.
    */
   @Override
   public String getUserAddress() {
      return (String) contextData.get(Headers.CIBET_USERADDRESS.name());
   }

}
