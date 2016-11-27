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
package com.logitags.cibet.authentication;

import java.io.Serializable;

public abstract class AbstractAuthenticationProvider implements AuthenticationProvider, Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   public static final String DEFAULT_TENANT = "__DEFAULT";

   /**
    * returns always DEFAULT_TENANT.
    */
   @Override
   public String getTenant() {
      return DEFAULT_TENANT;
   }

   /**
    * returns null.
    */
   @Override
   public String getUserAddress() {
      return null;
   }

   /**
    * returns null
    * 
    * @param securityContextInfo
    *           context
    * @return Security context
    */
   @Override
   public SecurityContext initSecurityContext(String securityContextInfo) {
      return null;
   }

   /**
    * does nothing
    * 
    * @param secCtx
    *           Security context
    */
   @Override
   public void stopSecurityContext(SecurityContext secCtx) {
   }

   /**
    * returns null.
    * 
    * @return
    */
   @Override
   public String createSecurityContextHeader() {
      return null;
   }

}
