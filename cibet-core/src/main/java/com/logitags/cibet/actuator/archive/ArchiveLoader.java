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
/**
 * 
 */
package com.logitags.cibet.actuator.archive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.sensor.jpa.JpaResource;

/**
 * API for the cibet component
 */
@CibetContext
public abstract class ArchiveLoader {

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(ArchiveLoader.class);

   /**
    * loads all archives of the current tenant.
    * 
    * @return list of Archives
    */
   public static List<Archive> loadArchives() {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_TENANT);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all Archives of all tenants
    * 
    * @return list of Archives
    */
   public static List<Archive> loadAllArchives() {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL);
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all Archive records for the given target type of the current tenant. Target type could be e.g. the name of a
    * class or a table name.
    * 
    * @param target
    *           target
    * @return list of Archives
    */
   public static List<Archive> loadArchives(String target) {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_CLASS);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("targetType", target);
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all Archive records for the given target type. Tenant is not taken into account. Target type could be e.g.
    * the name of a class or a table name.
    * 
    * @param target
    *           target
    * @return list of Archives
    */
   public static List<Archive> loadAllArchives(String target) {
      Query query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Archive.SEL_ALL_BY_CLASS_NO_TENANT);
      query.setParameter("targetType", target);
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all archives that belong to the same case id for the current tenant.
    * 
    * @param caseId
    *           incident id
    * @return list of Archives
    */
   public static List<Archive> loadArchivesByCaseId(String caseId) {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_ALL_BY_CASEID);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("caseId", caseId);
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all archives that belong to the same case id, regardless of tenant.
    * 
    * @param caseId
    *           incident id
    * @return list of Archives
    */
   public static List<Archive> loadAllArchivesByCaseId(String caseId) {
      Query query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Archive.SEL_ALL_BY_CASEID_NO_TENANT);
      query.setParameter("caseId", caseId);
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all archives that reference a method call in the given class for the current tenant.
    * 
    * @param objectClass
    *           target type, class name
    * @param methodName
    *           name of method
    * @return list of Archives
    */
   public static List<Archive> loadArchivesByMethodName(Class<?> objectClass, String methodName) {
      if (objectClass == null) {
         String msg = "parameter objectClass may not be null! "
               + "Call method loadArchivesByMethodName() with a Class object";
         log.error(msg);
         throw new RuntimeException(msg);
      }
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_BY_METHODNAME);
      query.setParameter("tenant", Context.internalSessionScope().getTenant());
      query.setParameter("objectClass", objectClass.getName());
      query.setParameter("methodName", methodName);
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads all archives that reference a method call in the given class. Tenant is not taken into account.
    * 
    * @param objectClass
    *           target type, class name
    * @param methodName
    *           name of method
    * @return list of Archives
    */
   public static List<Archive> loadAllArchivesByMethodName(Class<?> objectClass, String methodName) {
      if (objectClass == null) {
         String msg = "parameter objectClass may not be null! "
               + "Call method loadArchivesByMethodName() with a Class object";
         log.error(msg);
         throw new RuntimeException(msg);
      }
      Query query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Archive.SEL_BY_METHODNAME_NO_TENANT);
      query.setParameter("objectClass", objectClass.getName());
      query.setParameter("methodName", methodName);
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * load all archives with a resource of type JpaResource that have the given object ID and target type. Tenant is not
    * taken into account.
    * 
    * @param target
    *           target
    * @param primaryKeyId
    *           primary key
    * @return list of Archives
    */
   public static List<Archive> loadArchivesByPrimaryKeyId(String target, Object primaryKeyId) {
      Query query = Context.internalRequestScope().getEntityManager().createNamedQuery(Archive.SEL_BY_PRIMARYKEYID);
      query.setParameter("targetType", target);
      if (primaryKeyId == null) {
         query.setParameter("primaryKeyId", null);
      } else {
         query.setParameter("primaryKeyId", primaryKeyId.toString());
      }
      List<Archive> list = (List<Archive>) query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * loads archives with the given groupId. Tenant is not taken into account.
    * 
    * @param groupId
    *           groupId
    * @return
    */
   public static List<Archive> loadAllArchivesByGroupId(String groupId) {
      TypedQuery<Archive> query = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(Archive.SEL_BY_GROUPID, Archive.class);
      query.setParameter("groupId", groupId);
      List<Archive> list = query.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * analyzes the given Archive list for object differences of JPA entity resources. If the Archive is not the archive
    * of a JPA entity, the map entry for that archive contains the empty list. The map entry for Archives not in state
    * EXECUTED contains the empty list.
    * <p>
    * The result map contains the found Archive objects as keys and a list of Difference objects as values which
    * represents the differences between the archived state of the JPA entity and the state of the precedent archived
    * state.
    * <p>
    * If archives JPA1, JPA2, Other3 and JPA4 have been found, the map will contain the following pairs:
    * <p>
    * JPA1 | empty List<br>
    * JPA2 | Dif List(JPA2 - JPA1)<br>
    * Other3 | empty List<br>
    * JPA4 | Dif List(JPA4 - JPA2)
    * <p>
    * The List of Differences could be empty if the archived event is not an UPDATE.
    * 
    * @param archives
    *           List of archives
    * @return Map of Archives with Difference
    */
   public static Map<Archive, List<Difference>> analyzeDifferences(List<Archive> archives) {
      if (archives.isEmpty()) {
         return Collections.emptyMap();
      }

      ArchiveComparator comp = new ArchiveComparator();
      Set<Archive> orderedList = new TreeSet<>(comp);
      orderedList.addAll(archives);
      Archive[] array = orderedList.toArray(new Archive[0]);

      Map<Archive, List<Difference>> map = new TreeMap<Archive, List<Difference>>(comp);
      map.put(array[0], Collections.<Difference> emptyList());
      for (int i = 1; i < array.length; i++) {
         if (!(array[i].getResource() instanceof JpaResource)) {
            // only analyze differences of JPA entities
            map.put(array[i], Collections.<Difference> emptyList());
            continue;
         }
         int li = i - 1;
         Archive lastArchive = array[li];
         while ((!(lastArchive.getResource() instanceof JpaResource)
               || lastArchive.getExecutionStatus() != ExecutionStatus.EXECUTED) && li > 0) {
            li = li - 1;
            lastArchive = array[li];
         }

         if (!(lastArchive.getResource() instanceof JpaResource)
               || lastArchive.getExecutionStatus() != ExecutionStatus.EXECUTED) {
            map.put(array[i], Collections.<Difference> emptyList());
            continue;
         }

         List<Difference> difList = CibetUtil.compare(array[i].getResource(), lastArchive.getResource());
         log.debug("put " + difList.size() + " differences into archive " + array[i].getArchiveId());
         map.put(array[i], difList);
      }

      return map;
   }

   /**
    * load all archives for the current tenant where target type is equal to the entity class and resource parameters
    * exist with name equal to the key of the map and stringValue equal to the map's value.
    * 
    * @param entityClass
    *           target type
    * @param properties
    *           map of property name and value as search parameters for ResourceParameter.
    * @return list of Archives
    */
   public static List<Archive> loadArchivesByProperties(Class<?> entityClass, Map<String, Object> properties) {
      if (entityClass == null) {
         String msg = "parameter entityClass may not be null! "
               + "Call method loadArchivesByProperties() with a Class object";
         log.error(msg);
         throw new RuntimeException(msg);
      }

      List<Object> params = new ArrayList<>();
      params.add(Context.sessionScope().getTenant());
      params.add(entityClass.getName());
      StringBuffer sql = new StringBuffer();
      sql.append(
            "SELECT a.* FROM CIB_ARCHIVE a, CIB_RESOURCE r WHERE a.RESOURCEID = r.RESOURCEID and a.TENANT = ? AND r.TARGET = ?");

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
      Query q = em.createNativeQuery(sql.toString(), Archive.class);
      for (int i = 0; i < params.size(); i++) {
         q.setParameter(i + 1, params.get(i));
      }

      List<Archive> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * load all archives where target type is equal to the entity class and resource parameters exist with name equal to
    * the key of the map and stringValue equal to the map's value. Tenant is not taken into account.
    * 
    * @param entityClass
    *           target type
    * @param properties
    *           map of property name and value as search parameters for ResourceParameter.
    * @return list of Archives
    */
   public static List<Archive> loadAllArchivesByProperties(Class<?> entityClass, Map<String, Object> properties) {
      if (entityClass == null) {
         String msg = "parameter entityClass may not be null! "
               + "Call method loadArchivesByProperties() with a Class object";
         log.error(msg);
         throw new RuntimeException(msg);
      }

      List<Object> params = new ArrayList<>();
      params.add(entityClass.getName());
      StringBuffer sql = new StringBuffer();
      sql.append("SELECT a.* FROM CIB_ARCHIVE a WHERE a.TARGET = ?");

      if (properties != null) {
         for (Entry<String, Object> entry : properties.entrySet()) {
            String value = entry.getValue() == null ? null : entry.getValue().toString();

            sql.append(" AND EXISTS (SELECT 1 FROM CIB_RESOURCEPARAMETER p WHERE p.NAME = ?");
            params.add(entry.getKey());
            sql.append(" AND p.STRINGVALUE = ?");
            params.add(value);
            sql.append(" AND p.ARCHIVEID = a.ARCHIVEID");
            sql.append(")");
         }
      }

      log.debug("SQL: " + sql);
      EntityManager em = Context.internalRequestScope().getEntityManager();
      Query q = em.createNativeQuery(sql.toString(), Archive.class);
      for (int i = 0; i < params.size(); i++) {
         q.setParameter(i + 1, params.get(i));
      }

      List<Archive> list = q.getResultList();
      decrypt(list);
      return list;
   }

   /**
    * decrypts the Archive and Resource data if the Archive is encrypted.
    * 
    * @param list
    */
   public static void decrypt(List<Archive> list) {
      for (Archive arch : list) {
         arch.decrypt();
      }
   }

   /**
    * checks the integrity of all Archive entries in the database
    * 
    * @return a list of Archive objects where the checksum is wrong
    */
   public static List<Archive> checkIntegrity() {
      List<Archive> result = new ArrayList<Archive>();

      EntityManager em = Context.internalRequestScope().getEntityManager();
      TypedQuery<Archive> q = em.createNamedQuery(Archive.SEL_ALL, Archive.class);
      List<Archive> list = q.getResultList();
      for (Archive ar : list) {
         if (ar.checkChecksum() == false) {
            result.add(ar);
         }
      }
      return result;
   }

}
