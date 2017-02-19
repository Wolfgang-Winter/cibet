/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 *******************************************************************************
 */
package com.logitags.cibet.sensor.jdbc.bridge;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityTransaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class JdbcBridgeEntityTransaction implements EntityTransaction {

   private Log log = LogFactory.getLog(JdbcBridgeEntityTransaction.class);

   private Connection jdbcConnection;

   public JdbcBridgeEntityTransaction(Connection conn) {
      jdbcConnection = conn;
   }

   @Override
   public void begin() {
      // ignore
   }

   @Override
   public void commit() {
      try {
         if (jdbcConnection != null) {
            jdbcConnection.commit();
         } else {
            throw new CibetJdbcException(
                  "Connection created from DataSource can not be committed. "
                        + "It is possibly a managed connection and should be commited by a container.");
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public boolean getRollbackOnly() {
      // ignore
      return false;
   }

   @Override
   public boolean isActive() {
      try {
         if (jdbcConnection != null) {
            return !jdbcConnection.isClosed();
         } else {
            log.warn("jdbcConnection is null. Don't call this method when using "
                  + "a DataSource with JdbcBridgeEntityManager");
            return false;
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void rollback() {
      try {
         if (jdbcConnection != null) {
            jdbcConnection.rollback();
         } else {
            throw new CibetJdbcException(
                  "Connection created from DataSource can not be rolled back. "
                        + "It is possibly a managed connection and should be rolled back by a container.");
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setRollbackOnly() {
      // ignore
   }

}
