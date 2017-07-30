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

import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jpa.JpaResource;

@CibetContext
public abstract class DcLoader {

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(DcLoader.class);

   /**
    * convenient method that returns all unreleased Controllable for the given tenant and the given target type.
    * 
    * @param target
    *           target
    * @return list of Controllable
    */
   public static List<Controllable> findUnreleased(String target) {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(Controllable.SEL_BY_TENANT_CLASS);
      q.setParameter("tenant", Context.internalSessionScope().getTenant() + "%");
      q.setParameter("oclass", target);
      List<Controllable> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * convenient method that returns all unreleased Controllable for all tenants and the given target type.
    * 
    * @param target
    *           target
    * @return list of Controllable
    */
   public static List<Controllable> findAllUnreleased(String target) {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(Controllable.SEL_BY_CLASS);
      q.setParameter("oclass", target);
      List<Controllable> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * convenient method that returns all unreleased Controllables for the given tenant.
    * 
    * @return List of Controllable
    */
   public static List<Controllable> findUnreleased() {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(Controllable.SEL_BY_TENANT);
      q.setParameter("tenant", Context.internalSessionScope().getTenant() + "%");
      List<Controllable> list = q.getResultList();

      decrypt(list);
      return list;
   }

   /**
    * convenient method that returns all unreleased Controllables for all tenants.
    * 
    * @return List of Controllable
    */
   public static List<Controllable> findAllUnreleased() {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(Controllable.SEL_ALL);
      List<Controllable> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all Controllables of the current tenant that belong to the same case ID.
    * 
    * @param caseId
    *           incidence id
    * @return list of Controllable
    */
   public static List<Controllable> loadByCaseId(String caseId) {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(Controllable.SEL_BY_CASEID);
      String tenant = Context.internalSessionScope().getTenant();
      query.setParameter("tenant", tenant + "%");
      query.setParameter("caseId", caseId);
      List<Controllable> list = query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all Controllables that belong to the same case ID. Tenant is not taken into account.
    * 
    * @param caseId
    *           incidence id
    * @return list of Controllable
    */
   public static List<Controllable> loadAllByCaseId(String caseId) {
      Query query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Controllable.SEL_BY_CASEID_NO_TENANT);
      query.setParameter("caseId", caseId);
      List<Controllable> list = query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads Controllables of the current tenant that are created by the given user
    * 
    * @param user
    *           user id
    * @return list of Controllable
    */
   public static List<Controllable> loadByUser(String user) {
      TypedQuery<Controllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Controllable.SEL_BY_USER, Controllable.class);
      query.setParameter("user", user);
      query.setParameter("tenant", Context.internalSessionScope().getTenant() + "%");
      List<Controllable> list = query.getResultList();
      decrypt(list);

      return list;
   }

   /**
    * loads Controllables that are created by the given user. Tenant is not taken into account.
    * 
    * @param user
    *           user id
    * @return list of Controllable
    */
   public static List<Controllable> loadAllByUser(String user) {
      TypedQuery<Controllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Controllable.SEL_BY_USER_NO_TENANT, Controllable.class);
      query.setParameter("user", user);
      List<Controllable> list = query.getResultList();
      decrypt(list);

      return list;
   }

   /**
    * load all Controllables for the current tenant where target type is equal to the entity class and resource
    * parameters exist with name equal to the key of the map and stringValue equal to the map's value.
    * 
    * @param entityClass
    *           target type
    * @param properties
    *           map of property name and value as search parameters for ResourceParameter.
    * @return list of Controllable
    */
   public static List<Controllable> loadByProperties(Class<?> entityClass, Map<String, Object> properties) {
      if (entityClass == null) {
         String msg = "parameter entityClass may not be null! "
               + "Call method loadControllablesByProperties() with a Class object";
         log.error(msg);
         throw new RuntimeException(msg);
      }

      List<Object> params = new ArrayList<Object>();
      params.add(Context.sessionScope().getTenant() + "%");
      params.add(entityClass.getName());
      StringBuffer sql = new StringBuffer();
      sql.append(
            "SELECT a.* FROM CIB_CONTROLLABLE a, CIB_RESOURCE r WHERE a.RESOURCEID = r.RESOURCEID and a.TENANT = ? AND r.TARGET = ?");

      if (properties != null) {
         for (Entry<String, Object> entry : properties.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().toString();

            sql.append(" AND EXISTS (SELECT 1 FROM CIB_RESOURCEPARAMETER p WHERE p.NAME = ?");
            params.add(entry.getKey());
            sql.append(" AND p.STRINGVALUE = ?");
            params.add(value);
            sql.append(" AND p.RESOURCEID = a.RESOURCEID");
            sql.append(")");
         }
      }

      log.debug("SQL: " + sql);
      EntityManager em = Context.internalRequestScope().getEntityManager();
      Query q = em.createNativeQuery(sql.toString(), Controllable.class);
      for (int i = 0; i < params.size(); i++) {
         q.setParameter(i + 1, params.get(i));
      }

      List<Controllable> list = q.getResultList();

      decrypt(list);
      return list;
   }

   /**
    * load all Controllables where target type is equal to the entity class and resource parameters exist with name
    * equal to the key of the map and stringValue equal to the map's value. Tenant is not taken into account.
    * 
    * @param entityClass
    *           target type
    * @param properties
    *           map of property name and value as search parameters for ResourceParameter.
    * @return list of Controllable
    */
   public static List<Controllable> loadAllByProperties(Class<?> entityClass, Map<String, Object> properties) {
      if (entityClass == null) {
         String msg = "parameter entityClass may not be null! "
               + "Call method loadControllablesByProperties() with a Class object";
         log.error(msg);
         throw new RuntimeException(msg);
      }

      List<Object> params = new ArrayList<Object>();
      params.add(entityClass.getName());
      StringBuffer sql = new StringBuffer();
      sql.append(
            "SELECT a.* FROM CIB_CONTROLLABLE a, CIB_RESOURCE r WHERE a.RESOURCEID = r.RESOURCEID and r.TARGET = ?");

      if (properties != null) {
         for (Entry<String, Object> entry : properties.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().toString();

            sql.append(" AND EXISTS (SELECT 1 FROM CIB_RESOURCEPARAMETER p WHERE p.NAME = ?");
            params.add(entry.getKey());
            sql.append(" AND p.STRINGVALUE = ?");
            params.add(value);
            sql.append(" AND p.RESOURCEID = a.RESOURCEID");
            sql.append(")");
         }
      }

      log.debug("SQL: " + sql);
      EntityManager em = Context.internalRequestScope().getEntityManager();
      Query q = em.createNativeQuery(sql.toString(), Controllable.class);
      for (int i = 0; i < params.size(); i++) {
         q.setParameter(i + 1, params.get(i));
      }

      List<Controllable> list = q.getResultList();

      decrypt(list);
      return list;
   }

   /**
    * loads all Controllables with the given groupId. Tenant is not considered.
    * 
    * @param groupId
    * @return
    */
   public static List<Controllable> loadAllByGroupId(String groupId) {
      TypedQuery<Controllable> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Controllable.SEL_BY_GROUPID, Controllable.class);
      query.setParameter("groupId", groupId);
      List<Controllable> list = query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * decrypts the Controllable and Resource data if the Controllable is encrypted.
    * 
    * @param list
    */
   public static void decrypt(List<Controllable> list) {
      for (Controllable dc : list) {
         dc.decrypt();
      }
   }

   /**
    * compares a scheduled modified resource object or a modified resource object under dual control with the unmodified
    * original object. Only applicable for JPA resources and ControlEvent UPDATE.
    * 
    * @param dc
    *           a Controllable representing either a scheduled or a dual control event
    * @return
    */
   public static List<Difference> differences(Controllable dc) {
      if (dc == null || dc.getResource() == null) {
         throw new IllegalArgumentException("Parameter Controllable (or dc.getResource()) must not be null");
      }
      Resource res = dc.getResource();
      if (dc.getControlEvent() != ControlEvent.UPDATE || !(res instanceof JpaResource)) {
         log.info("Differences can only be analyzed for UDATE events of JPA entites");
         return Collections.emptyList();
      }

      ResourceParameter rp = dc.getResource().getParameter(FourEyesActuator.CLEANOBJECT);
      if (rp != null) {
         // scheduled Dc
         return CibetUtil.compare(dc.getResource().getUnencodedTargetObject(), rp.getUnencodedValue());
      }

      rp = dc.getResource().getParameter(FourEyesActuator.DIFFERENCES);
      if (rp != null) {
         // dual control Dc
         return (List<Difference>) rp.getUnencodedValue();
      }

      String err = "Controllable [" + dc + "] is in wrong state: Failed to find neither CLEANOBJECT nor DIFFERENCES ["
            + res + "]";
      log.error(err);
      throw new IllegalStateException(err);

   }

}
