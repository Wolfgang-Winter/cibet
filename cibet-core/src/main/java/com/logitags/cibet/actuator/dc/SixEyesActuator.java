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

import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.notification.NotificationProvider;

/**
 * applies six-eyes control.
 */
public class SixEyesActuator extends FourEyesActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 8597319392966217825L;

   private transient Log log = LogFactory.getLog(SixEyesActuator.class);

   public static final String DEFAULTNAME = "SIX_EYES";

   public SixEyesActuator() {
      setName(DEFAULTNAME);
   }

   public SixEyesActuator(String name) {
      setName(name);
   }

   @Override
   protected Controllable createControlledObject(ControlEvent event, EventMetadata ctx) {
      Controllable co = super.createControlledObject(event, ctx);
      co.setExecutionStatus(ExecutionStatus.FIRST_POSTPONED);
      return co;
   }

   /**
    * set actuator of the ControlledObject to SIX_EYES.
    * 
    * @param dc
    *           Controllable object
    */
   @Override
   protected void setActuatorSpecificProperties(Controllable dc) {
      dc.setFirstApprovalUser(Context.internalSessionScope().getApprovalUser());
      dc.setFirstApprovalAddress(Context.internalSessionScope().getApprovalAddress());
   }

   @Override
   protected void notifyAssigned(ExecutionStatus status, Controllable dc) {
      if (!isSendAssignNotification()) return;
      if (status == ExecutionStatus.FIRST_POSTPONED && dc.getFirstApprovalAddress() != null) {
         NotificationProvider notifProvider = Configuration.instance().getNotificationProvider();
         if (notifProvider != null) {
            notifProvider.notify(ExecutionStatus.FIRST_POSTPONED, dc);
         }
      } else if (status == ExecutionStatus.FIRST_RELEASED && dc.getReleaseAddress() != null) {
         NotificationProvider notifProvider = Configuration.instance().getNotificationProvider();
         if (notifProvider != null) {
            notifProvider.notify(ExecutionStatus.POSTPONED, dc);
         }
      }
   }

   @Override
   protected ExecutionStatus getPostponedExecutionStatus() {
      return ExecutionStatus.FIRST_POSTPONED;
   }

   private ControlEvent controlEventForFirstRelease(Controllable co) {
      switch (co.getControlEvent()) {
      case INVOKE:
         return ControlEvent.FIRST_RELEASE_INVOKE;
      case DELETE:
         return ControlEvent.FIRST_RELEASE_DELETE;
      case INSERT:
         return ControlEvent.FIRST_RELEASE_INSERT;
      case UPDATE:
         return ControlEvent.FIRST_RELEASE_UPDATE;
      case SELECT:
         return ControlEvent.FIRST_RELEASE_SELECT;
      case UPDATEQUERY:
         return ControlEvent.FIRST_RELEASE_UPDATEQUERY;
      default:
         String msg = "Controlled object [" + co.getControllableId() + "] with control event " + co.getControlEvent()
               + " cannot be first released";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
   }

   @Override
   protected void checkApprovalUserId(Controllable co) throws InvalidUserException {
      String approvalUserId = Context.internalSessionScope().getUser();
      if (approvalUserId == null) {
         String msg = "Release without user id not possible. No user set in CibetContext!";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      if (approvalUserId.equals(co.getCreateUser())) {
         String msg = "release failed: user id of releasing user is equal to the " + "user id of the initial user";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      if (co.getExecutionStatus() == ExecutionStatus.FIRST_POSTPONED) {
         // first release
         if (co.getFirstApprovalUser() != null && !co.getFirstApprovalUser().equals(approvalUserId)) {
            String msg = "release failed: Only user" + co.getFirstApprovalUser()
                  + " is allowed to make first release/reject of Controllable with ID " + co.getControllableId();
            log.error(msg);
            throw new InvalidUserException(msg);
         }

      } else {
         // final release
         if (approvalUserId.equals(co.getFirstApprovalUser())) {
            String msg = "user id of second releasing user is equal to the " + " user id of the first releasing user";
            log.error(msg);
            throw new InvalidUserException(msg);
         }

         if (co.getReleaseUser() != null && !co.getReleaseUser().equals(approvalUserId)) {
            String msg = "release failed: Only user" + co.getReleaseUser()
                  + " is allowed to release/reject Controllable with ID " + co.getControllableId();
            log.error(msg);
            throw new InvalidUserException(msg);
         }
      }
   }

   /**
    * checks if the user id who rejects is allowed to reject.
    * 
    * @param co
    *           Controllable object
    * @throws InvalidUserException
    *            if user has no permission
    */
   @Override
   protected void checkRejectUserId(Controllable co) throws InvalidUserException {
      String rejectUserId = Context.internalSessionScope().getUser();
      if (rejectUserId == null) {
         String msg = "Reject/pass back without user id not possible. No user set in CibetContext!";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      // initiating user can always reject/pass back
      if (rejectUserId.equals(co.getCreateUser())) {
         return;
      }

      if (co.getExecutionStatus() == ExecutionStatus.FIRST_POSTPONED) {
         // first release
         if (co.getFirstApprovalUser() != null && !co.getFirstApprovalUser().equals(rejectUserId)) {
            String msg = "reject/pass back failed: Only user" + co.getFirstApprovalUser()
                  + " is allowed to reject/pass back Controllable with ID " + co.getControllableId();
            log.error(msg);
            throw new InvalidUserException(msg);
         }

      } else {
         // final release
         if (co.getReleaseUser() != null && !co.getReleaseUser().equals(rejectUserId)) {
            String msg = "reject/pass back failed: Only user" + co.getReleaseUser()
                  + " is allowed to reject/pass back Controllable with ID " + co.getControllableId();
            log.error(msg);
            throw new InvalidUserException(msg);
         }
      }
   }

   @Override
   protected void checkExecutionStatus(String action, Controllable co) throws ResourceApplyException {
      if (co.getExecutionStatus() != ExecutionStatus.FIRST_POSTPONED
            && co.getExecutionStatus() != ExecutionStatus.FIRST_RELEASED) {
         String err = "Failed to " + action + " Controllable with ID " + co.getControllableId()
               + ": should be in status FIRST_POSTPONED or FIRST_RELEASED but is in status " + co.getExecutionStatus();
         log.warn(err);
         throw new ResourceApplyException(err);
      }
   }

   protected void checkRejectExecutionStatus(Controllable co) throws ResourceApplyException {
      if (co.getExecutionStatus() != ExecutionStatus.FIRST_POSTPONED
            && co.getExecutionStatus() != ExecutionStatus.FIRST_RELEASED
            && co.getExecutionStatus() != ExecutionStatus.PASSEDBACK) {
         String err = "Failed to pass back Controllable with ID " + co.getControllableId()
               + ": should be in status FIRST_POSTPONED, FIRST_RELEASED or PASSEDBACK but is in status "
               + co.getExecutionStatus();
         log.warn(err);
         throw new ResourceApplyException(err);
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.dc.FourEyesActuator#release(com.logitags.cibet .actuator.common.Controllable,
    * java.lang.String)
    */
   @Override
   public Object release(Controllable co, String remark) throws ResourceApplyException {
      checkExecutionStatus("release", co);

      ControlEvent originalControlEvent = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      String originalCaseId = Context.internalRequestScope().getCaseId();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         ControlEvent thisEvent = null;
         if (co.getExecutionStatus() == ExecutionStatus.FIRST_POSTPONED) {
            thisEvent = controlEventForFirstRelease(co);
         } else if (co.getExecutionStatus() == ExecutionStatus.FIRST_RELEASED) {
            thisEvent = controlEventForRelease(co);
         }

         log.debug("release event: " + thisEvent);
         checkApprovalUserId(co);

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, thisEvent);
         Context.internalRequestScope().setCaseId(co.getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLLABLE, co);

         Object result = co.getResource().apply(co.getControlEvent());

         if (!Context.requestScope().isPlaying()) {
            EventResult eventResult = Context.internalRequestScope().getExecutedEventResult();
            if (eventResult == null) {
               eventResult = new EventResult();
               eventResult.setExecutionStatus(ExecutionStatus.EXECUTED);
               Context.internalRequestScope().registerEventResult(eventResult);
            }
            ExecutionStatus status = eventResult.getExecutionStatus();
            if (status != ExecutionStatus.DENIED) {
               if (co.getExecutionStatus() == ExecutionStatus.FIRST_POSTPONED) {
                  // first release
                  co.setExecutionStatus(ExecutionStatus.FIRST_RELEASED);
                  co.setFirstApprovalDate(new Date());
                  co.setFirstApprovalUser(Context.internalSessionScope().getUser());
                  co.setFirstApprovalRemark(Context.internalRequestScope().getRemark());
                  co.setReleaseUser(Context.internalSessionScope().getApprovalUser());
                  co.setReleaseAddress(Context.internalSessionScope().getApprovalAddress());

                  if (isSendReleaseNotification()) {
                     log.debug("co.getCreateAddress(): " + co.getCreateAddress());
                     notifyApproval(co);
                  }
                  super.notifyAssigned(ExecutionStatus.POSTPONED, co);
               } else {
                  // second release
                  if (status == ExecutionStatus.EXECUTED) {
                     co.setExecutionStatus(ExecutionStatus.EXECUTED);
                  }
                  co.setReleaseDate(new Date());
                  co.setReleaseUser(Context.internalSessionScope().getUser());
                  co.setReleaseRemark(Context.internalRequestScope().getRemark());
                  if (isSendReleaseNotification()) {
                     notifyApproval(co);
                  }
               }

               // if (isEncrypt()) {
               // co.encrypt();
               // }
               Context.internalRequestScope().getEntityManager().merge(co);
            }
         }

         log.debug("end SixEyesActuator.release");
         return result;
      } finally {
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
         Context.internalRequestScope().setCaseId(originalCaseId);
         Context.internalRequestScope().setRemark(originalRemark);
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLLABLE);
      }
   }
}
