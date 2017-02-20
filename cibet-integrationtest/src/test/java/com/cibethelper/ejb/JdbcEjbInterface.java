/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
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

import java.util.List;

import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.core.ControlEvent;

public interface JdbcEjbInterface {

   int executeJdbc(String sql, boolean rollback);

   String registerSetpoint(String clazz, List<String> acts, ControlEvent... events);

   void unregisterSetpoint(String id);

   void release(DcControllable dc) throws Exception;
}
