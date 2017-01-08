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
package com.cibethelper.ejb;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.lock.AlreadyLockedException;
import com.logitags.cibet.actuator.lock.LockedObject;
import com.logitags.cibet.core.EventResult;

public interface RemoteEJB {

   <T> T update(T entity) throws Exception;

   <T> T persist(T entity);

   TEntity storeTEntityParallel(TEntity te);

   EventResult callTransitiveEjb(TEntity te);

   EventResult executeUpdateQuery(String qn, Object... objects);

   LockedObject lock(String targetType) throws AlreadyLockedException;
}
