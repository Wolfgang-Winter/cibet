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
package com.cibethelper.base;

import org.apache.log4j.Logger;

import com.logitags.cibet.sensor.pojo.CibetIntercept;

public class AspectInvokeTestClass {

   private static Logger log = Logger.getLogger(AspectInvokeTestClass.class);

   @CibetIntercept
   public String callWithAspect(String msg) {
      return msg;
   }

   public String callWithoutAspect(String msg) {
      return msg;
   }

   @CibetIntercept
   public void callWithException() throws Exception {
      log.warn("throw new Exception(Expected FAIL)");
      throw new Exception("Expected FAIL");
   }
}
