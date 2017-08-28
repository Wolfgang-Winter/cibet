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
package com.logitags.cibet.control;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class ConcreteControl implements Serializable {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private Control control;

   private Set<String> includes = new HashSet<>();

   private Set<String> excludes = new HashSet<>();

   public ConcreteControl(Control control) {
      if (control == null) {
         throw new IllegalArgumentException("Illegal Control name. Control is not registered");
      }
      this.control = control;
   }

   /**
    * @return the control
    */
   public Control getControl() {
      return control;
   }

   /**
    * @param control
    *           the control to set
    */
   public void setControl(Control control) {
      this.control = control;
   }

   /**
    * @return the includes
    */
   public Set<String> getIncludes() {
      return includes;
   }

   /**
    * @param includes
    *           the includes to set
    */
   public void setIncludes(Set<String> includes) {
      this.includes = includes;
   }

   /**
    * @return the excludes
    */
   public Set<String> getExcludes() {
      return excludes;
   }

   /**
    * @param excludes
    *           the excludes to set
    */
   public void setExcludes(Set<String> excludes) {
      this.excludes = excludes;
   }

   /*
    * (non-Javadoc)
    * 
    * @see java.lang.Object#toString()
    */
   @Override
   public String toString() {
      StringBuffer b = new StringBuffer();
      b.append(control.getName());
      b.append(" (in:");
      for (String s : getIncludes()) {
         b.append(" ");
         b.append(s);
      }
      b.append(") (ex:");
      for (String s : getExcludes()) {
         b.append(" ");
         b.append(s);
      }
      b.append(")");

      return b.toString();
   }

}
