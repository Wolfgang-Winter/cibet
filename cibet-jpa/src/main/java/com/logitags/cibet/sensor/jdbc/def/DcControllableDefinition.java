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

import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class DcControllableDefinition extends ResourceDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = 3483812263086473190L;

   private static Log log = LogFactory.getLog(DcControllableDefinition.class);

   public static final String SEL_BY_PRIMARYKEY = "DCCONTROLLABLE.SEL_BY_PRIMARYKEY";

   protected static final String DCCONTROLLABLE = "d.dccontrollableid, d.caseid, d.controlevent, d.createuser, d.createdate, "
         + "d.createaddress, d.createremark, d.tenant, d.actuator, d.firstapprovuser, d.firstapprovaldate, d.firstapprovaddr, "
         + "d.firstapprovremark, d.approvaluser, d.approvaldate, d.approvaladdress, d.approvalremark, d.executionstatus, d.version"
         + ", d.scheduleddate, d.executiondate, d.resourceid";
   protected static final String DCCONTROLLABLE2 = "dccontrollableid, caseid, controlevent, createuser, createdate, "
         + "createaddress, createremark, tenant, actuator, firstapprovuser, firstapprovaldate, firstapprovaddr, "
         + "firstapprovremark, approvaluser, approvaldate, approvaladdress, approvalremark, executionstatus, version"
         + ", scheduleddate, executiondate, resourceid";

   private static final String INSERT_DCCONTROLLABLE = "INSERT INTO cib_dccontrollable (" + DCCONTROLLABLE2
         + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

   private static final String UPDATE_DCCONTROLLABLE = "UPDATE cib_dccontrollable SET "
         + "caseid=?, controlevent=?, actuator=?, firstapprovuser=?, firstapprovaldate=?, firstapprovremark=?, "
         + "version=version+1, approvaluser=?, approvaldate=?, approvaladdress=?, approvalremark=?,  executionstatus=?, "
         + "scheduleddate=?, executiondate=? WHERE dccontrollableid=? AND version = ?";

   private static final String DELETE_DCCONTROLLABLE = "DELETE FROM cib_dccontrollable WHERE dccontrollableid=?";

   private static DcControllableDefinition instance;

   public static synchronized DcControllableDefinition getInstance() {
      if (instance == null) {
         instance = new DcControllableDefinition();
      }
      return instance;
   }

   public DcControllableDefinition() {
      queries.put(SEL_BY_PRIMARYKEY,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable d WHERE d.dccontrollableid = ?");

      queries.put(DcControllable.SEL_BY_TENANT, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d WHERE d.tenant LIKE ? AND (d.executionstatus = 'POSTPONED' OR d.executionstatus = 'FIRST_POSTPONED' OR d.executionstatus = 'FIRST_RELEASED')");

      queries.put(DcControllable.SEL_ALL, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d WHERE (d.executionstatus = 'POSTPONED' OR d.executionstatus = 'FIRST_POSTPONED' OR d.executionstatus = 'FIRST_RELEASED')");

      queries.put(DcControllable.SEL_BY_TENANT_CLASS, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d, cib_resource r WHERE d.resourceid = r.resourceid and d.tenant LIKE ? AND r.targettype = ? AND (d.executionstatus = 'POSTPONED' OR d.executionstatus = 'FIRST_POSTPONED' OR d.executionstatus = 'FIRST_RELEASED')");

      queries.put(DcControllable.SEL_BY_CLASS, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d, cib_resource r WHERE d.resourceid = r.resourceid and r.targettype = ? AND (d.executionstatus = 'POSTPONED' OR d.executionStatus = 'FIRST_POSTPONED' OR d.executionStatus = 'FIRST_RELEASED')");

      queries.put(DcControllable.SEL_BY_ID_CLASS, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d, cib_resource r WHERE d.resourceid = r.resourceid and r.primarykeyid = ? AND r.targettype = ? AND (d.executionstatus = 'POSTPONED' OR d.executionstatus = 'FIRST_POSTPONED' OR d.executionstatus = 'FIRST_RELEASED')");

      queries.put(DcControllable.SEL_BY_CASEID, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d WHERE d.tenant LIKE ? AND d.caseid = ? ORDER BY d.createdate");

      queries.put(DcControllable.SEL_BY_CASEID_NO_TENANT,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable d WHERE d.caseid = ? ORDER BY d.createdate");

      queries.put(DcControllable.SEL_BY_UNIQUEID, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d, cib_resource r WHERE d.resourceid = r.resourceid and r.uniqueid = ? ORDER BY d.createdate");

      queries.put(DcControllable.SEL_BY_USER,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable d WHERE d.createuser = ? AND d.tenant = ?");

      queries.put(DcControllable.SEL_BY_USER_NO_TENANT,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable d WHERE d.createuser = ?");

      queries.put(DcControllable.SEL_SCHED_BY_DATE, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d WHERE d.actuator = ? AND d.executionstatus = 'SCHEDULED' AND d.scheduleddate <= ?");

      queries.put(DcControllable.SEL_SCHED_BY_TARGETTYPE, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d, cib_resource r WHERE d.resourceid = r.resourceid and d.tenant LIKE ? AND r.targettype = ? AND d.executionstatus = 'SCHEDULED'");

      queries.put(DcControllable.SEL_SCHED_BY_TARGETTYPE_NO_TENANT, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d, cib_resource r WHERE d.resourceid = r.resourceid and r.targettype = ? AND d.executionstatus = 'SCHEDULED'");

      queries.put(DcControllable.SEL_SCHED_BY_TENANT, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable d WHERE d.tenant LIKE ? AND d.executionstatus = 'SCHEDULED'");

      queries.put(DcControllable.SEL_SCHED,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable d WHERE d.executionstatus = 'SCHEDULED'");
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
      List<DcControllable> list = new ArrayList<DcControllable>();

      while (rs.next()) {
         DcControllable dc = new DcControllable();
         list.add(dc);

         dc.setDcControllableId(rs.getString(1));
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
      DcControllable dc = (DcControllable) obj;

      dc.prePersist();
      dc.setVersion(1);

      super.persist(jdbcConnection, dc.getResource());

      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_DCCONTROLLABLE);
      try {
         ps.setString(1, dc.getDcControllableId());
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
      DcControllable dc = (DcControllable) obj;
      PreparedStatement ps = jdbcConnection.prepareStatement(UPDATE_DCCONTROLLABLE);
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

         ps.setString(14, dc.getDcControllableId());
         ps.setInt(15, dc.getVersion());

         int count = ps.executeUpdate();
         if (count != 1) {
            throw new CibetJdbcException(
                  "Failed to update DcControllable with id " + dc.getDcControllableId() + ": id not found");
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
         DcControllable dc = (DcControllable) obj;
         ps = jdbcConnection.prepareStatement(DELETE_DCCONTROLLABLE);
         ps.setString(1, dc.getDcControllableId());
         int count = ps.executeUpdate();
         log.debug(count + " DcControllable deleted");
      } finally {
         if (ps != null)
            ps.close();
      }
   }

}
