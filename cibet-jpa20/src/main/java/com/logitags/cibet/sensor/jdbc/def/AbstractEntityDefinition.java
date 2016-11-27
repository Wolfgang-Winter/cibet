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
package com.logitags.cibet.sensor.jdbc.def;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.sensor.jdbc.bridge.JdbcBridgeEntityManager;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

/**
 * implements default methods of EntityDefinition interface.
 * 
 */
public abstract class AbstractEntityDefinition implements Serializable, EntityDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = -846806363398287306L;

   private Log log = LogFactory.getLog(AbstractEntityDefinition.class);

   protected Map<String, String> queries = new HashMap<String, String>();

   @Override
   public Map<String, String> getQueries() {
      return queries;
   }

   /**
    * throws CibetJdbcException
    * 
    * @see com.logitags.cibet.sensor.jdbc.def.EntityDefinition#remove(java.sql.
    *      Connection, java.lang.Object)
    */
   @Override
   public void remove(Connection jdbcConnection, Object obj) throws SQLException {
      throw new CibetJdbcException("remove() method not implemented");
   }

   /**
    * find an entity with the given primary key using the given named query.
    * 
    * @param jdbcConnection
    * @param queryName
    * @param primaryKey
    * @return
    */
   protected Object find(Connection jdbcConnection, String queryName, Object primaryKey) {
      JdbcBridgeEntityManager em = new JdbcBridgeEntityManager(jdbcConnection);
      Query q = em.createNamedQuery(queryName);
      q.setParameter(1, primaryKey);
      return q.getSingleResult();
   }

   /**
    * sets a byte[] value into the PreparedStatement. If Database is Oracle and
    * size of the byte value is larger than 4K, applies special Oracle blob
    * handling by using OutputStream into Oracle BLOB.
    * 
    * @param ps
    * @param value
    * @param index
    * @throws SQLException
    */
   protected void setBlob(PreparedStatement ps, byte[] value, int index) throws SQLException {
      ps.setBytes(index, value);
      // if (JdbcBridgeEntityManager.isOracle() && value != null
      // && value.length > 4000) {
      // OracleBlobHandler.setBlobParam(ps, value, index);
      // } else {
      // ps.setBytes(index, value);
      // }
   }

   /**
    * retrieves a byte array from a database blob column. If Database is Oracle,
    * applies special Oracle blob handling by using InputStream.
    * 
    * @param rs
    * @param index
    * @return
    * @throws SQLException
    */
   protected byte[] getBlob(ResultSet rs, int index) throws SQLException {
      return rs.getBytes(index);
      // if (JdbcBridgeEntityManager.isOracle()) {
      // return OracleBlobHandler.getBlobValue(rs, index);
      // } else {
      // return rs.getBytes(index);
      // }
   }

   /**
    * throws CibetJdbcException.
    * 
    * @see com.logitags.cibet.sensor.jdbc.def.EntityDefinition#createFromResultSet
    *      (java.sql.ResultSet)
    */
   @Override
   public List<?> createFromResultSet(ResultSet rs) throws SQLException {
      throw new CibetJdbcException("createFromResultSet() method not implemented");
   }

   /**
    * throws CibetJdbcException.
    * 
    * @see com.logitags.cibet.sensor.jdbc.def.EntityDefinition#persist(java.sql.
    *      Connection, java.lang.Object)
    */
   @Override
   public void persist(Connection jdbcConnection, Object obj) throws SQLException {
      throw new CibetJdbcException("persist() method not implemented");
   }

   /**
    * throws CibetJdbcException.
    * 
    * @see com.logitags.cibet.sensor.jdbc.def.EntityDefinition#merge(java.sql.Connection
    *      , java.lang.Object)
    */
   @Override
   public <T> T merge(Connection jdbcConnection, T obj) throws SQLException {
      throw new CibetJdbcException("merge() method not implemented");
   }

}
