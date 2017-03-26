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
package com.logitags.cibet.sensor.jdbc.driver;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.Context;
import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;

/**
 * Implementation of Driver interface that acts as a Cibet sensor for JDBC requests. URLs are formatted like this:
 * jdbc:cibet:native sql driver class:native URL (without leading jdbc:)
 * <p>
 * Examples:
 * <p>
 * jdbc:cibet:oracle.jdbc.OracleDriver:oracle:thin:@dbms:1521:DB
 * <p>
 * jdbc:cibet:org.apache.derby.jdbc.ClientDriver:derby://localhost:1527/ cibettest
 * <p>
 * jdbc:cibet:com.mysql.jdbc.Driver:mysql://192.168.1.5:3306/test
 * 
 */
public class CibetDriver implements Driver {

   private static Log log = LogFactory.getLog(CibetDriver.class);

   public static final String CIBET_PREFIX = "jdbc:cibet:";

   static {
      register();
   }

   public static void register() {
      try {
         CibetDriver driverInst = new CibetDriver();
         DriverManager.registerDriver(driverInst);
         log.info("register Cibet JDBC driver");
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public Connection connect(String url, Properties info) throws SQLException {
      if (!acceptsURL(url))
         return null;
      Driver nativeDriver = parseNativeDriver(url);
      Connection conn = nativeDriver.connect(parseNativeUrl(url), info);

      Connection cibetConnection = new CibetConnection(conn);
      Context.internalRequestScope().setApplicationEntityManager(new JdbcBridgeEntityManager(cibetConnection));
      return cibetConnection;
   }

   @Override
   public boolean acceptsURL(String url) throws SQLException {
      return url.startsWith(CIBET_PREFIX);
   }

   @Override
   public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
      if (!acceptsURL(url)) {
         log.error("CibetDriver does not accept URLS of type " + url);
         return new DriverPropertyInfo[] {};
      }
      Driver nativeDriver = parseNativeDriver(url);
      return nativeDriver.getPropertyInfo(parseNativeUrl(url), info);
   }

   @Override
   public int getMajorVersion() {
      return 1;
   }

   @Override
   public int getMinorVersion() {
      return 0;
   }

   @Override
   public boolean jdbcCompliant() {
      return false;
   }

   private Driver parseNativeDriver(String url) throws SQLException {
      int thirdColon = thirdColon(url);
      String driverName = url.substring(CIBET_PREFIX.length(), thirdColon);
      try {
         Class.forName(driverName);
      } catch (ClassNotFoundException e) {
         throw new CibetJdbcException(
               "Failed to instantiate JDBC driver " + driverName + " parsed from URL " + url + ": " + e.getMessage(),
               e);
      }
      String nativeUrl = "jdbc" + url.substring(thirdColon);
      return DriverManager.getDriver(nativeUrl);

   }

   private int thirdColon(String url) {
      int colon = url.indexOf(":", CIBET_PREFIX.length());
      if (colon == -1) {
         throw new CibetJdbcException("Failed to parse JDBC connection URL " + url
               + ": The URL is not in format jdbc:cibet:<nativedriver class name>:"
               + "<native connection URL without leading jdbc:>");
      }
      return colon;
   }

   private String parseNativeUrl(String url) {
      return "jdbc" + url.substring(thirdColon(url));
   }

   @Override
   public Logger getParentLogger() throws SQLFeatureNotSupportedException {
      return LogManager.getLogManager().getLogger(this.getClass().getName());
   }

}
