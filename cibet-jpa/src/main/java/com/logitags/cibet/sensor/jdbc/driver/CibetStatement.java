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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.control.Controller;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;

/**
 * Implementation of Statement interface that acts as a Cibet sensor for JDBC requests.
 */
public class CibetStatement implements Statement {

   private Log log = LogFactory.getLog(CibetStatement.class);

   protected static final String SENSOR_NAME = "JDBC";

   protected Statement nativeS;

   protected CibetConnection cibetConnection;

   public CibetStatement(CibetConnection conn, Statement st) {
      cibetConnection = conn;
      nativeS = st;
   }

   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException {
      return nativeS.unwrap(iface);
   }

   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return nativeS.isWrapperFor(iface);
   }

   @Override
   public ResultSet executeQuery(String sql) throws SQLException {
      return nativeS.executeQuery(sql);
   }

   @Override
   public int executeUpdate(String sql) throws SQLException {
      return internalExecuteUpdate(sql, null);
   }

   @Override
   public void close() throws SQLException {
      nativeS.close();
   }

   @Override
   public int getMaxFieldSize() throws SQLException {
      return nativeS.getMaxFieldSize();
   }

   @Override
   public void setMaxFieldSize(int max) throws SQLException {
      nativeS.setMaxFieldSize(max);
   }

   @Override
   public int getMaxRows() throws SQLException {
      return nativeS.getMaxRows();
   }

   @Override
   public void setMaxRows(int max) throws SQLException {
      nativeS.setMaxRows(max);
   }

   @Override
   public void setEscapeProcessing(boolean enable) throws SQLException {
      nativeS.setEscapeProcessing(enable);
   }

   @Override
   public int getQueryTimeout() throws SQLException {
      return nativeS.getQueryTimeout();
   }

   @Override
   public void setQueryTimeout(int seconds) throws SQLException {
      nativeS.setQueryTimeout(seconds);
   }

   @Override
   public void cancel() throws SQLException {
      nativeS.cancel();
   }

   @Override
   public SQLWarning getWarnings() throws SQLException {
      return nativeS.getWarnings();
   }

   @Override
   public void clearWarnings() throws SQLException {
      nativeS.clearWarnings();
   }

   @Override
   public void setCursorName(String name) throws SQLException {
      nativeS.setCursorName(name);
   }

   @Override
   public boolean execute(String sql) throws SQLException {
      return internalExecute(sql, null);
   }

   @Override
   public ResultSet getResultSet() throws SQLException {
      return nativeS.getResultSet();
   }

   @Override
   public int getUpdateCount() throws SQLException {
      return nativeS.getUpdateCount();
   }

   @Override
   public boolean getMoreResults() throws SQLException {
      return nativeS.getMoreResults();
   }

   @Override
   public void setFetchDirection(int direction) throws SQLException {
      nativeS.setFetchDirection(direction);
   }

   @Override
   public int getFetchDirection() throws SQLException {
      return nativeS.getFetchDirection();
   }

   @Override
   public void setFetchSize(int rows) throws SQLException {
      nativeS.setFetchSize(rows);
   }

   @Override
   public int getFetchSize() throws SQLException {
      return nativeS.getFetchSize();
   }

   @Override
   public int getResultSetConcurrency() throws SQLException {
      return nativeS.getResultSetConcurrency();
   }

   @Override
   public int getResultSetType() throws SQLException {
      return nativeS.getResultSetType();
   }

   @Override
   public void addBatch(String sql) throws SQLException {
      nativeS.addBatch(sql);
   }

   @Override
   public void clearBatch() throws SQLException {
      nativeS.clearBatch();
   }

   @Override
   public int[] executeBatch() throws SQLException {
      return nativeS.executeBatch();
   }

   @Override
   public Connection getConnection() throws SQLException {
      return cibetConnection;
   }

   @Override
   public boolean getMoreResults(int current) throws SQLException {
      return nativeS.getMoreResults(current);
   }

   @Override
   public ResultSet getGeneratedKeys() throws SQLException {
      return nativeS.getGeneratedKeys();
   }

   @Override
   public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
      return internalExecuteUpdate(sql, Integer.valueOf(autoGeneratedKeys));
   }

   @Override
   public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
      return internalExecuteUpdate(sql, columnIndexes);
   }

   @Override
   public int executeUpdate(String sql, String[] columnNames) throws SQLException {
      return internalExecuteUpdate(sql, columnNames);
   }

   @Override
   public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
      return internalExecute(sql, Integer.valueOf(autoGeneratedKeys));
   }

   @Override
   public boolean execute(String sql, int[] columnIndexes) throws SQLException {
      return internalExecute(sql, columnIndexes);
   }

   @Override
   public boolean execute(String sql, String[] columnNames) throws SQLException {
      return internalExecute(sql, columnNames);
   }

   @Override
   public int getResultSetHoldability() throws SQLException {
      return nativeS.getResultSetHoldability();
   }

   @Override
   public boolean isClosed() throws SQLException {
      return nativeS.isClosed();
   }

   @Override
   public void setPoolable(boolean poolable) throws SQLException {
      nativeS.setPoolable(poolable);
   }

   @Override
   public boolean isPoolable() throws SQLException {
      return nativeS.isPoolable();
   }

   protected EventMetadata createJdbcEventMetadata(SqlParser parser, ControlEvent originalEvent, SqlParameter pk,
         Set<ResourceParameter> parameters) {
      ControlEvent event = (ControlEvent) Context.internalRequestScope().getProperty(InternalRequestScope.CONTROLEVENT);
      if (event != null) {
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLEVENT);
      } else {
         event = originalEvent;
      }

      Resource res = new JdbcResource(parser.getSql(), parser.getTarget(), pk, parameters);
      EventMetadata md = new EventMetadata(SENSOR_NAME, event, res);

      return md;
   }

   @Override
   public void closeOnCompletion() throws SQLException {
      nativeS.closeOnCompletion();
   }

   @Override
   public boolean isCloseOnCompletion() throws SQLException {
      return nativeS.isCloseOnCompletion();
   }

   protected void finish(boolean startManaging, EventMetadata metadata, EventResult thisResult) throws SQLException {
      try {
         if (metadata != null) {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.afterEvent(metadata);
            }

            metadata.evaluateEventExecuteStatus();
         }

      } catch (SQLException | RuntimeException e) {
         throw e;
      } catch (Throwable e) {
         throw new SQLException(e);
      } finally {
         if (thisResult != null && metadata != null) {
            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               thisResult.setExecutionStatus(ExecutionStatus.EXECUTED);
            } else {
               thisResult.setExecutionStatus(metadata.getExecutionStatus());
            }
         }

         if (startManaging) {
            Context.end();
         }
      }
   }

   private boolean internalExecute(String sql, Object addValue) throws SQLException {
      if (sql == null) {
         throw new IllegalArgumentException("Failed execute Statement: SQL is null");
      }

      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;

      try {
         startManaging = Context.start();

         SqlParser parser = new SqlParser(cibetConnection, sql);
         ControlEvent originalEvent = parser.getControlEvent();
         metadata = createJdbcEventMetadata(parser, originalEvent, parser.getPrimaryKey(), null);

         metadata.getResource().addParameter("StatementType", StatementType.STATEMENT_EXECUTE,
               ParameterType.JDBC_STATEMENT_TYPE);
         if (addValue != null) {
            metadata.getResource().addParameter("StatementAddedValue", addValue,
                  ParameterType.JDBC_STATEMENT_ADDITIONAL_VALUE);
         }

         Controller.evaluate(metadata);
         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         try {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.beforeEvent(metadata);
            }

            boolean result = false;
            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);

               if (!Context.requestScope().isPlaying()) {
                  if (addValue == null) {
                     result = nativeS.execute(sql);
                  } else if (addValue instanceof Integer) {
                     result = nativeS.execute(sql, (int) addValue);
                  } else if (addValue instanceof int[]) {
                     result = nativeS.execute(sql, (int[]) addValue);
                  } else if (addValue instanceof String[]) {
                     result = nativeS.execute(sql, (String[]) addValue);
                  }

                  if (log.isDebugEnabled()) {
                     log.debug(sql + " |result: " + result);
                  }
               }
            }

            metadata.getResource().setResultObject(result);

         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            metadata.setExecutionStatus(ExecutionStatus.ERROR);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);
         }

      } finally {
         finish(startManaging, metadata, thisResult);
      }

      return (boolean) metadata.getResource().getResultObject();
   }

   private int internalExecuteUpdate(String sql, Object addValue) throws SQLException {
      if (sql == null) {
         throw new IllegalArgumentException("Failed execute Statement: SQL is null");
      }
      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;

      try {
         startManaging = Context.start();

         SqlParser parser = new SqlParser(cibetConnection, sql);
         ControlEvent originalEvent = parser.getControlEvent();
         metadata = createJdbcEventMetadata(parser, originalEvent, parser.getPrimaryKey(), null);

         metadata.getResource().addParameter("StatementType", StatementType.STATEMENT_EXECUTEUPDATE,
               ParameterType.JDBC_STATEMENT_TYPE);
         if (addValue != null) {
            metadata.getResource().addParameter("StatementAddedValue", addValue,
                  ParameterType.JDBC_STATEMENT_ADDITIONAL_VALUE);
         }

         Controller.evaluate(metadata);
         thisResult = Context.internalRequestScope().registerEventResult(new EventResult(SENSOR_NAME, metadata));

         try {
            for (Actuator actuator : metadata.getActuators()) {
               actuator.beforeEvent(metadata);
            }

            int count = 0;
            if (metadata.getExecutionStatus() == ExecutionStatus.EXECUTING) {
               metadata.setExecutionStatus(ExecutionStatus.EXECUTED);

               if (!Context.requestScope().isPlaying()) {
                  if (addValue == null) {
                     count = nativeS.executeUpdate(sql);
                  } else if (addValue instanceof Integer) {
                     count = nativeS.executeUpdate(sql, (int) addValue);
                  } else if (addValue instanceof int[]) {
                     count = nativeS.executeUpdate(sql, (int[]) addValue);
                  } else if (addValue instanceof String[]) {
                     count = nativeS.executeUpdate(sql, (String[]) addValue);
                  }

                  if (log.isDebugEnabled()) {
                     log.debug(sql + " |result: " + count);
                  }
               }
            }

            metadata.getResource().setResultObject(count);

         } catch (Throwable e) {
            log.error(e.getMessage(), e);
            metadata.setExecutionStatus(ExecutionStatus.ERROR);
            Context.requestScope().setRemark(e.getMessage());
            metadata.setException(e);
         }

      } finally {
         finish(startManaging, metadata, thisResult);
      }

      return (int) metadata.getResource().getResultObject();
   }

}
