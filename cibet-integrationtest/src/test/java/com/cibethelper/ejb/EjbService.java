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
package com.cibethelper.ejb;

import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;

import com.logitags.cibet.core.CibetUtil;

@Stateless
public class EjbService {

   private static Logger log = Logger.getLogger(EjbService.class);

   @PersistenceContext(unitName = "APPL-UNIT")
   private EntityManager cibet2;

   public String logThis(String msg) {
      String m = "EjbService.logThis called with: " + msg;
      log.info(m);
      return m;
   }

   public String logThisForAspect(String msg) {
      String m = "EjbService.logThisForAspect called with: " + msg;
      log.info(m);
      return m;
   }

   public <T> T merge(T entity) {
      log.debug("start EjbService.merge");
      return cibet2.merge(entity);
   }

   public Object find(Class clazz, Object id) {
      log.debug("find lazy");
      cibet2.clear();
      return cibet2.find(clazz, id);
   }

   public Object findAndLoadComplete(Class clazz, Object id) {
      log.debug("findAndLoadComplete--");
      cibet2.clear();
      Object o = cibet2.find(clazz, id);

      log.debug("before loadLazyEntities");
      CibetUtil.isLoaded(o);
      CibetUtil.loadLazyEntities(o, o.getClass());
      log.debug("after loadLazyEntities");
      CibetUtil.isLoaded(o);
      return o;
   }

   public Object persist(Object entity) {
      log.debug("start EjbService.persist");
      cibet2.persist(entity);
      return entity;
   }

}
