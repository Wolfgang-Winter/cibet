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
package com.logitags.cibet.authentication;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.sensor.http.Headers;

public class HttpSessionAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   @Override
   public String getUsername() {
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         HttpSession session = req.getSession(false);
         if (session != null) {
            Object user = session.getAttribute(Headers.CIBET_USER.name());
            if (user != null && user instanceof String) {
               return (String) user;
            }
         }
      }

      return null;
   }

   /**
    * returns http session attribute 'tenant' if existing, otherwise DEFAULT_TENANT.
    */
   @Override
   public String getTenant() {
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         HttpSession session = req.getSession(false);
         if (session != null) {
            Object tenant = session.getAttribute(Headers.CIBET_TENANT.name());
            if (tenant != null && tenant instanceof String) {
               return (String) tenant;
            }
         }
      }

      return DEFAULT_TENANT;
   }

   /**
    * returns http session attribute 'userAddress' if existing, otherwise null.
    */
   @Override
   public String getUserAddress() {
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         HttpSession session = req.getSession(false);
         if (session != null) {
            Object addr = session.getAttribute(Headers.CIBET_USERADDRESS.name());
            if (addr != null && addr instanceof String) {
               return (String) addr;
            }
         }
      }
      return null;
   }

}
