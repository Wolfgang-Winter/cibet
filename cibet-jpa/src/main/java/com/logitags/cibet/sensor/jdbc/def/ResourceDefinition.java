/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2016 Dr. Wolfgang Winter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************
 */
package com.logitags.cibet.sensor.jdbc.def;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Set;

import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.ejb.EjbResource;
import com.logitags.cibet.sensor.http.HttpRequestResource;
import com.logitags.cibet.sensor.jdbc.driver.CibetJdbcException;
import com.logitags.cibet.sensor.jdbc.driver.JdbcResource;
import com.logitags.cibet.sensor.jpa.JpaQueryResource;
import com.logitags.cibet.sensor.jpa.JpaResource;
import com.logitags.cibet.sensor.pojo.MethodResource;

public class ResourceDefinition extends AbstractEntityDefinition {

   /**
    * 
    */
   private static final long serialVersionUID = 1L;

   private static final String RESOURCEPARAMETER = "parameterid, name, classname, encodedvalue, sequence, parametertype, stringvalue, resourceid";
   private static final String RESOURCE = "resourcetype, resourceid, result, target, targetobject, primarykeyid, method, invokerclass, invokerparam, "
         + "encrypted, keyreference, uniqueid, groupid";

   private static final String SEL_RESOURCE = "SELECT " + RESOURCE + " FROM CIB_RESOURCE WHERE resourceid = ?";

   private static final String SEL_RESOURCEPARAMETER = "SELECT " + RESOURCEPARAMETER
         + " FROM CIB_RESOURCEPARAMETER WHERE resourceid = ?";

   private static final String INSERT_RESOURCE = "INSERT INTO CIB_RESOURCE (" + RESOURCE
         + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)";
   private static final String INSERT_RESOURCEPARAMETER = "INSERT INTO CIB_RESOURCEPARAMETER (" + RESOURCEPARAMETER
         + ") VALUES (?,?,?,?,?,?,?,?)";

   private static final String UPDATE_RESOURCE = "UPDATE cib_resource SET result=? WHERE resourceid=?";

   private static ResourceDefinition instance;

   public static synchronized ResourceDefinition getInstance() {
      if (instance == null) {
         instance = new ResourceDefinition();
      }
      return instance;
   }

   protected ResourceDefinition() {
   }

   /**
    * 
    * @param resRs
    * @param archiveId
    * @return
    * @throws SQLException
    */
   protected Resource loadResource(ResultSet rs, String archiveId) throws SQLException {
      PreparedStatement ps = null;
      try {
         ps = rs.getStatement().getConnection().prepareStatement(SEL_RESOURCE);
         ps.setString(1, archiveId);
         ResultSet resRs = ps.executeQuery();
         resRs.next();

         Resource r = null;
         String type = resRs.getString(1);
         switch (type) {
         case "EjbResource":
            r = new EjbResource();
            ((EjbResource) r).setMethod(resRs.getString(7));
            ((EjbResource) r).setInvokerClass(resRs.getString(8));
            ((EjbResource) r).setInvokerParam(resRs.getString(9));
            break;

         case "HttpRequestResource":
            r = new HttpRequestResource();
            ((HttpRequestResource) r).setMethod(resRs.getString(7));
            break;

         case "JdbcResource":
            r = new JdbcResource();
            ((JdbcResource) r).setPrimaryKeyId(resRs.getString(6));
            break;

         case "JpaResource":
            r = new JpaResource();
            ((JpaResource) r).setPrimaryKeyId(resRs.getString(6));
            break;

         case "JpaQueryResource":
            r = new JpaQueryResource();
            break;

         case "MethodResource":
            r = new MethodResource();
            ((MethodResource) r).setMethod(resRs.getString(7));
            ((MethodResource) r).setInvokerClass(resRs.getString(8));
            ((MethodResource) r).setInvokerParam(resRs.getString(9));
            break;

         default:
            throw new SQLException("Unsupported Resource type " + type);
         }

         r.setResourceId(resRs.getString(2));
         r.setResult(getBlob(resRs, 3));
         r.setTarget(resRs.getString(4));
         r.setTargetObject(getBlob(resRs, 5));
         r.setEncrypted(resRs.getBoolean(10));
         r.setKeyReference(resRs.getString(11));
         r.setUniqueId(resRs.getString(12));
         r.setGroupId(resRs.getString(13));

         ps.close();
         resRs.close();

         ps = rs.getStatement().getConnection().prepareStatement(SEL_RESOURCEPARAMETER);
         ps.setString(1, r.getResourceId());
         ResultSet paramRS = ps.executeQuery();
         while (paramRS.next()) {
            ResourceParameter ap = new ResourceParameter();
            r.addParameter(ap);
            ap.setParameterId(paramRS.getString(1));
            ap.setName(paramRS.getString(2));
            ap.setClassname(paramRS.getString(3));
            ap.setEncodedValue(getBlob(paramRS, 4));
            ap.setSequence(paramRS.getInt(5));
            ap.setParameterType(ParameterType.valueOf(paramRS.getString(6)));
            ap.setStringValue(paramRS.getString(7));
         }

         return r;
      } finally {
         if (ps != null)
            ps.close();
      }
   }

   /**
    * 
    * @param jdbcConnection
    * @param ar
    * @throws SQLException
    */
   @Override
   public void persist(Connection jdbcConnection, Object obj) throws SQLException {
      Resource r = (Resource) obj;
      if (r.getResourceId() != null) {
         // already persisted
         return;
      }

      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_RESOURCE);
      try {
         r.prePersist();

         ps.setNull(6, Types.VARCHAR);
         ps.setNull(7, Types.VARCHAR);
         ps.setNull(8, Types.VARCHAR);
         ps.setNull(9, Types.VARCHAR);

         switch (r.getClass().getSimpleName()) {
         case "EjbResource":
            ps.setString(7, ((EjbResource) r).getMethod());
            ps.setString(8, ((EjbResource) r).getInvokerClass());
            ps.setString(9, ((EjbResource) r).getInvokerParam());
            break;

         case "HttpRequestResource":
            ps.setString(7, ((HttpRequestResource) r).getMethod());
            break;

         case "JdbcResource":
            ps.setString(6, ((JdbcResource) r).getPrimaryKeyId());
            break;

         case "JpaResource":
            ps.setString(6, ((JpaResource) r).getPrimaryKeyId());
            break;

         case "JpaQueryResource":
            break;

         case "MethodResource":
            ps.setString(7, ((MethodResource) r).getMethod());
            ps.setString(8, ((MethodResource) r).getInvokerClass());
            ps.setString(9, ((MethodResource) r).getInvokerParam());
            break;

         default:
            throw new SQLException("Unsupported Resource type " + r.getClass().getSimpleName());
         }

         ps.setString(1, r.getClass().getSimpleName());
         ps.setString(2, r.getResourceId());
         setBlob(ps, r.getResult(), 3);
         ps.setString(4, r.getTarget());
         setBlob(ps, r.getTargetObject(), 5);

         ps.setBoolean(10, r.isEncrypted());
         ps.setString(11, r.getKeyReference());
         ps.setString(12, r.getUniqueId());
         ps.setString(13, r.getGroupId());
         ps.executeUpdate();

         persistResourceParameters(jdbcConnection, r);

      } finally {
         if (ps != null)
            ps.close();
      }
   }

   @Override
   public <T> T merge(Connection jdbcConnection, T obj) throws SQLException {
      Resource r = (Resource) obj;
      PreparedStatement ps = jdbcConnection.prepareStatement(UPDATE_RESOURCE);
      try {
         setBlob(ps, r.getResult(), 1);
         ps.setString(2, r.getResourceId());

         int count = ps.executeUpdate();
         if (count != 1) {
            throw new CibetJdbcException("Failed to update Resource with id " + r.getResourceId() + ": id not found");
         }
      } finally {
         if (ps != null)
            ps.close();
      }
      return obj;
   }

   /**
    *
    * 
    * @param jdbcConnection
    * @param r
    * @throws SQLException
    */
   private void persistResourceParameters(Connection jdbcConnection, Resource r) throws SQLException {
      PreparedStatement ps = jdbcConnection.prepareStatement(INSERT_RESOURCEPARAMETER);
      try {
         Set<ResourceParameter> parameters = r.getParameters();
         for (ResourceParameter par : parameters) {
            par.prePersist();

            ps.setString(1, par.getParameterId());
            ps.setString(2, par.getName());
            ps.setString(3, par.getClassname());
            setBlob(ps, par.getEncodedValue(), 4);
            ps.setInt(5, par.getSequence());
            ps.setString(6, par.getParameterType().name());
            ps.setString(7, par.getStringValue());
            ps.setString(8, r.getResourceId());
            ps.executeUpdate();
         }
         r.setParameters(parameters);
      } finally {
         if (ps != null)
            ps.close();
      }
   }

   @Override
   public <T> T find(Connection jdbcConnection, Class<T> clazz, Object primaryKey) {
      throw new CibetJdbcException("find() method not implemented");
   }

}
