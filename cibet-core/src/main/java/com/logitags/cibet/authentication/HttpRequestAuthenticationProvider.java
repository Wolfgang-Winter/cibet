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

import java.security.Principal;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;

/**
 * This implementation retrieves the user name from the HttpServletRequest user principal retrieved from CibetContext.
 * HttpServletRequest is set into the context when CibetContextFilter is applied. If no user is set in
 * HttpServletRequest user principal, it retrieves the user name from HttpServletRequest.remoteUser.
 * 
 * 
 * @author Wolfgang
 * 
 */
public class HttpRequestAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = -1341868052782009213L;
   private transient Log log = LogFactory.getLog(HttpRequestAuthenticationProvider.class);

   @Override
   public String getUsername() {
      String name = null;
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         Principal p = req.getUserPrincipal();
         if (p != null) {
            log.debug("find user name from HttpServletRequest.getUserPrincipal(): " + p.getName());
            name = p.getName();
         } else if (req.getRemoteUser() != null) {
            log.debug("find user name from HttpServletRequest.getRemoteUser(): " + req.getRemoteUser());
            name = req.getRemoteUser();
         }
      }
      return name;
   }

}
