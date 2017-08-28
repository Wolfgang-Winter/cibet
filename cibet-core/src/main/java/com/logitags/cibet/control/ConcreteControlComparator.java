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

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;

import com.logitags.cibet.config.Configuration;

public class ConcreteControlComparator implements Serializable, Comparator<ConcreteControl> {

   /**
    * 
    */
   private static final long serialVersionUID = 8017710795553467074L;
   private List<String> controlNames;

   public ConcreteControlComparator() {
   }

   public ConcreteControlComparator(List<String> list) {
      controlNames = list;
   }

   @Override
   public int compare(ConcreteControl o1, ConcreteControl o2) {
      if (o1 == null || o2 == null) return 0;
      if (o1.getControl() == null || o2.getControl() == null) return 0;
      if (o1.getControl().getName() == null || o2.getControl().getName() == null) return 0;

      if (o1.getControl().getName().equals(o2.getControl().getName())) return 0;

      if (controlNames == null) {
         controlNames = Configuration.instance().getControlNames();
      }

      int int1 = controlNames.indexOf(o1.getControl().getName());
      if (int1 == -1) int1 = 1000;
      int int2 = controlNames.indexOf(o2.getControl().getName());
      if (int2 == -1) int2 = 1000;
      return int1 < int2 ? -1 : 1;
   }
}
