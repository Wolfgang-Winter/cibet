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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;

/**
 * This implementation retrieves the user name from HttpServletRequest.remoteHost : HttpServletRequest.remotePort if
 * present. Otherwise ANONYMOUS is returned as user.
 * 
 * 
 * @author Wolfgang
 * 
 */
public class IPAuthenticationProvider extends AbstractAuthenticationProvider {

   /**
    * 
    */
   private static final long serialVersionUID = -1341868052782009213L;
   private transient Log log = LogFactory.getLog(IPAuthenticationProvider.class);

   @Override
   public String getUsername() {
      String user = "ANONYMOUS";
      HttpServletRequest req = Context.internalSessionScope().getHttpRequest();
      if (req != null) {
         user = req.getRemoteHost() + ":" + req.getRemotePort();
      }
      return user;
   }

}
