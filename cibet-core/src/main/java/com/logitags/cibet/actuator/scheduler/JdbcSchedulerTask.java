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

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.context.Context;

public class JdbcSchedulerTask extends SESchedulerTask implements SchedulerTask {

   private static Log log = LogFactory.getLog(JdbcSchedulerTask.class);

   private DataSource dataSource;

   @Override
   public void run() {
      log.info("run Timer " + timerConfig.getSchedulerName());

      if (dataSource == null && timerConfig.getPersistenceReference() != null) {
         try {
            javax.naming.Context ctx = new InitialContext();
            dataSource = (DataSource) ctx.lookup(timerConfig.getPersistenceReference());
         } catch (NamingException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }
      }

      Connection conn = null;

      try {
         Context.internalRequestScope().setManaged(false);
         Context.start();

         if (dataSource != null) {
            try {
               conn = dataSource.getConnection();
            } catch (SQLException e) {
               log.error(e.getMessage(), e);
               throw new RuntimeException(e);
            }

            Class<EntityManager> clazz = (Class<EntityManager>) Class
                  .forName("com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager");
            Constructor<EntityManager> constr = clazz.getConstructor(Connection.class);
            EntityManager appEm = constr.newInstance(conn);
            Context.internalRequestScope().setApplicationEntityManager(appEm);
         }

         Context.sessionScope().setUser("SchedulerTask-" + timerConfig.getSchedulerName());

         EntityManager em = Context.internalRequestScope().getOrCreateEntityManager(false);
         TypedQuery<Controllable> q = em.createNamedQuery(Controllable.SEL_SCHED_BY_DATE, Controllable.class);
         q.setParameter("actuator", timerConfig.getSchedulerName());
         q.setParameter("currentDate", new Date(), TemporalType.TIMESTAMP);
         List<Controllable> list = q.getResultList();
         for (Controllable co : list) {
            co.decrypt();
            process(co);

         }

         if (conn != null) {
            conn.commit();
         }

      } catch (Exception e) {
         log.error(e.getMessage(), e);
         if (conn != null) {
            try {
               conn.rollback();
            } catch (SQLException e1) {
               log.error(e1.getMessage(), e1);
            }
         }
         Context.requestScope().setRollbackOnly(true);
      } finally {
         Context.end();
         if (conn != null) {
            try {
               conn.close();
            } catch (SQLException e) {
               log.error(e.getMessage(), e);
            }
         }
      }
   }

}
