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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Remote;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.cibethelper.base.CibetTestDataSource;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.Setpoint;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;

@Stateless
@Remote
@TransactionManagement(value = TransactionManagementType.BEAN)
public class JdbcEjb implements JdbcEjbInterface {

   private static Logger log = Logger.getLogger(JdbcEjb.class);

   @Resource
   private SessionContext ejbCtx;

   @CibetContext
   public int executeJdbc(String sql, boolean rollback) {
      Context.sessionScope().setUser("Klabautermann");
      Context.sessionScope().setTenant("testTenant");

      DataSource dataSource = new CibetTestDataSource();
      Connection con = null;
      int count;
      try {
         con = dataSource.getConnection();
         Statement st = con.createStatement();
         count = st.executeUpdate(sql);
      } catch (SQLException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      } finally {
         try {
            if (rollback) {
               Context.requestScope().setRollbackOnly(true);
               // ejbCtx.setRollbackOnly();
            } else {
               con.commit();
            }
            if (con != null) {
               con.close();
            }
         } catch (SQLException e) {
            log.error(e.getMessage(), e);
         }
      }

      return count;
   }

   public String registerSetpoint(String clazz, List<String> acts, ControlEvent... events) {
      Setpoint sp = new Setpoint(String.valueOf(new Date().getTime()));
      if (clazz != null) {
         sp.addTargetIncludes(clazz);
      }
      sp.addEventIncludes(events);
      Configuration cman = Configuration.instance();
      for (String scheme : acts) {
         sp.addActuator(cman.getActuator(scheme));
      }
      cman.registerSetpoint(sp);
      return sp.getId();
   }

   public void unregisterSetpoint(String id) {
      Configuration.instance().unregisterSetpoint(null, id);
   }

   @CibetContext
   public void release(Controllable dc) throws Exception {
      log.debug("now release");

      Connection con = null;
      try {
         Context.sessionScope().setUser("test2");
         Context.sessionScope().setTenant("testTenant");
         DataSource dataSource = new CibetTestDataSource();
         con = dataSource.getConnection();
         dc.release(new JdbcBridgeEntityManager(con), null);
      } finally {
         if (con != null) {
            try {
               con.commit();
               con.close();
            } catch (SQLException e) {
               log.error(e.getMessage(), e);
            }
         }
      }
   }

}
