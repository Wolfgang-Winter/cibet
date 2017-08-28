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

import java.io.ByteArrayInputStream;
import java.io.CharArrayReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Actuator;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.control.Controller;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterSequenceComparator;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.ResourceParameter;

/**
 * Implementation of PreparedStatement interface that acts as a Cibet sensor for JDBC requests.
 * 
 */
public class CibetPreparedStatement extends CibetStatement implements PreparedStatement {

   private static transient Log log = LogFactory.getLog(CibetPreparedStatement.class);

   private static final String PARAMETER_NAME_PREFIX = "JDBC-";

   private PreparedStatement nativePS;

   private String sql;

   private Map<Integer, ResourceParameter> parameters = new TreeMap<Integer, ResourceParameter>();

   public CibetPreparedStatement(CibetConnection conn, PreparedStatement ps, String sql) {
      super(conn, ps);
      nativePS = ps;
      this.sql = sql;
   }

   @Override
   public ResultSet executeQuery() throws SQLException {
      return nativePS.executeQuery();
   }

   @Override
   public boolean execute() throws SQLException {
      if (sql == null) {
         throw new IllegalArgumentException("Failed execute PreparedStatement: SQL is null");
      }
      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;

      try {
         startManaging = Context.start();

         SqlParser parser = new SqlParser(cibetConnection, sql);
         ControlEvent originalEvent = parser.getControlEvent();
         Set<ResourceParameter> rpSet = new TreeSet<>(new ParameterSequenceComparator());
         rpSet.addAll(parameters.values());
         metadata = createJdbcEventMetadata(parser, originalEvent, getPrimaryKey(parser), rpSet);

         metadata.getResource().addParameter("StatementType", StatementType.PREPAREDSTATEMENT_EXECUTE,
               ParameterType.JDBC_STATEMENT_TYPE);

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
                  result = nativePS.execute();
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

   @Override
   public int executeUpdate() throws SQLException {
      if (sql == null) {
         throw new IllegalArgumentException("Failed execute PreparedStatement: SQL is null");
      }
      boolean startManaging = true;
      EventMetadata metadata = null;
      EventResult thisResult = null;

      try {
         startManaging = Context.start();

         SqlParser parser = new SqlParser(cibetConnection, sql);
         ControlEvent originalEvent = parser.getControlEvent();

         Set<ResourceParameter> rpSet = new TreeSet<>(new ParameterSequenceComparator());
         rpSet.addAll(parameters.values());
         metadata = createJdbcEventMetadata(parser, originalEvent, getPrimaryKey(parser), rpSet);

         metadata.getResource().addParameter("StatementType", StatementType.PREPAREDSTATEMENT_EXECUTEUPDATE,
               ParameterType.JDBC_STATEMENT_TYPE);

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
                  count = nativePS.executeUpdate();
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

   private SqlParameter getPrimaryKey(SqlParser parser) {
      SqlParameter pk = new SqlParameter(parser.getPrimaryKey());
      if (pk.getSequence() > 0) {
         pk.setValue(parameters.get(pk.getSequence()).getUnencodedValue());
      }
      if (log.isDebugEnabled()) {
         log.debug("PrimaryKey: " + pk.getColumn() + " = " + pk.getValue());
      }
      return pk;
   }

   @Override
   public void clearParameters() throws SQLException {
      parameters.clear();
      nativePS.clearParameters();
   }

   @Override
   public void setNull(int parameterIndex, int sqlType) throws SQLException {
      nativePS.setNull(parameterIndex, sqlType);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(sqlType),
            null, ParameterType.JDBC_PARAMETER_NULL, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setBoolean(int parameterIndex, boolean x) throws SQLException {
      nativePS.setBoolean(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, Boolean.class.getName(),
            x, ParameterType.JDBC_PARAMETER_BOOLEAN, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setByte(int parameterIndex, byte x) throws SQLException {
      nativePS.setByte(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, byte.class.getName(), x,
            ParameterType.JDBC_PARAMETER_BYTE, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setShort(int parameterIndex, short x) throws SQLException {
      nativePS.setShort(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, short.class.getName(), x,
            ParameterType.JDBC_PARAMETER_SHORT, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setInt(int parameterIndex, int x) throws SQLException {
      nativePS.setInt(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, int.class.getName(), x,
            ParameterType.JDBC_PARAMETER_INT, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setLong(int parameterIndex, long x) throws SQLException {
      nativePS.setLong(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, long.class.getName(), x,
            ParameterType.JDBC_PARAMETER_LONG, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setFloat(int parameterIndex, float x) throws SQLException {
      nativePS.setFloat(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, float.class.getName(), x,
            ParameterType.JDBC_PARAMETER_FLOAT, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setDouble(int parameterIndex, double x) throws SQLException {
      nativePS.setDouble(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, double.class.getName(), x,
            ParameterType.JDBC_PARAMETER_DOUBLE, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setBigDecimal(int parameterIndex, BigDecimal x) throws SQLException {
      nativePS.setBigDecimal(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex,
            BigDecimal.class.getName(), x, ParameterType.JDBC_PARAMETER_BIGDECIMAL, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setString(int parameterIndex, String x) throws SQLException {
      nativePS.setString(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.class.getName(), x,
            ParameterType.JDBC_PARAMETER_STRING, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setBytes(int parameterIndex, byte[] x) throws SQLException {
      nativePS.setBytes(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, byte[].class.getName(), x,
            ParameterType.JDBC_PARAMETER_BYTES, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setDate(int parameterIndex, Date x) throws SQLException {
      nativePS.setDate(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, Date.class.getName(), x,
            ParameterType.JDBC_PARAMETER_DATE, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setTime(int parameterIndex, Time x) throws SQLException {
      nativePS.setTime(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, Time.class.getName(), x,
            ParameterType.JDBC_PARAMETER_TIME, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setTimestamp(int parameterIndex, Timestamp x) throws SQLException {
      nativePS.setTimestamp(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, Timestamp.class.getName(),
            x, ParameterType.JDBC_PARAMETER_TIMESTAMP, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setAsciiStream(int parameterIndex, InputStream x, int length) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x, length);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setAsciiStream(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               bytes, ParameterType.JDBC_PARAMETER_ASCIISTREAM_INT, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setUnicodeStream(int parameterIndex, InputStream x, int length) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x, length);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setUnicodeStream(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               bytes, ParameterType.JDBC_PARAMETER_UNICODESTREAM, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setBinaryStream(int parameterIndex, InputStream x, int length) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x, length);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setBinaryStream(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               bytes, ParameterType.JDBC_PARAMETER_BINARYSTREAM_INT, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
      nativePS.setObject(parameterIndex, x, targetSqlType);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex,
            String.valueOf(targetSqlType), x, ParameterType.JDBC_PARAMETER_OBJECT_TARGETSQLTYPE, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setObject(int parameterIndex, Object x) throws SQLException {
      nativePS.setObject(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, Object.class.getName(), x,
            ParameterType.JDBC_PARAMETER_OBJECT, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void addBatch() throws SQLException {
      nativePS.addBatch();
   }

   @Override
   public void setCharacterStream(int parameterIndex, Reader reader, int length) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setCharacterStream(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               chars, ParameterType.JDBC_PARAMETER_CHARACTERSTREAM_INT, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setRef(int parameterIndex, Ref x) throws SQLException {
      throw new SQLFeatureNotSupportedException("PreparedStatement.setRef() not supported by this driver");
   }

   @Override
   public void setBlob(int parameterIndex, Blob x) throws SQLException {
      try {
         Blob copy = null;
         byte[] bytes = null;
         if (x != null) {
            bytes = IOUtils.toByteArray(x.getBinaryStream());
            copy = this.getConnection().createBlob();
            OutputStream out = copy.setBinaryStream(1L);
            out.write(bytes);
            out.flush();
            out.close();
         }
         nativePS.setBlob(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "java.sql.Bob", bytes,
               ParameterType.JDBC_PARAMETER_BLOB, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setClob(int parameterIndex, Clob x) throws SQLException {
      try {
         Clob copy = null;
         char[] chars = null;
         if (x != null) {
            chars = IOUtils.toCharArray(x.getCharacterStream());
            copy = this.getConnection().createClob();
            Writer out = copy.setCharacterStream(1L);
            out.write(chars);
            out.flush();
            out.close();
         }
         nativePS.setClob(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "java.sql.Clob", chars,
               ParameterType.JDBC_PARAMETER_CLOB, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setArray(int parameterIndex, Array x) throws SQLException {
      throw new SQLFeatureNotSupportedException("PreparedStatement.setArray() not supported by this driver");
   }

   @Override
   public ResultSetMetaData getMetaData() throws SQLException {
      return nativePS.getMetaData();
   }

   @Override
   public void setDate(int parameterIndex, Date x, Calendar cal) throws SQLException {
      nativePS.setDate(parameterIndex, x, cal);
      String strDate = x == null ? "-" : String.valueOf(x.getTime());
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, strDate, cal,
            ParameterType.JDBC_PARAMETER_DATE_CAL, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setTime(int parameterIndex, Time x, Calendar cal) throws SQLException {
      nativePS.setTime(parameterIndex, x, cal);
      String strDate = x == null ? "-" : String.valueOf(x.getTime());
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, strDate, cal,
            ParameterType.JDBC_PARAMETER_TIME_CAL, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setTimestamp(int parameterIndex, Timestamp x, Calendar cal) throws SQLException {
      nativePS.setTimestamp(parameterIndex, x, cal);
      String strDate = x == null ? "-" : String.valueOf(x.getTime());
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, strDate, cal,
            ParameterType.JDBC_PARAMETER_TIMESTAMP_CAL, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setNull(int parameterIndex, int sqlType, String typeName) throws SQLException {
      nativePS.setNull(parameterIndex, sqlType, typeName);
      ResourceParameter param = new ResourceParameter(typeName, String.valueOf(sqlType), null,
            ParameterType.JDBC_PARAMETER_NULL_TYPENAME, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setURL(int parameterIndex, URL x) throws SQLException {
      nativePS.setURL(parameterIndex, x);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, URL.class.getName(), x,
            ParameterType.JDBC_PARAMETER_URL, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public ParameterMetaData getParameterMetaData() throws SQLException {
      return nativePS.getParameterMetaData();
   }

   @Override
   public void setRowId(int parameterIndex, RowId x) throws SQLException {
      throw new SQLFeatureNotSupportedException("PreparedStatement.setRowId() not supported by this driver");
   }

   @Override
   public void setNString(int parameterIndex, String value) throws SQLException {
      nativePS.setNString(parameterIndex, value);
      ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.class.getName(),
            value, ParameterType.JDBC_PARAMETER_NSTRING, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setNCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setNCharacterStream(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               chars, ParameterType.JDBC_PARAMETER_NCHARACTERSTREAM_LONG, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setNClob(int parameterIndex, NClob value) throws SQLException {
      try {
         NClob copy = null;
         char[] chars = null;
         if (value != null) {
            chars = IOUtils.toCharArray(value.getCharacterStream());
            copy = this.getConnection().createNClob();
            Writer out = copy.setCharacterStream(1L);
            out.write(chars);
            out.flush();
            out.close();
         }
         nativePS.setNClob(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "java.sql.NClob",
               chars, ParameterType.JDBC_PARAMETER_NCLOB, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setClob(int parameterIndex, Reader reader, long length) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setClob(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               chars, ParameterType.JDBC_PARAMETER_CLOB_READER_LONG, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setBlob(int parameterIndex, InputStream x, long length) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x, length);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setBlob(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               bytes, ParameterType.JDBC_PARAMETER_BLOB_INPUTSTREAM_LONG, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setNClob(int parameterIndex, Reader reader, long length) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setNClob(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               chars, ParameterType.JDBC_PARAMETER_NCLOB_READER_LONG, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setSQLXML(int parameterIndex, SQLXML xmlObject) throws SQLException {
      throw new SQLFeatureNotSupportedException("PreparedStatement.setSQLXML() not supported by this driver");
   }

   @Override
   public void setObject(int parameterIndex, Object x, int targetSqlType, int scaleOrLength) throws SQLException {
      nativePS.setObject(parameterIndex, x, targetSqlType, scaleOrLength);
      ResourceParameter param = new ResourceParameter(String.valueOf(scaleOrLength), String.valueOf(targetSqlType), x,
            ParameterType.JDBC_PARAMETER_OBJECT_TARGETSQLTYPE_SCALE, parameterIndex);
      parameters.put(parameterIndex, param);
   }

   @Override
   public void setAsciiStream(int parameterIndex, InputStream x, long length) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x, length);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setAsciiStream(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               bytes, ParameterType.JDBC_PARAMETER_ASCIISTREAM_LONG, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setBinaryStream(int parameterIndex, InputStream x, long length) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x, length);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setBinaryStream(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               bytes, ParameterType.JDBC_PARAMETER_BINARYSTREAM_LONG, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setCharacterStream(int parameterIndex, Reader reader, long length) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setCharacterStream(parameterIndex, copy, length);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, String.valueOf(length),
               chars, ParameterType.JDBC_PARAMETER_CHARACTERSTREAM_LONG, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setAsciiStream(int parameterIndex, InputStream x) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setAsciiStream(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "-", bytes,
               ParameterType.JDBC_PARAMETER_ASCIISTREAM, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setBinaryStream(int parameterIndex, InputStream x) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setBinaryStream(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "-", bytes,
               ParameterType.JDBC_PARAMETER_BINARYSTREAM, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setCharacterStream(int parameterIndex, Reader reader) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setCharacterStream(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "-", chars,
               ParameterType.JDBC_PARAMETER_CHARACTERSTREAM, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setNCharacterStream(int parameterIndex, Reader reader) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setNCharacterStream(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "-", chars,
               ParameterType.JDBC_PARAMETER_NCHARACTERSTREAM, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setClob(int parameterIndex, Reader reader) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setClob(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "-", chars,
               ParameterType.JDBC_PARAMETER_CLOB_READER, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setBlob(int parameterIndex, InputStream x) throws SQLException {
      try {
         byte[] bytes = x == null ? null : IOUtils.toByteArray(x);
         InputStream copy = bytes == null ? null : new ByteArrayInputStream(bytes);
         nativePS.setBlob(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "-", bytes,
               ParameterType.JDBC_PARAMETER_BLOB_INPUTSTREAM, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public void setNClob(int parameterIndex, Reader reader) throws SQLException {
      try {
         char[] chars = reader == null ? null : IOUtils.toCharArray(reader);
         Reader copy = chars == null ? null : new CharArrayReader(chars);
         nativePS.setNClob(parameterIndex, copy);
         ResourceParameter param = new ResourceParameter(PARAMETER_NAME_PREFIX + parameterIndex, "-", chars,
               ParameterType.JDBC_PARAMETER_NCLOB_READER, parameterIndex);
         parameters.put(parameterIndex, param);
      } catch (IOException e) {
         log.error(e.getMessage(), e);
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

}
