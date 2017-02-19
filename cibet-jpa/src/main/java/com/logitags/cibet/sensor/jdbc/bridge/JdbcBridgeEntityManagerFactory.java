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
package com.logitags.cibet.sensor.jdbc.bridge;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import javax.persistence.Cache;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnitUtil;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.metamodel.Metamodel;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation for creating JdbcBridgeEntityManager instances. DataSource
 * must be in JNDI context under name'java:comp/env/jdbc/CibetJDBC'
 * <p>
 * The IDGenerator implementation must be in JNDI context under name
 * 'java:comp/env/bean/CibetIdGenerator'.
 * 
 * @author Wolfgang
 * 
 */
public class JdbcBridgeEntityManagerFactory implements EntityManagerFactory {

   private static Log log = LogFactory.getLog(JdbcBridgeEntityManagerFactory.class);

   private DataSource dataSource;

   public JdbcBridgeEntityManagerFactory(DataSource ds) {
      dataSource = ds;
   }

   @Override
   public EntityManager createEntityManager() {
      JdbcBridgeEntityManager em;
      try {
         Connection con = dataSource.getConnection();
         con.setAutoCommit(false);
         em = new JdbcBridgeEntityManager(con);
         em.setEntityManagerFactory(this);
         return em;
      } catch (SQLException e) {
         log.error(e.getMessage(), e);
         throw new RuntimeException(e);
      }
   }

   @Override
   public EntityManager createEntityManager(Map map) {
      return null;
   }

   @Override
   public CriteriaBuilder getCriteriaBuilder() {
      return null;
   }

   @Override
   public Metamodel getMetamodel() {
      return null;
   }

   @Override
   public boolean isOpen() {
      return true;
   }

   @Override
   public void close() {
   }

   @Override
   public Map<String, Object> getProperties() {
      return null;
   }

   @Override
   public Cache getCache() {
      return null;
   }

   @Override
   public PersistenceUnitUtil getPersistenceUnitUtil() {
      return null;
   }

}
