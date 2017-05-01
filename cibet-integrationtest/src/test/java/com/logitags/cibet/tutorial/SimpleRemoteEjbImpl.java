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
package com.logitags.cibet.tutorial;

import javax.ejb.Remote;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;

import org.apache.log4j.Logger;

import com.logitags.cibet.context.CibetContextInterceptor;
import com.logitags.cibet.sensor.ejb.CibetInterceptor;

@Stateless
@Remote
public class SimpleRemoteEjbImpl implements SimpleRemoteEjb {

   private static Logger log = Logger.getLogger(SimpleRemoteEjbImpl.class);

   @Override
   @Interceptors({ CibetContextInterceptor.class, SchedulerInterceptor.class, CibetInterceptor.class })
   public String writeString(String param) {
      log.info(param);
      return param;
   }

   @Override
   public String writeStringNoIntercept(String param) {
      log.info(param);
      return param;
   }

}
