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
package com.cibethelper.entities;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.sensor.jdbc.def.AbstractEntityDefinition;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class TPSEntityDefinition extends AbstractEntityDefinition {

   private Log log = LogFactory.getLog(TPSEntityDefinition.class);

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static final String SEL_TPSENTITY = "SELECT id, datevalue, timevalue, timestampvalue FROM TPSENTITY WHERE id = ?";

   private static TPSEntityDefinition instance;

   public static synchronized TPSEntityDefinition getInstance() {
      if (instance == null) {
         instance = new TPSEntityDefinition();
      }
      return instance;
   }

   public TPSEntityDefinition() {
      queries.put("INSERT INTO tpsentity (id, datevalue, timevalue, timestampvalue) " + "VALUES (?,?,?,?)",
            "INSERT INTO tpsentity (id, datevalue, timevalue, timestampvalue) " + "VALUES (?,?,?,?)");
      queries.put("SELECT id, datevalue, timevalue, timestampvalue from tpsentity WHERE datevalue = ?",
            "SELECT id, datevalue, timevalue, timestampvalue from tpsentity WHERE datevalue = ?");
      queries.put("SELECT id, datevalue, timevalue, timestampvalue from tpsentity WHERE timevalue = ?",
            "SELECT id, datevalue, timevalue, timestampvalue from tpsentity WHERE timevalue = ?");
      queries.put("SELECT id, datevalue, timevalue, timestampvalue from tpsentity WHERE timestampvalue = ?",
            "SELECT id, datevalue, timevalue, timestampvalue from tpsentity WHERE timestampvalue = ?");
      queries.put("INSERT INTO tpsentity (id, bytes) VALUES (?,?)", "INSERT INTO tpsentity (id, bytes) VALUES (?,?)");
   }

   @Override
   public <T> T find(Connection jdbcConnection, Class<T> clazz, Object primaryKey) {
      try {
         Long id = (Long) primaryKey;
         PreparedStatement stmt = jdbcConnection.prepareStatement(SEL_TPSENTITY);
         stmt.setLong(1, id);
         ResultSet rs = stmt.executeQuery();
         if (rs.next()) {
            TPSEntity te = new TPSEntity();
            te.setId(rs.getLong(1));
            te.setDate(rs.getDate(2));
            te.setTime(rs.getTime(3));
            te.setTimestamp(rs.getTimestamp(4));
            return (T) te;
         } else {
            return null;
         }
      } catch (SQLException e) {
         throw new CibetJdbcException(e.getMessage(), e);
      }
   }

   @Override
   public List<Object> createFromResultSet(ResultSet rs) throws SQLException {
      List<Object> list = new ArrayList<Object>();

      while (rs.next()) {
         TPSEntity te = new TPSEntity();
         list.add(te);
         te.setId(rs.getLong(1));
         te.setDate(rs.getDate(2));
         te.setTime(rs.getTime(3));
         te.setTimestamp(rs.getTimestamp(4));
      }
      return list;
   }

}
