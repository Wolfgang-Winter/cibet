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

import com.logitags.cibet.control.AbstractControl;
import com.logitags.cibet.core.EventMetadata;

public class TrueCustomControl extends AbstractControl {

   private String name = "TRUE";

   private String gaga;

   /**
    * 
    */
   private static final long serialVersionUID = -7149481961750111424L;

   @Override
   public String getName() {
      return name;
   }

   public void setName(String name) {
      this.name = name;
   }

   public String getGaga() {
      return gaga;
   }

   public void setGaga(String g) {
      gaga = g;
   }

   @Override
   public boolean evaluate(Object controlValue, EventMetadata metadata) {
      return true;
   }

}
