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

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Implementation of Connection interface that acts as a Cibet sensor for JDBC requests.
 * 
 */
public class CibetConnection implements Connection {

   private static transient Log log = LogFactory.getLog(CibetConnection.class);

   private Connection nativeConnection;

   public CibetConnection(Connection conn) {
      nativeConnection = conn;
   }

   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException {
      return nativeConnection.unwrap(iface);
   }

   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return nativeConnection.isWrapperFor(iface);
   }

   @Override
   public Statement createStatement() throws SQLException {
      Statement st = nativeConnection.createStatement();
      return new CibetStatement(this, st);
   }

   @Override
   public PreparedStatement prepareStatement(String sql) throws SQLException {
      log.debug("enter prepareStatement(String sql)");
      PreparedStatement ps = nativeConnection.prepareStatement(sql);
      return new CibetPreparedStatement(this, ps, sql);
   }

   @Override
   public CallableStatement prepareCall(String sql) throws SQLException {
      return nativeConnection.prepareCall(sql);
   }

   @Override
   public String nativeSQL(String sql) throws SQLException {
      return nativeConnection.nativeSQL(sql);
   }

   @Override
   public void setAutoCommit(boolean autoCommit) throws SQLException {
      nativeConnection.setAutoCommit(autoCommit);
   }

   @Override
   public boolean getAutoCommit() throws SQLException {
      return nativeConnection.getAutoCommit();
   }

   @Override
   public void commit() throws SQLException {
      nativeConnection.commit();
   }

   @Override
   public void rollback() throws SQLException {
      nativeConnection.rollback();
   }

   @Override
   public void close() throws SQLException {
      log.debug("close connection");
      // CibetContext.removeProperty(CibetContext.USE_NATIVE_DRIVER);
      nativeConnection.close();
   }

   @Override
   public boolean isClosed() throws SQLException {
      return nativeConnection.isClosed();
   }

   @Override
   public DatabaseMetaData getMetaData() throws SQLException {
      return nativeConnection.getMetaData();
   }

   @Override
   public void setReadOnly(boolean readOnly) throws SQLException {
      nativeConnection.setReadOnly(readOnly);
   }

   @Override
   public boolean isReadOnly() throws SQLException {
      return nativeConnection.isReadOnly();
   }

   @Override
   public void setCatalog(String catalog) throws SQLException {
      nativeConnection.setCatalog(catalog);
   }

   @Override
   public String getCatalog() throws SQLException {
      return nativeConnection.getCatalog();
   }

   @Override
   public void setTransactionIsolation(int level) throws SQLException {
      nativeConnection.setTransactionIsolation(level);
   }

   @Override
   public int getTransactionIsolation() throws SQLException {
      return nativeConnection.getTransactionIsolation();
   }

   @Override
   public SQLWarning getWarnings() throws SQLException {
      return nativeConnection.getWarnings();
   }

   @Override
   public void clearWarnings() throws SQLException {
      nativeConnection.clearWarnings();
   }

   @Override
   public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
      Statement st = nativeConnection.createStatement(resultSetType, resultSetConcurrency);
      return new CibetStatement(this, st);
   }

   @Override
   public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
         throws SQLException {
      PreparedStatement ps = nativeConnection.prepareStatement(sql, resultSetType, resultSetConcurrency);
      return new CibetPreparedStatement(this, ps, sql);
   }

   @Override
   public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
      return nativeConnection.prepareCall(sql, resultSetType, resultSetConcurrency);
   }

   @Override
   public Map<String, Class<?>> getTypeMap() throws SQLException {
      return nativeConnection.getTypeMap();
      // Map<String, Class<?>> map = nativeConnection.getTypeMap();
      // map.put(CIBETDRIVER_TYPE, this.getClass());
      // return map;
   }

   @Override
   public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
      nativeConnection.setTypeMap(map);
   }

   @Override
   public void setHoldability(int holdability) throws SQLException {
      nativeConnection.setHoldability(holdability);
   }

   @Override
   public int getHoldability() throws SQLException {
      return nativeConnection.getHoldability();
   }

   @Override
   public Savepoint setSavepoint() throws SQLException {
      return nativeConnection.setSavepoint();
   }

   @Override
   public Savepoint setSavepoint(String name) throws SQLException {
      return nativeConnection.setSavepoint(name);
   }

   @Override
   public void rollback(Savepoint savepoint) throws SQLException {
      nativeConnection.rollback(savepoint);
   }

   @Override
   public void releaseSavepoint(Savepoint savepoint) throws SQLException {
      nativeConnection.releaseSavepoint(savepoint);
   }

   @Override
   public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
         throws SQLException {
      Statement st = nativeConnection.createStatement(resultSetType, resultSetConcurrency, resultSetHoldability);
      return new CibetStatement(this, st);
   }

   @Override
   public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
         int resultSetHoldability) throws SQLException {
      PreparedStatement ps = nativeConnection.prepareStatement(sql, resultSetType, resultSetConcurrency,
            resultSetHoldability);
      return new CibetPreparedStatement(this, ps, sql);
   }

   @Override
   public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
         int resultSetHoldability) throws SQLException {
      return nativeConnection.prepareCall(sql, resultSetType, resultSetConcurrency, resultSetHoldability);
   }

   @Override
   public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
      PreparedStatement ps = nativeConnection.prepareStatement(sql, autoGeneratedKeys);
      return new CibetPreparedStatement(this, ps, sql);
   }

   @Override
   public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
      PreparedStatement ps = nativeConnection.prepareStatement(sql, columnIndexes);
      return new CibetPreparedStatement(this, ps, sql);
   }

   @Override
   public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
      PreparedStatement ps = nativeConnection.prepareStatement(sql, columnNames);
      return new CibetPreparedStatement(this, ps, sql);
   }

   @Override
   public Clob createClob() throws SQLException {
      return nativeConnection.createClob();
   }

   @Override
   public Blob createBlob() throws SQLException {
      return nativeConnection.createBlob();
   }

   @Override
   public NClob createNClob() throws SQLException {
      return nativeConnection.createNClob();
   }

   @Override
   public SQLXML createSQLXML() throws SQLException {
      return nativeConnection.createSQLXML();
   }

   @Override
   public boolean isValid(int timeout) throws SQLException {
      return nativeConnection.isValid(timeout);
   }

   @Override
   public void setClientInfo(String name, String value) throws SQLClientInfoException {
      nativeConnection.setClientInfo(name, value);
   }

   @Override
   public void setClientInfo(Properties properties) throws SQLClientInfoException {
      nativeConnection.setClientInfo(properties);
   }

   @Override
   public String getClientInfo(String name) throws SQLException {
      return nativeConnection.getClientInfo(name);
   }

   @Override
   public Properties getClientInfo() throws SQLException {
      return nativeConnection.getClientInfo();
   }

   @Override
   public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
      return nativeConnection.createArrayOf(typeName, elements);
   }

   @Override
   public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
      return nativeConnection.createStruct(typeName, attributes);
   }

   /**
    * @return the nativeConnection
    */
   public Connection getNativeConnection() {
      return nativeConnection;
   }

   @Override
   public void setSchema(String schema) throws SQLException {
      nativeConnection.setSchema(schema);
   }

   @Override
   public String getSchema() throws SQLException {
      return nativeConnection.getSchema();
   }

   @Override
   public void abort(Executor executor) throws SQLException {
      nativeConnection.abort(executor);
   }

   @Override
   public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
      nativeConnection.setNetworkTimeout(executor, milliseconds);
   }

   @Override
   public int getNetworkTimeout() throws SQLException {
      return nativeConnection.getNetworkTimeout();
   }

}
