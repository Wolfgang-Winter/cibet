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

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * generates unique IDs based on a database table. Uses Hi, Lo values.
 * Threadsafe because connection is committed when set with method
 * setConnection()
 * 
 */
public class CommittingTableIdGenerator extends TableIdGenerator {

   private static transient Log log = LogFactory
         .getLog(CommittingTableIdGenerator.class);

   private static IdGenerator cInstance;

   public static synchronized IdGenerator getInstance(Connection conn) {
      if (cInstance == null) {
         cInstance = new CommittingTableIdGenerator();
      }
      ((CommittingTableIdGenerator) cInstance).setConnection(conn);
      log.debug(cInstance + " CommittingTableIdGenerator returned");
      return cInstance;
   }

   public static synchronized IdGenerator getInstance(DataSource ds) {
      String msg = "Failed to instantiate CommittingTableIdGenerator"
            + ": Can only be instantiated with Connection, not with DataSource";
      log.error(msg);
      throw new IllegalArgumentException(msg);
   }

   protected CommittingTableIdGenerator() {
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.jdbc.TableIdGenerator#finalizeConnection(java
    * .sql.Connection)
    */
   @Override
   protected void finalizeConnection(Connection conn) throws SQLException {
      if (conn != null && dataSource == null) {
         if (conn.getAutoCommit() == false) {
            log.debug("commit");
            conn.commit();
         }
      }
      super.finalizeConnection(conn);
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.jdbc.TableIdGenerator#rollback(java.sql.Connection
    * )
    */
   @Override
   protected void rollback(Connection conn) throws SQLException {
      if (conn != null && dataSource == null) {
         if (conn.getAutoCommit() == false) {
            log.debug("rollback");
            conn.rollback();
         }
      }
   }

}
