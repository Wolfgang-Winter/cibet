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

public class SecurityContext {

   private Object anyContext;

   private boolean initialised;

   public SecurityContext(boolean isInit) {
      initialised = isInit;
   }

   public SecurityContext(boolean isInit, Object ctx) {
      initialised = isInit;
      anyContext = ctx;
   }

   /**
    * @return the anyContext
    */
   public Object getAnyContext() {
      return anyContext;
   }

   /**
    * @param anyContext
    *           the anyContext to set
    */
   public void setAnyContext(Object anyContext) {
      this.anyContext = anyContext;
   }

   /**
    * @return the initialised
    */
   public boolean isInitialised() {
      return initialised;
   }

   /**
    * @param initialised
    *           the initialised to set
    */
   public void setInitialised(boolean initialised) {
      this.initialised = initialised;
   }

}
