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

import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

/**
 * A definition that can be used for anonymous updates, inserts and deletes. The entity must not be known. Has only on
 * native query registered.
 * 
 */
public class PseudoEntityDefinition extends AbstractEntityDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = 1799845001138084311L;

   public PseudoEntityDefinition(String sqlQuery) {
      queries.put(sqlQuery, sqlQuery);
   }

   @Override
   public <T> T find(Connection jdbcConnection, Class<T> clazz, Object primaryKey) {
      throw new CibetJdbcException("find() method not implemented");
   }

}
