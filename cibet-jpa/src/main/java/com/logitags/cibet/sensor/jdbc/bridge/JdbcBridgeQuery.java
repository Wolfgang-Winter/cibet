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
import java.sql.Types;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.FlushModeType;
import javax.persistence.LockModeType;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Parameter;
import javax.persistence.Query;
import javax.persistence.TemporalType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.sensor.jdbc.def.EntityDefinition;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class JdbcBridgeQuery implements Query {

   private Log log = LogFactory.getLog(JdbcBridgeQuery.class);

   private String queryName;

   private List<Object> parameters = new LinkedList<Object>();

   protected Object additionalQueryValue;

   private JdbcBridgeEntityManager entityManager;

   /**
    * Constructs a JDBCQuery object.
    * 
    * @param em
    *           database connection to use
    * @param queryName
    *           a named query name or a native query, depending on isNative
    */
   public JdbcBridgeQuery(JdbcBridgeEntityManager em, String queryName) {
      this.entityManager = em;
      this.queryName = queryName;
   }

   @Override
   public List getResultList() {
      ResultSet rs = null;
      PreparedStatement pstmt = null;
      Connection conn = null;
      try {
         Map<String, EntityDefinition> map = JdbcBridgeEntityManager.getQueryDefinitions();
         log.debug(queryName);

         EntityDefinition entityDef = JdbcBridgeEntityManager.getQueryDefinitions().get(queryName);
         if (entityDef == null) {
            String msg = "no EntityDefinition registered for name '" + queryName + "'";
            log.error(msg);
            throw new CibetJdbcException(msg);
         }

         final String sql = entityDef.getQueries().get(queryName);
         if (sql == null) {
            String msg = "no SQL query registered for name '" + queryName + "' in " + entityDef;
            log.error(msg);
            throw new CibetJdbcException(msg);
         }

         conn = entityManager.getNativeConnection();
         pstmt = conn.prepareStatement(sql);
         int count = 0;
         for (Object param : parameters) {
            count++;
            if (param == null) {
               pstmt.setNull(count, Types.VARCHAR);

            } else if (param instanceof java.sql.Date) {
               pstmt.setDate(count, (java.sql.Date) param);

            } else if (param instanceof java.sql.Time) {
               pstmt.setTime(count, (java.sql.Time) param);

            } else if (param instanceof Date) {
               Timestamp ts = new Timestamp(((Date) param).getTime());
               pstmt.setTimestamp(count, ts);
            } else if (param.getClass().isEnum()) {
               Enum<?> ee = (Enum<?>) param;
               pstmt.setString(count, ee.name());

            } else {
               pstmt.setObject(count, param);
            }
         }
         rs = pstmt.executeQuery();
         List<?> list = entityDef.createFromResultSet(rs);
         log.debug(list.size() + " objects loaded from database");
         return list;

      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      } finally {
         try {
            if (rs != null) rs.close();
            if (pstmt != null) pstmt.close();
            entityManager.finalizeConnection(conn);
         } catch (SQLException e) {
            log.error(e.getMessage(), e);
         }
      }
   }

   @Override
   public Object getSingleResult() {
      List<Object> list = null;
      list = getResultList();
      if (list.size() == 1) {
         return list.get(0);
      } else if (list.size() == 0) {
         throw new NoResultException();
      } else {
         throw new NonUniqueResultException();
      }
   }

   @Override
   public int executeUpdate() {
      PreparedStatement stmt = null;
      Connection conn = null;
      try {
         EntityDefinition entityDef = JdbcBridgeEntityManager.getQueryDefinitions().get(queryName);
         if (entityDef == null) {
            String msg = "no EntityDefinition registered for name '" + queryName + "'";
            log.error(msg);
            throw new CibetJdbcException(msg);
         }

         final String sql = entityDef.getQueries().get(queryName);
         if (sql == null) {
            String msg = "no SQL query registered for name '" + queryName + "' in " + entityDef;
            log.error(msg);
            throw new CibetJdbcException(msg);
         } else {
            log.debug(sql);
         }

         int count = 0;

         conn = entityManager.getNativeConnection();
         stmt = conn.prepareStatement(sql);
         int pcount = 0;
         for (Object param : parameters) {
            pcount++;
            if (param == null) {
               stmt.setNull(pcount, Types.VARCHAR);
            } else if (param instanceof Date) {
               Timestamp ts = new Timestamp(((Date) param).getTime());
               stmt.setTimestamp(pcount, ts);
            } else if (param instanceof byte[]) {
               stmt.setBytes(pcount, (byte[]) param);
               // if (JdbcBridgeEntityManager.isOracle()
               // && ((byte[]) param).length > 4000) {
               // OracleBlobHandler.setBlobParam(stmt, (byte[]) param, pcount);
               // } else {
               // stmt.setBytes(pcount, (byte[]) param);
               // }
            } else if (param.getClass().isEnum()) {
               Enum<?> ee = (Enum<?>) param;
               stmt.setString(pcount, ee.name());
            } else {
               stmt.setObject(pcount, param);
            }
         }
         count = stmt.executeUpdate();
         log.debug(count + " records affected");
         return count;
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      } finally {
         try {
            if (stmt != null) stmt.close();
            entityManager.finalizeConnection(conn);
         } catch (SQLException e) {
            throw new CibetJdbcException(e.getMessage(), e);
         }
      }
   }

   @Override
   public Query setMaxResults(int i) {
      log.warn("call to JdbcQuery.setMaxResults ignored");
      return this;
   }

   @Override
   public Query setFirstResult(int i) {
      log.warn("call to JdbcQuery.setFirstResult ignored");
      return this;
   }

   @Override
   public Query setHint(String s, Object obj) {
      log.warn("call to JdbcQuery.setHint ignored");
      return this;
   }

   @Override
   public Query setParameter(String s, Object obj) {
      parameters.add(obj);
      return this;
   }

   @Override
   public Query setParameter(String s, Date date, TemporalType temporaltype) {
      parameters.add(date);
      return this;
   }

   @Override
   public Query setParameter(String s, Calendar calendar, TemporalType temporaltype) {
      parameters.add(calendar);
      return this;
   }

   @Override
   public Query setParameter(int i, Object obj) {
      parameters.add(obj);
      return this;
   }

   @Override
   public Query setParameter(int i, Date date, TemporalType temporaltype) {
      parameters.add(date);
      return this;
   }

   @Override
   public Query setParameter(int i, Calendar calendar, TemporalType temporaltype) {
      parameters.add(calendar);
      return this;
   }

   @Override
   public Query setFlushMode(FlushModeType flushmodetype) {
      log.warn("call to JdbcQuery.setFlushMode ignored");
      return this;
   }

   @Override
   public int getFirstResult() {
      log.warn("method not implemented");
      return 0;
   }

   @Override
   public FlushModeType getFlushMode() {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public Map<String, Object> getHints() {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public LockModeType getLockMode() {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public int getMaxResults() {
      log.warn("method not implemented");
      return 0;
   }

   @Override
   public Parameter<?> getParameter(String arg0) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public Parameter<?> getParameter(int arg0) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public <T> Parameter<T> getParameter(String arg0, Class<T> arg1) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public <T> Parameter<T> getParameter(int arg0, Class<T> arg1) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public <T> T getParameterValue(Parameter<T> arg0) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public Object getParameterValue(String arg0) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public Object getParameterValue(int arg0) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public Set<Parameter<?>> getParameters() {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public boolean isBound(Parameter<?> arg0) {
      log.warn("method not implemented");
      return false;
   }

   @Override
   public Query setLockMode(LockModeType arg0) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public <T> Query setParameter(Parameter<T> arg0, T arg1) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public Query setParameter(Parameter<Calendar> arg0, Calendar arg1, TemporalType arg2) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public Query setParameter(Parameter<Date> arg0, Date arg1, TemporalType arg2) {
      log.warn("method not implemented");
      return null;
   }

   @Override
   public <T> T unwrap(Class<T> arg0) {
      log.warn("method not implemented");
      return null;
   }

}
