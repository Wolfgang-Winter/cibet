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

import com.logitags.cibet.actuator.archive.Archive;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;

public class ArchiveDefinition extends ResourceDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = 4878446144634455170L;

   private static Log log = LogFactory.getLog(ArchiveDefinition.class);

   public static final String SEL_BY_PRIMARYKEY = "ARCHIVE.SEL_BY_PRIMARYKEY";

   protected static final String ARCHIVE = "a.archiveid, a.remark, a.checksum, a.controlevent, a.createuser, a.createdate, a.tenant, a.caseid, "
         + "a.executionstatus, a.version, a.resourceid";
   protected static final String ARCHIVE2 = "archiveid, remark, checksum, controlevent, createuser, createdate, tenant, caseid, "
         + "executionstatus, version, resourceid";

   private static final String INSERT_ARCHIVE = "INSERT INTO CIB_ARCHIVE (" + ARCHIVE2
         + ") VALUES (?,?,?,?,?,?,?,?,?, 1, ?)";

   private static final String UPDATE_ARCHIVE = "UPDATE cib_archive SET remark=?, "
         + "checksum=?, controlevent=?, createuser=?, createdate=?, tenant=?, caseid=?, executionstatus=?, "
         + "version=version+1 WHERE archiveid=? AND version=?";

   private static final String DELETE_ARCHIVE = "DELETE FROM cib_archive WHERE archiveid = ? and version = ?";

   private static ArchiveDefinition instance;

   public static synchronized ArchiveDefinition getInstance() {
      if (instance == null) {
         instance = new ArchiveDefinition();
      }
      return instance;
   }

   protected ArchiveDefinition() {
      queries.put(SEL_BY_PRIMARYKEY, "SELECT " + ARCHIVE + " FROM cib_archive a WHERE a.archiveid = ?");

      queries.put(Archive.SEL_ALL, "SELECT " + ARCHIVE + " FROM cib_archive a ORDER BY a.createdate");
      queries.put(Archive.SEL_ALL_BY_TENANT,
            "SELECT " + ARCHIVE + " FROM cib_archive a WHERE a.tenant LIKE ? ORDER BY a.createdate");
      queries.put(Archive.SEL_ALL_BY_CLASS,
            "SELECT " + ARCHIVE
                  + " FROM cib_archive a, cib_resource r WHERE a.resourceid = r.resourceid and a.tenant LIKE ? "
                  + "AND r.target = ? ORDER BY a.createdate");
      queries.put(Archive.SEL_ALL_BY_CLASS_NO_TENANT, "SELECT " + ARCHIVE
            + " FROM cib_archive a, cib_resource r WHERE a.resourceid = r.resourceid and r.target = ? ORDER BY a.createdate");
      queries.put(Archive.SEL_ALL_BY_CASEID, "SELECT " + ARCHIVE + " FROM cib_archive a WHERE a.tenant LIKE ? "
            + "AND a.caseid = ? ORDER BY a.createdate");
      queries.put(Archive.SEL_ALL_BY_CASEID_NO_TENANT,
            "SELECT " + ARCHIVE + " FROM cib_archive a WHERE a.caseid = ? ORDER BY a.createdate");
      queries.put(Archive.SEL_BY_METHODNAME,
            "SELECT " + ARCHIVE
                  + " FROM cib_archive a, cib_resource r WHERE a.resourceid = r.resourceid and a.tenant = ? AND r.target = ? "
                  + "AND r.method = ? ORDER BY a.createdate");
      queries.put(Archive.SEL_BY_METHODNAME_NO_TENANT,
            "SELECT " + ARCHIVE
                  + " FROM cib_archive a, cib_resource r WHERE a.resourceid = r.resourceid and r.target = ? "
                  + "AND r.method = ? ORDER BY a.createdate");
      queries.put(Archive.SEL_BY_PRIMARYKEYID,
            "SELECT " + ARCHIVE + " FROM cib_archive a, cib_resource r WHERE a.resourceid = r.resourceid and "
                  + "r.target = ? AND r.primarykeyid = ? ORDER BY a.createdate");
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
         list.add(ar);

         ar.setArchiveId(rs.getString(1));
         ar.setRemark(rs.getString(2));
         ar.setChecksum(rs.getString(3));
         String ce = rs.getString(4);
         if (rs.wasNull()) {
            ar.setControlEvent(null);
         } else {
            ar.setControlEvent(ControlEvent.valueOf(ce));
         }
         ar.setCreateUser(rs.getString(5));
         ar.setCreateDate(rs.getTimestamp(6));
         ar.setTenant(rs.getString(7));
         ar.setCaseId(rs.getString(8));
         ar.setExecutionStatus(ExecutionStatus.valueOf(rs.getString(9)));
         ar.setVersion(rs.getInt(10));

         String resourceId = rs.getString(11);
         Resource r = loadResource(rs, resourceId);
         ar.setResource(r);
      }
      return list;
   }

   @Override
   public void persist(Connection jdbcConnection, Object obj) throws SQLException {
      Archive ar = (Archive) obj;
      ar.prePersist();

      super.persist(jdbcConnection, ar.getResource());

      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_ARCHIVE);
      try {
         ps.setString(1, ar.getArchiveId());
         ps.setString(2, ar.getRemark());
         ps.setString(3, ar.getChecksum());
         if (ar.getControlEvent() == null) {
            ps.setNull(4, Types.VARCHAR);
         } else {
            ps.setString(4, ar.getControlEvent().name());
         }
         ps.setString(5, ar.getCreateUser());
         ps.setTimestamp(6, new Timestamp(ar.getCreateDate().getTime()));
         ps.setString(7, ar.getTenant());
         ps.setString(8, ar.getCaseId());
         ps.setString(9, ar.getExecutionStatus().name());
         ps.setString(10, ar.getResource().getResourceId());

         ps.executeUpdate();

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
         ps.setString(2, ar.getChecksum());
         if (ar.getControlEvent() == null) {
            ps.setNull(3, Types.VARCHAR);
         } else {
            ps.setString(3, ar.getControlEvent().name());
         }
         ps.setString(4, ar.getCreateUser());
         if (ar.getCreateDate() != null) {
            ps.setTimestamp(5, new Timestamp(ar.getCreateDate().getTime()));
         } else {
            ps.setNull(5, Types.TIMESTAMP);
         }
         ps.setString(6, ar.getTenant());
         ps.setString(7, ar.getCaseId());
         ps.setString(8, ar.getExecutionStatus().name());

         ps.setString(9, ar.getArchiveId());
         ps.setInt(10, ar.getVersion());

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
    * @see com.logitags.cibet.sensor.jdbc.def.AbstractEntityDefinition#remove(java .sql.Connection, java.lang.Object)
    */
   @Override
   public void remove(Connection jdbcConnection, Object obj) throws SQLException {
      PreparedStatement stmt = null;
      try {
         Archive ar = (Archive) obj;
         stmt = jdbcConnection.prepareStatement(DELETE_ARCHIVE);
         stmt.setString(1, ar.getArchiveId());
         stmt.setInt(2, ar.getVersion());
         int count = stmt.executeUpdate();
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
