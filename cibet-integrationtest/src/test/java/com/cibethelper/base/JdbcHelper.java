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
package com.cibethelper.base;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import com.cibethelper.entities.TEntity;
import com.logitags.cibet.context.InitializationService;

public abstract class JdbcHelper extends CoreTestBase {

   private static Logger log = Logger.getLogger(JdbcHelper.class);

   private static final String PROPERTIES = "META-INF/jdbc-connection.properties";
   private static final String URL = "db.url";
   private static final String DBUSER = "db.user";
   private static final String PASSWORD = "db.password";

   // private static final String DEL_STMT = "delete from CIB_SEQUENCE";
   private static final String DEL_TESTENTITY = "delete from cib_testentity";
   private static final String DEL_TPSENTITY = "delete from tpsentity";
   private static final String DEL_ARCHIVE = "delete from cib_archive";
   private static final String DEL_SYNT2ENTITY = "delete from cib_syntetic2entity";
   private static final String DEL_ARCHIVEPARAMETER = "delete from CIB_RESOURCEPARAMETER";
   private static final String DEL_DCCONTROLLABLE = "delete from CIB_DCCONTROLLABLE";
   private static final String DEL_LOCKEDOBJECT = "delete from CIB_LOCKEDOBJECT";

   protected static final String USER = "THE_USER";

   protected Connection connection;

   protected DataSource dataSource = new CibetTestDataSource();

   @BeforeClass
   public static void beforeClassJdbcHelper() throws Exception {
      log.info("call beforeClassJdbcHelper(");
      Field f = InitializationService.class.getDeclaredField("LOCAL_PERSISTENCEUNIT");
      f.setAccessible(true);
      f.set(null, "jdbc-CibetLocal");

      f = InitializationService.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, null);
   }

   @AfterClass
   public static void afterClassJdbcHelper() throws Exception {
      log.info("call afterClassJdbcHelper(");
      Field f = InitializationService.class.getDeclaredField("LOCAL_PERSISTENCEUNIT");
      f.setAccessible(true);
      f.set(null, "CibetLocal");

      f = InitializationService.class.getDeclaredField("instance");
      f.setAccessible(true);
      f.set(null, null);
   }

   @Before
   public void before() throws IOException, SQLException {
      connection = dataSource.getConnection();
   }

   @After
   public void doAfter() {
      log.info("cleaning database ...");
      try {
         PreparedStatement stmt;
         // = connection.prepareStatement(DEL_STMT);
         // int count = stmt.executeUpdate();
         // log.info(count + " records cleaned from table CIB_SEQUENCE");

         stmt = connection.prepareStatement(DEL_TESTENTITY);
         int count = stmt.executeUpdate();
         log.info(count + " records cleaned from table CIB_TESTENTITY");

         stmt = connection.prepareStatement(DEL_TPSENTITY);
         count = stmt.executeUpdate();
         log.info(count + " records cleaned from table tpsentity");

         stmt = connection.prepareStatement(DEL_ARCHIVEPARAMETER);
         count = stmt.executeUpdate();
         log.info(count + " records cleaned from table CIB_RESOURCEPARAMETER");

         stmt = connection.prepareStatement(DEL_ARCHIVE);
         count = stmt.executeUpdate();
         log.info(count + " records cleaned from table CIB_ARCHIVE");

         stmt = connection.prepareStatement(DEL_DCCONTROLLABLE);
         count = stmt.executeUpdate();
         log.info(count + " records cleaned from table CIB_DCCONTROLLABLE");

         stmt = connection.prepareStatement(DEL_LOCKEDOBJECT);
         count = stmt.executeUpdate();
         log.info(count + " records cleaned from table CIB_LOCKEDOBJECT");

         stmt = connection.prepareStatement(DEL_SYNT2ENTITY);
         count = stmt.executeUpdate();
         log.info(count + " records cleaned from table CIB_SYNT2ENTITY");

         if (!connection.getAutoCommit()) {
            connection.commit();
         }
      } catch (Exception e) {
         log.error(e.getMessage(), e);
         if (connection != null) {
            try {
               connection.close();
            } catch (SQLException e1) {
               log.error(e1.getMessage(), e1);
            }
         }
      }
   }

   public static Connection createConnection() throws IOException, SQLException {
      Properties props = new Properties();
      props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES));

      return DriverManager.getConnection(props.getProperty(URL), props.getProperty(DBUSER),
            props.getProperty(PASSWORD));
   }

   protected TEntity find(long id) throws SQLException {
      PreparedStatement stmt = connection
            .prepareStatement("select nameValue, counter, owner from CIB_TESTENTITY where id = " + id);
      ResultSet rs = stmt.executeQuery();
      if (rs.next()) {
         TEntity te = new TEntity(rs.getString(1), rs.getInt(2), rs.getString(3));
         te.setId(id);
         return te;
      } else {
         return null;
      }
   }
}
