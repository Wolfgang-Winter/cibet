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
package com.logitags.cibet.jndi;

import java.util.ArrayList;
import java.util.List;

/**
 * JBoss: $ejbSimpleName/[remote,local]
 * 
 * @author Wolfgang
 * 
 */
public class JBossSimpleNameStrategy implements JndiNameStrategy {

   @Override
   public List<String> getJNDINames(Class<?> clazz) {
      List<String> names = new ArrayList<String>();
      names.add(clazz.getSimpleName() + "/remote");
      names.add(clazz.getSimpleName() + "/local");
      return names;
   }

}
