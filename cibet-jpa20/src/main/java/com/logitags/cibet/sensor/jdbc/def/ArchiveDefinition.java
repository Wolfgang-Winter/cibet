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

import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class ArchiveDefinition extends AbstractEntityDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = 4878446144634455170L;

   private static Log log = LogFactory.getLog(ArchiveDefinition.class);

   public static final String SEL_BY_PRIMARYKEY = "ARCHIVE.SEL_BY_PRIMARYKEY";

   protected static final String ARCHIVE = "archiveid, remark, result, checksum, controlevent, targettype, createuser, "
         + "createdate, tenant, caseid, target, primarykeyid, method, invokerclass, invokerparam, executionstatus, version, "
         + "resourcehandlerclass, encrypted, keyreference, uniqueid, groupid";
   private static final String ARCHIVEPARAMETER = "parameterid, archiveid, name, classname, encodedvalue, sequence, parametertype, stringvalue";

   private static final String SEL_ARCHIVEPARAMETER = "SELECT " + ARCHIVEPARAMETER
         + " FROM CIB_RESOURCEPARAMETER WHERE archiveid = ?";

   private static final String INSERT_ARCHIVE = "INSERT INTO CIB_ARCHIVE (" + ARCHIVE
         + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?, 1, ?,?,?,?,?)";
   private static final String INSERT_ARCHIVEPARAMETER = "INSERT INTO CIB_RESOURCEPARAMETER (" + ARCHIVEPARAMETER
         + ") VALUES (?,?,?,?,?,?,?,?)";

   private static final String UPDATE_ARCHIVE = "UPDATE cib_archive SET remark=?, result=?, "
         + "checksum=?, controlevent=?, targettype=?, createuser=?, createdate=?, "
         + "tenant=?, caseid=?, target=?, primarykeyid=?, method=?, invokerclass=?, invokerparam=?, executionstatus=?, "
         + "version=version+1, resourcehandlerclass=?, encrypted=?, keyreference=?, uniqueid=?, groupid=? "
         + "WHERE archiveid=? AND version=?";

   private static final String DELETE_PARAM = "DELETE FROM cib_resourceparameter WHERE archiveid = ?";

   private static final String DELETE_ARCHIVE = "DELETE FROM cib_archive WHERE archiveid = ? and version = ?";

   private static ArchiveDefinition instance;

   public static synchronized ArchiveDefinition getInstance() {
      if (instance == null) {
         instance = new ArchiveDefinition();
      }
      return instance;
   }

   protected ArchiveDefinition() {
      queries.put(SEL_BY_PRIMARYKEY, "SELECT " + ARCHIVE + " FROM cib_archive WHERE archiveid = ?");

      queries.put(Archive.SEL_ALL, "SELECT " + ARCHIVE + " FROM cib_archive ORDER BY createdate");
      queries.put(Archive.SEL_ALL_BY_TENANT,
            "SELECT " + ARCHIVE + " FROM cib_archive WHERE tenant LIKE ? ORDER BY createdate");
      queries.put(Archive.SEL_ALL_BY_CLASS,
            "SELECT " + ARCHIVE + " FROM cib_archive WHERE tenant LIKE ? " + "AND targettype = ? ORDER BY createdate");
      queries.put(Archive.SEL_ALL_BY_CLASS_NO_TENANT,
            "SELECT " + ARCHIVE + " FROM cib_archive WHERE targettype = ? ORDER BY createdate");
      queries.put(Archive.SEL_ALL_BY_CASEID,
            "SELECT " + ARCHIVE + " FROM cib_archive WHERE tenant LIKE ? " + "AND caseid = ? ORDER BY createdate");
      queries.put(Archive.SEL_ALL_BY_CASEID_NO_TENANT,
            "SELECT " + ARCHIVE + " FROM cib_archive WHERE caseid = ? ORDER BY createdate");
      queries.put(Archive.SEL_BY_METHODNAME, "SELECT " + ARCHIVE
            + " FROM cib_archive WHERE tenant = ? AND targettype = ? " + "AND method = ? ORDER BY createdate");
      queries.put(Archive.SEL_BY_METHODNAME_NO_TENANT,
            "SELECT " + ARCHIVE + " FROM cib_archive WHERE targettype = ? " + "AND method = ? ORDER BY createdate");
      queries.put(Archive.SEL_BY_PRIMARYKEYID, "SELECT " + ARCHIVE + " FROM cib_archive WHERE "
            + "targettype = ? AND primarykeyid = ? ORDER BY createdate");
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
      List<Archive> list = new ArrayList<Archive>();

      while (rs.next()) {
         Archive ar = new Archive();
         Resource r = new Resource();
         ar.setResource(r);
         list.add(ar);

         ar.setArchiveId(rs.getString(1));
         ar.setRemark(rs.getString(2));
         r.setResult(getBlob(rs, 3));
         ar.setChecksum(rs.getString(4));
         String ce = rs.getString(5);
         if (rs.wasNull()) {
            ar.setControlEvent(null);
         } else {
            ar.setControlEvent(ControlEvent.valueOf(ce));
         }
         r.setTargetType(rs.getString(6));
         ar.setCreateUser(rs.getString(7));
         ar.setCreateDate(rs.getTimestamp(8));
         ar.setTenant(rs.getString(9));
         ar.setCaseId(rs.getString(10));
         r.setTarget(getBlob(rs, 11));
         r.setPrimaryKeyId(rs.getString(12));
         r.setMethod(rs.getString(13));
         r.setInvokerClass(rs.getString(14));
         r.setInvokerParam(rs.getString(15));
         ar.setExecutionStatus(ExecutionStatus.valueOf(rs.getString(16)));
         ar.setVersion(rs.getInt(17));
         r.setResourceHandlerClass(rs.getString(18));
         r.setEncrypted(rs.getBoolean(19));
         r.setKeyReference(rs.getString(20));
         r.setUniqueId(rs.getString(21));
         r.setGroupId(rs.getString(22));

         PreparedStatement ps = null;
         try {
            ps = rs.getStatement().getConnection().prepareStatement(SEL_ARCHIVEPARAMETER);
            ps.setString(1, ar.getArchiveId());
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
      Archive ar = (Archive) obj;
      ar.setArchiveId(UUID.randomUUID().toString());
      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_ARCHIVE);
      try {
         ps.setString(1, ar.getArchiveId());
         ps.setString(2, ar.getRemark());
         setBlob(ps, ar.getResource().getResult(), 3);
         ps.setString(4, ar.getChecksum());
         if (ar.getControlEvent() == null) {
            ps.setNull(5, Types.VARCHAR);
         } else {
            ps.setString(5, ar.getControlEvent().name());
         }
         ps.setString(6, ar.getResource().getTargetType());
         ps.setString(7, ar.getCreateUser());
         ps.setTimestamp(8, new Timestamp(ar.getCreateDate().getTime()));
         ps.setString(9, ar.getTenant());
         ps.setString(10, ar.getCaseId());
         setBlob(ps, ar.getResource().getTarget(), 11);
         ps.setString(12, ar.getResource().getPrimaryKeyId());
         ps.setString(13, ar.getResource().getMethod());
         ps.setString(14, ar.getResource().getInvokerClass());
         ps.setString(15, ar.getResource().getInvokerParam());
         ps.setString(16, ar.getExecutionStatus().name());

         ps.setString(17, ar.getResource().getResourceHandlerClass());
         ps.setBoolean(18, ar.getResource().isEncrypted());
         ps.setString(19, ar.getResource().getKeyReference());
         ps.setString(20, ar.getResource().getUniqueId());
         ps.setString(21, ar.getResource().getGroupId());

         ps.executeUpdate();

         persistArchiveParameters(jdbcConnection, ar);

      } finally {
         if (ps != null)
            ps.close();
      }
   }

   private void persistArchiveParameters(Connection jdbcConnection, Archive ar) throws SQLException {
      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_ARCHIVEPARAMETER);
      try {
         for (ResourceParameter par : ar.getResource().getParameters()) {
            par.setParameterId(UUID.randomUUID().toString());

            ps.setString(1, par.getParameterId());
            ps.setString(2, ar.getArchiveId());
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
      Archive ar = (Archive) obj;
      PreparedStatement ps = jdbcConnection.prepareStatement(UPDATE_ARCHIVE);
      try {
         ps.setString(1, ar.getRemark());
         setBlob(ps, ar.getResource().getResult(), 2);
         ps.setString(3, ar.getChecksum());
         if (ar.getControlEvent() == null) {
            ps.setNull(4, Types.VARCHAR);
         } else {
            ps.setString(4, ar.getControlEvent().name());
         }
         ps.setString(5, ar.getResource().getTargetType());
         ps.setString(6, ar.getCreateUser());
         if (ar.getCreateDate() != null) {
            ps.setTimestamp(7, new Timestamp(ar.getCreateDate().getTime()));
         } else {
            ps.setNull(7, Types.TIMESTAMP);
         }
         ps.setString(8, ar.getTenant());
         ps.setString(9, ar.getCaseId());
         setBlob(ps, ar.getResource().getTarget(), 10);
         ps.setString(11, ar.getResource().getPrimaryKeyId());

         ps.setString(12, ar.getResource().getMethod());
         ps.setString(13, ar.getResource().getInvokerClass());
         ps.setString(14, ar.getResource().getInvokerParam());

         ps.setString(15, ar.getExecutionStatus().name());
         ps.setString(16, ar.getResource().getResourceHandlerClass());
         ps.setBoolean(17, ar.getResource().isEncrypted());
         ps.setString(18, ar.getResource().getKeyReference());
         ps.setString(19, ar.getResource().getUniqueId());
         ps.setString(20, ar.getResource().getGroupId());

         ps.setString(21, ar.getArchiveId());
         ps.setInt(22, ar.getVersion());

         ar.setVersion(ar.getVersion() + 1);

         int count = ps.executeUpdate();
         if (count != 1) {
            throw new CibetJdbcException("Failed to update Archive with id " + ar.getArchiveId() + " and version "
                  + ar.getVersion() + ": id not found or optimistic locked");
         }
      } finally {
         if (ps != null)
            ps.close();
      }
      return obj;
   }

   /*
    * (non-Javadoc)
    * 
    * @see
    * com.logitags.cibet.sensor.jdbc.def.AbstractEntityDefinition#remove(java
    * .sql.Connection, java.lang.Object)
    */
   @Override
   public void remove(Connection jdbcConnection, Object obj) throws SQLException {
      PreparedStatement stmt = null;
      try {
         Archive ar = (Archive) obj;
         stmt = jdbcConnection.prepareStatement(DELETE_PARAM);
         stmt.setString(1, ar.getArchiveId());
         int count = stmt.executeUpdate();
         log.info(count + " ResourceParameter removed");

         stmt = jdbcConnection.prepareStatement(DELETE_ARCHIVE);
         stmt.setString(1, ar.getArchiveId());
         stmt.setInt(2, ar.getVersion());
         count = stmt.executeUpdate();
         log.info(count + " Archive removed");
         if (count != 1) {
            throw new CibetJdbcException("Failed to delete Archive with id " + ar.getArchiveId() + " and version "
                  + ar.getVersion() + ": id not found or optimistic locked");
         }
      } finally {
         if (stmt != null)
            stmt.close();
      }
   }

}
