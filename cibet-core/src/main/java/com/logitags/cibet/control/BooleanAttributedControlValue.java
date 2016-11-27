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
package com.logitags.cibet.control;

import java.util.ArrayList;
import java.util.List;

public class BooleanAttributedControlValue {

   private boolean booleanValue = false;

   private List<String> values = new ArrayList<String>();;

   /**
    * @return the booleanValue
    */
   public boolean isBooleanValue() {
      return booleanValue;
   }

   /**
    * @param booleanValue
    *           the booleanValue to set
    */
   public void setBooleanValue(boolean booleanValue) {
      this.booleanValue = booleanValue;
   }

   /**
    * @return the values
    */
   public List<String> getValues() {
      return values;
   }

   /**
    * @param values
    *           the values to set
    */
   public void setValues(List<String> values) {
      this.values = values;
   }
}
