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
package com.logitags.cibet.actuator.scheduler;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.Query;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.dc.DcControllable;
import com.logitags.cibet.actuator.dc.DcControllableComparator;
import com.logitags.cibet.actuator.dc.DcLoader;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.AnnotationUtil;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.jpa.JpaResource;

/**
 * API for the cibet dual control management
 */
@CibetContext
public abstract class SchedulerLoader extends DcLoader {

   private static Log log = LogFactory.getLog(SchedulerLoader.class);

   /**
    * convenient method that returns all scheduled Controllables for the given tenant and target type.
    * 
    * @param targetType
    * @return
    */
   public static List<DcControllable> findScheduled(String targetType) {
      Query q = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_SCHED_BY_TARGETTYPE);
      q.setParameter("tenant", Context.internalSessionScope().getTenant());
      q.setParameter("oclass", targetType);
      List<DcControllable> list = q.getResultList();
      for (DcControllable dc : list) {
         dc.decrypt();
      }

      return list;
   }

   /**
    * convenient method that returns all scheduled Controllables for all tenants and the given target type.
    * 
    * @param targetType
    * @return
    */
   public static List<DcControllable> findAllScheduled(String targetType) {
      Query q = Context.internalRequestScope().getEntityManager()
            .createNamedQuery(DcControllable.SEL_SCHED_BY_TARGETTYPE_NO_TENANT);
      q.setParameter("oclass", targetType);
      List<DcControllable> list = q.getResultList();
      for (DcControllable dc : list) {
         dc.decrypt();
      }

      return list;
   }

   /**
    * convenient method that returns all scheduled Controllables for the given tenant.
    * 
    * @return List of DcControllable
    */
   public static List<DcControllable> findScheduled() {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_SCHED_BY_TENANT);
      q.setParameter("tenant", Context.internalSessionScope().getTenant());
      List<DcControllable> list = q.getResultList();
      for (DcControllable dc : list) {
         dc.decrypt();
      }

      return list;
   }

   /**
    * convenient method that returns all scheduled Controllables of all tenants.
    * 
    * @return List of DcControllable
    */
   public static List<DcControllable> findAllScheduled() {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_SCHED);
      List<DcControllable> list = q.getResultList();
      for (DcControllable dc : list) {
         dc.decrypt();
      }

      return list;
   }

   /**
    * returns the modifications that have been executed at the scheduled time on an entity for an UPDATE control event.
    * If the DcControllable does not represent a business case JPA update of an entity, the returned list is empty.<br>
    * The differences represent the actual modifications that have been executed on the current state of the object, not
    * the planned modifications at scheduled time. Between scheduled time and execution time the entity may have
    * undergone other state changes that are taken into account.
    * 
    * @param dc
    *           DcControllable
    * @return the list of differences
    */
   public static List<Difference> executedDifferences(DcControllable dc) {
      if (dc.getExecutionStatus() != ExecutionStatus.EXECUTED || dc.getControlEvent() != ControlEvent.UPDATE
            || !(dc.getResource() instanceof JpaResource)) {
         return Collections.emptyList();
      }

      ResourceParameter origRP = dc.getResource().getParameter(SchedulerActuator.ORIGINAL_OBJECT);
      if (origRP == null) {
         String err = "Failed to find executed differences: DcControllable does not contain data of the original object state";
         log.error(err);
         throw new RuntimeException(err);
      }
      Object original = CibetUtil.decode(origRP.getEncodedValue());
      Object modified = dc.getResource().getResultObject();
      List<Difference> diffs = CibetUtil.compare(modified, original);
      return diffs;
   }

   /**
    * check if there exist scheduled events for a JPA entity. The method returns a map where the key is a scheduled
    * DcControllable object. If the event is other than UPDATE, the map value is null. For UPDATE events, the map value
    * contains a list of the scheduled updates.
    * 
    * @param entity
    * @return
    */
   public static Map<DcControllable, List<Difference>> scheduledDifferences(Serializable entity) {
      String primaryKey = AnnotationUtil.primaryKeyAsString(entity);
      return scheduledDifferences(entity.getClass(), primaryKey);
   }

   /**
    * check if there exist scheduled events for a JPA entity of type entityClass and the given primary key. The method
    * returns a map where the key is a scheduled DcControllable object. If the event is other than UPDATE, the map value
    * is null. For UPDATE events, the map value contains a list of the scheduled updates.
    * 
    * @param entityClass
    *           entity
    * @param primaryKey
    *           primary key
    * @return
    */
   public static Map<DcControllable, List<Difference>> scheduledDifferences(Class<?> entityClass, Object primaryKey) {
      Map<DcControllable, List<Difference>> map = new TreeMap<DcControllable, List<Difference>>(
            new DcControllableComparator());
      String uniqueId = DigestUtils.sha256Hex(entityClass.getName() + primaryKey);
      log.debug("uniqueId: " + uniqueId);

      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_UNIQUEID);
      q.setParameter("uniqueId", uniqueId);
      List<DcControllable> list = (List<DcControllable>) q.getResultList();
      for (DcControllable dc : list) {
         if (dc.getExecutionStatus() != ExecutionStatus.SCHEDULED)
            continue;
         if (dc.getControlEvent() == ControlEvent.UPDATE) {

            ResourceParameter rp = dc.getResource().getParameter(SchedulerActuator.CLEANOBJECT);
            if (rp == null) {
               String err = "Failed to find base entity of " + entityClass.getName() + " for resource "
                     + dc.getResource();
               log.error(err);
               throw new RuntimeException(err);
            }
            List<Difference> difList = CibetUtil.compare(dc.getResource().getObject(), rp.getUnencodedValue());
            map.put(dc, difList);
         } else {
            map.put(dc, null);
         }
      }

      return map;
   }

}
