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
package com.logitags.cibet.actuator.scheduler;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;

public class SESchedulerTask extends TimerTask implements SchedulerTask {

   private static Log log = LogFactory.getLog(SESchedulerTask.class);

   private Timer timer;

   protected SchedulerTimerConfig timerConfig;

   private EntityManagerFactory EMF;

   public void startTimer(SchedulerTimerConfig timerConfig, Date timerStart, long timerPeriod) {
      stopTimer(timerConfig.getSchedulerName());

      this.timerConfig = timerConfig;
      timer = new Timer(true);
      timer.scheduleAtFixedRate(this, timerStart, timerPeriod * 1000);
      log.info("start SE timer for " + timerConfig.getSchedulerName());
   }

   public void stopTimer(String timerName) {
      if (timer != null) {
         log.info("terminate SE timer with name " + timerName);
         timer.cancel();
      }
   }

   @Override
   public void run() {
      log.info("run Timer " + timerConfig.getSchedulerName());
      EntityManager appEm = null;
      if (EMF == null && timerConfig.getPersistenceReference() != null) {
         EMF = Persistence.createEntityManagerFactory(timerConfig.getPersistenceReference());
      }

      try {
         Context.internalRequestScope().setManaged(false);
         Context.start();

         if (EMF != null) {
            appEm = EMF.createEntityManager();
            appEm.getTransaction().begin();
            Context.internalRequestScope().setApplicationEntityManager(appEm);
         }

         Context.sessionScope().setUser("SchedulerTask-" + timerConfig.getSchedulerName());

         EntityManager em = Context.internalRequestScope().getEntityManager();
         TypedQuery<DcControllable> q = em.createNamedQuery(DcControllable.SEL_SCHED_BY_DATE, DcControllable.class);
         q.setParameter("actuator", timerConfig.getSchedulerName());
         q.setParameter("currentDate", new Date(), TemporalType.TIMESTAMP);
         List<DcControllable> list = q.getResultList();
         log.info(list.size() + " due scheduled business cases found");
         for (DcControllable co : list) {
            co.decrypt();
            process(co);

         }

         if (appEm != null) {
            appEm.getTransaction().commit();
            appEm.close();
         }

      } catch (Exception e) {
         log.error(e.getMessage(), e);
         if (appEm != null) {
            appEm.getTransaction().rollback();
            appEm.close();
         }
         Context.requestScope().setRollbackOnly(true);
      } finally {
         Context.end();
      }
   }

   /**
    * ejb: apply
    * 
    * jdbc: jpa UPDATE: load original update and merge original
    * 
    * jpa SELECT, INSERT/DELETE apply
    * 
    * jpaQuery: apply
    * 
    * pojo: apply
    * 
    * servlet: apply
    * 
    * @param co
    */
   protected void process(DcControllable co) {
      ExecutionStatus status = null;
      SchedulerTaskInterceptor interceptor = null;
      try {
         SchedulerActuator schedact = (SchedulerActuator) Configuration.instance().getActuator(co.getActuator());
         interceptor = schedact.getBatchInterceptor();
         if (interceptor != null) {
            interceptor.beforeTask(co);
         }

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, controlEventForRelease(co));
         Context.requestScope().setCaseId(co.getCaseId());
         Context.internalRequestScope().setProperty(InternalRequestScope.DCCONTROLLABLE, co);
         Context.sessionScope().setTenant(co.getTenant());

         try {
            Object result = co.getResource().getResourceHandler().apply(co.getControlEvent());
            try {
               CibetUtil.encode(result);
               co.getResource().setResultObject(result);
            } catch (IOException e) {
               log.info("Cannot set apply result object " + result.getClass().getName() + " into Resource: "
                     + e.getMessage());
            }

            status = Context.internalRequestScope().getExecutedEventResult().getExecutionStatus();
            log.debug("status=" + status);
         } catch (Exception e) {
            log.error(e.getMessage(), e);
            status = ExecutionStatus.ERROR;
         }
      } catch (RejectException e) {
         status = ExecutionStatus.REJECTED;
         log.info("Business case has been rejected by " + interceptor.getClass().getName() + ": " + e.getMessage());

      } finally {
         log.debug("finally");
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
         Context.internalRequestScope().removeProperty(InternalRequestScope.DCCONTROLLABLE);
         Context.requestScope().setCaseId(null);
         Context.sessionScope().setTenant(null);

         co.setExecutionStatus(status);
         co.setExecutionDate(new Date());

         if (interceptor != null) {
            interceptor.afterTask(co);
         }

         if (!Context.requestScope().isPlaying()) {
            if (co.getResource().isEncrypted()) {
               co.encrypt();
            }

            co = Context.internalRequestScope().getEntityManager().merge(co);
         }
      }
   }

   protected ControlEvent controlEventForRelease(DcControllable co) {
      switch (co.getControlEvent()) {
      case INVOKE:
         return ControlEvent.RELEASE_INVOKE;
      case DELETE:
         return ControlEvent.RELEASE_DELETE;
      case INSERT:
         return ControlEvent.RELEASE_INSERT;
      case UPDATE:
         return ControlEvent.RELEASE_UPDATE;
      case SELECT:
         return ControlEvent.RELEASE_SELECT;
      default:
         String msg = "Controlled object [" + co.getDcControllableId() + "] with control event " + co.getControlEvent()
               + " cannot be released";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
   }

}
