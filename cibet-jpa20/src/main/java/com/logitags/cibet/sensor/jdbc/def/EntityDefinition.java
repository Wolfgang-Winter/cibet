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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * defines persistence methods for one entity class for jdbc.
 */
public interface EntityDefinition {

   /**
    * returns a map of the names of all defined named queries of the entity, mapped to the native SQL statement. The
    * name could be the name of a named query (if it is a named query), the native SQL query itself (if it is a native
    * query) or a JPA query (if it is an on-the-fly query)
    * 
    * @return
    */
   Map<String, String> getQueries();

   /**
    * calls all setter method of the entity to fill the entity from the resultset.
    * 
    * @param rs
    * @throws SQLException
    */
   List<?> createFromResultSet(ResultSet rs) throws SQLException;

   /**
    * persist the entity. An implementation of this method must not commit or close the connection.
    * 
    * @param jdbcConnection
    * @param obj
    */
   void persist(Connection jdbcConnection, Object obj) throws SQLException;

   /**
    * update the entity. An implementation of this method must not commit or close the connection.
    * 
    * @param <T>
    * @param jdbcConnection
    * @param obj
    * @return the updated object
    * @throws SQLException
    */
   <T> T merge(Connection jdbcConnection, T obj) throws SQLException;

   /**
    * find object by primary key. An implementation of this method must not commit or close the connection.
    * 
    * @param <T>
    * @param jdbcConnection
    * @param clazz
    * @param primaryKey
    * @return
    */
   <T> T find(Connection jdbcConnection, Class<T> clazz, Object primaryKey);

   /**
    * removes the entity from the database. An implementation of this method must not commit or close the connection.
    * 
    * @param jdbcConnection
    * @param obj
    */
   void remove(Connection jdbcConnection, Object obj) throws SQLException;

}
