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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.AbstractActuator;
import com.logitags.cibet.actuator.common.Controllable;
import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.config.Configuration;
import com.logitags.cibet.config.ProxyConfig;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;
import com.logitags.cibet.diff.Difference;
import com.logitags.cibet.notification.NotificationProvider;
import com.logitags.cibet.resource.ParameterType;
import com.logitags.cibet.resource.PersistenceUtil;
import com.logitags.cibet.resource.Resource;
import com.logitags.cibet.resource.ResourceParameter;
import com.logitags.cibet.sensor.ejb.EjbResource;

/**
 * applies four-eyes control.
 * 
 */
public class FourEyesActuator extends AbstractActuator implements DcActuator {

   /**
    * 
    */
   private static final long serialVersionUID = -3521038139055197044L;
   private transient Log log = LogFactory.getLog(FourEyesActuator.class);

   public static final String DEFAULTNAME = "FOUR_EYES";
   public static final String CLEANOBJECT = "__CLEAN_OBJECT";
   public static final String DIFFERENCES = "__DIFFERENCES";

   private boolean throwPostponedException = false;

   protected Class<? extends PostponedException> postponedExceptionType;

   /**
    * optional parameter for EJB sensor. In case cibet could not determine the jndi name of the EJB to invoke a method
    * automatically it must be set explicitly here.
    */
   protected String jndiName;

   private boolean sendAssignNotification = true;

   private boolean sendReleaseNotification = true;

   private boolean sendRejectNotification = true;

   private boolean sendPassBackNotification = true;

   /**
    * flag to encrypt target, result and parameter values of Resource.
    */
   private boolean encrypt = false;

   /**
    * flag if eager loading of a JPA resource is necessary before storing the Controllable.
    */
   private boolean loadEager = true;

   /**
    * list of property names that will be stored as ResourceParameters with the Controllable. Only applicable for
    * PERSIST events.
    */
   private Collection<String> storedProperties = new ArrayList<String>();

   public FourEyesActuator() {
      setName(DEFAULTNAME);
   }

   public FourEyesActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#beforeEvent(com.logitags.cibet.core. EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip beforeEvent of " + this.getClass().getSimpleName());
         return;
      }

      findUserId();

      switch (ctx.getControlEvent()) {
      case REJECT:
      case REJECT_DELETE:
      case REJECT_INSERT:
      case REJECT_INVOKE:
      case REJECT_UPDATE:
      case REJECT_SELECT:
         ctx.setExecutionStatus(ExecutionStatus.REJECTED);
         break;

      case PASSBACK:
      case PASSBACK_DELETE:
      case PASSBACK_INSERT:
      case PASSBACK_INVOKE:
      case PASSBACK_SELECT:
      case PASSBACK_UPDATE:
         ctx.setExecutionStatus(ExecutionStatus.PASSEDBACK);
         break;

      case FIRST_RELEASE:
      case FIRST_RELEASE_DELETE:
      case FIRST_RELEASE_INSERT:
      case FIRST_RELEASE_INVOKE:
      case FIRST_RELEASE_UPDATE:
      case FIRST_RELEASE_SELECT:
         ctx.setExecutionStatus(ExecutionStatus.FIRST_RELEASED);
         break;

      case UPDATE:
         if (isLoadEager()) {
            loadEager(ctx);
         }

      case DELETE:
      case RESTORE_UPDATE:
      case INVOKE:
      case IMPLICIT:
      case REDO:
         if (ctx.getExecutionStatus() == ExecutionStatus.EXECUTING) {
            checkUnapprovedResource(ctx);
         }
         // no break, fall through
      case INSERT:
      case SELECT:
      case RESTORE_INSERT:
      case SUBMIT_DELETE:
      case SUBMIT_INSERT:
      case SUBMIT_INVOKE:
      case SUBMIT_SELECT:
      case SUBMIT_UPDATE:
         ctx.setExecutionStatus(getPostponedExecutionStatus());
         if (isThrowPostponedException()) {
            try {
               ctx.setException(postponedExceptionType.newInstance());
            } catch (InstantiationException e) {
               throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            }
         }

      default:
         break;
      }
   }

   protected ExecutionStatus getPostponedExecutionStatus() {
      return ExecutionStatus.POSTPONED;
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.core.Actuator#afterEvent(com.logitags.cibet.core. EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      if (ctx.getExecutionStatus() == ExecutionStatus.ERROR) {
         log.info("ERROR detected. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      if (Context.requestScope().isPlaying()) {
         return;
      }

      Controllable dcObj = null;

      switch (ctx.getControlEvent()) {
      case UPDATE:
         dcObj = createControlledObject(ctx.getControlEvent(), ctx);

         List<Difference> diffs = PersistenceUtil.getDirtyUpdates(ctx);
         ResourceParameter propertyResParam = new ResourceParameter(DIFFERENCES, diffs.getClass().getName(), diffs,
               ParameterType.INTERNAL_PARAMETER, dcObj.getResource().getParameters().size() + 1);
         dcObj.getResource().addParameter(propertyResParam);
         break;

      case DELETE:
      case SELECT:
         dcObj = createControlledObject(ctx.getControlEvent(), ctx);
         break;

      case INSERT:
      case RESTORE_INSERT:
         EntityManager em = Context.internalRequestScope().getNullableApplicationEntityManager();
         if (em != null) {
            em.flush();
         }
         dcObj = createControlledObject(ControlEvent.INSERT, ctx);
         break;

      case RESTORE_UPDATE:
         dcObj = createControlledObject(ControlEvent.UPDATE, ctx);
         break;

      case IMPLICIT:
      case INVOKE:
      case REDO:
         dcObj = createControlledObject(ControlEvent.INVOKE, ctx);
         break;

      default:
         log.debug("skip " + ctx.getControlEvent() + " in " + this.getName());
         break;
      }

      if (dcObj != null) {
         if (dcObj.getResource().getResourceId() == null) {
            if (encrypt) {
               dcObj.getResource().encrypt();
            }
            Context.internalRequestScope().getEntityManager().persist(dcObj.getResource());
         }

         Context.internalRequestScope().getEntityManager().persist(dcObj);

         if (ctx.getException() != null && ctx.getException() instanceof PostponedException) {
            ((PostponedException) ctx.getException()).setControllable(dcObj);
         }

         notifyAssigned(ctx.getExecutionStatus(), dcObj);
      }
   }

   protected void notifyAssigned(ExecutionStatus status, Controllable dc) {
      if (sendAssignNotification && status == ExecutionStatus.POSTPONED && dc.getReleaseAddress() != null) {
         NotificationProvider notifProvider = Configuration.instance().getNotificationProvider();
         if (notifProvider != null) {
            notifProvider.notify(status, dc);
         }
      }
   }

   protected void notifyApproval(Controllable dc) {
      if (dc.getCreateAddress() != null) {
         NotificationProvider notifProvider = Configuration.instance().getNotificationProvider();
         if (notifProvider != null) {
            notifProvider.notify(dc.getExecutionStatus(), dc);
         }
      }
   }

   private String truncate255(String in) {
      if (in == null || in.length() <= 255) {
         return in;
      } else {
         return in.substring(0, 255);
      }
   }

   protected Controllable createControlledObject(ControlEvent event, EventMetadata ctx) {
      Controllable dc = new Controllable();
      dc.setControlEvent(event);
      dc.setCaseId(ctx.getCaseId());
      dc.setCreateUser(findUserId());
      dc.setCreateAddress(Context.internalSessionScope().getUserAddress());
      dc.setTenant(Context.internalSessionScope().getTenant());
      dc.setExecutionStatus(ExecutionStatus.POSTPONED);
      dc.setCreateRemark(truncate255(Context.requestScope().getRemark()));
      dc.setScheduledDate(Context.requestScope().getScheduledDate());
      dc.setResource(ctx.getResource());

      if (ctx.getResource() instanceof EjbResource) {
         ((EjbResource) dc.getResource()).setInvokerParam(jndiName);
      }

      dc.setActuator(getName());
      setActuatorSpecificProperties(dc);
      addStoredProperties(dc.getResource(), storedProperties);

      if (ctx.getProxyConfig() != null && (event == ControlEvent.FIRST_RELEASE_INVOKE || event == ControlEvent.INVOKE
            || event == ControlEvent.PASSBACK_INVOKE || event == ControlEvent.REDO
            || event == ControlEvent.REJECT_INVOKE || event == ControlEvent.RELEASE_INVOKE
            || event == ControlEvent.SUBMIT_INVOKE)) {

         ResourceParameter proxyConfig = new ResourceParameter(ProxyConfig.PROXYCONFIG, ProxyConfig.class.getName(),
               ctx.getProxyConfig(), ParameterType.INTERNAL_PARAMETER, dc.getResource().getParameters().size() + 1);
         dc.getResource().addParameter(proxyConfig);
      }

      return dc;
   }

   protected String findUserId() {
      String userId = Context.internalSessionScope().getUser();
      if (userId == null) {
         String txt = "User not set into CibetContext!";
         throw new RuntimeException(txt);
      }
      return userId;
   }

   protected void checkUnapprovedResource(EventMetadata ctx) {
      Resource resource = ctx.getResource();
      String uniqueId = resource.getUniqueId();
      log.debug("check unapproved resource with id " + uniqueId);
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(Controllable.SEL_BY_UNIQUEID);
      q.setParameter("uniqueId", uniqueId);
      List<Controllable> list = (List<Controllable>) q.getResultList();
      for (Controllable dc : list) {
         switch (dc.getExecutionStatus()) {
         case FIRST_POSTPONED:
         case FIRST_RELEASED:
         case PASSEDBACK:
         case POSTPONED:
            String msg = "An unreleased Dual Control business case with ID " + dc.getControllableId() + " and status "
                  + dc.getExecutionStatus() + " exists already for this resource of type " + resource.getTarget()
                  + ". This Dual Control business case must be approved or rejected first.";
            log.info(msg);
            throw new UnapprovedResourceException(msg, dc);

         default:
         }
      }
   }

   /**
    * set actuator of the ControlledObject to FOUR_EYES.
    * 
    * @param dc
    *           Controllable object
    */
   protected void setActuatorSpecificProperties(Controllable dc) {
      dc.setReleaseUser(Context.internalSessionScope().getApprovalUser());
      dc.setReleaseAddress(Context.internalSessionScope().getApprovalAddress());
   }

   /**
    * @return the throwPostponedException
    */
   public boolean isThrowPostponedException() {
      return throwPostponedException;
   }

   /**
    * @param throwP
    *           true if a PostponedException shall be thrown in case of postponed
    * 
    */
   public void setThrowPostponedException(boolean throwP) {
      this.throwPostponedException = throwP;
      if (this.throwPostponedException == true) {
         postponedExceptionType = resolvePostponedExceptionType();
      }
   }

   /**
    * @return the jndiName
    */
   public String getJndiName() {
      return jndiName;
   }

   /**
    * @param jndiName
    *           the jndiName to set
    */
   public void setJndiName(String jndiName) {
      this.jndiName = jndiName;
   }

   protected ControlEvent controlEventForRelease(Controllable co) {
      switch (co.getControlEvent()) {
      case INVOKE:
         return ControlEvent.RELEASE_INVOKE;
      case DELETE:
         return ControlEvent.RELEASE_DELETE;
      case INSERT:
         return ControlEvent.RELEASE_INSERT;
      case UPDATE:
         return ControlEvent.RELEASE_UPDATE;
      case SELECT:
         return ControlEvent.RELEASE_SELECT;
      case IMPLICIT:
         return ControlEvent.RELEASE;
      default:
         String msg = "Controlled object [" + co.getControllableId() + "] with control event " + co.getControlEvent()
               + " cannot be released";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
   }

   protected ControlEvent controlEventForReject(Controllable co) {
      switch (co.getControlEvent()) {
      case INVOKE:
         return ControlEvent.REJECT_INVOKE;
      case DELETE:
         return ControlEvent.REJECT_DELETE;
      case INSERT:
         return ControlEvent.REJECT_INSERT;
      case UPDATE:
         return ControlEvent.REJECT_UPDATE;
      case SELECT:
         return ControlEvent.REJECT_SELECT;
      case IMPLICIT:
         return ControlEvent.REJECT;
      default:
         String msg = "Controlled object [" + co.getControllableId() + "] with control event " + co.getControlEvent()
               + " cannot be rejected";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
   }

   protected ControlEvent controlEventForPassBack(Controllable co) {
      switch (co.getControlEvent()) {
      case INVOKE:
         return ControlEvent.PASSBACK_INVOKE;
      case DELETE:
         return ControlEvent.PASSBACK_DELETE;
      case INSERT:
         return ControlEvent.PASSBACK_INSERT;
      case UPDATE:
         return ControlEvent.PASSBACK_UPDATE;
      case SELECT:
         return ControlEvent.PASSBACK_SELECT;
      case IMPLICIT:
         return ControlEvent.PASSBACK;
      default:
         String msg = "Controlled object [" + co.getControllableId() + "] with control event " + co.getControlEvent()
               + " cannot be passed back";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
   }

   protected ControlEvent controlEventForSubmit(Controllable co) {
      switch (co.getControlEvent()) {
      case INVOKE:
         return ControlEvent.SUBMIT_INVOKE;
      case DELETE:
         return ControlEvent.SUBMIT_DELETE;
      case INSERT:
         return ControlEvent.SUBMIT_INSERT;
      case UPDATE:
         return ControlEvent.SUBMIT_UPDATE;
      case SELECT:
         return ControlEvent.SUBMIT_SELECT;
      case IMPLICIT:
         return ControlEvent.SUBMIT;
      default:
         String msg = "Controlled object [" + co.getControllableId() + "] with control event " + co.getControlEvent()
               + " cannot be submitted";
         log.error(msg);
         throw new IllegalArgumentException(msg);
      }
   }

   /**
    * checks if the user id who releases is different from the user who created the controlled object.
    * 
    * @param co
    *           Controllable
    */
   protected void checkApprovalUserId(Controllable co) throws InvalidUserException {
      String approvalUserId = Context.internalSessionScope().getUser();
      if (approvalUserId == null) {
         String msg = "Release without user id not possible. No user set in Context!";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      if (approvalUserId.equals(co.getCreateUser())) {
         String msg = "release failed: user id " + approvalUserId + " of releasing user is equal to the "
               + "user id of the initial user " + co.getCreateUser();
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      if (co.getReleaseUser() != null && !co.getReleaseUser().equals(approvalUserId)) {
         String msg = "release failed: Only user " + co.getReleaseUser()
               + " is allowed to release Controllable with ID " + co.getControllableId();
         log.error(msg);
         throw new InvalidUserException(msg);
      }
   }

   /**
    * checks if the user id who rejects is allowed to reject.
    * 
    * @param co
    *           Controllable
    * @throws InvalidUserException
    *            if user has no permission
    */
   protected void checkRejectUserId(Controllable co) throws InvalidUserException {
      String userId = Context.internalSessionScope().getUser();
      if (userId == null) {
         String msg = "Reject/pass back without user id not possible. No user set in CibetContext!";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      // initiating user can always reject/pass back
      if (userId.equals(co.getCreateUser())) {
         return;
      }

      if (co.getReleaseUser() != null && !co.getReleaseUser().equals(userId)) {
         String msg = "reject/pass back failed: Only user" + co.getReleaseUser()
               + " is allowed to reject/pass back Controllable with ID " + co.getControllableId();
         log.error(msg);
         throw new InvalidUserException(msg);
      }
   }

   protected void checkSubmitUserId(Controllable co) throws InvalidUserException {
      String userId = Context.internalSessionScope().getUser();
      if (userId == null) {
         String msg = "Submit without user id not possible. No user set in CibetContext!";
         log.error(msg);
         throw new InvalidUserException(msg);
      }

      // only initiating user can submit
      if (!userId.equals(co.getCreateUser())) {
         String msg = "submit failed: Only user " + co.getCreateUser() + " is allowed to submit Controllable with ID "
               + co.getControllableId();
         log.error(msg);
         throw new InvalidUserException(msg);
      }
   }

   /**
    * @return the postponedExceptionType
    */
   public Class<? extends PostponedException> getPostponedExceptionType() {
      return postponedExceptionType;
   }

   @Override
   public Object release(Controllable co, String remark) throws ResourceApplyException {
      checkExecutionStatus("release", co);

      ControlEvent originalControlEvent = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      String originalCaseId = Context.requestScope().getCaseId();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         ControlEvent thisEvent = controlEventForRelease(co);
         log.debug("release event: " + thisEvent);
         checkApprovalUserId(co);

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, thisEvent);
         Context.requestScope().setCaseId(co.getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLLABLE, co);

         Object result = co.getResource().apply(co.getControlEvent());

         if (!Context.requestScope().isPlaying()) {
            doRelease(co);
         }

         log.debug("end FourEyesActuator.release");
         return result;
      } finally {
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
         Context.requestScope().setCaseId(originalCaseId);
         Context.internalRequestScope().setRemark(originalRemark);
         Context.internalRequestScope().removeProperty(InternalRequestScope.CONTROLLABLE);
      }
   }

   protected void doRelease(Controllable co) {
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
         co.setReleaseDate(new Date());
         co.setReleaseUser(Context.internalSessionScope().getUser());
         co.setReleaseRemark(Context.internalRequestScope().getRemark());
         co = Context.internalRequestScope().getEntityManager().merge(co);
         if (sendReleaseNotification) {
            notifyApproval(co);
         }
      }
   }

   protected void checkExecutionStatus(String action, Controllable co) throws ResourceApplyException {
      if (co.getExecutionStatus() != ExecutionStatus.POSTPONED) {
         String err = "Failed to " + action + " Controllable with ID " + co.getControllableId()
               + ": should be in status POSTPONED but is in status " + co.getExecutionStatus();
         log.warn(err);
         throw new ResourceApplyException(err);
      }
   }

   protected void checkRejectExecutionStatus(Controllable co) throws ResourceApplyException {
      if (co.getExecutionStatus() != ExecutionStatus.POSTPONED
            && co.getExecutionStatus() != ExecutionStatus.PASSEDBACK) {
         String err = "Failed to reject Controllable with ID " + co.getControllableId()
               + ": should be in status POSTPONED or PASSEDBACK but is in status " + co.getExecutionStatus();
         log.warn(err);
         throw new ResourceApplyException(err);
      }
   }

   @Override
   public void reject(Controllable co, String remark) throws ResourceApplyException {
      checkRejectExecutionStatus(co);
      checkRejectUserId(co);

      ControlEvent originalControlEvent = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      String originalCaseId = Context.requestScope().getCaseId();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         ControlEvent event = controlEventForReject(co);
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, event);
         Context.requestScope().setCaseId(co.getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }

         co.getResource().apply(co.getControlEvent());

         if (!Context.requestScope().isPlaying()) {
            EventResult eventResult = Context.internalRequestScope().getExecutedEventResult();
            if (eventResult == null) {
               eventResult = new EventResult();
               eventResult.setExecutionStatus(ExecutionStatus.EXECUTED);
               Context.internalRequestScope().registerEventResult(eventResult);
            }
            if (eventResult.getExecutionStatus() != ExecutionStatus.DENIED) {
               co.setExecutionStatus(ExecutionStatus.REJECTED);
               co.setReleaseDate(new Date());
               co.setReleaseUser(Context.internalSessionScope().getUser());
               co.setReleaseRemark(Context.internalRequestScope().getRemark());

               // if (encrypt) {
               // co.encrypt();
               // }
               co = Context.internalRequestScope().getEntityManager().merge(co);
               if (sendRejectNotification) {
                  notifyApproval(co);
               }
            }
         }

      } finally {
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
         Context.internalRequestScope().setCaseId(originalCaseId);
         Context.internalRequestScope().setRemark(originalRemark);
      }
   }

   @Override
   public void passBack(Controllable co, String remark) throws ResourceApplyException {
      checkExecutionStatus("pass back", co);
      checkRejectUserId(co);

      ControlEvent originalControlEvent = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      String originalCaseId = Context.requestScope().getCaseId();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         ControlEvent event = controlEventForPassBack(co);
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, event);
         Context.requestScope().setCaseId(co.getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }

         co.getResource().apply(co.getControlEvent());

         if (!Context.requestScope().isPlaying()) {
            EventResult eventResult = Context.internalRequestScope().getExecutedEventResult();
            if (eventResult == null) {
               eventResult = new EventResult();
               eventResult.setExecutionStatus(ExecutionStatus.EXECUTED);
               Context.internalRequestScope().registerEventResult(eventResult);
            }
            if (eventResult.getExecutionStatus() != ExecutionStatus.DENIED) {
               co.setExecutionStatus(ExecutionStatus.PASSEDBACK);
               co.setReleaseDate(new Date());
               co.setReleaseUser(Context.internalSessionScope().getUser());
               co.setReleaseRemark(Context.internalRequestScope().getRemark());

               // if (encrypt) {
               // co.encrypt();
               // }
               co = Context.internalRequestScope().getEntityManager().merge(co);
               if (sendPassBackNotification) {
                  notifyApproval(co);
               }
            }

         }

      } finally {
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
         Context.internalRequestScope().setCaseId(originalCaseId);
         Context.internalRequestScope().setRemark(originalRemark);
      }
   }

   @Override
   public void submit(Controllable co, String remark) throws ResourceApplyException {
      checkSubmitUserId(co);

      ControlEvent originalControlEvent = (ControlEvent) Context.internalRequestScope()
            .getProperty(InternalRequestScope.CONTROLEVENT);
      String originalCaseId = Context.requestScope().getCaseId();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         ControlEvent event = controlEventForSubmit(co);
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, event);
         Context.requestScope().setCaseId(co.getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }

         co.getResource().apply(co.getControlEvent());

         if (!Context.requestScope().isPlaying()) {
            EventResult eventResult = Context.internalRequestScope().getExecutedEventResult();
            if (eventResult == null) {
               eventResult = new EventResult();
               eventResult.setExecutionStatus(ExecutionStatus.EXECUTED);
               Context.internalRequestScope().registerEventResult(eventResult);
            }
            if (eventResult.getExecutionStatus() != ExecutionStatus.DENIED) {
               co.setExecutionStatus(getPostponedExecutionStatus());
               co.setCreateRemark(Context.internalRequestScope().getRemark());
               setActuatorSpecificProperties(co);
               // reset scheduled date if user wants to
               if (Context.internalRequestScope().getScheduledDate() != null) {
                  co.setScheduledDate(Context.internalRequestScope().getScheduledDate());
               }

               // if (encrypt) {
               // co.encrypt();
               // }
               co = Context.internalRequestScope().getEntityManager().merge(co);
               notifyAssigned(ExecutionStatus.POSTPONED, co);
            }

         }

      } finally {
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
         Context.internalRequestScope().setCaseId(originalCaseId);
         Context.internalRequestScope().setRemark(originalRemark);
      }
   }

   /**
    * @return the sendAssignNotification
    */
   public boolean isSendAssignNotification() {
      return sendAssignNotification;
   }

   /**
    * @param sendAssignNotification
    *           the sendAssignNotification to set
    */
   public void setSendAssignNotification(boolean sendAssignNotification) {
      this.sendAssignNotification = sendAssignNotification;
   }

   /**
    * @return the sendReleaseNotification
    */
   public boolean isSendReleaseNotification() {
      return sendReleaseNotification;
   }

   /**
    * @param sendReleaseNotification
    *           the sendReleaseNotification to set
    */
   public void setSendReleaseNotification(boolean sendReleaseNotification) {
      this.sendReleaseNotification = sendReleaseNotification;
   }

   /**
    * @return the sendRejectNotification
    */
   public boolean isSendRejectNotification() {
      return sendRejectNotification;
   }

   /**
    * @param sendRejectNotification
    *           the sendRejectNotification to set
    */
   public void setSendRejectNotification(boolean sendRejectNotification) {
      this.sendRejectNotification = sendRejectNotification;
   }

   /**
    * @return the encrypt
    */
   public boolean isEncrypt() {
      return encrypt;
   }

   /**
    * @param encrypt
    *           the encrypt to set
    */
   public void setEncrypt(boolean encrypt) {
      this.encrypt = encrypt;
   }

   /**
    * @return the sendPassBackNotification
    */
   public boolean isSendPassBackNotification() {
      return sendPassBackNotification;
   }

   /**
    * @param sendPassBackNotification
    *           the sendPassBackNotification to set
    */
   public void setSendPassBackNotification(boolean sendPassBackNotification) {
      this.sendPassBackNotification = sendPassBackNotification;
   }

   /**
    * @return the storedProperties
    */
   public Collection<String> getStoredProperties() {
      return storedProperties;
   }

   /**
    * @param storedProperties
    *           the storedProperties to set
    */
   public void setStoredProperties(Collection<String> storedProperties) {
      this.storedProperties = storedProperties;
   }

   /**
    * @return the loadEager
    */
   public boolean isLoadEager() {
      return loadEager;
   }

   /**
    * @param loadEager
    *           the loadEager to set
    */
   public void setLoadEager(boolean loadEager) {
      this.loadEager = loadEager;
   }

}
