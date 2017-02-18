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
package com.cibethelper.base;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

import javax.sql.DataSource;

public class CibetTestDataSource implements DataSource {

   // private static Logger log = Logger.getLogger(CibetTestDataSource.class);

   private static final String PROPERTIES = "META-INF/jdbc-connection.properties";
   private static final String URL = "db.url";
   private static final String USER = "db.user";
   private static final String PASSWORD = "db.password";

   @Override
   public PrintWriter getLogWriter() throws SQLException {
      return null;
   }

   @Override
   public void setLogWriter(PrintWriter out) throws SQLException {
   }

   @Override
   public void setLoginTimeout(int seconds) throws SQLException {
   }

   @Override
   public int getLoginTimeout() throws SQLException {
      return 0;
   }

   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException {
      return null;
   }

   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
   }

   @Override
   public Connection getConnection() throws SQLException {
      Properties props = new Properties();
      try {
         props.load(Thread.currentThread().getContextClassLoader().getResourceAsStream(PROPERTIES));
      } catch (IOException e) {
         throw new SQLException(e);
      }
      return DriverManager.getConnection(props.getProperty(URL), props.getProperty(USER), props.getProperty(PASSWORD));
   }

   @Override
   public Connection getConnection(String username, String password) throws SQLException {
      return null;
   }

   @Override
   public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return null;
   }

}
