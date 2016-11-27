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

import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class DcControllableDefinition extends AbstractEntityDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = 3483812263086473190L;

   private static Log log = LogFactory.getLog(DcControllableDefinition.class);

   public static final String SEL_BY_PRIMARYKEY = "DCCONTROLLABLE.SEL_BY_PRIMARYKEY";

   protected static final String DCCONTROLLABLE = "dccontrollableid, caseid, controlevent, createuser, createdate, "
         + "createaddress, createremark, targettype, target, tenant, actuator, firstapprovuser, firstapprovaldate, firstapprovaddr, "
         + "firstapprovremark, approvaluser, approvaldate, approvaladdress, approvalremark, executionstatus, version"
         + ", primarykeyid, method, invokerclass, invokerparam, resourcehandlerclass, result, encrypted, keyreference, uniqueid"
         + ", scheduleddate, executiondate, groupid";
   private static final String DCPARAMETER = "parameterid, dccontrollableid, name, classname, encodedvalue, sequence, parametertype, stringvalue";

   private static final String SEL_DCPARAMETER = "SELECT " + DCPARAMETER
         + " FROM CIB_RESOURCEPARAMETER WHERE dccontrollableid = ?";

   private static final String INSERT_DCCONTROLLABLE = "INSERT INTO cib_dccontrollable (" + DCCONTROLLABLE
         + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
   private static final String INSERT_DCPARAMETER = "INSERT INTO CIB_RESOURCEPARAMETER (" + DCPARAMETER
         + ") VALUES (?,?,?,?,?,?,?,?)";

   private static final String UPDATE_DCCONTROLLABLE = "UPDATE cib_dccontrollable SET "
         + "caseid=?, controlevent=?, targettype=?, target=?, actuator=?, "
         + "firstapprovuser=?, firstapprovaldate=?, firstapprovremark=?, version=version+1, "
         + "primarykeyid=?, method=?, invokerclass=?, invokerparam=?, approvaluser=?, "
         + "approvaldate=?, approvaladdress=?, approvalremark=?,  executionstatus=?, resourcehandlerclass=?, result=?, encrypted=?, "
         + "keyreference=?, uniqueid=?, scheduleddate=?, executiondate=?, groupid=? WHERE dccontrollableid=? AND version = ?";

   private static final String DELETE_DCPARAMETER = "DELETE FROM CIB_RESOURCEPARAMETER WHERE dccontrollableid=?";
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
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable WHERE dccontrollableid = ?");

      queries.put(DcControllable.SEL_BY_TENANT, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable WHERE tenant LIKE ? AND (executionstatus = 'POSTPONED' OR executionstatus = 'FIRST_POSTPONED' OR executionstatus = 'FIRST_RELEASED')");
      queries.put(DcControllable.SEL_ALL, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable WHERE (executionstatus = 'POSTPONED' OR executionstatus = 'FIRST_POSTPONED' OR executionstatus = 'FIRST_RELEASED')");
      queries.put(DcControllable.SEL_BY_TENANT_CLASS, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable WHERE tenant LIKE ? AND targettype = ? AND (executionstatus = 'POSTPONED' OR executionstatus = 'FIRST_POSTPONED' OR executionstatus = 'FIRST_RELEASED')");
      queries.put(DcControllable.SEL_BY_CLASS, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable WHERE targettype = ? AND (executionstatus = 'POSTPONED' OR executionStatus = 'FIRST_POSTPONED' OR executionStatus = 'FIRST_RELEASED')");
      queries.put(DcControllable.SEL_BY_ID_CLASS, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable WHERE primarykeyid = ? AND targettype = ? AND (executionstatus = 'POSTPONED' OR executionstatus = 'FIRST_POSTPONED' OR executionstatus = 'FIRST_RELEASED')");
      queries.put(DcControllable.SEL_BY_CASEID, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable WHERE tenant LIKE ? AND caseid = ? ORDER BY createdate");
      queries.put(DcControllable.SEL_BY_CASEID_NO_TENANT,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable WHERE caseid = ? ORDER BY createdate");

      queries.put(DcControllable.SEL_BY_UNIQUEID,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable WHERE uniqueid = ? ORDER BY createdate");

      queries.put(DcControllable.SEL_BY_USER,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable WHERE createuser = ? AND tenant = ?");
      queries.put(DcControllable.SEL_BY_USER_NO_TENANT,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable WHERE createuser = ?");

      queries.put(DcControllable.SEL_SCHED_BY_DATE, "SELECT " + DCCONTROLLABLE
            + " FROM dccontrollable WHERE actuator = ? AND executionstatus = 'SCHEDULED' AND scheduleddate <= ?");

      queries.put(DcControllable.SEL_SCHED_BY_TARGETTYPE, "SELECT " + DCCONTROLLABLE
            + " FROM dccontrollable WHERE tenant LIKE ? AND targettype = ? AND executionstatus = 'SCHEDULED'");
      queries.put(DcControllable.SEL_SCHED_BY_TARGETTYPE_NO_TENANT,
            "SELECT " + DCCONTROLLABLE + " FROM dccontrollable WHERE targettype = ? AND executionstatus = 'SCHEDULED'");

      queries.put(DcControllable.SEL_SCHED_BY_TENANT, "SELECT " + DCCONTROLLABLE
            + " FROM cib_dccontrollable WHERE tenant LIKE ? AND executionstatus = 'SCHEDULED'");
      queries.put(DcControllable.SEL_SCHED,
            "SELECT " + DCCONTROLLABLE + " FROM cib_dccontrollable WHERE executionstatus = 'SCHEDULED'");
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
         Resource r = new Resource();
         dc.setResource(r);
         list.add(dc);

         dc.setDcControllableId(rs.getString(1));
         dc.setCaseId(rs.getString(2));
         dc.setControlEvent(ControlEvent.valueOf(rs.getString(3)));
         dc.setCreateUser(rs.getString(4));
         dc.setCreateDate(rs.getTimestamp(5));
         dc.setCreateAddress(rs.getString(6));
         dc.setCreateRemark(rs.getString(7));
         r.setTargetType(rs.getString(8));
         r.setTarget(getBlob(rs, 9));
         dc.setTenant(rs.getString(10));
         dc.setActuator(rs.getString(11));
         dc.setFirstApprovalUser(rs.getString(12));
         dc.setFirstApprovalDate(rs.getTimestamp(13));
         dc.setFirstApprovalAddress(rs.getString(14));
         dc.setFirstApprovalRemark(rs.getString(15));
         dc.setApprovalUser(rs.getString(16));
         dc.setApprovalDate(rs.getTimestamp(17));
         dc.setApprovalAddress(rs.getString(18));
         dc.setApprovalRemark(rs.getString(19));
         dc.setExecutionStatus(ExecutionStatus.valueOf(rs.getString(20)));
         dc.setVersion(rs.getInt(21));
         r.setPrimaryKeyId(rs.getString(22));
         r.setMethod(rs.getString(23));
         r.setInvokerClass(rs.getString(24));
         r.setInvokerParam(rs.getString(25));
         r.setResourceHandlerClass(rs.getString(26));
         r.setResult(getBlob(rs, 27));
         r.setEncrypted(rs.getBoolean(28));
         r.setKeyReference(rs.getString(29));
         r.setUniqueId(rs.getString(30));
         dc.setScheduledDate(rs.getTimestamp(31));
         dc.setExecutionDate(rs.getTimestamp(32));
         r.setGroupId(rs.getString(33));

         PreparedStatement ps = null;
         try {
            ps = rs.getStatement().getConnection().prepareStatement(SEL_DCPARAMETER);
            ps.setString(1, dc.getDcControllableId());
            ResultSet paramRS = ps.executeQuery();
            while (paramRS.next()) {
               ResourceParameter ap = new ResourceParameter();
               r.getParameters().add(ap);
               ap.setParameterId(paramRS.getString(1));
               ap.setName(paramRS.getString(3));
               ap.setClassname(paramRS.getString(4));
               ap.setEncodedValue(getBlob(paramRS, 5));
               ap.setSequence(paramRS.getInt(6));
               ap.setParameterType(ParameterType.valueOf(paramRS.getString(7)));
               ap.setStringValue(paramRS.getString(8));
            }
         } finally {
            if (ps != null)
               ps.close();
         }
      }
      return list;
   }

   @Override
   public void persist(Connection jdbcConnection, Object obj) throws SQLException {
      DcControllable dc = (DcControllable) obj;

      dc.setDcControllableId(UUID.randomUUID().toString());
      dc.setVersion(1);
      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_DCCONTROLLABLE);
      try {
         ps.setString(1, dc.getDcControllableId());
         ps.setString(2, dc.getCaseId());
         ps.setString(3, dc.getControlEvent().name());
         ps.setString(4, dc.getCreateUser());
         ps.setTimestamp(5, new Timestamp(dc.getCreateDate().getTime()));
         ps.setString(6, dc.getCreateAddress());
         ps.setString(7, dc.getCreateRemark());

         ps.setString(8, dc.getResource().getTargetType());
         setBlob(ps, dc.getResource().getTarget(), 9);
         ps.setString(10, dc.getTenant());
         ps.setString(11, dc.getActuator());
         ps.setString(12, dc.getFirstApprovalUser());
         if (dc.getFirstApprovalDate() != null) {
            ps.setTimestamp(13, new Timestamp(dc.getFirstApprovalDate().getTime()));
         } else {
            ps.setNull(13, Types.TIMESTAMP);
         }

         ps.setString(14, dc.getFirstApprovalAddress());
         ps.setString(15, dc.getFirstApprovalRemark());
         ps.setString(16, dc.getApprovalUser());
         if (dc.getApprovalDate() != null) {
            ps.setTimestamp(17, new Timestamp(dc.getApprovalDate().getTime()));
         } else {
            ps.setNull(17, Types.TIMESTAMP);
         }
         ps.setString(18, dc.getApprovalAddress());
         ps.setString(19, dc.getApprovalRemark());
         ps.setString(20, dc.getExecutionStatus().name());
         ps.setInt(21, dc.getVersion());
         ps.setString(22, dc.getResource().getPrimaryKeyId());
         ps.setString(23, dc.getResource().getMethod());
         ps.setString(24, dc.getResource().getInvokerClass());
         ps.setString(25, dc.getResource().getInvokerParam());
         ps.setString(26, dc.getResource().getResourceHandlerClass());
         setBlob(ps, dc.getResource().getResult(), 27);
         ps.setBoolean(28, dc.getResource().isEncrypted());
         ps.setString(29, dc.getResource().getKeyReference());
         ps.setString(30, dc.getResource().getUniqueId());
         if (dc.getScheduledDate() != null) {
            ps.setTimestamp(31, new Timestamp(dc.getScheduledDate().getTime()));
         } else {
            ps.setNull(31, Types.TIMESTAMP);
         }
         if (dc.getExecutionDate() != null) {
            ps.setTimestamp(32, new Timestamp(dc.getExecutionDate().getTime()));
         } else {
            ps.setNull(32, Types.TIMESTAMP);
         }
         ps.setString(33, dc.getResource().getGroupId());

         ps.executeUpdate();
      } finally {
         if (ps != null)
            ps.close();
      }

      persistDcParameters(jdbcConnection, dc);
   }

   private void persistDcParameters(Connection jdbcConnection, DcControllable dc) throws SQLException {
      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_DCPARAMETER);
      try {
         for (ResourceParameter par : dc.getResource().getParameters()) {
            par.setParameterId(UUID.randomUUID().toString());

            ps.setString(1, par.getParameterId());
            ps.setString(2, dc.getDcControllableId());
            ps.setString(3, par.getName());
            ps.setString(4, par.getClassname());
            setBlob(ps, par.getEncodedValue(), 5);
            ps.setInt(6, par.getSequence());
            ps.setString(7, par.getParameterType().name());
            ps.setString(8, par.getStringValue());
            ps.executeUpdate();
         }
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
         ps.setString(3, dc.getResource().getTargetType());
         setBlob(ps, dc.getResource().getTarget(), 4);
         ps.setString(5, dc.getActuator());
         ps.setString(6, dc.getFirstApprovalUser());
         if (dc.getFirstApprovalDate() != null) {
            ps.setTimestamp(7, new Timestamp(dc.getFirstApprovalDate().getTime()));
         } else {
            ps.setNull(7, Types.TIMESTAMP);
         }
         ps.setString(8, dc.getFirstApprovalRemark());

         ps.setString(9, dc.getResource().getPrimaryKeyId());
         ps.setString(10, dc.getResource().getMethod());
         ps.setString(11, dc.getResource().getInvokerClass());
         ps.setString(12, dc.getResource().getInvokerParam());

         ps.setString(13, dc.getApprovalUser());
         if (dc.getApprovalDate() != null) {
            ps.setTimestamp(14, new Timestamp(dc.getApprovalDate().getTime()));
         } else {
            ps.setNull(14, Types.TIMESTAMP);
         }
         ps.setString(15, dc.getApprovalAddress());
         ps.setString(16, dc.getApprovalRemark());
         ps.setString(17, dc.getExecutionStatus().name());
         ps.setString(18, dc.getResource().getResourceHandlerClass());
         setBlob(ps, dc.getResource().getResult(), 19);
         ps.setBoolean(20, dc.getResource().isEncrypted());
         ps.setString(21, dc.getResource().getKeyReference());
         ps.setString(22, dc.getResource().getUniqueId());
         if (dc.getScheduledDate() != null) {
            ps.setTimestamp(23, new Timestamp(dc.getScheduledDate().getTime()));
         } else {
            ps.setNull(23, Types.TIMESTAMP);
         }
         if (dc.getExecutionDate() != null) {
            ps.setTimestamp(24, new Timestamp(dc.getExecutionDate().getTime()));
         } else {
            ps.setNull(24, Types.TIMESTAMP);
         }
         ps.setString(25, dc.getResource().getGroupId());

         ps.setString(26, dc.getDcControllableId());
         ps.setInt(27, dc.getVersion());

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
         ps = jdbcConnection.prepareStatement(DELETE_DCPARAMETER);
         DcControllable dc = (DcControllable) obj;
         ps.setString(1, dc.getDcControllableId());
         int count = ps.executeUpdate();
         log.debug(count + " ResourceParameter deleted");

         ps = jdbcConnection.prepareStatement(DELETE_DCCONTROLLABLE);
         ps.setString(1, dc.getDcControllableId());
         count = ps.executeUpdate();
         log.debug(count + " DcControllable deleted");
      } finally {
         if (ps != null)
            ps.close();
      }
   }

}
