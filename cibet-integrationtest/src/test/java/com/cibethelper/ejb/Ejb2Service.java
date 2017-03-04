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
package com.cibethelper.ejb;

import javax.ejb.Stateless;

import org.apache.log4j.Logger;

import com.logitags.cibet.sensor.pojo.CibetIntercept;

@Stateless
public class Ejb2Service {

   private static Logger log = Logger.getLogger(Ejb2Service.class);

   public void doIt(String msg) {
      log.info("doIt called");
      logThis(msg);
   }

   @CibetIntercept
   public String logThis(String msg) {
      String m = "Ejb2Service.logThis called with: " + msg;
      log.info(m);
      return m;
   }

}
