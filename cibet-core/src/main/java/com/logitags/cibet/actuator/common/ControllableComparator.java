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
package com.logitags.cibet.actuator.common;

import java.io.Serializable;
import java.util.Comparator;

public class ControllableComparator implements Serializable, Comparator<Controllable> {

   /**
    * 
    */
   private static final long serialVersionUID = 8017710795553467074L;

   @Override
   public int compare(Controllable d1, Controllable d2) {
      if (d1 == null || d2 == null)
         throw new IllegalArgumentException("Controllable is null");
      return -(d1.getCreateDate().compareTo(d2.getCreateDate()));
   }

}
