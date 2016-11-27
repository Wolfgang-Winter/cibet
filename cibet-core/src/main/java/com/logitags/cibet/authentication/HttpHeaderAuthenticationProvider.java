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

import javax.servlet.http.HttpServletRequest;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.sensor.http.Headers;

/**
 * This implementation retrieves the user name, address and tenant from the HttpServletRequest headers CIBET_USER,
 * CIBET_USERADDRESS and CIBET_TENANT if present.
 * 
 * 
 * @author Wolfgang
 * 
 */
public class HttpHeaderAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = -1341868052782009213L;

   @Override
   public String getUsername() {
      String name = null;
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         name = req.getHeader(Headers.CIBET_USER.name());
      }
      return name;
   }

   /**
    * returns always DEFAULT_TENANT.
    */
   @Override
   public String getTenant() {
      String tenant = DEFAULT_TENANT;
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null && req.getHeader(Headers.CIBET_TENANT.name()) != null) {
         tenant = req.getHeader(Headers.CIBET_TENANT.name());
      }
      return tenant;
   }

   /**
    * returns null.
    */
   @Override
   public String getUserAddress() {
      String addr = null;
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         addr = req.getHeader(Headers.CIBET_USERADDRESS.name());
      }
      return addr;
   }

}
