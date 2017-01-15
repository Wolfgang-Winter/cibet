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
package com.logitags.cibet.context;

import java.io.Serializable;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.authentication.AnonymousAuthenticationProvider;
import com.logitags.cibet.authentication.AuthenticationProvider;
import com.logitags.cibet.authentication.InvocationContextAuthenticationProvider;

public class CibetContextInterceptor implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private Log log = LogFactory.getLog(CibetContextInterceptor.class);

   @AroundInvoke
   public Object controlInvoke(InvocationContext ctx) throws Exception {
      log.debug("execute CibetContextInterceptor with " + ctx);
      boolean isNewlyManaged = false;
      AuthenticationProvider auth = new InvocationContextAuthenticationProvider(ctx.getContextData());
      AuthenticationProvider anonymousAuth = new AnonymousAuthenticationProvider();
      try {
         isNewlyManaged = InitializationService.instance().startContext(null, auth, anonymousAuth);
         return ctx.proceed();
      } finally {
         if (isNewlyManaged) {
            InitializationService.instance().endContext();
         } else {
            Context.internalRequestScope().getAuthenticationProvider().getProviderChain().remove(auth);
            Context.internalRequestScope().getAuthenticationProvider().getProviderChain().remove(anonymousAuth);
         }
      }
   }

}
