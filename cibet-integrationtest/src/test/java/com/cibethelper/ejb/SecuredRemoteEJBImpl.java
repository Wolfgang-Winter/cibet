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

import java.security.Principal;
import java.util.List;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.log4j.Logger;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.lock.AlreadyLockedException;
import com.logitags.cibet.context.CibetContextInterceptor;
import com.logitags.cibet.core.EventResult;

/**
 * secured in jboss-ejb3.xml
 * 
 * @author Wolfgang
 *
 */
@Stateless
@Remote
public class SecuredRemoteEJBImpl implements RemoteEJB {

   private static Logger log = Logger.getLogger(SecuredRemoteEJBImpl.class);

   @PersistenceContext(unitName = "APPL-UNIT")
   private EntityManager em;

   @Resource
   private SessionContext ejbCtx;

   @Override
   @Interceptors({ CibetContextInterceptor.class })
   public <T> T update(T entity) throws Exception {
      log.debug("merge " + entity);
      return em.merge(entity);
   }

   @Override
   @Interceptors({ CibetContextInterceptor.class })
   @PermitAll
   public <T> T persist(T entity) {
      log.debug("start SecuredCibetRemoteEJBImpl.persist");

      Principal p = ejbCtx.getCallerPrincipal();
      String name = p.getName();
      log.debug("----------principal: " + p + " ||| " + name);

      em.persist(entity);
      return entity;
   }

   @Override
   public TEntity storeTEntityParallel(TEntity te) {
      return null;
   }

   @Override
   public EventResult callTransitiveEjb(TEntity te) {
      return null;
   }

   @Override
   public EventResult executeUpdateQuery(String qn, Object... objects) {
      return null;
   }

   @Override
   public Controllable lock(String targetType) throws AlreadyLockedException {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public List<Object> testInvoke(String str1, int int1, int int2, byte[] bytes1, TEntity entity, Long long1) {
      // TODO Auto-generated method stub
      return null;
   }

}
