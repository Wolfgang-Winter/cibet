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

import javax.annotation.Resource;
import javax.ejb.SessionContext;
import javax.ejb.Stateless;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.cibethelper.base.CibetTestDataSource;

@Stateless
public class JdbcEjb {

   private static Logger log = Logger.getLogger(JdbcEjb.class);

   @Resource
   private SessionContext ejbCtx;

   public int executeJdbc(String sql, boolean rollback) {
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
         if (con != null)
            try {
               con.close();
            } catch (SQLException e) {
               log.error(e.getMessage(), e);
            }
      }

      if (rollback)
         ejbCtx.setRollbackOnly();
      return count;
   }

}
