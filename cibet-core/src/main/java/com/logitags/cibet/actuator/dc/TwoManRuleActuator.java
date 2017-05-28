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

import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.context.InternalSessionScope;
import com.logitags.cibet.core.CibetUtil;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.PersistenceUtil;
import com.logitags.cibet.resource.ResourceParameter;

/**
 * applies four-eyes control.
 * 
 */
public class TwoManRuleActuator extends FourEyesActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -3521038139055197044L;
   private transient Log log = LogFactory.getLog(TwoManRuleActuator.class);

   public static final String DEFAULTNAME = "TWO_MAN_RULE";

   private boolean removeSecondUserAfterRelease = false;

   public TwoManRuleActuator() {
      setName(DEFAULTNAME);
   }

   public TwoManRuleActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#beforeEvent(com.logitags.cibet.core. EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      log.debug("TwoManRuleActuator.beforeEvent");
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip TwoManRuleActuator.beforeEvent");
         return;
      }

      switch (ctx.getControlEvent()) {
      case FIRST_RELEASE_INVOKE:
      case FIRST_RELEASE_DELETE:
      case FIRST_RELEASE_INSERT:
      case FIRST_RELEASE_UPDATE:
      case FIRST_RELEASE_SELECT:
         ctx.setExecutionStatus(ExecutionStatus.POSTPONED);
         break;

      case REJECT_DELETE:
      case REJECT_INSERT:
      case REJECT_UPDATE:
      case REJECT_INVOKE:
      case REJECT_SELECT:
         ctx.setExecutionStatus(ExecutionStatus.REJECTED);
         break;

      case PASSBACK_DELETE:
      case PASSBACK_INSERT:
      case PASSBACK_INVOKE:
      case PASSBACK_SELECT:
      case PASSBACK_UPDATE:
         ctx.setExecutionStatus(ExecutionStatus.PASSEDBACK);
         break;

      case UPDATE:
         if (isLoadEager()) {
            loadEager(ctx);
         }

      case DELETE:
      case RESTORE_UPDATE:
      case INVOKE:
      case REDO:
         checkUnapprovedResource(ctx);
         // no break, fall through
      case INSERT:
      case RESTORE_INSERT:
      case SELECT:
      case SUBMIT_DELETE:
      case SUBMIT_INSERT:
      case SUBMIT_INVOKE:
      case SUBMIT_SELECT:
      case SUBMIT_UPDATE:

         if (Context.internalSessionScope().getSecondUser() != null) {
            if (Context.internalSessionScope().getSecondUser().equals(Context.internalSessionScope().getUser())) {
               String msg = "release failed: user id of releasing user is equal to the "
                     + "user id of the initial user";
               throw new RuntimeException(msg, new InvalidUserException(msg));
            }
         }

         ctx.setExecutionStatus(ExecutionStatus.POSTPONED);
         if (isThrowPostponedException()) {
            try {
               ctx.setException(postponedExceptionType.newInstance());
            } catch (InstantiationException e) {
               throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            }
         }

         break;

      default:
         break;
      }

      findUserId();
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#afterEvent(com.logitags.cibet.core. EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      log.debug("TwoManRuleActuator.afterEvent");
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip TwoManRuleActuator.afterEvent");
         return;
      }

      if (ctx.getExecutionStatus() == ExecutionStatus.ERROR) {
         log.info("ERROR detected. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      DcControllable dcObj = null;

      switch (ctx.getControlEvent()) {
      case RELEASE_DELETE:
      case RELEASE_UPDATE:
      case RELEASE_INSERT:
      case RELEASE_INVOKE:
      case RELEASE_SELECT:
      case FIRST_RELEASE_INVOKE:
      case FIRST_RELEASE_DELETE:
      case FIRST_RELEASE_INSERT:
      case FIRST_RELEASE_UPDATE:
      case FIRST_RELEASE_SELECT:
      case REJECT_DELETE:
      case REJECT_INSERT:
      case REJECT_UPDATE:
      case REJECT_INVOKE:
      case REJECT_SELECT:
      case PASSBACK_DELETE:
      case PASSBACK_INSERT:
      case PASSBACK_INVOKE:
      case PASSBACK_SELECT:
      case PASSBACK_UPDATE:
      case SUBMIT_DELETE:
      case SUBMIT_INSERT:
      case SUBMIT_INVOKE:
      case SUBMIT_SELECT:
      case SUBMIT_UPDATE:
         break;

      case UPDATE:
         dcObj = createControlledObject(ctx.getControlEvent(), ctx);

         List<Difference> diffs = PersistenceUtil.getDirtyUpdates(ctx);
         ResourceParameter propertyResParam = new ResourceParameter(DIFFERENCES, diffs.getClass().getName(), diffs,
               ParameterType.INTERNAL_PARAMETER, dcObj.getResource().getParameters().size() + 1);
         dcObj.getResource().addParameter(propertyResParam);
         break;

      case DELETE:
      case SELECT:
      case INSERT:
         dcObj = createControlledObject(ctx.getControlEvent(), ctx);
         break;

      case RESTORE_INSERT:
         dcObj = createControlledObject(ControlEvent.INSERT, ctx);
         break;

      case INVOKE:
      case REDO:
         dcObj = createControlledObject(ControlEvent.INVOKE, ctx);
         break;

      case RESTORE_UPDATE:
         dcObj = createControlledObject(ControlEvent.UPDATE, ctx);
         break;

      default:
         String err = "Failed to execute afterEvent of " + this.getClass().getSimpleName() + ": "
               + ctx.getControlEvent() + " is an abstract ControlEvent";
         log.error(err);
         throw new RuntimeException(err);
      }

      if (dcObj != null) {
         if (Context.internalSessionScope().getSecondUser() != null) {
            try {
               Object result = release(dcObj, "Two-Man-Rule: direct release by second user");
               try {
                  CibetUtil.encode(result);
                  ctx.getResource().setResultObject(result);
               } catch (IOException e) {
                  log.info("Cannot set apply result object " + result.getClass().getName() + " into Resource: "
                        + e.getMessage());
               }

            } catch (InvalidUserException e) {
               log.warn(e.getMessage());
            } catch (ResourceApplyException e) {
               throw new RuntimeException(e.getMessage(), e);
            }
         }

         if (!Context.requestScope().isPlaying()) {
            if (dcObj.getResource().getResourceId() == null) {
               if (isEncrypt()) {
                  dcObj.getResource().encrypt();
               }
               Context.internalRequestScope().getEntityManager().persist(dcObj.getResource());
            }

            Context.internalRequestScope().getEntityManager().persist(dcObj);

            if (ctx.getException() != null && ctx.getException() instanceof PostponedException) {
               ((PostponedException) ctx.getException()).setDcControllable(dcObj);
            }

            notifyAssigned(ctx.getExecutionStatus(), dcObj);
         }
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.dc.FourEyesActuator#release(com.logitags.cibet .actuator.dc.DcControllable,
    * java.lang.String)
    */
   @Override
   public Object release(DcControllable co, String remark) throws ResourceApplyException {
      if (co.getExecutionStatus() != ExecutionStatus.POSTPONED) {
         String err = "Failed to release DcControllable with ID " + co.getDcControllableId()
               + ": should be in status POSTPONED but is in status " + co.getExecutionStatus();
         log.warn(err);
         throw new ResourceApplyException(err);
      }

      ControlEvent originalControlEvent = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      String originalCaseId = Context.internalRequestScope().getCaseId();
      String originalUser = Context.internalSessionScope().getUser();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         ControlEvent thisEvent = controlEventForRelease(co);
         log.debug("release event: " + thisEvent);
         checkApprovalUserId(co);

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, thisEvent);
         Context.internalRequestScope().setCaseId(co.getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }
         Context.internalSessionScope().setUser(Context.internalSessionScope().getSecondUser());
         Context.internalRequestScope().setProperty(InternalRequestScope.DCCONTROLLABLE, co);

         Object result = co.getResource().apply(co.getControlEvent());

         if (!Context.requestScope().isPlaying()) {
            EventResult eventResult = Context.internalRequestScope().getExecutedEventResult();
            if (eventResult == null) {
               eventResult = new EventResult();
               eventResult.setExecutionStatus(ExecutionStatus.EXECUTED);
               Context.internalRequestScope().registerEventResult(eventResult);
            }
            ExecutionStatus status = eventResult.getExecutionStatus();
            if (status == ExecutionStatus.EXECUTED || status == ExecutionStatus.SCHEDULED) {

               if (status == ExecutionStatus.EXECUTED) {
                  co.setExecutionStatus(ExecutionStatus.EXECUTED);
               }
               co.setApprovalDate(new Date());
               co.setApprovalUser(Context.internalSessionScope().getSecondUser());
               co.setApprovalRemark(Context.internalRequestScope().getRemark());

               if (co.getDcControllableId() != null) {
                  // if (isEncrypt()) {
                  // co.encrypt();
                  // }
                  co = Context.internalRequestScope().getEntityManager().merge(co);
               }
               if (isSendReleaseNotification()) {
                  notifyApproval(co);
               }
            }
         }

         log.debug("end TwoManRuleActuator.release");
         return result;
      } finally {
         Context.internalSessionScope().setUser(originalUser);
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
         Context.internalRequestScope().setCaseId(originalCaseId);
         Context.internalRequestScope().setRemark(originalRemark);
         Context.internalRequestScope().removeProperty(InternalRequestScope.DCCONTROLLABLE);

         if (removeSecondUserAfterRelease && !Context.requestScope().isPlaying()) {
            Context.internalSessionScope().setSecondUser(null);
            Context.internalSessionScope().setProperty(InternalSessionScope.SECOND_PRINCIPAL, null);
         }
      }
   }

   /**
    * @return the removeSecondUserAfterRelease
    */
   public boolean isRemoveSecondUserAfterRelease() {
      return removeSecondUserAfterRelease;
   }

   /**
    * @param removeS
    *           if true the second logged in user will be removed after the business case
    */
   public void setRemoveSecondUserAfterRelease(boolean removeS) {
      this.removeSecondUserAfterRelease = removeS;
   }

   /**
    * checks if the user id who releases / rejects is different from the user who created the controlled object.
    * 
    * @param co
    *           DcControllable
    * @throws InvalidUserException
    *            if the user has no permission
    */
   protected void checkApprovalUserId(DcControllable co) throws InvalidUserException {
      String approvalUserId = Context.internalSessionScope().getSecondUser();
      if (approvalUserId == null) {
         String msg = "Release without second user id not possible. No second user set in CibetContext!";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      if (approvalUserId.equals(co.getCreateUser())) {
         String msg = "release failed: user id of second authenticated user is "
               + "equal to the first authenticated user";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      String actualUser = Context.internalSessionScope().getUser();
      if (!actualUser.equals(co.getCreateUser())) {
         String msg = "release failed: the actual authenticated user is "
               + "not equal to the authenticated user who initiated the action";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      if (co.getApprovalUser() != null && !co.getApprovalUser().equals(approvalUserId)) {
         String msg = "release failed: Only user" + co.getApprovalUser()
               + " is allowed to release/reject DcControllable with ID " + co.getDcControllableId();
         log.error(msg);
         throw new InvalidUserException(msg);
      }
   }

}
