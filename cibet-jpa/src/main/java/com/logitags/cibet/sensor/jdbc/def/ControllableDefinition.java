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

import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class ControllableDefinition extends ResourceDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = 3483812263086473190L;

   private static Log log = LogFactory.getLog(ControllableDefinition.class);

   public static final String SEL_BY_PRIMARYKEY = "CONTROLLABLE.SEL_BY_PRIMARYKEY";

   protected static final String CONTROLLABLE = "d.controllableid, d.caseid, d.controlevent, d.createuser, d.createdate, "
         + "d.createaddress, d.createremark, d.tenant, d.actuator, d.firstapprovuser, d.firstapprovaldate, d.firstapprovaddr, "
         + "d.firstapprovremark, d.approvaluser, d.approvaldate, d.approvaladdress, d.approvalremark, d.executionstatus, d.version"
         + ", d.scheduleddate, d.executiondate, d.resourceid";
   protected static final String CONTROLLABLE2 = "controllableid, caseid, controlevent, createuser, createdate, "
         + "createaddress, createremark, tenant, actuator, firstapprovuser, firstapprovaldate, firstapprovaddr, "
         + "firstapprovremark, approvaluser, approvaldate, approvaladdress, approvalremark, executionstatus, version"
         + ", scheduleddate, executiondate, resourceid";

   private static final String INSERT_CONTROLLABLE = "INSERT INTO cib_controllable (" + CONTROLLABLE2
         + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

   private static final String UPDATE_CONTROLLABLE = "UPDATE cib_controllable SET "
         + "caseid=?, controlevent=?, actuator=?, firstapprovuser=?, firstapprovaldate=?, firstapprovremark=?, "
         + "version=version+1, approvaluser=?, approvaldate=?, approvaladdress=?, approvalremark=?,  executionstatus=?, "
         + "scheduleddate=?, executiondate=? WHERE controllableid=? AND version = ?";

   private static final String DELETE_CONTROLLABLE = "DELETE FROM cib_controllable WHERE controllableid=?";

   private static ControllableDefinition instance;

   public static synchronized ControllableDefinition getInstance() {
      if (instance == null) {
         instance = new ControllableDefinition();
      }
      return instance;
   }

   public ControllableDefinition() {
      queries.put(SEL_BY_PRIMARYKEY, "SELECT " + CONTROLLABLE + " FROM cib_controllable d WHERE d.controllableid = ?");

      queries.put(Controllable.SEL_BY_TENANT, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d WHERE d.tenant LIKE ? AND (d.executionstatus = 'POSTPONED' OR d.executionstatus = 'FIRST_POSTPONED' OR d.executionstatus = 'FIRST_RELEASED')");

      queries.put(Controllable.SEL_ALL, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d WHERE (d.executionstatus = 'POSTPONED' OR d.executionstatus = 'FIRST_POSTPONED' OR d.executionstatus = 'FIRST_RELEASED')");

      queries.put(Controllable.SEL_BY_TENANT_CLASS, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d, cib_resource r WHERE d.resourceid = r.resourceid and d.tenant LIKE ? AND r.target = ? AND (d.executionstatus = 'POSTPONED' OR d.executionstatus = 'FIRST_POSTPONED' OR d.executionstatus = 'FIRST_RELEASED')");

      queries.put(Controllable.SEL_BY_CLASS, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d, cib_resource r WHERE d.resourceid = r.resourceid and r.target = ? AND (d.executionstatus = 'POSTPONED' OR d.executionStatus = 'FIRST_POSTPONED' OR d.executionStatus = 'FIRST_RELEASED')");

      queries.put(Controllable.SEL_BY_ID_CLASS, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d, cib_resource r WHERE d.resourceid = r.resourceid and r.primarykeyid = ? AND r.target = ? AND (d.executionstatus = 'POSTPONED' OR d.executionstatus = 'FIRST_POSTPONED' OR d.executionstatus = 'FIRST_RELEASED')");

      queries.put(Controllable.SEL_BY_CASEID, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d WHERE d.tenant LIKE ? AND d.caseid = ? ORDER BY d.createdate");

      queries.put(Controllable.SEL_BY_CASEID_NO_TENANT,
            "SELECT " + CONTROLLABLE + " FROM cib_controllable d WHERE d.caseid = ? ORDER BY d.createdate");

      queries.put(Controllable.SEL_BY_UNIQUEID, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d, cib_resource r WHERE d.resourceid = r.resourceid and r.uniqueid = ? ORDER BY d.createdate");

      queries.put(Controllable.SEL_BY_USER,
            "SELECT " + CONTROLLABLE + " FROM cib_controllable d WHERE d.createuser = ? AND d.tenant = ?");

      queries.put(Controllable.SEL_BY_USER_NO_TENANT,
            "SELECT " + CONTROLLABLE + " FROM cib_controllable d WHERE d.createuser = ?");

      queries.put(Controllable.SEL_SCHED_BY_DATE, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d WHERE d.actuator = ? AND d.executionstatus = 'SCHEDULED' AND d.scheduleddate <= ?");

      queries.put(Controllable.SEL_SCHED_BY_TARGETTYPE, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d, cib_resource r WHERE d.resourceid = r.resourceid and d.tenant LIKE ? AND r.target = ? AND d.executionstatus = 'SCHEDULED'");

      queries.put(Controllable.SEL_SCHED_BY_TARGETTYPE_NO_TENANT, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d, cib_resource r WHERE d.resourceid = r.resourceid and r.target = ? AND d.executionstatus = 'SCHEDULED'");

      queries.put(Controllable.SEL_SCHED_BY_TENANT, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d WHERE d.tenant LIKE ? AND d.executionstatus = 'SCHEDULED'");

      queries.put(Controllable.SEL_SCHED,
            "SELECT " + CONTROLLABLE + " FROM cib_controllable d WHERE d.executionstatus = 'SCHEDULED'");

      queries.put(Controllable.SEL_LOCKED_BY_TARGETTYPE,
            "SELECT " + CONTROLLABLE + " FROM cib_controllable d, cib_resource r WHERE d.resourceid = r.resourceid and "
                  + "d.tenant LIKE ? AND r.target = ? AND d.executionstatus = 'LOCKED'");

      queries.put(Controllable.SEL_LOCKED_BY_TARGETTYPE_METHOD,
            "SELECT " + CONTROLLABLE + " FROM cib_controllable d, cib_resource r WHERE d.resourceid = r.resourceid and "
                  + "d.tenant LIKE ? AND r.target = ? AND r.method = ? AND d.executionstatus = 'LOCKED'");

      queries.put(Controllable.SEL_LOCKED_ALL, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d WHERE d.tenant LIKE ? AND d.executionstatus = 'LOCKED'");

      queries.put(Controllable.SEL_LOCKED_BY_USER, "SELECT " + CONTROLLABLE
            + " FROM cib_controllable d WHERE d.tenant LIKE ? AND d.createuser = ? AND d.executionstatus = 'LOCKED'");
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
      List<Controllable> list = new ArrayList<Controllable>();

      while (rs.next()) {
         Controllable dc = new Controllable();
         list.add(dc);

         dc.setControllableId(rs.getString(1));
         dc.setCaseId(rs.getString(2));
         dc.setControlEvent(ControlEvent.valueOf(rs.getString(3)));
         dc.setCreateUser(rs.getString(4));
         dc.setCreateDate(rs.getTimestamp(5));
         dc.setCreateAddress(rs.getString(6));
         dc.setCreateRemark(rs.getString(7));
         dc.setTenant(rs.getString(8));
         dc.setActuator(rs.getString(9));
         dc.setFirstApprovalUser(rs.getString(10));
         dc.setFirstApprovalDate(rs.getTimestamp(11));
         dc.setFirstApprovalAddress(rs.getString(12));
         dc.setFirstApprovalRemark(rs.getString(13));
         dc.setApprovalUser(rs.getString(14));
         dc.setApprovalDate(rs.getTimestamp(15));
         dc.setApprovalAddress(rs.getString(16));
         dc.setApprovalRemark(rs.getString(17));
         dc.setExecutionStatus(ExecutionStatus.valueOf(rs.getString(18)));
         dc.setVersion(rs.getInt(19));
         dc.setScheduledDate(rs.getTimestamp(20));
         dc.setExecutionDate(rs.getTimestamp(21));

         String resourceId = rs.getString(22);
         Resource r = loadResource(rs, resourceId);
         dc.setResource(r);

      }
      return list;
   }

   @Override
   public void persist(Connection jdbcConnection, Object obj) throws SQLException {
      Controllable dc = (Controllable) obj;

      dc.prePersist();
      dc.setVersion(1);

      super.persist(jdbcConnection, dc.getResource());

      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_CONTROLLABLE);
      try {
         ps.setString(1, dc.getControllableId());
         ps.setString(2, dc.getCaseId());
         ps.setString(3, dc.getControlEvent().name());
         ps.setString(4, dc.getCreateUser());
         ps.setTimestamp(5, new Timestamp(dc.getCreateDate().getTime()));
         ps.setString(6, dc.getCreateAddress());
         ps.setString(7, dc.getCreateRemark());

         ps.setString(8, dc.getTenant());
         ps.setString(9, dc.getActuator());
         ps.setString(10, dc.getFirstApprovalUser());
         if (dc.getFirstApprovalDate() != null) {
            ps.setTimestamp(11, new Timestamp(dc.getFirstApprovalDate().getTime()));
         } else {
            ps.setNull(11, Types.TIMESTAMP);
         }

         ps.setString(12, dc.getFirstApprovalAddress());
         ps.setString(13, dc.getFirstApprovalRemark());
         ps.setString(14, dc.getApprovalUser());
         if (dc.getApprovalDate() != null) {
            ps.setTimestamp(15, new Timestamp(dc.getApprovalDate().getTime()));
         } else {
            ps.setNull(15, Types.TIMESTAMP);
         }
         ps.setString(16, dc.getApprovalAddress());
         ps.setString(17, dc.getApprovalRemark());
         ps.setString(18, dc.getExecutionStatus().name());
         ps.setInt(19, dc.getVersion());
         if (dc.getScheduledDate() != null) {
            ps.setTimestamp(20, new Timestamp(dc.getScheduledDate().getTime()));
         } else {
            ps.setNull(20, Types.TIMESTAMP);
         }
         if (dc.getExecutionDate() != null) {
            ps.setTimestamp(21, new Timestamp(dc.getExecutionDate().getTime()));
         } else {
            ps.setNull(21, Types.TIMESTAMP);
         }
         ps.setString(22, dc.getResource().getResourceId());

         ps.executeUpdate();
      } finally {
         if (ps != null)
            ps.close();
      }
   }

   @Override
   public <T> T merge(Connection jdbcConnection, T obj) throws SQLException {
      Controllable dc = (Controllable) obj;
      PreparedStatement ps = jdbcConnection.prepareStatement(UPDATE_CONTROLLABLE);
      try {
         ps.setString(1, dc.getCaseId());
         ps.setString(2, dc.getControlEvent().name());
         ps.setString(3, dc.getActuator());
         ps.setString(4, dc.getFirstApprovalUser());
         if (dc.getFirstApprovalDate() != null) {
            ps.setTimestamp(5, new Timestamp(dc.getFirstApprovalDate().getTime()));
         } else {
            ps.setNull(5, Types.TIMESTAMP);
         }
         ps.setString(6, dc.getFirstApprovalRemark());
         ps.setString(7, dc.getApprovalUser());
         if (dc.getApprovalDate() != null) {
            ps.setTimestamp(8, new Timestamp(dc.getApprovalDate().getTime()));
         } else {
            ps.setNull(8, Types.TIMESTAMP);
         }
         ps.setString(9, dc.getApprovalAddress());
         ps.setString(10, dc.getApprovalRemark());
         ps.setString(11, dc.getExecutionStatus().name());
         if (dc.getScheduledDate() != null) {
            ps.setTimestamp(12, new Timestamp(dc.getScheduledDate().getTime()));
         } else {
            ps.setNull(12, Types.TIMESTAMP);
         }
         if (dc.getExecutionDate() != null) {
            ps.setTimestamp(13, new Timestamp(dc.getExecutionDate().getTime()));
         } else {
            ps.setNull(13, Types.TIMESTAMP);
         }

         ps.setString(14, dc.getControllableId());
         ps.setInt(15, dc.getVersion());

         int count = ps.executeUpdate();
         if (count != 1) {
            throw new CibetJdbcException(
                  "Failed to update Controllable with id " + dc.getControllableId() + ": id not found");
         }
         dc.setVersion(dc.getVersion() + 1);
      } finally {
         if (ps != null)
            ps.close();
      }
      return obj;
   }

   @Override
   public void remove(Connection jdbcConnection, Object obj) throws SQLException {
      PreparedStatement ps = null;
      try {
         Controllable dc = (Controllable) obj;
         ps = jdbcConnection.prepareStatement(DELETE_CONTROLLABLE);
         ps.setString(1, dc.getControllableId());
         int count = ps.executeUpdate();
         log.debug(count + " Controllable deleted");
      } finally {
         if (ps != null)
            ps.close();
      }
   }

}
