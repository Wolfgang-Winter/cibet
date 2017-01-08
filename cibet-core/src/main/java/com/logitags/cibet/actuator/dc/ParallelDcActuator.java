/*
 *******************************************************************************
 * L O G I T A G S
 * Software and Programming
 * Dr. Wolfgang Winter
 * Germany
 *
 * All rights reserved
 *
 * Copyright 2012 Dr. Wolfgang Winter
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

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Query;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.logitags.cibet.actuator.common.InvalidUserException;
import com.logitags.cibet.actuator.common.PostponedException;
import com.logitags.cibet.context.Context;
import com.logitags.cibet.context.InternalRequestScope;
import com.logitags.cibet.core.CibetException;
import com.logitags.cibet.core.ControlEvent;
import com.logitags.cibet.core.EventMetadata;
import com.logitags.cibet.core.EventResult;
import com.logitags.cibet.core.ExecutionStatus;

public class ParallelDcActuator extends FourEyesActuator {

   /**
    * 
    */
   private static final long serialVersionUID = 6812847126331743106L;

   private transient Log log = LogFactory.getLog(ParallelDcActuator.class);

   public static final String DEFAULTNAME = "PARALLEL_DC";

   /**
    * minimum number of parallel executions. Default is 1
    */
   private int executions = 1;

   /**
    * minimum time lag between two executions in sec. Default is 0.
    */
   private int timelag = 0;

   /**
    * true if the executions must be done by different users. The release must always be done by another user, different
    * from those who did the executions. Default is true.
    */
   private boolean differentUsers = true;

   public ParallelDcActuator() {
      setName(DEFAULTNAME);
   }

   public ParallelDcActuator(String name) {
      setName(name);
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.dc.FourEyesActuator#beforeEvent(com.logitags .cibet.core.EventMetadata)
    */
   @Override
   public void beforeEvent(EventMetadata ctx) {
      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip beforeEvent of " + this.getClass().getSimpleName());
         return;
      }

      switch (ctx.getControlEvent()) {
      case INVOKE:
         checkUnapprovedResource(ctx);

         if (timelag > 0 || differentUsers) {
            log.debug(ctx.getCaseId());
            List<DcControllable> dcList = DcLoader.loadByCaseId(ctx.getCaseId());
            if (!dcList.isEmpty()) {
               // check timelag
               long lag = new Date().getTime() - dcList.get(dcList.size() - 1).getCreateDate().getTime();
               if (lag < timelag * 1000) {
                  String err = "Execution of method " + ctx.getResource().getMethod() + " controlled by " + getName()
                        + " not possible: Time lag between subsequent executions must be " + timelag
                        + " sec but is only " + lag / 1000 + " sec.";
                  log.warn(err);
                  throw new CibetException(err);
               }

               // check if different user
               if (differentUsers) {
                  String user = findUserId();
                  for (DcControllable dc : dcList) {
                     if (user.equals(dc.getCreateUser()) && dc.getExecutionStatus() == ExecutionStatus.POSTPONED) {
                        String err = "This resource must be executed by different users. User " + user
                              + " has already invoked resource " + ctx.getResource();
                        log.warn(err);
                        throw new InvalidUserException(err);
                     }
                  }
               }
            }
         }

         findUserId();

         if (isThrowPostponedException()) {
            try {
               ctx.setException(postponedExceptionType.newInstance());
            } catch (InstantiationException e) {
               throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
               throw new RuntimeException(e);
            }
         }

         // fall through

      case SUBMIT_INVOKE:
         if (!Context.requestScope().isPlaying()) {
            Context.internalRequestScope().setProperty(InternalRequestScope.IS_POSTPONED, true);
         }
         break;

      default:
         break;
      }
   }

   /*
    * (non-Javadoc)
    * 
    * @see com.logitags.cibet.actuator.dc.FourEyesActuator#afterEvent(com.logitags .cibet.core.EventMetadata)
    */
   @Override
   public void afterEvent(EventMetadata ctx) {
      Context.internalRequestScope().removeProperty(InternalRequestScope.IS_POSTPONED);

      if (ctx.getExecutionStatus() == ExecutionStatus.DENIED) {
         log.info("EventProceedStatus is DENIED. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      if (ctx.getExecutionStatus() == ExecutionStatus.ERROR) {
         log.info("ERROR detected. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      if (Context.requestScope().isPlaying()) {
         log.debug("Play mode. Skip afterEvent of " + this.getClass().getSimpleName());
         return;
      }

      switch (ctx.getControlEvent()) {
      case SUBMIT_INVOKE:
         ctx.setExecutionStatus(ExecutionStatus.POSTPONED);
         break;

      case INVOKE:
         ctx.setExecutionStatus(ExecutionStatus.POSTPONED);
         DcControllable dcObj = createControlledObject(ControlEvent.INVOKE, ctx);

         if (isEncrypt()) {
            dcObj.encrypt();
         }
         Context.internalRequestScope().getEntityManager().persist(dcObj);

         if (ctx.getException() != null && ctx.getException() instanceof PostponedException) {
            ((PostponedException) ctx.getException()).setDcControllable(dcObj);
         }

         notifyAssigned(ctx.getExecutionStatus(), dcObj);
         break;

      default:
         break;
      }
   }

   protected void checkUnapprovedResource(EventMetadata ctx) {
      Query q = Context.internalRequestScope().getEntityManager().createNamedQuery(DcControllable.SEL_BY_UNIQUEID);
      q.setParameter("uniqueId", ctx.getResource().getUniqueId());
      List<DcControllable> list = (List<DcControllable>) q.getResultList();
      for (DcControllable dc : list) {
         if (dc.getCaseId().equals(ctx.getCaseId()))
            continue;
         switch (dc.getExecutionStatus()) {
         case FIRST_POSTPONED:
         case FIRST_RELEASED:
         case PASSEDBACK:
         case POSTPONED:
            String msg = "An unreleased Dual Control business case with ID " + dc.getDcControllableId() + " and status "
                  + dc.getExecutionStatus() + " exists already for this resource of type "
                  + ctx.getResource().getTargetType()
                  + ". This Dual Control business case must be approved or rejected first.";
            log.info(msg);
            throw new UnapprovedResourceException(msg, dc);

         default:
         }
      }
   }

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
      String originalCaseId = Context.requestScope().getCaseId();
      String originalRemark = Context.internalRequestScope().getRemark();

      try {
         ControlEvent thisEvent = controlEventForRelease(co);
         log.debug("release event: " + thisEvent);

         List<DcControllable> dcList = DcLoader.loadByCaseId(co.getCaseId());
         log.debug(dcList.size() + " DcControllable loaded with caseId " + co.getCaseId());

         if (dcList.size() < executions) {
            String err = "Failed to release DcControllable with ID " + co.getDcControllableId() + ": The event\n"
                  + co.getResource() + "\nmust be executed at least " + executions
                  + " times but has been executed only " + dcList.size() + " times";
            log.warn(err);
            throw new ResourceApplyException(err);
         }

         Iterator<DcControllable> iter = dcList.iterator();
         while (iter.hasNext()) {
            DcControllable dcElement = iter.next();
            if (dcElement.getExecutionStatus() != ExecutionStatus.POSTPONED) {
               iter.remove();
            } else {
               checkApprovalUserId(dcElement);
            }
         }

         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, thisEvent);
         Context.requestScope().setCaseId(co.getCaseId());
         if (remark != null) {
            Context.internalRequestScope().setRemark(remark);
         }
         Context.internalRequestScope().setProperty(InternalRequestScope.DCCONTROLLABLE, co);

         Object result = co.getResource().getResourceHandler().apply(co.getControlEvent());

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
               co.setApprovalUser(Context.internalSessionScope().getUser());
               co.setApprovalRemark(Context.internalRequestScope().getRemark());

               if (isEncrypt()) {
                  co.encrypt();
               }
               co = Context.internalRequestScope().getEntityManager().merge(co);
               if (isSendReleaseNotification()) {
                  notifyApproval(co);
               }

               for (DcControllable dcElement : dcList) {
                  if (dcElement.getDcControllableId() != co.getDcControllableId()) {
                     dcElement.setExecutionStatus(ExecutionStatus.REJECTED);
                     dcElement.setApprovalDate(co.getApprovalDate());
                     dcElement.setApprovalUser(Context.internalSessionScope().getUser());
                     dcElement.setApprovalRemark(Context.internalRequestScope().getRemark());

                     if (isEncrypt()) {
                        dcElement.encrypt();
                     }
                     Context.internalRequestScope().getEntityManager().merge(dcElement);
                     if (isSendRejectNotification()) {
                        notifyApproval(dcElement);
                     }
                  }
               }
            }
         }

         log.debug("end ParallelDcActuator.release");
         return result;
      } finally {
         Context.internalRequestScope().setProperty(InternalRequestScope.CONTROLEVENT, originalControlEvent);
         Context.requestScope().setCaseId(originalCaseId);
         Context.internalRequestScope().setRemark(originalRemark);
         Context.internalRequestScope().removeProperty(InternalRequestScope.DCCONTROLLABLE);
      }
   }

   /**
    * @return the executions
    */
   public int getExecutions() {
      return executions;
   }

   /**
    * @param executions
    *           the executions to set
    */
   public void setExecutions(int executions) {
      this.executions = executions;
   }

   /**
    * @return the timelag
    */
   public int getTimelag() {
      return timelag;
   }

   /**
    * @param timelag
    *           the timelag to set
    */
   public void setTimelag(int timelag) {
      this.timelag = timelag;
   }

   /**
    * @return the differentUsers
    */
   public boolean isDifferentUsers() {
      return differentUsers;
   }

   /**
    * @param differentUsers
    *           the differentUsers to set
    */
   public void setDifferentUsers(boolean differentUsers) {
      this.differentUsers = differentUsers;
   }

}
