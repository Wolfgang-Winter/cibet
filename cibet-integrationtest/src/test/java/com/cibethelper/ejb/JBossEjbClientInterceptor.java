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
package com.cibethelper.ejb;

import org.jboss.ejb.client.EJBClientInterceptor;
import org.jboss.ejb.client.EJBClientInvocationContext;

import com.logitags.cibet.sensor.http.Headers;

public class JBossEjbClientInterceptor implements EJBClientInterceptor {

   private boolean active = true;

   @Override
   public void handleInvocation(EJBClientInvocationContext context) throws Exception {
      if (isActive()) {
         context.getContextData().put(Headers.CIBET_USER.name(), "Ernst");
         context.getContextData().put(Headers.CIBET_TENANT.name(), "comp");
      } else {
         context.getContextData().remove(Headers.CIBET_USER.name());
         context.getContextData().remove(Headers.CIBET_TENANT.name());
      }

      context.sendRequest();
   }

   @Override
   public Object handleInvocationResult(EJBClientInvocationContext context) throws Exception {
      return context.getResult();
   }

   /**
    * @return the active
    */
   public boolean isActive() {
      return active;
   }

   /**
    * @param active
    *           the active to set
    */
   public void setActive(boolean active) {
      this.active = active;
   }

}
