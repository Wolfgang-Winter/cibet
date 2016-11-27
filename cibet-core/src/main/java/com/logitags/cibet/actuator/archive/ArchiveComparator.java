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
package com.logitags.cibet.actuator.archive;

import java.io.Serializable;
import java.util.Comparator;

public class ArchiveComparator implements Serializable, Comparator<Archive> {

   /**
    * 
    */
   private static final long serialVersionUID = 8017710795553467074L;

   @Override
   public int compare(Archive a1, Archive a2) {
      if (a1 == null || a2 == null)
         throw new IllegalArgumentException("Archive is null");
      if (a1.getCreateDate() == null || a2.getCreateDate() == null) {
         throw new IllegalArgumentException("Archive.createDate is null");
      }
      return a1.getCreateDate().compareTo(a2.getCreateDate());
   }

}
