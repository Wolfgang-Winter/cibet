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
package com.logitags.cibet.actuator.dc;

import javax.persistence.EntityManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.CibetContext;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.ExecutionStatus;

/**
 * API for the cibet dual control management
 */
@CibetContext
public abstract class DcReleaser {

   /**
    * logger for tracing
    */
   private static Log log = LogFactory.getLog(DcReleaser.class);

   /**
    * releases an event on a JPA resource. If it is the second release of a 6-eyes process the controlled object entry
    * is updated. If it is a 4-eyes process the real object is updated.
    * 
    * @param entityManager
    *           EntityManager for performing the release. Could be null in case of a INVOKE event. If JDBC, use new
    *           JdbcBridgeEntityManager(connection)
    * @param co
    *           DcControllable to release
    * @param remark
    *           comment
    * @throws ResourceApplyException
    *            if the release action fails.
    */
   public static synchronized Object release(EntityManager entityManager, DcControllable co, String remark)
         throws ResourceApplyException {
      log.debug("start DefaultDcService.release");
      if (co == null) {
         String msg = "No controlled object to release";
         log.warn(msg);
         throw new IllegalArgumentException(msg);
      }

      if (entityManager != null) {
         Context.internalRequestScope().setApplicationEntityManager(entityManager);
      }
      if (co.getExecutionStatus() != ExecutionStatus.SCHEDULED) {
         Context.internalRequestScope().setScheduledDate(co.getScheduledDate());
      }
      DcActuator dc = (DcActuator) Configuration.instance().getActuator(co.getActuator());
      Object obj = dc.release(co, remark);
      return obj;
   }

   /**
    * release an event on a resource. Normally the resource involves no persistence here.
    * 
    * @param co
    *           DcControllable to release
    * @param remark
    *           comment
    * @return method return value or null
    * @throws ResourceApplyException
    *            in case of error
    */
   public static synchronized Object release(DcControllable co, String remark) throws ResourceApplyException {
      if (co != null && co.getControlEvent().isChildOf(ControlEvent.PERSIST)) {
         throw new IllegalArgumentException(
               "This release method is not usable for DcControllable with ControlEvent PERSIST. "
                     + "Use the release method which takes an EntityManager as parameter");
      }

      return release((EntityManager) null, co, remark);
   }

   /**
    * rejects a non-JPA controlled resource.
    * 
    * @param co
    *           DcControllable
    * @param remark
    *           comment
    * @throws ResourceApplyException
    *            in case of error
    */
   public static synchronized void reject(DcControllable co, String remark) throws ResourceApplyException {
      log.debug("start DefaultDcService.reject");
      if (co == null) {
         String msg = "No controlled object to reject";
         log.warn(msg);
         throw new IllegalArgumentException(msg);
      }

      if (co.getControlEvent().isChildOf(ControlEvent.PERSIST)) {
         throw new IllegalArgumentException(
               "This reject method is not usable for DcControllable with ControlEvent PERSIST. "
                     + "Use the reject method which takes an EntityManager as parameter");
      }
      reject(null, co, remark);
   }

   /**
    * rejects a controlled JPA entity. Rejects a postponed or scheduled DcControllable object if it is not yet released
    * or executed by the scheduler batch process. This reject method must be called when the postponed or scheduled
    * event is a PERSISTENCE event on an entity.
    */
   public static synchronized void reject(EntityManager entityManager, DcControllable co, String remark)
         throws ResourceApplyException {
      log.debug("start DefaultDcService.reject");
      if (co == null) {
         String msg = "No controlled object to reject";
         log.warn(msg);
         throw new IllegalArgumentException(msg);
      }

      if (entityManager != null) {
         Context.internalRequestScope().setApplicationEntityManager(entityManager);
      }

      DcActuator dc = (DcActuator) Configuration.instance().getActuator(co.getActuator());
      dc.reject(co, remark);
   }

   /**
    * returns the given dual control event back to the event producer. The event producer must correct the Controllable
    * or resource data before it can be released. The difference to reject() is that after rejection the controlled
    * resource is open for all users while when passed back to the event producing user, only this user can work on the
    * Controllable and the controlled resource.
    * 
    * @param co
    *           DcControllable to pass back
    * @param remark
    *           message from the passing back user to the passed back user
    * @throws ResourceApplyException
    *            in case of error
    */
   public static synchronized void passBack(DcControllable co, String remark) throws ResourceApplyException {
      if (co == null) {
         String msg = "No controlled object to pass back";
         log.warn(msg);
         throw new IllegalArgumentException(msg);
      }

      if (co.getControlEvent().isChildOf(ControlEvent.PERSIST)) {
         throw new IllegalArgumentException(
               "This passBack method is not usable for DcControllable with ControlEvent PERSIST. "
                     + "Use the passBack method which takes an EntityManager as parameter");
      }

      passBack(null, co, remark);
   }

   /**
    * returns the given dual control event back to the event producer. The event producer must correct the Controllable
    * or resource data before it can be released. The difference to reject() is that after rejection the controlled
    * resource is open for all users while when passed back to the event producing user, only this user can work on the
    * Controllable and the controlled resource.
    * 
    * @param entityManager
    *           the EntityManager used to persist the controlled entity
    * @param co
    *           DcControllable to pass back
    * @param remark
    *           message from the passing back user to the passed back user
    * @throws ResourceApplyException
    *            in case of error
    */
   public static synchronized void passBack(EntityManager entityManager, DcControllable co, String remark)
         throws ResourceApplyException {
      if (co == null) {
         String msg = "No controlled object to pass back";
         log.warn(msg);
         throw new IllegalArgumentException(msg);
      }

      if (entityManager != null) {
         Context.internalRequestScope().setApplicationEntityManager(entityManager);
      }

      DcActuator dc = (DcActuator) Configuration.instance().getActuator(co.getActuator());
      dc.passBack(co, remark);
   }

   /**
    * submits a DcControllable for release. Used by the user who created the DcControllable and to whom it is passed
    * back (see {@link #passBack(DcControllable, String)}). The ExecutionStatus will be set to POSTPONED or
    * FIRST_POSTPONED. Notifications will be sent if configured.
    * 
    * @param co
    *           DcControllable to submit
    * @param remark
    *           comment
    * @throws ResourceApplyException
    *            in case of error
    */
   public static synchronized void submit(DcControllable co, String remark) throws ResourceApplyException {
      if (co == null) {
         String msg = "No controlled object to submit";
         log.warn(msg);
         throw new IllegalArgumentException(msg);
      }
      if (co.getControlEvent().isChildOf(ControlEvent.PERSIST)) {
         throw new IllegalArgumentException(
               "This submit method is not usable for DcControllable with ControlEvent PERSIST. "
                     + "Use the submit method which takes an EntityManager as parameter");
      }

      submit(null, co, remark);
   }

   /**
    * submits a DcControllable for release. Used by the user who created the DcControllable and to whom it is passed
    * back (see {@link #passBack(DcControllable, String)}). The ExecutionStatus will be set to POSTPONED or
    * FIRST_POSTPONED. Notifications will be sent if configured.
    * 
    * @param entityManager
    *           the EntityManager used to persist the controlled entity
    * @param co
    *           DcControllable to submit
    * @param remark
    *           comment
    * @throws ResourceApplyException
    *            if an error occurs
    */
   public static synchronized void submit(EntityManager entityManager, DcControllable co, String remark)
         throws ResourceApplyException {
      if (co == null) {
         String msg = "No controlled object to submit";
         log.warn(msg);
         throw new IllegalArgumentException(msg);
      }

      if (entityManager != null) {
         Context.internalRequestScope().setApplicationEntityManager(entityManager);
      }

      DcActuator dc = (DcActuator) Configuration.instance().getActuator(co.getActuator());
      dc.submit(co, remark);
   }

}
