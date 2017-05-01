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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import com.logitags.cibet.authentication.AnonymousAuthenticationProvider;
import com.logitags.cibet.authentication.AuthenticationProvider;

@Aspect
public class CibetContextAspect {

   private Log log = LogFactory.getLog(CibetContextAspect.class);

   @Pointcut(value = "execution(* *.*(..)) && @within(interceptor)", argNames = "interceptor")
   void typeCibetContextInterceptor(com.logitags.cibet.context.CibetContext interceptor) {
   }

   @Pointcut(value = "execution(* *.*(..)) && @annotation(interceptor)", argNames = "interceptor")
   void methodCibetContextInterceptor(com.logitags.cibet.context.CibetContext interceptor) {
   }

   @Around(value = "methodCibetContextInterceptor(interceptor)", argNames = "thisJoinPoint, interceptor")
   public Object interceptMethod(ProceedingJoinPoint thisJoinPoint, com.logitags.cibet.context.CibetContext interceptor)
         throws Throwable {
      return doIntercept(thisJoinPoint, interceptor.allowAnonymous());
   }

   @Around(value = "typeCibetContextInterceptor(interceptor)", argNames = "thisJoinPoint, interceptor")
   public Object intercept(ProceedingJoinPoint thisJoinPoint, com.logitags.cibet.context.CibetContext interceptor)
         throws Throwable {
      return doIntercept(thisJoinPoint, interceptor.allowAnonymous());
   }

   private Object doIntercept(ProceedingJoinPoint thisJoinPoint, boolean allowAnonymous) throws Throwable {
      log.debug("execute before CibetContextAspect " + thisJoinPoint.toString());
      boolean isNewlyManaged = false;
      AuthenticationProvider auth = null;
      if (allowAnonymous) {
         auth = new AnonymousAuthenticationProvider();
      }
      try {
         isNewlyManaged = Context.start(null, auth);
         return thisJoinPoint.proceed();

      } finally {
         if (isNewlyManaged) {
            Context.end();
         } else {
            Context.internalRequestScope().getAuthenticationProvider().getProviderChain().remove(auth);
         }
      }
   }

}
