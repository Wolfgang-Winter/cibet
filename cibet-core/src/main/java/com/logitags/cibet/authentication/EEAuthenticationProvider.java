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

import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;

/**
 * This implementation retrieves the user name from SessionContext Caller Principal name.
 * 
 * @author Wolfgang
 * 
 */
public class EEAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = -1341868052782009213L;

   /**
    * returns SessionContext Caller name if set in CibetContextFilter
    */
   @Override
   public String getUsername() {
      String userName = (String) Context.internalRequestScope().getProperty(InternalRequestScope.CALLER_PRINCIPAL_NAME);
      if (userName == null || "unauthenticated".equals(userName.toLowerCase())
            || "ANONYMOUS".equals(userName.toUpperCase()) || "GUEST".equals(userName.toUpperCase())) {
         userName = null;
      }
      return userName;
   }

}
