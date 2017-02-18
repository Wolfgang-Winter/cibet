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

import java.util.Date;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.naming.Binding;
import javax.naming.InitialContext;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InitializationService;
import com.logitags.cibet.core.CibetException;

@Singleton
public class EESchedulerTask extends SESchedulerTask implements SchedulerTask {

   private static Log log = LogFactory.getLog(EESchedulerTask.class);

   @Resource
   private TimerService timerService;

   @PreDestroy
   public void destroy() {
      log.info("EESchedulerTask destroy");
      Configuration.instance().close();
   }

   @Override
   public void startTimer(SchedulerTimerConfig timerConfig, Date timerStart, long timerPeriod) throws CibetException {
      stopTimer(timerConfig.getSchedulerName());
      timerService.createIntervalTimer(timerStart, timerPeriod * 1000, new TimerConfig(timerConfig, false));
      log.info("start EE timer for " + timerConfig.getSchedulerName());
   }

   @Override
   public void stopTimer(String timerName) {
      for (Timer timer : timerService.getTimers()) {
         if (timerName.equals(((SchedulerTimerConfig) timer.getInfo()).getSchedulerName())) {
            timer.cancel();
            log.info("Scheduler EE Timer " + timerName + " stopped");
            return;
         }
      }
   }

   @Timeout
   public void handleTimer(final Timer timer) {
      SchedulerTimerConfig config = (SchedulerTimerConfig) timer.getInfo();
      log.info("run EE Timer " + config.getSchedulerName());

      try {
         Context.internalRequestScope().setManaged(false);
         InitializationService.instance().startContext(null);
         if (config.getPersistenceReference() != null) {
            setApplicationEntityManager(config.getPersistenceReference());
         }

         Context.sessionScope().setUser("SchedulerTask-" + config.getSchedulerName());

         EntityManager em = Context.internalRequestScope().getEntityManager();
         TypedQuery<DcControllable> q = em.createNamedQuery(DcControllable.SEL_SCHED_BY_DATE, DcControllable.class);
         q.setParameter("actuator", config.getSchedulerName());
         q.setParameter("currentDate", new Date(), TemporalType.TIMESTAMP);
         List<DcControllable> list = q.getResultList();
         log.info(list.size() + " due scheduled business cases found");
         for (DcControllable co : list) {
            co.decrypt();
            process(co);
         }

      } catch (Exception e) {
         log.error("Failed to execute EEScheduledTask Timer " + config.getSchedulerName() + ": " + e.getMessage(), e);
      } finally {
         InitializationService.instance().endContext();
      }
   }

   private EntityManager setApplicationEntityManager(String reference) {
      if (!reference.startsWith("java:comp/env/")) {
         reference = "java:comp/env/" + reference;
      }
      try {
         InitialContext context = new InitialContext();
         EntityManager entityManager = (EntityManager) context.lookup(reference);
         Context.internalRequestScope().setApplicationEntityManager(entityManager);
         return entityManager;
      } catch (NamingException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

   /**
    * Recursively exhaust the JNDI tree
    */
   private static final void listContext(javax.naming.Context ctx, String indent) {
      try {
         NamingEnumeration<Binding> list = ctx.listBindings("");
         while (list.hasMore()) {
            Binding item = (Binding) list.next();
            String className = item.getClassName();
            String name = item.getName();
            log.info(indent + className + " | " + name);
            Object o = item.getObject();
            if (o instanceof javax.naming.Context) {
               listContext((javax.naming.Context) o, indent + "   ");
            }
         }
      } catch (NamingException ex) {
         log.warn("JNDI failure: ", ex);
      }
   }

}
