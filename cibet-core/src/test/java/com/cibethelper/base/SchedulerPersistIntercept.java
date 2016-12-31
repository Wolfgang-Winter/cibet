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
package com.cibethelper.base;

import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.scheduler.RejectException;
import com.logitags.cibet.actuator.scheduler.SchedulerLoader;
import com.logitags.cibet.actuator.scheduler.SchedulerTaskInterceptor;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.diff.Difference;

public class SchedulerPersistIntercept implements SchedulerTaskInterceptor {

   private static Logger log = Logger.getLogger(SchedulerPersistIntercept.class);

   @Override
   public void beforeTask(DcControllable dc) throws RejectException {
      log.info("SchedulerPersistIntercept.beforeTask");
      EntityManager appEM = Context.internalRequestScope().getApplicationEntityManager();
      TEntity te = new TEntity("Nana", 888, "polo");
      appEM.persist(te);
   }

   @Override
   public void afterTask(DcControllable dc) {
      log.info("SchedulerPersistIntercept.afterTask");
      EntityManager appEM = Context.internalRequestScope().getApplicationEntityManager();
      TEntity te = new TEntity("Nana2", 999, "polo2");
      appEM.persist(te);

      List<Difference> diffs = SchedulerLoader.executedDifferences(dc);
      for (Difference d : diffs) {
         log.debug(d);
      }
      log.info("end SchedulerPersistIntercept.afterTask");
   }

}
