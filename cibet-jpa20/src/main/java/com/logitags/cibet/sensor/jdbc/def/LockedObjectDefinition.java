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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.lock.LockState;
import com.logitags.cibet.actuator.lock.LockedObject;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class LockedObjectDefinition extends AbstractEntityDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = -3764677890263002406L;

   private static Log log = LogFactory.getLog(LockedObjectDefinition.class);

   public static final String SEL_BY_PRIMARYKEY = "LOCKEDOBJECT.SEL_BY_PRIMARYKEY";

   protected static final String LOCKEDOBJECT = "lockedobjectid, lockdate, lockremark, "
         + "lockedby, lockstate, objectid, object, targettype, lockedevent, method, tenant,"
         + "unlockdate, unlockremark, unlockedby, version";

   private static final String INSERT_LOCKEDOBJECT = "INSERT INTO cib_lockedobject (" + LOCKEDOBJECT
         + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,1)";

   private static final String UPDATE_LOCKEDOBJECT = "UPDATE cib_lockedobject SET lockdate=?, lockremark=?, "
         + "lockedby=?, lockstate=?, objectid=?, object=?, targettype=?, lockedevent=?, "
         + "method=?, tenant=?, unlockdate=?, unlockremark=?, unlockedby=?, "
         + "version=version+1 WHERE lockedobjectid=? AND version = ?";

   private static final String DELETE_LOCKEDOBJECT = "DELETE FROM cib_lockedobject WHERE lockedobjectid=?";

   private static LockedObjectDefinition instance;

   public static synchronized LockedObjectDefinition getInstance() {
      if (instance == null) {
         instance = new LockedObjectDefinition();
      }
      return instance;
   }

   public LockedObjectDefinition() {
      queries.put(SEL_BY_PRIMARYKEY, "SELECT " + LOCKEDOBJECT + " FROM cib_lockedobject WHERE lockedobjectid = ?");
      queries.put(LockedObject.SEL_ALL, "SELECT " + LOCKEDOBJECT + " FROM cib_lockedobject WHERE tenant = ?");
      queries.put(LockedObject.SEL_LOCKED_ALL,
            "SELECT " + LOCKEDOBJECT + " FROM cib_lockedobject WHERE tenant = ? AND " + "lockstate = 'LOCKED'");
      queries.put(LockedObject.SEL_LOCKED_BY_USER, "SELECT " + LOCKEDOBJECT + " FROM cib_lockedobject WHERE tenant = ? "
            + "AND lockedby = ? AND lockstate = 'LOCKED'");
      queries.put(LockedObject.SEL_LOCKED_BY_TARGETTYPE, "SELECT " + LOCKEDOBJECT
            + " FROM cib_lockedobject WHERE tenant = ? AND " + "targettype = ? AND lockstate = 'LOCKED'");
      queries.put(LockedObject.SEL_LOCKED_BY_TARGETTYPE_METHOD,
            "SELECT " + LOCKEDOBJECT + " FROM cib_lockedobject WHERE tenant = ? AND "
                  + "targetType = ? AND method = ? AND lockstate = 'LOCKED'");

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
      List<LockedObject> list = new ArrayList<LockedObject>();

      while (rs.next()) {
         LockedObject lo = new LockedObject();
         list.add(lo);
         lo.setLockedObjectId(rs.getString(1));
         lo.setLockDate(rs.getTimestamp(2));
         lo.setLockRemark(rs.getString(3));
         lo.setLockedBy(rs.getString(4));
         lo.setLockState(LockState.valueOf(rs.getString(5)));
         lo.setObjectId(rs.getString(6));
         lo.setObject(getBlob(rs, 7));
         lo.setTargetType(rs.getString(8));
         lo.setLockedEvent(ControlEvent.valueOf(rs.getString(9)));
         lo.setMethod(rs.getString(10));
         lo.setTenant(rs.getString(11));
         lo.setUnlockDate(rs.getTimestamp(12));
         lo.setUnlockRemark(rs.getString(13));
         lo.setUnlockedBy(rs.getString(14));
         lo.setVersion(rs.getInt(15));
      }
      return list;
   }

   @Override
   public void persist(Connection jdbcConnection, Object obj) throws SQLException {
      LockedObject lo = (LockedObject) obj;
      lo.prePersist();
      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_LOCKEDOBJECT);
      try {
         ps.setString(1, lo.getLockedObjectId());
         ps.setTimestamp(2, new Timestamp(lo.getLockDate().getTime()));
         ps.setString(3, lo.getLockRemark());
         ps.setString(4, lo.getLockedBy());
         ps.setString(5, lo.getLockState().name());
         ps.setString(6, lo.getObjectId());
         setBlob(ps, lo.getObject(), 7);
         ps.setString(8, lo.getTargetType());
         ps.setString(9, lo.getLockedEvent().name());
         ps.setString(10, lo.getMethod());
         ps.setString(11, lo.getTenant());
         if (lo.getUnlockDate() != null) {
            ps.setTimestamp(12, new Timestamp(lo.getUnlockDate().getTime()));
         } else {
            ps.setNull(12, Types.TIMESTAMP);
         }
         ps.setString(13, lo.getUnlockRemark());
         ps.setString(14, lo.getUnlockedBy());
         ps.executeUpdate();

         lo.setVersion(1);
      } finally {
         if (ps != null)
            ps.close();
      }
   }

   @Override
   public <T> T merge(Connection jdbcConnection, T obj) throws SQLException {
      LockedObject lo = (LockedObject) obj;
      PreparedStatement ps = jdbcConnection.prepareStatement(UPDATE_LOCKEDOBJECT);
      try {
         ps.setTimestamp(1, new Timestamp(lo.getLockDate().getTime()));
         ps.setString(2, lo.getLockRemark());
         ps.setString(3, lo.getLockedBy());
         ps.setString(4, lo.getLockState().name());
         ps.setString(5, lo.getObjectId());
         setBlob(ps, lo.getObject(), 6);
         ps.setString(7, lo.getTargetType());
         ps.setString(8, lo.getLockedEvent().name());
         ps.setString(9, lo.getMethod());
         ps.setString(10, lo.getTenant());
         if (lo.getUnlockDate() != null) {
            ps.setTimestamp(11, new Timestamp(lo.getUnlockDate().getTime()));
         } else {
            ps.setNull(11, Types.TIMESTAMP);
         }
         ps.setString(12, lo.getUnlockRemark());
         ps.setString(13, lo.getUnlockedBy());
         ps.setString(14, lo.getLockedObjectId());
         ps.setInt(15, lo.getVersion());

         int count = ps.executeUpdate();
         if (count != 1) {
            throw new CibetJdbcException("Failed to update LockedObject with id/version " + lo.getLockedObjectId() + "/"
                  + lo.getVersion() + ": id not found");
         }
         lo.setVersion(lo.getVersion() + 1);
      } finally {
         if (ps != null)
            ps.close();
      }
      return obj;
   }

   @Override
   public void remove(Connection jdbcConnection, Object obj) throws SQLException {
      LockedObject lo = (LockedObject) obj;
      PreparedStatement ps = null;
      try {
         ps = jdbcConnection.prepareStatement(DELETE_LOCKEDOBJECT);
         ps.setString(1, lo.getLockedObjectId());
         int count = ps.executeUpdate();
         log.debug(count + " LockedObject deleted");
      } finally {
         if (ps != null)
            ps.close();
      }
   }

}
