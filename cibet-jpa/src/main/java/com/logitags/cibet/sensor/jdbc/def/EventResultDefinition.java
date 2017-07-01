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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

public class EventResultDefinition extends AbstractEntityDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = -3764677890263002406L;

   private static Log log = LogFactory.getLog(EventResultDefinition.class);

   public static final String SEL_BY_PRIMARYKEY = "EVENTRESULT.SEL_BY_PRIMARYKEY";
   private static final String SEL_CHILDREN_SQL = "EVENTRESULT.SEL_CHILDREN_SQL";

   private static final String DELETE_EVENTRESULT = "DELETE FROM cib_eventresult WHERE eventresultid=?";

   protected static final String EVENTRESULT = "eventresultid, parentresult_id, event, "
         + "resourc, sensor, setpoints, actuators, executionstatus, track_tenant, "
         + "track_user, executiontime, caseid";

   private static final String INSERT_EVENTRESULT = "INSERT INTO cib_eventresult (" + EVENTRESULT
         + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?)";

   private static final String SEL_CHILDREN = "SELECT " + EVENTRESULT + " FROM cib_eventresult WHERE parentresult_id=?";

   private static EventResultDefinition instance;

   public static synchronized EventResultDefinition getInstance() {
      if (instance == null) {
         instance = new EventResultDefinition();
      }
      return instance;
   }

   public EventResultDefinition() {
      queries.put(SEL_BY_PRIMARYKEY, "SELECT " + EVENTRESULT + " FROM cib_eventresult WHERE eventresultid = ?");
      queries.put(SEL_CHILDREN_SQL, SEL_CHILDREN);
   }

   @Override
   public <T> T find(Connection jdbcConnection, Class<T> clazz, Object primaryKey) {
      return (T) find(jdbcConnection, SEL_BY_PRIMARYKEY, primaryKey);
   }

   @Override
   public List<?> createFromResultSet(ResultSet rs) throws SQLException {
      if (rs == null) {
         throw new IllegalArgumentException("Failed to execute createFromResultSet: ResultSet is null");
      }
      List<EventResult> list = new ArrayList<EventResult>();

      while (rs.next()) {
         EventResult er = new EventResult();
         list.add(er);
         er.setEventResultId(rs.getString(1));
         // long parentId = rs.getLong(2);
         // don't load parent: endless loop!
         // if (!rs.wasNull()) {
         // EventResult parent = find(rs.getStatement().getConnection(),
         // EventResult.class, parentId);
         // er.setParentResult(parent);
         // }

         er.setEvent(ControlEvent.valueOf(rs.getString(3)));
         er.setResource(rs.getString(4));
         er.setSensor(rs.getString(5));
         er.setSetpoints(rs.getString(6));
         er.setActuators(rs.getString(7));
         er.setExecutionStatus(ExecutionStatus.valueOf(rs.getString(8)));
         er.setTenant(rs.getString(9));
         er.setUser(rs.getString(10));
         er.setExecutionTime(rs.getTimestamp(11));
         er.setCaseId(rs.getString(12));

         ResultSet childRS = null;
         try (PreparedStatement ps = rs.getStatement().getConnection().prepareStatement(SEL_CHILDREN)) {
            ps.setString(1, er.getEventResultId());
            childRS = ps.executeQuery();
            List<EventResult> children = (List<EventResult>) createFromResultSet(childRS);
            er.setChildResults(children);
         } finally {
            if (childRS != null)
               childRS.close();
         }
      }
      return list;
   }

   @Override
   public void persist(Connection jdbcConnection, Object obj) throws SQLException {
      EventResult er = (EventResult) obj;

      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_EVENTRESULT);
      try {
         persistEventResult(ps, er);
         for (EventResult child : er.getChildResults()) {
            persistEventResult(ps, child);
         }

      } finally {
         if (ps != null)
            ps.close();
      }
   }

   private void persistEventResult(PreparedStatement ps, EventResult er) throws SQLException {
      if (er.getParentResult() != null && er.getParentResult().getEventResultId() == null) {
         persistEventResult(ps, er.getParentResult());
      }

      er.setEventResultId(UUID.randomUUID().toString());

      ps.setString(1, er.getEventResultId());
      if (er.getParentResult() != null) {
         ps.setString(2, er.getParentResult().getEventResultId());
      } else {
         ps.setNull(2, Types.BIGINT);
      }
      ps.setString(3, er.getEvent().name());
      ps.setString(4, er.getResource());
      ps.setString(5, er.getSensor());
      ps.setString(6, er.getSetpoints());
      ps.setString(7, er.getActuators());
      ps.setString(8, er.getExecutionStatus().name());
      ps.setString(9, er.getTenant());
      ps.setString(10, er.getUser());
      ps.setTimestamp(11, new Timestamp(er.getExecutionTime().getTime()));
      ps.setString(12, er.getCaseId());
      ps.executeUpdate();
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.sensor.jdbc.def.AbstractEntityDefinition#remove(java .sql.Connection, java.lang.Object)
    */
   @Override
   public void remove(Connection jdbcConnection, Object obj) throws SQLException {
      PreparedStatement ps = null;
      try {
         ps = jdbcConnection.prepareStatement(DELETE_EVENTRESULT);
         removeEventResult(ps, (EventResult) obj);
      } finally {
         if (ps != null)
            ps.close();
      }
   }

   private void removeEventResult(PreparedStatement ps, EventResult er) throws SQLException {
      for (EventResult child : er.getChildResults()) {
         removeEventResult(ps, child);
      }
      ps.setString(1, er.getEventResultId());
      int count = ps.executeUpdate();
      log.debug(count + " EventResult deleted");
   }

}
