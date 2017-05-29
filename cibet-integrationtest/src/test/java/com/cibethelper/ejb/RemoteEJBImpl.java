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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

import org.apache.log4j.Logger;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.lock.AlreadyLockedException;
import com.logitags.cibet.actuator.lock.Locker;
import com.logitags.cibet.context.CibetContextInterceptor;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.sensor.ejb.CibetInterceptor;
import com.logitags.cibet.sensor.http.HttpMethod;

@Stateless
@Remote
public class RemoteEJBImpl implements RemoteEJB {

   private static Logger log = Logger.getLogger(RemoteEJBImpl.class);

   @PersistenceContext(unitName = "APPL-UNIT")
   private EntityManager em;

   @Resource
   private SessionContext ejbCtx;

   @EJB
   private SimpleEjb testEjb;

   @Override
   @Interceptors({ CibetContextInterceptor.class })
   public <T> T update(T entity) throws Exception {
      log.debug("merge " + entity);

      Context.requestScope().setScheduledDate(Calendar.SECOND, 2);
      return em.merge(entity);
   }

   @Override
   @Interceptors({ CibetContextInterceptor.class })
   public <T> T persist(T entity) {
      log.debug("start CibetRemoteEJBImpl.persist");

      Principal p = ejbCtx.getCallerPrincipal();
      String name = p.getName();
      log.debug("----------principal: " + p + " ||| " + name);
      log.debug("tenant: " + Context.sessionScope().getTenant());

      Context.requestScope().setScheduledDate(Calendar.SECOND, 2);

      em.persist(entity);
      return entity;
   }

   @Override
   @Interceptors({ CibetInterceptor.class })
   public TEntity storeTEntityParallel(TEntity te) {
      te.setCounter(te.getCounter() + 1);
      if (!Context.requestScope().isPostponed()) {
         log.debug("store " + te);
         em.persist(te);
      }
      return te;
   }

   @Override
   @Interceptors({ CibetContextInterceptor.class, CibetInterceptor.class })
   public EventResult callTransitiveEjb(TEntity te) {
      Context.requestScope().setScheduledDate(Calendar.SECOND, 3);
      TEntity te2 = testEjb.storeTEntityForTimerTask(te);
      log.debug("storeTEntityForTimerTask result: " + te2);
      EventResult ev = Context.requestScope().getExecutedEventResult();
      log.debug(ev);
      return ev;
   }

   @Override
   @Interceptors({ CibetContextInterceptor.class })
   public EventResult executeUpdateQuery(String qn, Object... objects) {
      log.debug("execute query: " + qn);
      Context.requestScope().setScheduledDate(Calendar.SECOND, 3);
      Query q = em.createNativeQuery(qn);
      int i = 1;
      for (Object ob : objects) {
         q.setParameter(i, ob);
         i++;
      }

      q.executeUpdate();
      EventResult ev = Context.requestScope().getExecutedEventResult();
      return ev;
   }

   public DcControllable lock(String url) throws AlreadyLockedException {
      DcControllable lo = Locker.lock(url, HttpMethod.GET, ControlEvent.INVOKE, "testremark");
      return lo;
   }

   public List<Object> testInvoke(String str1, int int1, int int2, byte[] bytes1, TEntity entity, Long long1) {
      log.info("start CibetTestEJBImpl.testInvoke");
      List<Object> list = new ArrayList<Object>();
      list.add(str1);
      list.add(int1);
      list.add(int2);
      list.add(bytes1);
      list.add(entity);
      list.add(long1);
      return list;
   }

}
