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

import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.sensor.ejb.CibetInterceptor;

@Stateless
public class SimpleEjb {

   private static Logger log = Logger.getLogger(SimpleEjb.class);

   @PersistenceContext(unitName = "APPL-UNIT")
   private EntityManager em;

   @Interceptors(CibetInterceptor.class)
   public TEntity storeTEntityForTimerTask(TEntity te) {
      te.setCounter(te.getCounter() + 1);
      if (!Context.requestScope().isPostponed()) {
         log.debug("store " + te);
         em.persist(te);
      }
      log.debug("end store ");
      return te;
   }

}
