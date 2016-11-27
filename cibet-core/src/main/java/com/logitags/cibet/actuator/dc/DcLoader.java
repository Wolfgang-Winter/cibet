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
package com.logitags.cibet.actuator.dc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jpa.JpaResourceHandler;

@CibetContext
public abstract class DcLoader {

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(DcLoader.class);

   /**
    * convenient method that returns all unreleased DcControllable for the given tenant and the given target type.
    * 
    * @param targetType
    *           target type
    * @return list of DcControllable
    */
   public static List<DcControllable> findUnreleased(String targetType) {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT_CLASS);
      q.setParameter("tenant", Context.internalSessionScope().getTenant());
      q.setParameter("oclass", targetType);
      List<DcControllable> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * convenient method that returns all unreleased DcControllable for all tenants and the given target type.
    * 
    * @param targetType
    *           target type
    * @return list of DcControllable
    */
   public static List<DcControllable> findAllUnreleased(String targetType) {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_CLASS);
      q.setParameter("oclass", targetType);
      List<DcControllable> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * convenient method that returns all unreleased DcControllables for the given tenant.
    * 
    * @return List of DcControllable
    */
   public static List<DcControllable> findUnreleased() {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_TENANT);
      q.setParameter("tenant", Context.internalSessionScope().getTenant());
      List<DcControllable> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * convenient method that returns all unreleased DcControllables for all tenants.
    * 
    * @return List of DcControllable
    */
   public static List<DcControllable> findAllUnreleased() {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_ALL);
      List<DcControllable> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all DcControllables of the current tenant that belong to the same case ID.
    * 
    * @param caseId
    *           incidence id
    * @return list of DcControllable
    */
   public static List<DcControllable> loadByCaseId(String caseId) {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_CASEID);
      String tenant = Context.internalSessionScope().getTenant();
      query.setParameter("tenant", tenant);
      query.setParameter("caseId", caseId);
      List<DcControllable> list = query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all DcControllables that belong to the same case ID. Tenant is not taken into account.
    * 
    * @param caseId
    *           incidence id
    * @return list of DcControllable
    */
   public static List<DcControllable> loadAllByCaseId(String caseId) {
      Query query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_BY_CASEID_NO_TENANT);
      query.setParameter("caseId", caseId);
      List<DcControllable> list = query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads DcControllables of the current tenant that are created by the given user
    * 
    * @param user
    *           user id
    * @return list of DcControllable
    */
   public static List<DcControllable> loadByUser(String user) {
      TypedQuery<DcControllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_BY_USER, DcControllable.class);
      query.setParameter("user", user);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      List<DcControllable> list = query.getResultList();
      decrypt(list);

      return list;
   }

   /**
    * loads DcControllables that are created by the given user. Tenant is not taken into account.
    * 
    * @param user
    *           user id
    * @return list of DcControllable
    */
   public static List<DcControllable> loadAllByUser(String user) {
      TypedQuery<DcControllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_BY_USER_NO_TENANT, DcControllable.class);
      query.setParameter("user", user);
      List<DcControllable> list = query.getResultList();
      decrypt(list);

      return list;
   }

   /**
    * load all DcControllables for the current tenant where target type is equal to the entity class and resource
    * parameters exist with name equal to the key of the map and stringValue equal to the map's value.
    * 
    * @param entityClass
    *           target type
    * @param properties
    *           map of property name and value as search parameters for ResourceParameter.
    * @return list of DcControllable
    */
   public static List<DcControllable> loadByProperties(Class<?> entityClass, Map<String, Object> properties) {
      if (entityClass == null) {
         String msg = "parameter entityClass may not be null! "
               + "Call method loadDcControllablesByProperties() with a Class object";
         log.error(msg);
         throw new RuntimeException(msg);
      }

      List<Object> params = new ArrayList<Object>();
      params.add(Context.sessionScope().getTenant());
      params.add(entityClass.getName());
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT a.* FROM CIB_DCCONTROLLABLE a WHERE a.TENANT = ? AND a.TARGETTYPE = ?");

      if (properties != null) {
         for (Entry<String, Object> entry : properties.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().toString();

            sql.append(" AND EXISTS (SELECT 1 FROM CIB_RESOURCEPARAMETER p WHERE p.NAME = ?");
            params.add(entry.getKey());
            sql.append(" AND p.STRINGVALUE = ?");
            params.add(value);
            sql.append(" AND p.DCCONTROLLABLEID = a.DCCONTROLLABLEID");
            sql.append(")");
         }
      }

      log.debug("SQL: " + sql);
      EntityManager em = Context.internalRequestScope().getEntityManager();
      Query q = em.createNativeQuery(sql.toString(), DcControllable.class);
      for (int i = 0; i < params.size(); i++) {
         q.setParameter(i + 1, params.get(i));
      }

      List<DcControllable> list = q.getResultList();

      decrypt(list);
      return list;
   }

   /**
    * load all DcControllables where target type is equal to the entity class and resource parameters exist with name
    * equal to the key of the map and stringValue equal to the map's value. Tenant is not taken into account.
    * 
    * @param entityClass
    *           target type
    * @param properties
    *           map of property name and value as search parameters for ResourceParameter.
    * @return list of DcControllable
    */
   public static List<DcControllable> loadAllByProperties(Class<?> entityClass, Map<String, Object> properties) {
      if (entityClass == null) {
         String msg = "parameter entityClass may not be null! "
               + "Call method loadDcControllablesByProperties() with a Class object";
         log.error(msg);
         throw new RuntimeException(msg);
      }

      List<Object> params = new ArrayList<Object>();
      params.add(entityClass.getName());
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT a.* FROM CIB_DCCONTROLLABLE a WHERE a.TARGETTYPE = ?");

      if (properties != null) {
         for (Entry<String, Object> entry : properties.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().toString();

            sql.append(" AND EXISTS (SELECT 1 FROM CIB_RESOURCEPARAMETER p WHERE p.NAME = ?");
            params.add(entry.getKey());
            sql.append(" AND p.STRINGVALUE = ?");
            params.add(value);
            sql.append(" AND p.DCCONTROLLABLEID = a.DCCONTROLLABLEID");
            sql.append(")");
         }
      }

      log.debug("SQL: " + sql);
      EntityManager em = Context.internalRequestScope().getEntityManager();
      Query q = em.createNativeQuery(sql.toString(), DcControllable.class);
      for (int i = 0; i < params.size(); i++) {
         q.setParameter(i + 1, params.get(i));
      }

      List<DcControllable> list = q.getResultList();

      decrypt(list);
      return list;
   }

   /**
    * loads all DcControllables with the given groupId. Tenant is not considered.
    * 
    * @param groupId
    * @return
    */
   public static List<DcControllable> loadAllByGroupId(String groupId) {
      TypedQuery<DcControllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_BY_GROUPID, DcControllable.class);
      query.setParameter("groupId", groupId);
      List<DcControllable> list = query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * decrypts the DcControllable and Resource data if the DcControllable is encrypted.
    * 
    * @param list
    */
   public static void decrypt(List<DcControllable> list) {
      for (DcControllable dc : list) {
         dc.decrypt();
      }
   }

   /**
    * compares a scheduled modified resource object or a modified resource object under dual control with the unmodified
    * original object. Only applicable for JPA resources and ControlEvent UPDATE.
    * 
    * @param dc
    *           a DcControllable representing either a scheduled or a dual control event
    * @return
    */
   public static List<Difference> differences(DcControllable dc) {
      if (dc == null || dc.getResource() == null) {
         throw new IllegalArgumentException("Parameter DcControllable (or dc.getResource()) must not be null");
      }
      Resource res = dc.getResource();
      if (dc.getControlEvent() != ControlEvent.UPDATE || !(res.getResourceHandler() instanceof JpaResourceHandler)) {
         log.info("Differences can only be analyzed for UDATE events of JPA entites");
         return Collections.emptyList();
      }

      ResourceParameter rp = dc.getResource().getParameter(FourEyesActuator.CLEANOBJECT);
      if (rp != null) {
         // scheduled Dc
         return CibetUtil.compare(dc.getResource().getObject(), rp.getUnencodedValue());
      }

      rp = dc.getResource().getParameter(FourEyesActuator.DIFFERENCES);
      if (rp != null) {
         // dual control Dc
         return (List<Difference>) rp.getUnencodedValue();
      }

      String err = "DcControllable [" + dc + "] is in wrong state: Failed to find neither CLEANOBJECT nor DIFFERENCES ["
            + res.getTargetType() + " with ID " + dc.getResource().getPrimaryKeyObject() + "]";
      log.error(err);
      throw new IllegalStateException(err);

   }

}
