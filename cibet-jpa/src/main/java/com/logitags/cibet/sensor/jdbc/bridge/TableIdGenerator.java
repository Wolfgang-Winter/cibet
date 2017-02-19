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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.sensor.jdbc.driver.CibetConnection;

/**
 * generates unique IDs based on a database table. Uses Hi, Lo values. Not threadsafe because connection is not
 * committed.
 * 
 */
public class TableIdGenerator implements IdGenerator {

   private static transient Log log = LogFactory.getLog(TableIdGenerator.class);

   private static final String SEL_STMT = "select HI, LASTUPDATE from CIB_SEQUENCE where SEQUENCE = ?";

   private static final String UPD_STMT = "update CIB_SEQUENCE set HI = ?, LASTUPDATE = ? where SEQUENCE = ?";

   private static final String INS_STMT = "insert into CIB_SEQUENCE (SEQUENCE, HI, LASTUPDATE) values (?,?,?)";

   private static Map<String, Long> currentHiMap = Collections.synchronizedMap(new HashMap<String, Long>());

   private static Map<String, Long> currentLoMap = Collections.synchronizedMap(new HashMap<String, Long>());

   private static final long RANGE = 100;

   private static IdGenerator instance;

   protected Connection connection;

   protected DataSource dataSource;

   public static synchronized IdGenerator getInstance(Connection conn) {
      if (instance == null) {
         instance = new TableIdGenerator();
      }
      ((TableIdGenerator) instance).setConnection(conn);
      log.debug(instance + " TableIdGenerator returned");
      return instance;
   }

   public static synchronized IdGenerator getInstance(DataSource ds) {
      if (instance == null) {
         instance = new TableIdGenerator();
      }
      ((TableIdGenerator) instance).setDataSource(ds);
      log.debug(instance + " TableIdGenerator returned");
      return instance;
   }

   protected TableIdGenerator() {
   }

   /**
    * @param con
    *           the connection to set
    */
   protected void setConnection(Connection con) {
      if (con == null) {
         throw new IllegalArgumentException("Failed to instantiate TableIdGenerator: connection is null");
      }
      this.connection = con;
      dataSource = null;
   }

   /**
    * @param ds
    *           the dataSource to set
    */
   protected void setDataSource(DataSource ds) {
      if (ds == null) {
         throw new IllegalArgumentException("Failed to instantiate TableIdGenerator: dataSource is null");
      }
      this.dataSource = ds;
      this.connection = null;
   }

   private static class Sequence {
      public String sequence;
      public Timestamp lastUpdate;
      public long hi;
   }

   @Override
   public synchronized long nextId(String sequence) {
      Long currentHi = currentHiMap.get(sequence);
      Long currentLo = currentLoMap.get(sequence);
      if (currentHi == null || currentLo >= RANGE) {
         // load new node
         try {
            currentHi = nextHi(sequence) * RANGE;
         } catch (SQLException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException(e);
         }
         currentHiMap.put(sequence, currentHi);
         currentLo = 0L;
      }

      currentLo++;
      currentLoMap.put(sequence, currentLo);
      return currentHi + currentLo;
   }

   protected synchronized long nextHi(String sequence) throws SQLException {
      PreparedStatement pstmt = null;
      Connection conn = getNativeConnection();

      try {
         Sequence seq = loadSequence(conn, sequence);
         pstmt = conn.prepareStatement(UPD_STMT);
         pstmt.setLong(1, seq.hi + 1);
         pstmt.setTimestamp(2, new Timestamp(Calendar.getInstance().getTimeInMillis()));
         pstmt.setString(3, sequence);
         pstmt.executeUpdate();
         if (log.isDebugEnabled()) {
            log.debug("new HI value " + seq.hi + " for sequence " + seq.sequence);
         }
         return seq.hi;
      } catch (SQLException e) {
         log.error(e.getMessage(), e);
         rollback(conn);
         throw e;
      } finally {
         if (pstmt != null) pstmt.close();
         finalizeConnection(conn);
      }
   }

   protected void finalizeConnection(Connection conn) throws SQLException {
      if (conn != null) {
         if (dataSource != null) {
            log.debug("close now connection");
            Context.internalRequestScope().removeProperty(InternalRequestScope.USE_NATIVE_DRIVER_FOR_ID_GENERATION);
            // this code works only with java 1.6 and above
            // if (conn.isWrapperFor(CibetConnection.class)) {
            // CibetConnection cc = conn.unwrap(CibetConnection.class);
            // cc.setUseNative(false);
            // }
            conn.close();
         } else {
            // conn.commit();
         }
      }
   }

   protected void rollback(Connection conn) throws SQLException {
      // no commit, no rollback
   }

   protected synchronized Sequence loadSequence(Connection conn, String sequence) throws SQLException {
      ResultSet rs = null;
      PreparedStatement pstmt = null;
      log.debug(this.getClass().getName() + " ..IDGEN.. : " + this);
      try {
         Sequence seq = new Sequence();
         seq.sequence = sequence;
         pstmt = conn.prepareStatement(SEL_STMT);
         pstmt.setString(1, sequence);
         rs = pstmt.executeQuery();
         if (rs.next()) {
            seq.hi = rs.getLong(1);
            seq.lastUpdate = rs.getTimestamp(2);
         } else {
            log.info("initialize sequence " + sequence);
            seq.hi = 1;
            seq.lastUpdate = new Timestamp(Calendar.getInstance().getTimeInMillis());
            pstmt = conn.prepareStatement(INS_STMT);
            pstmt.setString(1, sequence);
            pstmt.setLong(2, seq.hi);
            pstmt.setTimestamp(3, seq.lastUpdate);
            pstmt.executeUpdate();
         }
         return seq;
      } finally {
         if (rs != null) rs.close();
         if (pstmt != null) pstmt.close();
      }
   }

   private Connection getNativeConnection() throws SQLException {
      // EntityManager em = Context.requestScope().getEntityManager();
      // if (em.getDelegate() instanceof Connection) {
      // return (Connection) em.getDelegate();
      // } else if (em.getDelegate() instanceof DataSource) {
      // return ((DataSource) em.getDelegate()).getConnection();
      // } else {
      // throw new RuntimeException(
      // "getDelegate() of EntityManager in Request scope returns neither Connection nor Datasource but "
      // + em.getDelegate()
      // + ". The EntityManager must be of type JdbcBridgeEntityManager");
      // }

      Connection con;
      if (dataSource != null) {
         con = dataSource.getConnection();
         log.debug("Connection from DataSource " + dataSource + ", connection: " + con);
         // if (con.getTypeMap().containsKey(CibetConnection.CIBETDRIVER_TYPE))
         // {
         // log.debug("this is a CibetDriver");
         Context.internalRequestScope().setProperty(InternalRequestScope.USE_NATIVE_DRIVER_FOR_ID_GENERATION, true);
         // }

         // if (con.isWrapperFor(CibetConnection.class)) {
         // CibetConnection cc = con.unwrap(CibetConnection.class);
         // log.debug("set to native, was " + cc.isUseNative());
         // cc.setUseNative(true);
         // }

      } else {
         con = connection;
      }
      if (con instanceof CibetConnection) {
         log.debug("is CibetConnection");
         con = ((CibetConnection) con).getNativeConnection();
      }
      log.debug("return Connection " + con);

      return con;
   }
}
