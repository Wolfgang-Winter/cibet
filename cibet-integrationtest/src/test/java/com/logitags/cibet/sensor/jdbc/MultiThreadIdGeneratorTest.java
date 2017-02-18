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

package com.logitags.cibet.sensor.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.cibethelper.base.CibetTestDataSource;
import com.cibethelper.base.CoreTestBase;
import com.logitags.cibet.sensor.jdbc.bridge.IdGenerator;
import com.logitags.cibet.sensor.jdbc.bridge.TableIdGenerator;

/**
 *
 */
public class MultiThreadIdGeneratorTest extends CoreTestBase {

   /**
    * logger for tracing
    */
   private static Logger log = Logger.getLogger(MultiThreadIdGeneratorTest.class);

   private static Throwable throwable;

   private class ThreadExecution extends Thread {

      private static final String INS = "insert into CIB_TESTENTITY (ID, NAMEVALUE, COUNTER) VALUES (?,?,?)";

      private int loop;

      public ThreadExecution(String name, int loop) {
         super(name);
         this.loop = loop;
      }

      public void run() {
         Connection conn = null;
         try {
            DataSource ds = new CibetTestDataSource();
            conn = ds.getConnection();
            conn.setAutoCommit(false);
            IdGenerator gen = TableIdGenerator.getInstance(conn);
            PreparedStatement pstmt = conn.prepareStatement(INS);
            for (int i = 1; i < loop; i++) {

               long nextId = gen.nextId("test");
               log.debug("insert " + nextId);

               pstmt.setLong(1, nextId);
               pstmt.setString(2, this.getName());
               pstmt.setInt(3, (int) nextId);
               pstmt.executeUpdate();
               conn.commit();
               // if (i % 40 == 0) {
               // log.debug("commit on loop " + i);
               // conn.commit();
               // }
            }
            // conn.commit();
         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            throwable = e;
            try {
               conn.rollback();
            } catch (SQLException e1) {
               log.error(e1.getMessage(), e1);
            }
         } finally {
            if (conn != null) {
               try {
                  conn.close();
               } catch (SQLException e) {
                  log.error(e.getMessage(), e);
               }
            }
            log.info("connection finished");
         }
      }

   }

   // private synchronized long createUniqueId(DataSource
   // idGenerationDatasource,
   // String sequence) throws SQLException {
   // IdGenerator idGenerator = TableIdGenerator.getInstance();
   // Connection conn = null;
   // Boolean autocommit = null;
   // boolean rollback = false;
   // try {
   // Object[] result = idGenerator.initConnection(idGenerationDatasource);
   // conn = (Connection) result[0];
   // autocommit = (Boolean) result[1];
   // return idGenerator.nextId(conn, sequence);
   // } catch (SQLException e) {
   // rollback = true;
   // throw e;
   // } finally {
   // idGenerator.finalizeConnection(conn, autocommit, rollback);
   // }
   // }

   private void startThreads(int nbr, int loop) throws Exception {

      List<ThreadExecution> tlist = new ArrayList<ThreadExecution>();
      for (int i = 0; i < nbr; i++) {
         ThreadExecution t = new ThreadExecution("thread-" + i, loop);
         tlist.add(t);
      }
      log.info("start threads");
      for (ThreadExecution te : tlist) {
         te.start();
         Thread.sleep(100);
      }
      Thread.sleep(500);
      log.info("join threads");
      for (ThreadExecution te : tlist) {
         te.join();
      }
      Thread.sleep(500);
      log.info("threads joined");
   }

   @Test
   public void nextIdInitialize() throws Exception {
      log.info("start nextIdInitialize()");
      DataSource ds = new CibetTestDataSource();
      Connection conn = ds.getConnection();
      IdGenerator gen = TableIdGenerator.getInstance(conn);
      try {
         long nextId;
         long start = gen.nextId("test");
         log.debug("start=" + start);
         for (long i = start; i < start + 1210; i++) {
            nextId = gen.nextId("test");
            Assert.assertEquals(i + 1, nextId);
         }
         if (conn.getAutoCommit() == false)
            conn.commit();
         start = gen.nextId("test2");
         for (long i = start; i < start + 1210; i++) {
            nextId = gen.nextId("test2");
            Assert.assertEquals(i + 1, nextId);
         }
         if (conn.getAutoCommit() == false)
            conn.commit();
      } finally {
         if (conn != null)
            conn.close();
      }
   }

   @Test
   public void multipleThreads() throws Exception {
      log.info("start multipleThreads()");
      throwable = null;
      int nbr = 5;
      int loop = 50;

      startThreads(nbr, loop);
      if (throwable != null) {
         log.error(throwable.getMessage(), throwable);
      }
      Assert.assertNull(throwable);
   }

}
